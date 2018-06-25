/*
 * Copyright International Business Machines Corp, 2018.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.spectrumcomputing.cwl.exec.executor.lsf;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLInstanceService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLServiceFactory;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLStepBindingResolver;
import com.ibm.spectrumcomputing.cwl.exec.util.command.LSFCommandUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.outputs.CWLOutputJsonParser;
import com.ibm.spectrumcomputing.cwl.exec.util.outputs.OutputsCapturer;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowOutputParameter;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Run a CWL process (Workflow or CommandLineTool) instance with LSF
 */
public final class LSFWorkflowRunner {

    private static final Logger logger = LoggerFactory.getLogger(LSFWorkflowRunner.class);

    private static LSFWorkflowRunner workflowRunner;

    private final CWLInstanceService persistenceService = CWLServiceFactory.getService(CWLInstanceService.class);
    private final List<LSFWorkflowStepRunner> steps = new ArrayList<>();
    private final CWLInstance instance;
    private final int stepsCount;

    /**
     * Stops this runner
     */
    public static void stop() {
        if (workflowRunner != null) {
            //For CTRL-C operation, the instance may be not finished
            if (!workflowRunner.instance.isFinished()) {
                workflowRunner.instance.setState(CWLInstanceState.EXITED);
                workflowRunner.instance.setEndTime((new Date()).getTime());
                workflowRunner.instance.setExitCode(255);
                workflowRunner.persistenceService.updateCWLProcessInstance(workflowRunner.instance);
            }
            workflowRunner.killWaitingJobs();
            LSFReadyScatteJobExecutor.getExecutor().stop();
            LSFBsubExecutor.getExecutor().stop();
            LSFBwaitExecutor.getExecutor().stop();
        }
    }

    /**
     * Get a singleton runner
     * 
     * @param instance
     *            A CWL process (Workflow or CommandLineTool) instance will be ran with LSF
     * @return A singleton runner
     * @throws CWLException
     *             Failed to run the given CWL process instance
     */
    public static synchronized LSFWorkflowRunner runner(CWLInstance instance) throws CWLException {
        if (workflowRunner == null) {
            workflowRunner = new LSFWorkflowRunner(instance);
        }
        return workflowRunner;
    }

    private LSFWorkflowRunner(CWLInstance instance) throws CWLException {
        this.instance = instance;
        this.addSteps(this.instance);
        this.stepsCount = steps.size();
    }

    /**
     * Starts runner
     */
    public void start() {
        instance.setState(CWLInstanceState.RUNNING);
        instance.setStartTime(new Date().getTime());
        for (LSFWorkflowStepRunner step : steps) {
            // When rerun a workflow, the step state may be DONE
            if (step.getInstance().getState() == CWLInstanceState.DONE) {
                logger.info(ResourceLoader.getMessage("cwl.exec.job.has.done", step.getInstance().getName()));
            } else {
                logger.debug("submit step {}", step.getInstance().getName());
                LSFBsubExecutor.getExecutor().submit(new LSFBsubExecutorTask(step));
            }
        }
    }

    protected CWLInstance getInstance() {
        return instance;
    }

    protected void broadcast(LSFJobEvent event) {
        LSFJobEventType type = event.getType();
        logger.debug("broadcast event {}, {}", event.getType(), event.getInstanceName());
        switch (type) {
        case START:
            startStep(event);
            break;
        case DONE:
            finishWorkflow();
            break;
        case EXIT:
            exitWorkflow(event);
            break;
        default:
            break;
        }
    }

    private void startStep(LSFJobEvent event) {
        boolean hasListener = false;
        for (LSFWorkflowStepRunner step : steps) {
            CWLInstanceState state = step.getInstance().getState();
            if (state != CWLInstanceState.DONE && state != CWLInstanceState.EXITED && step.listen(event)) {
                hasListener = true;
            }
        }
        if (!hasListener) { // terminal step
            bwaitTerminalStep(event.getInstanceId());
        }
    }

    private synchronized void finishWorkflow() {
        int doneStepsCount = 0;
        for (LSFWorkflowStepRunner step : steps) {
            CWLInstanceState state = step.getInstance().getState();
            if (state == CWLInstanceState.DONE) {
                doneStepsCount = doneStepsCount + 1;
            }
        }
        logger.debug("done steps count: {}, steps count: {}", doneStepsCount, stepsCount);
        if (doneStepsCount == stepsCount) {
            if (instance instanceof CWLCommandInstance) {
                updateLSFTerminatedJob((CWLCommandInstance) instance, CWLInstanceState.DONE, 0);
            } else if (instance instanceof CWLWorkflowInstance) {
                updateLSFTerminatedFlow((CWLWorkflowInstance) instance);
            }
        } else {
            List<String> doneStepNames = new ArrayList<>();
            List<String> runningStepNames = new ArrayList<>();
            List<String> waitingStepNames = new ArrayList<>();
            for (LSFWorkflowStepRunner step : steps) {
                CWLInstanceState state = step.getInstance().getState();
                switch(state) {
                case DONE:
                    doneStepNames.add(step.getInstance().getName());
                    break;
                case RUNNING:
                    runningStepNames.add(step.getInstance().getName());
                    break;
                case WAITING:
                    waitingStepNames.add(step.getInstance().getName());
                    break;
                default:
                    break;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("{} step(s) was done: {}", doneStepNames.size(), doneStepNames);
                logger.debug("{} step(s) is running: {}", runningStepNames.size(), runningStepNames);
                logger.debug("{} step(s) is waiting: {}", waitingStepNames.size(), waitingStepNames);
            }
        }
    }

    private void exitWorkflow(LSFJobEvent event) {
        if (instance instanceof CWLCommandInstance) {
            updateLSFTerminatedJob((CWLCommandInstance) instance, CWLInstanceState.EXITED, event.getExitCode());
        } else if (instance instanceof CWLWorkflowInstance) {
            updateLSFTerminatedFlow((CWLWorkflowInstance) instance, CWLInstanceState.EXITED, event.getExitCode());
        }
    }

    private void updateLSFTerminatedJob(CWLCommandInstance instance, CWLInstanceState state, int exitCode) {
        if (state == CWLInstanceState.DONE) {
            try {
                collectOutputs(instance);
                instance.setExitCode(0);
            } catch (CWLException e) {
                state = CWLInstanceState.EXITED;
                logger.error(
                        ResourceLoader.getMessage("cwl.exec.job.capture.outputs", instance.getName(), e.getMessage()));
                instance.setExitCode(254);
            }
        }
        if (state == CWLInstanceState.EXITED) {
            logger.error(ResourceLoader.getMessage("cwl.exec.job.exited", instance.getName()));
            if (instance.getExitCode() != -1) {
                instance.setExitCode(exitCode);
            }
        }
        instance.setEndTime(new Date().getTime());
        instance.setState(state);
        persistenceService.updateCWLProcessInstance(instance);
        instance.setFinished(true);
    }

    private void updateLSFTerminatedFlow(CWLWorkflowInstance instance) {
        //avoid to finish early
        if (instance.getState() != CWLInstanceState.DONE) {
            updateLSFTerminatedFlow((CWLWorkflowInstance) instance, CWLInstanceState.DONE, 0);
        }
    }

    private void updateLSFTerminatedFlow(CWLWorkflowInstance instance, CWLInstanceState state, int exitCode) {
        if (state == CWLInstanceState.DONE) {
            try {
                OutputsCapturer.captureWorkflowOutputs(instance);
                logger.info(ResourceLoader.getMessage("cwl.exec.workflow.done", instance.getName()));
                collectOutputs(instance);
                instance.setExitCode(0);
            } catch (CWLException e) {
                state = CWLInstanceState.EXITED;
                logger.error(ResourceLoader.getMessage("cwl.exec.workflow.capture.outputs", instance.getName(),
                        e.getMessage()));
                instance.setExitCode(254);
            }
        }
        if (state == CWLInstanceState.EXITED) {
            instance.setExitCode(exitCode);
            logger.error(ResourceLoader.getMessage("cwl.exec.workflow.exited", instance.getName(),
                    instance.getExitCode()));
            killWaitingJobs();
        }
        instance.setEndTime(new Date().getTime());
        instance.setState(state);
        persistenceService.updateCWLProcessInstance(instance);
        instance.setFinished(true);
    }

    private void addSteps(CWLInstance instance) throws CWLException {
        if (instance instanceof CWLCommandInstance) {
            LSFWorkflowStepRunner step = new LSFWorkflowStepRunner(this, (CWLCommandInstance) instance);
            steps.add(step);
        } else if (instance instanceof CWLWorkflowInstance) {
            CWLWorkflowInstance main = (CWLWorkflowInstance) instance;
            List<CWLInstance> instances = main.getInstances();
            for (CWLInstance subInstance : instances) {
                if (subInstance instanceof CWLCommandInstance) {
                    LSFWorkflowStepRunner step = new LSFWorkflowStepRunner(this, (CWLCommandInstance) subInstance);
                    steps.add(step);
                }
            }
        }
    }

    private void bwaitTerminalStep(String terminalStepId) {
        for (LSFWorkflowStepRunner step : steps) {
            if (step.getInstance().getId().equals(terminalStepId)) {
                LSFBwaitExecutor.getExecutor().submit(new LSFBwaitExecutorTask(step, true));
                break;
            }
        }
    }

    private void killWaitingJobs() {
        List<Long> jobIds = new ArrayList<>();
        for (LSFWorkflowStepRunner step : steps) {
            CWLCommandInstance stepInstance = step.getInstance();
            if (stepInstance.getState() == CWLInstanceState.WAITING && stepInstance.getHPCJobId() != -1) {
                logger.warn(ResourceLoader.getMessage("cwl.exec.job.kill", stepInstance.getName(),
                        String.valueOf(stepInstance.getHPCJobId())));
                stepInstance.setState(CWLInstanceState.KILLED);
                jobIds.add(Long.valueOf(stepInstance.getHPCJobId()));
            }
        }
        LSFCommandUtil.killJobs(jobIds);
    }

    private void collectOutputs(CWLInstance instance) throws CWLException {
        Path cwlOutputJsonPath = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR), "cwl.output.json");
        if (cwlOutputJsonPath.toFile().exists()) {
            try {
                JsonNode jsonNode = CWLOutputJsonParser.parseCWLOutputJson(instance, cwlOutputJsonPath);
                CWLExecUtil.printStdoutMsg(CommonUtil.asPrettyJsonStr(jsonNode));
            } catch (IOException | CWLException e) {
                throw new CWLException(e.getMessage(), 255);
            }
        } else {
            OutputsCapturer.copyOutputFiles(instance);
            Map<String, Object> values = new HashMap<>();
            for (CWLParameter output : instance.getProcess().getOutputs()) {
                if (instance instanceof CWLWorkflowInstance) {
                    CWLStepBindingResolver.resolveWorkflowOutput((CWLWorkflowInstance) instance,
                            (WorkflowOutputParameter) output);
                }
                values.put(output.getId(), output.getValue());
            }
            CWLExecUtil.printStdoutMsg(CommonUtil.asPrettyJsonStr(values));
        }
    }
}

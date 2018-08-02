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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.service.CWLInstanceService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLRuntimeService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLServiceFactory;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecConfUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLStepBindingResolver;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutionResult;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutor;
import com.ibm.spectrumcomputing.cwl.exec.util.command.LSFCommandUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.CommandStdIOEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.InputsEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.outputs.OutputsCapturer;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.conf.PostFailureScript;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLScatterHolder;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.WorkflowStepInput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.model.process.tool.ExpressionTool;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * A LSF bwait task, this task will wait dependencies of a step, if all of
 * dependencies were done, a done event (the step has no dependencies) or a
 * start event will be send
 */
final class LSFBwaitExecutorTask implements Runnable {

    private static final String BWAIT = "bwait";

    private static final String CMD_FAILED_MSG = "cwl.exec.command.failed";
    private static final String JOB_START_WAIT_MSG = "cwl.exec.job.start.wait";

    private static final Logger logger = LoggerFactory.getLogger(LSFBwaitExecutorTask.class);

    private final CWLInstanceService persistenceService = CWLServiceFactory.getService(CWLInstanceService.class);
    private final CWLRuntimeService runtimeService = CWLServiceFactory.getService(CWLRuntimeService.class);
    private final boolean terminated;
    private final LSFWorkflowStepRunner step;

    private List<CWLCommandInstance> dependencies = null;

    /*
     * The terminated is a flag to mark the step is terminated step or not
     */
    protected LSFBwaitExecutorTask(LSFWorkflowStepRunner step, boolean terminated) {
        this.terminated = terminated;
        this.step = step;
        if (!this.terminated) {
            this.dependencies = findAllDependencyInstances(step);
        }
        logger.debug("A bwait task ({}) (terminated={})", step.getInstance().getName(), terminated);
    }

    @Override
    public void run() {
        CWLCommandInstance instance = step.getInstance();
        try {
            int exitCode = waitSteps();
            if (exitCode != 0) {
                LSFJobEvent event = new LSFJobEvent(LSFJobEventType.EXIT, instance.getId(), instance.getName());
                event.setExitCode(exitCode);
                step.getMain().broadcast(event);
            }
        } catch (Exception e) {
            logger.error(ResourceLoader.getMessage("cwl.exec.job.wait.failed",
                    instance.getName(), String.valueOf(instance.getHPCJobId()), e.getMessage()));
            if (logger.isDebugEnabled()) {
                logger.error("The exception stacks:", e);
            }
            LSFJobEvent event = new LSFJobEvent(LSFJobEventType.EXIT, instance.getId(), instance.getName());
            event.setExitCode(255);
            step.getMain().broadcast(event);
        }
    }

    private int waitSteps() throws CWLException {
        CWLCommandInstance instance = step.getInstance();
        List<String> bwait = buildStepBwaitCommamd(instance, this.dependencies);
        if(!(step.getInstance().getProcess() instanceof ExpressionTool)) {
            logger.info(ResourceLoader.getMessage(JOB_START_WAIT_MSG, CWLExecUtil.asPrettyCommandStr(bwait)));
        }
        CommandExecutionResult bwaitResult = CommandExecutor.run(bwait);
        if (bwaitResult.getExitCode() != 0) {
            String bwaitFailedTipMsg = ResourceLoader.getMessage("cwl.exec.job.bwait.failed",
                    bwaitResult.getExitCode(), bwaitResult.getErrMsg());
            logger.debug(bwaitFailedTipMsg);
            CWLCommandInstance failedInstance = tryToRecover(instance, this.dependencies);
            if (failedInstance != null) {
                if (this.terminated) {
                    logger.error(bwaitFailedTipMsg);
                }
                return failedInstance.getExitCode();
            }
        }
        makeStepSuccessful(instance, this.dependencies);
        return 0;
    }

    private void makeStepSuccessful(CWLCommandInstance instance,
            List<CWLCommandInstance> dependencies) throws CWLException {
        if (dependencies == null) {
            captureStepOutputs(instance);
        } else {
            for (CWLCommandInstance dependency : dependencies) {
                captureStepOutputs(dependency);
            }
            // Special handling for docker app was enabled, if the docker app
            // was enabled, we need modify the
            // job env, but LSF bmod cannot support modify the job env, so we
            // need kill the placeholder job
            // firstly, then re-bsub the job with the env
            if (dockcerAppEnabled(instance)) {
                resubmitDockerStep(instance);
            } else {
                fillOutActualCommand(instance);
                resume(instance);
            }
        }
    }

    private CWLCommandInstance tryToRecover(CWLCommandInstance mainInstance,
            List<CWLCommandInstance> dependencies) {
        CWLCommandInstance failedInstance = null;
        if (dependencies == null) {
            failedInstance = recoverFailedInstance(mainInstance);
        } else {
            for (CWLCommandInstance dependency : dependencies) {
                CWLCommandInstance failedDependency = recoverFailedInstance(dependency);
                if (failedDependency != null) {
                    failedInstance = failedDependency;
                    break;
                }
            }
        }
        return failedInstance;
    }

    private List<String> buildSingleBwaitCommand(CWLCommandInstance instance) {
        return Arrays.asList(BWAIT, "-w", String.format("done(%d)", instance.getHPCJobId()));
    }

    private List<String> buildBwaitCommand(List<CWLCommandInstance> instances) {
        List<String> waitJobs = new ArrayList<>();
        for (CWLCommandInstance instance : instances) {
            waitJobs.add(String.format("done(%d)", instance.getHPCJobId()));
        }
        return Arrays.asList(BWAIT, "-w", String.join(" && ", waitJobs));
    }

    private PostFailureScript getPostScriptConf(CWLInstance instance) {
        CWLInstance main = CWLExecUtil.findMainInstance(instance);
        FlowExecConf flowExecConf = main.getFlowExecConf();
        return CWLExecConfUtil.getPostFailureScript(flowExecConf, instance.getName());
    }

    private List<String> buildStepBwaitCommamd(CWLCommandInstance instance, List<CWLCommandInstance> dependencies) {
        List<String> bwait = null;
        if (dependencies == null) {
            bwait = buildSingleBwaitCommand(instance);
        } else {
            bwait = buildBwaitCommand(dependencies);
        }
        return bwait;
    }

    private CWLCommandInstance recoverFailedInstance(CWLCommandInstance instance) {
        CWLCommandInstance failedInstance = null;
        int exitCode = findLSFJobExitCode(instance.getHPCJobId());
        if (!inSuccessCodes(exitCode, instance)) {
            PostFailureScript pfs = getPostScriptConf(instance);
            if (pfs != null) {
                if (Paths.get(pfs.getScript()).toFile().exists()) {
                    logger.info(ResourceLoader.getMessage("cwl.exec.job.start.recovery",
                            String.valueOf(instance.getHPCJobId())));
                    if (applyPostFailureScript(instance, pfs)) {
                        exitCode = 0;
                    }
                } else {
                    logger.error(ResourceLoader.getMessage("cwl.exec.job.postscript.notexist", pfs.getScript()));
                }
            }
            if (exitCode != 0) {
                instance.setState(CWLInstanceState.EXITED);
                instance.setEndTime(new Date().getTime());
                instance.setExitCode(exitCode);
                persistenceService.updateCWLProcessInstance(instance);
                failedInstance = instance;
            }
        }
        return failedInstance;
    }

    private boolean dockcerAppEnabled(CWLCommandInstance instance) {
        boolean enabled = false;
        DockerRequirement dockerReq = CWLExecUtil.findRequirement(instance, DockerRequirement.class);
        if (dockerReq != null) {
            CWLInstance main = CWLExecUtil.findMainInstance(instance);
            FlowExecConf flowExecConf = main.getFlowExecConf();
            String app = CWLExecConfUtil.getApp(flowExecConf, instance.getName());
            enabled = (app != null && app.length() > 0);
        }
        return enabled;
    }

    private void resubmitDockerStep(CWLCommandInstance instance) throws CWLException {
        List<Long> placeholderJobs = Arrays.asList(instance.getHPCJobId());
        List<String> commands = fillOutPlaceholderStep(instance);
        if (logger.isDebugEnabled()) {
            logger.debug("Prepare bsub command for LSF docker app\n{}",
                    CWLExecUtil.asPrettyCommandStr(commands));
        }
        CommandExecutionResult bsubResult = CommandExecutor.run(commands);
        instance.setExecutionResult(bsubResult);
        if (bsubResult.getExitCode() == 0) {
            LSFCommandUtil.killJobs(placeholderJobs);
            logger.debug("Job placeholder ({}) was killed when bsub with docker app", placeholderJobs.get(0));
            logger.debug("Job ({}) was re-submitted with {}", instance.getName(), bsubResult.getOutMsg());
            String jobId = CWLExecUtil.matchJobId("Job <(\\d+)>.*", bsubResult.getOutMsg());
            instance.setHPCJobId(Long.valueOf(jobId));
            if (instance.isReadyToRun()) {
                instance.setState(CWLInstanceState.RUNNING);
                instance.setStartTime(new Date().getTime());
            }
            persistenceService.updateCWLProcessInstance(instance);
            if (instance.isReadyToRun()) {
                logger.debug("start the ready docker step ({})", step.getInstance().getName());
                step.getMain().broadcast(
                        new LSFJobEvent(LSFJobEventType.START, instance.getId(), instance.getName()));
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CMD_FAILED_MSG, commands, bsubResult.getErrMsg()),
                    255);
        }
    }

    private void fillOutActualCommand(CWLCommandInstance instance) throws CWLException {
        if (instance.getScatter() != null) {
            // For scatter step, we scatter it firstly, after the scatter jobs
            // are done, we bsub a
            // placeholder job (just exit the scatter exit code) to represent
            // the scatter step
            instance.setScatterHolders(new ArrayList<>());
            buildScatterCommands(instance);
            createScatterResultGatherStep(instance);
        } else {
            fillOutPlaceholderStep(instance);
        }
    }

    private void resume(CWLCommandInstance instance) throws CWLException {
        List<String> bresume = Arrays.asList("bresume", String.valueOf(instance.getHPCJobId()));
        logger.info(ResourceLoader.getMessage("cwl.exec.job.resume",
                step.getInstance().getName(),
                String.valueOf(step.getInstance().getHPCJobId()),
                CWLExecUtil.asPrettyCommandStr(bresume)));
        CommandExecutionResult resumeResult = CommandExecutor.run(bresume);
        if (resumeResult.getExitCode() == 0) {
            instance.setState(CWLInstanceState.RUNNING);
            instance.setStartTime(new Date().getTime());
            persistenceService.updateCWLProcessInstance(instance);
            logger.debug("resume the ready step ({})", step.getInstance().getName());
            LSFJobEvent event = new LSFJobEvent(LSFJobEventType.START, instance.getId(), instance.getName());
            step.getMain().broadcast(event);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CMD_FAILED_MSG, bresume, resumeResult.getErrMsg()),
                    255);
        }
    }

    private boolean applyPostFailureScript(CWLCommandInstance instance, PostFailureScript pfs) {
        Map<String, String> env = new HashMap<>();
        env.put("CWLEXEC_JOB_ID", String.valueOf(instance.getHPCJobId()));
        env.put("CWLEXEC_JOB_BSUB", String.join(" ", instance.getCommands()));
        env.put("CWLEXEC_JOB_CMD", String.join(" ", instance.getCommands().get(instance.getCommands().size() - 1)));
        env.put("CWLEXEC_JOB_CWD", instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR));
        env.put("CWLEXEC_JOB_OUTDIR", instance.getRuntime().get(CommonUtil.RUNTIME_OUTPUT_DIR));
        env.put("CWLEXEC_JOB_RESREQ", findResreq(instance.getCommands()));
        if (logger.isDebugEnabled()) {
            logger.debug("post-failure-script={} timeout={} retry={}", pfs.getScript(), pfs.getTimeout(),
                    pfs.getRetry());
            env.forEach((k, v) -> logger.debug(k + "=" + v));
        }
        for (int retry = 1; retry <= pfs.getRetry(); retry++) {
            env.put("CWLEXEC_RETRY_NUM", String.valueOf(retry));
            logger.info(ResourceLoader.getMessage("cwl.exec.job.retry", retry, pfs.getRetry()));
            CommandExecutionResult result = CommandExecutor.run(Arrays.asList(pfs.getScript()), env,
                    pfs.getTimeout());
            if (result.getExitCode() == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("command stdout: {}", result.getOutMsg());
                }
                List<String> bwait = buildSingleBwaitCommand(instance);
                logger.info(ResourceLoader.getMessage(JOB_START_WAIT_MSG,
                        CWLExecUtil.asPrettyCommandStr(bwait)));
                CommandExecutionResult waitResult = CommandExecutor.run(bwait);
                if (waitResult.getExitCode() != 0) {
                    continue;
                } else {
                    logger.info(ResourceLoader.getMessage("cwl.exec.job.recovery.success"));
                    return true;
                }
            } else {
                logger.error(ResourceLoader.getMessage("cwl.exec.job.postscript.exited", pfs.getScript(),
                        result.getExitCode(), result.getErrMsg()));
                return false;
            }
        }
        return false;
    }

    private String findResreq(List<String> commands) {
        int index = commands.indexOf("-R");
        if (index != -1 && commands.size() > index) {
            return commands.get(index + 1);
        }
        return "";
    }

    private List<CWLCommandInstance> findAllDependencyInstances(LSFWorkflowStepRunner step) {
        List<CWLCommandInstance> instances = new ArrayList<>();
        List<String> expectDependencies = step.getExpectDependencies();
        for (String dependencyId : expectDependencies) {
            CWLWorkflowInstance wokflowInstance = (CWLWorkflowInstance) step.getMain().getInstance();
            for (CWLInstance instance : wokflowInstance.getInstances()) {
                if (instance instanceof CWLCommandInstance && dependencyId.equals(instance.getId())) {
                    instances.add((CWLCommandInstance) instance);
                }
            }
        }
        return instances;
    }

    private void captureStepOutputs(CWLCommandInstance instance) throws CWLException {
        if (this.terminated) {
            Path cwlOutputJsonFile = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR), "cwl.output.json");
            if (!cwlOutputJsonFile.toFile().exists()) {
                OutputsCapturer.captureCommandOutputs(instance);
            } else {
                logger.debug("The step ({}) has the cwl.output.json {}", instance.getName(), cwlOutputJsonFile);
            }
        } else {
            OutputsCapturer.captureCommandOutputs(instance);
        }
        if(!(instance.getProcess() instanceof ExpressionTool)) {
	        logger.info(ResourceLoader.getMessage("cwl.exec.job.done",
	                instance.getName(),
	                String.valueOf(instance.getHPCJobId()),
	                IOUtil.readLSFOutputFile(Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR),
	                        String.format("%d_out", instance.getHPCJobId())))));
        }
        instance.setState(CWLInstanceState.DONE);
        instance.setEndTime(new Date().getTime());
        persistenceService.updateCWLProcessInstance(instance);
        step.getMain().broadcast(new LSFJobEvent(LSFJobEventType.DONE, instance.getId(), instance.getName()));
    }

    private void createScatterResultGatherStep(CWLCommandInstance instance) throws CWLException {
        // Scatter a single job to scatter jobs and submit them
        CWLExecUtil.printScatterTip(instance);
        int scatterIndex = 1;
        for (CWLScatterHolder scatterHolder : instance.getScatterHolders()) {
            List<String> scatterCommands = scatterHolder.getCommand();
            String history = ResourceLoader.getMessage("cwl.exec.scatter.job.start", instance.getName(),
                    scatterIndex, CWLExecUtil.asPrettyCommandStr(scatterCommands));
            logger.info(history);
            scatterIndex = scatterIndex + 1;
        }
        List<CommandExecutionResult> resultList = CommandExecutor.runScatter(instance.getScatterHolders());
        List<String> waitJobs = new ArrayList<>();
        scatterIndex = 1;
        for (CommandExecutionResult result : resultList) {
            if (result.getExitCode() == 0) {
                logger.info(ResourceLoader.getMessage("cwl.exec.scatter.job.submitted", instance.getName(),
                        scatterIndex, result.getOutMsg()));
                String jobId = CWLExecUtil.matchJobId("Job <(\\d+)>.*", result.getOutMsg());
                waitJobs.add(String.format("done(%s)", jobId));
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CMD_FAILED_MSG, result.getCommands(), result.getErrMsg()),
                        255);
            }
            scatterIndex = scatterIndex + 1;
        }
        // Start to wait scatter jobs and gather the wait result
        int waitCode = 0;
        List<CommandExecutionResult> waitResults = CommandExecutor
                .runScatter(LSFCommandUtil.pageWaitCommands(waitJobs));
        for (CommandExecutionResult result : waitResults) {
            if (result.getExitCode() != 0) {
                waitCode = result.getExitCode();
                break;
            }
        }
        // Fill out the wait result
        List<String> commands = LSFCommandUtil.buildScatterWaitJobCommmand(instance, waitCode);
        instance.setCommands(commands);
        Path placeholder = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR), instance.getName());
        logger.info(
                ResourceLoader.getMessage("cwl.exec.scatter.job.fill.command", placeholder));
        IOUtil.createCommandScript(placeholder, commands.get(commands.size() - 1));
    }

    private List<String> fillOutPlaceholderStep(CWLCommandInstance instance) throws CWLException {
        WorkflowStep instStep = instance.getStep();
        List<WorkflowStepInput> in = instStep.getIn();
        for (WorkflowStepInput stepInput : in) {
            CWLStepBindingResolver.resolveStepInput(instance, instStep, stepInput);
        }
        CommandLineTool commandLineTool = (CommandLineTool) instStep.getRun();
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(instance, InlineJavascriptRequirement.class);
        Map<String, String> runtime = instance.getRuntime();
        List<CommandInputParameter> inputs = commandLineTool.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        // print the value of inputs
        for (CommandInputParameter parameter : inputs) {
            Object value = parameter.getValue();
            if (value == null) {
                value = parameter.getDefaultValue();
            }
            if (value != null && value instanceof CWLFile) {
                value = ((CWLFile) value).getPath();
            }
            logger.debug("Resolve input ({}) of step ({}) to <{}>", parameter.getId(), instance.getName(), value);
        }
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStdout());
        instance.setReadyToRun(true);
        List<String> commands = runtimeService.buildRuntimeCommand(instance);
        Path placeholder = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR),
                instance.getName().replace("/", "_"));
        if (!(commandLineTool instanceof ExpressionTool)) {
            logger.info(ResourceLoader.getMessage("cwl.exec.job.fill.command", commands.get(commands.size() - 1),
                    placeholder));
        }
        IOUtil.createCommandScript(placeholder, commands.get(commands.size() - 1));
        instance.setCommands(commands);
        return commands;
    }

    private void buildScatterCommands(CWLCommandInstance instance) throws CWLException {
        WorkflowStep instStep = instance.getStep();
        List<WorkflowStepInput> in = instStep.getIn();
        for (WorkflowStepInput stepInput : in) {
            CWLStepBindingResolver.resolveStepInput(instance, instStep, stepInput);
        }
        instance.setReadyToRun(true);
        runtimeService.buildRuntimeScatterCommands(instance);
    }

    private int findLSFJobExitCode(long jobId) {
        int exitCode = 255;
        CWLInstanceState state = LSFCommandUtil.findLSFJobState(jobId);
        if (state != CWLInstanceState.EXITED) {
            for (int i = 0; i < 10; i++) {
                state = LSFCommandUtil.findLSFJobState(jobId);
                if (state == CWLInstanceState.EXITED) {
                    exitCode = LSFCommandUtil.findLSFJobExitCode(jobId);
                    break;
                }
                try {
                    Thread.sleep(1000L * (i + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            exitCode = LSFCommandUtil.findLSFJobExitCode(jobId);
        }
        return exitCode;
    }

    private boolean inSuccessCodes(int exitCode, CWLCommandInstance instance) {
        boolean r = false;
        CommandLineTool tool = (CommandLineTool) instance.getProcess();
        if (tool.getSuccessCodes() != null) {
            for (int code : tool.getSuccessCodes()) {
                if (code == exitCode) {
                    r = true;
                    break;
                }
            }
        }
        return r;
    }

}

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

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.service.CWLInstanceService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLRuntimeService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLServiceFactory;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutionResult;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutor;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * A LSF bsub task, this task will bsub a step to LSF, after the step was
 * submitted, a start event will be send, this event will drive the flow
 * continue to run
 */
final class LSFBsubExecutorTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(LSFBsubExecutorTask.class);

    private final CWLInstanceService persistenceService = CWLServiceFactory.getService(CWLInstanceService.class);
    private final CWLRuntimeService runtimeService = CWLServiceFactory.getService(CWLRuntimeService.class);
    private final LSFWorkflowStepRunner step;

    protected LSFBsubExecutorTask(LSFWorkflowStepRunner step) {
        this.step = step;
    }

    @Override
    public void run() {
        try {
            runStep();
        } catch (Exception e) {
            CWLCommandInstance instance = step.getInstance();
            logger.error(
                    ResourceLoader.getMessage("cwl.exec.job.start.failed", instance.getName(), e.getMessage()));
            if (logger.isDebugEnabled()) {
                logger.error("The exception stacks:", e);
            }
            LSFJobEvent event = new LSFJobEvent(LSFJobEventType.EXIT, instance.getId(), instance.getName());
            event.setExitCode(255);
            step.getMain().broadcast(event);
        }
    }

    private void runStep() throws CWLException {
        CWLCommandInstance instance = step.getInstance();
        List<String> bsub = null;
        if (instance.isReadyToRun()) {
            if (instance.getScatter() != null) {
                // For ready scatter step, we scatter it directly, after the
                // scatter jobs are done, we bsub a
                // placeholder job (just exit the scatter exit code) to
                // represent the scatter step
                LSFReadyScatteJobExecutor.getExecutor().submit(new LSFReadyScatterJobExecutorTask(step, instance));
                return;
            } else {
                bsub = instance.getCommands();
                logger.info(ResourceLoader.getMessage("cwl.exec.job.start",
                        instance.getName(),
                        CWLExecUtil.asPrettyCommandStr(bsub)));
            }
        } else {
            bsub = runtimeService.buildRuntimeCommand(instance);
            logger.info(ResourceLoader.getMessage("cwl.exec.job.prestart",
                    instance.getName(),
                    CWLExecUtil.asPrettyCommandStr(bsub)));
        }
        CommandExecutionResult bsubResult = CommandExecutor.run(bsub);
        instance.setExecutionResult(bsubResult);
        if (bsubResult.getExitCode() == 0) {
            logger.info(ResourceLoader.getMessage("cwl.exec.job.submitted", instance.getName(),
                    bsubResult.getOutMsg()));
            String jobId = CWLExecUtil.matchJobId("Job <(\\d+)>.*", bsubResult.getOutMsg());
            instance.setHPCJobId(Long.valueOf(jobId));
            if (instance.isReadyToRun()) {
                instance.setState(CWLInstanceState.RUNNING);
                instance.setStartTime(new Date().getTime());
            }
            persistenceService.updateCWLProcessInstance(instance);
            if (instance.isReadyToRun()) {
                logger.debug("start the ready step ({})", step.getInstance().getName());
                step.getMain().broadcast(new LSFJobEvent(LSFJobEventType.START, instance.getId(), instance.getName()));
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.exec.command.failed",
                            bsub,
                            bsubResult.getErrMsg()),
                    255);
        }
    }
}

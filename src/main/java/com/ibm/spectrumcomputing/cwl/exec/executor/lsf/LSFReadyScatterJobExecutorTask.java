/*
 * Copyright 2002-2012 the original author or authors.
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

import com.ibm.spectrumcomputing.cwl.exec.service.CWLInstanceService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLServiceFactory;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutionResult;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutor;
import com.ibm.spectrumcomputing.cwl.exec.util.command.LSFCommandUtil;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/*
 * A ready scatter job task, this task will scatter the job firstly, then bsub a
 * placeholder job for this scatter job
 */
class LSFReadyScatterJobExecutorTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(LSFReadyScatterJobExecutorTask.class);

    private final CWLInstanceService persistenceService = CWLServiceFactory.getService(CWLInstanceService.class);
    private final LSFWorkflowStepRunner step;
    private final CWLCommandInstance instance;

    protected LSFReadyScatterJobExecutorTask(LSFWorkflowStepRunner step, CWLCommandInstance instance) {
        this.step = step;
        this.instance = instance;
    }

    @Override
    public void run() {
        try {
            runReadyScatterStep();
        } catch (Exception e) {
            logger.error(ResourceLoader.getMessage("cwl.exec.scatter.job.start.failed", instance.getName(),
                    e.getMessage()));
            if (logger.isDebugEnabled()) {
                logger.error("The exception stacks:", e);
            }
            LSFJobEvent event = new LSFJobEvent(LSFJobEventType.EXIT, instance.getId(), instance.getName());
            event.setExitCode(255);
            step.getMain().broadcast(event);
        }
    }

    private void runReadyScatterStep() throws CWLException {
        List<String> bsub = createScatterResultGatherJob(instance);
        logger.info(ResourceLoader.getMessage("cwl.exec.scatter.gather.job.start", instance.getName()));
        CommandExecutionResult bsubResult = CommandExecutor.run(bsub);
        instance.setExecutionResult(bsubResult);
        if (bsubResult.getExitCode() == 0) {
            logger.info(ResourceLoader.getMessage("cwl.exec.scatter.gather.job.submitted",
                    instance.getName(), bsubResult.getOutMsg()));
            String jobId = CWLExecUtil.matchJobId("Job <(\\d+)>.*", bsubResult.getOutMsg());
            instance.setHPCJobId(Long.valueOf(jobId));
            if (instance.isReadyToRun()) {
                instance.setState(CWLInstanceState.RUNNING);
                instance.setStartTime(new Date().getTime());
            }
            persistenceService.updateCWLProcessInstance(instance);
            if (instance.isReadyToRun()) {
                logger.debug("start the ready scatter step ({})", step.getInstance().getName());
                step.getMain().broadcast(new LSFJobEvent(LSFJobEventType.START, instance.getId(), instance.getName()));
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.exec.command.failed",
                            bsubResult.getCommands(),
                            bsubResult.getErrMsg()),
                    255);
        }
    }

    private List<String> createScatterResultGatherJob(CWLCommandInstance instance) throws CWLException {
        List<String> bsub = null;
        if (instance.isEmptyScatter()) {
            logger.debug("Job ({}) scattering over empty input", instance.getName());
            bsub = Arrays.asList("bsub", "exit 0");
        } else {
            // scatter the step and submit scattered jobs
            CWLExecUtil.printScatterTip(instance);
            int scatterIndex = 1;
            for (List<String> scatterCommands : instance.getScatterCommands()) {
                String history = ResourceLoader.getMessage("cwl.exec.scatter.job.start", instance.getName(),
                        scatterIndex, CWLExecUtil.asPrettyCommandStr(scatterCommands));
                logger.info(history);
                scatterIndex = scatterIndex + 1;
            }
            List<CommandExecutionResult> resultList = CommandExecutor.runScatter(instance.getScatterCommands());
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
                            ResourceLoader.getMessage("cwl.exec.command.failed",
                                    result.getCommands(),
                                    result.getErrMsg()),
                            255);
                }
                scatterIndex = scatterIndex + 1;
            }
            // start to wait scattered jobs, after waited, gather the wait
            // result and build a gather job
            int waitCode = 0;
            List<CommandExecutionResult> waitResults = CommandExecutor
                    .runScatter(LSFCommandUtil.pageWaitCommands(waitJobs));
            for (CommandExecutionResult result : waitResults) {
                if (result.getExitCode() != 0) {
                    logger.debug(ResourceLoader.getMessage("cwl.exec.job.bwait.failed", result.getExitCode(),
                            result.getErrMsg()));
                    instance.setState(CWLInstanceState.EXITED);
                    instance.setEndTime(new Date().getTime());
                    persistenceService.updateCWLProcessInstance(instance);
                    waitCode = result.getExitCode();
                    break;
                }
            }
            bsub = new ArrayList<>();
            bsub.add("bsub");
            bsub.add("exit " + waitCode);
        }
        return bsub;
    }
}

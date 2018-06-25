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
package com.ibm.spectrumcomputing.cwl.exec.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.CWLExec;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLStepBindingResolver;
import com.ibm.spectrumcomputing.cwl.exec.util.command.LSFCommandUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.outputs.OutputsCapturer;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.persistence.CWLStepProcessRecord;
import com.ibm.spectrumcomputing.cwl.model.persistence.CWLMainProcessRecord;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.WorkflowStepInput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.CWLParser;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Provides cwlexec public APIs, includes:
 * <ul>
 *  <li>Executes a CWL process</li>
 *  <li>Queries the executed CWL process records</li>
 *  <li>Reruns an exited CWL process</li>
 * </ul>
 */
public final class CWLExecService {

    private static final Logger logger = LoggerFactory.getLogger(CWLExecService.class);

    private static final String FILE_UNACCESSED_MSG = "cwl.io.file.unaccessed.with.type";

    private final CWLExec engine = CWLExec.cwlexec();
    private final CWLInstanceService persistenceService;

    protected CWLExecService(CWLInstanceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * Submits a CWL process (Workflow or CommandLineTool) to execute
     * 
     * @param owner
     *            The owner of CWL process description file
     * @param descPath
     *            The CWL process description file path
     * @param inputSettingsPath
     *            The CWL process input settings file path, if the CWL process
     *            has no input settings, it can be null
     * @param execConfPath
     *            The CWL process execution configuration file path, if the CWL
     *            process has no execution configuration, it can be null
     * @return An executed CWL process instance
     * @throws CWLException
     *             Failed to execute the CWL process
     */
    public CWLInstance submit(String owner,
            String descPath,
            String inputSettingsPath,
            String execConfPath) throws CWLException {
        if (descPath == null) {
            throw new IllegalArgumentException("The argument descriptionFilePath is required.");
        }
        String[] parts = IOUtil.splitDescFilePath(descPath);
        String descriptionPath = parts[0];
        String mainId = parts[1];
        File descriptionFile = IOUtil.yieldFile(descriptionPath, null, new String[] { ".cwl" }, false);
        CWLProcess processObj = toCWLProcess(descriptionFile, mainId);
        FlowExecConf flowExecConf = parseExecConf(execConfPath);
        processObj.setExecConfPath(execConfPath);
        loadInputSettings(processObj, inputSettingsPath);
        CWLInstance instance = persistenceService.createMainInstance(owner, processObj, flowExecConf);
        CWLExecUtil.printCWLInstanceInfo(instance);
        if (instance instanceof CWLWorkflowInstance) {
            Workflow workflow = (Workflow) instance.getProcess();
            List<WorkflowStep> steps = workflow.getSteps();
            if (steps == null || steps.isEmpty()) {
                terminateEmptyWorkflow((CWLWorkflowInstance) instance);
                return instance;
            }
        }
        engine.submit(instance);
        return instance;
    }

    /**
     * Finds all finished (done or exited) CWL process (Workflow or
     * CommandLineTool) records
     * 
     * @return The list of finished CWL process records
     */
    public List<CWLMainProcessRecord> findFinishedCWLProcesses() {
        return persistenceService.findFinishedCWLProcessRecords();
    }

    /**
     * Finds a CWL process (Workflow or CommandLineTool) record by ID
     * 
     * @param processId
     *            The ID of a flow
     * @return A CWL process record
     */
    public CWLMainProcessRecord findWorkflow(String processId) {
        return persistenceService.findCWLProcessRecord(processId);
    }

    /**
     * Finds a CWL Workflow and check it has running jobs or not
     * 
     * @param workflowId
     *            A ID of the CWL Workflow
     * @return If the CWL Workflow has running jobs return true, otherwise, return false
     */
    public boolean hasRunningJobs(String workflowId) {
        boolean hasRunningJobs = false;
        List<CWLStepProcessRecord> runningSteps = persistenceService.findStepsByState(workflowId, CWLInstanceState.RUNNING);
        for (CWLStepProcessRecord step : runningSteps) {
            // Retrieve the job actual state from LSF
            logger.debug("{}, {}, {}", workflowId, step.getId(), step.getName());
            CWLInstanceState state = LSFCommandUtil.findLSFJobState(step.getHpcJobId());
            if (state == CWLInstanceState.RUNNING) {
                hasRunningJobs = true;
                break;
            }
        }
        return hasRunningJobs;
    }

    /**
     * Reruns a CWL process (Workflow or CommandLineTool)
     * 
     * @param processId
     *            A ID of the CWL process (Workflow or CommandLineTool)
     * @return A reran CWL process instance
     * @throws CWLException
     *             Failed to rerun the CWL process instance
     */
    public CWLInstance rerun(String processId) throws CWLException {
        CWLMainProcessRecord workflowRecord = persistenceService.findCWLProcessRecord(processId);
        if (workflowRecord == null) {
            throw new CWLException(ResourceLoader.getMessage("cwl.exec.workflow.not.found", processId), 255);
        }
        if (workflowRecord.getState() != CWLInstanceState.EXITED) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.exec.workflow.not.exited", processId, workflowRecord.getState()),
                    255);
        }
        logger.info(ResourceLoader.getMessage("cwl.exec.workflow.rerun.start", workflowRecord.getName(),
                workflowRecord.getId()));
        System.setProperty(IOUtil.WORK_TOP_DIR, Paths.get(workflowRecord.getWorkDir()).getParent().toString());
        System.setProperty(IOUtil.OUTPUT_TOP_DIR, Paths.get(workflowRecord.getOutputsDir()).getParent().toString());
        CWLProcess processObj = toCWLProcess(new File(workflowRecord.getDescPath()), workflowRecord.getMainId());
        FlowExecConf flowExecConf = parseExecConf(workflowRecord.getExecConfPath());
        processObj.setExecConfPath(workflowRecord.getExecConfPath());
        loadInputSettings(processObj, workflowRecord.getInputsPath());
        CWLInstance instance = persistenceService.recoverInstance(workflowRecord, processObj, flowExecConf);
        CWLExecUtil.printCWLInstanceInfo(instance);
        engine.submit(instance);
        return instance;
    }

    private CWLProcess toCWLProcess(File descriptionFile, String mainId) throws CWLException {
        if (!descriptionFile.exists()) {
            throw new IllegalArgumentException(
                    ResourceLoader.getMessage(FILE_UNACCESSED_MSG, "description",
                            descriptionFile.getAbsolutePath()));
        }
        return CWLParser.yieldCWLProcessObject(descriptionFile, mainId, null);
    }

    private FlowExecConf parseExecConf(String execConfPath) throws CWLException {
        FlowExecConf flowExecConf = null;
        if (execConfPath != null) {
            File execConfFile = new File(execConfPath);
            if (execConfFile.exists()) {
                flowExecConf = CWLParser.parseFlowExecConf(execConfFile);
            } else {
                throw new IllegalArgumentException(
                        ResourceLoader.getMessage(FILE_UNACCESSED_MSG, "execution configuration",
                                execConfPath));
            }
        }
        return flowExecConf;
    }

    private void loadInputSettings(CWLProcess processObj, String inputSettingsPath) throws CWLException {
        if (inputSettingsPath != null) {
            Path inputSettingsFilePath = Paths.get(inputSettingsPath);
            if (!inputSettingsFilePath.isAbsolute()) {
                inputSettingsFilePath = Paths.get(System.getProperty("user.dir"), inputSettingsPath);
            }
            File inputSettingsFile = inputSettingsFilePath.toFile();
            if (!inputSettingsFile.exists()) {
                throw new IllegalArgumentException(
                        ResourceLoader.getMessage(FILE_UNACCESSED_MSG, "inputsettings",
                                inputSettingsFile));
            }
            CWLParser.loadInputSettings(processObj, inputSettingsFile);
        }
    }

    private void terminateEmptyWorkflow(CWLWorkflowInstance instance) {
        instance.setStartTime(new Date().getTime());
        instance.setState(CWLInstanceState.RUNNING);
        try {
            logger.info(ResourceLoader.getMessage("cwl.exec.workflow.done", instance.getName()));
            WorkflowStep step = instance.getStep();
            if (step != null) {
                List<WorkflowStepInput> inputs = step.getIn();
                if (inputs != null) {
                    for (WorkflowStepInput stepInput : inputs) {
                        CWLStepBindingResolver.resolveStepInput(instance, step, stepInput);
                    }
                }
            }
            OutputsCapturer.copyOutputFiles(instance);
            Map<String, Object> values = new HashMap<>();
            for (CWLParameter output : instance.getProcess().getOutputs()) {
                CWLStepBindingResolver.resolveWorkflowOutput((CWLWorkflowInstance) instance,
                        (WorkflowOutputParameter) output);
                values.put(output.getId(), output.getValue());
            }
            CWLExecUtil.printStdoutMsg(CommonUtil.asPrettyJsonStr(values));
            instance.setState(CWLInstanceState.DONE);
            instance.setExitCode(0);
        } catch (CWLException e) {
            logger.error(ResourceLoader.getMessage("cwl.exec.workflow.capture.outputs", instance.getName(),
                    e.getMessage()));
            instance.setState(CWLInstanceState.EXITED);
            instance.setExitCode(254);
        }
        instance.setEndTime(new Date().getTime());
        persistenceService.updateCWLProcessInstance(instance);
        instance.setFinished(true);
    }

}

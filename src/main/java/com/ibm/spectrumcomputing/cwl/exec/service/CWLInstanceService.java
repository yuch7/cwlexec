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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLStepBindingResolver;
import com.ibm.spectrumcomputing.cwl.exec.util.DatabaseManager;
import com.ibm.spectrumcomputing.cwl.exec.util.command.LSFCommandUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.CommandStdIOEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.InputsEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.RequirementsEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.outputs.OutputsCapturer;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLExpressionInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.persistence.CWLMainProcessRecord;
import com.ibm.spectrumcomputing.cwl.model.persistence.CWLStepProcessRecord;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.InputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.model.process.tool.ExpressionTool;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Provides methods to update CWL process instance on runtime
 */
public final class CWLInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(CWLInstanceService.class);

    private final CWLRuntimeService runtimeService;
    private final DatabaseManager dbManager;

    protected CWLInstanceService(CWLRuntimeService runtimeService, DatabaseManager dbManger) {
        this.runtimeService = runtimeService;
        this.dbManager = dbManger;
    }

    /**
     * Updates a CWL process instance on runtime
     * 
     * @param instance
     *            A CWL process instance
     */
    public void updateCWLProcessInstance(CWLInstance instance) {
        if (instance != null) {
            Session session = dbManager.getSessionFactory().openSession();
            Transaction transaction = session.beginTransaction();
            String id = instance.getId();
            try {
                if (instance.isMain()) {
                    CWLMainProcessRecord workflow = session.get(CWLMainProcessRecord.class, id);
                    if (workflow != null) {
                        updateMainInstance(workflow, instance);
                        session.update(workflow);
                    }
                } else {
                    CWLStepProcessRecord step = session.get(CWLStepProcessRecord.class, id);
                    if (step != null) {
                        updateStepInstance(step, instance);
                        session.update(step);
                    }
                }
                session.flush();
                transaction.commit();
                logger.debug("update instance {} ({}) with {}.",
                        instance.getId(), instance.getName(), instance.getState());
            } catch (Exception e) {
                transaction.rollback();
                // after rollback, re-throw the exception
                throw e;
            } finally {
                session.close();
            }
        }
    }

    /*
     * Finds all CWL main process instance (Workflow or CommmandLineTool) records
     */
    protected List<CWLMainProcessRecord> findCWLProcessRecords() {
        Session session = dbManager.getSessionFactory().openSession();
        String hql = String.format("FROM %s order by submitTime desc", CWLMainProcessRecord.class.getName());
        List<CWLMainProcessRecord> records = session.createQuery(hql, CWLMainProcessRecord.class).list();
        session.close();
        return records;
    }

    /*
     * Finds all finished (exited and done) CWL main process (Workflow or
     * CommmandLineTool) instance records
     */
    protected List<CWLMainProcessRecord> findFinishedCWLProcessRecords() {
        Session session = dbManager.getSessionFactory().openSession();
        String hql = String.format("FROM %s WHERE state=4 OR state=5 order by submitTime desc",
                CWLMainProcessRecord.class.getName());
        List<CWLMainProcessRecord> records = session.createQuery(hql, CWLMainProcessRecord.class).list();
        session.close();
        return records;
    }

    /*
     * Finds a CWL main process instance (Workflow or CommmandLineTool) instance record
     * by id
     */
    protected CWLMainProcessRecord findCWLProcessRecord(String workflowId) {
        CWLMainProcessRecord r = null;
        Session session = dbManager.getSessionFactory().openSession();
        if (workflowId != null) {
            r = session.get(CWLMainProcessRecord.class, workflowId);
        }
        session.close();
        return r;
    }

    /*
     * Finds CWL step process instance records by step state
     */
    protected List<CWLStepProcessRecord> findStepsByState(String parentId, CWLInstanceState state) {
        Session session = dbManager.getSessionFactory().openSession();
        String hql = String.format("FROM %s WHERE state=:state and parentId=:parentId", CWLStepProcessRecord.class.getName());
        Query<CWLStepProcessRecord> query = session.createQuery(hql, CWLStepProcessRecord.class);
        query.setParameter("state", state);
        query.setParameter("parentId", parentId);
        List<CWLStepProcessRecord> records = query.list();
        session.close();
        return records;
    }

    /*
     * Creates a CWL main process (Workflow or CommmandLineTool) instance
     */
    protected CWLInstance createMainInstance(String owner,
            CWLProcess processObj,
            FlowExecConf flowExecConf) throws CWLException {
        Session session = dbManager.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        CWLInstance instance = null;
        try {
            CWLMainProcessRecord record = new CWLMainProcessRecord();
            record.setName(IOUtil.findFileNameRoot(processObj.getDescPath()));
            record.setOwner(owner);
            record.setDescPath(processObj.getDescPath());
            record.setInputsPath(processObj.getInputsPath());
            record.setExecConfPath(processObj.getExecConfPath());
            record.setState(CWLInstanceState.WAITING);
            record.setSubmitTime(new Date().getTime());
            record.setRuntimeEnv(CWLExecUtil.getRuntimeEnv());
            record.setMainId(processObj.getMainId());
            session.save(record);
            instance = buildMainInstance(record, processObj, flowExecConf, false);
            record.setOutputsDir(Paths.get(
                    System.getProperty(IOUtil.OUTPUT_TOP_DIR), record.getName() + "-" + record.getId()).toString());
            record.setWorkDir(Paths.get(System.getProperty(IOUtil.WORK_TOP_DIR), record.getId()).toString());
            transaction.commit();
        } catch (CWLException e) {
            transaction.rollback();
            // after rollback, re-throw the exception
            throw e;
        } finally {
            session.close();
        }
        return instance;
    }

    /*
     * Recovers a CWL main process (Workflow or CommmandLineTool) instance
     */
    protected CWLInstance recoverInstance(CWLMainProcessRecord record,
            CWLProcess processObj,
            FlowExecConf flowExecConf) throws CWLException {
        return buildMainInstance(record, processObj, flowExecConf, true);
    }

    
    private CWLStepProcessRecord findStepByName(String parentId, String stepName) {
        CWLStepProcessRecord record = null;
        Session session = dbManager.getSessionFactory().openSession();
        String hql = String.format("FROM %s WHERE name=:stepName and parentId=:parentId",
                CWLStepProcessRecord.class.getName());
        Query<CWLStepProcessRecord> query = session.createQuery(hql, CWLStepProcessRecord.class);
        query.setParameter("stepName", stepName);
        query.setParameter("parentId", parentId);
        record = query.uniqueResult();
        session.close();
        return record;
    }

    private void updateMainInstance(CWLMainProcessRecord workflowRecord, CWLInstance instance) {
        workflowRecord.setState(instance.getState());
        if (instance instanceof CWLCommandInstance) {
            workflowRecord.setHpcJobId(((CWLCommandInstance) instance).getHPCJobId());
        }
        workflowRecord.setStartTime(instance.getStartTime());
        workflowRecord.setEndTime(instance.getEndTime());
        workflowRecord.setExitCode(instance.getExitCode());
    }

    private void updateStepInstance(CWLStepProcessRecord step, CWLInstance instance) {
        step.setState(instance.getState());
        if (instance instanceof CWLCommandInstance) {
            step.setHpcJobId(((CWLCommandInstance) instance).getHPCJobId());
        }
        step.setStartTime(instance.getStartTime());
        step.setEndTime(instance.getEndTime());
        step.setExitCode(instance.getExitCode());
    }

    private CWLInstance buildMainInstance(CWLMainProcessRecord record,
            CWLProcess processObj,
            FlowExecConf flowExecConf,
            boolean recover) throws CWLException {
        CWLInstance instance = null;
        String processId = record.getId();
        String owner = record.getOwner();
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj,
                InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainResReq(jsReq, processObj);
        Map<String, String> runtime = runtimeService.prepareMainRuntime(record, processObj);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        if (processObj instanceof ExpressionTool) {
        	ExpressionTool expressionTool = (ExpressionTool)processObj;
            List<CommandInputParameter> inputs = expressionTool.getInputs();
            InputsEvaluator.eval(jsReq, runtime, inputs);
            instance = new CWLExpressionInstance(processId, owner, expressionTool, flowExecConf);
            instance.setName(record.getName());
            instance.setRuntimeEnv(record.getRuntimeEnv());
            instance.setRuntime(runtime);
            instance.setReadyToRun(true);
            ((CWLExpressionInstance)instance).setExpression(expressionTool.getExpression());
        } else if (processObj instanceof Workflow) {
            Workflow workflow = (Workflow) processObj;
            List<InputParameter> inputs = workflow.getInputs();
            InputsEvaluator.eval(jsReq, runtime, inputs);
            CWLWorkflowInstance workflowInstance = new CWLWorkflowInstance(processId, owner, workflow, flowExecConf);
            instance = workflowInstance;
            instance.setName(record.getName());
            instance.setRuntimeEnv(record.getRuntimeEnv());
            instance.setRuntime(runtime);
            instance.setReadyToRun(true);
            for (WorkflowStep step : workflow.getSteps()) {
                workflowInstance.getInstances().addAll(createStepInstances(workflowInstance, step, recover));
            }
        } else if (processObj instanceof CommandLineTool) {
            CommandLineTool commandLineTool = (CommandLineTool) processObj;
            List<CommandInputParameter> inputs = commandLineTool.getInputs();
            InputsEvaluator.eval(jsReq, runtime, inputs);
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStdin());
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStderr());
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStdout());
            instance = new CWLCommandInstance(processId, owner, commandLineTool, flowExecConf);
            instance.setName(record.getName());
            instance.setRuntimeEnv(record.getRuntimeEnv());
            instance.setRuntime(runtime);
            instance.setReadyToRun(true);
            ((CWLCommandInstance) instance).setCommands(runtimeService.buildRuntimeCommand((CWLCommandInstance) instance));
        }
        else {
            throw new UnsupportedOperationException(String.format("The process (%s) cannot be supported",
                    processObj.getClass().getName()));
        }
        return instance;
    }

    private List<CWLInstance> createStepInstances(CWLWorkflowInstance parent,
            WorkflowStep step,
            boolean recover) throws CWLException {
        List<CWLInstance> stepInstances = new ArrayList<>();
        String name = parent.getParent() == null ? step.getId() : parent.getName() + "/" + step.getId();
        if (step.getRun() instanceof Workflow) {
            Workflow subWorkflow = (Workflow) step.getRun();
            CWLWorkflowInstance subWorkflowInstance = new CWLWorkflowInstance(null, parent.getOwner(), subWorkflow,
                    parent);
            subWorkflowInstance.setName(name);
            subWorkflowInstance.setStep(step);
            Map<String, String> runtime = runtimeService.prepareStepRuntime(parent.getOwner(), parent, step);
            subWorkflowInstance.setRuntime(runtime);
            CWLStepBindingResolver.resolveStepInputs(parent, step);
            for (WorkflowStep subWorkflowStep : subWorkflow.getSteps()) {
                stepInstances.addAll(createStepInstances(subWorkflowInstance, subWorkflowStep, recover));
            }
        } else {
            boolean prepared = CWLStepBindingResolver.resolveStepInputs(parent, step);
            CWLInstance mainInstance = parent;
            while (mainInstance.getParent() != null) {
                mainInstance = mainInstance.getParent();
            }
            if (recover) {
                recoverStepInstance(stepInstances, parent, name, step, prepared);
            } else {
                addCWLStepInstance(stepInstances, parent, mainInstance, name, step, prepared);
            }
        }
        return stepInstances;
    }

    private void addCWLStepInstance(List<CWLInstance> stepInstances,
            CWLWorkflowInstance parent,
            CWLInstance mainInstance,
            String name,
            WorkflowStep step,
            boolean prepared) throws CWLException {
        Session session = dbManager.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        try {
            CWLStepProcessRecord record = new CWLStepProcessRecord();
            record.setParentId(mainInstance.getId());
            record.setName(name);
            record.setOwner(parent.getOwner());
            record.setState(CWLInstanceState.WAITING);
            record.setSubmitTime(new Date().getTime());
            record.setRuntimeEnv(CWLExecUtil.getRuntimeEnv());
            session.save(record);
            stepInstances.add(buildStepInstance(parent, record, step, prepared));
            transaction.commit();
        } catch (CWLException e) {
            transaction.rollback();
            // re-throw the CWL exception
            throw e;
        } finally {
            session.close();
        }
    }

    private void recoverStepInstance(List<CWLInstance> stepInstances,
            CWLWorkflowInstance parent,
            String name,
            WorkflowStep step,
            boolean prepared) throws CWLException {
        CWLInstance mainInstance = parent;
        while(mainInstance.getParent() != null) {
            mainInstance = mainInstance.getParent();
        }
        CWLStepProcessRecord record = findStepByName(mainInstance.getId(), name);
        CWLInstance stepInstance = buildStepInstance(parent, record, step, prepared);
        stepInstances.add(stepInstance);
        if (stepInstance instanceof CWLCommandInstance) {
            if (record.getState() == CWLInstanceState.DONE) {
                OutputsCapturer.captureCommandOutputs((CWLCommandInstance) stepInstance);
                stepInstance.setState(CWLInstanceState.DONE);
            } else if (record.getState() == CWLInstanceState.EXITED) {
                stepInstance.setState(CWLInstanceState.WAITING);
            } else if (record.getState() != CWLInstanceState.WAITING) {
                CWLInstanceState state = LSFCommandUtil.findLSFJobState(record.getHpcJobId());
                logger.debug("Query the step ({}) state from LSF (current={}, lsf={})",
                        record.getName(), record.getState(), state);
                if (state == CWLInstanceState.DONE) {
                    try {
                        OutputsCapturer.captureCommandOutputs((CWLCommandInstance) stepInstance);
                        stepInstance.setState(CWLInstanceState.DONE);
                        //Only persist done step, for other cases, just modify the runtime state
                        updateCWLProcessInstance(stepInstance);
                    } catch (CWLException e) {
                        //The step is done, but cannot capture outputs
                        logger.debug("Failed to capture the output for done step ({})", record.getName());
                        stepInstance.setState(CWLInstanceState.WAITING);
                    }
                } else if (state == CWLInstanceState.EXITED) {
                    stepInstance.setState(CWLInstanceState.WAITING);
                } else {
                    long jobId = record.getHpcJobId();
                    logger.warn(ResourceLoader.getMessage("cwl.exec.workflow.rerun.kill.job",
                            record.getName(), String.valueOf(jobId)));
                    LSFCommandUtil.killJobs(Arrays.asList(jobId));
                }
            }
        }
        logger.debug("recover step: {}, state={}, prepared={}", stepInstance.getName(), stepInstance.getState(), prepared);
    }

    private CWLInstance buildStepInstance(CWLWorkflowInstance parent,
            CWLStepProcessRecord record,
            WorkflowStep step,
            boolean prepared) throws CWLException {
        CWLInstance instance = null;
        String processId = record.getId();
        String owner = record.getOwner();
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(step,
                InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalStepResReq(jsReq, step);
        Map<String, String> runtime = runtimeService.prepareStepRuntime(owner, parent, step);
        RequirementsEvaluator.evalStepEnvVarReq(jsReq, runtime, step);
        if (step.getRun() instanceof CommandLineTool) {
            CommandLineTool commandLineTool = (CommandLineTool) step.getRun();
            //For scatter step, postpone the evaluation phase until building command
            if (prepared && step.getScatter() == null) {
                List<CommandInputParameter> inputs = commandLineTool.getInputs();
                InputsEvaluator.eval(jsReq, runtime, inputs);
                CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStdin());
                CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStderr());
                CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStdout());
            }
            instance = new CWLCommandInstance(processId, owner, commandLineTool, parent);
            instance.setName(record.getName());
            instance.setRuntimeEnv(record.getRuntimeEnv());
            instance.setRuntime(runtime);
            ((CWLCommandInstance) instance).setScatter(step.getScatter());
            ((CWLCommandInstance) instance).setScatterMethod(step.getScatterMethod());
            instance.setStep(step);
            ((CWLCommandInstance) instance).setMain(false);
            if (prepared) {
                instance.setReadyToRun(true);
                if (((CWLCommandInstance) instance).getScatter() != null) {
                    ((CWLCommandInstance) instance).setScatterHolders(new ArrayList<>());
                    runtimeService.buildRuntimeScatterCommands((CWLCommandInstance) instance);
                } else {
                    ((CWLCommandInstance) instance).setCommands(
                            runtimeService.buildRuntimeCommand((CWLCommandInstance) instance));
                }
            }
        } else {
            throw new UnsupportedOperationException(String.format("The process (%s) cannot be supported",
                    step.getClass().getName()));
        }
        return instance;
    }
}

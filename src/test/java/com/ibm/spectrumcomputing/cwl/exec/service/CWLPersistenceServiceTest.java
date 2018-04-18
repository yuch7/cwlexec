package com.ibm.spectrumcomputing.cwl.exec.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.spectrumcomputing.cwl.exec.executor.CWLInstanceSchedulerTask;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLInstanceService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLRuntimeService;
import com.ibm.spectrumcomputing.cwl.exec.util.DatabaseManager;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandUtil;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.persistence.CWLMainProcessRecord;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.parser.CWLParser;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

public class CWLPersistenceServiceTest extends CWLExecTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CWLInstanceSchedulerTask.class);

    private CWLInstanceService persistenceService;

    @Before
    public void beforeTest() throws CWLException{
        if (!is_win) {
            CWLRuntimeService runtimeService = mock(CWLRuntimeService.class);
            Map<String, String> runtime = new HashMap<String, String>();
            runtime.put(CommonUtil.RUNTIME_OUTPUT_DIR, "test_outputs");
            runtime.put("outdirSize", "10240");
            runtime.put(CommonUtil.RUNTIME_TMP_DIR, "test_workdir");
            runtime.put("tmpdirSize", "20480");
            given(runtimeService.prepareMainRuntime(any(), any())).willReturn(runtime);
            DatabaseManager hibernateHelper = new DatabaseManager(testDatabaseConfig());
            persistenceService = new CWLInstanceService(runtimeService, hibernateHelper);
        }
    }

    @Test
    public void createMainInstance() throws CWLException {
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#createMainInstance is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "compile.cwl"), "main");
        CWLInstance instance1 = persistenceService.createMainInstance(owner, processObj, null);
        assertNotNull(instance1);
        assertEquals(3, ((CWLWorkflowInstance)instance1).getInstances().size());
        int readyInstances = 0;
        for (CWLInstance step : ((CWLWorkflowInstance)instance1).getInstances()) {
            if (step.isReadyToRun()) {
                readyInstances = readyInstances + 1;
            }
        }
        assertEquals(2, readyInstances);
        CWLInstance instance2 = persistenceService.createMainInstance(owner, processObj, null);
        assertNotNull(instance2);
        List<CWLMainProcessRecord> records = persistenceService.findCWLProcessRecords();
        assertEquals(2, records.size());
        
        for (CWLMainProcessRecord record : records) {
            assertEquals(CWLInstanceState.WAITING, record.getState());
        }
        
    }

    @Test
    public void createMainInstanceWithSubWorkflow() throws CWLException {
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#createMainInstanceWithSubWorkflow is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "3rd-workflow.cwl"), "main");
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "inputs/3rd-workflow.yml"));
        CWLInstance instance = persistenceService.createMainInstance(owner, processObj, null);
        assertNotNull(instance);
        assertEquals(true, instance instanceof CWLWorkflowInstance);
        assertEquals(7, ((CWLWorkflowInstance)instance).getInstances().size());
        int readyInstances = 0;
        for (CWLInstance step : ((CWLWorkflowInstance)instance).getInstances()) {
            if (step.isReadyToRun()) {
                readyInstances = readyInstances + 1;
            }
        }
        assertEquals(1, readyInstances);
    }

    @Test
    public void updateProcessState() throws CWLException {
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#updateProcessState is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "compile.cwl"), "main");
        CWLInstance instance = persistenceService.createMainInstance(owner, processObj, null);
        long start = new Date().getTime();
        instance.setStartTime(start);
        instance.setState(CWLInstanceState.PENDING);
        persistenceService.updateCWLProcessInstance(instance);
        List<CWLMainProcessRecord> records = persistenceService.findCWLProcessRecords();
        assertEquals(1, records.size());
        assertEquals(CWLInstanceState.PENDING, records.get(0).getState());
        assertEquals(start, records.get(0).getStartTime().longValue());
        assertNull(records.get(0).getEndTime());
        instance.setState(CWLInstanceState.RUNNING);
        persistenceService.updateCWLProcessInstance(instance);
        records = persistenceService.findCWLProcessRecords();
        assertEquals(1, records.size());
        assertEquals(start, records.get(0).getStartTime().longValue());
        assertNull(records.get(0).getEndTime());
        assertEquals(CWLInstanceState.RUNNING, records.get(0).getState());
        long end = new Date().getTime();
        instance.setEndTime(end);
        instance.setState(CWLInstanceState.DONE);
        persistenceService.updateCWLProcessInstance(instance);
        records = persistenceService.findCWLProcessRecords();
        assertEquals(1, records.size());
        assertEquals(CWLInstanceState.DONE, records.get(0).getState());
        assertEquals(start, records.get(0).getStartTime().longValue());
        assertEquals(end, records.get(0).getEndTime().longValue());
    }

    @Test
    public void findAllProcessInstances() throws CWLException{
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#findAllProcessInstances is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "compile.cwl"), "main");
        CWLInstance instance = persistenceService.createMainInstance(owner, processObj, null);
        long start = new Date().getTime();
        instance.setStartTime(start);
        instance.setState(CWLInstanceState.DONE);
        persistenceService.updateCWLProcessInstance(instance);
        //2rd one
        Workflow processObj2 = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "compile.cwl"), "main");
        CWLInstance instance2 = persistenceService.createMainInstance(owner, processObj2, null);
        Assert.assertEquals(CWLInstanceState.WAITING, instance2.getState());
        //3rd one
        Workflow processObj3 = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "1st-workflow.cwl"), "main");
        CWLParser.loadInputSettings(processObj3, new File(DEF_ROOT_PATH + "inputs/1st-workflow.yml"));
        CWLInstance instance3 = persistenceService.createMainInstance(owner, processObj3, null);
        instance3.setStartTime(start);
        instance3.setState(CWLInstanceState.EXITED);
        persistenceService.updateCWLProcessInstance(instance3);
        //get all of the work flow records which its state is done or exited
        List<CWLMainProcessRecord> records = persistenceService.findFinishedCWLProcessRecords();
        Assert.assertNotNull(records);
        Assert.assertEquals(2, records.size());
        CWLMainProcessRecord wfRecord = persistenceService.findCWLProcessRecord(instance3.getId());
        Assert.assertNotNull(wfRecord);
        Assert.assertEquals(instance3.getId(), wfRecord.getId());
    }

    @Test
    public void evaluateStepInValueFrom() throws CWLException {
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#evaluateStepInValueFrom is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "step_in_valueFrom/workflow.cwl"));
        CWLWorkflowInstance instance = (CWLWorkflowInstance) persistenceService.createMainInstance(owner, processObj, null);
        assertEquals(3, instance.getInstances().size());
        CWLInstance step1 = findInstance(instance, "step1");
        CWLParameter step1Msg = findParameter("msg", step1.getProcess().getInputs());
        assertEquals("three", step1Msg.getValue());
        CWLInstance step2 = findInstance(instance, "step2");
        CWLParameter step2Msg = findParameter("msg", step2.getProcess().getInputs());
        assertEquals("one", step2Msg.getValue());
        CWLInstance step3 = findInstance(instance, "step3");
        CWLParameter step3Msg = findParameter("msg", step3.getProcess().getInputs());
        assertEquals("other_two", step3Msg.getValue());
    }

    @Test
    public void createScatterDot() throws CWLException {
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#createScatterDot is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "scatter-job-dot.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "scatter-inp2.json"));
        CWLWorkflowInstance instance = (CWLWorkflowInstance) persistenceService.createMainInstance(owner, processObj, null);
        CWLCommandInstance stepInstance = (CWLCommandInstance) instance.getInstances().get(0);
        stepInstance.setRuntime((instance.getRuntime()));
        List<List<String>> commands = CommandUtil.buildScatterCommand((CWLCommandInstance) instance.getInstances().get(0));
        assertEquals(2, commands.size());
        assertTrue(String.join(" ", commands.get(0)).startsWith("echo -n foo one three"));
        assertTrue(String.join(" ", commands.get(1)).startsWith("echo -n foo two four"));
    }

    @Test
    public void createScatterNested() throws CWLException {
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#createScatterNested is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "scatter-job-nested.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "scatter-inp2.json"));
        CWLWorkflowInstance instance = (CWLWorkflowInstance) persistenceService.createMainInstance(owner, processObj, null);
        CWLCommandInstance stepInstance = (CWLCommandInstance) instance.getInstances().get(0);
        stepInstance.setRuntime((instance.getRuntime()));
        List<List<String>> commands = CommandUtil.buildScatterCommand((CWLCommandInstance) instance.getInstances().get(0));
        assertEquals(4, commands.size());
        assertTrue(String.join(" ", commands.get(0)).startsWith("echo -n foo one three"));
        assertTrue(String.join(" ", commands.get(1)).startsWith("echo -n foo one four"));
        assertTrue(String.join(" ", commands.get(2)).startsWith("echo -n foo two three"));
        assertTrue(String.join(" ", commands.get(3)).startsWith("echo -n foo two four"));
    }

    @Test
    public void createScatterValuefromWF1() throws CWLException {
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#createScatterValuefromWF1 is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "scatter-valuefrom-wf1.cwl"));
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "scatter-valuefrom-job1.json"));
        CWLWorkflowInstance instance = (CWLWorkflowInstance) persistenceService.createMainInstance(owner, processObj, null);
        assertNotNull(instance);
    }

    @Test
    public void createScatterValuefromWF5() throws CWLException {
        if (is_win) {
            logger.warn("CWLPersistenceServiceTest#createScatterValuefromWF5 is unsupported on Windows.");
            return;
        }
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "scatter-valuefrom-wf5.cwl"));
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "scatter-valuefrom-job1.json"));
        CWLWorkflowInstance instance = (CWLWorkflowInstance) persistenceService.createMainInstance(owner, processObj, null);
        assertNotNull(instance);
    }

    private CWLInstance findInstance(CWLWorkflowInstance instance, String name) {
        for (CWLInstance subInstance : instance.getInstances()) {
            if (name.equals(subInstance.getName())) {
                return subInstance;
            }
        }
        return null;
    }
}

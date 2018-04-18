package com.ibm.spectrumcomputing.cwl.exec.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecConfUtil;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.parser.CWLParser;

public class CWLExecConfUtilTest extends CWLExecTestBase {

    @Test
    public void parseWorkflowConf() throws CWLException {
        FlowExecConf flowExecConf = CWLParser.parseFlowExecConf(new File(DEF_ROOT_PATH + "config/flowConfig.json"));
        assertNull(CWLExecConfUtil.getApp(flowExecConf, "test"));
        assertEquals("testQ", CWLExecConfUtil.getQueue(flowExecConf, "test"));
        assertEquals("testPro", CWLExecConfUtil.getProject(flowExecConf, "test"));
        assertTrue(CWLExecConfUtil.isRerunnable(flowExecConf, "test"));
    }

    @Test
    public void parseStepConf() throws CWLException {
        FlowExecConf flowExecConf = CWLParser.parseFlowExecConf(new File(DEF_ROOT_PATH + "config/stepConfig.json"));
        assertEquals("flow_app", CWLExecConfUtil.getApp(flowExecConf, "test"));
        assertEquals("testQQ", CWLExecConfUtil.getQueue(flowExecConf, "test"));
        assertEquals("testPP", CWLExecConfUtil.getProject(flowExecConf, "test"));
        assertEquals("flow_res", CWLExecConfUtil.getResource(flowExecConf, "test"));
        assertFalse(CWLExecConfUtil.isRerunnable(flowExecConf, "test"));
        assertEquals("app", CWLExecConfUtil.getApp(flowExecConf, "step1"));
        assertEquals("test", CWLExecConfUtil.getQueue(flowExecConf, "step1"));
        assertEquals("testProject", CWLExecConfUtil.getProject(flowExecConf, "step1"));
        assertEquals("docker", CWLExecConfUtil.getResource(flowExecConf, "step1"));
        assertTrue(CWLExecConfUtil.isRerunnable(flowExecConf, "step1"));
    }

    @Test
    public void parseWorkFlowPfscript() throws CWLException {
        FlowExecConf flowExecConf = CWLParser.parseFlowExecConf(new File(DEF_ROOT_PATH + "config/flowConfig.json"));
        assertEquals("/path/to/recoverscript.sh",
                CWLExecConfUtil.getPostFailureScript(flowExecConf, "test").getScript());
        assertEquals(10, CWLExecConfUtil.getPostFailureScript(flowExecConf, "test").getTimeout());
        assertEquals(2, CWLExecConfUtil.getPostFailureScript(flowExecConf, "test").getRetry());
    }

    @Test
    public void parseStepPfscript() throws CWLException {
        FlowExecConf flowExecConf = CWLParser.parseFlowExecConf(new File(DEF_ROOT_PATH + "config/stepConfig.json"));
        assertEquals("/path/to/recoverscript.sh",
                CWLExecConfUtil.getPostFailureScript(flowExecConf, "step1").getScript());
        assertEquals(5, CWLExecConfUtil.getPostFailureScript(flowExecConf, "step1").getTimeout());
        assertEquals(1, CWLExecConfUtil.getPostFailureScript(flowExecConf, "step1").getRetry());
    }
}

package com.ibm.spectrumcomputing.cwl.exec.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ResourceRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ShellCommandRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;

public class CWLEngineUtilTest extends CWLExecTestBase {

    @Test
    public void matchLSFJobId() {
        String id = CWLExecUtil.matchJobId("Job <(\\d+)>.*", "Job <aaaa>");
        assertNull(id);
        id = CWLExecUtil.matchJobId("Job <(\\d+)>.*", "Job <585> is submitted to default queue <normal>.");
        assertEquals("585", id);
    }

    @Test
    public void findRequirement() {
        CommandLineTool commandLineTool = new CommandLineTool(new ArrayList<>(), new ArrayList<>());
        commandLineTool.setRequirements(Arrays.asList(new ResourceRequirement(), new ShellCommandRequirement()));
        ResourceRequirement resReq = CWLExecUtil.findRequirement(commandLineTool, ResourceRequirement.class);
        assertNotNull(resReq);
        ShellCommandRequirement shellReq = CWLExecUtil.findRequirement(commandLineTool, ShellCommandRequirement.class);
        assertNotNull(shellReq);
    }

    @Test
    public void getRuntimeEnv() {
        assertEquals(RuntimeEnv.LSF, CWLExecUtil.getRuntimeEnv());
    }
}

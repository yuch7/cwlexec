package com.ibm.spectrumcomputing.cwl.parser;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Dirent;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InitialWorkDirRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;

public class WorkDirParserTest extends CWLExecTestBase {

    @Test
    public void parseWorkDirWithDirent() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "createfile.cwl"));
        List<Requirement> requirements = processObj.getRequirements();
        InitialWorkDirRequirement initialWorkDirRequirement = (InitialWorkDirRequirement) requirements.get(0);
        assertEquals(1, initialWorkDirRequirement.getDirentListing().size());
        Dirent cwlDirent = initialWorkDirRequirement.getDirentListing().get(0);
        assertEquals("example.conf", cwlDirent.getEntryname().getValue());
        assertEquals("CONFIGVAR=$(inputs.message)", cwlDirent.getEntry().getValue().trim());
        assertEquals(true, cwlDirent.isWritable());
    }

    @Test
    public void parseWorkDirWithListing() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "linkfile.cwl"));
        List<Requirement> requirements = processObj.getRequirements();
        InitialWorkDirRequirement initialWorkDirRequirement = (InitialWorkDirRequirement) requirements.get(1);
        assertEquals(3, initialWorkDirRequirement.getExprListing().size());
    }

    @Test
    public void parseWorkDirWithFile() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "workdir_listing_file.cwl"));
        List<Requirement> requirements = processObj.getRequirements();
        InitialWorkDirRequirement initialWorkDirRequirement = (InitialWorkDirRequirement) requirements.get(0);
        assertEquals(2, initialWorkDirRequirement.getFileListing().size());
        CWLFile c = (CWLFile) initialWorkDirRequirement.getFileListing().get(0);
        assertEquals("SRR1031972.bedGraph", c.getBasename());
    }

    @Test
    public void parseWorkDirWithDir() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "workdir_listing_file.cwl"));
        List<Requirement> requirements = processObj.getRequirements();
        InitialWorkDirRequirement initialWorkDirRequirement = (InitialWorkDirRequirement) requirements.get(0);
        assertEquals(1, initialWorkDirRequirement.getDirListing().size());
        CWLDirectory c = (CWLDirectory) initialWorkDirRequirement.getDirListing().get(0);
        assertEquals("files", c.getBasename());
    }
}

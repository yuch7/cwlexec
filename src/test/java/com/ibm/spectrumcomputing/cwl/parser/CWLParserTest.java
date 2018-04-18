package com.ibm.spectrumcomputing.cwl.parser;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.CWLVersion;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullValue;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvVarRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvironmentDef;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ResourceRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.parser.CWLParser;

public class CWLParserTest extends CWLExecTestBase {

    @Test
    public void yieldCWLProcessObjWithoutDefFile() throws CWLException {
        File none = new File("none");
        thrown.expect(CWLException.class);
        thrown.expectMessage("Failed to");
        CWLParser.yieldCWLProcessObject(none);
    }

    @Test
    public void yieldCWLProcessObjWithBadFile() throws CWLException {
        String cwlFilePathName = DEF_ROOT_PATH + "cwl_bad.json";
        File bad = new File(cwlFilePathName);
        thrown.expect(CWLException.class);
        thrown.expectMessage("Failed to process CWL description file");
        CWLParser.yieldCWLProcessObject(bad);
    }

    @Test
    public void yieldCWLProcessObjWithoutClass() throws CWLException {
        String cwlFilePathName = DEF_ROOT_PATH + "cwl_no_class.cwl";
        File noClass = new File(cwlFilePathName);
        thrown.expect(CWLException.class);
        thrown.expectMessage("[class]");
        CWLParser.yieldCWLProcessObject(noClass);
    }

    @Test
    public void yieldCWLProcessObjWithInvalidClass() throws CWLException {
        String cwlFilePathName = DEF_ROOT_PATH + "cwl_invalid_class.cwl";
        File invalidClass = new File(cwlFilePathName);
        thrown.expect(CWLException.class);
        CWLParser.yieldCWLProcessObject(invalidClass);
    }

    @Test
    public void yieldCWLProcessObjWithUnsupportVersion() throws CWLException {
        String cwlFilePathName = DEF_ROOT_PATH + "cwl_unsupported_version.cwl";
        File unsupportedClass = new File(cwlFilePathName);
        thrown.expect(CWLException.class);
        thrown.expectMessage("v2.0");
        CWLParser.yieldCWLProcessObject(unsupportedClass);
    }

    @Test
    public void parseRequirements() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "requirements.cwl"));
        List<Requirement> requirements = processObj.getRequirements();
        List<Requirement> hints =  processObj.getHints();
        assertEquals(6, requirements.size());
        InlineJavascriptRequirement inlineJavascriptRequirement = (InlineJavascriptRequirement) requirements.get(0);
        assertNull(inlineJavascriptRequirement.getExpressionLib());
        EnvVarRequirement envVarRequirement = (EnvVarRequirement) requirements.get(1);
        List<EnvironmentDef> environmentDefs = envVarRequirement.getEnvDef();
        assertEquals(3, environmentDefs.size());
        assertEquals("AL_USE_CONCATENATED_GENOME", environmentDefs.get(0).getEnvName());
        assertEquals("$(inputs.CONCATENATED_GENOME?\"1\":\"0\")", environmentDefs.get(0).getEnvValue().getExpression());
        assertEquals("AL_BWA_ALN_PARAMS", environmentDefs.get(1).getEnvName());
        assertEquals("-k 0 -n 0 -t 4", environmentDefs.get(1).getEnvValue().getValue());
        DockerRequirement dockerRequirement = (DockerRequirement) requirements.get(2);
        assertEquals("debian:stretch-slim", dockerRequirement.getDockerPull());
        assertNotNull(dockerRequirement.getDockerFile());
        assertTrue(dockerRequirement.getDockerFile().startsWith("FROM debian:stretch-slim"));
        ResourceRequirement resourceRequirement = (ResourceRequirement) requirements.get(4);
        assertEquals(8L, resourceRequirement.getCoresMin().longValue());
        assertEquals(10240L, resourceRequirement.getRamMin().longValue());
        assertEquals(512000L, resourceRequirement.getOutdirMin().longValue());
        assertEquals(1, hints.size());
        inlineJavascriptRequirement = (InlineJavascriptRequirement) hints.get(0);
        assertEquals(1, inlineJavascriptRequirement.getExpressionLib().size());
    }

    @Test
    public void parseInputBasicTypes() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "input-basic-types.cwl"));
        List<? extends CWLParameter> inputs = processObj.getInputs();
        assertEquals(9, inputs.size());
        CommandInputParameter nullType = (CommandInputParameter)findParameter("nullType", inputs);
        assertEquals(CWLTypeSymbol.NULL, nullType.getType().getType().getSymbol());
        assertEquals(NullValue.NULL, nullType.getDefaultValue());
        CommandInputParameter boolType = (CommandInputParameter)findParameter("boolType", inputs);
        assertEquals(false, boolType.getDefaultValue());
        CommandInputParameter intType = (CommandInputParameter)findParameter("intType", inputs);
        assertEquals(100, intType.getDefaultValue());
        CommandInputParameter longType = (CommandInputParameter)findParameter("longType", inputs);
        assertEquals(32000000000L, longType.getDefaultValue());
        CommandInputParameter floatType = (CommandInputParameter)findParameter("floatType", inputs);
        assertEquals(0.04f, floatType.getDefaultValue());
        CommandInputParameter doubleType = (CommandInputParameter)findParameter("doubleType", inputs);
        assertEquals(0.02d, doubleType.getDefaultValue());
        CommandInputParameter stringType = (CommandInputParameter)findParameter("stringType", inputs);
        assertEquals("test", stringType.getDefaultValue());
        CommandInputParameter fileType = (CommandInputParameter)findParameter("fileType", inputs);
        CWLFile cwlFile = (CWLFile) fileType.getDefaultValue();
        assertEquals("data_1.fastq.bz2", Paths.get(cwlFile.getPath()).getFileName().toString());
        CommandInputParameter dirType = (CommandInputParameter)findParameter("dirType", inputs);
        CWLDirectory dir = (CWLDirectory) dirType.getDefaultValue();
        assertEquals("files", Paths.get(dir.getPath()).getFileName().toString());
        assertEquals(12, dir.getListing().size());
    }

    @Test
    public void parseSchemaTypes() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "input-schema-types.cwl"));
        List<? extends CWLParameter> inputs = processObj.getInputs();
        assertEquals(3, inputs.size());
        CommandInputParameter dept = (CommandInputParameter)findParameter("dept", inputs);
        assertEquals(CWLTypeSymbol.ENUM, dept.getType().getType().getSymbol());
        assertEquals("-bg", dept.getDefaultValue());
        CommandInputParameter strings = (CommandInputParameter)findParameter("strings", inputs);
        assertEquals(CWLTypeSymbol.ARRAY, strings.getType().getType().getSymbol());
        assertEquals("[test1, test2]", strings.getDefaultValue().toString());
        CommandInputParameter files = (CommandInputParameter)findParameter("files", inputs);
        assertEquals(CWLTypeSymbol.ARRAY, files.getType().getType().getSymbol());
        @SuppressWarnings("rawtypes")
        List fileList = (List) files.getDefaultValue();
        assertEquals(2, fileList.size());
        CWLFile cwlFile1 = (CWLFile) fileList.get(0);
        assertEquals("data_1.fastq.bz2", Paths.get(cwlFile1.getPath()).getFileName().toString());
        CWLFile cwlFile2 = (CWLFile) fileList.get(1);
        assertEquals("data_2.fastq.bz2", Paths.get(cwlFile2.getPath()).getFileName().toString());
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void parseTwoDimensionalArray() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "input-two-dimensional-array.cwl"));
        CommandLineTool commandLineTool = ((CommandLineTool) processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) commandLineTool.getInputs();
        assertEquals(1, inputs.size());
        CommandInputParameter param = inputs.get(0);
        assertTrue(CWLTypeSymbol.ARRAY == param.getType().getType().getSymbol());
        InputArrayType arrayType = (InputArrayType) param.getType().getType();
        assertTrue(CWLTypeSymbol.ARRAY == arrayType.getItems().getType().getSymbol());
        assertTrue(param.getDefaultValue() instanceof List);
        List defaultValue = (List)param.getDefaultValue();
        assertEquals(2, defaultValue.size());
        List first = (List)defaultValue.get(0);
        InputArrayType itemType = ((InputArrayType) arrayType.getItems().getType());
        assertEquals(CWLTypeSymbol.STRING, itemType.getItems().getType().getSymbol());
        assertEquals("[key1, key2]", first.toString());
    }

    @Test
    public void parseMultiTypes() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "input-multi-types.cwl"));
        List<? extends CWLParameter> inputs = processObj.getInputs();
        assertEquals(6, inputs.size());
        CommandInputParameter var0 = (CommandInputParameter)findParameter("var0", inputs);
        assertEquals(CWLTypeSymbol.NULL, var0.getType().getType().getSymbol());
        assertEquals(NullValue.NULL, var0.getDefaultValue());
        CommandInputParameter var1 = (CommandInputParameter)findParameter("var1", inputs);
        assertEquals(CWLTypeSymbol.STRING, var1.getType().getType().getSymbol());
        assertEquals("test0", var1.getDefaultValue());
        CommandInputParameter var2 = (CommandInputParameter)findParameter("var2", inputs);
        assertEquals(CWLTypeSymbol.ARRAY, var2.getType().getType().getSymbol());
        InputArrayType arrayType = (InputArrayType) var2.getType().getType();
        assertEquals(CWLTypeSymbol.STRING, arrayType.getItems().getType().getSymbol());
        assertEquals("[test0, test1]", var2.getDefaultValue().toString());
        CommandInputParameter var3 = (CommandInputParameter)findParameter("var3", inputs);
        assertEquals(CWLTypeSymbol.NULL, var3.getType().getType().getSymbol());
        assertEquals(NullValue.NULL, var3.getDefaultValue());
        CommandInputParameter var4 = (CommandInputParameter)findParameter("var4", inputs);
        assertEquals(CWLTypeSymbol.ARRAY, var4.getType().getType().getSymbol());
        arrayType = (InputArrayType) var4.getType().getType();
        assertEquals(CWLTypeSymbol.STRING, arrayType.getItems().getType().getSymbol());
        assertEquals("[test0, test1]", var4.getDefaultValue().toString());
        CommandInputParameter var5 = (CommandInputParameter)findParameter("var5", inputs);
        assertEquals(CWLTypeSymbol.ARRAY, var5.getType().getType().getSymbol());
        arrayType = (InputArrayType) var5.getType().getType();
        assertEquals(CWLTypeSymbol.STRING, arrayType.getItems().getType().getSymbol());
        assertEquals("[test0, test1]", var5.getDefaultValue().toString());
    }

    @Test
    public void parseInputSettings() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + File.separator + "inputs" + File.separator+ "input-types.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + File.separator + "inputs" + File.separator+ "input-settings.json"));
        List<? extends CWLParameter> inputs = processObj.getInputs();
        assertEquals(6, inputs.size());
        CommandInputParameter var0 = (CommandInputParameter)findParameter("var0", inputs);
        assertEquals(CWLTypeSymbol.STRING, var0.getType().getType().getSymbol());
        assertEquals("test", var0.getValue());
        CommandInputParameter var1 = (CommandInputParameter)findParameter("var1", inputs);
        assertEquals(CWLTypeSymbol.NULL, var1.getType().getType().getSymbol());
        assertNull(var1.getValue());
        assertEquals(NullValue.NULL, var1.getDefaultValue());
        CommandInputParameter var2 = (CommandInputParameter)findParameter("var2", inputs);
        assertEquals(CWLTypeSymbol.ARRAY, var2.getType().getType().getSymbol());
        InputArrayType arrayType = (InputArrayType) var2.getType().getType();
        assertEquals(CWLTypeSymbol.STRING, arrayType.getItems().getType().getSymbol());
        assertEquals("[test0, test1]", var2.getValue().toString());
        CommandInputParameter var3 = (CommandInputParameter)findParameter("var3", inputs);
        assertEquals(CWLTypeSymbol.NULL, var3.getType().getType().getSymbol());
        assertEquals(NullValue.NULL, var3.getDefaultValue());
        CommandInputParameter var4 = (CommandInputParameter)findParameter("var4", inputs);
        assertEquals(CWLTypeSymbol.ARRAY, var4.getType().getType().getSymbol());
        arrayType = (InputArrayType) var4.getType().getType();
        assertEquals(CWLTypeSymbol.STRING, arrayType.getItems().getType().getSymbol());
        assertEquals("[test0, test1]", var4.getValue().toString());
        CommandInputParameter var5 = (CommandInputParameter)findParameter("var5", inputs);
        assertEquals(CWLTypeSymbol.STRING, var5.getType().getType().getSymbol());
        assertEquals("test", var5.getValue());
    }

    @Test
    public void parseArguments() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "arg-eval.cwl"));
        CommandLineTool commandLineTool = ((CommandLineTool) processObj);
        assertEquals(CWLProcess.CLASS_COMMANDLINETOOL, commandLineTool.getClazz());
        assertTrue(CWLVersion.V10 == commandLineTool.getCwlVersion());
        assertEquals(1, commandLineTool.getRequirements().size());
        assertEquals(0, commandLineTool.getInputs().size());
        assertEquals(0, commandLineTool.getOutputs().size());
        assertEquals(1, commandLineTool.getBaseCommand().size());
        assertEquals("echo", commandLineTool.getBaseCommand().get(0));
        assertEquals(3, commandLineTool.getArguments().size());
    }

    @Test
    public void yieldLinuxSort() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "linux-sort.cwl"));
        CommandLineTool commandLineTool = ((CommandLineTool) processObj);
        assertEquals(CWLProcess.CLASS_COMMANDLINETOOL, commandLineTool.getClazz());
        assertTrue(CWLVersion.V10 == commandLineTool.getCwlVersion());
        assertEquals(1, commandLineTool.getRequirements().size());
        assertEquals(3, commandLineTool.getInputs().size());
        assertEquals(1, commandLineTool.getOutputs().size());
        assertEquals("$(inputs.output)", commandLineTool.getStdout().getExpression());
        assertEquals(1, commandLineTool.getBaseCommand().size());
        assertEquals("sort", commandLineTool.getBaseCommand().get(0));
        assertEquals("linux-sort.cwl is developed for CWL consortium\n", commandLineTool.getDoc());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void loadLinuxSortInputSettings() throws CWLException, JsonProcessingException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "linux-sort.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "linux-sort-job.json"));
        List<? extends CWLParameter> params = processObj.getInputs();
        assertEquals(3, params.size());
        assertEquals("./files/SRR1031972.bedGraph.sorted", findParameter("output", params).getValue());
        List<CWLFile> input = (List<CWLFile>) findParameter("input", params).getValue();
        Path path = Paths.get(input.get(0).getPath());
        assertEquals("SRR1031972.bedGraph", path.getFileName().toString());
        List<String> key = (List<String>) findParameter("key", params).getValue();
        assertEquals(2, key.size());
    }

    @Test
    public void parseOuputStdout() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "output-stdout.cwl"));
        CommandLineTool commandLineTool = ((CommandLineTool) processObj);
        assertEquals("a_stdout_file", commandLineTool.getStdout().getValue());
        assertEquals("a_stderr_file", commandLineTool.getStderr().getValue());
        List<CommandOutputParameter> outputs = (List<CommandOutputParameter>) commandLineTool.getOutputs();
        CommandOutputParameter out = outputs.get(0);
        assertEquals(CWLTypeSymbol.FILE, out.getType().getType().getSymbol());
        assertTrue(out.isStreamable());
        assertEquals("a_stdout_file", out.getOutputBinding().getGlob().getGlobExpr().getValue());
        CommandOutputParameter err = outputs.get(1);
        assertEquals(CWLTypeSymbol.FILE, err.getType().getType().getSymbol());
        assertTrue(err.isStreamable());
        assertEquals("a_stderr_file", err.getOutputBinding().getGlob().getGlobExpr().getValue());
    }

    @Test
    public void parseOuputStdoutWithoutStd() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "output-stdout-withoutstd.cwl"));
        CommandLineTool commandLineTool = ((CommandLineTool) processObj);
        assertTrue(commandLineTool.getStdout().getValue().startsWith("random_stdout_"));
        assertTrue(commandLineTool.getStderr().getValue().startsWith("random_stderr_"));
        List<CommandOutputParameter> outputs = (List<CommandOutputParameter>) commandLineTool.getOutputs();
        CommandOutputParameter out = outputs.get(0);
        assertEquals(CWLTypeSymbol.FILE, out.getType().getType().getSymbol());
        assertTrue(out.isStreamable());
        assertTrue(out.getOutputBinding().getGlob().getGlobExpr().getValue().startsWith("random_stdout_"));
        CommandOutputParameter err = outputs.get(1);
        assertEquals(CWLTypeSymbol.FILE, err.getType().getType().getSymbol());
        assertTrue(err.isStreamable());
        assertTrue(err.getOutputBinding().getGlob().getGlobExpr().getValue().startsWith("random_stderr_"));
    }

    @Test
    public void parseCompile() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "compile.cwl"), "main");
        assertEquals(3, processObj.getSteps().size());
        assertNotNull(processObj.getSteps().get(0).getRun());
        CommandLineTool commandLineTool = (CommandLineTool) processObj.getSteps().get(0).getRun();
        assertTrue(commandLineTool.getArguments().size() > 0);
        assertNotNull(processObj.getSteps().get(1).getRun());
        assertNotNull(processObj.getSteps().get(2).getRun());
    }

    @Test
    public void parseWorkflowWithStepRefrences() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "1st-workflow-full.cwl"), "main");
        assertEquals(2, processObj.getSteps().size());
        assertNotNull(processObj.getSteps().get(0).getRun());
        assertNotNull(processObj.getSteps().get(1).getRun());
    }

    @Test
    public void parseWorkflowWithStepsFromExternalFiles() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "1st-workflow.cwl"));
        assertEquals(2, processObj.getSteps().size());
        assertNotNull(processObj.getSteps().get(0).getRun());
        assertNotNull(processObj.getSteps().get(1).getRun());
    }

    @Test
    public void parseWorkflowWithImportedSteps() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "1st-workflow-import.cwl"));
        assertEquals(2, processObj.getSteps().size());
        assertNotNull(processObj.getSteps().get(0).getRun());
        assertNotNull(processObj.getSteps().get(1).getRun());
    }

    @Test
    public void parseWorkflowWithNestedWorkflows() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "1st-workflow-nested.cwl"));
        assertEquals(2, processObj.getSteps().size());
        assertNotNull(processObj.getSteps().get(0).getRun());
        assertNotNull(processObj.getSteps().get(1).getRun());
    }

    @Test
    public void parseWorkflowWithInvalidStepRunValue() throws CWLException {
        thrown.expect(CWLException.class);
        thrown.expectMessage("The field");
        CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "1st-workflow-bad1.cwl"), "main");
    }

    @Test
    public void parseWorkflowWithInvalidStringValueOfStepRun() throws CWLException {
        thrown.expect(CWLException.class);
        thrown.expectMessage("The field");
        CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "1st-workflow-bad2.cwl"), "main");
    }

    @Test
    public void parseSubWorkflow() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "2nd-workflow.cwl"), "main");
        assertEquals(3, processObj.getSteps().size());
        assertNotNull(processObj.getSteps().get(0).getRun());
        assertNotNull(processObj.getSteps().get(1).getRun());
        assertTrue(processObj.getSteps().get(1).getRun() instanceof Workflow);
        assertNotNull(processObj.getSteps().get(2).getRun());
    }

    @Test
    public void parseSubWorkflowWithSubWorkflow() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "3rd-workflow.cwl"), "main");
        assertEquals(4, processObj.getSteps().size());
        assertNotNull(processObj.getSteps().get(0).getRun());
        assertNotNull(processObj.getSteps().get(1).getRun());
        assertNotNull(processObj.getSteps().get(2).getRun());
        assertNotNull(processObj.getSteps().get(3).getRun());
        assertTrue(processObj.getSteps().get(2).getRun() instanceof Workflow);
        Workflow subWorkflow = (Workflow) processObj.getSteps().get(2).getRun();
        assertEquals(3, subWorkflow.getSteps().size());
        assertNotNull(subWorkflow.getSteps().get(0).getRun());
        assertNotNull(subWorkflow.getSteps().get(1).getRun());
        assertNotNull(subWorkflow.getSteps().get(2).getRun());
        assertTrue(subWorkflow.getSteps().get(1).getRun() instanceof Workflow);
        Workflow subSubWorkflow = (Workflow) subWorkflow.getSteps().get(1).getRun();
        assertEquals(2, subSubWorkflow.getSteps().size());
        assertNotNull(subSubWorkflow.getSteps().get(0).getRun());
        assertNotNull(subSubWorkflow.getSteps().get(1).getRun());
    }

    @Test
    public void parseSubWorkflowWithouSubworkflowFeatureRequirement() throws CWLException {
        thrown.expect(CWLException.class);
        thrown.expectMessage("[SubworkflowFeatureRequirement]");
        CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "2nd-workflow-bad.cwl"), "main");
    }

    @Test
    public void parseWorkflowWithInputFile() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "1st-workflow.cwl"));
        assertNotNull(processObj);
        assertNotNull(processObj.getInputs());
        assertEquals(2, processObj.getInputs().size());
        assertNull(processObj.getInputs().get(0).getValue());
        assertNull(processObj.getInputs().get(1).getValue());
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + File.separator + "inputs" + File.separator + "1st-workflow.yml"));
        assertNotNull(processObj.getInputs().get(0).getValue());
        assertNotNull(processObj.getInputs().get(1).getValue());
    }

    @Test
    public void parseWorkflowWithScatterSteps() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "scatter-job.cwl"));
        assertNotNull(processObj.getSteps().get(0).getScatter());
    }

    @Test
    public void parseWorkflowWithScatterNestedSteps() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "scatter-job-nested.cwl"));
        assertNotNull(processObj.getSteps().get(0).getScatter());
        assertEquals("NESTED_CROSSPRODUCT", processObj.getSteps().get(0).getScatterMethod().toString());
    }

    @Test
    public void parseFlowExecConf() throws CWLException {
        FlowExecConf flowExecConf = CWLParser.parseFlowExecConf(new File(DEF_ROOT_PATH + "config/flowConfig.json"));
        assertEquals("testPro", flowExecConf.getProject());
        assertEquals("testQ", flowExecConf.getQueue());
        assertTrue(flowExecConf.isRerunnable());
        flowExecConf = CWLParser.parseFlowExecConf(new File(DEF_ROOT_PATH + "config/stepConfig.json"));
        assertEquals("testPP", flowExecConf.getProject());
        assertEquals("testQQ", flowExecConf.getQueue());
        assertFalse(flowExecConf.isRerunnable());
        assertEquals(1, flowExecConf.getSteps().size());
        assertTrue(flowExecConf.getSteps().get("step1").isRerunnable());
    }

    @Test
    public void parseWorkflowWithScatterDotSteps() throws CWLException {
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "scatter-job-dot.cwl"), "main");
        assertNotNull(processObj.getSteps().get(0).getScatter());
        assertEquals("DOTPRODUCT", processObj.getSteps().get(0).getScatterMethod().toString());
    }

    @Test
    public void parseSchemadefWf() throws CWLException {
        //schemadef-wf
        Workflow processObj = (Workflow) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "schemadef-wf.cwl"), null);
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "schemadef-job.json"));
        assertNotNull(processObj);
    }

    @Test
    public void parseNamespace() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "formattest3.cwl"));
        CommandLineTool commandLineTool = ((CommandLineTool) processObj);
        assertNotNull(commandLineTool.getNamespaces());
        assertEquals("http://edamontology.org/", commandLineTool.getNamespaces().get("edam"));
        assertEquals("http://galaxyproject.org/formats/", commandLineTool.getNamespaces().get("gx"));
    }

    @Test
    public void parseOutputFormat() throws CWLException {
        CWLProcess processObj = CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "formattest3.cwl"));
        CommandLineTool commandLineTool = ((CommandLineTool) processObj);
        assertEquals(1, commandLineTool.getOutputs().stream()
                .filter(s -> s.getFormat().getFormat().getExpression().equals("$(inputs.input.format)")).count());
        assertEquals(1, commandLineTool.getInputs().stream()
                .filter(s -> s.getFormat().getFormat().getValue().equals("gx:fasta")).count());
        assertEquals(0, commandLineTool.getOutputs().stream()
                .filter(s -> s.getFormat().getFormat().getExpression().equals("undefined")).count());
        assertEquals(0, commandLineTool.getInputs().stream()
                .filter(s -> s.getFormat().getFormat().getValue().equals("undefined")).count());
    }

}

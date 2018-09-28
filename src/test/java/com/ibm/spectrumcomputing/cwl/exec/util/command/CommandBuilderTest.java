package com.ibm.spectrumcomputing.cwl.exec.util.command;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.command.DockerCommandBuilder;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.CommandStdIOEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.InputsEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.RequirementsEvaluator;
import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvVarRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.parser.CWLParser;

public class CommandBuilderTest extends CWLExecTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CommandBuilderTest.class);

    @Test
    public void buildLinuxSort() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "linux-sort.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "linux-sort-job.json"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(6, commands.size());
    }

    @Test
    public void buildArguments() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "arg-eval.cwl"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(16, commands.size());
        assertEquals("echo", commands.get(0));
        assertEquals("-A", commands.get(1));
        assertEquals("1", commands.get(15));
    }

    @Test
    public void buildTwoDimensionalArray() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "input-two-dimensional-array.cwl"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        //echo -k -kk key1 -kk key2 -k -kk key3 -kk key4
        assertEquals(11, commands.size());
        assertEquals("echo", commands.get(0));
        assertEquals("-k", commands.get(1));
        assertEquals("-kk", commands.get(2));
        assertEquals("key1", commands.get(3));
        assertEquals("-kk", commands.get(4));
        assertEquals("key2", commands.get(5));
        assertEquals("-k", commands.get(6));
        assertEquals("-kk", commands.get(7));
        assertEquals("key3", commands.get(8));
        assertEquals("-kk", commands.get(9));
        assertEquals("key4", commands.get(10));
    }

    @Test
    public void buildArgSort() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "arg-sort.cwl"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(25, commands.size());
        assertEquals("echo", commands.get(0));
        assertEquals("-k", commands.get(1));
        assertEquals("baz", commands.get(12));
        assertEquals("-C", commands.get(13));
        assertEquals("test", commands.get(24));
    }

    @Test
    public void buildSecondaryFiles() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "secondaryfiles/secondaryfiles.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "secondaryfiles/secondaryfiles-job.json"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        assertEquals(4, inputs.size());
        CommandInputParameter input0 = (CommandInputParameter) findParameter("input0", inputs);
        assertEquals(0, ((CWLFile) input0.getValue()).getSecondaryFiles().size());
        CommandInputParameter input1 = (CommandInputParameter) findParameter("input1", inputs);
        assertEquals(1, ((CWLFile) input1.getValue()).getSecondaryFiles().size());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(2, commands.size());
        assertEquals("cat", commands.get(0));
        assertTrue(commands.get(1).endsWith("ref.fasta.test\""));
    }

    @Test
    public void buildArraySecondaryFiles() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "secondaryfiles/array-secondaryfiles.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "secondaryfiles/array-secondaryfiles-job.json"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(3, commands.size());
    }

    @Test
    public void buildPipe() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "shellCmdReq.cwl"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(3, commands.size());
    }

    @Test
    public void buildEnvVarRequirement() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "envVarRequirement.cwl"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        EnvVarRequirement envVarRequirement = CWLExecUtil.findRequirement(processObj, EnvVarRequirement.class);
        assertEquals("0", envVarRequirement.getEnvDef().get(0).getEnvValue().getValue());
        assertEquals("1", envVarRequirement.getEnvDef().get(1).getEnvValue().getValue());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(1, commands.size());
        assertEquals("echo", commands.get(0));
    }

    @Test
    public void buildSecondaryFilesDocker() throws CWLException {
        if (!is_win) {
            CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "docker/docker-array-secondaryfiles.cwl"));
            CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "docker/docker-array-secondaryfiles-job.json"));
            InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
            RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
            
            List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
            InputsEvaluator.eval(jsReq, runtime, inputs);
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
            CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
            instance.setRuntime(runtime);
            instance.setRuntimeEnv(RuntimeEnv.LOCAL);
            List<String> commands = CommandUtil.buildCommand(instance);
            DockerRequirement dockerRequirement = CWLExecUtil.findRequirement(instance.getProcess(), DockerRequirement.class);
            commands = DockerCommandBuilder.buildDockerRun(dockerRequirement, instance, commands);
            assertEquals(24, commands.size());
        } else {
            logger.info("The CommandBuilderTest.buildSecondaryFilesDocker cannot run on the Windows");
        }
    }

    @Test
    public void buildDocker() throws CWLException {
        if (!is_win) {
            CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "docker/docker.cwl"));
            CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "docker/docker-job.yml"));
            InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
            RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
            
            List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
            InputsEvaluator.eval(jsReq, runtime, inputs);
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
            CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
            CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
            instance.setRuntime(runtime);
            instance.setRuntimeEnv(RuntimeEnv.LOCAL);
            List<String> commands = CommandUtil.buildCommand(instance);
            DockerRequirement dockerRequirement = CWLExecUtil.findRequirement(instance.getProcess(), DockerRequirement.class);
            commands = DockerCommandBuilder.buildDockerRun(dockerRequirement, instance, commands);
            assertEquals(14, commands.size());
        } else {
            logger.info("The CommandBuilderTest.buildDocker cannot run on the Windows");
        }
    }

    @Test
    public void buildStdio() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "stdio.cwl"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(1, commands.size());
    }

    @Test
    public void buildArrayInputs() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "array-inputs.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "array-inputs-job.yml"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        //-A one two three -BB -B=four -B=five -B=six -C=seven,eight,nine
        assertEquals("[echo, -A, one, two, three, -BB, -B=four, -B=five, -B=six, -C=seven,eight,nine]", commands.toString());
    }

    @Test
    public void buildBWAMem() throws CWLException {
        if (is_win) {
            logger.warn("CommandBuilderTest#buildBWAMem is unspported on windowns.");
            return;
        }
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "bwa-mem-tool.cwl"));
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "bwa-mem-job.json"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(20, commands.size());
    }

    @Test
    public void buildBindingTest() throws CWLException {
        if (is_win) {
            logger.warn("CommandBuilderTest#buildBindingTest is unspported on windowns.");
            return;
        }
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "binding-test.cwl"));
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "bwa-mem-job.json"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(20, commands.size());
    }

    @Test
    public void buildTmapTool() throws CWLException {
        if (is_win) {
            logger.warn("CommandBuilderTest#buildTmapTool is unspported on windowns.");
            return;
        }
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "tmap-tool.cwl"));
        assertEquals(3, processObj.getInputs().size());
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "tmap-job.json"));
        CWLParameter stages = findParameter("stages", processObj.getInputs());
        @SuppressWarnings("unchecked")
        List<InputRecordField> recordFields = (List<InputRecordField>) stages.getValue();
        assertEquals(2, recordFields.size());
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(16, commands.size());
    }

    @Test
    public void buildImportedHint() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "imported-hint.cwl"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(3, commands.size());
    }

    @Test
    public void buildSchemadefTool() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "schemadef-tool.cwl"));
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "schemadef-job.json"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(2, commands.size());
    }

    @Test
    public void buildRecordOutput() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "record-output.cwl"));
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "record-output-job.json"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(3, commands.size());
    }

    @Test
    public void buildRecord() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "record.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "record-job2.yml"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(7, commands.size());
    }

    @Test
    public void buildTemplateTool() throws CWLException {
        if (is_win) {
            logger.warn("CommandBuilderTest#buildTemplateTool is unspported on windowns.");
            return;
        }
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "template-tool.cwl"));
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "cat-job.json"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(15, commands.size());
    }

    @Test
    public void buildEchoTool() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "echo-tool.cwl"));
        CWLParser.loadInputSettings(processObj, new File(CONFORMANCE_PATH + "env-job.json"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(4, commands.size());
    }

    @Test
    public void parseInitialWorkDir() throws CWLException {
        if (is_win) {
            logger.warn("CommandBuilderTest#parseInitialWorkDir is unspported on windowns.");
            return;
        }
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "initialworkdir/attributor-prok-cheetah.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "initialworkdir/attributor-prok-cheetah.json"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        RequirementsEvaluator.evalInitialWorkDirReq(instance);
        assertNotNull(instance);
    }

    @Test
    public void parseEnumArray() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "enum_array/test.cwl"));
        CWLParser.loadInputSettings(processObj, new File(DEF_ROOT_PATH + "enum_array/test.input.yaml"));
        assertNotNull(processObj);
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, processObj.getStdout());
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        List<String> commands = CommandUtil.buildCommand(instance);
        assertEquals(4, commands.size());
    }
} 

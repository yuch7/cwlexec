package com.ibm.spectrumcomputing.cwl.exec.util.outputs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.InputsEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.RequirementsEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.outputs.OutputsCapturer;
import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.parser.CWLParser;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

public class OutputsCapturerTest extends CWLExecTestBase {

    private static final Logger logger = LoggerFactory.getLogger(OutputsCapturerTest.class);

    @Test
    public void captureCommandOutputs() throws CWLException {
        if (is_win) {
            logger.warn("The OutputsCapturerTest#captureCommandOutputs is unsupported on Windows.");
            return;
        }
        Map<String, String> runtime = new HashMap<>();
        runtime.put(CommonUtil.RUNTIME_TMP_DIR, DEF_ROOT_PATH + "outputs");
        runtime.put(CommonUtil.RUNTIME_OUTPUT_DIR, System.getProperty("java.io.tmpdir") + "test_output_" + CommonUtil.getRandomStr());
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "outputs/outputs.cwl"));
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(processObj, InlineJavascriptRequirement.class);
        RequirementsEvaluator.evalMainEnvVarReq(jsReq, runtime, processObj);
        List<CommandInputParameter> inputs = (List<CommandInputParameter>) processObj.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        OutputsCapturer.captureCommandOutputs(instance);
        assertEquals(3, processObj.getOutputs().size());
        assertEquals("test0.test1", findParameter("output_path", processObj.getOutputs()).getValue());
        assertEquals(1234.56789, findParameter("output_length", processObj.getOutputs()).getValue());
        @SuppressWarnings("unchecked")
        List<CWLFile> files = (List<CWLFile>) findParameter("output_sraFiles", processObj.getOutputs()).getValue();
        assertEquals(3, files.size());
    }

    @Test
    public void captureParamsOutputs() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(CONFORMANCE_PATH + "params.cwl"));
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        instance.setRuntimeEnv(RuntimeEnv.LOCAL);
        OutputsCapturer.captureCommandOutputs(instance);
        assertEquals(28, processObj.getOutputs().size());
    }
}

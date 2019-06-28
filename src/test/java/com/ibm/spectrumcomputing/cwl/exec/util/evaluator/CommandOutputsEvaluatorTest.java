package com.ibm.spectrumcomputing.cwl.exec.util.evaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.parser.CWLParser;

public class CommandOutputsEvaluatorTest extends CWLExecTestBase {

    @Test
    public void evalExpression() throws CWLException {
        CommandLineTool processObj = (CommandLineTool) CWLParser.yieldCWLProcessObject(new File(DEF_ROOT_PATH + "multiple-arrays.cwl"));
        assertNotNull(processObj);
        CWLCommandInstance instance = new CWLCommandInstance("test", owner, processObj, new FlowExecConf());
        instance.setRuntime(runtime);
        assertNotNull(instance);
        Map<String, Object> results = new HashMap<>();
        for (CommandOutputParameter output : processObj.getOutputs()) {
            Object obj = CommandOutputsEvaluator.evalExpression(instance, processObj.getInputs(), output.getType().getType(), output.getId());
            results.put(output.getId(), obj);
        }
        assertEquals(2, results.size());
        assertEquals("[0, 1, 2, 3]", results.get("int_array").toString());
        assertEquals("[hello0.txt, hello1.txt, hello2.txt, hello3.txt]", results.get("str_array").toString());
    }
}

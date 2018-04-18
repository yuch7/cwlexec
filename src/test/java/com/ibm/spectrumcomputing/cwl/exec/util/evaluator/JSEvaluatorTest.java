package com.ibm.spectrumcomputing.cwl.exec.util.evaluator;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.JSEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.JSResultWrapper;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;

public class JSEvaluatorTest extends CWLExecTestBase {

    @Test
    public void nullResult() throws CWLException {
        String expr = "${return null;}";
        JSResultWrapper r = JSEvaluator.evaluate(expr);
        assertTrue(r.isNull());
        expr = "${var a; return a;}";
        r = JSEvaluator.evaluate(expr);
        assertTrue(r.isNull());
    }

    @Test
    public void stringResult() throws CWLException {
        String expr = "$(\"/foo/bar/baz\".split('/').slice(-1)[0])";
        JSResultWrapper r = JSEvaluator.evaluate(expr);
        assertTrue(r.isString());
        assertEquals("baz", r.asString());
    }

    @Test
    public void longResult() throws CWLException {
        String expr = "$(1+1)";
        JSResultWrapper r = JSEvaluator.evaluate(expr);
        assertTrue(r.isLong());
        assertEquals(2L, r.asLong());
    }

    @Test
    public void doubleResult() throws CWLException {
        String expr = "$(1.1+1.2)";
        JSResultWrapper r = JSEvaluator.evaluate(expr);
        assertTrue(r.isDouble());
        assertEquals(2.3, r.asDouble(), 0.0);
    }

    @Test
    public void boolResult() throws CWLException {
        String expr = "$(true)";
        JSResultWrapper r = JSEvaluator.evaluate(expr);
        assertTrue(r.isBool());
        assertTrue(r.asBool());
    }

    @Test
    public void arrayResult() throws CWLException {
        String expr = "${return [1, 2, 3];}";
        JSResultWrapper r = JSEvaluator.evaluate(expr);
        assertTrue(r.isArray());
        assertEquals(JSResultWrapper.ResultType.ARRAY, r.getType());
        assertEquals(3, r.elements().size());
    }

    @Test
    public void objectResult() throws CWLException {
        String expr = "${return {\"a\": 1,"
                + "\"b\":4,"
                + "\"c\": {"
                +   "\"c1\": 5,"
                +   "\"c2\": [1,2,3]"
                + "}"
                + "};}";
        JSResultWrapper r = JSEvaluator.evaluate(expr);
        assertFalse(r.isArray());
        assertTrue(r.isObject());
        assertEquals(JSResultWrapper.ResultType.OBJECT, r.getType());
        assertEquals(3, r.keys().size());
        JSResultWrapper c = r.getValue("c");
        assertEquals(JSResultWrapper.ResultType.OBJECT, c.getType());
        JSResultWrapper c2 = c.getValue("c2");
        assertEquals(JSResultWrapper.ResultType.ARRAY, c2.getType());
        assertEquals(3, c2.elements().size());
    }

    @Test
    public void evaluateWithLibs() throws CWLException {
        List<String> libs = Arrays.asList("var inputs={\"output\": \"./test-files/SRR1031972.bedGraph.sorted\"}");
        String expr = "$(inputs.output)";
        JSResultWrapper r = JSEvaluator.evaluate(libs, expr);
        assertTrue(r.isString());
        assertEquals("./test-files/SRR1031972.bedGraph.sorted", r.asString());
    }

    @Test
    public void hasJSONObj() throws CWLException {
        JSResultWrapper r = JSEvaluator.evaluate("${return (JSON !== null)}");
        assertTrue(r.isBool());
        assertTrue(r.asBool());
    }
}

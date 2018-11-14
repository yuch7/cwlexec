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
package com.ibm.spectrumcomputing.cwl.exec.util.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.WorkflowStepInput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.ScatterMethod;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

/**
 * Utility methods for evaluating the CWL step process valueFrom expression
 */
public class StepInValueFromEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(StepInValueFromEvaluator.class);

    private StepInValueFromEvaluator() {
    }

    /**
     * Evaluates a CWL step process valueFrom expression
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of the CWL step process
     * @param runtime
     *            The runtime of the CWL step process
     * @param inputs
     *            The corresponding input parameters of the CWL step process
     * @param self
     *            The corresponding input parameter value of the CWL step process
     * @param step
     *            A CWL step process object
     * @param srcParam
     *            The input of the CWL step process
     * @param targetInput
     *            The corresponding command/expression input
     * @return The value of the expression
     * @throws CWLException
     *             Failed to evaluate the expression
     */
    public static Object eval(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<CWLParameter> inputs,
            Object self,
            WorkflowStep step,
            WorkflowStepInput srcParam,
            CWLParameter targetInput) throws CWLException {
        CWLFieldValue valueFrom = srcParam.getValueFrom();
        Object value = null;
        if (valueFrom != null) {
            value = evalValue(jsReq, runtime, self, step, srcParam, targetInput, inputs, valueFrom);
        }
        // There is a JS engine bug, cast double to integer if necessary
        CWLType type = targetInput.getType().getType();
        if (CWLTypeSymbol.INT == type.getSymbol() && value instanceof Double) {
            value = Long.valueOf((((Double) value).longValue()));
        }
        return value;
    }

    /**
     * Evaluates a CWL step process valueFrom expression
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of the CWL step process
     * @param runtime
     *            The runtime of the CWL step process
     * @param inputs
     *            The corresponding input parameters of the CWL step process
     * @param self
     *            The corresponding input parameter value of the CWL step process
     * @param valueFromExpr
     *            The valueFrom expression
     * @return The value of the expression
     * @throws CWLException
     *             Failed to evaluate the expression
     */
    public static Object evalExpr(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<? extends CWLParameter> inputs,
            Object self,
            String valueFromExpr) throws CWLException {
        Object value = null;
        if (valueFromExpr != null) {
            List<String> context = JSEvaluator.constructEvalContext(jsReq);
            context.add(JSEvaluator.toRuntimeContext(runtime));
            context.add(JSEvaluator.toInputsContext(inputs));
            context.add(JSEvaluator.toSelfContext(self));
            JSResultWrapper r = JSEvaluator.evaluate(context, valueFromExpr);
            value = toExprValue(r);
        }
        return value;
    }

    private static Object evalValue(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            Object self,
            WorkflowStep step,
            WorkflowStepInput stepInput,
            CWLParameter targetInput,
            List<CWLParameter> inputs,
            CWLFieldValue valueFrom) throws CWLException {
        Object value = valueFrom.getValue();
        if (value == null) {
            if (step.getScatter() != null) {
                if (step.getScatter().contains(stepInput.getId())) {
                    @SuppressWarnings("unchecked")
                    List<Object> scatters = (List<Object>) self;
                    List<Object> values = new ArrayList<>();
                    for (Object scatter : scatters) {
                        values.add(evalExpr(jsReq, runtime, inputs, scatter, valueFrom.getExpression()));
                    }
                    value = values;
                } else {
                    CWLFieldValue valueFromExpr = stepInput.getValueFrom();
                    if (valueFromExpr != null) {
                        value = getValueFromRecordSource(self, step, stepInput);
                        if (value == null) {
                            // put off this evaluation, refer to issue #36
                            targetInput.setSelf(self);
                            targetInput.setDelayedValueFromExpr(valueFromExpr.getExpression());
                            value = new ArrayList<Object>();
                        }
                    }
                }
            } else {
                value = evalExpr(jsReq, runtime, inputs, self, valueFrom.getExpression());
            }
        }
        return value;
    }

    // Only handle the single evaluation expression, e.g. $(inputs.var1)
    // see: test/integration-test/v1.0/scatter-valuefrom-wf5.cwl 
    private static Object getValueFromRecordSource(Object self, WorkflowStep step, WorkflowStepInput stepInput) {
        Object value = null;
        for (String scatterId : step.getScatter()) {
            String expr = stepInput.getValueFrom().getExpression();
            if (expr.startsWith("$(inputs") && expr.contains("inputs." + scatterId)) {
                return scatterInput(stepInput.getId(), scatterId, step);
            }
        }
        return value;
    }

    private static Object scatterInput(String stepInputId, String dependentScatter, WorkflowStep step) {
        Object value = null;
        CWLParameter targetParam = CommonUtil.findParameter(dependentScatter, step.getRun().getInputs());
        if (targetParam != null) {
            value = targetParam.getValue();
            step.getScatter().add(stepInputId);
            step.setScatterMethod(ScatterMethod.DOTPRODUCT);
        }
        return value;
    }

    private static Object toExprValue(JSResultWrapper r) throws CWLException {
        Object value = null;
        if (!r.isNull()) {
            if (r.isString()) {
                value = r.asString();
            } else if (r.isLong()) {
                value = r.asLong();
            } else if (r.isDouble()) {
                value = r.asDouble();
            } else if (r.isArray()) {
                List<Object> values = new ArrayList<>();
                for (JSResultWrapper e : r.elements()) {
                    values.add(toExprValue(e));
                }
                value = values;
            } else if (r.isCWLFile()) {
                value = r.asCWLFile();
            } else if (r.isCWLDirectory()) {
                value = r.asCWLDirectory();
            } else if (r.isBool()) {
                value = r.asBool();
            } else {
                // other type is not handled yet
                logger.warn("ValueFrom type ({}) is not handled yet.", r.getType());
            }
        }
        return value;
    }
}

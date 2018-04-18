/*
 * Copyright 2002-2012 the original author or authors.
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

import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandLineBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Utility methods for evaluating CommandLineBinding expression
 */
public final class CommandLineBindingEvaluator {

    public static final String DEFAULT_ITEM_SEPARATOR = "__is__";
    public static final String BOOLEAN_VALUE = "True";

    private CommandLineBindingEvaluator() {
    }

    /**
     * Evaluates a given CommandLineBinding valueFrom expression
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of the expression
     * @param runtime
     *            The runtime of the expression
     * @param inputs
     *            The inputs of the expression
     * @param self
     *            The self object of the expression, if the CommandLineBinding
     *            object is from the CWL CommandLineTool argument, it is null,
     *            if the CommandLineBinding object is from the CommandLineTool
     *            input, it is the value of the input
     * @param binding
     *            A given CommandLineBinding object
     * @return The value of CommandLineBinding valueFrom expression
     * @throws CWLException
     *             Failed to evaluate the CommandLineBinding valueFrom
     *             expression
     */
    public static String evalValueFrom(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<CommandInputParameter> inputs,
            Object self,
            CommandLineBinding binding) throws CWLException {
        String valueFrom = null;
        CWLFieldValue valueFromExpr = binding.getValueFrom();
        if (valueFromExpr != null) {
            String expr = valueFromExpr.getExpression();
            if (expr != null) {
                List<String> context = JSEvaluator.constructEvalContext(jsReq);
                context.add(JSEvaluator.toRuntimeContext(runtime));
                context.add(JSEvaluator.toInputsContext(inputs));
                context.add(JSEvaluator.toSelfContext(self));
                JSResultWrapper r = JSEvaluator.evaluate(context, expr);
                if (!r.isNull()) {
                    valueFromExpr.setValue(toStringValue(r, binding));
                    valueFrom = valueFromExpr.getValue();
                }
            }
        }
        return valueFrom;
    }

    private static String toStringValue(JSResultWrapper r, CommandLineBinding binding) throws CWLException {
        String value = null;
        if (r.isBool()) {
            if (r.asBool() && !binding.isEmpty()) {
                value = BOOLEAN_VALUE;
            }
        } else if (r.isString()) {
            value = r.asString();
            if (value.indexOf(System.getProperty("line.separator")) != -1) {
                value = "'" + value.replaceAll("'", "'\"'\"'") + "'";
            }
        } else if (r.isDouble()) {
            value = String.valueOf(r.asDouble());
        } else if (r.isLong()) {
            value = String.valueOf(r.asLong());
        } else if (r.isArray()) {
            List<JSResultWrapper> elements = r.elements();
            List<String> strings = new ArrayList<>();
            for (JSResultWrapper element : elements) {
                strings.add(element.getStringRepresentation());
            }
            String itemSeparator = binding.getItemSeparator();
            if (itemSeparator == null) {
                itemSeparator = DEFAULT_ITEM_SEPARATOR;
            }
            value = String.join(itemSeparator, strings);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.expression.command.line.binding.valuefrom.invalid"),
                    253);
        }
        return value;
    }
}

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandOutputBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.OutputBindingGlob;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Utility methods for evaluating CommandOutputBinding expression
 */
public final class CommandOutputBindingEvaluator {

    private CommandOutputBindingEvaluator() {
    }

    /**
     * Evaluates the glob expression of a given CommandOutputBinding
     * 
     * @param jsRequirement
     *            The InlineJavascriptRequirement of the expression
     * @param inputs
     *            The inputs of the expression
     * @param outputBinding
     *            A given CommandOutputBinding object
     * @throws CWLException
     *             Failed to evaluate the glob expression
     */
    public static void evalGlob(InlineJavascriptRequirement jsRequirement,
            List<CommandInputParameter> inputs,
            CommandOutputBinding outputBinding,
            boolean scatter) throws CWLException {
        if (outputBinding != null) {
            OutputBindingGlob glob = outputBinding.getGlob();
            if (glob != null && glob.getGlobExpr() != null) {
                String globExpr = glob.getGlobExpr().getExpression();
                if (globExpr != null) {
                    List<String> context = JSEvaluator.constructEvalContext(jsRequirement);
                    context.add(JSEvaluator.toInputsContext(inputs));
                    if (scatter &&
                            inputs.size() == 1 &&
                            inputs.get(0).getType().getType().getSymbol() != CWLTypeSymbol.ARRAY &&
                            inputs.get(0).getValue() instanceof List) {
                        glob.getGlobExpr().setValue(globExpr.replaceAll(
                                "\\$\\([^\\(\\)]*(\\(.*?\\)[^\\(\\)]*)*\\)\\s*[, ]*", "*"));
                        return;
                    }
                    JSResultWrapper r = JSEvaluator.evaluate(context, globExpr);
                    if (r.isString()) {
                        glob.getGlobExpr().setValue(r.asString());
                    } else if (r.isArray()) {
                        glob.setPatterns(evalGlobPatterns(r, globExpr));
                    } else {
                        throw new CWLException(
                                ResourceLoader.getMessage(
                                        "cwl.expression.evaluate.failed.with.wrong.result",
                                        globExpr,
                                        "glob",
                                        "a string or an array of string"),
                                253);
                    }
                }
            }
        }
    }

    /**
     * Evaluate the CommandOutputBinding evalOutput expression 
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of the expression
     * @param inputs
     *            The inputs of the expression
     * @param self
     *            The files are globbed by CommandOutputBinding glob expression
     * @param type
     *            The type of CommandOutputParameter
     * @param expr
     *            The CommandOutputBinding evalOutput expression
     * @return The value of evalOutput expression
     * @throws CWLException
     *             Failed to evaluate the evalOutput expression
     */
    public static Object evalOutputEval(InlineJavascriptRequirement jsReq,
            List<CommandInputParameter> inputs,
            List<CWLFileBase> self,
            CWLType type,
            String expr) throws CWLException {
        Object value = null;
        if (expr != null) {
            List<String> context = JSEvaluator.constructEvalContext(jsReq);
            context.add(JSEvaluator.toInputsContext(inputs));
            context.add(JSEvaluator.toSelfContext(self));
            if (isSingleExpr(expr)) {
                JSResultWrapper r = JSEvaluator.evaluate(context, expr);
                value = toCWLValue(type, r);
            } else {
                value = evalCombinedExpr(type, context, expr);
            }
        }
        return value;
    }

    private static List<String> evalGlobPatterns(JSResultWrapper r, String globExpr) throws CWLException {
        List<String> patterns = new ArrayList<>();
        for (JSResultWrapper e : r.elements()) {
            if (e.isString()) {
                patterns.add(e.asString());
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(
                                "cwl.expression.evaluate.failed.with.wrong.result",
                                globExpr,
                                "glob",
                                "a string or an array of string"),
                        253);
            }
        }
        return patterns;
    }

    private static boolean isSingleExpr(String expr) {
        Pattern pattern = Pattern.compile("\\$\\(.*?\\)");
        Matcher matcher = pattern.matcher(expr);
        return !matcher.find(1);
    }

    private static String evalCombinedExpr(CWLType type,
            List<String> context,
            String text) throws CWLException {
        Pattern pattern = Pattern.compile("\\$\\(.*?\\)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String expr = matcher.group();
            JSResultWrapper r = JSEvaluator.evaluate(context, expr);
            Object value = toCWLValue(type, r);
            text = text.replace(expr, String.valueOf(value));
        }
        return text;
    }

    private static Object toCWLValue(CWLType type, JSResultWrapper r) throws CWLException {
        Object value = null;
        CWLTypeSymbol typeSymbol = type.getSymbol();
        switch (typeSymbol) {
        case FILE:
            value = r.asCWLFile();
            break;
        case DIRECTORY:
            value = r.asCWLDirectory();
            break;
        case INT:
        case LONG:
            value = toLong(r);
            break;
        case FLOAT:
        case DOUBLE:
            value = toDouble(r);
            break;
        case STRING:
        case ENUM:
            value = toString(r);
            break;
        case BOOLEAN:
            value = toBool(r);
            break;
        case ARRAY:
            List<Object> values = new ArrayList<>();
            CWLType items = ((OutputArrayType) type).getItems().getType();
            for (JSResultWrapper e : r.elements()) {
                values.add(toCWLValue(items, e));
            }
            value = values;
            break;
        case ANY:
            value = r.getValue();
            break;
        default:
            break;
        }
        return value;
    }

    private static String toString(JSResultWrapper r) throws CWLException {
        String value = null;
        if (!r.isNull()) {
            if (r.isString()) {
                value = r.asString();
            } else {
                throw new CWLException(
                        String.format("expact the outputEval is a string, but is %s", r.getType()),
                        253);
            }
        }
        return value;
    }

    private static Double toDouble(JSResultWrapper r) throws CWLException {
        Double value = null;
        if (!r.isNull()) {
            if (r.isDouble()) {
                value = r.asDouble();
            } else {
                throw new CWLException(
                        String.format("expact the outputEval is a double, but is %s", r.getType()),
                        253);
            }
        }
        return value;
    }

    private static Long toLong(JSResultWrapper r) throws CWLException {
        Long value = null;
        if (!r.isNull()) {
            if (r.isLong()) {
                value = r.asLong();
            } else if (r.isDouble()) {
                value = (long) r.asDouble();
            } else {
                throw new CWLException(
                        String.format("expact the outputEval is a long, but is %s", r.getType()),
                        253);
            }
        }
        return value;
    }

    private static Boolean toBool(JSResultWrapper r) throws CWLException {
        Boolean value = null;
        if (!r.isNull()) {
            if (r.isBool()) {
                value = r.asBool();
            } else {
                throw new CWLException(
                        String.format("expact the outputEval is a boolean, but is %s", r.getType()),
                        253);
            }
        }
        return value;
    }
}

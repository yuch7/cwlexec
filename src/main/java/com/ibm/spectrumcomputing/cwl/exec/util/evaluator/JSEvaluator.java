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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullValue;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * Utility methods for evaluating the CWL JavaScript expression following ECMAScript 5.1
 */
final class JSEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(JSEvaluator.class);

    private JSEvaluator() {}

    protected static JSResultWrapper evaluate(String expr) throws CWLException {
        return evaluate(null, expr);
    }

    protected static JSResultWrapper evaluate(List<String> expressionLibs, String expr) throws CWLException {
        JSResultWrapper result = null;
        if (expr != null) {
            StringBuilder scriptBuilder = new StringBuilder();
            scriptBuilder.append(buildExpressionLib(expressionLibs));
            String singleExpr = buildSingleExpr(expr);
            if (singleExpr != null) {
                scriptBuilder.append(singleExpr);
                String script = scriptBuilder.toString().trim();
                if (!script.isEmpty()) {
                    try {
                        logger.debug("Evaluate js expression \"{}\" with context\n{}", expr, expressionLibs);
                        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
                        result = new JSResultWrapper(engine.eval(script));
                        logger.debug("Evaluated js expression \"{}\" to {}", expr, result);
                    } catch (ScriptException e) {
                        throw new CWLException(
                                ResourceLoader.getMessage("cwl.expression.evaluate.failed", expr, e.getMessage()),
                                253);
                    }
                }
            } else {
                //The expression is not a single expression, so the evaluated result should always be a string
                result = new JSResultWrapper(evalScript(expressionLibs, expr.trim()).trim());
                logger.debug("Evaluated script \"{}\" to {}", expr, result);
            }
        }
        return result;
    }

    protected static List<String> constructEvalContext(InlineJavascriptRequirement jsReq) {
        List<String> context = new ArrayList<>();
        if (jsReq != null && jsReq.getExpressionLib() != null && !jsReq.getExpressionLib().isEmpty()) {
            context.addAll(jsReq.getExpressionLib());
        }
        return context;
    }

    protected static String toRuntimeContext(Map<String, String> runtime) {
        List<String> elements = new ArrayList<>();
        for (Entry<String, String> e : runtime.entrySet()) {
            String inputJson = CommonUtil.asJsonStr(e.getKey(), e.getValue());
            if (inputJson != null) {
                elements.add(inputJson.substring(1, inputJson.length() - 1));
            }
        }
        return String.format("var runtime={%s};", String.join(",", elements));
    }

    protected static String toInputsContext(List<? extends CWLParameter> inputs) {
        List<String> elements = new ArrayList<>();
        for (CWLParameter input : inputs) {
            Object value = input.getValue();
            if (value == null || value == NullValue.NULL) {
                value = input.getDefaultValue();
            }
            if (input.getType() != null &&
                    input.getType().getType() != null &&
                    input.getType().getType().getSymbol() == CWLTypeSymbol.RECORD) {
                if (value instanceof List<?>) {
                    List<?> recordFields = (List<?>) value;
                    List<String> records = new ArrayList<>();
                    for (Object recordField : recordFields) {
                        if (recordField instanceof InputRecordField) {
                            String inputJson = CommonUtil.asJsonStr(((InputRecordField) recordField).getName(),
                                    ((InputRecordField) recordField).getValue());
                            if (inputJson != null) {
                                records.add(inputJson.substring(1, inputJson.length() - 1));
                            }
                        }
                    }
                    if (!records.isEmpty()) {
                        elements.add(String.format("\"%s\":{%s}", input.getId(), String.join(",", records)));
                    }
                }
            } else if (value instanceof List<?>) {
                List<?> recordFields = (List<?>) value;
                List<String> records = new ArrayList<>();
                for (Object recordField : recordFields) {
                    if (recordField instanceof InputRecordField) {
                        String inputJson = CommonUtil.asJsonStr(((InputRecordField) recordField).getName(),
                                ((InputRecordField) recordField).getValue());
                        if (inputJson != null) {
                            records.add(inputJson.substring(1, inputJson.length() - 1));
                        }
                    }
                }
                if (!records.isEmpty()) {
                    return String.format("var inputs={%s};", String.join(",", records));
                } else {
                    String inputJson = CommonUtil.asJsonStr(input.getId(), value);
                    if (inputJson != null) {
                        elements.add(inputJson.substring(1, inputJson.length() - 1));
                    }
                }
            } else {
                String inputJson = CommonUtil.asJsonStr(input.getId(), value);
                if (inputJson != null) {
                    elements.add(inputJson.substring(1, inputJson.length() - 1));
                }
            }
        }
        return String.format("var inputs={%s};", String.join(",", elements));
    }

    protected static String toSelfContext(Object obj) {
        return String.format("var self=%s;", CommonUtil.asJsonStr(obj));
    }

    private static String evalScript(List<String> expressionLibs, String script) throws CWLException {
        Pattern pattern = Pattern.compile("\\$\\([^\\(\\)]*(\\(.*?\\)[^\\(\\)]*)*\\)\\s*[, ]*");
        Matcher matcher = pattern.matcher(script);
        String express = null;
        JSResultWrapper r = null;
        String value = null;
        while (matcher.find()) {
            express = matcher.group().trim();
            r = JSEvaluator.evaluate(expressionLibs, express);
            if (!r.isNull()) {
                if (r.isBool()) {
                    value = String.valueOf(r.asBool());
                } else if (r.isString()) {
                    value = r.asString();
                } else if (r.isDouble()) {
                    value = String.valueOf(r.asDouble());
                } else if (r.isLong()) {
                    value = String.valueOf(r.asLong());
                } else {
                    throw new CWLException(
                            "The expression must return a bool, string or number",
                            253);
                }
            }
            script = script.replace(matcher.group(), value);
        }
        return script;
    }

    private static String buildExpressionLib(List<String> expressionLibs) {
        StringBuilder script = new StringBuilder();
        if (expressionLibs != null) {
            for (String expressionLib : expressionLibs) {
                if (expressionLib.endsWith(";")) {
                    script.append(expressionLib);
                } else {
                    script.append(expressionLib + ";");
                }
            }
        }
        return script.toString();
    }

     private static String buildSingleExpr(String expr) throws CWLException {
        String script = expr.trim();
        if (script.startsWith("$") && (script.lastIndexOf("$(") == 0 || script.lastIndexOf("${") == 0)) {
            if (script.substring(1).startsWith("(") && script.substring(1).endsWith(")")) {
                script = script.substring(2, script.length() - 1).trim();
                if(script.startsWith("{") && script.endsWith("}")) {
                    return String.format("var __cwlvar=%s; __cwlvar", script);
                }
                return script;
            } else if (script.substring(1).startsWith("{") && script.substring(1).endsWith("}")) {
                return String.format("var __cwlfun=function(){%s}; __cwlfun();", script.substring(2, script.length() - 1));
            }
        }
        return null;
    }

}

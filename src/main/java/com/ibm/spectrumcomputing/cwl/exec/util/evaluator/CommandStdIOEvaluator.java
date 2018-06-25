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

import java.util.List;
import java.util.Map;

import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Utility methods for evaluating a CommandLineTool stdout/stderr/stdin expression
 */
public final class CommandStdIOEvaluator {

    private CommandStdIOEvaluator() {
    }

    /**
     * Evaluates a CommandLineTool stdout/stderr/stdin expression
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of the CommandLineTool
     * @param runtime
     *            The runtime of the CommandLineTool
     * @param inputs
     *            The inputs of the CommandLineTool
     * @param exprPlaceholder
     *            The stdout/stderr/stdin expression of CommandLineTool the object
     * @throws CWLException
     *             Failed to evaluate the expression
     */
    public static void eval(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<CommandInputParameter> inputs,
            CWLFieldValue exprPlaceholder) throws CWLException {
        if (exprPlaceholder != null) {
            String expr = exprPlaceholder.getExpression();
            if (expr != null) {
                List<String> context = JSEvaluator.constructEvalContext(jsReq);
                context.add(JSEvaluator.toRuntimeContext(runtime));
                context.add(JSEvaluator.toInputsContext(inputs));
                JSResultWrapper r = JSEvaluator.evaluate(context, expr);
                if (r.isString()) {
                    exprPlaceholder.setValue(r.asString());
                } else {
                    throw new CWLException(ResourceLoader.getMessage(
                            "cwl.expression.evaluate.failed.with.wrong.result", expr, "std", "string"),
                            253);
                }
            }
        }
    }
}

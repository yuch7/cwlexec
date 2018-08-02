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
package com.ibm.spectrumcomputing.cwl.parser;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.ExpressionTool;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * Parses CWL ExpressionTool document description file
 */
final class ExpressionToolParser extends CommandLineToolParser {

	private static final String EXPRESSION = "expression";
	private static final String BASE_COMMAND = "exit 0";

	protected static ExpressionTool yieldExpressionTool(String descTop, JsonNode node) throws CWLException {
		ExpressionTool expressionTool = null;
		List<Requirement> requirements = RequirementParser.processRequirements(descTop, REQUIREMENTS,
				node.get(REQUIREMENTS));
		// processing id
		String id = processStringField("id", node.get("id"));
		// processing inputs
		List<CommandInputParameter> inputs = toCommandInputs(descTop, node.get(INPUTS), id);
		if (inputs == null) {
			throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, INPUTS), 251);
		}
		// processing outputs
		List<CommandOutputParameter> outputs = toCommandOutputs(descTop, node.get("outputs"), id);
		if (outputs == null) {
			throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, "outputs"), 251);
		}
		// create ExpressionTool
		expressionTool = new ExpressionTool(inputs, outputs);
		expressionTool.setId(id);
		// processing requirements
		expressionTool.setRequirements(requirements);
		// processing hints
		expressionTool.setHints(RequirementParser.processHints(descTop, node.get("hints")));
		// processing label
		String label = processStringField(LABEL, node.get(LABEL));
		expressionTool.setLabel(label);
		// processing base command, "exit 0" means it could be processed by LSF
		expressionTool.setBaseCommand(Arrays.asList(BASE_COMMAND));
		// processing doc
		String doc = processStringField("doc", node.get("doc"));
		expressionTool.setDoc(doc);
		// processing expression field
		String expression = toExpression(node.get(EXPRESSION));
		expressionTool.setExpression(expression);
		// processing successCodes
		int[] successCodes = processExitCodeField("successCodes", node.get("successCodes"));
		expressionTool.setSuccessCodes(successCodes);
		return expressionTool;
	}

	private static String toExpression(JsonNode expressionNode) throws CWLException {
		String expression = null;
		if (expressionNode != null) {
			if (expressionNode.isTextual()) {
				return expressionNode.asText();
			} else {
				throw new CWLException(
						ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, EXPRESSION, "string"), 251);
			}
		}
		return expression;
	}

}

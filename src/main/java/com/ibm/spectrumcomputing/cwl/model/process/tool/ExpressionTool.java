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
package com.ibm.spectrumcomputing.cwl.model.process.tool;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;

/**
 * Represents a CWL ExpressionTool process object
 */
@JsonInclude(Include.NON_NULL)
public class ExpressionTool extends CommandLineTool {

	private String expression;
	
	public ExpressionTool(List<CommandInputParameter> inputs, List<CommandOutputParameter> outputs) {
		super(inputs, outputs);
	}

	@Override
	public String getClazz() {
		return CLASS_EXPRESSIONTOOL;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

}

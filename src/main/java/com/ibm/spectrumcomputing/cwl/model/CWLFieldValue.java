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
package com.ibm.spectrumcomputing.cwl.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a CWL field value, the value of a field can be a constant string
 * or an expression.<br>
 * e.g. For CommandLineTool#stdin field, the value of it can be a string or
 * Expression
 */
@JsonInclude(Include.NON_NULL)
public class CWLFieldValue {

    private String value;
    private String expression;

    /**
     * Returns the result value of this expression
     * 
     * @return The result value of this expression
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the result value of this expression
     * 
     * @param value
     *            The result value of this expression
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns a CWL expression
     * 
     * @return A CWL expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Sets a CWL expression
     * 
     * @param expression
     *            A CWL expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }
}

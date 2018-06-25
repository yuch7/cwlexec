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
package com.ibm.spectrumcomputing.cwl.model.process.requirement;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents InlineJavascriptRequirement
 */
@JsonInclude(Include.NON_NULL)
public class InlineJavascriptRequirement extends Requirement {

    private List<String> expressionLib;

    /**
     * Always "InlineJavascriptRequirement"
     */
    @Override
    public String getClazz() {
        return "InlineJavascriptRequirement";
    }

    /**
     * Returns an additional code fragments that will also be inserted before
     * executing the expression code.
     * 
     * @return A list of expressions
     */
    public List<String> getExpressionLib() {
        return expressionLib;
    }

    /**
     * Sets an additional code fragments that will also be inserted before executing
     * the expression code.
     * 
     * @param expressionLib
     *            A list of expressions
     */
    public void setExpressionLib(List<String> expressionLib) {
        this.expressionLib = expressionLib;
    }
}

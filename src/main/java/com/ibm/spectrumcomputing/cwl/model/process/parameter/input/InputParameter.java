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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandLineBinding;

/**
 * Represents a InputParameter object for CWL Workflow process
 */
@JsonInclude(Include.NON_NULL)
public class InputParameter extends CWLParameter {

    private CommandLineBinding inputBinding;

    /**
     * Constructs a InputParameter object
     * 
     * @param id
     *            The ID of this object
     */
    public InputParameter(String id) {
        this.id = id;
    }

    /**
     * Returns the CommandLineBinding of this object
     * 
     * @return A CommandLineBinding object
     */
    public CommandLineBinding getInputBinding() {
        return inputBinding;
    }

    /**
     * Sets the CommandLineBinding of this object
     * 
     * @param inputBinding
     *            A CommandLineBinding object
     */
    public void setInputBinding(CommandLineBinding inputBinding) {
        this.inputBinding = inputBinding;
    }
}

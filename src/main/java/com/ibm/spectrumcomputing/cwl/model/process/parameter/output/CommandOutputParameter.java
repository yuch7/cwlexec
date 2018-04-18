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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandOutputBinding;

/**
 * Represents a CommandOutputParameter object
 */
@JsonInclude(Include.NON_NULL)
public class CommandOutputParameter extends CWLParameter {

    private CommandOutputBinding outputBinding;

    /**
     * Constructs a CommandOutputParameter object
     */
    public CommandOutputParameter() {
    }

    /**
     * Constructs a CommandOutputParameter object
     * 
     * @param id
     *            The ID of this object
     */
    public CommandOutputParameter(String id) {
        this.id = id;
    }

    /**
     * Returns the CommandOutputBinding of this object
     * 
     * @return A CommandOutputBinding object
     */
    public CommandOutputBinding getOutputBinding() {
        return outputBinding;
    }

    /**
     * Sets the CommandOutputBinding of this object
     * 
     * @param outputBinding
     *            A CommandOutputBinding object
     */
    public void setOutputBinding(CommandOutputBinding outputBinding) {
        this.outputBinding = outputBinding;
    }
}

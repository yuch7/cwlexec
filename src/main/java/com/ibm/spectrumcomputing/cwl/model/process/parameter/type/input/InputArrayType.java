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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.ParameterType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandLineBinding;

/**
 * Represents an input array type
 */
@JsonInclude(Include.NON_NULL)
public class InputArrayType extends CWLType {

    private ParameterType items;

    private CommandLineBinding inputBinding;

    /**
     * Constructs an InputArrayType object
     * 
     * @param items
     *            The type of the array elements
     */
    public InputArrayType(ParameterType items) {
        this.items = items;
    }

    /**
     * Returns the input binding object
     * 
     * @return A CommandLineBinding object
     */
    public CommandLineBinding getInputBinding() {
        return inputBinding;
    }

    /**
     * Sets the input binding object
     * 
     * @param inputBinding
     *            A CommandLineBinding object
     */
    public void setInputBinding(CommandLineBinding inputBinding) {
        this.inputBinding = inputBinding;
    }

    /**
     * Returns the type of the array elements.
     * 
     * @return The type of the array elements
     */
    public ParameterType getItems() {
        return items;
    }

    /**
     * Always ARRAY
     */
    @Override
    public CWLTypeSymbol getSymbol() {
        return CWLTypeSymbol.ARRAY;
    }
}

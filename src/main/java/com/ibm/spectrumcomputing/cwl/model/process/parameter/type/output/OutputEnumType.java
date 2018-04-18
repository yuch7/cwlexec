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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandOutputBinding;

/**
 * Represents an output enum type
 */
@JsonInclude(Include.NON_NULL)
public class OutputEnumType extends CWLType {

    private List<String> symbols;

    private CommandOutputBinding outputBinding;

    /**
     * Constructs an OutputEnumType object
     * 
     * @param symbols
     *            A list of symbols
     */
    public OutputEnumType(List<String> symbols) {
        this.symbols = symbols;
    }

    /**
     * Returns the output binding object
     * 
     * @return A CommandOutputBinding object
     */
    public CommandOutputBinding getOutputBinding() {
        return outputBinding;
    }

    /**
     * Sets the output binding object
     * 
     * @param outputBinding
     *            A CommandOutputBinding object
     */
    public void setOutputBinding(CommandOutputBinding outputBinding) {
        this.outputBinding = outputBinding;
    }

    /**
     * Returns a list of symbols
     * 
     * @return A list of symbols
     */
    public List<String> getSymbols() {
        return symbols;
    }

    /**
     * Always ENUM
     */
    @Override
    public CWLTypeSymbol getSymbol() {
        return CWLTypeSymbol.ENUM;
    }
}

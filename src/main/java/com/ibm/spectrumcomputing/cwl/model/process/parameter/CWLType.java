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
package com.ibm.spectrumcomputing.cwl.model.process.parameter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a CWL type
 */
@JsonInclude(Include.NON_NULL)
public abstract class CWLType {

    private String label;

    /**
     * Returns the symbol of this type
     * 
     * @return A CWL type symbol
     */
    public abstract CWLTypeSymbol getSymbol();

    /**
     * Returns a human-readable label for this type
     * 
     * @return A human-readable label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets a human-readable label for this type
     * 
     * @param label
     *            A human-readable label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Represents this CWL type object to a string
     */
    @Override
    public String toString() {
        return this.getSymbol().toString();
    }
}

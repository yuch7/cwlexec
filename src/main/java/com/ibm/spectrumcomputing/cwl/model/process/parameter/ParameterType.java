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
package com.ibm.spectrumcomputing.cwl.model.process.parameter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents the type of a parameter, the type of a parameter can be a certain
 * type or a list of types, if the type is a list of types, the parameter actual
 * type will be one of type in the list, after the parameter value was applied,
 * the actual parameter type will be determined.
 */
@JsonInclude(Include.NON_NULL)
public class ParameterType {

    private CWLType type;
    private List<CWLType> types;

    /**
     * Returns a certain type for this a parameter
     * 
     * @return A CWL type
     */
    public CWLType getType() {
        return type;
    }

    /**
     * Sets a certain type for this a parameter
     * 
     * @param type
     *            A CWL type
     */
    public void setType(CWLType type) {
        this.type = type;
    }

    /**
     * Returns a list of CWL types
     * 
     * @return A list of CWL types
     */
    public List<CWLType> getTypes() {
        return types;
    }

    /**
     * Sets a list of CWL types
     * 
     * @param types
     *            A list of CWL types
     */
    public void setTypes(List<CWLType> types) {
        this.types = types;
    }
}

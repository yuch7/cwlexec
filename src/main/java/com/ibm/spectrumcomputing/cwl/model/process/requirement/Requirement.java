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
package com.ibm.spectrumcomputing.cwl.model.process.requirement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The base class for requirements 
 */
@JsonInclude(Include.NON_NULL)
public abstract class Requirement {

    /**
     * Returns the class of a given requirement
     * 
     * @return The class of a given requirement
     */
    @JsonProperty("class")
    public abstract String getClazz();

    /**
     * Returns a string representation of this object
     */
    @Override
    public String toString() {
        return "CWLRequirement";
    }

}

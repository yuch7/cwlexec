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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents EnvVarRequirement
 */
@JsonInclude(Include.NON_NULL)
public class EnvVarRequirement extends Requirement {

    private List<EnvironmentDef> envDef;

    /**
     * Always "EnvVarRequirement"
     */
    @Override
    public String getClazz() {
        return "EnvVarRequirement";
    }

    /**
     * Constructs an EnvVarRequirement by a list of EnvironmentDef
     * 
     * @param envDef
     *            A list of EnvironmentDef
     */
    public EnvVarRequirement(List<EnvironmentDef> envDef) {
        this.envDef = envDef;
    }

    /**
     * Returns a list of EnvironmentDef
     * 
     * @return A list of EnvironmentDef
     */
    public List<EnvironmentDef> getEnvDef() {
        return this.envDef;
    }
}

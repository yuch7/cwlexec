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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;

/**
 * Represents EnvironmentDef, it defines an environment variable that hat will
 * be set in the runtime environment by the workflow platform when executing the
 * command line tool.
 */
@JsonInclude(Include.NON_NULL)
public class EnvironmentDef {

    private String envName;
    private CWLFieldValue envValue;

    /**
     * Constructs an EnvironmentDef object
     * 
     * @param envName
     *            The environment variable name
     * @param envValue
     *            The environment variable value
     */
    public EnvironmentDef(String envName, CWLFieldValue envValue) {
        if (envName == null || envName.length() == 0) {
            throw new IllegalArgumentException("The EnvironmentDef#envName is null or empty.");
        }
        if (envValue == null) {
            throw new IllegalArgumentException("The EnvironmentDef#envValue is null.");
        }
        this.envName = envName;
        this.envValue = envValue;
    }

    /**
     * Returns the environment variable name
     * 
     * @return The environment variable name
     */
    public String getEnvName() {
        return envName;
    }

    /**
     * Returns the environment variable value
     * 
     * @return The environment variable value
     */
    public CWLFieldValue getEnvValue() {
        return envValue;
    }
}

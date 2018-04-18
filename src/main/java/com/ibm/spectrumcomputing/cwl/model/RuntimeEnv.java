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
package com.ibm.spectrumcomputing.cwl.model;

/**
 * Enumerates a CWL runtime environment, like LSF, UNIX local, etc.
 */
public enum RuntimeEnv {

    /**
     * The UNIX local runtime environment
     */
    LOCAL("local"),
    /**
     * The LSF runtime environment
     */
    LSF("lsf");

    private final String name;

    private RuntimeEnv(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this runtime environment
     * 
     * @return The name of this runtime environment
     */
    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Returns a runtime environment of the specified runtime environment type
     * with the specified name.
     * 
     * @param name
     *            The name of a runtime environment, currently, it can be
     *            "local" or "lsf"
     * 
     * @return A runtime environment
     */
    public static RuntimeEnv toRuntimeEnv(String name) {
        RuntimeEnv runtime = null;
        if (name != null && name.length() != 0) {
            for (RuntimeEnv p : RuntimeEnv.values()) {
                if (p.name.equals(name)) {
                    runtime = p;
                    break;
                }
            }
        }
        return runtime;
    }
}

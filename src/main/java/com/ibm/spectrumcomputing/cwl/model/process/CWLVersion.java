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
package com.ibm.spectrumcomputing.cwl.model.process;

/**
 * Represents the CWL version
 */
public enum CWLVersion {
    /**
     * draft-3
     */
    DRAFT3("draft-3", 0),
    /**
     * v1.0
     */
    V10("v1.0", 1000);

    private String symbol;
    private int version;

    private CWLVersion(String symbol, int version) {
        this.symbol = symbol;
        this.version = version;
    }

    @Override
    public String toString() {
        return this.symbol;
    }

    /**
     * Returns an integer symbol for a given version
     * 
     * @return An integer symbol
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns a string symbol for a given version
     * 
     * @return A string symbol
     */
    public String getSymbol() {
        return this.symbol;
    }
}

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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.binding;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;

/**
 * Represents a OutputBindingGlob object, it can contain a glob pattern, a list
 * of glob patterns, or a glob expression. <br>
 * If an array is provided, find files that match any pattern in the array. <br>
 * If an expression is provided, the expression must return a string or an array
 * of strings, which will then be evaluated as one or more glob patterns.
 */
@JsonInclude(Include.NON_NULL)
public class OutputBindingGlob {

    private List<String> patterns;
    private CWLFieldValue globExpr;
    private int scatterIndex = -1;

    /**
     * Returns a list of glob patterns
     * 
     * @return A list of glob patterns
     */
    public List<String> getPatterns() {
        return patterns;
    }

    /**
     * Sets a list of glob patterns
     * 
     * @param patterns
     *            A list of glob patterns
     */
    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    /**
     * Returns a glob pattern or a glob expression
     * 
     * @return A glob pattern or a glob expression
     */
    public CWLFieldValue getGlobExpr() {
        return globExpr;
    }

    /**
     * Sets a glob pattern or a glob expression
     * 
     * @param globExpr
     *            A glob pattern or a glob expression
     */
    public void setGlobExpr(CWLFieldValue globExpr) {
        this.globExpr = globExpr;
    }

    /**
     * @return The index of the scatter
     */
    public int getScatterIndex() {
        return scatterIndex;
    }

    /**
     * If the output is from a scatter, sets its index
     * 
     * @param scatterIndex
     *            The index of the scatter
     */
    public void setScatterIndex(int scatterIndex) {
        this.scatterIndex = scatterIndex;
    }
}

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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.binding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;

/**
 * Represents a CommandOutputBinding object
 */
@JsonInclude(Include.NON_NULL)
public class CommandOutputBinding {

    private OutputBindingGlob glob;
    @JsonInclude(Include.NON_DEFAULT)
    private boolean loadContents;
    private CWLFieldValue outputEval;

    /**
     * Returns an OutputBindingGlob object
     * 
     * @return An OutputBindingGlob object
     */
    public OutputBindingGlob getGlob() {
        return glob;
    }

    /**
     * Sets an OutputBindingGlob object
     * 
     * @param glob
     *            An OutputBindingGlob object
     */
    public void setGlob(OutputBindingGlob glob) {
        this.glob = glob;
    }

    /**
     * If the output is a File, uses this flag to mark to read up to the first
     * 64 KiB of text from the file and place it in the "contents" field of the
     * file object for use by expressions.
     * 
     * @return A flag to indicate to load file contents or not
     */
    public boolean isLoadContents() {
        return loadContents;
    }

    /**
     * If the output is a File, sets a flag to indicate to load file contents or
     * not
     * 
     * @param loadContents
     *            A flag to indicate to load file contents or not
     */
    public void setLoadContents(boolean loadContents) {
        this.loadContents = loadContents;
    }

    /**
     * Returns an expression, evaluates the expression to generate the output
     * value.
     * 
     * @return An outputEval expression
     */
    public CWLFieldValue getOutputEval() {
        return outputEval;
    }

    /**
     * Sets an expression, evaluates the expression to generate the output
     * value.
     * 
     * @param outputEval
     *            An outputEval expression
     */
    public void setOutputEval(CWLFieldValue outputEval) {
        this.outputEval = outputEval;
    }
}

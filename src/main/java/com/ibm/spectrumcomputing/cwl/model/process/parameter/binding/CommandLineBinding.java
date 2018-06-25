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
 * Represents a CommandLineBinding object
 */
@JsonInclude(Include.NON_NULL)
public class CommandLineBinding {

    @JsonInclude(Include.NON_DEFAULT)
    private boolean loadContents;
    @JsonInclude(Include.NON_DEFAULT)
    private int position;
    private String prefix;
    @JsonInclude(Include.NON_DEFAULT)
    private boolean separate = true;
    private String itemSeparator;
    private CWLFieldValue valueFrom;
    @JsonInclude(Include.NON_DEFAULT)
    private boolean shellQuote = true;

    /**
     * If the parameter is a File, uses this flag to mark to read up to the
     * first 64 KiB of text from the file and place it in the "contents" field
     * of the file object for use by expressions.
     * 
     * @return A flag to indicate to load file contents or not
     */
    public boolean isLoadContents() {
        return loadContents;
    }

    /**
     * If the parameter is a File, sets a flag to indicate to load file contents
     * or not
     * 
     * @param loadContents
     *            A flag to indicate to load file contents or not
     */
    public void setLoadContents(boolean loadContents) {
        this.loadContents = loadContents;
    }

    /**
     * Returns the parameter position, default position is 0.
     * 
     * @return The parameter position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the parameter position
     * 
     * @param position
     *            The parameter position
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Returns a prefix, it will be added before the parameter value.
     * 
     * @return A prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets a prefix, it will be added before the parameter value.
     * 
     * @param prefix
     *            A prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns a flag to indicate how to add the prefix
     * 
     * @return If true (default), then the prefix and value must be added as
     *         separate command line arguments; if false, prefix and value must
     *         be concatenated into a single command line argument.
     */
    public boolean isSeparate() {
        return separate;
    }

    /**
     * Sets a flag to indicate how to add the prefix
     * 
     * @param separate
     *            A flag to separate prefix and value
     */
    public void setSeparate(boolean separate) {
        this.separate = separate;
    }

    /**
     * Returns an item separator, it will used to join the array elements into a
     * single string with the elements separated by this separator
     * 
     * @return An item separator
     */
    public String getItemSeparator() {
        return itemSeparator;
    }

    /**
     * Sets an item separator
     * 
     * @param itemSeparator
     *            An item separator
     */
    public void setItemSeparator(String itemSeparator) {
        this.itemSeparator = itemSeparator;
    }

    /**
     * Returns a valueFrom object
     * 
     * @return A valueFrom object
     */
    public CWLFieldValue getValueFrom() {
        return valueFrom;
    }

    /**
     * Sets a valueFrom object
     * 
     * @param valueFrom
     *            A valueFrom object
     */
    public void setValueFrom(CWLFieldValue valueFrom) {
        this.valueFrom = valueFrom;
    }

    /**
     * Returns a flag to indicate to quote the parameter or not, default is true
     * 
     * @return A shell quote flag
     */
    public boolean isShellQuote() {
        return shellQuote;
    }

    /**
     * Sets a flag to indicate to quote the parameter or not
     * 
     * @param shellQuote
     *            A shell quote flag
     */
    public void setShellQuote(boolean shellQuote) {
        this.shellQuote = shellQuote;
    }

    /**
     * Determine a CommandLineBinding is an empty object
     * 
     * @return True, this object is empty
     */
    public boolean isEmpty() {
        boolean isEmpty = false;
        if (this.position == 0 &&
                this.prefix == null &&
                this.valueFrom == null &&
                this.itemSeparator == null &&
                !this.loadContents &&
                this.separate &&
                this.shellQuote) {
            isEmpty = true;
        }
        return isEmpty;
    }
}

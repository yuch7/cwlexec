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
package com.ibm.spectrumcomputing.cwl.model.process.requirement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;

/**
 * Represents a Dirent object
 */
@JsonInclude(Include.NON_NULL)
public class Dirent {

    private CWLFieldValue entry;
    private CWLFieldValue entryname;
    private boolean writable;

    /**
     * Construncts a Dirent object by an entry value
     * 
     * @param entry
     *            A value of entry
     */
    public Dirent(CWLFieldValue entry) {
        this.entry = entry;
    }

    /**
     * Returns a value of entry, the value will used as the contents of a file
     * 
     * @return A value of entry
     */
    public CWLFieldValue getEntry() {
        return entry;
    }

    /**
     * Returns a name of entry, it represents the name of the file or
     * subdirectory to create in the output directory
     * 
     * @return The name of entry.
     */
    public CWLFieldValue getEntryname() {
        return entryname;
    }

    /**
     * Sets the name of entry
     * 
     * @param entryname
     *            The name of entry
     */
    public void setEntryname(CWLFieldValue entryname) {
        this.entryname = entryname;
    }

    /**
     * Returns a flag to mark the file or directory is writable or not
     * 
     * @return If true, the file or directory must be writable by the tool.
     */
    public boolean isWritable() {
        return writable;
    }

    /**
     * Sets a flag to mark the file or directory is writable or not
     * 
     * @param writable
     *            If true, the file or directory must be writable by the tool.
     */
    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    /**
     * Always "Dirent"
     * 
     * @return Always "Dirent"
     */
    @JsonProperty("class")
    public String getClazz() {
        return "Dirent";
    }

}

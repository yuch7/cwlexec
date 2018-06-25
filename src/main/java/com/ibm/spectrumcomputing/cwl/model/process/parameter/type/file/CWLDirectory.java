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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a CWLDirectory object
 */
@JsonInclude(Include.NON_NULL)
public class CWLDirectory extends CWLFileBase {

    private List<CWLFileBase> listing;

    /**
     * Returns the list of files or subdirectories contained in this directory.
     * 
     * @return A list of files or subdirectories
     */
    public List<CWLFileBase> getListing() {
        return listing;
    }

    /**
     * Sets the list of files or subdirectories contained in this directory.
     * 
     * @param listing
     *            A list of files or subdirectories
     */
    public void setListing(List<CWLFileBase> listing) {
        this.listing = listing;
    }

    /**
     * Always "Directory"
     */
    @Override
    public String getClazz() {
        return "Directory";
    }
}

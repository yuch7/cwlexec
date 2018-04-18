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
package com.ibm.spectrumcomputing.cwl.model.process.parameter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;

/**
 * Represents a format of a file, the format preferrably defined within an
 * ontology.
 */
@JsonInclude(Include.NON_NULL)
public class FileFormat {

    private List<String> formats;
    private CWLFieldValue format;

    /**
     * Returns a list of IRIs that represents file formats
     * 
     * @return A list of IRIs
     */
    public List<String> getFormats() {
        return formats;
    }

    /**
     * Sets a list of IRIs that represents file formats
     * 
     * @param formats
     *            A list of IRIs
     */
    public void setFormats(List<String> formats) {
        this.formats = formats;
    }

    /**
     * Returns a string or expression that represents file format
     * 
     * @return A CWLFieldValue object
     */
    public CWLFieldValue getFormat() {
        return format;
    }

    /**
     * Sets a string or expression that represents file format
     * 
     * @param format
     *            A CWLFieldValue object
     */
    public void setFormat(CWLFieldValue format) {
        this.format = format;
    }
}

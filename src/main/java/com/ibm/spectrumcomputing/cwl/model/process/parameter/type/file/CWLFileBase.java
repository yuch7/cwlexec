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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a CWLFileBase object, extends by {@link CWLFile} and
 * {@link CWLDirectory}
 */
@JsonInclude(Include.NON_NULL)
public abstract class CWLFileBase {

    private String location;
    private String path;
    private String basename;
    private String srcPath;

    /**
     * Returns an IRI that identifies the file resource.
     * 
     * @return A file location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets an IRI that identifies the file resource.
     * 
     * @param location
     *            A file location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns a local host path where the File is available when a
     * CommandLineTool is executed.
     * 
     * @return A file path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets a local host path where the File is available when a CommandLineTool
     * is executed.
     * 
     * @param path
     *            A file path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the base name of a file
     * 
     * @return The base name of a file
     */
    public String getBasename() {
        return basename;
    }

    /**
     * Sets the base name of a file
     * 
     * @param basename
     *            The base name of a file
     */
    public void setBasename(String basename) {
        this.basename = basename;
    }

    /**
     * Returns a file path, it represents the source file path, this value will
     * be set after the file was copied to a shared working directory
     * 
     * @return A file path
     */
    public String getSrcPath() {
        return srcPath;
    }

    /**
     * Sets the source file path after the file was copied to a shared working
     * directory
     * 
     * @param srcPath
     *            A file path
     */
    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    /**
     * Returns the class of the object
     * 
     * @return The class of the object
     */
    @JsonProperty("class")
    public abstract String getClazz();

    /**
     * Represents the file object to a string
     */
    @Override
    public String toString() {
        return this.getClazz() + ":" + this.path;
    }
}

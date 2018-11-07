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
package com.ibm.spectrumcomputing.cwl.model.process.parameter;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;

/**
 * Represents a base CWL parameter
 */
@JsonInclude(Include.NON_NULL)
public abstract class CWLParameter {

    protected String id;
    private String label;
    private List<CWLFieldValue> secondaryFiles;
    @JsonInclude(Include.NON_DEFAULT)
    private boolean streamable;
    private List<String> doc;
    private FileFormat format;
    private ParameterType type;
    @XmlElement(name = "default")
    private Object defaultValue;
    // The actual value after evaluation
    private Object value;
    // only for scatter case, refer to issue #36
    private Object self;
    private String delayedValueFromExpr;

    /**
     * Returns the ID of this parameter
     * 
     * @return An ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns a human-readable label of this object
     * 
     * @return A human-readable label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets a human-readable label of this object
     * 
     * @param label
     *            A human-readable label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * If the parameter is a file, returns a list of patterns or expressions
     * specifying files or directories that must be included alongside the
     * primary file
     * 
     * @return A list of patterns or expressions
     */
    public List<CWLFieldValue> getSecondaryFiles() {
        return secondaryFiles;
    }

    /**
     * If the parameter is a file, sets a list of patterns or expressions
     * specifying files or directories that must be included alongside the
     * primary file
     * 
     * @param secondaryFiles
     *            A list of patterns or expressions
     */
    public void setSecondaryFiles(List<CWLFieldValue> secondaryFiles) {
        this.secondaryFiles = secondaryFiles;
    }

    /**
     * If the parameter is a file, returns a flag indicates that the file can be
     * read or written sequentially without seeking or not.
     * 
     * @return If true, the file can be read or written sequentially without
     *         seeking
     */
    public boolean isStreamable() {
        return streamable;
    }

    /**
     * If the parameter is a file, sets a flag indicates that the file can be
     * read or written sequentially without seeking or not.
     * 
     * @param streamable
     *            If true, the file can be read or written sequentially without
     *            seeking
     */
    public void setStreamable(boolean streamable) {
        this.streamable = streamable;
    }

    /**
     * Returns a list of documentation strings which should be concatenated.
     * 
     * @return A list of documentation strings
     */
    public List<String> getDoc() {
        return doc;
    }

    /**
     * Sets a list of documentation strings which should be concatenated.
     * 
     * @param doc
     *            A list of documentation strings
     */
    public void setDoc(List<String> doc) {
        this.doc = doc;
    }

    /**
     * If the parameter is a file, returns the format of it
     * 
     * @return A file format
     */
    public FileFormat getFormat() {
        return format;
    }

    /**
     * If the parameter is a file, sets the format of it
     * 
     * @param format
     *            A file format
     */
    public void setFormat(FileFormat format) {
        this.format = format;
    }

    /**
     * Returns the type of this parameter
     * 
     * @return A type of a parameter
     */
    public ParameterType getType() {
        return type;
    }

    /**
     * Sets the type of this parameter
     * 
     * @param type
     *            A type of a parameter
     */
    public void setType(ParameterType type) {
        this.type = type;
    }

    /**
     * Returns the default value of this parameter if the parameter is missing
     * from the input object, or if the value of the parameter in the input
     * object is null
     * 
     * @return A default value
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default value of this parameter if the parameter is missing from
     * the input object, or if the value of the parameter in the input object is
     * null
     * 
     * @param defaultValue
     *            A default value
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the value of this parameter
     * 
     * @return A value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of this parameter
     * 
     * @param value
     *            A value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    public Object getSelf() {
        return self;
    }

    public void setSelf(Object self) {
        this.self = self;
    }

    public String getDelayedValueFromExpr() {
        return delayedValueFromExpr;
    }

    public void setDelayedValueFromExpr(String valueFromExpr) {
        this.delayedValueFromExpr = valueFromExpr;
    }
}

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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.input;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.LinkMergeMethod;

/**
 * Represents a WorkflowStepInput object
 */
@JsonInclude(Include.NON_NULL)
public class WorkflowStepInput {

    private String id;
    private List<String> source;
    private LinkMergeMethod linkMerge = LinkMergeMethod.MERGE_NESTED;
    @XmlElement(name = "default")
    private Object defaultValue;
    private CWLFieldValue valueFrom;

    /**
     * Constructs a WorkflowStepInput object
     * 
     * @param id
     *            The ID of this object
     */
    public WorkflowStepInput(String id) {
        this.id = id;
    }

    /**
     * Returns one or more workflow parameters that will provide input to the
     * underlying step parameter.
     * 
     * @return A list of sources
     */
    public List<String> getSource() {
        return source;
    }

    /**
     * Sets one or more workflow parameters that will provide input to the
     * underlying step parameter.
     * 
     * @param source
     *            A list of sources
     */
    public void setSource(List<String> source) {
        this.source = source;
    }

    /**
     * Returns the method to use to merge multiple inbound links into a single
     * array. If not specified, the default method is "merge_nested".
     * 
     * @return A LinkMergeMethod object
     */
    public LinkMergeMethod getLinkMerge() {
        return linkMerge;
    }

    /**
     * Sets the method to use to merge multiple inbound links into a single
     * array.
     * 
     * @param linkMerge
     *            A LinkMergeMethod object
     */
    public void setLinkMerge(LinkMergeMethod linkMerge) {
        this.linkMerge = linkMerge;
    }

    /**
     * Returns the default value for this parameter to use if either there is no
     * source field, or the value produced by the source is null.
     * 
     * @return The default value of this parameter
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default value for this parameter to use if either there is no
     * source field, or the value produced by the source is null.
     * 
     * @param defaultValue
     *            The default value for this parameter
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the valueFrom of this parameter, the value of valueFrom will used
     * as the final value for input parameter
     * 
     * @return The valueFrom of this parameter
     */
    public CWLFieldValue getValueFrom() {
        return valueFrom;
    }

    /**
     * Sets the valueFrom for this parameter, the value of valueFrom will used
     * as the final value for input parameter
     * 
     * @param valueFrom
     *            The valueFrom for this parameter
     */
    public void setValueFrom(CWLFieldValue valueFrom) {
        this.valueFrom = valueFrom;
    }

    /**
     * Returns the ID of this object
     * 
     * @return An ID of this object
     */
    public String getId() {
        return id;
    }
}

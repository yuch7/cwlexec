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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.output;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandOutputBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.LinkMergeMethod;

/**
 * Represents a WorkflowOutputParameter object
 */
@JsonInclude(Include.NON_NULL)
public class WorkflowOutputParameter extends CWLParameter {

    private CommandOutputBinding outputBinding;
    private List<String> outputSource;
    private LinkMergeMethod linkMerge = LinkMergeMethod.MERGE_NESTED;

    /**
     * Constructs a WorkflowOutputParameter object
     */
    public WorkflowOutputParameter() {
    }

    /**
     * Constructs a WorkflowOutputParameter object
     * 
     * @param id
     *            The ID of this object
     */
    public WorkflowOutputParameter(String id) {
        this.id = id;
    }

    /**
     * Returns the CommandOutputBinding of this object
     * 
     * @return A CommandOutputBinding object
     */
    public CommandOutputBinding getOutputBinding() {
        return outputBinding;
    }

    /**
     * Sets the CommandOutputBinding of this object
     * 
     * @param outputBinding
     *            A CommandOutputBinding object
     */
    public void setOutputBinding(CommandOutputBinding outputBinding) {
        this.outputBinding = outputBinding;
    }

    /**
     * Returns one or more workflow parameters that supply the value of to the
     * output parameter.
     * 
     * @return A list of output sources
     */
    public List<String> getOutputSource() {
        return outputSource;
    }

    /**
     * Sets one or more workflow parameters that supply the value of to the
     * output parameter.
     * 
     * @param outputSource
     *            A list of output sources
     */
    public void setOutputSource(List<String> outputSource) {
        this.outputSource = outputSource;
    }

    /**
     * Returns the method to use to merge multiple sources into a single array.
     * If not specified, the default method is "merge_nested".
     * 
     * @return The link merge method
     */
    public LinkMergeMethod getLinkMerge() {
        return linkMerge;
    }

    /**
     * Sets the method to use to merge multiple sources into a single array.
     * 
     * @param linkMerge
     *            The link merge method
     */
    public void setLinkMerge(LinkMergeMethod linkMerge) {
        this.linkMerge = linkMerge;
    }
}

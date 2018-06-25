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
package com.ibm.spectrumcomputing.cwl.model.process.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.WorkflowStepInput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.ScatterMethod;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowStepOutput;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;

/**
 * Represents a CWL WorkflowStep process object
 */
@JsonInclude(Include.NON_NULL)
public class WorkflowStep {

    private String id;
    private List<WorkflowStepInput> in;
    private List<WorkflowStepOutput> out;
    private String runId;
    private CWLProcess run;
    private List<Requirement> requirements;
    private List<Requirement> hints;
    private String label;
    private String doc;
    private List<String> scatter;
    private ScatterMethod scatterMethod;
    // Only as a dependency indicator
    private List<String> dependencies;

    /**
     * Constructs a CWL WorkflowStep process object
     * 
     * @param id
     *            The unique identifier for this workflow step object
     * @param in
     *            The input parameters of the workflow step object. The process is
     *            ready to run when all required input parameters are associated
     *            with concrete values.
     * @param out
     *            The parameters representing the output of the process.
     */
    public WorkflowStep(String id, List<WorkflowStepInput> in, List<WorkflowStepOutput> out) {
        this.id = id;
        this.in = in;
        this.out = out;
    }

    /**
     * Returns the ID of this workflow step object
     * 
     * @return The ID of this workflow step object
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the inputs of this workflow step object
     * 
     * @return The inputs of this workflow step object
     */
    public List<WorkflowStepInput> getIn() {
        return in;
    }

    /**
     * Returns the outputs of this workflow step object
     * 
     * @return The outputs of this workflow step object
     */
    public List<WorkflowStepOutput> getOut() {
        return out;
    }

    /**
     * Returns the ID of the CWL run process object
     * 
     * @return An ID of the CWL run process object
     */
    public String getRunId() {
        return runId;
    }

    /**
     * Sets the ID of the CWL run process object
     * 
     * @param runId
     *            An ID of the CWL run process object
     */
    public void setRunId(String runId) {
        this.runId = runId;
    }

    /**
     * Returns the CWL run process object
     * 
     * @return A CWL process object
     */
    public CWLProcess getRun() {
        return run;
    }

    /**
     * Sets a given CWL process object for this step to run
     * 
     * @param run
     *            A given CWL process object
     */
    public void setRun(CWLProcess run) {
        this.run = run;
    }

    /**
     * Returns the requirements of this step
     * 
     * @return A list of requirements
     */
    public List<Requirement> getRequirements() {
        return requirements;
    }

    /**
     * Sets the requirements for this step
     * 
     * @param requirements
     *            A list of requirements
     */
    public void setRequirements(List<Requirement> requirements) {
        this.requirements = requirements;
    }

    /**
     * Returns the hints of this step
     * 
     * @return A list of hints
     */
    public List<Requirement> getHints() {
        return hints;
    }

    /**
     * Sets the hints for this step
     * 
     * @param hints
     *            A list of hints
     */
    public void setHints(List<Requirement> hints) {
        this.hints = hints;
    }

    /**
     * Returns a human-readable label of this process object
     * 
     * @return A label of this process object
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets a human-readable label of this process object
     * 
     * @param label
     *            A human-readable label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns a human-readable description of this process object
     * 
     * @return A description of this process object
     */
    public String getDoc() {
        return doc;
    }

    /**
     * Sets a human-readable description of this process object
     * 
     * @param doc
     *            A human-readable description
     */
    public void setDoc(String doc) {
        this.doc = doc;
    }

    /**
     * Returns a "scatter" list, it specifies that the associated workflow step or
     * subworkflow should execute separately over a list of input elements. Each job
     * making up a scatter operation is independent and be executed concurrently.
     * 
     * @return A list of scatter inputs
     */
    public List<String> getScatter() {
        return scatter;
    }

    /**
     * Sets a "scatter" list, it specifies that the associated workflow step or
     * subworkflow should execute separately over a list of input elements. Each job
     * making up a scatter operation is independent and be executed concurrently.
     * 
     * @param scatter
     *            A list of scatter inputs
     */
    public void setScatter(List<String> scatter) {
        this.scatter = scatter;
    }

    /**
     * If scatter is an array of more than one element, returns the scatter method
     * 
     * @return A scatter method
     */
    public ScatterMethod getScatterMethod() {
        return scatterMethod;
    }

    /**
     * If scatter is an array of more than one element, sets a scatter method
     * 
     * @param scatterMethod
     *            A scatter method
     */
    public void setScatterMethod(ScatterMethod scatterMethod) {
        this.scatterMethod = scatterMethod;
    }

    /**
     * Returns the step input source list of this step
     * 
     * @return A list of dependencies
     */
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the step input source list  for this step
     * 
     * @param dependencies
     *            A list of dependencies
     */
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

}

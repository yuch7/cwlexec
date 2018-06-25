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
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.InputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowOutputParameter;

/**
 * Represents a CWL Workflow process object
 */
@JsonInclude(Include.NON_NULL)
public class Workflow extends CWLProcess {

    private List<InputParameter> inputs;
    private List<WorkflowOutputParameter> outputs;
    private List<WorkflowStep> steps;

    /**
     * Constructs a CWL Workflow process object
     * 
     * @param inputs
     *            The inputs of this process object. The process is ready to run
     *            when all required input parameters are associated with concrete
     *            values.
     * @param outputs
     *            The outputs of this process object
     * @param steps
     *            The individual steps that make up the workflow. Each step is
     *            executed when all of its input data links are fufilled.
     */
    public Workflow(List<InputParameter> inputs, List<WorkflowOutputParameter> outputs, List<WorkflowStep> steps) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.steps = steps;
    }

    /**
     * Always Workflow
     */
    @Override
    public String getClazz() {
        return CLASS_WORKFLOW;
    }

    /**
     * Returns the inputs of this process object
     */
    @Override
    public List<InputParameter> getInputs() {
        return this.inputs;
    }

    /**
     * Returns the outputs of this process object
     */
    @Override
    public List<WorkflowOutputParameter> getOutputs() {
        return outputs;
    }

    /**
     * Returns the steps of this process object
     * 
     * @return A list of steps
     */
    public List<WorkflowStep> getSteps() {
        return steps;
    }

}

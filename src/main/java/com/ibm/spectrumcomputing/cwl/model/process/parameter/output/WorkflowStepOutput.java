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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a WorkflowStepOutput object
 */
@JsonInclude(Include.NON_NULL)
public class WorkflowStepOutput {

    private String id;

    /**
     * Constructs a WorkflowStepOutput object
     * 
     * @param id
     *            A unique identifier for this workflow output parameter. This
     *            is the identifier to use in the source field of
     *            WorkflowStepInput to connect the output value to downstream
     *            parameters.
     */
    public WorkflowStepOutput(String id) {
        this.id = id;
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

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
package com.ibm.spectrumcomputing.cwl.model.instance;

import java.util.ArrayList;
import java.util.List;

import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;

/**
 * Represents a CWL Workflow instance
 */
public class CWLWorkflowInstance extends CWLInstance {

    private final List<CWLInstance> instances = new ArrayList<>();

    /**
     * Constructs a CWL Workflow instance if the corresponding process is a main
     * process
     * 
     * @param id
     *            An UUID ID for this instance
     * @param owner
     *            The owner of this instance
     * @param workflow
     *            The CWL Workflow
     * @param flowExecConf
     *            The execution configurations, can be null
     */
    public CWLWorkflowInstance(String id,
            String owner,
            Workflow workflow,
            FlowExecConf flowExecConf) {
        super(id, owner, workflow, null, flowExecConf);
    }

    /**
     * Constructs a CWL Workflow instance if the corresponding process is a step
     * process
     * 
     * @param id
     *            An UUID ID for this instance
     * @param owner
     *            The owner of this instance
     * @param workflow
     *            The CWL Workflow
     * @param parent
     *            The parent instance of this step
     */
    public CWLWorkflowInstance(String id,
            String owner,
            Workflow workflow,
            CWLWorkflowInstance parent) {
        super(id, owner, workflow, parent, null);
    }

    /**
     * Returns all of step instances for this instance
     * 
     * @return All of step instances
     */
    public List<CWLInstance> getInstances() {
        return instances;
    }

    /**
     * Add a step instance for this instance
     * 
     * @param instance
     *            A step instance
     */
    public void addInstance(CWLInstance instance) {
        this.instances.add(instance);
    }
}

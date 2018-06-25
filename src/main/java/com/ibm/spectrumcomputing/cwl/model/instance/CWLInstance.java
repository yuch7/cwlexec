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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;

/**
 * The base class for {@link CWLCommandInstance} and {@link CWLWorkflowInstance}
 *
 */
@JsonInclude(Include.NON_NULL)
public abstract class CWLInstance {

    private final String id;
    private final String owner;
    private final CWLProcess process;
    private final CWLWorkflowInstance parent;
    private final FlowExecConf flowExecConf;

    private String name;

    private Map<String, String> runtime;
    private RuntimeEnv runtimeEnv;

    private Long startTime;
    private Long endTime;

    private volatile CWLInstanceState state = CWLInstanceState.WAITING;

    private boolean readyToRun = false;
    private boolean isMain = true;
    private boolean finished = false;

    private int exitCode = -1;

    private WorkflowStep step;

    /**
     * Constructs a CWL process instance
     * 
     * @param id
     *            An UUID ID for this instance
     * @param owner
     *            The owner of this instance
     * @param process
     *            The CWL process of this instance
     * @param parent
     *            If the instance is a step, the parent of this instance
     * @param flowExecConf
     *            The execution configuration of this instance
     */
    public CWLInstance(String id,
            String owner,
            CWLProcess process,
            CWLWorkflowInstance parent,
            FlowExecConf flowExecConf) {
        this.id = id;
        this.owner = owner;
        this.process = process;
        this.parent = parent;
        this.flowExecConf = flowExecConf;
    }

    /**
     * Returns the ID of this instance
     * 
     * @return The ID of this instance
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the owner of this instance
     * 
     * @return The owner of this instance
     */
    public String getOwner() {
        return owner;
    }

    /**
     * If this instance, returns the parent instance of it
     * 
     * @return A CWL Workflow instance
     */
    public CWLWorkflowInstance getParent() {
        return parent;
    }

    /**
     * Returns the execution configuration for this instance
     * 
     * @return The execution configuration
     */
    public FlowExecConf getFlowExecConf() {
        return flowExecConf;
    }

    /**
     * Returns the CWL process of this instance
     * 
     * @return The CWL process of this instance
     */
    public CWLProcess getProcess() {
        return process;
    }

    /**
     * Returns the name of this instance
     * 
     * @return The name of this instance
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this instance
     * 
     * @param name
     *            The name of this instance
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the runtime of this instance
     * 
     * @return The runtime of this instance
     */
    public Map<String, String> getRuntime() {
        return runtime;
    }

    /**
     * Sets the runtime of this instance
     * 
     * @param runtime
     *            The runtime of this instance
     */
    public void setRuntime(Map<String, String> runtime) {
        this.runtime = runtime;
    }

    /**
     * Returns the runtime environment of this instance
     * 
     * @return The runtime environment of this instance
     */
    public RuntimeEnv getRuntimeEnv() {
        return runtimeEnv;
    }

    /**
     * Sets the runtime environment of this instance
     * 
     * @param runtimeEnv
     *            The runtime environment of this instance
     */
    public void setRuntimeEnv(RuntimeEnv runtimeEnv) {
        this.runtimeEnv = runtimeEnv;
    }

    /**
     * Returns the start time of this instance
     * 
     * @return The start time of this instance
     */
    public Long getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time of this instance
     * 
     * @param startTime
     *            The start time of this instance
     */
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the end time of this instance
     * 
     * @return The end time of this instance
     */
    public Long getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time of this instance
     * 
     * @param endTime
     *            The end time of this instance
     */
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    /**
     * Returns the state of this instance
     * 
     * @return The state of this instance
     */
    public synchronized CWLInstanceState getState() {
        return state;
    }

    /**
     * Sets the state of this instance
     * 
     * @param state
     *            The state of this instance
     */
    public synchronized void setState(CWLInstanceState state) {
        this.state = state;
    }

    /**
     * Returns a flag to mark this instance is ready to run, the all of
     * dependent inputs of this instance are resolved
     * 
     * @return True, the instance is ready to run
     */
    public boolean isReadyToRun() {
        return readyToRun;
    }

    /**
     * After all of dependent inputs of this instance are resolved, mark this
     * instance to ready to run
     * 
     * @param readyToRun
     *            A flag to mark this instance is ready to run
     */
    public void setReadyToRun(boolean readyToRun) {
        this.readyToRun = readyToRun;
    }

    /**
     * Returns a flag to mark this instance is a CWL main process
     * 
     * @return True, this instance is a CWL main process
     */
    public boolean isMain() {
        return isMain;
    }

    /**
     * Mark this instance is a CWL main process
     * 
     * @param isMain
     *            True, this instance is a CWL main process
     */
    public void setMain(boolean isMain) {
        this.isMain = isMain;
    }

    /**
     * Returns a flag to mark this instance is finished
     * 
     * @return True, the instance is finished
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Mark this instance is finished
     * 
     * @param finished
     *            True, the instance is finished
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Returns the exit code of this instance
     * 
     * @return The exit code of this instance
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Sets the exit code of this instance
     * 
     * @param exitCode
     *            The exit code of this instance
     */
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * If this instance is a CWL Workflow step (a CommandLineTool or
     * SubWorkflow) instance, returns the step
     * 
     * @return A CWL Workflow step
     */
    public WorkflowStep getStep() {
        return step;
    }

    /**
     * Sets the CWL Workflow step for a CWL Workflow step (a CommandLineTool or
     * SubWorkflow) instance
     * 
     * @param step
     *            A CWL Workflow step
     */
    public void setStep(WorkflowStep step) {
        this.step = step;
    }
}

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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutionResult;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.ScatterMethod;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;

/**
 * Represents a CWL CommandLineTool instance
 */
@JsonInclude(Include.NON_NULL)
public class CWLCommandInstance extends CWLInstance {

    // HPC job id
    private volatile long hpcJobId = -1L;

    private List<String> commands;
    private CommandExecutionResult executionResult;

    private boolean emptyScatter;
    private List<String> scatter;
    private ScatterMethod scatterMethod;
    private List<CWLScatterHolder> scatterHolders;

    /**
     * Constructs a CWL CommandLineTool instance if the corresponding process is
     * a main process
     * 
     * @param id
     *            An UUID for this instance
     * @param owner
     *            The owner of this instance
     * @param commandLineTool
     *            The CWL CommandLineTool
     * @param flowExecConf
     *            The execution configurations, can be null
     */
    public CWLCommandInstance(String id,
            String owner,
            CommandLineTool commandLineTool,
            FlowExecConf flowExecConf) {
        super(id, owner, commandLineTool, null, flowExecConf);
    }

    /**
     * Constructs a CWL CommandLineTool instance if the corresponding process is
     * a step process
     * 
     * @param id
     *            An UUID for this instance
     * @param owner
     *            The owner of this instance
     * @param commandLineTool
     *            The CWL CommandLineTool
     * @param parent
     *            The parent of this step
     */
    public CWLCommandInstance(String id,
            String owner,
            CommandLineTool commandLineTool,
            CWLWorkflowInstance parent) {
        super(id, owner, commandLineTool, parent, null);
    }

    /**
     * If the CommandLineTool running with a HPC platform, returns the
     * corresponding HPC job ID
     * 
     * @return The corresponding HPC job ID
     */
    public long getHPCJobId() {
        return hpcJobId;
    }

    /**
     * If the CommandLineTool running with a HPC platform, sets the
     * corresponding HPC job ID
     * 
     * @param hpcJobId
     *            The corresponding HPC job ID
     */
    public void setHPCJobId(long hpcJobId) {
        this.hpcJobId = hpcJobId;
    }

    /**
     * Returns the execution command of this instance
     * 
     * @return An execution command
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * Sets the execution command of this instance
     * 
     * @param runCommands
     *            An execution command
     */
    public void setCommands(List<String> runCommands) {
        this.commands = runCommands;
    }

    /**
     * Returns the command execution result of this instance
     * 
     * @return The command execution result of this instance
     */
    public CommandExecutionResult getExecutionResult() {
        return executionResult;
    }

    /**
     * After this instance was executed, sets the command execution result for
     * this instance
     * 
     * @param executionResult
     *            The command execution result of this instance
     */
    public void setExecutionResult(CommandExecutionResult executionResult) {
        this.executionResult = executionResult;
    }

    /**
     * If this instance needs to scatter, returns the list of the scatter
     * 
     * @return The list of the scatter
     */
    public List<String> getScatter() {
        return scatter;
    }

    /**
     * If this instance needs to scatter, sets the list of the scatter
     * 
     * @param scatter
     *            The list of the scatter
     */
    public void setScatter(List<String> scatter) {
        this.scatter = scatter;
    }

    /**
     * If this instance needs to scatter, returns the method of the scatter
     * 
     * @return The method of the scatter
     */
    public ScatterMethod getScatterMethod() {
        return scatterMethod;
    }

    /**
     * If this instance needs to scatter, sets the method of the scatter
     * 
     * @param scatterMethod
     *            The method of the scatter
     */
    public void setScatterMethod(ScatterMethod scatterMethod) {
        this.scatterMethod = scatterMethod;
    }

    /**
     * Returns a flag to mark this instance is an empty scatter
     * 
     * @return True, an empty scatter
     */
    public boolean isEmptyScatter() {
        return emptyScatter;
    }

    /**
     * Sets a flag to mark this instance is an empty scatter
     * 
     * @param emptyScatter
     *            True, an empty scatter
     */
    public void setEmptyScatter(boolean emptyScatter) {
        this.emptyScatter = emptyScatter;
    }

    /**
     * If this instance needs to scatter, returns the list of scatter holders
     * 
     * @return The list of scatter holders
     */
    public List<CWLScatterHolder> getScatterHolders() {
        return scatterHolders;
    }

    /**
     * If this instance needs to scatter, sets the list of scatter holders
     * 
     * @param scatterHolders
     *            The list of scatter holders
     */
    public void setScatterHolders(List<CWLScatterHolder> scatterHolders) {
        this.scatterHolders = scatterHolders;
    }
}

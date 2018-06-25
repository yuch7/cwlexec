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

import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;

/**
 * Holds the related inputs, command for a scattered step at runtime
 */
public class CWLScatterHolder {

    private int scatterIndex;
    private List<CommandInputParameter> inputs;
    private List<String> command;

    /**
     * Returns the index of a scatter step
     * 
     * @return The index of a scatter step
     */
    public int getScatterIndex() {
        return scatterIndex;
    }

    /**
     * Sets an index for a scatter step, the index is from 1
     * 
     * @param scatterIndex
     *            The index of a scatter step
     */
    public void setScatterIndex(int scatterIndex) {
        this.scatterIndex = scatterIndex;
    }

    /**
     * Returns the inputs of a scatter step
     * 
     * @return The inputs of a scatter step
     */
    public List<CommandInputParameter> getInputs() {
        return inputs;
    }

    /**
     * Sets the inputs for a scatter step
     * 
     * @param inputs
     *            The step inputs
     */
    public void setInputs(List<CommandInputParameter> inputs) {
        this.inputs = inputs;
    }

    /**
     * Returns the command of a scatter step
     * 
     * @return The scatter command
     */
    public List<String> getCommand() {
        return command;
    }

    /**
     * Sets the command for a scatter step
     * 
     * @param command
     *            The scatter command
     */
    public void setCommand(List<String> command) {
        this.command = command;
    }
}

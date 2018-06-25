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
package com.ibm.spectrumcomputing.cwl.model.process.tool;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandLineBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;

/**
 * Represents a CWL CommandLineTool process object
 */
@JsonInclude(Include.NON_NULL)
public class CommandLineTool extends CWLProcess {

    private final List<CommandInputParameter> inputs;
    private final List<CommandOutputParameter> outputs;
    private List<String> baseCommand;
    private List<CommandLineBinding> arguments;
    private CWLFieldValue stdin;
    private CWLFieldValue stderr;
    private CWLFieldValue stdout;
    private int[] successCodes;

    /**
     * Constructs a CWL CommandLineTool process object
     * 
     * @param inputs
     *            The inputs of this process object, The process is ready to run
     *            when all required input parameters are associated with concrete
     *            values.
     * @param outputs
     *            The outputs of this process object
     */
    public CommandLineTool(List<CommandInputParameter> inputs, List<CommandOutputParameter> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Always CommandLineTool
     */
    @Override
    public String getClazz() {
        return CLASS_COMMANDLINETOOL;
    }

    /**
     * Returns the inputs of this process object
     */
    @Override
    public List<CommandInputParameter> getInputs() {
        return inputs;
    }

    /**
     * Returns the outputs of this process object
     */
    @Override
    public List<CommandOutputParameter> getOutputs() {
        return outputs;
    }

    /**
     * Returns the baseCommand of this process object
     * 
     * @return A baseCommand
     */
    public List<String> getBaseCommand() {
        return baseCommand;
    }

    /**
     * Sets a command to execute. If an array, the first element of the array is the
     * command to execute, and subsequent elements are mandatory command line
     * arguments.
     * 
     * @param baseCommand
     *            A baseCommand
     */
    public void setBaseCommand(List<String> baseCommand) {
        this.baseCommand = baseCommand;
    }

    /**
     * Returns this list of CommandLineBinding for this process object
     * 
     * @return A list of CommandLineBinding
     */
    public List<CommandLineBinding> getArguments() {
        return arguments;
    }

    /**
     * Sets the command line bindings which are not directly associated with input
     * parameters.
     * 
     * @param arguments
     *            A list of CommandLineBinding
     */
    public void setArguments(List<CommandLineBinding> arguments) {
        this.arguments = arguments;
    }

    /**
     * Returns a CWLExpression object, the value of the CWLExpression object is a
     * path to a file whose contents must be piped into the command's standard input
     * stream.
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getStdin() {
        return stdin;
    }

    /**
     * Sets stdin for this process object, the stdin can be an expression or a
     * string, using a CWLExpression object to represent them
     * 
     * @param stdin
     *            A CWLExpression object
     */
    public void setStdin(CWLFieldValue stdin) {
        this.stdin = stdin;
    }

    /**
     * Returns a CWLExpression object, the value of the CWLExpression object is a
     * file. Capture the command's standard error stream to the file written to the
     * designated output directory.
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getStderr() {
        return stderr;
    }

    /**
     * Sets stderr for this process object, the stderr can be an expression or a
     * string, using a CWLExpression object to represent them
     * 
     * @param stderr
     *            A CWLExpression object
     */
    public void setStderr(CWLFieldValue stderr) {
        this.stderr = stderr;
    }

    /**
     * Returns a CWLExpression object, the value of the CWLExpression object is a
     * file. Capture the command's standard output stream to the file written to the
     * designated output directory.
     * 
     * @return A CWLExpression object
     */
    public CWLFieldValue getStdout() {
        return stdout;
    }

    /**
     * Sets stdout for this process object, the stdout can be an expression or a
     * string, using a CWLExpression object to represent them
     * 
     * @param stdout
     *            A CWLExpression object
     */
    public void setStdout(CWLFieldValue stdout) {
        this.stdout = stdout;
    }

    /**
     * Returns the success exit codes
     * 
     * @return An array of success exit codes
     */
    public int[] getSuccessCodes() {
        return successCodes;
    }

    /**
     * Sets the success exit codes that indicate the process completed successfully.
     * 
     * @param successCodes
     *            An array of success exit codes
     */
    public void setSuccessCodes(int[] successCodes) {
        this.successCodes = successCodes;
    }
}

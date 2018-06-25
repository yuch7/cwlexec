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
package com.ibm.spectrumcomputing.cwl.exec.util.command;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

/**
 * Represents a command execution result
 */
@JsonInclude(Include.NON_NULL)
public final class CommandExecutionResult {

    private final int exitCode;
    private final String outMsg;
    private final String errMsg;
    private final boolean executed;
    @JsonIgnore
    private final List<String> commands;

    protected CommandExecutionResult(List<String> commands,
            boolean executed,
            int exitCode,
            String outMsg,
            String errMsg) {
        this.exitCode = exitCode;
        this.outMsg = outMsg;
        this.errMsg = errMsg;
        this.executed = executed;
        this.commands = commands;
    }

    /**
     * @return The exit code of the command
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * @return The command stdout
     */
    public String getOutMsg() {
        return outMsg;
    }

    /**
     * @return The command stderr
     */
    public String getErrMsg() {
        return errMsg;
    }

    /**
     * A flag to represent the command was executed.
     * 
     * @return If command does not exists, or execution timeout, return false
     */
    public boolean isExecuted() {
        return this.executed;
    }

    /**
     * @return The executed commands
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * Represents this object as JSON
     */
    @Override
    public String toString() {
        return CommonUtil.asJsonStr(this);
    }
}

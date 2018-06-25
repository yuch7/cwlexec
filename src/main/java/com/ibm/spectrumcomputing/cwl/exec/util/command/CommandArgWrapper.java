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

import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandLineBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;

/*
 * Wraps the command arguments or inputs to make them can be compared
 */
final class CommandArgWrapper implements Comparable<CommandArgWrapper> {

    private CommandLineBinding argument;
    private CommandInputParameter inputParameter;
    private int position;
    private int index;
    private String key;

    protected CommandArgWrapper(int index, CommandLineBinding argument) {
        this.argument = argument;
        this.index = index;
        this.position = argument.getPosition();
    }

    protected CommandArgWrapper(int index, CommandInputParameter inputParameter) {
        this.index = index;
        this.inputParameter = inputParameter;
        this.key = inputParameter.getId();
        this.position = inputParameter.getInputBinding().getPosition();
    }

    protected CommandLineBinding getArgument() {
        return argument;
    }

    protected CommandInputParameter getInputParameter() {
        return inputParameter;
    }

    @Override
    public int compareTo(CommandArgWrapper another) {
        int dsort = this.position - another.position;
        if (dsort == 0) {
            if (this.key != null && another.key != null) {
                dsort = this.key.compareTo(another.key);
            }
            if (dsort == 0) {
                dsort = this.index - another.index;
            }
        }
        return dsort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((argument == null) ? 0 : argument.hashCode());
        result = prime * result + index;
        result = prime * result + ((inputParameter == null) ? 0 : inputParameter.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + position;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }
}

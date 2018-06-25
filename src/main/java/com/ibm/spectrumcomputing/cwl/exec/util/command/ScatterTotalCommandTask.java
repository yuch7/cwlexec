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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

import com.ibm.spectrumcomputing.cwl.model.instance.CWLScatterHolder;

/*
 * A scatter command task, fork the scatter jobs, then join them
 */
final class ScatterTotalCommandTask extends RecursiveTask<List<CommandExecutionResult>> {

    private static final long serialVersionUID = 1L;

    private final List<CWLScatterHolder> scatterHolders;
    private final Map<String, String> envVars;
    private final String workdir;

    protected ScatterTotalCommandTask(List<CWLScatterHolder> scatterHolders, Map<String, String> envVars, Path workdir) {
        super();
        this.scatterHolders = scatterHolders;
        this.envVars = envVars;
        if (workdir != null) {
            this.workdir = workdir.toString();
        } else {
            this.workdir = null;
        }
    }

    @Override
    protected List<CommandExecutionResult> compute() {
        List<CommandExecutionResult> results = new ArrayList<>();
        List<RecursiveTask<CommandExecutionResult>> forks = new ArrayList<>();
        for (CWLScatterHolder scatterHolder : scatterHolders) {
            Path wordirPath = null;
            if (this.workdir != null) {
                wordirPath = Paths.get(this.workdir);
            }
            List<String> command = scatterHolder.getCommand();
            ScatterSingleCommandTask task = new ScatterSingleCommandTask(command, envVars, wordirPath);
            forks.add(task);
            task.fork();
        }
        for (RecursiveTask<CommandExecutionResult> task : forks) {
            results.add(task.join());
        }
        return results;
    }
}

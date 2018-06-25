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
package com.ibm.spectrumcomputing.cwl.exec.executor.lsf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * The LSF bsub command executor, for each bsub request, there is a thread to
 * handle it.
 */
final class LSFBsubExecutor {

    private static LSFBsubExecutor executor;
    private final ExecutorService service;

    private LSFBsubExecutor() {
        service = Executors.newCachedThreadPool();
    }

    /*
     * Returns a singleton bsub executor
     */
    protected static synchronized LSFBsubExecutor getExecutor() {
        if (executor == null) {
            executor = new LSFBsubExecutor();
        }
        return executor;
    }

    /*
     * Submits a LSF bsub task
     */
    protected void submit(LSFBsubExecutorTask task) {
        service.submit(task);
    }

    /*
     * Stops this executor
     */
    protected void stop() {
        service.shutdown();
    }
}

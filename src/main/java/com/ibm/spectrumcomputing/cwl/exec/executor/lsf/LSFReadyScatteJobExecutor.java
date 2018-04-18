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
package com.ibm.spectrumcomputing.cwl.exec.executor.lsf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * The ready scatter job executor, for each scatter request, there is a thread
 * to handle it.
 */
class LSFReadyScatteJobExecutor {

    private static LSFReadyScatteJobExecutor executor;
    private final ExecutorService service;

    private LSFReadyScatteJobExecutor() {
        service = Executors.newCachedThreadPool();
    }

    /*
     * Returns a singleton executor
     */
    public static synchronized LSFReadyScatteJobExecutor getExecutor() {
        if (executor == null) {
            executor = new LSFReadyScatteJobExecutor();
        }
        return executor;
    }

    public void submit(LSFReadyScatterJobExecutorTask task) {
        service.submit(task);
    }

    /*
     * Stops this executor
     */
    public void stop() {
        service.shutdown();
    }
}

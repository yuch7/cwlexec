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
package com.ibm.spectrumcomputing.cwl.exec.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Schedules a CWL process instance to run
 */
public final class CWLInstanceScheduler {

    private static CWLInstanceScheduler scheduler;

    private final ScheduledExecutorService service;
    private final List<CWLInstanceSchedulerTask> scheduledTasks = new ArrayList<>();

    private CWLInstanceScheduler() {
        ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(10);
        scheduledExecutor.setRemoveOnCancelPolicy(true);
        this.service = scheduledExecutor;
    }

    /**
     * Get a singleton CWL process instance scheduler
     * 
     * @return A singleton CWL process instance scheduler
     */
    public static synchronized CWLInstanceScheduler getScheduler() {
        if (scheduler == null) {
            scheduler = new CWLInstanceScheduler();
        }
        return scheduler;
    }

    /**
     * Schedules a CWL process instance scheduler task
     * 
     * @param task
     *            A CWL process instance scheduler task
     */
    public void scheduler(CWLInstanceSchedulerTask task) {
        if (task == null) {
            throw new IllegalArgumentException("The CWL instance scheduler task is null");
        }
        ScheduledFuture<?> future = service.scheduleAtFixedRate(task,
                0L,
                500L, // 0.5s
                TimeUnit.MILLISECONDS);
        task.setFuture(future);
        scheduledTasks.add(task);
    }

    /**
     * Stops this scheduler, If there are unfinished tasks in this scheduler, they
     * will be canceled.
     */
    public void stop() {
        for (CWLInstanceSchedulerTask task : scheduledTasks) {
            if (!task.isFinished()) {
                task.cancelTask();
            }
        }
        service.shutdown();
    }
}

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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.executor.lsf.LSFWorkflowRunner;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;

/**
 * A CWL process instance scheduler task, this task uses
 * {@link LSFWorkflowRunner} to run the given CWL process instance.
 * <br>
 * After {@link CWLInstanceScheduler} start to schedule this task, we can use
 * {@link waitFuture} to wait this task until it is finished
 */
public final class CWLInstanceSchedulerTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CWLInstanceSchedulerTask.class);

    private ScheduledFuture<?> future;
    private final CWLInstance instance;

    /**
     * Construct a CWL process instance scheduler task
     * 
     * @param instance
     *            A CWL process instance
     */
    public CWLInstanceSchedulerTask(CWLInstance instance) {
        this.instance = instance;
    }

    /**
     * Run the CWL process instance in this task, if it is failed to run, sets the
     * instance exit code to 255
     */
    @Override
    public void run() {
        if (!Thread.currentThread().isInterrupted()) {
            try {
                schedule();
            } catch (Exception e) {
                logger.error("Fail to run {} ({})", instance.getName(), e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.error("The exception stacks:", e);
                }
                instance.setExitCode(255);
                cancelTask();
            }
        }
    }

    private void schedule() throws CWLException {
        if (instance.getState() == CWLInstanceState.WAITING) {
            LSFWorkflowRunner runner = LSFWorkflowRunner.runner(instance);
            runner.start();
        }
        if (instance.isFinished()) {
            cancelTask();
        }
    }

    protected void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    protected void cancelTask() {
        if (future != null) {
            LSFWorkflowRunner.stop();
            future.cancel(false);
        }
    }

    protected boolean isFinished() {
        boolean finished = false;
        if (future != null) {
            finished = (future.isCancelled() || future.isDone());
        }
        return finished;
    }

    /**
     * Waits this task is finished, this method will block until the task was done
     * or cancelled.
     */
    public void waitFuture() {
        if (this.future != null) {
            try {
                // wait the scheduler done
                this.future.get();
            } catch (CancellationException e) {
                // Task was cancelled, ignored this exception
                logger.debug("Finish to scheule {} ({}) with exit code {}",
                        instance.getId(),
                        instance.getName(),
                        instance.getExitCode());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Fail to schedule the workflow {}", instance.getId());
            }
        }
    }
}

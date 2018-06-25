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
package com.ibm.spectrumcomputing.cwl.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.executor.CWLInstanceScheduler;
import com.ibm.spectrumcomputing.cwl.exec.executor.CWLInstanceSchedulerTask;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLServiceFactory;
import com.ibm.spectrumcomputing.cwl.exec.util.DatabaseManager;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;

/**
 * The main class of cwlexec
 */
public final class CWLExec {

    private static final Logger logger = LoggerFactory.getLogger(CWLExec.class);

    private static CWLExec cwlexec;

    /**
     * Create and return a singleton cwlexec
     * 
     * @return A singleton cwlexec
     */
    public static synchronized CWLExec cwlexec() {
        if (cwlexec == null) {
            cwlexec = new CWLExec();
        }
        return cwlexec;
    }

    private final DatabaseManager dbMgr;

    private volatile boolean running = false;

    private CWLExec() {
        dbMgr = new DatabaseManager();
    }

    /**
     * Submits a CWL process instance to {@link CWLInstanceScheduler} to run the
     * instance
     * 
     * @param instance
     *            A CWL process instance
     */
    public void submit(CWLInstance instance) {
        if (!running) {
            throw new IllegalAccessError("The cwlexec is not started");
        }
        if (instance == null) {
            throw new IllegalArgumentException("Failed to initialize cwl process instance.");
        }
        CWLInstanceSchedulerTask task = new CWLInstanceSchedulerTask(instance);
        CWLInstanceScheduler.getScheduler().scheduler(task);
        task.waitFuture();
    }

    /**
     * Starts this cwlexec, the {@link CWLServiceFactory} will be initialized.
     */
    public synchronized void start() {
        if (!running) {
            CWLServiceFactory.init(dbMgr);
            running = true;
        }
    }

    /**
     * Stops this cwlexec
     * 
     * @param dbOnly
     *            Only closes the database
     */
    public synchronized void stop(boolean dbOnly) {
        if (running) {
            logger.debug("Stop cwlexec...");
            if (!dbOnly) {
                CWLInstanceScheduler.getScheduler().stop();
            }
            dbMgr.getSessionFactory().close();
            running = false;
            logger.debug("cwlexec has been stopped");
        }
    }
}

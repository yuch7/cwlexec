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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLInstanceDependencyResolver;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;

/*
 * Run a CWL Workflow step instance
 */
final class LSFWorkflowStepRunner {

    private static final Logger logger = LoggerFactory.getLogger(LSFWorkflowStepRunner.class);

    private final LSFWorkflowRunner main;
    private final CWLCommandInstance instance;
    private final List<String> expectDependencies = new ArrayList<>();

    private AtomicInteger actualDependencies = new AtomicInteger(0);

    protected LSFWorkflowStepRunner(LSFWorkflowRunner main, CWLCommandInstance instance) throws CWLException {
        this.main = main;
        this.instance = instance;
        if (this.instance.getState() != CWLInstanceState.DONE) {
            resovleExpectDependencies();
        }
    }

    protected LSFWorkflowRunner getMain() {
        return main;
    }

    protected CWLCommandInstance getInstance() {
        return instance;
    }

    protected List<String> getExpectDependencies() {
        return expectDependencies;
    }

    /*
     * Listen a job start event, then start to wait this job
     */
    protected boolean listen(LSFJobEvent event) {
        if (event.getType() == LSFJobEventType.START && expectDependencies.contains(event.getInstanceId())) {
            if (expectDependencies.size() == actualDependencies.incrementAndGet()) {
                LSFBwaitExecutor.getExecutor()
                        .submit(new LSFBwaitExecutorTask(this, isTerminatedStep(event.getInstanceId())));
            }
            return true;
        }
        return false;
    }

    private void resovleExpectDependencies() throws CWLException {
        if (instance.isReadyToRun()) {
            if (!isDependency(instance.getName())) {
                logger.debug("A single step {}", instance.getName());
                expectDependencies.add(instance.getId());
            }
        } else {
            Set<String> stepNames = CWLInstanceDependencyResolver.resolveDependentSteps(instance);
            logger.debug("step ({}) depends on steps: {}", instance.getName(), stepNames);
            // translate step names into step instance ids
            if (main.getInstance() instanceof CWLWorkflowInstance) {
                CWLWorkflowInstance workflowInstance = (CWLWorkflowInstance) main.getInstance();
                for (CWLInstance stepInstance : workflowInstance.getInstances()) {
                    if (stepNames.contains(stepInstance.getName()))
                        expectDependencies.add(stepInstance.getId());
                }
            }
        }
    }

    private boolean isDependency(String instanceName) throws CWLException {
        boolean isDependency = false;
        CWLInstance mainInstance = main.getInstance();
        if (mainInstance instanceof CWLWorkflowInstance) {
            CWLWorkflowInstance workflowInstance = (CWLWorkflowInstance) mainInstance;
            for (CWLInstance subInstance : workflowInstance.getInstances()) {
                if (subInstance instanceof CWLCommandInstance && !subInstance.isReadyToRun()) {
                    Set<String> stepNames = CWLInstanceDependencyResolver.resolveDependentSteps((CWLCommandInstance) subInstance);
                    if (stepNames.contains(instanceName)) {
                        isDependency = true;
                        break;
                    }
                }
            }
        }
        return isDependency;
    }

    private boolean isTerminatedStep(String instanceId) {
        return (expectDependencies.size() == 1) &&
                (expectDependencies.get(0).equals(instanceId)) &&
                (this.getInstance().getId().equals(instanceId));
    }
}

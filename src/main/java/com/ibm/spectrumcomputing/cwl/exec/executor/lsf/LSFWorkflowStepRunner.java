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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.service.CWLRuntimeService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLServiceFactory;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLInstanceDependencyResolver;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLStepBindingResolver;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.CommandStdIOEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.InputsEvaluator;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.WorkflowStepInput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;

/*
 * Run a CWL Workflow step instance
 */
final class LSFWorkflowStepRunner {

    private static final Logger logger = LoggerFactory.getLogger(LSFWorkflowStepRunner.class);

    private final LSFWorkflowRunner main;
    private final CWLCommandInstance instance;
    private final List<String> expectDependencies = new ArrayList<>();

    private AtomicInteger actualDependencies = new AtomicInteger(0);

    private final CWLRuntimeService runtimeService = CWLServiceFactory.getService(CWLRuntimeService.class);

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
                addStepDependetents(stepNames, workflowInstance);
                if (expectDependencies.isEmpty()) {
                    //when rerun a flow, the dependent step may be done
                    logger.debug("step ({}) dependents are all done, ready to run", instance.getName());
                    prepareStepCommand(instance);
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

    private void addStepDependetents(Set<String> stepNames, CWLWorkflowInstance workflowInstance) {
        for (CWLInstance stepInstance : workflowInstance.getInstances()) {
            if (stepNames.contains(stepInstance.getName())) {
                if (stepInstance.getState() == CWLInstanceState.DONE) {
                    //when rerun a flow, the dependent step may be done
                    logger.debug("dependent step ({}) is alreay done, don't wait it.", stepInstance.getName());
                } else {
                    expectDependencies.add(stepInstance.getId());
                }
            }
        }
    }

    private void prepareStepCommand(CWLCommandInstance instance) throws CWLException {
        WorkflowStep instStep = instance.getStep();
        List<WorkflowStepInput> in = instStep.getIn();
        for (WorkflowStepInput stepInput : in) {
            CWLStepBindingResolver.resolveStepInput(instance, instStep, stepInput);
        }
        CommandLineTool commandLineTool = (CommandLineTool) instStep.getRun();
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(instance, InlineJavascriptRequirement.class);
        Map<String, String> runtime = instance.getRuntime();
        List<CommandInputParameter> inputs = commandLineTool.getInputs();
        InputsEvaluator.eval(jsReq, runtime, inputs);
        // print the value of inputs
        for (CommandInputParameter parameter : inputs) {
            Object value = parameter.getValue();
            if (value == null) {
                value = parameter.getDefaultValue();
            }
            if (value != null && value instanceof CWLFile) {
                value = ((CWLFile) value).getPath();
            }
            logger.debug("Resolve input ({}) of step ({}) to <{}>", parameter.getId(), instance.getName(), value);
        }
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStdin());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStderr());
        CommandStdIOEvaluator.eval(jsReq, runtime, inputs, commandLineTool.getStdout());
        instance.setReadyToRun(true);
        List<String> commands = runtimeService.buildRuntimeCommand(instance);
        instance.setCommands(commands);
    }
}

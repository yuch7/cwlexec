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
package com.ibm.spectrumcomputing.cwl.exec.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.WorkflowStepInput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Resolves the dependent of a CWL process instance
 */
public class CWLInstanceDependencyResolver {

    private CWLInstanceDependencyResolver() {
    }

    /**
     * Find all dependent CWL Workflow steps of a given CWL process instance
     * 
     * @param cwlInstance
     *            A CWL process instance
     * @return A set of step names on which the CWL process instance depends
     * @throws CWLException
     *             Failed to resolve the dependents
     */
    public static Set<String> resolveDependentSteps(CWLInstance cwlInstance) throws CWLException {
        if (cwlInstance == null) {
            throw new IllegalArgumentException("Argument (cwlInstance) is null");
        }

        Set<String> dependentStepNames = new HashSet<>();

        WorkflowStep step = cwlInstance.getStep();
        for (String source : step.getDependencies()) {
            int index = source.lastIndexOf('/');
            if (index != -1) {
                // case 1: source refers to output of some step in the current
                // workflow
                String stepName = source.substring(0, index);
                String outputId = source.substring(index + 1);
                dependentStepNames.addAll(resolveStepOutputDependentSteps(
                        CWLStepBindingResolver.findStepInstance(cwlInstance, stepName), outputId));
            } else if (cwlInstance.getParent() != null && cwlInstance.getParent() instanceof CWLWorkflowInstance) {
                // case 2: source refers to input of current workflow and the
                // current workflow is a subworkflow in its parent workflow
                CWLWorkflowInstance workflowInstance = cwlInstance.getParent();
                Workflow workflow = (Workflow) workflowInstance.getProcess();
                CWLParameter workflowInputParam = CommonUtil.findParameter(source, workflow.getInputs());
                if (workflowInputParam != null) {
                    dependentStepNames.addAll(resolveWorkflowInputDependentSteps(workflowInputParam, workflowInstance));
                } else {
                    throw new CWLException(
                            ResourceLoader.getMessage("cwl.workflow.input.not.found",
                                    source,
                                    cwlInstance.getProcess().getId(),
                                    cwlInstance.getName()),
                            255);
                }
            }
        }

        return dependentStepNames;
    }

    private static Set<String> resolveStepOutputDependentSteps(CWLInstance cwlInstance,
            String outputId) throws CWLException {
        if (cwlInstance == null) {
            throw new IllegalArgumentException("Argument (cwlInstance) is null");
        }
        Set<String> dependentStepNames = new HashSet<>();
        if (cwlInstance instanceof CWLWorkflowInstance) {
            CWLWorkflowInstance workflowInstance = (CWLWorkflowInstance) cwlInstance;
            Workflow workflow = (Workflow) workflowInstance.getProcess();
            WorkflowOutputParameter outputParameter = (WorkflowOutputParameter) CommonUtil.findParameter(outputId,
                    workflow.getOutputs());
            if (outputParameter == null) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.workflow.output.not.found",
                                outputId,
                                workflow.getId(),
                                cwlInstance.getName()),
                        255);
            }
            List<String> sources = outputParameter.getOutputSource();
            if (sources != null) {
                for (String source : sources) {
                    int index = source.lastIndexOf('/');
                    if (index != -1) {
                        String stepName = cwlInstance.getName() + "/" + source.substring(0, index);
                        String subOutputId = source.substring(index + 1);
                        dependentStepNames.addAll(resolveStepOutputDependentSteps(
                                CWLStepBindingResolver.findStepInstance(cwlInstance, stepName), subOutputId));
                    }
                }
            }
        } else {
            dependentStepNames.add(cwlInstance.getName());
        }
        return dependentStepNames;
    }

    private static Set<String> resolveWorkflowInputDependentSteps(CWLParameter workflowInputParam,
            CWLWorkflowInstance workflowInstance) throws CWLException {
        Set<String> dependentStepNames = new HashSet<>();
        CWLWorkflowInstance parent = workflowInstance.getParent();
        WorkflowStep step = workflowInstance.getStep();
        if (parent == null || step == null) {
            return dependentStepNames;
        }
        WorkflowStepInput stepInput = findStepInput(step, workflowInputParam);
        if (stepInput == null) {
            if (workflowInputParam.getDefaultValue() == null) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.workflow.step.input.not.found",
                                workflowInputParam.getId(),
                                workflowInstance.getName()),
                        255);
            }
            return dependentStepNames;
        } else {
            List<String> sources = stepInput.getSource();
            if (sources == null || sources.isEmpty()) {
                return dependentStepNames;
            }
            for (String source : sources) {
                addSourceDependent(dependentStepNames, parent, source);
            }
        }
        return dependentStepNames;
    }

    private static WorkflowStepInput findStepInput(WorkflowStep step, CWLParameter workflowInputParam) {
        WorkflowStepInput stepInput = null;
        for (WorkflowStepInput input : step.getIn()) {
            if (input.getId().equals(workflowInputParam.getId())) {
                stepInput = input;
                break;
            }
        }
        return stepInput;
    }

    private static void addSourceDependent(Set<String> dependentStepNames, CWLWorkflowInstance parent, String source)
            throws CWLException {
        int index = source.lastIndexOf('/');
        if (index == -1) {
            Workflow workflow = (Workflow) parent.getProcess();
            CWLParameter inputParam = CommonUtil.findParameter(source, workflow.getInputs());
            if (inputParam == null) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.workflow.input.not.found",
                                source,
                                workflow.getId(),
                                parent.getName()),
                        255);
            }
            dependentStepNames.addAll(resolveWorkflowInputDependentSteps(inputParam, parent));
        } else {
            dependentStepNames.add(findStepFullName(parent, source.substring(0, index)));
        }
    }

    private static String findStepFullName(CWLWorkflowInstance parent, String stepName) {
        String fullName = stepName;
        for (CWLInstance step : parent.getInstances()) {
            if (step.getName().endsWith("/" + stepName)) {
                fullName = step.getName();
                break;
            }
        }
        return fullName;
    }
}

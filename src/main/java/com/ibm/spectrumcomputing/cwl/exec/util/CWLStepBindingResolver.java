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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.StepInValueFromEvaluator;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.InputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.WorkflowStepInput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Resolves the source binding of a CWL step process
 */
public class CWLStepBindingResolver {

	private static final Logger logger = LoggerFactory.getLogger(CWLStepBindingResolver.class);
	
    private static final String INPUT_NOT_FOUND_MSG = "cwl.workflow.input.not.found";
    private static final String OUTPUT_NOT_FOUND_MSG = "cwl.workflow.output.not.found";

    private CWLStepBindingResolver() {}

    /**
     * Resolve the input values of a CWL Workflow steps
     * 
     * @param workflowInstance
     *            The CWL Workflow instance
     * @param step
     *            The CWL Workflow step process
     * @return True if all inputs of the step are resolved
     * @throws CWLException
     *             Failed to resolve the step inputs
     */
    public static boolean resolveStepInputs(CWLWorkflowInstance workflowInstance,
            WorkflowStep step) throws CWLException {
        for (WorkflowStepInput stepInput : step.getIn()) {
            boolean resolved = resolveStepInput(workflowInstance, step.getRun(), step, stepInput);
            if (!resolved) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolve the input value of a given CWL step process instance
     * 
     * @param stepInstance
     *            A given CWL step process instance (CWLCommandInstance or
     *            CWLWorkflowInstance)
     * @param step
     *            The CWL Workflow step
     * @param stepInput
     *            The CWL Workflow step input
     * @throws CWLException
     *             Failed to resolve the step input
     */
    public static void resolveStepInput(CWLInstance stepInstance,
            WorkflowStep step,
            WorkflowStepInput stepInput) throws CWLException {
        CWLParameter runInputParam = CommonUtil.findParameter(stepInput.getId(), stepInstance.getProcess().getInputs());
        if (runInputParam == null) {
            return;
        }
        // do nothing if the value of the parameter has already been evaluated
        if (runInputParam.getValue() != null) {
            return;
        }
        // otherwise calculate the parameter value
        CWLWorkflowInstance parent = stepInstance.getParent();
        List<Object> sourceValues = new ArrayList<>();
        List<String> sources = stepInput.getSource();
        if (sources == null || sources.isEmpty()) {
            // set value to default value if no source exists
            // step default value should overwrite run default value
            if (stepInput.getDefaultValue() != null) {
                runInputParam.setValue(stepInput.getDefaultValue());
            } else if (runInputParam.getDefaultValue() != null) {
                runInputParam.setValue(runInputParam.getDefaultValue());
            }
        } else {
            // collect source values
            for (String source : sources) {
                int index = source.lastIndexOf('/');
                if (index == -1) {
                    // case 1: source refers to input of the workflow
                    Workflow workflow = (Workflow) parent.getProcess();
                    CWLParameter workflowInputParam = CommonUtil.findParameter(source, workflow.getInputs());
                    if (workflowInputParam == null) {
                        throw new CWLException(
                                ResourceLoader.getMessage(INPUT_NOT_FOUND_MSG,
                                        source,
                                        workflow.getId(),
                                        parent.getName()),
                                250);
                    }
                    Object parameterValue = workflowInputParam.getValue();
                    // bind the input parameter if the parameter is not
                    // evaluated yet and the whole workflow is one of steps in
                    // the parent workflow
                    if (parameterValue == null && parent.getParent() != null) {
                        WorkflowStep parentStep = parent.getStep();
                        for (WorkflowStepInput parentStepInput : parentStep.getIn()) {
                            if (parentStepInput.getId().equals(workflowInputParam.getId())) {
                                resolveStepInput(parent, parentStep, parentStepInput);
                                break;
                            }
                        }
                    }
                    parameterValue = workflowInputParam.getValue();
                    if(parameterValue == null) {
                    	parameterValue = stepInput.getDefaultValue();
                    }
                    if (parameterValue == null) {
                        sourceValues.add(mapRecordValues(workflowInputParam, workflowInputParam.getDefaultValue()));
                    } else {
                        sourceValues.add(mapRecordValues(workflowInputParam, parameterValue));
                    }
                } else {
                    // case 2: source refers to output of other step in the same
                    // workflow
                    String stepName = source.substring(0, index);
                    String outputId = source.substring(index + 1);
                    CWLInstance dependentStepInstance = findStepInstance(stepInstance, stepName);
                    boolean resolved = resolveStepOutput(dependentStepInstance, outputId);
                    if (resolved) {
                        CWLParameter stepOutParam = CommonUtil.findParameter(outputId,
                                dependentStepInstance.getProcess().getOutputs());
                        if (stepOutParam == null) {
                            throw new CWLException(
                                    ResourceLoader.getMessage(OUTPUT_NOT_FOUND_MSG,
                                            outputId,
                                            dependentStepInstance.getProcess().getId(),
                                            dependentStepInstance.getName()),
                                    250);
                        }
                        Object srcValue = stepOutParam.getValue();
                        if (srcValue == null) {
                            srcValue = stepOutParam.getDefaultValue();
                        }
                        //Fix count-lines11-null-step-wf-noET.cwl
                        if (srcValue == null) {
                            srcValue = stepInput.getDefaultValue();
                        }
                        Object recordValues = mapRecordValues(stepOutParam, srcValue);
                        if (recordValues != null) {
                        	sourceValues.add(recordValues);	
                        }
                        
                    } else {
                        throw new CWLException(
                                ResourceLoader.getMessage("cwl.workflow.step.not.resolved", stepInstance.getName()),
                                255);
                    }
                }
            }
            // set the value of input parameter according to source values
            if (stepInput.getSource().size() > 1) {
                runInputParam.setValue(sourceValues);
            } else if (!sourceValues.isEmpty()) {
                Object value = sourceValues.get(0);
                boolean isScatterStep = false;
                if (stepInstance instanceof CWLCommandInstance && ((CWLCommandInstance) stepInstance).getScatter() != null) {
                    isScatterStep = true;
                }
                if (!isScatterStep) {
                    if (value instanceof List<?> &&
                            (runInputParam.getType().getType() != null &&
                                runInputParam.getType().getType().getSymbol() != CWLTypeSymbol.ARRAY)) {
                            value = ((List<?>) value).get(0);
                    }
                }
                runInputParam.setValue(value);
            }
        }
        // evaluate expression if exists
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(stepInstance, 
                InlineJavascriptRequirement.class);
        List<CWLParameter> inputs = getStepInputParameters(parent, step);
        
        Object self = toStepInputSelf(sourceValues);
        Object valueFrom = StepInValueFromEvaluator.eval(jsReq,
                parent.getRuntime(),
                inputs,
                self,
                step,
                stepInput,
                runInputParam);
        if (valueFrom != null) {
            runInputParam.setValue(valueFrom);
        }
    }

    /**
     * Resolve the output of a CWL Workflow step instance
     * 
     * @param stepInstance
     *            A CWL Workflow step instance (CWLCommandInstance or
     *            CWLWorkflowInstance)
     * @param outputId
     *            The output ID of the CWL Workflow step
     * @return True if the output is resolvable
     * @throws CWLException
     *             Failed to resolve the output
     */
    public static boolean resolveStepOutput(CWLInstance stepInstance, String outputId) throws CWLException {
        if (stepInstance instanceof CWLWorkflowInstance) {
            CWLWorkflowInstance workflowInstance = (CWLWorkflowInstance) stepInstance;
            WorkflowOutputParameter outputParameter = (WorkflowOutputParameter) CommonUtil.findParameter(outputId,
                    workflowInstance.getProcess().getOutputs());
            if (outputParameter == null) {
                throw new CWLException(
                        ResourceLoader.getMessage(OUTPUT_NOT_FOUND_MSG,
                                outputId,
                                workflowInstance.getProcess().getId(),
                                workflowInstance.getName()),
                        255);
            }
            List<Object> sourceValues = new ArrayList<>();
            for (String source : outputParameter.getOutputSource()) {
                int index = source.lastIndexOf('/');
                if (index != -1) {
                    String stepName = workflowInstance.getParent() == null ? source.substring(0, index)
                            : workflowInstance.getName() + "/" + source.substring(0, index);
                    String subOutputId = source.substring(index + 1);
                    CWLInstance dependentStepInstance = findStepInstance(stepInstance, stepName);
                    boolean resolved = resolveStepOutput(dependentStepInstance, subOutputId);
                    if (resolved) {
                        CWLParameter stepOutParam = CommonUtil.findParameter(subOutputId,
                                dependentStepInstance.getProcess().getOutputs());
                        if (stepOutParam == null) {
                            throw new CWLException(
                                    ResourceLoader.getMessage(OUTPUT_NOT_FOUND_MSG,
                                            subOutputId,
                                            dependentStepInstance.getProcess().getId(),
                                            dependentStepInstance.getName()),
                                    255);
                        }
                        Object srcValue = stepOutParam.getValue();
                        if (srcValue == null) {
                            srcValue = stepOutParam.getDefaultValue();
                        }
                        sourceValues.add(mapRecordValues(stepOutParam, srcValue));
                    } else {
                        return false;
                    }
                }
            }
            // set the value of output parameter according to source values
            CWLType paramType = outputParameter.getType().getType();
            if (paramType.getSymbol() == CWLTypeSymbol.ARRAY && !sourceValues.isEmpty()) {
                outputParameter.setValue(sourceValues);
            } else if (!sourceValues.isEmpty()) {
                outputParameter.setValue(sourceValues.get(0));
            }
            return true;
        } else {
            return stepInstance.getState() == CWLInstanceState.DONE;
        }
    }

    /**
     * Resolve the output value of a CWL Workflow
     * 
     * @param workflowInstance
     *            A CWL Workflow instance
     * @param outputParameter
     *            The output parameter of the CWL Workflow
     * @throws CWLException
     *             Failed to resolve the output
     */
    public static void resolveWorkflowOutput(CWLWorkflowInstance workflowInstance,
            WorkflowOutputParameter outputParameter) throws CWLException {
        // do nothing if the output parameter has already been resolved
        if (outputParameter.getValue() != null) {
            return;
        }
        // otherwise, resolve the parameter value
        List<Object> sourceValues = new ArrayList<>();
        for (String source : outputParameter.getOutputSource()) {
            int index = source.lastIndexOf('/');
            if (index != -1) {
                String stepName = workflowInstance.getParent() == null ? source.substring(0, index)
                        : workflowInstance.getName() + "/" + source.substring(0, index);
                String subOutputId = source.substring(index + 1);
                CWLInstance dependentStepInstance = findStepInstance(workflowInstance, stepName);
                CWLParameter stepOutParam = CommonUtil.findParameter(subOutputId,
                        dependentStepInstance.getProcess().getOutputs());
                if (stepOutParam == null) {
                    throw new CWLException(
                            ResourceLoader.getMessage(OUTPUT_NOT_FOUND_MSG,
                                    subOutputId,
                                    dependentStepInstance.getProcess().getId(),
                                    dependentStepInstance.getName()),
                            255);
                }
                if (dependentStepInstance instanceof CWLWorkflowInstance) {
                    resolveWorkflowOutput((CWLWorkflowInstance) dependentStepInstance,
                            (WorkflowOutputParameter) stepOutParam);
                } else if (dependentStepInstance.getState() != CWLInstanceState.DONE) {
                    throw new CWLException(
                            ResourceLoader.getMessage("cwl.workflow.output.not.resolved",
                                    subOutputId,
                                    dependentStepInstance.getName()),
                            255);
                }
                Object srcValue = stepOutParam.getValue();
                if (srcValue == null) {
                    srcValue = stepOutParam.getDefaultValue();
                }
                sourceValues.add(mapRecordValues(stepOutParam, srcValue));
            } else {
                CWLParameter inputParam = CommonUtil.findParameter(source,
                        workflowInstance.getProcess().getInputs());
                if (inputParam == null) {
                    throw new CWLException(
                            ResourceLoader.getMessage(INPUT_NOT_FOUND_MSG,
                                    source,
                                    workflowInstance.getProcess().getId(),
                                    workflowInstance.getName()),
                            255);
                }
                Object srcValue = inputParam.getValue();
                if (srcValue == null) {
                    srcValue = inputParam.getDefaultValue();
                }
                sourceValues.add(mapRecordValues(inputParam, srcValue));
            }
        }
        // set the value of output parameter according to source values
        CWLType paramType = outputParameter.getType().getType();
        if (paramType.getSymbol() == CWLTypeSymbol.ARRAY && sourceValues.size() > 1) {
            outputParameter.setValue(sourceValues);
        } else if (!sourceValues.isEmpty()) {
            outputParameter.setValue(sourceValues.get(0));
        }
    }

    protected static CWLInstance findStepInstance(CWLInstance cwlInstance, String stepName) throws CWLException {
        return findStepInstance(cwlInstance, stepName, false);
    }

    private static boolean resolveStepInput(CWLWorkflowInstance parent,
            CWLProcess cwlProcess,
            WorkflowStep step,
            WorkflowStepInput stepInput) throws CWLException {
        CWLParameter runInputParam = CommonUtil.findParameter(stepInput.getId(), cwlProcess.getInputs());
        if (runInputParam == null) {
            return true;
        }
        // do nothing if the input has been resolved
        if (runInputParam.getValue() != null) {
            return true;
        }
        // otherwise calculate the parameter value
        List<Object> sourceValues = new ArrayList<>();
        List<String> sources = stepInput.getSource();
        if (sources == null || sources.isEmpty()) {
            // set value to the default value if no source exists
            // step default value should overwrite run default value
            if (stepInput.getDefaultValue() != null) {
                runInputParam.setValue(stepInput.getDefaultValue());
            } else if (runInputParam.getDefaultValue() != null) {
                runInputParam.setValue(runInputParam.getDefaultValue());
            }
        } else {
            // collect source values
            for (String source : sources) {
                int index = source.lastIndexOf('/');
                if (index == -1) {
                    // case 1: source refers to input of the workflow
                    Workflow workflow = (Workflow) parent.getProcess();
                    CWLParameter workflowInputParam = CommonUtil.findParameter(source, workflow.getInputs());
                    if (workflowInputParam == null) {
                        throw new CWLException(
                                ResourceLoader.getMessage(INPUT_NOT_FOUND_MSG,
                                        source,
                                        workflow.getId(),
                                        parent.getName()),
                                250);
                    }
                    Object parameterValue = workflowInputParam.getValue();
                    // bind the input parameter if the parameter is not
                    // evaluated yet and the whole workflow is one of steps in
                    // its parent workflow
                    if (parameterValue == null && parent.getParent() != null) {
                        WorkflowStep parentStep = parent.getStep();
                        for (WorkflowStepInput parentStepInput : parentStep.getIn()) {
                            if (parentStepInput.getId().equals(workflowInputParam.getId())) {
                                boolean resolvable = resolveStepInput(parent.getParent(), parent.getStep().getRun(),
                                        parentStep, parentStepInput);
                                if (!resolvable) {
                                    return false;
                                }
                            }
                        }
                    }
                    parameterValue = workflowInputParam.getValue();
                    if(parameterValue == null) {
                    	parameterValue = stepInput.getDefaultValue();
                    }
                    if (parameterValue == null) {
                        sourceValues.add(mapRecordValues(workflowInputParam, workflowInputParam.getDefaultValue()));
                    } else {
                        sourceValues.add(mapRecordValues(workflowInputParam, parameterValue));
                    }
                } else {
                    // case 2: source refers to output of other step in the same
                    // workflow
                    String stepName = source.substring(0, index);
                    String outputId = source.substring(index + 1);
                    CWLInstance dependentStepInstance = findStepInstance(parent, stepName, true);
                    if (dependentStepInstance == null) {
                        return false;
                    }
                    boolean resolved = resolveStepOutput(dependentStepInstance, outputId);
                    if (resolved) {
                        CWLParameter stepOutParam = CommonUtil.findParameter(outputId,
                                dependentStepInstance.getProcess().getOutputs());
                        if (stepOutParam == null) {
                            throw new CWLException(
                                    ResourceLoader.getMessage(
                                            OUTPUT_NOT_FOUND_MSG,
                                            outputId,
                                            dependentStepInstance.getProcess().getId(),
                                            dependentStepInstance.getName()),
                                    250);
                        }
                        Object srcValue = stepOutParam.getValue();
                        if (srcValue == null) {
                            srcValue = stepOutParam.getDefaultValue();
                        }
                        sourceValues.add(mapRecordValues(stepOutParam, srcValue));
                    } else {
                        return false;
                    }
                }
            }
            // set the value of input parameter according to source values
            if (stepInput.getSource().size() > 1) {
                runInputParam.setValue(sourceValues);
            } else if (!sourceValues.isEmpty()) {
                runInputParam.setValue(sourceValues.get(0));
            }
        }
        // evaluate expression
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(step, InlineJavascriptRequirement.class);
        List<CWLParameter> inputs = null;
        try {
            inputs = getStepInputParameters(parent, step);
        } catch (CWLException e) {
            return false;
        }
        Object valueFrom = StepInValueFromEvaluator.eval(jsReq,
                parent.getRuntime(),
                inputs,
                toStepInputSelf(sourceValues),
                step,
                stepInput,
                runInputParam);
        if (valueFrom != null) {
            runInputParam.setValue(valueFrom);
        }
        return true;
    }

    private static Object toStepInputSelf(List<Object> soureValues) {
        if (soureValues.isEmpty()) {
            return null;
        }
        return soureValues.size() == 1 ? soureValues.get(0) : soureValues;
    }

    private static CWLInstance findStepInstance(CWLInstance cwlInstance,
            String stepName,
            boolean suppressException) throws CWLException {
        CWLInstance parent = cwlInstance;
        logger.debug("Instance name: {}", parent.getName());
        while (parent.getParent() != null) {
            parent = parent.getParent();
            logger.debug("Parent instance name: {}", parent.getName());
        }
        if (parent instanceof CWLWorkflowInstance) {
            CWLWorkflowInstance workflowInstance = (CWLWorkflowInstance) parent;
            logger.debug("# of {} instance: {}", parent.getName(), workflowInstance.getInstances().size());
            for (CWLInstance stepInstance : workflowInstance.getInstances()) {
                logger.debug("Description path: {}", stepInstance.getProcess().getDescPath());
                logger.debug("Step name: {}", stepInstance.getName());
                logger.debug("Parent step name: {}", stepInstance.getParent().getName());
                if (stepInstance.getName().equals(stepName)) {
                    return stepInstance;
                }
                if (stepInstance.getParent().getName().equals(stepName)) {
                    return stepInstance.getParent();
                }
            }
        } else {
            logger.debug("{} is not CWLWorkflowInstance, {}", parent.getName(), parent.getClass().getTypeName());
        }
        if (suppressException) {
            return null;
        } else {
            throw new CWLException(ResourceLoader.getMessage("cwl.workflow.step.not.found", stepName), 255);
        }
    }

    private static Object mapRecordValues(CWLParameter workflowInputParam, Object inputValue) {
        if (inputValue == null) {
            return null;
        }
        Object value = inputValue;
        CWLType type = workflowInputParam.getType().getType();
        if (type != null && (type.getSymbol() == CWLTypeSymbol.ARRAY)) {
            CWLType itemType = null;
            if (type instanceof InputArrayType) {
                itemType = ((InputArrayType) type).getItems().getType();
            } else if (type instanceof OutputArrayType) {
                itemType = ((OutputArrayType) type).getItems().getType();
            }
            if (itemType != null && itemType.getSymbol() == CWLTypeSymbol.RECORD) {
                @SuppressWarnings("unchecked")
                List<InputRecordField> records = (List<InputRecordField>) inputValue;
                List<Map<String, Object>> recordMaps = new ArrayList<>();
                for (InputRecordField record : records) {
                    Map<String, Object> recordMap = new HashMap<>();
                    @SuppressWarnings("unchecked")
                    List<InputRecordField> recordValue = (List<InputRecordField>) record.getValue();
                    recordMap.put(recordValue.get(0).getName(), recordValue.get(0).getValue());
                    recordMaps.add(recordMap);
                }
                value = recordMaps;
            }
        }
        return value;
    }

    private static List<CWLParameter> getStepInputParameters(CWLInstance parent,
            WorkflowStep step) throws CWLException {
        List<CWLParameter> parameters = new ArrayList<>();
        for (WorkflowStepInput stepInput : step.getIn()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Step input of {}: {}", step.getId(), stepInput.getId());
            }
            CWLParameter parameter = CommonUtil.findParameter(stepInput.getId(), step.getRun().getInputs());
            if (parameter == null) {
                parameter = new InputParameter(stepInput.getId());
                List<String> sources = stepInput.getSource();
                if (sources == null || sources.isEmpty()) {
                    // set value to the default value if no source exists
                    if (stepInput.getDefaultValue() != null) {
                        parameter.setValue(stepInput.getDefaultValue());
                    }
                } else {
                    // collect source values
                    List<Object> sourceValues = new ArrayList<>();
                    for (String source : sources) {
                        int index = source.lastIndexOf('/');
                        if (index == -1) {
                            // case 1: source refers to input of the workflow
                            Workflow workflow = (Workflow) parent.getProcess();
                            CWLParameter workflowInputParam = CommonUtil.findParameter(source, workflow.getInputs());
                            if (workflowInputParam == null) {
                                throw new CWLException(
                                        ResourceLoader.getMessage(INPUT_NOT_FOUND_MSG,
                                                source,
                                                workflow.getId(),
                                                parent.getName()),
                                        255);
                            }
                            Object parameterValue = workflowInputParam.getValue();
                            // bind the input parameter if the parameter is not
                            // evaluated yet and the whole workflow is one of
                            // steps in its parent workflow
                            if (parameterValue == null && parent.getParent() != null) {
                                WorkflowStep parentStep = parent.getStep();
                                for (WorkflowStepInput parentStepInput : parentStep.getIn()) {
                                    if (parentStepInput.getId().equals(workflowInputParam.getId())) {
                                        resolveStepInput(parent.getParent(), parentStep, parentStepInput);
                                        break;
                                    }
                                }
                            }
                            parameterValue = workflowInputParam.getValue();
                            if(parameterValue == null) {
                            	parameterValue = stepInput.getDefaultValue();
                            }
                            if (parameterValue == null) {
                                sourceValues
                                        .add(mapRecordValues(workflowInputParam, workflowInputParam.getDefaultValue()));
                            } else {
                                sourceValues.add(mapRecordValues(workflowInputParam, parameterValue));
                            }
                        } else {
                            // case 2: source refers to output of other step in
                            // the same workflow
                            String stepName = source.substring(0, index);
                            String outputId = source.substring(index + 1);
                            CWLInstance dependentStepInstance = findStepInstance(parent, stepName);
                            boolean resolved = resolveStepOutput(dependentStepInstance, outputId);
                            if (resolved) {
                                CWLParameter stepOutParam = CommonUtil.findParameter(outputId,
                                        dependentStepInstance.getProcess().getOutputs());
                                if (stepOutParam == null) {
                                    throw new CWLException(
                                            ResourceLoader.getMessage(OUTPUT_NOT_FOUND_MSG,
                                                    outputId,
                                                    dependentStepInstance.getProcess().getId(),
                                                    dependentStepInstance.getName()),
                                            255);
                                }
                                Object srcValue = stepOutParam.getValue();
                                if (srcValue == null) {
                                    srcValue = stepOutParam.getDefaultValue();
                                }
                                sourceValues.add(mapRecordValues(stepOutParam, srcValue));
                            } else {
                                throw new CWLException(
                                        ResourceLoader.getMessage("cwl.workflow.output.not.resolved",
                                                outputId,
                                                dependentStepInstance.getName()),
                                        255);
                            }
                        }
                    }
                    // set the value of input parameter
                    if (sourceValues.size() > 1) {
                        parameter.setValue(sourceValues);
                    } else if (sourceValues.size() == 1) {
                        parameter.setValue(sourceValues.get(0));
                    } else if (stepInput.getDefaultValue() != null) {
                        parameter.setValue(stepInput.getDefaultValue());
                    }
                }
            }
            parameters.add(parameter);
        }
        return parameters;
    }
}

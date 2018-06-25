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
package com.ibm.spectrumcomputing.cwl.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.InputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.WorkflowStepInput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.LinkMergeMethod;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.ScatterMethod;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowStepOutput;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullValue;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.SubworkflowFeatureRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * Parses CWL Workflow document description file
 */
final class WorkflowParser extends BaseParser {

    private static final String SOURCE = "source";
    private static final String SCATTER = "scatter";
    private static final String LINK_MERGE = "linkMerge";
    private static final String STEPS = "steps";
    private static final String ARRAY_OR_MAP = "array or map";

    private WorkflowParser() {
    }

    protected static Workflow yieldWorkflow(String descTop,
            String descFilePath,
            String owner,
            JsonNode node,
            String namespace) throws CWLException {
        Workflow workflow = null;
        List<Requirement> requirements = RequirementParser.processRequirements(descTop, REQUIREMENTS,
                node.get(REQUIREMENTS));
        List<Requirement> hints = RequirementParser.processHints(descTop, node.get("hints"));
        String id = processStringField("id", node.get("id"));
        List<InputParameter> inputs = toInputs(descTop, node.get(INPUTS), id);
        if (inputs == null) {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, INPUTS), 251);
        }
        List<WorkflowOutputParameter> outputs = toOutputs(node.get("outputs"), id);
        if (outputs == null) {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, "outputs"), 251);
        }
        List<WorkflowStep> steps = toSteps(descTop, descFilePath, owner, node.get(STEPS), namespace, id);
        if (steps == null) {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, STEPS), 251);
        }
        workflow = new Workflow(inputs, outputs, steps);
        workflow.setId(id);
        workflow.setRequirements(requirements);
        workflow.setHints(hints);
        workflow.setLabel(processStringField(LABEL, node.get(LABEL)));
        workflow.setDoc(processStringField("doc", node.get("doc")));
        validateWorkflowRequirements(workflow);
        return workflow;
    }

    private static List<InputParameter> toInputs(String descTop,
            JsonNode inputsNode,
            String processId) throws CWLException {
        List<InputParameter> inputs = null;
        if (inputsNode != null) {
            inputs = new ArrayList<>();
            if (inputsNode.isArray()) {
                Iterator<JsonNode> elements = inputsNode.elements();
                while (elements.hasNext()) {
                    InputParameter input = toInputParameter(elements.next(), descTop, processId);
                    inputs.add(input);
                }
            } else if (inputsNode.isObject()) { // the inputs type is map
                Iterator<Entry<String, JsonNode>> fields = inputsNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    InputParameter input = toInputParameter(descTop, field.getKey(), field.getValue(), processId);
                    inputs.add(input);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, INPUTS, ARRAY_OR_MAP), 251);
            }
        }
        return inputs;
    }

    private static InputParameter toInputParameter(JsonNode inputNode,
            String descTop,
            String processId) throws CWLException {
        String id = processStringField("id", inputNode.get("id"));
        if (id == null || id.length() == 0) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.field.required.in", "id", "CommandInputParameter"), 251);
        }
        return toInputParameter(descTop, id, inputNode, processId);
    }

    private static InputParameter toInputParameter(String descTop,
            String id,
            JsonNode inputNode,
            String processId) throws CWLException {
        if (processId != null && id.startsWith(processId + "/")) {
            id = id.substring(processId.length() + 1);
        }
        InputParameter input = new InputParameter(id);
        // process type array
        if (inputNode.isArray()) {
            input.setType(processInputParameterType(id, inputNode));
            return input;
        }

        if (inputNode.isTextual()) {
            // input node is a string, treat it to a basic type
            input.setType(processParameterType(true, id, inputNode.asText()));
        }
        // processing type
        JsonNode typeNode = inputNode.get("type");
        if (typeNode != null) {
            input.setType(processInputParameterType(id, typeNode));
        }
        // processing label
        String label = processStringField(LABEL, inputNode.get(LABEL));
        input.setLabel(label);
        // processing secondaryFiles, only valid when type: File or is an array
        // of items: File.
        if (hasFileType(input.getType(), true)) {
            input.setSecondaryFiles(processSecondaryFilesField(id, inputNode.get("secondaryFiles")));
        }
        // processing streamable, only valid when type: File or is an array of
        // items: File.
        if (hasFileType(input.getType(), true)) {
            input.setStreamable(processStreamableField(id, inputNode.get("streamable")));
        }
        // processing doc
        JsonNode docNode = inputNode.get("doc");
        if (docNode != null) {
            input.setDoc(processStringOrStringArrayField("doc", docNode));
        }
        // processing format, only valid when type: File or is an array of
        // items: File.
        if (hasFileType(input.getType(), true)) {
            input.setFormat(processFormatField(id, inputNode.get("format")));
        }
        // processing inputBinding
        JsonNode inputBindingNode = inputNode.get(INPUT_BINDING);
        if (inputBindingNode != null && inputBindingNode.isObject()) {
            input.setInputBinding(processCommandLineBinding(input.getType(), inputBindingNode));
        }
        // processing default
        JsonNode defaultNode = inputNode.get("default");
        if (defaultNode != null) {
            Object defaultValue = InputSettingsParser.bindInputSettingValue(descTop, input, defaultNode);
            input.setDefaultValue(defaultValue);
        } else {
            if (hasNullType(input.getType())) {
                // After input settings was loaded, this type may be changed
                input.getType().setType(new NullType());
                input.setDefaultValue(NullValue.NULL);
            }
        }
        return input;
    }

    private static List<WorkflowOutputParameter> toOutputs(JsonNode outputsNode,
            String processId) throws CWLException {
        List<WorkflowOutputParameter> outputs = null;
        if (outputsNode != null) {
            outputs = new ArrayList<>();
            if (outputsNode.isArray()) {
                Iterator<JsonNode> elements = outputsNode.elements();
                while (elements.hasNext()) {
                    WorkflowOutputParameter output = toOutputParameter(elements.next(), processId);
                    outputs.add(output);
                }
            } else if (outputsNode.isObject()) { // the outputs type is map
                Iterator<Entry<String, JsonNode>> fields = outputsNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    WorkflowOutputParameter output = toOutputParameter(field.getKey(), field.getValue(), processId);
                    outputs.add(output);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, INPUTS, ARRAY_OR_MAP),
                        251);
            }
        }
        return outputs;
    }

    private static WorkflowOutputParameter toOutputParameter(JsonNode outputNode,
            String processId) throws CWLException {
        String id = processStringField("id", outputNode.get("id"));
        if (id == null || id.length() == 0) {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, "id"),
                    251);
        }
        return toOutputParameter(id, outputNode, processId);
    }

    private static WorkflowOutputParameter toOutputParameter(String id,
            JsonNode outputNode,
            String processId) throws CWLException {
        id = replaceProcessId(processId, id);
        WorkflowOutputParameter output = new WorkflowOutputParameter(id);
        JsonNode typeNode = outputNode.get("type");
        if (typeNode != null) {
            output.setType(processOutputParameterType(id, typeNode));
        }
        String label = processStringField(LABEL, outputNode.get(LABEL));
        output.setLabel(label);
        if (hasFileType(output.getType(), false)) {
            output.setSecondaryFiles(processSecondaryFilesField(id, outputNode.get("secondaryFiles")));
        }
        if (hasFileType(output.getType(), false)) {
            output.setStreamable(processStreamableField(id, outputNode.get("streamable")));
        }
        JsonNode docNode = outputNode.get("doc");
        if (docNode != null) {
            output.setDoc(processStringOrStringArrayField("doc", docNode));
        }
        JsonNode outputBindingNode = outputNode.get(OUTPUT_BINDING);
        if (outputBindingNode != null && outputBindingNode.isObject()) {
            output.setOutputBinding(processCommandOutputBinding(outputBindingNode));
        }
        if (hasFileType(output.getType(), false)) {
            output.setFormat(processFormatField(id, outputNode.get("format")));
        }
        List<String> sources = processStringOrStringArrayField("outputSource", outputNode.get("outputSource"));
        List<String> outputSources = new ArrayList<>();
        for (int i = 0; sources != null && i < sources.size(); i++) {
            String source = sources.get(i);
            if (processId != null && source.startsWith(processId + "/")) {
                source = source.substring(processId.length() + 1);
            }
            outputSources.add(source);
        }
        output.setOutputSource(outputSources);
        String linkMergeSymbol = processStringField(LINK_MERGE, outputNode.get(LINK_MERGE));
        if (linkMergeSymbol != null) {
            LinkMergeMethod linkMerge = LinkMergeMethod.findMethod(linkMergeSymbol);
            if (linkMerge != null) {
                output.setLinkMerge(linkMerge);
            }
        }
        return output;
    }

    private static List<WorkflowStep> toSteps(String descTop,
            String descFilePath,
            String owner,
            JsonNode stepsNode,
            String parentNamespace,
            String processId) throws CWLException {
        List<WorkflowStep> steps = null;
        if (stepsNode != null) {
            steps = new ArrayList<>();
            if (stepsNode.isArray()) {
                Iterator<JsonNode> elements = stepsNode.elements();
                while (elements.hasNext()) {
                    JsonNode stepNode = elements.next();
                    String id = processStringField("id", stepNode.get("id"));
                    WorkflowStep step = toWorkflowStep(descTop, descFilePath, owner, id, stepNode, parentNamespace,
                            processId);
                    steps.add(step);
                }
            } else if (stepsNode.isObject()) {
                Iterator<Entry<String, JsonNode>> fields = stepsNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    WorkflowStep step = toWorkflowStep(descTop, descFilePath, owner, field.getKey(), field.getValue(),
                            parentNamespace, processId);
                    steps.add(step);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, STEPS, ARRAY_OR_MAP),
                        251);
            }
        }
        return steps;
    }

    private static WorkflowStep toWorkflowStep(String descTop,
            String descFilePath,
            String owner,
            String id,
            JsonNode stepNode,
            String parentNamespace,
            String processId) throws CWLException {
        id = replaceProcessId(processId, id);
        List<WorkflowStepInput> in = toWorkflowStepInputs(descTop, stepNode.get("in"), parentNamespace, processId, id);
        if (in == null) {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, id + "#in"), 251);
        }
        List<WorkflowStepOutput> out = toWorkflowStepOutputs(stepNode.get("out"), processId, id);
        if (out == null) {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, id + "#out"), 251);
        }
        WorkflowStep step = new WorkflowStep(id, in, out);
        String namespace = parentNamespace == null ? step.getId() : parentNamespace + "/" + step.getId();
        JsonNode runNode = stepNode.get("run");
        if (runNode != null) {
            processRunField(descTop, descFilePath, owner, id, step, runNode, namespace);
        } else {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, id + "#run"), 251);
        }
        step.setRequirements(RequirementParser.processRequirements(descTop, REQUIREMENTS, stepNode.get(REQUIREMENTS)));
        step.setHints(RequirementParser.processHints(descTop, stepNode.get("hints")));
        step.setLabel(processStringField(LABEL, stepNode.get(LABEL)));
        step.setDoc(processStringField("doc", stepNode.get("doc")));
        JsonNode scatterNode = stepNode.get(SCATTER);
        if (scatterNode != null) {
            JsonNode scatterMethodNode = stepNode.get("scatterMethod");
            List<String> scatters = new ArrayList<>();
            if (scatterMethodNode == null) {
                scatters.add(processStringField(SCATTER, scatterNode));
            } else {
                scatters.addAll(processStringArrayField(SCATTER, scatterNode));
                String scatterMethodStr = processStringField("scatterMethod", scatterMethodNode);
                ScatterMethod scatterMethod = null;
                if (scatterMethodStr.equals("dotproduct")) {
                    scatterMethod = ScatterMethod.DOTPRODUCT;
                } else if (scatterMethodStr.equals("nested_crossproduct")) {
                    scatterMethod = ScatterMethod.NESTED_CROSSPRODUCT;
                } else if (scatterMethodStr.equals("flat_crossproduct")) {
                    scatterMethod = ScatterMethod.FLAT_CROSSPRODUCT;
                } else {
                    throw new CWLException(
                            ResourceLoader.getMessage("cwl.parser.invalid.scatter.method", scatterMethodStr),
                            251);
                }
                step.setScatterMethod(scatterMethod);
            }
            step.setScatter(scatters);
        }
        return resovleDependencies(step);
    }

    private static void processRunField(String descTop,
            String descFilePath,
            String owner,
            String id,
            WorkflowStep step,
            JsonNode runNode,
            String namespace) throws CWLException {
        if (runNode.isTextual()) {
            String runId = runNode.asText();
            if (runId.startsWith("#")) {
                // run may be a reference of workflow defined in the same
                // description file
                step.setRunId(runId);
            } else if (runId.endsWith(".cwl")) {
                // run may be a workflow defined in external file
                String descriptionFile = IOUtil.resolveImportURI(descTop, runId);
                CWLProcess cwlProcess = CWLParser.yieldCWLProcessObject(new File(descriptionFile), null, namespace);
                step.setRun(cwlProcess);
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_FIELD,
                                id + "#" + runId,
                                "The value should either starts with '#' or ends with '.cwl'."),
                        251);
            }
        } else {
            CWLProcess cwlProcess = null;
            JsonNode importNode = runNode.get(IMPORT);
            if (importNode != null) {
                // run filed may be a $import directive
                if (importNode.isTextual()) {
                    String descriptionFile = IOUtil.resolveImportURI(descTop, importNode.asText());
                    cwlProcess = CWLParser.yieldCWLProcessObject(new File(descriptionFile), null, namespace);
                } else {
                    throw new CWLException(
                            ResourceLoader.getMessage(CWL_PARSER_INVALID_FIELD,
                                    id + "#$import",
                                    "The value should be a string."),
                            251);
                }
            } else {
                // run filed may be a nested workflow
                cwlProcess = CWLParser.processCWLDescription(descFilePath, owner, runNode, namespace);
            }
            step.setRun(cwlProcess);
        }
    }

    private static String replaceProcessId(String processId, String id) {
        if (processId != null && id.startsWith(processId + "/")) {
            id = id.substring(processId.length() + 1);
        }
        return id;
    }

    private static String replaceSrcId(String processId, String stepId, String srcId) {
        if (processId != null && srcId != null && srcId.startsWith(processId + "/")) {
            srcId = srcId.substring(processId.length() + 1);
            if (srcId.startsWith(stepId + "/")) {
                srcId = srcId.substring(stepId.length() + 1);
            }
        }
        return srcId;
    }

    private static List<String> addStepNamespace(List<String> sources,
            String parentNamespace,
            String processId,
            String stepId) {
        List<String> expandedSources = null;
        if (sources != null) {
            expandedSources = new ArrayList<>();
            for (String source : sources) {
                source = replaceSrcId(processId, stepId, source);
                if (source != null && source.indexOf('/') == -1) {
                    expandedSources.add(source);
                } else {
                    expandedSources.add(parentNamespace == null ? source : parentNamespace + "/" + source);
                }
            }
        }
        return expandedSources;
    }

    private static List<WorkflowStepInput> toWorkflowStepInputs(String descTop,
            JsonNode inNode,
            String parentNamespace,
            String processId,
            String stepId) throws CWLException {
        List<WorkflowStepInput> in = null;
        if (inNode != null) {
            in = new ArrayList<>();
            if (inNode.isArray()) {
                Iterator<JsonNode> elements = inNode.elements();
                while (elements.hasNext()) {
                    JsonNode stepInputNode = elements.next();
                    String id = processStringField("id", stepInputNode.get("id"));
                    WorkflowStepInput stepInput = toWorkflowStepInput(descTop, id, stepInputNode, parentNamespace,
                            processId, stepId);
                    in.add(stepInput);
                }
            } else if (inNode.isObject()) {
                Iterator<Entry<String, JsonNode>> fields = inNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    WorkflowStepInput stepInput = toWorkflowStepInput(descTop, field.getKey(), field.getValue(),
                            parentNamespace, processId, stepId);
                    in.add(stepInput);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, "in", ARRAY_OR_MAP),
                        251);
            }
        }
        return in;
    }

    private static WorkflowStepInput toWorkflowStepInput(String descTop,
            String id,
            JsonNode stepInputNode,
            String parentNamespace,
            String processId,
            String stepId) throws CWLException {
        if (processId != null && id.startsWith(processId + "/")) {
            id = id.substring(processId.length() + 1);
            if (id.startsWith(stepId + "/")) {
                id = id.substring(stepId.length() + 1);
            }
        }
        WorkflowStepInput stepInput = new WorkflowStepInput(id);
        if (stepInputNode.isTextual()) {
            stepInput.setSource(
                    addStepNamespace(Arrays.asList(stepInputNode.asText()), parentNamespace, processId, stepId));
        } else if (stepInputNode.isArray()) {
            stepInput.setSource(addStepNamespace(processStringArrayField(SOURCE, stepInputNode), parentNamespace,
                    processId, stepId));
        } else {
            toWorkflowStepInput(descTop, id, stepInput, stepInputNode, parentNamespace, processId, stepId);
        }
        return stepInput;
    }

    private static void toWorkflowStepInput(String descTop,
            String id,
            WorkflowStepInput stepInput,
            JsonNode stepInputNode,
            String parentNamespace,
            String processId,
            String stepId) throws CWLException {
        JsonNode sourceNode = stepInputNode.get(SOURCE);
        if (sourceNode != null) {
            if (sourceNode.isTextual()) {
                stepInput.setSource(addStepNamespace(Arrays.asList(processStringField(SOURCE, sourceNode)),
                        parentNamespace, processId, stepId));
            } else if (sourceNode.isArray()) {
                stepInput.setSource(addStepNamespace(processStringArrayField(SOURCE, sourceNode), parentNamespace,
                        processId, stepId));
            }
        }
        JsonNode linkMergeNode = stepInputNode.get(LINK_MERGE);
        if (linkMergeNode != null) {
            LinkMergeMethod linkMerge = LinkMergeMethod
                    .findMethod(processStringField(id + "#linkMerge", linkMergeNode));
            if (linkMerge != null) {
                stepInput.setLinkMerge(linkMerge);
            }
        }
        JsonNode defaultNode = stepInputNode.get("default");
        if (defaultNode != null) {
            stepInput.setDefaultValue(toDefaultValue(descTop, id, defaultNode));
        }
        stepInput.setValueFrom(processExpressionField(id + "#valueFrom", stepInputNode.get("valueFrom")));
    }

    private static Object toDefaultValue(String descTop,
            String id,
            JsonNode defaultNode) throws CWLException {
        Object value = null;
        if (defaultNode != null) {
            if (defaultNode.isTextual()) {
                value = defaultNode.asText();
            } else if (defaultNode.isInt()) {
                value = Integer.valueOf(defaultNode.asInt());
            } else if (defaultNode.isLong()) {
                value = Long.valueOf(defaultNode.asLong());
            } else if (defaultNode.isFloat()) {
                value = Float.valueOf(defaultNode.floatValue());
            } else if (defaultNode.isDouble()) {
                value = Double.valueOf(defaultNode.asDouble());
            } else if (defaultNode.isBoolean()) {
                value = Boolean.valueOf(defaultNode.asBoolean());
            } else if (defaultNode.isArray()) {
                value = toDefaultArrayValue(descTop, id, defaultNode);
            } else if (defaultNode.isObject()) {
                value = toDefaultObjectValue(descTop, id, defaultNode);
            }
        }
        return value;
    }

    private static List<Object> toDefaultArrayValue(String descTop,
            String id,
            JsonNode defaultNode) throws CWLException {
        List<Object> values = new ArrayList<>();
        Iterator<JsonNode> elements = defaultNode.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            values.add(toDefaultValue(descTop, id, element));
        }
        return values;
    }

    private static Object toDefaultObjectValue(String descTop,
            String id,
            JsonNode defaultNode) throws CWLException {
        JsonNode classNode = defaultNode.get(CLASS);
        if (classNode != null && classNode.isTextual()) {
            String clazz = classNode.asText();
            if ("File".equals(clazz)) {
                return processCWLFile(descTop, id, defaultNode, true);
            } else if ("Directory".equals(clazz)) {
                return processCWLDirectory(descTop, id, defaultNode, true);
            }
        }
        return null;
    }

    private static List<WorkflowStepOutput> toWorkflowStepOutputs(JsonNode outNode,
            String processId,
            String stepId) throws CWLException {
        List<WorkflowStepOutput> out = null;
        if (outNode != null) {
            if (outNode.isArray()) {
                out = new ArrayList<>();
                Iterator<JsonNode> elements = outNode.elements();
                while (elements.hasNext()) {
                    processWorkflowStepOutputs(out, elements.next(), processId, stepId);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, "out", "array"),
                        251);
            }
        }
        return out;
    }

    private static void processWorkflowStepOutputs(List<WorkflowStepOutput> out,
            JsonNode stepOutNode,
            String processId,
            String stepId) {
        if (stepOutNode.isTextual()) {
            String id = stepOutNode.asText();
            replaceSrcId(processId, stepId, id);
            out.add(new WorkflowStepOutput(id));
        } else if (stepOutNode.isObject()) {
            JsonNode idNode = stepOutNode.get("id");
            if (idNode != null && idNode.isTextual()) {
                String id = idNode.asText();
                replaceSrcId(processId, stepId, id);
                out.add(new WorkflowStepOutput(id));
            }
        }
    }

    private static WorkflowStep resovleDependencies(WorkflowStep step) {
        List<String> dependencies = new ArrayList<>();
        for (WorkflowStepInput stepInput : step.getIn()) {
            List<String> soruce = stepInput.getSource();
            if (soruce != null) {
                dependencies.addAll(soruce);
            }
        }
        if (!dependencies.isEmpty()) {
            step.setDependencies(dependencies);
        }
        return step;
    }

    private static void validateWorkflowRequirements(Workflow workflow) throws CWLException {
        if (workflow == null) {
            throw new IllegalArgumentException("workflow is null.");
        }
        // check whether SubworkflowFeatureRequirement exists if nested workflow
        // is found in any step
        List<WorkflowStep> steps = workflow.getSteps();
        if (steps == null) {
            steps = Collections.emptyList();
        }
        String stepMissingRequirement = findStepMissingRequirement(steps);
        // try to find SubworkflowFeatureRequirement in workflow requirements
        findSubworkflowFeatureReq(stepMissingRequirement, workflow);
    }

    private static String findStepMissingRequirement(List<WorkflowStep> steps) {
        String stepMissingRequirement = null;
        for (WorkflowStep step : steps) {
            CWLProcess cwlProcess = step.getRun();
            if (cwlProcess instanceof Workflow) {
                List<Requirement> requirements = step.getRequirements();
                if (requirements == null) {
                    requirements = Collections.emptyList();
                }
                boolean found = findSubworkflowFeatureReq(requirements);
                if (!found) {
                    stepMissingRequirement = step.getId();
                    break;
                }
            }
        }
        return stepMissingRequirement;
    }

    private static boolean findSubworkflowFeatureReq(List<Requirement> requirements) {
        boolean found = false;
        for (Requirement requirement : requirements) {
            if (requirement instanceof SubworkflowFeatureRequirement) {
                found = true;
                break;
            }
        }
        return found;
    }

    private static void findSubworkflowFeatureReq(String stepMissingRequirement,
            Workflow workflow) throws CWLException {
        if (stepMissingRequirement != null) {
            boolean found = false;
            List<Requirement> requirements = workflow.getRequirements();
            if (requirements == null) {
                requirements = Collections.emptyList();
            }
            for (Requirement requirement : requirements) {
                if (requirement instanceof SubworkflowFeatureRequirement) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.parser.requirement.required",
                                "SubworkflowFeatureRequirement",
                                stepMissingRequirement),
                        251);
            }
        }
    }
}
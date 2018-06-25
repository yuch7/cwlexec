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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.ParameterType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandLineBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandOutputBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.OutputBindingGlob;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.FileType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullValue;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * Parses CWL CommandLineTool document description file
 */
final class CommandLineToolParser extends BaseParser {

    private static final String FORMAT = "format";
    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    private static final String ARGUMENTS = "arguments";
    private static final String BASE_COMMAND = "baseCommand";

    private CommandLineToolParser() {
    }

    protected static CommandLineTool yieldCommandLineTool(String descTop,
            JsonNode node) throws CWLException {
        CommandLineTool commandLineTool = null;
        List<Requirement> requirements = RequirementParser.processRequirements(descTop, REQUIREMENTS,
                node.get(REQUIREMENTS));
        // processing id
        String id = processStringField("id", node.get("id"));
        // processing inputs
        List<CommandInputParameter> inputs = toCommandInputs(descTop, node.get(INPUTS), id);
        if (inputs == null) {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, INPUTS), 251);
        }
        // processing outputs
        List<CommandOutputParameter> outputs = toCommandOutputs(descTop, node.get("outputs"), id);
        if (outputs == null) {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, "outputs"), 251);
        }
        // create CommandLineTool
        commandLineTool = new CommandLineTool(inputs, outputs);
        commandLineTool.setId(id);
        // processing requirements
        commandLineTool.setRequirements(requirements);
        // processing hints
        commandLineTool.setHints(RequirementParser.processHints(descTop, node.get("hints")));
        // processing label
        String label = processStringField(LABEL, node.get(LABEL));
        commandLineTool.setLabel(label);
        // processing doc
        String doc = processStringField("doc", node.get("doc"));
        commandLineTool.setDoc(doc);
        // processing baseCommand field
        List<String> baseCommand = toBaseCommand(node.get(BASE_COMMAND));
        commandLineTool.setBaseCommand(baseCommand);
        // processing arguments
        List<CommandLineBinding> arguments = toArguments(node.get(ARGUMENTS));
        commandLineTool.setArguments(arguments);
        // processing stdin
        CWLFieldValue stdin = processExpressionField("stdin", node.get("stdin"));
        commandLineTool.setStdin(stdin);
        // processing stderr
        CWLFieldValue stderr = processExpressionField(STDERR, node.get(STDERR));
        commandLineTool.setStderr(resetSTD(outputs, STDERR, stderr));
        // processing stdout
        CWLFieldValue stdout = processExpressionField(STDOUT, node.get(STDOUT));
        commandLineTool.setStdout(resetSTD(outputs, STDOUT, stdout));
        // processing successCodes
        int[] successCodes = processExitCodeField("successCodes", node.get("successCodes"));
        commandLineTool.setSuccessCodes(successCodes);
        return commandLineTool;
    }

    private static List<CommandInputParameter> toCommandInputs(String descTop,
            JsonNode inputsNode,
            String processId) throws CWLException {
        List<CommandInputParameter> inputs = null;
        if (inputsNode != null) {
            inputs = new ArrayList<>();
            if (inputsNode.isArray()) {
                Iterator<JsonNode> elements = inputsNode.elements();
                while (elements.hasNext()) {
                    CommandInputParameter input = toCommandInputParameter(descTop, elements.next(), processId);
                    inputs.add(input);
                }
            } else if (inputsNode.isObject()) { // the inputs type is map
                Iterator<Entry<String, JsonNode>> fields = inputsNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    CommandInputParameter input = toCommandInputParameter(descTop, field.getKey(), field.getValue(),
                            processId);
                    inputs.add(input);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, INPUTS, "array or map"), 251);
            }
        }
        return inputs;
    }

    private static CommandInputParameter toCommandInputParameter(String descTop,
            JsonNode inputNode,
            String processId) throws CWLException {
        String id = processStringField("id", inputNode.get("id"));
        if (id == null || id.length() == 0) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.field.required.in", "id", "CommandInputParameter"), 251);
        }
        return toCommandInputParameter(descTop, id, inputNode, processId);
    }

    private static CommandInputParameter toCommandInputParameter(String descTop,
            String id,
            JsonNode inputNode,
            String processId) throws CWLException {
        if (processId != null && id.startsWith(processId + "/")) {
            id = id.substring(processId.length() + 1);
        }
        CommandInputParameter input = new CommandInputParameter(id);
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
            input.setFormat(processFormatField(id, inputNode.get(FORMAT)));
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

    private static List<CommandOutputParameter> toCommandOutputs(String parentPath,
            JsonNode outputsNode,
            String processId) throws CWLException {
        List<CommandOutputParameter> outputs = null;
        if (outputsNode != null) {
            outputs = new ArrayList<>();
            if (outputsNode.isArray()) {
                Iterator<JsonNode> elements = outputsNode.elements();
                while (elements.hasNext()) {
                    CommandOutputParameter output = toCommandOutputParameter(elements.next(), processId);
                    outputs.add(output);
                }
            } else if (outputsNode.isObject()) {
                if (!importedOuputs(parentPath, outputsNode, outputs, processId)) {
                    Iterator<Entry<String, JsonNode>> fields = outputsNode.fields();
                    while (fields.hasNext()) {
                        Entry<String, JsonNode> field = fields.next();
                        CommandOutputParameter output = toCommandOutputParameter(field.getKey(), field.getValue(),
                                processId);
                        outputs.add(output);
                    }
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, INPUTS, "array or map"), 251);
            }
        }
        return outputs;
    }

    private static boolean importedOuputs(String parentPath,
            JsonNode outputsNode,
            List<CommandOutputParameter> outputs,
            String processId) throws CWLException {
        boolean imported = false;
        JsonNode importNode = outputsNode.get(IMPORT);
        if (importNode != null) {
            if (importNode.isTextual()) {
                String importFilePath = IOUtil.resolveImportURI(parentPath, importNode.asText());
                File importFile = IOUtil.yieldFile(importFilePath, null, null, true);
                try {
                    outputs.addAll(toCommandOutputs(parentPath, IOUtil.toJsonNode(importFile), processId));
                } catch (IOException e) {
                    throw new CWLException(
                            ResourceLoader.getMessage(CWL_PARSER_INVALID_FIELD, "output", IMPORT), 251);
                }
                imported = true;
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_FIELD, "output", IMPORT), 251);
            }
        }
        return imported;
    }

    private static CommandOutputParameter toCommandOutputParameter(JsonNode outputNode,
            String processId) throws CWLException {
        String id = processStringField("id", outputNode.get("id"));
        if (id == null || id.length() == 0) {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, "id"), 251);
        }
        return toCommandOutputParameter(id, outputNode, processId);
    }

    private static CommandOutputParameter toCommandOutputParameter(String id,
            JsonNode outputNode,
            String processId) throws CWLException {
        if (processId != null && id.startsWith(processId + "/")) {
            id = id.substring(processId.length() + 1);
        }
        CommandOutputParameter output = new CommandOutputParameter(id);
        if (outputNode.isTextual()) {
            String symble = outputNode.asText();
            if (STDOUT.equals(symble)) {
                return toSTDIOParameter(id, outputNode, STDOUT);
            } else if (STDERR.equals(symble)) {
                return toSTDIOParameter(id, outputNode, STDERR);
            } else {
                // output node is a string, treat it to a basic type
                output.setType(processParameterType(false, id, symble));
            }
        }
        JsonNode typeNode = outputNode.get("type");
        if (typeNode != null && typeNode.isTextual()) {
            String symble = typeNode.asText();
            if (STDOUT.equals(symble)) {
                return toSTDIOParameter(id, outputNode, STDOUT);
            } else if (STDERR.equals(symble)) {
                return toSTDIOParameter(id, outputNode, STDERR);
            }
        }
        if (typeNode != null) {
            output.setType(processOutputParameterType(id, typeNode));
        }
        processCommandOutputParameter(id, outputNode, output);
        return output;
    }

    private static void processCommandOutputParameter(String id,
            JsonNode outputNode,
            CommandOutputParameter output) throws CWLException {
        // processing label
        String label = processStringField(LABEL, outputNode.get(LABEL));
        output.setLabel(label);
        // processing secondaryFiles, only valid when type: File or is an array
        // of items: File.
        if (hasFileType(output.getType(), false)) {
            output.setSecondaryFiles(processSecondaryFilesField(id, outputNode.get("secondaryFiles")));
        }
        // processing streamable, only valid when type: File or is an array of
        // items: File.
        if (hasFileType(output.getType(), false)) {
            output.setStreamable(processStreamableField(id, outputNode.get("streamable")));
        }
        // processing doc
        JsonNode docNode = outputNode.get("doc");
        if (docNode != null) {
            output.setDoc(processStringOrStringArrayField("doc", docNode));
        }
        // processing outputBinding
        JsonNode outputBindingNode = outputNode.get(OUTPUT_BINDING);
        if (outputBindingNode != null && outputBindingNode.isObject()) {
            output.setOutputBinding(processCommandOutputBinding(outputBindingNode));
        }
        // processing format, only valid when type: File or is an array of
        // items: File.
        if (hasFileType(output.getType(), false)) {
            output.setFormat(processFormatField(id, outputNode.get(FORMAT)));
        }
    }

    private static CommandOutputParameter toSTDIOParameter(String id, JsonNode outputNode, String std) throws CWLException {
        CommandOutputParameter output = new CommandOutputParameter(id);
        ParameterType type = new ParameterType();
        type.setType(new FileType());
        output.setType(type);
        output.setStreamable(true);
        output.setFormat(processFormatField(id, outputNode.get(FORMAT)));
        CommandOutputBinding outputBinding = new CommandOutputBinding();
        OutputBindingGlob glob = new OutputBindingGlob();
        CWLFieldValue globExpr = new CWLFieldValue();
        globExpr.setValue(String.format("random_%s_%s", std, CommonUtil.getRandomStr()));
        glob.setGlobExpr(globExpr);
        outputBinding.setGlob(glob);
        output.setOutputBinding(outputBinding);
        return output;
    }

    private static List<String> toBaseCommand(JsonNode commandNode) throws CWLException {
        List<String> baseCommand = null;
        if (commandNode != null) {
            baseCommand = new ArrayList<>();
            if (commandNode.isTextual()) {
                baseCommand.add(commandNode.asText());
            } else if (commandNode.isArray()) {
                List<String> commands = processStringArrayField(BASE_COMMAND, commandNode);
                if (commands != null) {
                    baseCommand.addAll(commands);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, BASE_COMMAND, "string or array"), 251);
            }
        }
        return baseCommand;
    }

    private static List<CommandLineBinding> toArguments(JsonNode argumentsNode) throws CWLException {
        List<CommandLineBinding> arguments = null;
        if (argumentsNode != null) {
            arguments = new ArrayList<>();
            if (argumentsNode.isArray()) {
                Iterator<JsonNode> elements = argumentsNode.elements();
                while (elements.hasNext()) {
                    JsonNode element = elements.next();
                    if (element.isTextual()) {
                        arguments.add(toArgument(element));
                    } else if (element.isObject()) {
                        arguments.add(processCommandLineBinding(null, element));
                    } else {
                        throw new CWLException(
                                ResourceLoader.getMessage("cwl.parser.invalid.array.type", ARGUMENTS), 251);
                    }
                }
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, ARGUMENTS, "array"), 251);
            }
        }
        return arguments;
    }

    private static CommandLineBinding toArgument(JsonNode element) {
        CommandLineBinding binding = new CommandLineBinding();
        CWLFieldValue valueFrom = new CWLFieldValue();
        if (element.asText().startsWith("$")) {
            valueFrom.setExpression(element.asText());
        } else {
            String value = element.asText();
            if (value.indexOf(System.getProperty("line.separator")) != -1) {
                value = "'" + value.replaceAll("'", "'\"'\"'") + "'";
            }
            valueFrom.setValue(value);
        }
        binding.setValueFrom(valueFrom);
        return binding;
    }

    private static CWLFieldValue resetSTD(List<CommandOutputParameter> outputs, String std, CWLFieldValue stdExpr) {
        for (CommandOutputParameter output : outputs) {
            if ((output.getType().getType()) != null
                    && (output.getType().getType().getSymbol() == CWLTypeSymbol.FILE)) {
                CommandOutputBinding binding = output.getOutputBinding();
                if (binding != null) {
                    CWLFieldValue newExprPlaceholder = resetSTDGlob(binding.getGlob(), std, stdExpr);
                    if (newExprPlaceholder != null) {
                        return newExprPlaceholder;
                    }
                }
            }
        }
        return stdExpr;
    }

    private static CWLFieldValue resetSTDGlob(OutputBindingGlob glob, String std, CWLFieldValue stdExpr) {
        CWLFieldValue newExprPlaceholder = null;
        CWLFieldValue globExpr = glob.getGlobExpr();
        if (globExpr != null) {
            String value = globExpr.getValue();
            if (value != null && value.startsWith("random_" + std)) {
                newExprPlaceholder = new CWLFieldValue();
                if (stdExpr != null) {
                    String exprValue = stdExpr.getValue();
                    String exprExpr = stdExpr.getExpression();
                    if (exprValue != null || exprExpr != null) {
                        globExpr.setValue(exprValue);
                        globExpr.setExpression(exprExpr);
                        newExprPlaceholder.setValue(exprValue);
                        newExprPlaceholder.setExpression(exprExpr);
                    } else {
                        newExprPlaceholder.setValue(value);
                    }
                } else {
                    newExprPlaceholder.setValue(value);
                }
            }
        }
        return newExprPlaceholder;
    }

    private static int[] processExitCodeField(String key, JsonNode arrayNode) throws CWLException {
        List<Integer> array = null;
        int[] intArray = null;
        if (arrayNode != null) {
            array = new ArrayList<>();
            Iterator<JsonNode> elements = arrayNode.elements();
            while (elements.hasNext()) {
                JsonNode element = elements.next();
                if (element.isInt()) {
                    array.add(element.asInt());
                } else {
                    throw new CWLException(
                            ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, "array<int>"), 251);
                }
            }
            intArray = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                intArray[i] = array.get(i);
            }
        }
        return intArray;
    }
}

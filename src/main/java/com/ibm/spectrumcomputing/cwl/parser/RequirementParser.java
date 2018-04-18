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
package com.ibm.spectrumcomputing.cwl.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Dirent;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvVarRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvironmentDef;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InitialWorkDirRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.MultipleInputFeatureRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ResourceRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ShellCommandRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.StepInputExpressionRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.SubworkflowFeatureRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ScatterFeatureRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.SchemaDefRequirement;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * Parses the requirements (or hints)
 */
final class RequirementParser extends BaseParser {

    private static final Logger logger = LoggerFactory.getLogger(RequirementParser.class);

    private static final String INITIAL_WORK_DIR_REQUIREMENT_LISTING = "InitialWorkDirRequirement#listing";
    private static final String ENV_VAR_REQUIREMENT_ENV_DEF = "EnvVarRequirement#envDef";
    private static final String INITIAL_WORK_DIR_REQUIREMENT = "InitialWorkDirRequirement";
    private static final String REQUIREMENT = "requirement";

    private RequirementParser() {
    }

    protected static List<Requirement> processHints(String descTop, JsonNode hintsNode) {
        List<Requirement> hints = null;
        if (hintsNode != null) {
            if (hintsNode.isArray()) {
                hints = new ArrayList<>();
                Iterator<JsonNode> elements = hintsNode.elements();
                while (elements.hasNext()) {
                    tryToAddHint(hints, descTop, elements.next());
                }
            } else if (hintsNode.isObject()) { // the requirements type is map
                hints = new ArrayList<>();
                Iterator<Entry<String, JsonNode>> fields = hintsNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    try {
                        Requirement requirement = processRequirement(descTop, field.getKey(), field.getValue());
                        hints.add(requirement);
                    } catch (Exception e) {
                        logger.warn("The hint <{}> is unsupported.", field.getKey());
                    }
                }
            }
        }
        return hints;
    }

    protected static List<Requirement> processRequirements(String descTop,
            String key,
            JsonNode requirementsNode) throws CWLException {
        List<Requirement> requirements = new ArrayList<>();
        if (requirementsNode != null) {
            if (requirementsNode.isArray()) {
                Iterator<JsonNode> elements = requirementsNode.elements();
                while (elements.hasNext()) {
                    Requirement requirement = processRequirement(descTop, elements.next());
                    requirements.add(requirement);
                }
            } else if (requirementsNode.isObject()) {
                Iterator<Entry<String, JsonNode>> fields = requirementsNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    Requirement requirement = processRequirement(descTop, field.getKey(), field.getValue());
                    requirements.add(requirement);
                }
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, "array or map"), 251);
            }
        }
        return requirements;
    }

    private static void tryToAddHint(List<Requirement> hints, String descTop, JsonNode jsonNode) {
        try {
            Requirement requirement = processRequirement(descTop, jsonNode);
            hints.add(requirement);
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
    }

    private static Requirement processRequirement(String parentPath,
            String clazz,
            JsonNode requirementNode) throws CWLException {
        Requirement requirement = null;
        if (clazz != null && requirementNode.isObject()) {
            switch (clazz) {
            case "InlineJavascriptRequirement":
                requirement = processInlineJavascriptRequirement(parentPath, requirementNode);
                break;
            case "EnvVarRequirement":
                requirement = processEnvVarRequirement(requirementNode);
                break;
            case "DockerRequirement":
                requirement = processDockerRequirement(parentPath, requirementNode);
                break;
            case "ShellCommandRequirement":
                requirement = new ShellCommandRequirement();
                break;
            case "ResourceRequirement":
                requirement = processResourceRequirement(requirementNode);
                break;
            case "MultipleInputFeatureRequirement":
                requirement = new MultipleInputFeatureRequirement();
                break;
            case INITIAL_WORK_DIR_REQUIREMENT:
                requirement = processWorkDirRequirement(parentPath, requirementNode);
                break;
            case "ScatterFeatureRequirement":
                requirement = new ScatterFeatureRequirement();
                break;
            case "SubworkflowFeatureRequirement":
                requirement = new SubworkflowFeatureRequirement();
                break;
            case "StepInputExpressionRequirement":
                requirement = new StepInputExpressionRequirement();
                break;
            case "SchemaDefRequirement":
                requirement = processSchemaDefRequirement(requirementNode);
                break;
            default:
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.parser.field.unsupported", REQUIREMENT, clazz),
                        33);
            }
        }
        return requirement;
    }

    private static Requirement processRequirement(String parentPath,
            JsonNode requirementNode) throws CWLException {
        Requirement requirement = null;
        if (requirementNode.isObject()) {
            JsonNode importNode = requirementNode.get(IMPORT);
            if (importNode != null) {
                if (importNode.isTextual()) {
                    String importFilePath = IOUtil.resolveImportURI(parentPath, importNode.asText());
                    parentPath = IOUtil.resolveBaseURI(importFilePath);
                    requirementNode = importRequirement(importFilePath);
                } else {
                    throw new CWLException(
                            ResourceLoader.getMessage(CWL_PARSER_INVALID_FIELD, REQUIREMENT, IMPORT),
                            251);
                }
            }
            JsonNode classNode = requirementNode.get(CLASS);
            if (classNode != null && classNode.isTextual()) {
                requirement = processRequirement(parentPath, classNode.asText(), requirementNode);
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, REQUIREMENT, CLASS),
                        251);
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, REQUIREMENT, "requirement object"),
                    251);
        }
        return requirement;
    }

    private static InlineJavascriptRequirement processInlineJavascriptRequirement(String parentPath,
            JsonNode requirementNode) throws CWLException {
        InlineJavascriptRequirement jsRequirement = new InlineJavascriptRequirement();
        JsonNode expressionLibNode = requirementNode.get("expressionLib");
        if (expressionLibNode != null) {
            List<String> expressionLib = new ArrayList<>();
            Iterator<JsonNode> elements = expressionLibNode.elements();
            while (elements.hasNext()) {
                JsonNode element = elements.next();
                processExpressionLib(expressionLib, parentPath, element);

            }
            jsRequirement.setExpressionLib(expressionLib);
        }
        return jsRequirement;
    }

    private static void processExpressionLib(List<String> expressionLib,
            String parentPath,
            JsonNode element) throws CWLException {
        if (element.isTextual()) {
            expressionLib.add(element.asText());
        } else if (element.isObject()) {
            Iterator<Entry<String, JsonNode>> fields = element.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> field = fields.next();
                if (IMPORT.equals(field.getKey()) || "$include".equals(field.getKey())) {
                    Path path = Paths.get(parentPath, field.getValue().asText());
                    if (path.toFile().exists()) {
                        expressionLib.add(IOUtil.readJSFile(path).toString());
                    }
                }
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, "InlineJavascriptRequirement", "string"),
                    251);
        }
    }

    private static EnvVarRequirement processEnvVarRequirement(JsonNode requirementNode) throws CWLException {
        List<EnvironmentDef> envDefs = new ArrayList<>();
        JsonNode envDefsNode = requirementNode.get("envDef");
        if (envDefsNode != null) {
            if (envDefsNode.isArray()) {
                Iterator<JsonNode> elements = envDefsNode.elements();
                while (elements.hasNext()) {
                    JsonNode element = elements.next();
                    String envName = processStringField(ENV_VAR_REQUIREMENT_ENV_DEF, element.get("envName"));
                    CWLFieldValue envValue = processExpressionField(ENV_VAR_REQUIREMENT_ENV_DEF,
                            element.get("envValue"));
                    envDefs.add(new EnvironmentDef(envName, envValue));
                }
            } else if (envDefsNode.isObject()) { // map
                Iterator<Entry<String, JsonNode>> fields = envDefsNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    String envName = field.getKey();
                    CWLFieldValue envValue = processExpressionField(ENV_VAR_REQUIREMENT_ENV_DEF, field.getValue());
                    envDefs.add(new EnvironmentDef(envName, envValue));
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE,
                                ENV_VAR_REQUIREMENT_ENV_DEF, "array or map"),
                        251);
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, ENV_VAR_REQUIREMENT_ENV_DEF),
                    251);
        }
        return new EnvVarRequirement(envDefs);
    }

    private static DockerRequirement processDockerRequirement(String parentPath,
            JsonNode requirementNode) throws CWLException {
        DockerRequirement dockerRequirement = new DockerRequirement();
        dockerRequirement.setDockerPull(
                processStringField("DockerRequirement#dockerPull", requirementNode.get("dockerPull")));
        dockerRequirement.setDockerLoad(
                processStringField("DockerRequirement#dockerLoad", requirementNode.get("dockerLoad")));
        // import the value of dockerFile from external file if necessary
        String dockerFile = processStringField("DockerRequirement#dockerFile", requirementNode.get("dockerFile"));
        if (dockerFile != null && dockerFile.startsWith(IMPORT)) {
            String[] parts = dockerFile.split(":");
            if (parts.length == 2 && parts[0].trim().equals(IMPORT)) {
                dockerFile = importDockerFile(parentPath, parts[1].trim());
            }
        }
        dockerRequirement.setDockerFile(dockerFile);
        dockerRequirement.setDockerImport(
                processStringField("DockerRequirement#dockerImport", requirementNode.get("dockerImport")));
        dockerRequirement.setDockerImageId(
                processStringField("DockerRequirement#dockerImageId", requirementNode.get("dockerImageId")));
        dockerRequirement.setDockerOutputDirectory(
                processStringField("DockerRequirement#dockerOutputDirectory",
                        requirementNode.get("dockerOutputDirectory")));
        return dockerRequirement;
    }

    private static ResourceRequirement processResourceRequirement(JsonNode requirementNode) {
        ResourceRequirement resourceRequirement = new ResourceRequirement();
        processCores(resourceRequirement, requirementNode);
        processRam(resourceRequirement, requirementNode);
        processTmpdir(resourceRequirement, requirementNode);
        processOutdir(resourceRequirement, requirementNode);
        return resourceRequirement;
    }

    private static void processCores(ResourceRequirement resourceRequirement, JsonNode requirementNode) {
        // coresMin
        try {
            resourceRequirement.setCoresMin(
                    processLongField("ResourceRequirement#coresMin", requirementNode.get("coresMin")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        try {
            resourceRequirement.setCoresMinExpr(
                    processExpressionField("ResourceRequirement#coresMin", requirementNode.get("coresMin")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        // coresMax
        try {
            resourceRequirement.setCoresMax(
                    processLongField("ResourceRequirement#coresMax", requirementNode.get("coresMax")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        try {
            resourceRequirement.setCoresMaxExpr(
                    processExpressionField("ResourceRequirement#coresMax", requirementNode.get("coresMax")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
    }

    private static void processRam(ResourceRequirement resourceRequirement, JsonNode requirementNode) {
        // ramMin
        try {
            resourceRequirement.setRamMin(
                    processLongField("ResourceRequirement#ramMin", requirementNode.get("ramMin")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        try {
            resourceRequirement.setRamMinExpr(
                    processExpressionField("ResourceRequirement#ramMin", requirementNode.get("ramMin")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        // ramMax
        try {
            resourceRequirement.setRamMax(
                    processLongField("ResourceRequirement#ramMax", requirementNode.get("ramMax")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        try {
            resourceRequirement.setRamMaxExpr(
                    processExpressionField("ResourceRequirement#ramMax", requirementNode.get("ramMax")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
    }

    private static void processTmpdir(ResourceRequirement resourceRequirement, JsonNode requirementNode) {
        // tmpdirMin
        try {
            resourceRequirement.setTmpdirMin(
                    processLongField("ResourceRequirement#tmpdirMin", requirementNode.get("tmpdirMin")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        try {
            resourceRequirement.setTmpdirMinExpr(
                    processExpressionField("ResourceRequirement#tmpdirMin", requirementNode.get("tmpdirMin")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        // tmpdirMax
        try {
            resourceRequirement.setTmpdirMax(
                    processLongField("ResourceRequirement#tmpdirMax", requirementNode.get("tmpdirMax")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        try {
            resourceRequirement.setTmpdirMaxExpr(
                    processExpressionField("ResourceRequirement#tmpdirMax", requirementNode.get("tmpdirMax")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
    }

    private static void processOutdir(ResourceRequirement resourceRequirement, JsonNode requirementNode) {
        // outdirMin
        try {
            resourceRequirement.setOutdirMin(
                    processLongField("ResourceRequirement#outdirMin", requirementNode.get("outdirMin")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        try {
            resourceRequirement.setOutdirMinExpr(
                    processExpressionField("ResourceRequirement#outdirMin", requirementNode.get("outdirMin")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        // outdirMax
        try {
            resourceRequirement.setOutdirMax(
                    processLongField("ResourceRequirement#outdirMax", requirementNode.get("outdirMax")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
        try {
            resourceRequirement.setOutdirMaxExpr(
                    processExpressionField("ResourceRequirement#outdirMax", requirementNode.get("outdirMax")));
        } catch (CWLException e) {
            logger.trace(e.getMessage());
        }
    }

    private static InitialWorkDirRequirement processWorkDirRequirement(String descTop,
            JsonNode requirementNode) throws CWLException {
        JsonNode listingNode = requirementNode.get("listing");
        if (listingNode == null) {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, INITIAL_WORK_DIR_REQUIREMENT_LISTING),
                    251);
        }
        InitialWorkDirRequirement workdirRequirement = new InitialWorkDirRequirement();
        if (listingNode.isArray()) {
            // Declare support types
            List<Dirent> dirents = new ArrayList<>();
            List<CWLFileBase> files = new ArrayList<>();
            List<CWLFileBase> dirs = new ArrayList<>();
            List<CWLFieldValue> exprs = new ArrayList<>();
            Iterator<JsonNode> elements = listingNode.elements();
            while (elements.hasNext()) {
                processWorkdirElement(descTop, elements.next(), dirents, files, dirs, exprs);
            }
            if (!dirents.isEmpty()) {
                workdirRequirement.setDirentListing(dirents);
            }
            if (!files.isEmpty()) {
                workdirRequirement.setFileListing(files);
            }
            if (!dirs.isEmpty()) {
                workdirRequirement.setDirListing(dirs);
            }
            if (!exprs.isEmpty()) {
                workdirRequirement.setExprListing(exprs);
            }
        } else if (listingNode.isTextual()) {
            CWLFieldValue listing = processExpressionField(INITIAL_WORK_DIR_REQUIREMENT_LISTING, listingNode);
            workdirRequirement.setListing(listing);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, 
                            INITIAL_WORK_DIR_REQUIREMENT_LISTING,
                            "is empty"),
                    251);
        }
        return workdirRequirement;
    }

    private static void processWorkdirElement(String descTop,
            JsonNode workdirNode,
            List<Dirent> dirents,
            List<CWLFileBase> files,
            List<CWLFileBase> dirs,
            List<CWLFieldValue> exprs) throws CWLException {
        if (workdirNode.get("entry") != null) {
            // Dirent parsing array<Dirent>
            CWLFieldValue entry = processWorkDirEntryField(INITIAL_WORK_DIR_REQUIREMENT_LISTING,
                    workdirNode.get("entry"));
            CWLFieldValue entryname = null;
            boolean writable = false;
            if (workdirNode.get("entryname") != null) {
                entryname = processWorkDirEntryField(INITIAL_WORK_DIR_REQUIREMENT_LISTING,
                        workdirNode.get("entryname"));
            }
            if (workdirNode.get("writable") != null) {
                writable = processBooleanField(INITIAL_WORK_DIR_REQUIREMENT_LISTING,
                        workdirNode.get("writable"));
            }
            Dirent dirent = new Dirent(entry);
            dirent.setEntryname(entryname);
            dirent.setWritable(writable);
            dirents.add(dirent);
        } else if (workdirNode.get(CLASS) != null) {
            if ("File".equals(workdirNode.get(CLASS).asText())) {
                // File parsing array<File>
                CWLFile cwlFile = processCWLFile(descTop, INITIAL_WORK_DIR_REQUIREMENT, workdirNode, true);
                files.add(cwlFile);
            } else if ("Directory".equals(workdirNode.get(CLASS).asText())) {
                // Directory parsing array<Directory>
                CWLDirectory cwlDir = processCWLDirectory(descTop, INITIAL_WORK_DIR_REQUIREMENT, workdirNode, true);
                dirs.add(cwlDir);
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE,
                                INITIAL_WORK_DIR_REQUIREMENT_LISTING,
                                "File or Directory is required"),
                        251);
            }
        } else {
            // listing parsing string | Expression
            CWLFieldValue listing = processExpressionField(INITIAL_WORK_DIR_REQUIREMENT_LISTING, workdirNode);
            if (listing == null) {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED,
                                INITIAL_WORK_DIR_REQUIREMENT_LISTING),
                        251);
            } else {
                exprs.add(listing);
            }
        }
    }

    private static JsonNode importRequirement(String importFilePath) throws CWLException {
        try {
            File importFile = IOUtil.yieldFile(importFilePath, null, null, true);
            return IOUtil.toJsonNode(importFile);
        } catch (IOException | CWLException e) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.fail.to.process", importFilePath, e.getMessage()),
                    251);
        }
    }

    private static String importDockerFile(String parentPath, String dockerFilePath) throws CWLException {
        dockerFilePath = IOUtil.resolveImportURI(parentPath, dockerFilePath);
        try {
            File dockerFile = IOUtil.yieldFile(dockerFilePath, null, null, true);
            return IOUtil.read64KiB(dockerFile);
        } catch (CWLException e) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.fail.to.process", dockerFilePath, e.getMessage()),
                    251);
        }
    }

    private static SchemaDefRequirement processSchemaDefRequirement(JsonNode requirementNode) throws CWLException {
        SchemaDefRequirement schemaDefRequirement = new SchemaDefRequirement();
        JsonNode typesNode = requirementNode.get("types");
        if (typesNode != null) {
            if (typesNode.isArray()) {
                Iterator<JsonNode> schemaTypes = typesNode.elements();
                while (schemaTypes.hasNext()) {
                    JsonNode schemaType = schemaTypes.next();
                    schemaDefRequirement.addSchemaType(buildSchemaType(schemaType));
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, "SchemaDefRequirement#types", "is empty"),
                        251);
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, "SchemaDefRequirement#types"), 251);
        }
        return schemaDefRequirement;
    }

    private static CWLType buildSchemaType(JsonNode typeNode) throws CWLException {
        JsonNode typeSymbolNode = typeNode.get("type");
        if (typeSymbolNode != null && typeSymbolNode.isTextual() && ("record".equals(typeSymbolNode.asText()))) {
            InputRecordType recordType = new InputRecordType();
            recordType.setName(processStringField("record#name", typeNode.get("name")));
            recordType.setLabel(processStringField("record#label", typeNode.get(LABEL)));
            JsonNode fieldNodes = typeNode.get("fields");
            if (fieldNodes != null && fieldNodes.isArray()) {
                List<InputRecordField> fields = new ArrayList<>();
                Iterator<JsonNode> elements = fieldNodes.elements();
                while (elements.hasNext()) {
                    JsonNode fileldNode = elements.next();
                    InputRecordField inputRecordField = new InputRecordField();
                    inputRecordField.setName(processStringField("recordField#name", fileldNode.get("name")));
                    inputRecordField.setLabel(processStringField("recordField#label", fileldNode.get(LABEL)));
                    inputRecordField.setDoc(processStringField("recordField#label", fileldNode.get("doc")));
                    inputRecordField.setRecordType(
                            processInputParameterType("recordField#type", fileldNode.get("type")));
                    JsonNode bindingNode = fileldNode.get(INPUT_BINDING);
                    if (bindingNode != null) {
                        inputRecordField.setInputBinding(
                                processCommandLineBinding(inputRecordField.getRecordType(), bindingNode));
                    }
                    fields.add(inputRecordField);
                }
                recordType.setFields(fields);
            }
            schemaRefTypes.put(recordType.getName(), recordType);
            return recordType;
        }
        throw new CWLException(
                ResourceLoader.getMessage("cwl.parser.field.required.in", "SchemaType", "type"),
                251);
    }
}
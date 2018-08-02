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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.spectrumcomputing.cwl.model.Pair;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.conf.StepExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.CWLVersion;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Provides a series of APIs to
 * <ul>
 *  <li>Parses CWL document file</li>
 *  <li>Loads input settings for a CWL process object</li>
 *  <li>Parses a cwlexec execution configuration file</li>
 * </ul>
 */
public final class CWLParser {

    private CWLParser() {
    }

    private static final Logger logger = LoggerFactory.getLogger(CWLParser.class);

    // execution configuration
    private static final String APP = "app";
    private static final String RESOURCE = "res_req";
    private static final String RERUNNABLE = "rerunnable";
    private static final String PROJECT = "project";
    private static final String QUEUE = "queue";
    private static final String POST_FAILURE_SCRIPT = "post-failure-script";

    /**
     * Processes a CWL description file and yields a CWL process object
     * 
     * @param descriptionFile
     *            A CWL description file
     * @return A CWL process object (CommandLineTool or Workflow)
     * @throws CWLException
     *             Failed to process the CWL description file
     */
    public static CWLProcess yieldCWLProcessObject(File descriptionFile) throws CWLException {
        return yieldCWLProcessObject(descriptionFile, null, null);
    }

    /**
     * Processes a CWL description file that has a $graph directive and yields a
     * CWL process object
     * 
     * @param descriptionFile
     *            A CWL description file
     * @param mainProcessId
     *            If a CWL description file has $graph directive, the
     *            mainProcessId indicates the CWL main process
     * @return A CWL process object (CommandLineTool or Workflow)
     * @throws CWLException
     *             Failed to process the CWL description file
     */
    public static CWLProcess yieldCWLProcessObject(File descriptionFile,
            String mainProcessId) throws CWLException {
        return yieldCWLProcessObject(descriptionFile, mainProcessId, null);
    }

    /**
     * Processes a CWL description file that has $graph and $namespace
     * directives and yields a processed object
     * 
     * @param descriptionFile
     *            A CWL description file
     * @param mainProcessId
     *            If a CWL description file has $graph directive, the
     *            mainProcessId indicates the CWL main process
     * @param namespace
     *            The namespace for a CWL Workflow step
     * @return A CWL process object (CommandLineTool or Workflow)
     * @throws CWLException
     *             Failed to process the CWL description file
     */
    public static CWLProcess yieldCWLProcessObject(File descriptionFile,
            String mainProcessId,
            String namespace) throws CWLException {
        if (descriptionFile == null) {
            throw new IllegalArgumentException("The description file is null.");
        }
        CWLProcess processObj = null;
        String descFilePath;
        try {
            descFilePath = descriptionFile.getCanonicalPath();
        } catch (IOException e) {
            descFilePath = descriptionFile.getAbsolutePath();
        }
        String owner = getFileOwner(descriptionFile);
        try {
            JsonNode node = IOUtil.toJsonNode(descriptionFile, false);
            logger.debug("Start to process {}", descFilePath);
            CWLVersion cwlVersion = BaseParser.processCWLVersion(node);
            JsonNode graphNode = node.get("$graph");
            if (graphNode != null) {
                if (mainProcessId == null) {
                    throw new CWLException(ResourceLoader.getMessage("cwl.parser.graph.main.required"), 251);
                }
                Pair<CWLProcess, List<JsonNode>> graph = processGraph(descFilePath, mainProcessId, owner, graphNode,
                        namespace);
                processObj = graph.getKey();
                if (processObj instanceof Workflow) {
                    buildGraphWorkflow(descFilePath, owner, (Workflow) processObj, graph.getValue(), namespace);
                    processObj.setMainId(mainProcessId);
                    processObj.setCwlVersion(cwlVersion);
                }
            } else {
                processObj = processCWLDescription(descFilePath, owner, node, namespace);
                processObj.setCwlVersion(cwlVersion);
                String processId = descriptionFile.getName();
                int index = processId.lastIndexOf('.');
                if (index != -1) {
                    processId = processId.substring(0, index);
                }
                processObj.setId(processId);
            }
            JsonNode namespacesNode = node.get("$namespaces");
            if (namespacesNode != null) {
                processObj.setNamespaces(processNamespaces(namespacesNode));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("After processed {}", CommonUtil.asPrettyJsonStr(processObj));
            }
        } catch (IOException e) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.fail.to.process", descFilePath, e.getMessage()), 251);
        }
        return processObj;
    }

    /**
     * Loads input object from an input settings file for a given CWL process object
     * 
     * @param processObj
     *            A CWL process object
     * @param inputSettingsFile
     *            An input settings file
     * @throws CWLException
     *             Failed to load the CWL input settings file
     */
    public static void loadInputSettings(CWLProcess processObj, File inputSettingsFile) throws CWLException {
        if (processObj == null || inputSettingsFile == null) {
            throw new IllegalArgumentException("The description file or owner file is null.");
        }
        String inputSettingFilePath;
        try {
            inputSettingFilePath = inputSettingsFile.getCanonicalPath();
        } catch (IOException e) {
            inputSettingFilePath = inputSettingsFile.getAbsolutePath();
        }
        try {
            JsonNode settingsNode = IOUtil.toJsonNode(inputSettingsFile);
            if (processObj instanceof CommandLineTool || processObj instanceof Workflow) {
                logger.debug("Apply input settings {}", inputSettingFilePath);
                List<? extends CWLParameter> inputs = processObj.getInputs();
                String parentPath = Paths.get(inputSettingFilePath).getParent().toString();
                InputSettingsParser.processInputSettings(parentPath, settingsNode, inputs);
                processObj.setInputsPath(inputSettingFilePath);
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.parser.field.unsupported", BaseParser.CLASS,
                                processObj.getClazz()),
                        33);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Finish to apply {}", CommonUtil.asPrettyJsonStr(processObj));
            }
        } catch (IOException e) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.fail.to.process.inputsettings", inputSettingFilePath,
                            e.getMessage()),
                    252);
        }
    }

    /**
     * Parses a cwlexec execution configuration file
     * 
     * @param confFile
     *            A cwlexec execution configuration file
     * @return The cwlexec execution configuration object
     * @throws CWLException
     *             Failed to parse the execution configuration file
     */
    public static FlowExecConf parseFlowExecConf(File confFile) throws CWLException {
        if (confFile == null) {
            throw new IllegalArgumentException("The configuration file is null.");
        }
        FlowExecConf flowExecConf = new FlowExecConf();
        try {
            JsonNode configNode = IOUtil.toJsonNode(confFile);
            flowExecConf.setProject(BaseParser.processStringField(PROJECT, configNode.get(PROJECT)));
            flowExecConf.setQueue(BaseParser.processStringField(QUEUE, configNode.get(QUEUE)));
            Boolean rerunable = BaseParser.processBooleanField(RERUNNABLE, configNode.get(RERUNNABLE));
            flowExecConf.setRerunnable(rerunable != null ? rerunable.booleanValue() : false);
            flowExecConf.setApp(BaseParser.processStringField(APP, configNode.get(APP)));
            flowExecConf.setResource(BaseParser.processStringField(RESOURCE, configNode.get(RESOURCE)));
            flowExecConf.setPostFailureScript(
                    BaseParser.processPostFailureScript(POST_FAILURE_SCRIPT, configNode.get(POST_FAILURE_SCRIPT)));
            JsonNode stepsConfigNode = configNode.get("steps");
            if (stepsConfigNode != null && stepsConfigNode.isObject()) {
                Map<String, StepExecConf> steps = new HashMap<>();
                Iterator<Entry<String, JsonNode>> fields = stepsConfigNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    String stepId = field.getKey();
                    JsonNode stepConfigNode = field.getValue();
                    StepExecConf stepExecConf = new StepExecConf();
                    stepExecConf.setApp(BaseParser.processStringField(stepId + "#app", stepConfigNode.get(APP)));
                    stepExecConf.setProject(
                            BaseParser.processStringField(stepId + "#project", stepConfigNode.get(PROJECT)));
                    stepExecConf.setQueue(BaseParser.processStringField(stepId + "#queue", stepConfigNode.get(QUEUE)));
                    Boolean stepRerunnable = BaseParser.processBooleanField(stepId + "#rerunnable",
                            stepConfigNode.get(RERUNNABLE));
                    stepExecConf.setRerunnable(stepRerunnable != null ? stepRerunnable.booleanValue() : false);
                    stepExecConf.setResource(
                            BaseParser.processStringField(stepId + "#resource", stepConfigNode.get(RESOURCE)));
                    stepExecConf.setPostFailureScript(BaseParser.processPostFailureScript(stepId + "#post-failure-script",
                            stepConfigNode.get(POST_FAILURE_SCRIPT)));
                    steps.put(stepId, stepExecConf);
                }
                flowExecConf.setSteps(steps);
            }
        } catch (IOException e) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.fail.to.process.exec.config", confFile.getAbsolutePath(),
                            e.getMessage()),
                    255);
        }
        return flowExecConf;
    }

    /**
     * Transforms a CWLFile JSON object to a CWLFile object
     * 
     * @param descFilePath
     *            The CWL description file path
     * @param id
     *            The ID of the CWLFile object
     * @param fileNode
     *            A CWLFile JSON object
     * @return A CWLFile object
     * @throws CWLException
     *             Failed to transform the CWLFile JSON object
     */
    public static CWLFile transformCWLFileNode(String descFilePath, String id, JsonNode fileNode) throws CWLException {
        CWLFile cwlFile = null;
        if (descFilePath != null && id != null && fileNode != null) {
            cwlFile = BaseParser.processCWLFile(descFilePath, id, fileNode, false);
        }
        return cwlFile;
    }

    /**
     * Transforms a CWLDirectory JSON object to a CWLDirectory object
     * 
     * @param descFilePath
     *            The CWL description file path
     * @param id
     *            The ID of the CWLDirectory object
     * @param dirNode
     *            A CWLDirectory JSON object
     * @return A CWLDirectory object
     * @throws CWLException
     *             Failed to transform the CWLDirectory JSON object
     */
    public static CWLDirectory transformCWLDirectoryNode(String descFilePath,
            String id,
            JsonNode dirNode) throws CWLException {
        CWLDirectory cwlDir = null;
        if (descFilePath != null && id != null && dirNode != null) {
            cwlDir = BaseParser.processCWLDirectory(descFilePath, id, dirNode, false);
        }
        return cwlDir;
    }

    protected static CWLProcess processCWLDescription(String descFilePath,
            String owner,
            JsonNode descNode,
            String namespace) throws CWLException {
        CWLProcess processObj = null;
        JsonNode clazzNode = descNode.get(BaseParser.CLASS);
        String clazz = null;
        if (clazzNode != null) {
            if (clazzNode.isTextual()) {
                clazz = clazzNode.asText();
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(BaseParser.CWL_PARSER_INVALID_TYPE, BaseParser.CLASS, "string"),
                        251);
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(BaseParser.CWL_PARSER_FIELD_REQUIRED, BaseParser.CLASS),
                    251);
        }
        String descTop = Paths.get(descFilePath).getParent().toString();
        if (CWLProcess.CLASS_COMMANDLINETOOL.equals(clazz)) {
            processObj = CommandLineToolParser.yieldCommandLineTool(descTop, descNode);
        } else if (CWLProcess.CLASS_WORKFLOW.equals(clazz)) {
            processObj = WorkflowParser.yieldWorkflow(descTop, descFilePath, owner, descNode, namespace);
        } else if (CWLProcess.CLASS_EXPRESSIONTOOL.equals(clazz)) {
            processObj = ExpressionToolParser.yieldExpressionTool(descTop, descNode);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.field.unsupported", BaseParser.CLASS, clazz),
                    33);
        }
        processObj.setOwner(owner);
        processObj.setDescPath(descFilePath);
        return processObj;
    }

    private static String getFileOwner(File file) {
        String owner = null;
        if (file != null) {
            Path path = Paths.get(file.getAbsolutePath());
            try {
                UserPrincipal user = Files.getFileAttributeView(path, FileOwnerAttributeView.class).getOwner();
                return user.getName();
            } catch (IOException e) {
                logger.warn("Fail to get the owner of {}, ({})", path, e.getMessage());
            }
        }
        return owner;
    }

    private static Pair<CWLProcess, List<JsonNode>> processGraph(String descFilePath,
            String mainProcessId,
            String owner,
            JsonNode graphNode,
            String namespace) throws CWLException {
        CWLProcess main = null;
        List<JsonNode> subNodes = new ArrayList<>();
        if (graphNode.isArray()) {
            Iterator<JsonNode> descNodes = graphNode.elements();
            while (descNodes.hasNext()) {
                JsonNode processNode = descNodes.next();
                String processId = BaseParser.processStringField("id", processNode.get("id"));
                if (mainProcessId.equals(processId)) {
                    main = processCWLDescription(descFilePath, owner, processNode, namespace);
                } else if (processId != null && processId.startsWith("#")
                        && processId.substring(1).equals(mainProcessId)) {
                    main = processCWLDescription(descFilePath, owner, processNode, namespace);
                } else {
                    subNodes.add(processNode);
                }
            }
        } else {
            throw new CWLException(ResourceLoader.getMessage("cwl.parser.graph.array.required"), 251);
        }
        return new Pair<>(main, subNodes);
    }

    private static Map<String, String> processNamespaces(JsonNode node) throws CWLException {
        Map<String, String> namespaces = new HashMap<>();
        if (node.isObject()) {
            Iterator<Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> field = fields.next();
                namespaces.put(field.getKey(), field.getValue().asText());
            }
        } else {
            throw new CWLException(ResourceLoader.getMessage("cwl.parser.namespaces.required"), 251);
        }
        return namespaces;
    }

    private static void buildGraphWorkflow(String descFilePath,
            String owner,
            Workflow main,
            List<JsonNode> subNodes,
            String parentNamespace) throws CWLException {
        List<WorkflowStep> steps = main.getSteps();
        for (WorkflowStep step : steps) {
            String runId = step.getRunId();
            String namespace = parentNamespace == null ? step.getId() : parentNamespace + "/" + step.getId();
            for (JsonNode processNode : subNodes) {
                String processId = BaseParser.processStringField("id", processNode.get("id"));
                if (runId != null && runId.startsWith("#")
                        && (runId.substring(1).equals(processId) || runId.equals(processId))) {
                    CWLProcess processObj = processCWLDescription(descFilePath, owner, processNode, namespace);
                    step.setRun(processObj);
                    if (processObj instanceof Workflow) {
                        buildGraphWorkflow(descFilePath, owner, (Workflow) processObj, subNodes, namespace);
                    }
                    break;
                }
            }
        }
    }
}

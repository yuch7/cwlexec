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
package com.ibm.spectrumcomputing.cwl.exec.util.outputs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.ScatterMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLStepBindingResolver;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.CommandOutputBindingEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.CommandOutputsEvaluator;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.ParameterType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandOutputBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.OutputBindingGlob;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.WorkflowOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputRecordType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.Workflow;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * After CWL instance was done, capture the outputs of a CWL process instance
 */
public class OutputsCapturer {

    private static final Logger logger = LoggerFactory.getLogger(OutputsCapturer.class);

    private OutputsCapturer() {}

    /**
     * Captures the outputs of a CWL CommandLineTool instance
     * 
     * @param instance
     *            A CWL CommandLineTool instance
     * @throws CWLException
     *             Failed to capture the outputs
     */
    public static void captureCommandOutputs(CWLCommandInstance instance) throws CWLException {
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(instance, InlineJavascriptRequirement.class);
        List<CommandInputParameter> inputs = commandLineTool.getInputs();
        List<CommandOutputParameter> outputs = commandLineTool.getOutputs();
        for (CommandOutputParameter output : outputs) {
            CommandOutputBinding outputBinding = output.getOutputBinding();
            CommandOutputBindingEvaluator.evalGlob(jsReq, inputs, outputBinding);
            ParameterType outputParamType = output.getType();
            if (outputParamType.getType() != null) {
                captureCommandOutputsByType(jsReq, instance, output);
            } else if (outputParamType.getTypes() != null) {
                captureCommandOutputsByTypes(jsReq, instance, output);
            }
            CommandOutputsEvaluator.eval(jsReq, instance.getRuntime(), inputs, output);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("capture outputs: {}", CommonUtil.asPrettyJsonStr(outputs));
        }
    }

    /**
     * Captures the outputs of a CWL Workflow instance
     * 
     * @param instance
     *            A CWL Workflow instance
     */
    public static void captureWorkflowOutputs(CWLWorkflowInstance instance) {
        Workflow workflow = (Workflow) instance.getProcess();
        List<WorkflowOutputParameter> outputs = workflow.getOutputs();
        for (WorkflowOutputParameter output : outputs) {
            if (output.getOutputSource() != null) {
                for (String outputSrc : output.getOutputSource()) {
                    int lastSeparatorIndex = outputSrc.lastIndexOf('/');
                    String stepId = outputSrc.substring(0, lastSeparatorIndex);
                    WorkflowStep step = findStep(stepId, workflow.getSteps());
                    if (step != null) {
                        List<? extends CWLParameter> stepOutputParams = step.getRun().getOutputs();
                        String outputId = outputSrc.substring(lastSeparatorIndex + 1);
                        CWLParameter stepOutParam = CommonUtil.findParameter(outputId, stepOutputParams);
                        output.setValue(stepOutParam.getValue());
                    }
                }
            }
        }
    }

    /**
     * Copies the final outputs (file or directory) of the CWL process instance to its outdir
     * 
     * @param instance
     *            A CWL process instance
     * @throws CWLException
     *             Failed to copy the outputs
     */
    public static void copyOutputFiles(CWLInstance instance) throws CWLException {
        CWLProcess process = instance.getProcess();
        String owner = instance.getOwner();
        String outputTopDir = System.getProperty(IOUtil.OUTPUT_TOP_DIR);
        Path outputDir = Paths.get(outputTopDir, String.format("%s-%s", instance.getName(), instance.getId()));
        IOUtil.mkdirs(owner, outputDir);
        List<? extends CWLParameter> outputs = process.getOutputs();
        Map<String, String> namespaces = process.getNamespaces();
        for (CWLParameter output : outputs) {
            if (instance instanceof CWLWorkflowInstance) {
                CWLStepBindingResolver.resolveWorkflowOutput((CWLWorkflowInstance) instance,
                        (WorkflowOutputParameter) output);
            }
            CWLType type = output.getType().getType();
            if (type == null || type.getSymbol() == CWLTypeSymbol.NULL) {
                continue;
            }
            CWLTypeSymbol typeSymbol = type.getSymbol();
            if (typeSymbol == CWLTypeSymbol.FILE) {
                copyFileOutput(owner, namespaces, outputDir, output, false);
            } else if (typeSymbol == CWLTypeSymbol.DIRECTORY) {
                copyDirOutput(owner, outputDir, output, false);
            } else if (typeSymbol == CWLTypeSymbol.ARRAY) {
                copyArrayOutput(owner, namespaces, outputDir, output, false);
            } else if (typeSymbol == CWLTypeSymbol.RECORD) {
                copyRecordOutput(owner, namespaces, outputDir, output, false);
            }
        }
    }

    private static void captureCommandOutputsByType(InlineJavascriptRequirement jsReq,
            CWLCommandInstance instance,
            CommandOutputParameter output) throws CWLException {
        CWLType outputType = output.getType().getType();
        if (outputType.getSymbol() != CWLTypeSymbol.NULL) {
            String owner = instance.getOwner();
            Path tmpOutputDir = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR));
            CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
            CommandOutputBinding outputBinding = output.getOutputBinding();
            List<CommandInputParameter> inputs = commandLineTool.getInputs();
            Object value = null;
            if (instance.getScatter() != null) {
                value = findScatterOuputValue(jsReq, instance, outputType, output);
            } else {
                value = findOutputValue(owner,
                        tmpOutputDir,
                        instance.getHPCJobId(),
                        jsReq,
                        inputs,
                        outputType,
                        outputBinding);
            }
            if (value == null) {
                throw new CWLException(ResourceLoader.getMessage("cwl.output.value.not.found", output.getId()), 250);
            }
            output.setValue(value);
        }
    }

    private static void captureCommandOutputsByTypes(InlineJavascriptRequirement jsReq,
            CWLCommandInstance instance,
            CommandOutputParameter output) throws CWLException {
        String owner = instance.getOwner();
        Path tmpOutputDir = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR));
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        CommandOutputBinding outputBinding = output.getOutputBinding();
        List<CommandInputParameter> inputs = commandLineTool.getInputs();
        ParameterType outputParamType = output.getType();
        List<CWLType> outputTypes = outputParamType.getTypes();
        Object value = null;
        boolean canBeNull = false;
        for (CWLType outputType : outputTypes) {
            if (outputType.getSymbol() == CWLTypeSymbol.NULL) {
                canBeNull = true;
            }
            try {
                if (instance.getScatter() != null) {
                    int arraySize = instance.getScatterCommands().size();
                    boolean emptyScatter = instance.isEmptyScatter();
                    value = findScatterOutputValue(jsReq, instance, outputType, output, arraySize, 0, emptyScatter);
                } else {
                    value = findOutputValue(owner, tmpOutputDir, instance.getHPCJobId(), jsReq, inputs, outputType,
                            outputBinding);
                }
            } catch (CWLException e) {
                // ignore, output is not mandatory validation
                if (logger.isDebugEnabled()) {
                    logger.debug("Output is empty and it is not mandatory for type is {}", outputType.getSymbol());
                }
            }
            if (value != null) {
                outputParamType.setType(outputType);
                output.setValue(value);
                break;
            }
        }
        if (value == null && !canBeNull) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.output.value.not.found", instance.getId()), 250);
        }
    }

    private static Object findScatterOuputValue(InlineJavascriptRequirement jsReq,
            CWLCommandInstance instance,
            CWLType outputType,
            CommandOutputParameter output) throws CWLException {
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        List<CommandInputParameter> inputs = commandLineTool.getInputs();
        int scatterSize = instance.getScatterCommands().size();
        int groupSize = 0;
        if (instance.getStep().getScatterMethod() == ScatterMethod.NESTED_CROSSPRODUCT) {
            String firstId = instance.getScatter().get(0);
            for (CommandInputParameter in : inputs) {
                if (in.getId().equals(firstId)) {
                    int firstSize = ((List<?>) in.getValue()).size();
                    if (firstSize > 0) {
                        groupSize = scatterSize / firstSize;
                    }
                    break;
                }
            }
        }
        boolean emptyScatter = instance.isEmptyScatter();
        return findScatterOutputValue(jsReq, instance, outputType, output, scatterSize, groupSize, emptyScatter);
    }

    private static Object findOutputValue(String owner,
            Path globDir,
            long jobId,
            InlineJavascriptRequirement jsReq,
            List<CommandInputParameter> inputs,
            CWLType type,
            CommandOutputBinding outputBinding) throws CWLException {
        List<CWLFileBase> globFiles = globFiles(jobId, globDir, outputBinding);
        Object value = evalOutputEval(jsReq, inputs, globFiles, type, outputBinding);
        if (value == null) {
            List<CWLFile> files = new ArrayList<>();
            List<CWLDirectory> dirs = new ArrayList<>();
            filterFilesAndDirs(globFiles, files, dirs);
            CWLTypeSymbol typeSymbol = type.getSymbol();
            if (typeSymbol == CWLTypeSymbol.FILE) {
                value = findCWLFile(globDir, toGlobPatterns(outputBinding), files);
            } else if (typeSymbol == CWLTypeSymbol.DIRECTORY) {
                value = findCWLDir(globDir, toGlobPatterns(outputBinding), dirs);
            } else if (typeSymbol == CWLTypeSymbol.ARRAY) {
                CWLTypeSymbol items = ((OutputArrayType) type).getItems().getType().getSymbol();
                value = findArrayValue(items, files, dirs);
            } else if (typeSymbol == CWLTypeSymbol.RECORD) {
                OutputRecordType outputRecordType = (OutputRecordType) type;
                List<OutputRecordField> records = outputRecordType.getFields();
                List<OutputRecordField> recordValues = new ArrayList<>();
                for (OutputRecordField record : records) {
                    if (record.getOutputBinding() != null) {
                        OutputRecordField recordField = new OutputRecordField();
                        recordField.setName(record.getName());
                        recordField.setRecordType(record.getRecordType());
                        recordField.setValue(findOutputValue(owner,
                                globDir,
                                jobId,
                                jsReq,
                                inputs,
                                record.getRecordType().getType(),
                                record.getOutputBinding()));
                        recordValues.add(recordField);
                    }
                }
                value = recordValues;
            }
        }
        return value;
    }

    private static List<CWLFileBase> globFiles(long jobId, Path globDir, CommandOutputBinding outputBinding)
            throws CWLException {
        List<CWLFileBase> globFiles = new ArrayList<>();
        if (outputBinding != null) {
            OutputBindingGlob glob = outputBinding.getGlob();
            globFiles = globOutputFiles(jobId, glob, globDir, outputBinding.isLoadContents());
        }
        return globFiles;
    }

    private static Object evalOutputEval(InlineJavascriptRequirement jsReq,
            List<CommandInputParameter> inputs,
            List<CWLFileBase> globFiles,
            CWLType outputType,
            CommandOutputBinding outputBinding) throws CWLException {
        Object value = null;
        if (outputBinding != null) {
            CWLFieldValue outputEval = outputBinding.getOutputEval();
            if (outputEval != null) {
                value = CommandOutputBindingEvaluator.evalOutputEval(jsReq,
                        inputs,
                        globFiles,
                        outputType,
                        outputEval.getExpression());
            }
        }
        return value;
    }

    private static CWLFile findCWLFile(Path globDir, List<String> globPatterns, List<CWLFile> files)
            throws CWLException {
        CWLFile cwlFile = null;
        if (files.size() == 1) {
            cwlFile = files.get(0);
        } else if (files.size() > 1) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.output.value.match.failed", "files", globPatterns, globDir),
                    253);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.output.value.not.match", "file", globPatterns, globDir),
                    253);
        }
        return cwlFile;
    }

    private static CWLDirectory findCWLDir(Path globDir, List<String> globPatterns, List<CWLDirectory> dirs)
            throws CWLException {
        CWLDirectory cwlDir = null;
        if (dirs.size() == 1) {
            cwlDir = dirs.get(0);
        } else if (dirs.size() > 1) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.output.value.match.failed", "directories", globPatterns, globDir),
                    253);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.output.value.not.match", "directory", globPatterns, globDir),
                    253);
        }
        return cwlDir;
    }

    private static Object findArrayValue(CWLTypeSymbol items, List<CWLFile> files, List<CWLDirectory> dirs) {
        if (items == CWLTypeSymbol.FILE) {
            return files;
        } else if (items == CWLTypeSymbol.DIRECTORY) {
            return dirs;
        } else {
            return new ArrayList<>();
        }
    }

    private static Object findScatterOutputValue(InlineJavascriptRequirement jsReq,
            CWLCommandInstance instance,
            CWLType outputType,
            CommandOutputParameter output,
            int scatterSize,
            int groupSize,
            boolean emptyScatter) throws CWLException {
        String owner = instance.getOwner();
        Path globDir = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR));
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        CommandOutputBinding outputBinding = output.getOutputBinding();
        List<CommandInputParameter> inputs = commandLineTool.getInputs();
        List<Object> valueList = new ArrayList<>();
        List<Object> groupList = new ArrayList<>();
        int length = 0;
        if (emptyScatter) {
            addEmptyScatter(valueList, scatterSize, groupSize);
        } else {
            for (int i = 1; i <= scatterSize; i++) {
                CommandOutputBinding scatterOutputBinding = new CommandOutputBinding();
                OutputBindingGlob glob = new OutputBindingGlob();
                glob.setPatterns(outputBinding.getGlob().getPatterns());
                glob.setGlobExpr(new CWLFieldValue());
                glob.getGlobExpr().setExpression(outputBinding.getGlob().getGlobExpr().getExpression());
                glob.getGlobExpr().setValue(outputBinding.getGlob().getGlobExpr().getValue());
                glob.setScatterIndex(i);
                scatterOutputBinding.setGlob(glob);
                scatterOutputBinding.setOutputEval(outputBinding.getOutputEval());
                scatterOutputBinding.setLoadContents(outputBinding.isLoadContents());
                Object value = findOutputValue(owner,
                        globDir,
                        instance.getHPCJobId(),
                        jsReq,
                        inputs,
                        outputType,
                        scatterOutputBinding);
                if (groupSize > 0) {
                    groupList.add(value);
                    length++;
                    if (length % groupSize == 0) {
                        valueList.add(groupList);
                        groupList = new ArrayList<>();
                    }
                } else {
                    valueList.add(value);
                }
            }
        }
        writeScatterValues(globDir, outputBinding, valueList);
        return valueList;
    }

    private static void addEmptyScatter(List<Object> valueList, int scatterSize, int groupSize) {
        if (groupSize > 0) {
            int numGroups = scatterSize / groupSize;
            for (int i = 0; i < numGroups; i++) {
                valueList.add(new ArrayList<>());
            }
        }
    }

    private static void writeScatterValues(Path globDir, CommandOutputBinding outputBinding, List<Object> valueList) {
        Path fpath = globDir.resolve(outputBinding.getGlob().getGlobExpr().getValue());
        try (BufferedWriter bfw = Files.newBufferedWriter(Files.createFile(fpath))) {
            for (Object value: valueList) {
                if (value != null) {
                    bfw.write(value.toString());
                }
                bfw.newLine();
            }
            bfw.flush();
        } catch (IOException e) {
            logger.error("Fail to write scatter values\n", e);
        }
    }

    private static void copyFileOutput(String owner,
            Map<String, String> namespaces,
            Path outputDir,
            CWLParameter output,
            boolean nochecksum) throws CWLException {
        CWLFile tmpFile = (CWLFile) output.getValue();
        String path = tmpFile.getPath();
        if (path != null && path.startsWith(IOUtil.FILE_PREFIX)) {
            path = path.substring(7);
        }
        Path src = Paths.get(path);
        Path target = Paths.get(outputDir.toString(), tmpFile.getBasename());
        IOUtil.copy(owner, src, target);
        CWLFile outputFile = toCWLFile(namespaces, output, target, nochecksum);
        copySecondaryFiles(owner, tmpFile.getSecondaryFiles(), outputFile, nochecksum);
        output.setValue(outputFile);
    }

    private static void copyDirOutput(String owner,
            Path outputDir,
            CWLParameter output,
            boolean nochecksum) throws CWLException {
        CWLDirectory tmpDir = (CWLDirectory) output.getValue();
        String path = tmpDir.getPath();
        if (path != null && path.startsWith(IOUtil.FILE_PREFIX)) {
            path = path.substring(7);
        }
        Path src = Paths.get(path);
        Path target = Paths.get(outputDir.toString(), tmpDir.getBasename());
        IOUtil.copy(owner, src, target);
        CWLDirectory targetDir = IOUtil.toCWLDirectory(target);
        IOUtil.traverseDirListing(target.toString(), targetDir.getListing(), nochecksum);
        output.setValue(targetDir);
    }

    private static void copyArrayOutput(String owner,
            Map<String, String> namespaces,
            Path outputDir,
            CWLParameter output,
            boolean nochecksum) throws CWLException {
        CWLTypeSymbol items = ((OutputArrayType) output.getType().getType()).getItems().getType().getSymbol();
        if (items == CWLTypeSymbol.FILE) {
            @SuppressWarnings("unchecked")
            List<CWLFile> files = (List<CWLFile>) output.getValue();
            List<CWLFile> outputFiles = new ArrayList<>();
            for (CWLFile file : files) {
                Path src = Paths.get(file.getPath());
                Path desc = Paths.get(outputDir.toString(), file.getBasename());
                IOUtil.copy(owner, src, desc);
                CWLFile outputFile = toCWLFile(namespaces, output, desc, nochecksum);
                outputFiles.add(outputFile);
            }
            output.setValue(outputFiles);
        }
    }

    private static void copyRecordOutput(String owner,
            Map<String, String> namespaces,
            Path outputDir,
            CWLParameter output,
            boolean nochecksum) throws CWLException {
        Object outputRecords = output.getValue();
        if (outputRecords instanceof List) {
            Map<String, CWLFile> recordMaps = new HashMap<>();
            @SuppressWarnings("unchecked")
            List<OutputRecordField> records = (List<OutputRecordField>) outputRecords;
            for (OutputRecordField record : records) {
                if (record.getValue() instanceof CWLFile) {
                    CWLFile tmpFile = (CWLFile) record.getValue();
                    Path src = Paths.get(tmpFile.getPath());
                    Path desc = Paths.get(outputDir.toString(), tmpFile.getBasename());
                    IOUtil.copy(owner, src, desc);
                    CWLFile outputFile = toCWLFile(namespaces, output, desc, nochecksum);
                    recordMaps.put(record.getName(), outputFile);
                }
            }
            output.setValue(recordMaps);
        }
    }

    private static CWLFile toCWLFile(Map<String, String> namespaces, CWLParameter output, Path target, boolean nochecksum) {
        CWLFile outputFile = IOUtil.toCWLFile(target, nochecksum);
        if (output.getFormat() == null || output.getFormat().getFormat() == null) {
            return outputFile;
        }
        String outputFormat = null;
        if (output.getFormat().getFormat().getExpression() != null) {
            outputFormat = output.getFormat().getFormat().getExpression();
        } else if (output.getFormat().getFormat().getValue() != null) {
            outputFormat = output.getFormat().getFormat().getValue();
        }
        if (outputFormat != null && outputFormat.indexOf(':') != -1) {
            String prxfix = outputFormat.substring(0, outputFormat.indexOf(':'));
            String value = outputFormat.substring(outputFormat.indexOf(':') + 1);
            String processFormat = namespaces.entrySet().stream()
                    .filter(map -> map.getKey().equals(prxfix))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.joining());
            String format = processFormat.concat(value);
            outputFile.setFormat(format);
        }
        return outputFile;
    }

    private static void copySecondaryFiles(String owner,
            List<CWLFileBase> secondaryFiles,
            CWLFile outputFile,
            boolean nochecksum) throws CWLException {
        if (secondaryFiles != null && !secondaryFiles.isEmpty()) {
            List<CWLFileBase> outputSecondaryFiles = new ArrayList<>();
            for (CWLFileBase sf : secondaryFiles) {
                Path target = Paths.get(outputFile.getPath()).getParent().resolve(sf.getBasename());
                Path src = Paths.get(sf.getPath());
                if (!src.toFile().exists()) {
                    continue;
                }
                IOUtil.copy(owner, src, target);
                if (target.toFile().isDirectory()) {
                    CWLDirectory cwlDir = IOUtil.toCWLDirectory(target);
                    IOUtil.traverseDirListing(target.toString(), cwlDir.getListing(), nochecksum);
                    outputSecondaryFiles.add(cwlDir);
                } else {
                    outputSecondaryFiles.add(IOUtil.toCWLFile(target, nochecksum));
                }
            }
            outputFile.setSecondaryFiles(outputSecondaryFiles);
        }
    }

    private static List<CWLFileBase> globOutputFiles(long jobId,
            OutputBindingGlob glob,
            Path globDir,
            boolean loadContents) throws CWLException {
        List<CWLFileBase> cwlFiles = new ArrayList<>();
        if (glob != null) {
            List<String> patterns = glob.getPatterns();
            CWLFieldValue globExpr = glob.getGlobExpr();
            if (patterns != null) {
                for (String pattern : patterns) {
                    cwlFiles.addAll(matchFiles(jobId, pattern, globDir, loadContents));
                }
            } else if (globExpr != null) {
                String pattern = globExpr.getValue();
                if (pattern != null) {
                    List<CWLFileBase> matchedFiles = matchFiles(jobId, pattern, globDir, loadContents);
                    if (glob.getScatterIndex() != -1 && matchedFiles.isEmpty()) {
                        //draft-3, the scatter output may not be from an array
                        matchedFiles = matchFiles(jobId, pattern + "_" + glob.getScatterIndex(), globDir, loadContents);
                    }
                    cwlFiles.addAll(matchedFiles);
                }
            }
        }
        return cwlFiles;
    }

    private static List<CWLFileBase> matchFiles(long jobId, String pattern, Path globDir, boolean loadContents)
            throws CWLException {
        logger.debug("glob \"{}\" in \"{}\"", pattern, globDir);
        List<CWLFileBase> cwlFiles = new ArrayList<>();
        List<Path> matchedPaths = IOUtil.glob(pattern, globDir);
        logger.debug("matched paths: {}", matchedPaths);
        for (Path matchedPath : matchedPaths) {
            String matchedPathFileName = matchedPath.getFileName().toString();
            if ("*".equals(pattern) &&
                    (String.format("%d_out", jobId).equals(matchedPathFileName) ||
                            String.format("%d_err", jobId).equals(matchedPathFileName))) {
                continue;
            }
            File primary = matchedPath.toFile();
            if (primary.isDirectory()) {
                CWLDirectory cwlDirectory = IOUtil.toCWLDirectory(matchedPath);
                IOUtil.traverseDirListing(matchedPath.toString(), cwlDirectory.getListing(), true);
                cwlFiles.add(cwlDirectory);
            } else {
                CWLFile cwlFile = IOUtil.toCWLFile(matchedPath, true);
                if (loadContents) {
                    cwlFile.setContents(IOUtil.read64KiB(matchedPath.toFile()));
                }
                cwlFiles.add(cwlFile);
            }
        }
        cwlFiles.sort((f1, f2) -> f1.getBasename().compareTo(f2.getBasename()));
        return cwlFiles;
    }

    private static void filterFilesAndDirs(List<CWLFileBase> globFiles, List<CWLFile> files, List<CWLDirectory> dirs) {
        for (CWLFileBase file : globFiles) {
            if (file instanceof CWLFile) {
                files.add((CWLFile) file);
            } else if (file instanceof CWLDirectory) {
                dirs.add((CWLDirectory) file);
            }
        }
    }

    private static List<String> toGlobPatterns(CommandOutputBinding outputBinding) {
        List<String> patterns = new ArrayList<>();
        if (outputBinding != null) {
            OutputBindingGlob glob = outputBinding.getGlob();
            if (glob != null) {
                if (glob.getPatterns() != null) {
                    patterns.addAll(glob.getPatterns());
                }
                if (glob.getGlobExpr().getValue() != null) {
                    patterns.add(glob.getGlobExpr().getValue());
                }
            }
        }
        return patterns;
    }

    private static WorkflowStep findStep(String stepId, List<WorkflowStep> steps) {
        WorkflowStep step = null;
        for (WorkflowStep s : steps) {
            if (stepId.equals(s.getId())) {
                step = s;
                break;
            }
        }
        return step;
    }
}

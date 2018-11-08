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
package com.ibm.spectrumcomputing.cwl.exec.util.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.CommandLineBindingEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.CommandStdIOEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.InputsEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.RequirementsEvaluator;
import com.ibm.spectrumcomputing.cwl.exec.util.evaluator.StepInValueFromEvaluator;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLScatterHolder;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandLineBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.method.ScatterMethod;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.DirectoryType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.FileType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullValue;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.StringType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Dirent;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InitialWorkDirRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ShellCommandRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.model.process.tool.ExpressionTool;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Utility for building a CWL UNIX local execution commands for a CWL process instance
 */
public final class CommandUtil {

    private static final Logger logger = LoggerFactory.getLogger(CommandUtil.class);

    private CommandUtil() {
    }

    public static final String PRESERVE_ENTIRE_ENV = "preserve.entire.env";
    public static final String PRESERVE_ENV = "preserve.env";

    /**
     * Builds a CWL UNIX local execution command for a given CWL process instance
     * 
     * @param instance
     *            A CWL process instance
     * @return An UNIX execution command
     * @throws CWLException
     *             Fail to build the command
     */
    public static List<String> buildCommand(CWLCommandInstance instance) throws CWLException {
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        List<CommandInputParameter> inputs = commandLineTool.getInputs();
        return buildCommand(instance, inputs, 0);
    }

    /**
     * Builds a CWL UNIX local execution command for a given CWL scatter process
     * instance
     * 
     * @param instance
     *            A CWL scatter process instance
     * @throws CWLException
     *             Fail to build the command
     */
    public static void buildScatterCommand(CWLCommandInstance instance) throws CWLException {
        List<List<CommandInputParameter>> scatterInputs = scatterInputs(instance);
        if (scatterInputs.isEmpty()) {
            instance.setEmptyScatter(true);
            scatterInputs.add(new ArrayList<>());
        }
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        List<CommandInputParameter> originalInputs = commandLineTool.getInputs();
        for (int i = 0; i < scatterInputs.size(); i++) {
            List<CommandInputParameter> totalInputs = new ArrayList<>();
            for (CommandInputParameter in : originalInputs) {
                boolean found = false;
                for (String scatterId : instance.getScatter()) {
                    if (in.getId().equals(scatterId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    totalInputs.add(in);
                }
            }
            totalInputs.addAll(scatterInputs.get(i));
            CWLScatterHolder scatterHolder = new CWLScatterHolder();
            InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(instance, InlineJavascriptRequirement.class);
            Map<String, String> runtime = instance.getRuntime();
            InputsEvaluator.eval(jsReq, runtime, totalInputs);
            // refer to issue #36 and #37
            if (!needToPutOff(totalInputs)) {
                CommandStdIOEvaluator.eval(jsReq, runtime, totalInputs, commandLineTool.getStdin());
                CommandStdIOEvaluator.eval(jsReq, runtime, totalInputs, commandLineTool.getStderr());
                CommandStdIOEvaluator.eval(jsReq, runtime, totalInputs, commandLineTool.getStdout());
            }
            scatterHolder.setScatterIndex(i + 1);
            scatterHolder.setInputs(totalInputs);
            scatterHolder.setCommand(buildCommand(instance, totalInputs, scatterHolder.getScatterIndex()));
            instance.getScatterHolders().add(scatterHolder);
        }
    }

    private static boolean needToPutOff(List<CommandInputParameter> totalInputs) {
        for (CommandInputParameter input : totalInputs) {
            if (input.getDelayedValueFromExpr() != null) {
                return true;
            }
        }
        return false;
    }

    private static List<CommandInputParameter> copyInputs(List<CommandInputParameter> inputs, Object value) {
        List<CommandInputParameter> copied = new ArrayList<>();
        for (CommandInputParameter input : inputs) {
            if (input.getDelayedValueFromExpr() != null) {
                CommandInputParameter copiedInput = new CommandInputParameter(input.getId());
                copiedInput.setValue(value);
                copied.add(copiedInput);
            } else {
                copied.add(input);
            }
        }
        return copied;
    }

    private static List<String> buildCommand(CWLCommandInstance instance,
            List<CommandInputParameter> inputs,
            int scatterIndex) throws CWLException {
        List<String> commands = new ArrayList<>();
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        List<String> baseCommand = commandLineTool.getBaseCommand();
        if (baseCommand != null && !baseCommand.isEmpty()) {
            commands.addAll(baseCommand);
        }
        // Evaluates the InitialWorkDirRequirement and prepare input files
        RequirementsEvaluator.evalInitialWorkDirReq(instance);
        // Prepare input files
        prepareInputsFiles(instance, inputs);
        // Sort the arguments and input parameters
        List<CommandArgWrapper> sorted = sortCommandArguments(commandLineTool.getArguments(), inputs);
        // Add all arguments to commands
        if(!(instance.getProcess() instanceof ExpressionTool)) {
            commands.addAll(attachCommandBindings(instance, inputs, sorted));
        } else {
            attachCommandBindings(instance, inputs, sorted);
        }
        // build command stdin
        String stdinPath = buildStdin(instance);
        if (stdinPath != null) {
            commands = Arrays.asList(String.format("%s < %s", String.join(" ", commands), stdinPath));
        }
        ShellCommandRequirement shellReq = CWLExecUtil.findRequirement(instance, ShellCommandRequirement.class);
        if (shellReq != null) {
            commands = Arrays.asList("/bin/sh", "-c", quoteCommand(commands));
            logger.debug("Has ShellCommandRequirement, build commands as:\n{}", commands);
        } else {
            commands = quoteShellCommand(commands);
            logger.debug("Has Shell Command, build commands as:\n{}", commands);
        }
        DockerRequirement dockerReq = CWLExecUtil.findRequirement(instance, DockerRequirement.class);
        if (dockerReq != null) {
            commands = DockerCommandBuilder.buildDockerRun(dockerReq, instance, commands);
            logger.debug("Has DockerRequirement, build commands as:\n{}", commands);
        }
        String stdout = buildCommandOut(instance, scatterIndex);
        if (stdout != null && !commands.isEmpty()) {
            int last = commands.size() - 1;
            commands.set(last, String.format("%s %s", commands.get(last), stdout));
            logger.debug("Has stdout/stderr, build commands as:\n{}", commands);
        }
        return commands;
    }

    private static List<String> quoteShellCommand(List<String> commands) {
        List<String> quotedCommands = new ArrayList<>();
        if (commands.size() > 2 &&
                (commands.get(0).equals("/bin/sh") ||
                        commands.get(0).equals("/bin/bash") ||
                        commands.get(0).equals("sh")) &&
                 commands.get(1).equals("-c")) {
            List<String> shellCommands = new ArrayList<>();
            for (int i = 2; i < commands.size(); i++) {
                shellCommands.add(commands.get(i));
            }
            quotedCommands.add(commands.get(0));
            quotedCommands.add(commands.get(1));
            quotedCommands.add(quoteCommand(shellCommands));
        } else {
            quotedCommands.addAll(commands);
        }
        return quotedCommands;
    }

    /*
     * Builds stdout and stderr for a specified CWL instance, a stdout (>stdout)
     * or stdout (2>stderr) or both (>stdout 2>stderr) will be return
     */
    private static String buildCommandOut(CWLCommandInstance instance, int scatterIndex) throws CWLException {
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        String outputRedirection = buildStdout(instance, commandLineTool, scatterIndex);
        CWLFieldValue stderrExpr = commandLineTool.getStderr();
        if (stderrExpr != null) {
            String stderr = stderrExpr.getValue();
            if (stderr != null) {
                Path stderrPath = Paths.get(stderr);
                Path stderrParentPath = stderrPath.getParent();
                Path tmperrDir = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR));
                if (stderrParentPath != null) {
                    tmperrDir = Paths.get(tmperrDir.toString(), stderrParentPath.toString());
                }
                IOUtil.mkdirs(instance.getOwner(), tmperrDir);
                String tmpStderr = Paths.get(tmperrDir.toString(), stderrPath.getFileName().toString()).toString();
                logger.debug("mapping stderr ({}) to ({})", stderr, tmpStderr);
                if (outputRedirection != null) {
                    return String.format("%s 2>%s", outputRedirection, tmpStderr);
                } else {
                    return String.format("2>%s", tmpStderr);
                }
            }
        }
        return outputRedirection;
    }

    private static String quoteCommand(List<String> commands) {
        String quoted = null;
        if (commands != null) {
            quoted = String.format("'%s'", String.join(" ", commands).replaceAll("'", "'\"'\"'"));
        }
        return quoted;
    }

    /*
     * In HPC environment, the input files should be shared with cluster, so
     * copy or link them to working directory (runtime tmpdir)
     */
    private static void prepareInputsFiles(CWLCommandInstance instance,
            List<CommandInputParameter> inputs) throws CWLException {
        Map<String, String> runtime = instance.getRuntime();
        String owner = instance.getOwner();
        InitialWorkDirRequirement initialWorkDirReq = CWLExecUtil.findRequirement(instance,
                InitialWorkDirRequirement.class);
        for (CommandInputParameter input : inputs) {
            Object inputValue = input.getValue();
            if (inputValue == null) {
                inputValue = input.getDefaultValue();
            }
            CWLType inputType = input.getType().getType();
            if (inputType == null) {
                inputType = input.getType().getTypes().get(0);
                logger.debug("Input ({}) has multi-types ({}), gets the fist ({})",
                        input.getType().getTypes(), inputType);
            }
            logger.debug("Prepare input (id={}, type={}, value={}) for {}",
                    input.getId(), inputType, inputValue, instance.getName());
            prepareInputFile(initialWorkDirReq, runtime, owner, input, inputType, inputValue);
        }
    }

    private static void prepareInputFile(InitialWorkDirRequirement initialWorkDirReq,
            Map<String, String> runtime,
            String owner,
            CommandInputParameter input,
            CWLType inputType,
            Object inputValue) throws CWLException {
        boolean needToCopy = true;
        if (initialWorkDirReq != null && inputValue instanceof CWLFileBase) {
            // skip copy if file has been copied by InitialWorkDirRequirement
            needToCopy = hasCopiedByInitialWorkDirReqListing(runtime,
                    initialWorkDirReq,
                    input,
                    (CWLFileBase) inputValue);
            if (needToCopy) {
                needToCopy = hasCopiedByInitialWorkDirReqDirent(runtime,
                        initialWorkDirReq,
                        input,
                        (CWLFileBase) inputValue);
            }
        }
        if (needToCopy) {
            //If the inputValue is an intermediate output, the value does not need to copy
            needToCopy = isIntermediateOutput(owner, inputValue);
        }
        if (needToCopy) {
            copyInputFiles(owner,
                    Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR)),
                    inputType,
                    inputValue);
        }
    }

    private static boolean hasCopiedByInitialWorkDirReqListing(Map<String, String> runtime,
            InitialWorkDirRequirement initialWorkDirReq,
            CommandInputParameter input,
            CWLFileBase inputValue) {
        boolean needToCopy = true;
        CWLFieldValue listing = initialWorkDirReq.getListing();
        if (listing != null) {
            String listingExpr = listing.getExpression();
            if (listingExpr != null && listingExpr.contains("inputs." + input.getId())) {
                inputValue.setSrcPath(inputValue.getPath());
                Path targetPath = Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR),
                        Paths.get(inputValue.getPath()).getFileName().toString());
                inputValue.setPath(targetPath.toString());
                needToCopy = false;
                logger.debug("Input file \"{}\" is already copied by InitialWorkDirReqListing",
                        inputValue.getSrcPath());
            }
        }
        List<CWLFieldValue> listings = initialWorkDirReq.getExprListing();
        if (listings != null) {
            for (CWLFieldValue expr : listings) {
                String listingExpr = expr.getExpression();
                if (listingExpr != null && listingExpr.contains("inputs." + input.getId())) {
                    inputValue.setSrcPath(inputValue.getPath());
                    Path targetPath = Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR),
                            Paths.get(inputValue.getPath()).getFileName().toString());
                    inputValue.setPath(targetPath.toString());
                    needToCopy = false;
                    logger.debug("Input file \"{}\" is already copied by InitialWorkDirReqListing",
                            inputValue.getSrcPath());
                }
            }
        }
        return needToCopy;
    }

    private static boolean hasCopiedByInitialWorkDirReqDirent(Map<String, String> runtime,
            InitialWorkDirRequirement initialWorkDirReq,
            CommandInputParameter input,
            CWLFileBase inputValue) {
        boolean needToCopy = true;
        List<Dirent> dirents = initialWorkDirReq.getDirentListing();
        if (dirents == null) {
            dirents = new ArrayList<>();
        }
        for (Dirent dirent : dirents) {
            String expr = dirent.getEntry().getExpression();
            if (expr == null) {
                continue;
            }
            if (expr.replace(" ", "").equals("$(inputs." + input.getId() + ")")) {
                inputValue.setSrcPath(inputValue.getPath());
                Path tmpDirPath = Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR));
                Path path = null;
                // only name is processed, and the expression of entry name is not handled
                if (dirent.getEntryname() == null || dirent.getEntryname().getValue() == null) {
                    Path fileName = Paths.get(inputValue.getPath()).getFileName();
                    path = tmpDirPath.resolve(fileName.toString());
                } else {
                    String entryName = dirent.getEntryname().getValue();
                    path = tmpDirPath.resolve(entryName);
                    inputValue.setBasename(entryName);
                }
                inputValue.setPath(path.toString());
                needToCopy = false;
                logger.debug("Input file \"{}\" is already copied by InitialWorkDirReqDirent", inputValue.getSrcPath());
            }
        }
        return needToCopy;
    }

    private static boolean isIntermediateOutput(String owner, Object inputValue) throws CWLException {
        boolean needToCopy = true;
        if (inputValue instanceof CWLFileBase) {
            String topWorkdir = System.getProperty(IOUtil.WORK_TOP_DIR);
            String inputPath = ((CWLFileBase) inputValue).getPath();
            if (topWorkdir != null && inputPath != null) {
                needToCopy = !inputPath.startsWith(topWorkdir);
                if (!needToCopy) {
                    logger.debug("Input file \"{}\" is already in working dir.", inputPath);
                    //The secondary files may be not copied
                    copySecondrayFiles(owner, (CWLFileBase) inputValue);
                }
            }
        }
        return needToCopy;
    }

    private static void copySecondrayFiles(String owner, CWLFileBase cwlFile) throws CWLException {
        if (cwlFile instanceof CWLFile) {
            List<CWLFileBase> secondaryFiles = ((CWLFile) cwlFile).getSecondaryFiles();
            if (secondaryFiles != null) {
                for (CWLFileBase sf : secondaryFiles) {
                    Path srcPath = Paths.get(sf.getPath());
                    Path targetPath = Paths.get(cwlFile.getPath()).getParent().resolve(srcPath.getFileName());
                    if (!targetPath.toFile().exists()) {
                        logger.debug("Copy secondary files: \"{}\" from \"{}\"", srcPath, targetPath);
                        IOUtil.copy(owner, srcPath, targetPath);
                        sf.setSrcPath(srcPath.toString());
                        sf.setPath(targetPath.toString());
                    }
                }
            }
        }
    }

    private static void copyInputFiles(String owner,
            Path tmpInputTopPath,
            CWLType inputType,
            Object inputValue) throws CWLException {
        CWLTypeSymbol typeSymbol = inputType.getSymbol();
        if (typeSymbol == CWLTypeSymbol.FILE || typeSymbol == CWLTypeSymbol.DIRECTORY) {
            copyInputFile(owner, tmpInputTopPath, inputType, inputValue);
        } else if (typeSymbol == CWLTypeSymbol.ARRAY) {
            CWLType itemType = ((InputArrayType) inputType).getItems().getType();
            if (inputValue instanceof List<?>) {
                List<?> itemValues = (List<?>) inputValue;
                for (Object itemValue : itemValues) {
                    copyInputFiles(owner, tmpInputTopPath, itemType, itemValue);
                }
            }
        } else if (typeSymbol == CWLTypeSymbol.RECORD && (inputValue instanceof List)) {
            @SuppressWarnings("unchecked")
            List<InputRecordField> fields = (List<InputRecordField>) inputValue;
            for (InputRecordField field : fields) {
                Object fieldValue = field.getValue();
                if (fieldValue instanceof CWLFile) {
                    copyInputFile(owner, tmpInputTopPath, (CWLFile) fieldValue);
                }
            }
        }
    }

    private static void copyInputFile(String owner,
            Path tmpInputTopPath,
            CWLType inputType,
            Object inputValue) throws CWLException {
        if (inputValue instanceof CWLFileBase) {
            copyInputFile(owner, tmpInputTopPath, (CWLFileBase) inputValue);
        } else if (inputValue instanceof List<?>) {
            List<?> itemValues = (List<?>) inputValue;
            for (Object itemValue : itemValues) {
                copyInputFiles(owner, tmpInputTopPath, inputType, itemValue);
            }
        }
    }

    private static void copyInputFile(String owner, Path tmpDirPath, CWLFileBase file) throws CWLException {
        if (file instanceof CWLFile) {
            List<CWLFileBase> secondaryFiles = ((CWLFile) file).getSecondaryFiles();
            if (secondaryFiles != null) {
                for (CWLFileBase sf : secondaryFiles) {
                    logger.debug("secondary files: \"{}\" from \"{}\"", sf.getPath(), file.getPath());
                    copyInputFile(owner, tmpDirPath, sf);
                }
            }
        }
        Path inputPath = Paths.get(file.getPath());
        Path tmpFilePath = tmpDirPath.resolve(inputPath.getFileName());
        //See conformance test - dir4.cwl, the file path may not exist, because the file basename is changed
        if (!inputPath.toFile().exists()) {
            inputPath = Paths.get(file.getLocation());
        }
        if (System.getProperty(IOUtil.USING_SYMBOL_LINK) != null) {
            try {
                logger.debug("linking input \"{}\" to \"{}\", owner={}", inputPath, tmpFilePath, owner);
                Files.createSymbolicLink(tmpFilePath, inputPath);
                logger.debug("linked input \"{}\" to \"{}\", owner={}", inputPath, tmpFilePath, owner);
            } catch (IOException e) {
                logger.error("Failed to create symbol link from \"{}\" to \"{}\": {}",
                        inputPath, tmpFilePath, e.getMessage());
            }
        } else {
            logger.debug("coping input \"{}\" to \"{}\", owner={}", inputPath, tmpFilePath, owner);
            IOUtil.copy(owner, inputPath, tmpFilePath);
            logger.debug("copied input \"{}\" to \"{}\", owner={}", inputPath, tmpFilePath, owner);
        }
        file.setSrcPath(file.getPath());
        file.setPath(tmpFilePath.toString());
        if (file instanceof CWLFile) {
            ((CWLFile) file).setDirname(tmpFilePath.getParent().toString());
        }
    }

    private static List<CommandArgWrapper> sortCommandArguments(List<CommandLineBinding> arguments,
            List<CommandInputParameter> inputs) {
        List<CommandArgWrapper> args = new ArrayList<>();
        int index = 0;
        if (arguments != null) {
            for (CommandLineBinding arg : arguments) {
                args.add(new CommandArgWrapper(index, arg));
                index = index + 1;
            }
        }
        for (CommandInputParameter param : inputs) {
            if (param.getInputBinding() != null) {
                args.add(new CommandArgWrapper(index, param));
                index = index + 1;
            } else {
                // The input parameter may be a record/array type, for a record, it
                // may has many specific types, we need treat them as a generic
                // Record
                index = wrapSchemaTypeParam(args, param, index);
            }
        }
        Collections.sort(args);
        return args;
    }

    private static int wrapSchemaTypeParam(List<CommandArgWrapper> args, CommandInputParameter param, int index) {
        boolean exclusive = false;
        CWLType paramType = param.getType().getType();
        if (paramType == null) {
            paramType = param.getType().getTypes().get(0);
            exclusive = true;
        }
        if (paramType instanceof InputRecordType) {
            @SuppressWarnings("unchecked")
            List<InputRecordField> fields = (List<InputRecordField>) param.getValue();
            for (InputRecordField field : fields) {
                if (field.getInputBinding() != null) {
                    CommandInputParameter fieldParam = new CommandInputParameter(field.getName());
                    fieldParam.setType(field.getRecordType());
                    fieldParam.setInputBinding(field.getInputBinding());
                    fieldParam.setValue(field.getValue());
                    args.add(new CommandArgWrapper(index, fieldParam));
                    index = index + 1;
                    if (exclusive) {
                        break;
                    }
                }
            }
        } else if (paramType instanceof InputArrayType) {
            if (((InputArrayType) paramType).getInputBinding() != null) {
                List<?> items = (List<?>) param.getValue();
                for (Object item : items) {
                    CommandInputParameter itemParam = new CommandInputParameter(param.getId());
                    itemParam.setType(((InputArrayType) paramType).getItems());
                    itemParam.setInputBinding(((InputArrayType) paramType).getInputBinding());
                    itemParam.setValue(item);
                    args.add(new CommandArgWrapper(index, itemParam));
                    index = index + 1;
                }
            }
        }
        return index;
    }

    private static List<String> attachCommandBindings(CWLCommandInstance instance,
            List<CommandInputParameter> inputs,
            List<CommandArgWrapper> sorted) throws CWLException {
        Map<String, String> runtime = instance.getRuntime();
        InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(instance, InlineJavascriptRequirement.class);
        ShellCommandRequirement shellCmdReq = CWLExecUtil.findRequirement(instance, ShellCommandRequirement.class);
        InitialWorkDirRequirement initialWorkDirReq = CWLExecUtil.findRequirement(instance,
                InitialWorkDirRequirement.class);
        List<String> args = new ArrayList<>();
        for (CommandArgWrapper wrapper : sorted) {
            CommandLineBinding arg = wrapper.getArgument();
            if (arg != null) {
                attachArgumentCommandBinding(args, shellCmdReq, jsReq, runtime, inputs, arg);
            }
            CommandInputParameter input = wrapper.getInputParameter();
            if (input != null) {
                attachInputArgument(args,
                        initialWorkDirReq,
                        shellCmdReq,
                        jsReq,
                        instance,
                        inputs,
                        input);
            }
        }
        return args;
    }

    private static void attachArgumentCommandBinding(List<String> args,
            ShellCommandRequirement shellCmdReq,
            InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<CommandInputParameter> inputs,
            CommandLineBinding argument) throws CWLException {
        List<String> prefixedArg = null;
        CWLFieldValue valueFromExpr = argument.getValueFrom();
        if (valueFromExpr != null) {
            if (valueFromExpr.getExpression() != null) {
                String arg = CommandLineBindingEvaluator.evalValueFrom(jsReq, runtime, inputs, null, argument);
                String[] splitArgs = arg.split(CommandLineBindingEvaluator.DEFAULT_ITEM_SEPARATOR);
                if (splitArgs.length > 1) {
                    prefixedArg = addPrefix(argument.getPrefix(), splitArgs);
                } else {
                    prefixedArg = tryToAddPrefix(argument, quoteArg(shellCmdReq, argument.isShellQuote(), arg));
                }
            } else if (valueFromExpr.getValue() != null) {
                prefixedArg = tryToAddPrefix(argument,
                        quoteArg(shellCmdReq, argument.isShellQuote(), argument.getValueFrom().getValue()));
            }
        }
        if (prefixedArg != null) {
            logger.debug("command argument: {}", prefixedArg);
            args.addAll(prefixedArg);
        }
    }

    private static String quoteArg(ShellCommandRequirement shellCmdReq, boolean shellQuote, String arg) {
        String quotedArg = arg;
        boolean needQuote = false;
        if (shellCmdReq == null) {
            needQuote = true;
        } else {
            needQuote = shellQuote;
        }
        if (needQuote && hasShellMetacharacters(arg)) {
            //The arg has been quote by '
            if (arg.startsWith("'") && arg.endsWith("'")) {
                return quotedArg;
            }
            quotedArg = String.format("\"%s\"", arg);
            logger.debug("Quote argument {} to {}", arg, quotedArg);
        }
        return quotedArg;
    }

    private static boolean hasShellMetacharacters(String arg) {
        boolean has = false;
        List<String> shellMetacharacters = Arrays.asList(">", "<", "&", "|", "*", "\\", "\t", "$", ";", "#", "?", "[", "]", "`");
        for (String shellMetacharacter : shellMetacharacters) {
            if (arg.contains(shellMetacharacter)) {
                has = true;
                break;
            }
        }
        return has;
    }

    @SuppressWarnings("unchecked")
    private static void attachInputArgument(List<String> args,
            InitialWorkDirRequirement initialWorkDirReq,
            ShellCommandRequirement shellCmdReq,
            InlineJavascriptRequirement jsReq,
            CWLCommandInstance instance,
            List<CommandInputParameter> inputs,
            CommandInputParameter input) throws CWLException {
        String owner = instance.getOwner();
        Map<String, String> runtime = instance.getRuntime();
        Object inputValue = getInputValue(input);
        // evaluate the binding to get valueFrom, after evaluated,
        // if the valueFrom is not null, use the valueFrom (string)
        // as the actual value.
        String valueFrom = evalValueFrom(input, jsReq, runtime, inputValue, inputs);
        if (valueFrom != null) {
            inputValue = valueFrom;
        }
        // Translate the input value object to argument (String), in this
        // phase, the input type should be an exact type
        CWLType inputType = input.getType().getType();
        // The type of input value is not the defined type, the input value may
        // be from
        // valueFrom, use the value actual type, only string
        if (inputValue instanceof String && inputType.getSymbol() != CWLTypeSymbol.STRING) {
            inputType = new StringType();
        }
        // refer to issue #36 and #37
        if (input.getDelayedValueFromExpr() != null) {
            inputValue = StepInValueFromEvaluator.evalExpr(jsReq,
                    runtime,
                    inputs,
                    input.getSelf(),
                    input.getDelayedValueFromExpr());
            ((List<Object>) input.getValue()).add(inputValue);
            CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
            CommandStdIOEvaluator.eval(jsReq, runtime, copyInputs(inputs, inputValue), commandLineTool.getStdin());
            CommandStdIOEvaluator.eval(jsReq, runtime, copyInputs(inputs, inputValue), commandLineTool.getStderr());
            CommandStdIOEvaluator.eval(jsReq, runtime, copyInputs(inputs, inputValue), commandLineTool.getStdout());
        }
        if (inputValue != null && inputValue != NullValue.NULL) {
            logger.debug("The input (id={}, type={}, value={}) of step {}",
                    input.getId(),
                    inputType.getSymbol(),
                    inputValue,
                    instance.getName());
            String inputArg = null;
            if (CWLTypeSymbol.NULL == inputType.getSymbol()) {
                //The input has multiple types, one is null
                inputArg = toCommandArg(initialWorkDirReq, owner, runtime, input, inputValue);
            } else {
                inputArg = toCommandArg(runtime, input.getId(), inputType, inputValue);
            }
            if (inputArg != null) {
                if (input.getInputBinding() != null) {
                    bindInputBinding(instance.getName(), args, shellCmdReq, input, inputArg);
                } else {
                    logger.debug("The command input argument: {} for step {}", inputArg, instance.getName());
                    args.add(inputArg);
                }
            }
        } else {
            boolean canBeNull = false;
            if (input.getType().getTypes() != null) {
                for (CWLType cwlType : input.getType().getTypes()) {
                    if (cwlType.getSymbol() == CWLTypeSymbol.NULL) {
                        canBeNull = true;
                        break;
                    }
                }
            }
            if (inputType != null && CWLTypeSymbol.NULL == inputType.getSymbol()) {
                canBeNull = true;
            }
            if (!canBeNull) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.exec.argument.is.required",
                                input.getId(), instance.getName()),
                        252);
            }
        }
    }

    private static Object getInputValue(CommandInputParameter input) throws CWLException {
        Object inputValue = null;
        CommandLineBinding inputBinding = input.getInputBinding();
        CWLFieldValue valueFromExpr = inputBinding.getValueFrom();
        if (valueFromExpr != null && valueFromExpr.getValue() != null) {
            inputValue = valueFromExpr.getValue();
        }
        if (inputValue == null) {
            inputValue = input.getValue();
        }
        if (inputValue == null || inputValue == NullValue.NULL) {
            Object defaultValue = input.getDefaultValue();
            if (defaultValue != null) {
                inputValue = defaultValue;
            }
        }
        if ((inputValue instanceof Boolean) && inputBinding.isEmpty()) {
            inputValue = false;
        }
        if (inputBinding.isLoadContents() && (inputValue instanceof CWLFile)) {
            File inputFile = Paths.get(((CWLFile) inputValue).getPath()).toFile();
            ((CWLFile) inputValue).setContents(IOUtil.read64KiB(inputFile));
        }
        return inputValue;
    }

    private static String evalValueFrom(CommandInputParameter input,
            InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            Object self,
            List<CommandInputParameter> inputs) throws CWLException {
        String valueFrom = null;
        CommandLineBinding inputBinding = input.getInputBinding();
        CWLFieldValue valueFromExpr = inputBinding.getValueFrom();
        if (valueFromExpr != null && valueFromExpr.getExpression() != null) {
            if (input.getType().getType().getSymbol() == CWLTypeSymbol.RECORD) {
                //map record to key-value pairs
                Map<String, Object> recordObj = new HashMap<>();
                @SuppressWarnings("unchecked")
                List<InputRecordField> fields = (List<InputRecordField>) self;
                for (InputRecordField filed : fields) {
                    recordObj.put(filed.getName(), filed.getValue());
                }
                self = recordObj;
            }
            valueFrom = CommandLineBindingEvaluator.evalValueFrom(jsReq, runtime, inputs, self, inputBinding);
        }
        return valueFrom;
    }

    private static String toCommandArg(Map<String, String> runtime,
            String inputId,
            CWLType inputType,
            Object inputValue) throws CWLException {
        String argument = null;
        CWLTypeSymbol typeSymbol = inputType.getSymbol();
        switch (typeSymbol) {
        case BOOLEAN:
            if (Boolean.valueOf(String.valueOf(inputValue))) {
                argument = CommandLineBindingEvaluator.BOOLEAN_VALUE;
            }
            break;
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case STRING:
        case ENUM:
            argument = String.valueOf(inputValue);
            break;
        case ANY:
            argument = inputValue.toString();
            break;
        case FILE:
            argument = toCWLFileArg(inputValue);
            break;
        case DIRECTORY:
            argument = ((CWLDirectory) inputValue).getPath();
            break;
        case RECORD:
            argument = toRecordArg(runtime, inputValue);
            break;
        case ARRAY:
            argument = toArrayArg(runtime, inputId, (InputArrayType) inputType, inputValue);
            break;
        default:
            throw new CWLException(String.format("The type (%s) of input <%s> is invalid", typeSymbol, inputId),
                    253);
        }
        return argument;
    }

    private static String toCommandArg(InitialWorkDirRequirement initialWorkDirReq,
            String owner,
            Map<String, String> runtime,
            CommandInputParameter input,
            Object inputValue) throws CWLException {
        String argument = null;
        String inputId = input.getId();
        if (inputValue instanceof CWLFile) {
            input.getType().setType(new FileType());
            prepareInputFile(initialWorkDirReq, runtime, owner, input, input.getType().getType(), inputValue);
            argument = toCWLFileArg(inputValue);
        } else if (inputValue instanceof CWLDirectory) {
            input.getType().setType(new DirectoryType());
            prepareInputFile(initialWorkDirReq, runtime, owner, input, input.getType().getType(), inputValue);
            argument = ((CWLDirectory) inputValue).getPath();
        } else if (inputValue instanceof List) {
            List<CWLType> types = input.getType().getTypes();
            InputArrayType inputType = findInputArrayType(inputId, types);
            input.getType().setType(inputType);
            prepareInputFile(initialWorkDirReq, runtime, owner, input, inputType, inputValue);
            argument = toArrayArg(runtime, inputId, inputType, inputValue);
        } else if (inputValue instanceof InputRecordField) {
            argument = toRecordArg(runtime, inputValue);
        } else if (inputValue instanceof Boolean) {
            if (((Boolean)inputValue).booleanValue()) {
                argument = CommandLineBindingEvaluator.BOOLEAN_VALUE;
            }
        } else {
            argument = String.valueOf(inputValue);
        }
        return argument;
    }

    private static String toArrayArg(Map<String, String> runtime,
            String inputId,
            InputArrayType inputType,
            Object inputValue) throws CWLException {
        String argument = null;
        CWLType itemType = inputType.getItems().getType();
        if (itemType == null) {
            itemType = new InputRecordType();
        }
        CommandLineBinding itemBinding = inputType.getInputBinding();
        List<String> itemArgs = new ArrayList<>();
        @SuppressWarnings("rawtypes")
        List itemValues = (List) inputValue;
        if (!itemValues.isEmpty()) {
            for (Object itemValue : itemValues) {
                List<String> prefixInput = tryToAddPrefix(itemBinding,
                        toCommandArg(runtime, inputId, itemType, itemValue));
                itemArgs.add(String.join(" ", prefixInput));
            }
            if (itemBinding != null && itemBinding.getItemSeparator() != null) {
                argument = String.join(itemBinding.getItemSeparator(), itemArgs);
            } else {
                argument = String.join(" ", itemArgs);
            }
        }
        return argument;
    }

    private static InputArrayType findInputArrayType(String inputId, List<CWLType> types) throws CWLException {
        for (CWLType cwlType : types) {
            if (CWLTypeSymbol.ARRAY == cwlType.getSymbol()) {
                return (InputArrayType) cwlType;
            }
        }
        throw new CWLException(String.format("Expect the type of input <%s> is an array, but null", inputId),
                253);
    }

    private static String toCWLFileArg(Object inputValue) {
        String argument = null;
        if (inputValue instanceof String) {
            // The value from valueFrom, it may be a string
            argument = (String) inputValue;
        } else if (inputValue instanceof CWLFile) {
            argument = ((CWLFile) inputValue).getPath();
        } else if (inputValue instanceof List) {
            List<String> itemArgs = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<CWLFile> itemValues = (List<CWLFile>) inputValue;
            for (CWLFile itemValue : itemValues) {
                itemArgs.add(itemValue.getPath());
            }
            argument = String.join(" ", itemArgs);
        }
        return argument;
    }

    private static String toRecordArg(Map<String, String> runtime, Object inputValue)
            throws CWLException {
        if (inputValue instanceof InputRecordField) {
            InputRecordField recordField = (InputRecordField) inputValue; 
            StringBuilder arguments = new StringBuilder();
            @SuppressWarnings("unchecked")
            List<InputRecordField> fields = (List<InputRecordField>) recordField.getValue();
            List<CommandInputParameter> fieldParams = new ArrayList<>();
            for (InputRecordField field: fields) {
                CommandInputParameter fieldParam = new CommandInputParameter(field.getName());
                fieldParam.setInputBinding(field.getInputBinding());
                fieldParam.setType(field.getRecordType());
                fieldParam.setValue(field.getValue());
                fieldParams.add(fieldParam);
            }
            List<CommandArgWrapper> sorted =  sortCommandArguments(new ArrayList<>(), fieldParams);
            for (CommandArgWrapper wrapper : sorted) {
                CommandInputParameter fieldParam = wrapper.getInputParameter();
                CommandLineBinding fieldBinding = fieldParam.getInputBinding();
                List<String> prefixInput = tryToAddPrefix(fieldBinding, toCommandArg(runtime,
                        fieldParam.getId(),
                        fieldParam.getType().getType(),
                        fieldParam.getValue()));
                if (fieldBinding != null && fieldBinding.getItemSeparator() != null) {
                    arguments.append(String.join(fieldBinding.getItemSeparator(), prefixInput));
                    arguments.append(" ");
                } else {
                    arguments.append(String.join(" ", prefixInput));
                    arguments.append(" ");
                }
            }
            return arguments.toString();
        } else {
            return String.valueOf(inputValue);
        }
    }

    private static List<String> addPrefix(String prefix, String[] argValues) {
        List<String> prefixedArg = new ArrayList<>();
        if (prefix != null && prefix.length() != 0) {
            prefixedArg.add(prefix);
        }
        for (int i = 0; i < argValues.length; i++) {
            prefixedArg.add(argValues[i]);
        }
        return prefixedArg;
    }

    private static void bindInputBinding(String stepName,
            List<String> args,
            ShellCommandRequirement shellCmdReq,
            CommandInputParameter input,
            String inputArg) {
        String itemSeparator = input.getInputBinding().getItemSeparator();
        if (itemSeparator != null) {
            inputArg = inputArg.replaceAll(" ", itemSeparator);
        }
        List<String> prefixedArgs = tryToAddPrefix(input.getInputBinding(),
                quoteArg(shellCmdReq, input.getInputBinding().isShellQuote(), inputArg));
        for (String prefixedArg : prefixedArgs) {
            logger.debug("The command input argument: {} for step {}", inputArg, stepName);
            args.addAll(Arrays.asList(prefixedArg.split(" ")));
        }
    }

    private static List<String> tryToAddPrefix(CommandLineBinding binding, String value) {
        List<String> prefixedArg = null;
        if (value != null) {
            value = value.trim();
            prefixedArg = new ArrayList<>();
            if (binding != null) {
                addPrefixByBinding(prefixedArg, binding, value);
            } else {
                prefixedArg.add(value);
            }
        }
        return prefixedArg;
    }

    private static void addPrefixByBinding(List<String> prefixedArg, CommandLineBinding binding, String value) {
        boolean separate = binding.isSeparate();
        String prefix = binding.getPrefix();
        if (prefix != null && prefix.length() != 0) {
            if (CommandLineBindingEvaluator.BOOLEAN_VALUE.equals(value)) {
                prefixedArg.add(prefix);
            } else {
                if (separate) {
                    prefixedArg.add(prefix);
                    prefixedArg.add(value);
                } else {
                    prefixedArg.add(String.format("%s%s", prefix, value));
                }
            }
        } else {
            prefixedArg.add(value);
        }
    }

    private static String buildStdin(CWLCommandInstance instance) throws CWLException {
        String stdinLocation = null;
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        CWLFieldValue stdinExpr = commandLineTool.getStdin();
        if (stdinExpr != null) {
            String stdin = stdinExpr.getValue();
            if (stdin != null) {
                Path stdinPath = Paths.get(stdin);
                if (!stdinPath.isAbsolute()) {
                    stdinPath = Paths.get(Paths.get(commandLineTool.getDescPath()).getParent().toString(),
                            stdinPath.toString());
                }
                if (stdinPath.toFile().exists()) {
                    Map<String, String> runtimeEnv = instance.getRuntime();
                    Path workStdinPath = Paths.get(runtimeEnv.get(CommonUtil.RUNTIME_TMP_DIR),
                            stdinPath.getFileName().toString());
                    if (!workStdinPath.toFile().exists()) {
                        IOUtil.copy(instance.getOwner(), stdinPath, workStdinPath);
                        logger.debug("Copy stdin {} to {}", stdinPath, workStdinPath);
                    } else {
                        logger.debug("Map stdin {} to {}", stdinPath, workStdinPath);
                    }
                    stdinLocation = workStdinPath.toString();
                } else {
                    throw new CWLException(String.format("The stdin (%s) does not exist", stdin), 255);
                }
            }
        }
        return stdinLocation;
    }

    private static String buildStdout(CWLCommandInstance instance, CommandLineTool commandLineTool, int scatterIndex)
            throws CWLException {
        CWLFieldValue stdoutExpr = commandLineTool.getStdout();
        String outputRedirection = null;
        if (stdoutExpr != null) {
            String stdout = stdoutExpr.getValue();
            if (stdout != null) {
                if (scatterIndex > 0) {
                    // scatter job
                    stdout = String.format("scatter%d", scatterIndex) + File.separator + stdout;
                }
                Path stdoutPath = Paths.get(stdout);
                Path stdoutParentPath = stdoutPath.getParent();
                Path tmpoutDir = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR));
                if (stdoutParentPath != null) {
                    tmpoutDir = Paths.get(tmpoutDir.toString(), stdoutParentPath.toString());
                }
                IOUtil.mkdirs(instance.getOwner(), tmpoutDir);
                String tmpStdout = Paths.get(tmpoutDir.toString(), stdoutPath.getFileName().toString()).toString();
                logger.debug("mapping stdout ({}) to ({})", stdout, tmpStdout);
                outputRedirection = String.format(">%s", tmpStdout);
            }
        }
        return outputRedirection;
    }

    private static List<List<CommandInputParameter>> scatterInputs(CWLCommandInstance instance)
            throws CWLException {
        List<List<CommandInputParameter>> scatterInputs = new ArrayList<>();
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        List<String> scatters = instance.getScatter();
        if (scatters == null) {
            List<CommandInputParameter> inputs = commandLineTool.getInputs();
            scatterInputs.add(inputs);
            return scatterInputs;
        }
        if (instance.getScatterMethod() == ScatterMethod.DOTPRODUCT) {
            List<CWLParameter> scatterList = findScatterList(commandLineTool, scatters);
            scatterByDotProduct(scatterInputs, scatterList);
        } else if (instance.getScatterMethod() == ScatterMethod.FLAT_CROSSPRODUCT ||
                instance.getScatterMethod() == ScatterMethod.NESTED_CROSSPRODUCT) {
            List<CWLParameter> scatterList = findScatterList(commandLineTool, scatters);
            scatterByCrossProduct(instance, scatterInputs, scatterList);
        } else {
            scatterByDefault(scatterInputs, commandLineTool, scatters);
        }
        return scatterInputs;
    }

    private static List<CWLParameter> findScatterList(CommandLineTool commandLineTool, List<String> scatters) {
        List<CWLParameter> scatterList = new ArrayList<>();
        for (String scatter : scatters) {
            for (CWLParameter in : commandLineTool.getInputs()) {
                if (in.getId().equals(scatter)) {
                    scatterList.add(in);
                    break;
                }
            }
        }
        return scatterList;
    }

    private static void scatterByDotProduct(List<List<CommandInputParameter>> scatterInputs,
            List<CWLParameter> scatterList) throws CWLException {
        // The scatterList is a multi-dimensional array, the value of this list should be a list
        int length = 0;
        // Valid the inputs length and determine the final input length 
        for (CWLParameter scatter : scatterList) {
            Object value = scatter.getValue();
            if (value == null) {
                value = scatter.getDefaultValue();
            }
            if (value instanceof List) {
                if (length != 0 && length != ((List<?>) value).size()) {
                    throw new CWLException("The inputs length is not same when dotproduct a scatter inputs", 253);
                }
                length = ((List<?>) value).size();
            } else {
                throw new CWLException("The value of scatter[dot] is not an array.", 253);
            }
        }
        for (int i = 0; i < length; i++) {
            List<Object> dots = new ArrayList<>();
            for (CWLParameter scatter : scatterList) {
                Object value = scatter.getValue();
                if (value == null) {
                    value = scatter.getDefaultValue();
                }
                dots.add(((List<?>) value).get(i));
            }
            List<CommandInputParameter> input = new ArrayList<>();
            int paramIndex = 0;
            for (Object dot : dots) {
                CWLParameter srcParameter = scatterList.get(paramIndex);
                CommandInputParameter parameter = new CommandInputParameter(srcParameter.getId());
                parameter.setInputBinding(((CommandInputParameter) srcParameter).getInputBinding());
                parameter.setType(srcParameter.getType());
                parameter.setValue(dot);
                input.add(parameter);
                paramIndex = paramIndex + 1;
            }
            scatterInputs.add(input);
        }
    }

    private static void scatterByCrossProduct(CWLCommandInstance instance,
            List<List<CommandInputParameter>> scatterInputs,
            List<CWLParameter> scatterList) {
        //The dimValue is a two-dimensional array, the value of this list should be a list
        List<Object> dimValue = new ArrayList<>();
        for (CWLParameter scatter : scatterList) {
            Object value = scatter.getValue();
            if (value == null) {
                value = scatter.getDefaultValue();
            }
            dimValue.add(value);
        }
        for (Object values : dimValue) {
            if (values instanceof List && ((List<?>)values).isEmpty()) {
                instance.setEmptyScatter(true);
                break;
            }
        }
        //The recursiveResult is a two-dimensional array, the value of this list should be a list
        List<Object> recursiveResult = new ArrayList<>();
        cartesianProductRecursive(dimValue, recursiveResult, 0, new ArrayList<Object>());
        for (Object result : recursiveResult) {
            if (result instanceof List) {
                List<CommandInputParameter> input = new ArrayList<>();
                int paramIndex = 0;
                for (Object value : ((List<?>) result)) {
                    CWLParameter srcParameter = scatterList.get(paramIndex);
                    CommandInputParameter parameter = new CommandInputParameter(srcParameter.getId());
                    parameter.setValue(value);
                    CommandLineBinding parameterBinding = ((CommandInputParameter) srcParameter).getInputBinding();
                    parameter.setInputBinding(parameterBinding);
                    parameter.setType(srcParameter.getType());
                    input.add(parameter);
                    paramIndex = paramIndex + 1;
                }
                scatterInputs.add(input);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void scatterByDefault(List<List<CommandInputParameter>> scatterInputs,
            CommandLineTool commandLineTool,
            List<String> scatters) {
        String scatterId = scatters.get(0);
        for (CWLParameter in : commandLineTool.getInputs()) {
            if (in.getId().equals(scatterId)) {
                List<?> values = null;
                if (in.getValue() instanceof List<?>) {
                    values = (List<?>) in.getValue();
                } else {
                    //draft-3, the scatter input is not an array
                    values = Arrays.asList(in.getValue());
                }
                if (in.getValue() == null) {
                    values = (List<String>) in.getDefaultValue();
                }
                for (Object value : values) {
                    CommandInputParameter parameter = new CommandInputParameter(scatterId);
                    parameter.setValue(value);
                    parameter.setInputBinding(((CommandInputParameter) in).getInputBinding());
                    parameter.setType(in.getType());
                    List<CommandInputParameter> input = new ArrayList<>();
                    input.add(parameter);
                    scatterInputs.add(input);
                }
                break;
            }
        }
    }

    private static void cartesianProductRecursive(List<Object> dimValue,
            List<Object> result,
            int layer,
            List<Object> curList) {
        if (layer < dimValue.size() - 1) {
            Object layerValue = dimValue.get(layer);
            if (layerValue instanceof List) {
                List<?> layerList = (List<?>) layerValue;
                if (layerList.isEmpty()) {
                    cartesianProductRecursive(dimValue, result, layer + 1, curList);
                } else {
                    for (int i = 0; i < layerList.size(); i++) {
                        List<Object> list = new ArrayList<>(curList);
                        list.add(layerList.get(i));
                        cartesianProductRecursive(dimValue, result, layer + 1, list);
                    }
                }
            }
        } else if (layer == dimValue.size() - 1) {
            Object layerValue = dimValue.get(layer);
            if (layerValue instanceof List) {
                List<?> layerList = (List<?>) layerValue;
                if (layerList.isEmpty()) {
                    result.add(curList);
                } else {
                    for (int i = 0; i < layerList.size(); i++) {
                        List<Object> list = new ArrayList<>(curList);
                        list.add(layerList.get(i));
                        result.add(list);
                    }
                }
            }
        }
    }
}

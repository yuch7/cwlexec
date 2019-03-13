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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Dirent;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvVarRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvironmentDef;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InitialWorkDirRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.CommandLineTool;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;

/*
 * Utility for building "docker run" command
 */
final class DockerCommandBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DockerCommandBuilder.class);
    private static final boolean ON_WINDOWS = (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1);
    private static final String LINE_SEPARATOR = "line.separator";

    private DockerCommandBuilder() {
    }

    /*
     * Wraps a CWL CommandLineTool instance command to "docker run"
     */
    protected static List<String> buildDockerRun(DockerRequirement dockerReq,
            CWLCommandInstance instance,
            List<String> srcCommands, int scatterIndex) {
        CommandLineTool commandLineTool = (CommandLineTool) instance.getProcess();
        String owner = instance.getOwner();
        List<String> commands = new ArrayList<>();
        String dockerStageDir = "/var/lib/cwl";
        //Keep container STDIN open even if not attached (-i) and Automatically remove
        //the container when it exits (--rm)
        commands.addAll(Arrays.asList("docker", "run", "-i", "--rm"));
        // docker_outdir: output directory inside docker for this job
        String dockerOutdir = dockerReq.getDockerOutputDirectory();
        if (dockerOutdir == null) {
            dockerOutdir = "/var/spool/cwl";
        }
        // volume mapping
        String volumeRW = "--volume=%s:%s:rw";
        String volumeRO = "--volume=%s:%s:ro";
        Map<String, String> runtime = instance.getRuntime();
        // map working directory to docker outdir
        String tmpOutput = Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR)).toString();
        if (scatterIndex > 0) {
            // scatter job
        	tmpOutput = tmpOutput + File.separator + String.format("scatter%d", scatterIndex);
        }
        commands.add(String.format(volumeRW, tmpOutput, dockerOutdir));
        // map working directory to docker /tmp
        String tmp = Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR)).toString();
        commands.add(String.format(volumeRW, tmp, "/tmp"));
        Map<String, String> inputsMapping = mappingInputs(commandLineTool.getInputs(), dockerStageDir);
        // mapping InitialWorkDirRequirement
        InitialWorkDirRequirement initialWorkDirReq = CWLExecUtil.findRequirement(instance,
                InitialWorkDirRequirement.class);
        if (initialWorkDirReq != null) {
            mapInitialWorkDirReq(dockerOutdir, tmpOutput, initialWorkDirReq, commands);
            rewriteEntryFile(runtime, initialWorkDirReq, inputsMapping, dockerOutdir);
        }
        // mapping inputs
        for (Entry<String, String> mapping : inputsMapping.entrySet()) {
            commands.add(String.format(volumeRW, Paths.get(mapping.getKey()).getParent(),
                    Paths.get(mapping.getValue()).getParent()));
            commands.add(String.format(volumeRO, mapping.getKey(), mapping.getValue()));
        }
        commands.add(String.format("--workdir=%s", dockerOutdir));
        int userId = getUserId(owner);
        int groupId = getGroupId(owner);
        if (userId != -1 && groupId != -1) {
            commands.add(String.format("--user=%d:%d", userId, groupId));
        }
        // docker /tmp and outdir are mapped to working directory
        commands.add(String.format("--env=TMPDIR=%s", dockerOutdir));
        commands.add(String.format("--env=HOME=%s", dockerOutdir));
        EnvVarRequirement envVarReq = CWLExecUtil.findRequirement(instance, EnvVarRequirement.class);
        // If has EnvVarRequirement, add the env to --env
        if (envVarReq != null) {
            for (EnvironmentDef envDef : envVarReq.getEnvDef()) {
                commands.add(String.format("--env=%s=%s",
                        envDef.getEnvName(),
                        envDef.getEnvValue().getValue()));
            }
        }
        String imageId = findImageId(dockerReq);
        commands.add(imageId);
        if (srcCommands != null) {
            logger.debug("Build docker run source command: {}", srcCommands);
            commands.add(String.join(" ",
                    replaceInputsAndOutpus(commandLineTool, runtime, srcCommands, inputsMapping, dockerOutdir)));
        }
        logger.debug("Build docker run as: {}", commands);
        return commands;
    }

    /*
     * Finds a docker image from local images
     */
    protected static boolean hasDockerImage(DockerRequirement dockerRequirement) {
        boolean has = false;
        String imageId = findImageId(dockerRequirement);
        if (imageId != null) {
            List<String> commands = Arrays.asList("docker", "images", "-q", imageId);
            CommandExecutionResult result = CommandExecutor.run(commands);
            if (result.getExitCode() == 0 && result.getOutMsg() != null && !result.getOutMsg().isEmpty()) {
                logger.debug("The docker image \"{}\" ({}) exists", imageId, result.getOutMsg());
                has = true;
            }
        }
        return has;
    }

    protected static String findImageId(DockerRequirement dockerRequirement) {
        String imageId = null;
        String dockerImageId = dockerRequirement.getDockerImageId();
        if (dockerImageId == null) {
            dockerImageId = dockerRequirement.getDockerPull();
        }
        if (dockerImageId != null) {
            String[] ids = dockerImageId.split(":");
            if (ids.length == 1) {
                imageId = String.format("%s:%s", dockerImageId, "latest");
            } else if (ids.length == 2) {
                Matcher matcher = Pattern.compile("[\\w][\\w.-]{0,127}").matcher(ids[1]);
                if (matcher.matches()) {
                    imageId = dockerImageId;
                } else {
                    imageId = String.format("%s:%s", dockerImageId, "latest");
                }
            } else if (ids.length == 3) {
                Matcher matcher = Pattern.compile("[\\w][\\w.-]{0,127}").matcher(ids[2]);
                if (matcher.matches()) {
                    imageId = dockerImageId;
                }
            }
        }
        return imageId;
    }

    private static Map<String, String> mappingInputs(List<CommandInputParameter> inputs, String dockerStageDir) {
        Map<String, String> mapping = new HashMap<>();
        for (CommandInputParameter input : inputs) {
            mappingFiles(mapping, dockerStageDir, findInputFiles(input));
        }
        return mapping;
    }

    private static void mappingFiles(Map<String, String> mapping, String dockerStageDir, List<CWLFileBase> files) {
        if (files != null) {
            for (CWLFileBase file : files) {
                Path srcFilePath = Paths.get(file.getPath());
                Path destFilePath = Paths.get(dockerStageDir)
                        .resolve(srcFilePath.getParent().getFileName())
                        .resolve(srcFilePath.getFileName());
                logger.debug("Map docker volume \"{}\" to \"{}\"", srcFilePath, destFilePath);
                mapping.put(srcFilePath.toString(), destFilePath.toString());
                if (file instanceof CWLFile) {
                    mappingFiles(mapping, dockerStageDir, ((CWLFile) file).getSecondaryFiles());
                } else if (file instanceof CWLDirectory) {
                    mappingFiles(mapping, dockerStageDir, ((CWLDirectory) file).getListing());
                }
            }
        }
    }

    private static List<CWLFileBase> findInputFiles(CommandInputParameter input) {
        List<CWLFileBase> files = new ArrayList<>();
        Object value = input.getValue();
        if (value == null) {
            value = input.getDefaultValue();
        }
        if (value == null) {
            return files;
        }
        CWLType inputType = input.getType().getType();
        CWLTypeSymbol inputTypeSymbol = inputType.getSymbol();
        switch (inputTypeSymbol) {
        case FILE:
        	if(value instanceof List) {
        		List<CWLFile> fileArray = (List<CWLFile>) value;
        		for (CWLFile f : fileArray) {
                    files.add(f);
                }
        	} else {
        		files.add((CWLFile) value);
        	}
            break;
        case DIRECTORY:
            files.add((CWLDirectory) value);
            break;
        case ARRAY:
            CWLType itemType = ((InputArrayType) inputType).getItems().getType();
            CWLTypeSymbol itemTypeSymbol = itemType.getSymbol();
            switch (itemTypeSymbol) {
            case FILE:
                @SuppressWarnings("unchecked")
                List<CWLFile> fileArray = (List<CWLFile>) value;
                for (CWLFile f : fileArray) {
                    files.add(f);
                }
                break;
            case DIRECTORY:
                @SuppressWarnings("unchecked")
                List<CWLDirectory> dirArray = (List<CWLDirectory>) value;
                for (CWLDirectory d : dirArray) {
                    files.add(d);
                }
                break;
            default:
                break;
            }
            break;
        default:
            break;
        }
        return files;
    }

    private static int getUserId(String owner) {
        int userId = -1;
        if (!ON_WINDOWS) {
            CommandExecutionResult result = CommandExecutor.run(Arrays.asList("id", "-u", owner));
            if (result.isExecuted() && result.getExitCode() == 0) {
                userId = Integer.valueOf(result.getOutMsg());
            }
        }
        return userId;
    }

    private static int getGroupId(String owner) {
        int groupId = -1;
        if (!ON_WINDOWS) {
            CommandExecutionResult result = CommandExecutor.run(Arrays.asList("id", "-g", owner));
            if (result.isExecuted() && result.getExitCode() == 0) {
                groupId = Integer.valueOf(result.getOutMsg());
            }
        }
        return groupId;
    }

    private static void mapInitialWorkDirReq(String dockerOutdir,
            String tmpOutput,
            InitialWorkDirRequirement initialWorkDirReq,
            List<String> commands) {
        List<Dirent> cwlDirents = initialWorkDirReq.getDirentListing();
        if (cwlDirents != null) {
            for (Dirent dirent : cwlDirents) {
                String writable = dirent.isWritable() ? "rw" : "ro";
                if (dirent.getEntry().getValue() != null
                        && Paths.get(dirent.getEntry().getValue()).toFile().exists()) {
                    commands.add(String.format("--volume=%s:%s:%s", tmpOutput + "/" + dirent.getEntry().getValue(),
                            dockerOutdir + "/" + dirent.getEntry().getValue(), writable));
                }
                if (dirent.getEntryname().getValue() != null) {
                    commands.add(
                            String.format("--volume=%s:%s:%s", tmpOutput + "/" + dirent.getEntryname().getValue(),
                                    dockerOutdir + "/" + dirent.getEntryname().getValue(), writable));
                }
            }
        }
    }

    private static void rewriteEntryFile(Map<String, String> runtime,
            InitialWorkDirRequirement initialWorkDirReq,
            Map<String, String> inputsMapping,
            String dockerOutdir) {
        List<Dirent> dirents = initialWorkDirReq.getDirentListing();
        if (dirents != null) {
            try {
                for (Dirent dirent : dirents) {
                    String entryName = dirent.getEntryname().getValue();
                    if (entryName != null) {
                        File entryFile = new File(runtime.get(CommonUtil.RUNTIME_TMP_DIR) +
                                File.separator +
                                entryName);
                        if (entryFile.exists() && entryFile.isFile()) {
                            StringBuilder contentBuilder = new StringBuilder();
                            mapEntries(entryFile, inputsMapping, dockerOutdir, contentBuilder);
                            IOUtil.write(entryFile, contentBuilder.toString());
                        }
                    }
                }
            } catch (CWLException e) {
                logger.error("Failed to rewrite entry file: {}", e.getMessage());
            }
        }
    }

    private static void mapEntries(File entryFile,
            Map<String, String> inputsMapping,
            String dockerOutdir,
            StringBuilder contentBuilder) {
        Stream<String> stream = null;
        try {
            stream = Files.lines(entryFile.toPath(), StandardCharsets.UTF_8);
            stream.forEach(s -> {
                if ((s.length() != 0) && (s.indexOf('/') != -1)) {
                    String srcPath = s.substring(s.indexOf('/')).trim();
                    String mappedPath = inputsMapping.get(srcPath);
                    if (mappedPath == null) {
                        mappedPath = Paths.get(dockerOutdir).resolve(Paths.get(srcPath).getFileName()).toString();
                    }
                    String mapped = s.replaceAll(srcPath, mappedPath);
                    logger.debug("entry: \"{}\" mapped to \"{}\"", s, mapped);
                    contentBuilder.append(mapped).append(System.getProperty(LINE_SEPARATOR));
                } else {
                    logger.debug("entry: \"{}\"", s);
                    contentBuilder.append(s).append(System.getProperty(LINE_SEPARATOR));
                }
            });
        } catch (IOException e) {
            logger.error("Failed to read \"{}\": {}", entryFile.getAbsoluteFile(), e.getMessage());
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private static List<String> replaceInputsAndOutpus(CommandLineTool commandLineTool,
            Map<String, String> runtime,
            List<String> srcCommands,
            Map<String, String> inputsMapping,
            String dockerOutdir) {
        List<String> mappedCommands = new ArrayList<>();
        for (String command : srcCommands) {
            for (Entry<String, String> mapping : inputsMapping.entrySet()) {
                command = command.replaceAll(mapping.getKey(), mapping.getValue());
            }
            command = command.replaceAll(runtime.get(CommonUtil.RUNTIME_OUTPUT_DIR), dockerOutdir);
            mappedCommands.add(command);
        }
        if (commandLineTool.getStdin() != null) {
            mappedCommands = Arrays.asList("/bin/sh",
                    "-c",
                    String.format("'%s'", String.join(" ", mappedCommands).replaceAll("'", "'\"'\"'")));
        }
        return mappedCommands;
    }
}

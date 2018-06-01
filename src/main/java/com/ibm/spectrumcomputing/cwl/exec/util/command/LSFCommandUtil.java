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
package com.ibm.spectrumcomputing.cwl.exec.util.command;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstanceState;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLScatterHolder;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Utility methods for handling execution commands with LSF
 */
public class LSFCommandUtil {

    private LSFCommandUtil() {
    }

    private static final Logger logger = LoggerFactory.getLogger(LSFCommandUtil.class);

    /**
     * Pages the wait commands to avoid the wait command is too long
     * 
     * @param waitJobs
     *            the wait jobs
     * @return the bwait commands that is in scatter holder
     */
    public static List<CWLScatterHolder> pageWaitCommands(List<String> waitJobs) {
        int pageLimits = 100;
        List<CWLScatterHolder> scatterHolders = new ArrayList<>();
        if (waitJobs != null) {
            int nGroup = waitJobs.size() / pageLimits;
            int start = 0;
            int end = 0;
            for (int i = 0; i < nGroup; i++) {
                start = i * pageLimits;
                end = (i + 1) * pageLimits;
                List<String> listPage = waitJobs.subList(start, end);
                List<String> waitCommand = Arrays.asList("bwait", "-w", String.join(" && ", listPage));
                logger.info(ResourceLoader.getMessage("cwl.exec.job.start.wait",
                        CWLExecUtil.asPrettyCommandStr(waitCommand)));
                CWLScatterHolder scatterHolder = new CWLScatterHolder();
                scatterHolder.setCommand(waitCommand);
                scatterHolders.add(scatterHolder);
            }
            if (end < waitJobs.size()) {
                List<String> listPage = waitJobs.subList(end, waitJobs.size());
                List<String> waitCommand = Arrays.asList("bwait", "-w", String.join(" && ", listPage));
                logger.info(ResourceLoader.getMessage("cwl.exec.job.start.wait",
                        CWLExecUtil.asPrettyCommandStr(waitCommand)));
                CWLScatterHolder scatterHolder = new CWLScatterHolder();
                scatterHolder.setCommand(waitCommand);
                scatterHolders.add(scatterHolder);
            }
        }
        return scatterHolders;
    }

    /**
     * Find a LSF job state by id
     * 
     * @param jobId
     *            The id of the LSF job
     * @return The job state
     */
    public static CWLInstanceState findLSFJobState(long jobId) {
        CommandExecutionResult bjobsResult = CommandExecutor.run(Arrays.asList("/bin/sh",
                "-c",
                String.format("bjobs -o stat %d | awk 'NR==2 {print $1}'", jobId)));
        if (logger.isDebugEnabled()) {
            logger.debug("Job <{}> state={}", jobId, bjobsResult.getOutMsg());
        }
        return toCWLProcessState(bjobsResult.getOutMsg());
    }

    /**
     * Kill LSF jobs by id
     * 
     * @param jobIds
     *            The id of jobs
     */
    public static void killJobs(List<Long> jobIds) {
        if (jobIds != null && !jobIds.isEmpty()) {
            List<String> commands = new ArrayList<>();
            commands.add("bkill");
            for (Long jobId : jobIds) {
                commands.add(String.valueOf(jobId));
            }
            CommandExecutor.run(commands);
        }
    }

    /**
     * Find a LSF job exit code
     * 
     * @param jobId
     *            The id of the LSF job
     * @return The job exit code
     */
    public static int findLSFJobExitCode(long jobId) {
        CommandExecutionResult bjobsResult = CommandExecutor.run(Arrays.asList("/bin/sh",
                "-c",
                String.format("bjobs -o exit_code %d | awk 'NR==2 {print $1}'", jobId)));
        return Integer.valueOf(bjobsResult.getOutMsg());
    }

    /**
     * Prepares docker command in LSF environment
     * 
     * @param app
     *            The LSF application profile
     * @param dockerReq
     *            The DockerRequirement
     * @param instance
     *            The CWL instance
     * @return The docker command
     * @throws CWLException
     *             Failed to perare docker command
     */
    public static List<String> prepareLSFDocker(String app,
            DockerRequirement dockerReq,
            CWLCommandInstance instance) throws CWLException {
        List<String> commands = null;
        if ((app != null && app.length() > 0) && instance.isReadyToRun()) {
            commands = prepareLSFDockerByApp(dockerReq, instance);
        } else {
            commands = prepatedLSFDockerByRes(dockerReq, instance);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Prepare LSF docker run as\n{}", CWLExecUtil.asPrettyCommandStr(commands));
        }
        return commands;
    }

    private static List<String> prepareLSFDockerByApp(DockerRequirement dockerReq,
            CWLCommandInstance instance) throws CWLException {
        List<String> commands = new ArrayList<>();
        String imageId = dockerReq.getDockerImageId();
        if (imageId == null) {
            imageId = dockerReq.getDockerPull();
            if (imageId != null) {
                logger.debug("dockerImageId is not specified, use dockerPull <{}> instead", imageId);
            } else {
                throw new CWLException(ResourceLoader.getMessage("cwl.docker.imageId.required", instance.getName()),
                        255);
            }
        }
        String env = "LSB_CONTAINER_IMAGE=" + imageId;
        List<String> dockerCommands = DockerCommandBuilder.buildDockerRun(dockerReq, instance, null);
        if (!dockerCommands.isEmpty()) {
            StringBuilder option = new StringBuilder();
            for (String command : dockerCommands) {
                if ((command.startsWith("--volume") && command.indexOf(":/tmp:") == -1)
                        || command.startsWith("--env") || command.startsWith("--workdir")) {
                    option.append(command + " ");
                }
            }
            if (option.length() > 0) {
                option.substring(0, option.length() - 1);
            }
            env += ", LSB_CONTAINER_OPTIONS='" + option + "'";
        }
        commands.addAll(Arrays.asList("-env", env));
        return commands;
    }

    private static List<String> prepatedLSFDockerByRes(DockerRequirement dockerReq,
            CWLCommandInstance instance) throws CWLException {
        List<String> commands = new ArrayList<>();
        if (DockerCommandBuilder.hasDockerImage(dockerReq)) {
            return commands;
        }
        String imageId = dockerReq.getDockerImageId();
        if (imageId == null) {
            imageId = dockerReq.getDockerPull();
            if (imageId != null) {
                logger.debug("dockerImageId is not specified, use dockerPull <{}> instead", imageId);
            }
        }
        String dockerFileDir = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR)).toString();
        String tag = "";
        if (dockerReq.getDockerFile() != null) {
            IOUtil.write(new File(dockerFileDir + "/Dockerfile"), dockerReq.getDockerFile());
            if (imageId != null) {
                tag = String.format("--tag=%s ", imageId);
            }
            commands.addAll(Arrays.asList("-E", "docker build " + tag + dockerFileDir + "/."));
        } else if (dockerReq.getDockerLoad() != null) {
            String fileURI = dockerReq.getDockerLoad();
            if (isRemote(fileURI)) {
                fileURI = IOUtil.yieldFile(fileURI, dockerFileDir, null, false).getAbsolutePath();
            }
            commands.addAll(Arrays.asList("-E", "docker load --input " + fileURI));
        } else if (dockerReq.getDockerImport() != null) {
            String fileURI = dockerReq.getDockerImport();
            if (isRemote(fileURI)) {
                logger.debug("Download the docker image from \"{}\" ...", fileURI);
                fileURI = IOUtil.yieldFile(fileURI, dockerFileDir, null, false).getAbsolutePath();
            }
            if (imageId != null) {
                tag = " " + imageId;
            }
            commands.addAll(Arrays.asList("-E", "docker import " + fileURI + tag));
        }
        return commands;
    }

    private static boolean isRemote(String fileURI) {
        return fileURI.startsWith("http://") || fileURI.startsWith("https://") || fileURI.startsWith("ftp://");
    }

    private static CWLInstanceState toCWLProcessState(String lsfJobState) {
        CWLInstanceState state = CWLInstanceState.EXITED;
        if (lsfJobState != null) {
            switch (lsfJobState.trim()) {
            case "PEND":
                state = CWLInstanceState.PENDING;
                break;
            case "RUN":
                state = CWLInstanceState.RUNNING;
                break;
            case "PSUSP":
            case "USUSP":
            case "SSUSP":
                state = CWLInstanceState.SUSPENDED;
                break;
            case "DONE":
                state = CWLInstanceState.DONE;
                break;
            case "EXIT":
                state = CWLInstanceState.EXITED;
                break;
            default:
                state = CWLInstanceState.UNKNOWN;
                break;
            }
        }
        return state;
    }
}

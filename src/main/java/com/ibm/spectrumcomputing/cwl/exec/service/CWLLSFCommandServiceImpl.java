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
package com.ibm.spectrumcomputing.cwl.exec.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecConfUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandUtil;
import com.ibm.spectrumcomputing.cwl.exec.util.command.LSFCommandUtil;
import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLScatterHolder;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvVarRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvironmentDef;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ResourceRequirement;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

/*
 * A LSF runtime command implementation
 */
final class CWLLSFCommandServiceImpl implements CWLCommandService {

    private static final Logger logger = LoggerFactory.getLogger(CWLLSFCommandServiceImpl.class);

    @Override
    public List<String> buildCommand(CWLCommandInstance instance) throws CWLException {
        List<String> baseCommands = null;
        if (instance.isReadyToRun()) {
            baseCommands = CommandUtil.buildCommand(instance);
        }
        return buildCommand(instance, baseCommands, 0);
    }

    @Override
    public void buildScatterCommand(CWLCommandInstance instance) throws CWLException {
        CommandUtil.buildScatterCommand(instance);
        for (CWLScatterHolder scatterHolder : instance.getScatterHolders()) {
            List<String> srcCommand = scatterHolder.getCommand();
            logger.debug("scatter - source command: {}", srcCommand);
            List<String> lsfCommand = buildCommand(instance, srcCommand, scatterHolder.getScatterIndex());
            logger.debug("scatter - lsf command: {}", lsfCommand);
            //update the command
            scatterHolder.setCommand(lsfCommand);
        }
    }

    private List<String> buildCommand(CWLCommandInstance instance, List<String> baseCommands, int scatterIndex)
            throws CWLException {
        List<String> commands = new ArrayList<>();
        commands.add("bsub");
        if (scatterIndex > 0) {
            String scatterWorkDir = instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR)
                    + File.separator
                    + String.format("scatter%d", scatterIndex);
            IOUtil.mkdirs(instance.getOwner(), Paths.get(scatterWorkDir));
            commands.addAll(Arrays.asList("-cwd", scatterWorkDir));
        } else {
            commands.addAll(Arrays.asList("-cwd", instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR)));
        }
        commands.addAll(Arrays.asList("-o", "%J_out"));
        commands.addAll(Arrays.asList("-e", "%J_err"));
        CWLInstance mainInstance = CWLExecUtil.findMainInstance(instance);
        List<String> envVars = new ArrayList<>();
        if (System.getProperty(CommandUtil.PRESERVE_ENTIRE_ENV) != null &&
                "True".equalsIgnoreCase(System.getProperty(CommandUtil.PRESERVE_ENTIRE_ENV))) {
            logger.debug("Inherit all environment variables");
            envVars.add("all");
        }
        if (System.getProperty(CommandUtil.PRESERVE_ENV) != null) {
            envVars.addAll(Arrays.asList(System.getProperty(CommandUtil.PRESERVE_ENV).split(",")));
            logger.debug("Inherit vars: {}", envVars);
        }
        DockerRequirement dockerRequirement = CWLExecUtil.findRequirement(instance, DockerRequirement.class);
        //The TMPDIR will be set by docker run
        if (dockerRequirement == null) {
            envVars.add(String.format("TMPDIR=%s", mainInstance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR)));
        }
        EnvVarRequirement envVarRequirement = CWLExecUtil.findRequirement(instance, EnvVarRequirement.class);
        if (envVarRequirement != null) {
            for (EnvironmentDef envDef : envVarRequirement.getEnvDef()) {
                envVars.add(String.format("%s=%s", envDef.getEnvName(), envDef.getEnvValue().getValue()));
            }
        }
        if (!envVars.isEmpty()) {
            commands.addAll(Arrays.asList("-env", String.join(",", envVars)));
        }
        String app = addLSFOptionsFromExecConf(instance, commands);
        if (dockerRequirement != null) {
            commands.addAll(LSFCommandUtil.prepareLSFDocker(app, dockerRequirement, instance));
        }
        // The current job is not ready, using -H to hold it
        if (baseCommands == null) {
            Map<String, String> runtime = instance.getRuntime();
            Path placeholder = Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR), instance.getName().replace("/", "_"));
            IOUtil.createCommandScript(placeholder, null);
            commands.addAll(Arrays.asList("-H", placeholder.toString()));
        } else {
            if (dockerRequirement != null && app != null) {
                commands.add(baseCommands.get(baseCommands.size() - 1));
            } else {
                commands.add(String.join(" ", baseCommands));
            }
        }
        return commands;
    }

    /*
     * If user configured the app option, return it, else return null
     */
    private String addLSFOptionsFromExecConf(CWLCommandInstance instance, List<String> commands) {
        CWLInstance main = CWLExecUtil.findMainInstance(instance);
        FlowExecConf flowExecConf = main.getFlowExecConf();
        String queue = CWLExecConfUtil.getQueue(flowExecConf, instance.getName());
        if (queue != null && queue.length() > 0) {
            commands.addAll(Arrays.asList("-q", queue));
        }
        String project = CWLExecConfUtil.getProject(flowExecConf, instance.getName());
        if (project != null && project.length() > 0) {
            commands.addAll(Arrays.asList("-P", project));
        }
        if (CWLExecConfUtil.isRerunnable(flowExecConf, instance.getName())) {
            commands.add("-r");
        }
        String res = CWLExecConfUtil.getResource(flowExecConf, instance.getName());
        if (res != null && res.length() > 0) {
            commands.addAll(Arrays.asList("-R", res));
        } else {
            ResourceRequirement resReq = CWLExecUtil.findRequirement(instance, ResourceRequirement.class);
            if (resReq != null) {
                commands.addAll(buildResCommand(resReq));
            }
        }
        String app = CWLExecConfUtil.getApp(flowExecConf, instance.getName());
        if (app != null && app.length() > 0) {
            commands.addAll(Arrays.asList("-app", app));
        }
        return app;
    }

    private List<String> buildResCommand(ResourceRequirement resourceRequirement) {
        List<String> commands = new ArrayList<>();
        Long ramMax = resourceRequirement.getRamMax();
        Long ramMin = resourceRequirement.getRamMin();
        Long coresMax = resourceRequirement.getCoresMax();
        Long coresMin = resourceRequirement.getCoresMin();
        if (ramMin != null) {
            if (ramMin != ramMax) {
                commands.addAll(Arrays.asList("-M", String.valueOf(ramMax)));
            }
            commands.addAll(Arrays.asList("-R", String.format("mem > %d", ramMin)));
        }
        if (coresMin != null) {
            if (coresMin == coresMax) {
                commands.addAll(Arrays.asList("-n", String.format("%d", coresMin)));
            } else {
                commands.addAll(Arrays.asList("-n", String.format("%d,%d", coresMin, coresMax)));
            }
        }
        return commands;
    }

}

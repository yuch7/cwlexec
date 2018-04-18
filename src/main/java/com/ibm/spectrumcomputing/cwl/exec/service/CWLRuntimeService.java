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
package com.ibm.spectrumcomputing.cwl.exec.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.persistence.CWLMainProcessRecord;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ResourceRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

/**
 * Provides methods to build runtime environment dependent commands
 */
public class CWLRuntimeService {

    private static final String CORES = "cores";
    private static final String RAM = "ram";
    private static final String OUTDIR_SIZE = "outdirSize";
    private static final String TMPDIR_SIZE = "tmpdirSize";

    /**
     * Builds a runtime environment dependent command for a CWL CommandLineTool
     * instance
     * 
     * @param commandInstance
     *            A CWL CommandLineTool instance
     * @return An execution command
     * @throws CWLException
     *             Failed to build the command
     */
    public List<String> buildRuntimeCommand(CWLCommandInstance commandInstance) throws CWLException {
        RuntimeEnv runtimeEnv = commandInstance.getRuntimeEnv();
        CWLCommandService processService = CWLServiceFactory.getCommandService(runtimeEnv);
        return processService.buildCommand(commandInstance);
    }

    /**
     * Builds a list of runtime environment dependent commands for a CWL scatter step
     * instance
     * 
     * @param commandInstance
     *            A CWL scatter step instance
     * @return The list of scatter commands
     * @throws CWLException
     *             Failed to build the commands
     */
    public List<List<String>> buildRuntimeScatterCommands(CWLCommandInstance commandInstance) throws CWLException {
        RuntimeEnv runtimeEnv = commandInstance.getRuntimeEnv();
        CWLCommandService processService = CWLServiceFactory.getCommandService(runtimeEnv);
        return processService.buildScatterCommand(commandInstance);
    }

    /*
     * Builds runtime environment for a CWL main process
     */
    protected Map<String, String> prepareMainRuntime(CWLMainProcessRecord record,
            CWLProcess processObj) throws CWLException {
        String id = record.getId();
        String owner = record.getOwner();
        Map<String, String> runtime = new HashMap<>();
        // Prepare outdir and tmpdir, map them to work directory
        String workTopDir = System.getProperty(IOUtil.WORK_TOP_DIR);
        Path workdir = Paths.get(workTopDir, id);
        IOUtil.mkdirs(owner, workdir);
        runtime.put(CommonUtil.RUNTIME_TMP_DIR, workdir.toString());
        runtime.put(TMPDIR_SIZE, String.valueOf(workdir.toFile().getTotalSpace()));
        runtime.put(CommonUtil.RUNTIME_OUTPUT_DIR, workdir.toString());
        runtime.put(OUTDIR_SIZE, String.valueOf(workdir.toFile().getTotalSpace()));
        ResourceRequirement resReq = CWLExecUtil.findRequirement(processObj, ResourceRequirement.class);
        if (resReq != null) {
            runtime.put(CORES, resReq.getCoresMin() == null ? "1" : String.valueOf(resReq.getCoresMin().longValue()));
            runtime.put(RAM, resReq.getRamMin() == null ? "1024" : String.valueOf(resReq.getRamMin().longValue()));
        } else {
            runtime.put(CORES, "1");
            runtime.put(RAM, "1024");
        }
        return runtime;
    }

    /*
     * Builds runtime environment for a CWL step process
     */
    protected Map<String, String> prepareStepRuntime(String owner,
            CWLWorkflowInstance parent,
            WorkflowStep step) throws CWLException {
        Map<String, String> runtime = new HashMap<>();
        Path workdir = Paths.get(parent.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR), step.getId());
        IOUtil.mkdirs(owner, workdir);
        // The outputs of step will be located in workdir
        runtime.put(CommonUtil.RUNTIME_OUTPUT_DIR, workdir.toString());
        runtime.put(OUTDIR_SIZE, String.valueOf(workdir.toFile().getTotalSpace()));
        runtime.put(CommonUtil.RUNTIME_TMP_DIR, workdir.toString());
        runtime.put(TMPDIR_SIZE, String.valueOf(workdir.toFile().getTotalSpace()));
        ResourceRequirement resReq = CWLExecUtil.findRequirement(step, ResourceRequirement.class);
        if (resReq == null) {
            resReq = CWLExecUtil.findRequirement(parent, ResourceRequirement.class);
        }
        if (resReq != null) {
            runtime.put(CORES, resReq.getCoresMin() == null ? "0" : String.valueOf(resReq.getCoresMin().longValue()));
            runtime.put(RAM, resReq.getRamMin() == null ? "0" : String.valueOf(resReq.getRamMin().longValue()));
        } else {
            runtime.put(CORES, "1");
            runtime.put(RAM, "1");
        }
        return runtime;
    }
}

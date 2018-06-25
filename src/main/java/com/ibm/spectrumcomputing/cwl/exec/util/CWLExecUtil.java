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
package com.ibm.spectrumcomputing.cwl.exec.util;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLInstance;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLWorkflowInstance;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Requirement;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Utility methods for cwlexec
 */
public final class CWLExecUtil {

    private static final Logger logger = LoggerFactory.getLogger(CWLExecUtil.class);

    private CWLExecUtil() {
    }

    /**
     * Finds a given CWL process instance parent
     * 
     * @param instance
     *            A given CWL process instance
     * @return The parent of the given CWL process instance
     */
    public static CWLInstance findMainInstance(CWLInstance instance) {
        if (instance == null) {
            return null;
        }
        CWLWorkflowInstance parent = instance.getParent();
        if (parent == null) {
            return instance;
        } else {
            return findMainInstance(parent);
        }
    }

    /**
     * Finds a CWL process instance requirement from a CWL step process instance
     * by a given class of a requirement on runtime. <br>
     * The step requirement overrides the process (run field) requirement, the
     * step requirement overrides the parent requirement and the requirement
     * overrides hint
     * 
     * @param <T>
     *            A class of {@link Requirement}
     * @param instance
     *            A CWL step process instance
     * @param clazz
     *            A given class of a requirement
     * @return If the requirement are not found, return null
     */
    public static <T extends Requirement> T findRequirement(CWLInstance instance, Class<T> clazz) {
        T req = findRequirement(instance, clazz, false);
        if (req == null) {
            req = findRequirement(instance, clazz, true);
        }
        return req;
    }

    /**
     * Finds a CWL Workflow step requirement from a CWL Workflow step by a given
     * class of a requirement on a CWL Workflow step scope. <br>
     * The step requirement overrides process (run field) requirement and the
     * requirement overrides hint
     * 
     * @param <T>
     *            A class of {@link Requirement}
     * @param step
     *            A CWL Workflow step
     * @param clazz
     *            A given class of a requirement
     * @return If the requirement are not found, return null
     */
    public static <T extends Requirement> T findRequirement(WorkflowStep step, Class<T> clazz) {
        T req = null;
        if (step != null) {
            req = findRequirement(step, clazz, false);
            if (req == null) {
                req = findRequirement(step, clazz, true);
            }
        }
        return req;
    }

    /**
     * Finds a requirement by a given class of a requirement on CWL main process
     * scope
     * 
     * @param <T>
     *            A class of {@link Requirement}
     * @param process
     *            A CWL main process
     * @param clazz
     *            A given class of a requirement
     * @return If the requirement are not found, return null
     */
    public static <T extends Requirement> T findRequirement(CWLProcess process, Class<T> clazz) {
        T req = null;
        if (process != null) {
            req = findReq(process.getRequirements(), clazz);
            if (req == null) {
                req = findReq(process.getHints(), clazz);
            }
        }
        return req;
    }

    /**
     * Gets the CWL Workflow runtime environment
     * 
     * @return The CWL Workflow runtime environment, currently, it always
     *         returns LSF
     */
    public static RuntimeEnv getRuntimeEnv() {
        RuntimeEnv runtimeEnv = RuntimeEnv.toRuntimeEnv(System.getenv("CWL_ENGINE_RUNTIME"));
        if (runtimeEnv == null) {
            runtimeEnv = RuntimeEnv.LSF;
        }
        return runtimeEnv;
    }

    /**
     * Format a command to a readable command
     * 
     * @param commands
     *            A command with array format
     * @return A readable command
     */
    public static String asPrettyCommandStr(List<String> commands) {
        String cmd = null;
        if (commands != null) {
            cmd = String.join(" \\\n", commands);
        }
        return cmd;
    }

    /**
     * Find a HPC job id from a HPC job submission command output
     * 
     * @param regex
     *            The regular expression for HPC job submission command output
     * @param content
     *            The HPC job submission command output
     * @return The job id
     */
    public static String matchJobId(String regex, String content) {
        String jobId = null;
        if (content != null && regex != null) {
            Matcher matcher = Pattern.compile(regex).matcher(content);
            if (matcher.find()) {
                jobId = matcher.group(1);
            }
        }
        return jobId;
    }

    /**
     * Prints a given CWL process instance informations
     * 
     * @param instance
     *            A CWL process instance
     */
    public static void printCWLInstanceInfo(CWLInstance instance) {
        if (instance != null) {
            String msg = ResourceLoader.getMessage("cwl.workflow.id", instance.getId());
            logger.info(msg);
            msg = ResourceLoader.getMessage("cwl.workflow.name", instance.getName());
            logger.info(msg);
            msg = ResourceLoader.getMessage("cwl.workflow.description.file.path", instance.getProcess().getDescPath());
            logger.info(msg);
            if (instance.getProcess().getInputsPath() != null) {
                msg = ResourceLoader.getMessage("cwl.workflow.input.settings.file.path",
                        instance.getProcess().getInputsPath());
                logger.info(msg);
            }
            if (instance.getProcess().getExecConfPath() != null) {
                msg = ResourceLoader.getMessage("cwl.workflow.exec.configuration.file.path",
                        instance.getProcess().getExecConfPath());
                logger.info(msg);
            }
            Map<String, String> runtime = instance.getRuntime();
            msg = ResourceLoader.getMessage("cwl.workflow.outdir", runtime.get(CommonUtil.RUNTIME_OUTPUT_DIR));
            logger.info(msg);
            msg = ResourceLoader.getMessage("cwl.workflow.workdir", runtime.get(CommonUtil.RUNTIME_TMP_DIR));
            logger.info(msg);
            msg = ResourceLoader.getMessage("cwl.workflow.start", instance.getName());
            logger.info(msg);
        }
    }

    /**
     * Prints the tip of a given CWL scatter step instance
     * 
     * @param instance
     *            A CWL scatter step instance
     */
    public static void printScatterTip(CWLCommandInstance instance) {
        if (instance != null && instance.getScatter() != null) {
            int scatterSize = instance.getScatterHolders().size();
            if (instance.getScatterMethod() != null) {
                String scatterMethodName = instance.getScatterMethod().toString().toLowerCase();
                logger.info(ResourceLoader.getMessage("cwl.exec.scatter.job.scatter", instance.getName(), scatterSize,
                        scatterMethodName));
            } else {
                logger.info(ResourceLoader.getMessage("cwl.exec.scatter.job.scatter.without.method", instance.getName(),
                        scatterSize));
            }
        }
    }

    /**
     * Prints the message to stdout
     * 
     * @param msg
     *            A message will be printed
     */
    public static void printStdoutMsg(String msg) {
        if (msg != null) {
            System.out.println(msg);
        }
    }

    /**
     * Prints the message to stderr
     * 
     * @param msg
     *            A message will be printed
     */
    public static void printStderrMsg(String msg) {
        if (msg != null) {
            System.err.println(msg);
        }
    }

    /**
     * Prints the arguments to stdout with given output format
     * 
     * @param format
     *            An output formt
     * @param args
     *            The arguments will be printed
     */
    public static void formatPrint(String format, Object... args) {
        System.out.format(format, args);
    }

    /**
     * Validate a envvar name
     * 
     * @param envvarName
     *            A given ENVVAR name
     * @return If the name is valid, return true
     */
    public static boolean validateEnvvarName(String envvarName) {
        boolean result = false;
        if (envvarName != null && envvarName.length() > 0) {
            Matcher matcher = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*$").matcher(envvarName);
            result = matcher.find();
        }
        return result;
    }

    private static <T extends Requirement> T findRequirement(CWLInstance instance,
            Class<T> clazz,
            boolean fromHint) {
        T req = findStepReq(instance.getStep(), clazz, fromHint);
        if (req == null) {
            req = findProcessReq(instance.getProcess(), clazz, fromHint);
            if (req == null) {
                req = findParentReq(instance.getParent(), clazz, fromHint);
            }
        }
        return req;
    }

    private static <T extends Requirement> T findRequirement(WorkflowStep step,
            Class<T> clazz,
            boolean fromHint) {
        T req = null;
        if (step != null) {
            req = findStepReq(step, clazz, fromHint);
            if (req == null) {
                req = findProcessReq(step.getRun(), clazz, fromHint);
            }
        }
        return req;
    }

    private static <T extends Requirement> T findParentReq(CWLInstance parent,
            Class<T> clazz,
            boolean fromHint) {
        T req = null;
        if (parent != null) {
            req = findStepReq(parent.getStep(), clazz, fromHint);
            if (req == null) {
                req = findProcessReq(parent.getProcess(), clazz, fromHint);
                if (req == null) {
                    req = findParentReq(parent.getParent(), clazz, fromHint);
                }
            }
        }
        return req;
    }

    private static <T extends Requirement> T findStepReq(WorkflowStep step, Class<T> clazz, boolean fromHint) {
        T req = null;
        if (step != null) {
            if (!fromHint) {
                req = findReq(step.getRequirements(), clazz);
            } else {
                req = findReq(step.getHints(), clazz);
            }
        }
        return req;
    }

    private static <T extends Requirement> T findProcessReq(CWLProcess process, Class<T> clazz, boolean fromHint) {
        T req = null;
        if (process != null) {
            if (!fromHint) {
                req = findReq(process.getRequirements(), clazz);
            } else {
                req = findReq(process.getHints(), clazz);
            }
        }
        return req;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Requirement> T findReq(List<Requirement> reqs, Class<T> clazz) {
        T req = null;
        if (reqs != null) {
            for (Requirement r : reqs) {
                String reqClazz = clazz.getSimpleName();
                if (reqClazz.equals(r.getClazz())) {
                    req = (T) r;
                    break;
                }
            }
        }
        return req;
    }
}

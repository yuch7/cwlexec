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
package com.ibm.spectrumcomputing.cwl.exec.util.evaluator;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.process.CWLProcess;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.Dirent;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvVarRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.EnvironmentDef;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InitialWorkDirRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.ResourceRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.workflow.WorkflowStep;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Utility methods for evaluating the CWL InitialWorkDirRequirement,
 * EnvVarRequirement and ResourceRequirement
 */
public final class RequirementsEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(RequirementsEvaluator.class);

    private static final String RES_REQUIREMENT_FAILS_MSG = "cwl.expression.resreq.invalid";
    private static final String NO_SUCH_FILE_OR_DIRECTORY_MSG = "No such file or directory:%s";

    private RequirementsEvaluator() {
    }

    /**
     * Evaluates the InitialWorkDirRequirement of a CWL CommandLineTool instance
     * 
     * @param commandInstance
     *            A CWL CommandLineTool instance
     * @throws CWLException
     *             Failed to evaluate the InitialWorkDirRequirement
     */
    public static void evalInitialWorkDirReq(
            CWLCommandInstance commandInstance) throws CWLException {
        InitialWorkDirRequirement wdReq = CWLExecUtil.findRequirement(commandInstance,
                InitialWorkDirRequirement.class);
        if (wdReq != null) {
            InlineJavascriptRequirement jsReq = CWLExecUtil.findRequirement(commandInstance.getProcess(),
                    InlineJavascriptRequirement.class);
            evaluateInitialWorkDirRequirement(jsReq, commandInstance.getRuntime(), commandInstance.getProcess(), wdReq);
        }
    }

    /**
     * Evaluates the EnvVarRequirement of a CWL main process
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of the CWL main process
     * @param runtime
     *            The runtime of the CWL main process
     * @param processObj
     *            A CWL main process object
     * @throws CWLException
     *             Failed to evaluate the requirement
     */
    public static void evalMainEnvVarReq(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            CWLProcess processObj) throws CWLException {
        EnvVarRequirement envVarReq = CWLExecUtil.findRequirement(processObj, EnvVarRequirement.class);
        if (envVarReq != null) {
            evalEnvVarReq(jsReq, runtime, processObj, envVarReq);
        }
    }

    /**
     * Evaluates a CWL step process InitialWorkDirRequirement and
     * EnvVarRequirement
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of a CWL step process
     * @param runtime
     *            The runtime of a CWL step process
     * @param step
     *            A CWL step process object
     * @throws CWLException
     *             Failed to evaluate the requirement
     */
    public static void evalStepEnvVarReq(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            WorkflowStep step) throws CWLException {
        EnvVarRequirement envVarReq = CWLExecUtil.findRequirement(step, EnvVarRequirement.class);
        if (envVarReq != null) {
            evalEnvVarReq(jsReq, runtime, step.getRun(), envVarReq);
        }
    }

    /**
     * Evaluates the ResourceRequirement of a CWL main process
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of a CWL main process
     * @param process
     *            A CWL main process object
     * @return The evaluated ResourceRequirement of the CWL main process
     * @throws CWLException
     *             Failed to evaluate the requirement
     */
    public static ResourceRequirement evalMainResReq(InlineJavascriptRequirement jsReq,
            CWLProcess process) throws CWLException {
        ResourceRequirement resReq = CWLExecUtil.findRequirement(process, ResourceRequirement.class);
        List<? extends CWLParameter> inputs = process.getInputs();
        evalResReq(jsReq, resReq, inputs);
        return resReq;
    }

    /**
     * Evaluates the ResourceRequirement of a CWL step process
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of a CWL step process
     * @param step
     *            A CWL step process object
     * @return The evaluated ResourceRequirement of the CWL step process
     * @throws CWLException
     *             Failed to evaluate the requirement
     */
    public static ResourceRequirement evalStepResReq(InlineJavascriptRequirement jsReq,
            WorkflowStep step) throws CWLException {
        ResourceRequirement resReq = CWLExecUtil.findRequirement(step, ResourceRequirement.class);
        List<? extends CWLParameter> inputs = step.getRun().getInputs();
        evalResReq(jsReq, resReq, inputs);
        return resReq;
    }

    private static void evalResReq(InlineJavascriptRequirement jsReq,
            ResourceRequirement resReq,
            List<? extends CWLParameter> inputs) throws CWLException {
        if (resReq != null) {
            List<String> scriptLibs = new ArrayList<>();
            if (jsReq != null && jsReq.getExpressionLib() != null && !jsReq.getExpressionLib().isEmpty()) {
                scriptLibs.addAll(jsReq.getExpressionLib());
            }
            String inputsContext = JSEvaluator.toInputsContext(inputs);
            if (inputsContext.length() != 0) {
                scriptLibs.add(inputsContext);
            }
            evalCores(scriptLibs, resReq);
            evalRam(scriptLibs, resReq);
            evalTmpdir(scriptLibs, resReq);
            evalOutdir(scriptLibs, resReq);
        }
    }

    private static void evalCores(List<String> scriptLibs, ResourceRequirement resReq) throws CWLException {
        Long coresMin = evalLongNum(scriptLibs, resReq.getCoresMinExpr());
        if (coresMin != null) {
            resReq.setCoresMin(coresMin);
        }
        Long coresMax = evalLongNum(scriptLibs, resReq.getCoresMaxExpr());
        if (coresMax != null) {
            resReq.setCoresMax(coresMax);
        }
        coresMin = resReq.getCoresMin();
        coresMax = resReq.getCoresMax();
        if ((coresMin != null && coresMax != null) && (coresMin > coresMax)) {
            throw new CWLException(
                    ResourceLoader.getMessage(RES_REQUIREMENT_FAILS_MSG, "coresMin", "coresMax"),
                    253);
        }
        if (coresMin != null && coresMax == null) {
            resReq.setCoresMax(coresMin);
        }
        if (coresMin == null && coresMax != null) {
            resReq.setCoresMin(coresMax);
        }
    }

    private static void evalRam(List<String> scriptLibs, ResourceRequirement resReq) throws CWLException {
        Long ramMin = evalLongNum(scriptLibs, resReq.getRamMinExpr());
        if (ramMin != null) {
            resReq.setRamMin(ramMin);
        }
        Long ramMax = evalLongNum(scriptLibs, resReq.getRamMaxExpr());
        if (ramMax != null) {
            resReq.setRamMax(ramMax);
        }
        ramMin = resReq.getRamMin();
        ramMax = resReq.getRamMax();
        if (ramMin != null && ramMax != null && ramMin > ramMax) {
            throw new CWLException(
                    ResourceLoader.getMessage(RES_REQUIREMENT_FAILS_MSG, "ramMin", "ramMax"),
                    253);
        }
        if (ramMin != null && ramMax == null) {
            resReq.setRamMax(ramMin);
        }
        if (ramMin == null && ramMax != null) {
            resReq.setRamMin(ramMax);
        }
    }

    private static void evalTmpdir(List<String> scriptLibs, ResourceRequirement resReq) throws CWLException {
        Long tmpdirMin = evalLongNum(scriptLibs, resReq.getTmpdirMinExpr());
        if (tmpdirMin != null) {
            resReq.setTmpdirMin(tmpdirMin);
        }
        Long tmpdirMax = evalLongNum(scriptLibs, resReq.getTmpdirMaxExpr());
        if (tmpdirMax != null) {
            resReq.setTmpdirMax(tmpdirMax);
        }
        tmpdirMin = resReq.getTmpdirMin();
        tmpdirMax = resReq.getTmpdirMax();
        if (tmpdirMin == null && tmpdirMax == null) {
            resReq.setTmpdirMin(16L);
            resReq.setTmpdirMax(0L);
        }
        if (tmpdirMin != null && tmpdirMax != null && tmpdirMin > tmpdirMax) {
            throw new CWLException(
                    ResourceLoader.getMessage(RES_REQUIREMENT_FAILS_MSG, "tmpdirMin", "tmpdirMax"),
                    253);
        }
        if (tmpdirMin != null && tmpdirMax == null) {
            resReq.setTmpdirMax(tmpdirMin);
        }
        if (tmpdirMin == null && tmpdirMax != null) {
            resReq.setTmpdirMin(tmpdirMax);
        }
    }

    private static void evalOutdir(List<String> scriptLibs, ResourceRequirement resReq) throws CWLException {
        Long outdirMin = evalLongNum(scriptLibs, resReq.getOutdirMinExpr());
        if (outdirMin != null) {
            resReq.setOutdirMin(outdirMin);
        }
        Long outdirMax = evalLongNum(scriptLibs, resReq.getOutdirMaxExpr());
        if (outdirMax != null) {
            resReq.setOutdirMax(outdirMax);
        }
        outdirMin = resReq.getOutdirMin();
        outdirMax = resReq.getOutdirMax();
        if (outdirMin == null && outdirMax == null) {
            resReq.setOutdirMin(16L);
            resReq.setOutdirMax(0L);
        }
        if (outdirMin != null && outdirMax != null && outdirMin > outdirMax) {
            throw new CWLException(
                    ResourceLoader.getMessage(RES_REQUIREMENT_FAILS_MSG, "outdirMax", "outdirMax"),
                    253);
        }
        if (outdirMin != null && outdirMax == null) {
            resReq.setOutdirMax(outdirMin);
        }
        if (outdirMin == null && outdirMax != null) {
            resReq.setOutdirMin(outdirMax);
        }
    }

    private static Long evalLongNum(List<String> scriptLibs, CWLFieldValue exprPlaceholder) {
        Long longNum = null;
        if (exprPlaceholder != null) {
            String val = exprPlaceholder.getValue();
            String expr = exprPlaceholder.getExpression();
            try {
                if (val != null) {
                    longNum = Long.valueOf(val);
                } else if (expr != null) {
                    JSResultWrapper r = JSEvaluator.evaluate(scriptLibs, expr);
                    if (!r.isNull() && r.isLong()) {
                        longNum = Long.valueOf(r.asLong());
                    }
                }
            } catch (Exception e) {
                logger.debug("Fail to evalute the ResourceRequirement, {}", e.getMessage());
            }
        }
        return longNum;
    }

    private static void evaluateInitialWorkDirRequirement(InlineJavascriptRequirement jsRequirement,
            Map<String, String> runtime,
            CWLProcess processObj,
            InitialWorkDirRequirement initialWorkDirRequirement) throws CWLException {
        List<String> scriptLibs = new ArrayList<>();
        scriptLibs.addAll(JSEvaluator.constructEvalContext(jsRequirement));
        scriptLibs.add(JSEvaluator.toRuntimeContext(runtime));
        @SuppressWarnings("unchecked")
        String inputsContext = JSEvaluator.toInputsContext((List<CommandInputParameter>) processObj.getInputs());
        scriptLibs.add(inputsContext);
        // copy listing files to working directory
        copyInitialWorkDirListing(processObj.getOwner(), runtime, scriptLibs, initialWorkDirRequirement);
        // Only validates files and dirs, they also may need to be copied in
        // future
        validateFilesAndDirs(initialWorkDirRequirement);
        evalDirentListing(processObj.getOwner(), runtime, scriptLibs, initialWorkDirRequirement);
    }

    private static void copyInitialWorkDirListing(String owner,
            Map<String, String> runtime,
            List<String> scriptLibs,
            InitialWorkDirRequirement initialWorkDirReq) throws CWLException {
        CWLFieldValue listingExpr = initialWorkDirReq.getListing();
        if (listingExpr != null && listingExpr.getExpression() != null) {
            copySingleInitialWorkDirListing(owner, runtime, scriptLibs, listingExpr.getExpression());
        }
        List<CWLFieldValue> listingExprs = initialWorkDirReq.getExprListing();
        if (listingExprs != null) {
            for (CWLFieldValue expr : listingExprs) {
                if (expr.getExpression() != null) {
                    copySingleInitialWorkDirListing(owner, runtime, scriptLibs, expr.getExpression());
                }
            }
        }
    }

    private static void copySingleInitialWorkDirListing(String owner,
            Map<String, String> runtime,
            List<String> scriptLibs,
            String expr) throws CWLException {
        JSResultWrapper r = JSEvaluator.evaluate(scriptLibs, expr);
        if (!r.isNull()) {
            copyInitialWorkDirListingPath(owner, runtime, expr, r);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.expression.InitialWorkDirReq.listing.invalid", expr),
                    253);
        }
    }

    private static void copyInitialWorkDirListingPath(String owner,
            Map<String, String> runtime,
            String expr,
            JSResultWrapper r) throws CWLException {
        if (r.isArray()) {
            for (JSResultWrapper e : r.elements()) {
                copyInitialWorkDirListingPath(owner, runtime, expr, e);
            }
        } else if (r.isCWLFile()) {
            String path = r.asCWLFile().getPath();
            if (!Paths.get(path).toFile().exists()) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.expression.InitialWorkDirReq.listing.path.invalid",
                                expr,
                                path),
                        253);
            }
            Path targetPath = Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR), Paths.get(path).getFileName().toString());
            logger.debug("[InitialWorkDirListing] copy file \"{}\" to \"{}\"", path, targetPath);
            IOUtil.copy(owner, Paths.get(path), targetPath);
        } else if (r.isCWLDirectory()) {
            String path = r.asCWLDirectory().getPath();
            if (!Paths.get(path).toFile().exists()) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.expression.InitialWorkDirReq.listing.path.invalid",
                                expr,
                                path),
                        253);
            }
            Path targetPath = Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR), Paths.get(path).getFileName().toString());
            logger.debug("[InitialWorkDirListing] copy dir \"{}\" to \"{}\"", path, targetPath);
            IOUtil.copy(owner, Paths.get(path), targetPath);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.expression.InitialWorkDirReq.listing.type.invalid", expr),
                    253);
        }
    }

    private static void validateFilesAndDirs(InitialWorkDirRequirement initialWorkDirReq) throws CWLException {
        List<CWLFileBase> files = initialWorkDirReq.getFileListing();
        List<CWLFileBase> dirs = initialWorkDirReq.getDirListing();
        if (files != null) {
            for (CWLFileBase file : files) {
                if (!Paths.get(file.getPath()).toFile().exists()) {
                    throw new CWLException(String.format(NO_SUCH_FILE_OR_DIRECTORY_MSG, file.getPath()), 253);
                }
            }
        }
        if (dirs != null) {
            for (CWLFileBase dir : dirs) {
                if (!Paths.get(dir.getPath()).toFile().exists()) {
                    throw new CWLException(String.format(NO_SUCH_FILE_OR_DIRECTORY_MSG, dir.getPath()), 253);
                }
            }
        }
    }

    private static void evalDirentListing(String owner,
            Map<String, String> runtime,
            List<String> scriptLibs,
            InitialWorkDirRequirement initialWorkDirReq) throws CWLException {
        List<Dirent> dirents = initialWorkDirReq.getDirentListing();
        if (dirents != null) {
            for (Dirent dirent : dirents) {
                evalDirent(owner, runtime, scriptLibs, dirent);
            }
        }
    }

    private static void evalDirent(String owner,
            Map<String, String> runtime,
            List<String> scriptLibs,
            Dirent dirent) throws CWLException {
        if (dirent.getEntry() == null) {
            return;
        }
        String entryname = evalDirentEntryName(scriptLibs, dirent);
        String entry = null;
        if (dirent.getEntry().getExpression() != null) {
            JSResultWrapper r = JSEvaluator.evaluate(scriptLibs, dirent.getEntry().getExpression());
            if (!r.isNull()) {
                if (r.isString()) {
                    entry = r.asString();
                    dirent.getEntry().setValue(entry);
                } else if (r.isObject()) {
                    for (String key : r.keys()) {
                        copyDirentEntryObject(owner, runtime, entryname, key, r);
                    }
                }
            }
        } else if (dirent.getEntry().getValue() != null) {
            entry = evalDirentEntry(scriptLibs, dirent);
            dirent.getEntry().setValue(entry);
        }
        if (entryname != null && entry != null) {
            // create new file
            IOUtil.write(new File(runtime.get(CommonUtil.RUNTIME_TMP_DIR) + File.separator + entryname),
                    entry);
        }
    }

    private static void copyDirentEntryObject(String owner,
            Map<String, String> runtime,
            String entryname,
            String key,
            JSResultWrapper r) throws CWLException {
        if ("path".equals(key)) {
            Path sourcePath = Paths.get(r.getValue(key).asString());
            if (entryname != null) {
                // copy and rename file or directory
                Path targetPath = Paths
                        .get(runtime.get(CommonUtil.RUNTIME_TMP_DIR) + File.separator + entryname);
                IOUtil.copy(owner, sourcePath, targetPath);
            } else {
                // copy file or directory
                Path targetPath = Paths
                        .get(runtime.get(CommonUtil.RUNTIME_TMP_DIR) + File.separator
                                + sourcePath.getFileName());
                IOUtil.copy(owner, sourcePath, targetPath);
            }
        }
        if ("class".equals(key) && "Directory".equals(r.getValue(key).asString())
                && entryname != null) {
            // create new directory
            IOUtil.mkdirs(owner,
                    Paths.get(runtime.get(CommonUtil.RUNTIME_TMP_DIR) + File.separator + entryname));
        }
    }

    private static String evalDirentEntryName(List<String> scriptLibs, Dirent dirent) throws CWLException {
        String entryname = null;
        if (dirent.getEntryname() != null) {
            if (dirent.getEntryname().getExpression() != null) {
                JSResultWrapper r = JSEvaluator.evaluate(scriptLibs, dirent.getEntryname().getExpression());
                if (!r.isNull()) {
                    if (r.isString()) {
                        entryname = r.asString();
                    } else {
                        throw new CWLException(
                                ResourceLoader.getMessage("cwl.expression.InitialWorkDirReq.entry.name.invalid",
                                        dirent.getEntryname().getExpression()),
                                253);
                    }
                }
            } else if (dirent.getEntryname().getValue() != null) {
                entryname = dirent.getEntryname().getValue();
            }
            dirent.getEntryname().setValue(entryname);
        }
        return entryname;
    }

    private static String evalDirentEntry(List<String> scriptLibs, Dirent dirent) throws CWLException {
        String entry = dirent.getEntry().getValue();
        StringBuilder sb = new StringBuilder();
        String[] entries = entry.split(System.getProperty("line.separator"));
        for (String item : entries) {
            sb.append(evalDirentEntry(scriptLibs, item) + System.getProperty("line.separator"));
        }
        logger.debug("The entry of IninitalWorkDir:\n{}", sb);
        entry = sb.toString();
        Pattern pattern = Pattern.compile("\\$" + buildBracePattern(10));
        Matcher matcher = pattern.matcher(entry);
        while (matcher.find()) {
            String value = null;
            String expr = matcher.group();
            JSResultWrapper r = JSEvaluator.evaluate(scriptLibs, expr);
            if (!r.isNull()) {
                if (r.isBool()) {
                    value = String.valueOf(r.asBool());
                } else if (r.isString()) {
                    value = r.asString();
                } else if (r.isDouble()) {
                    value = String.valueOf(r.asDouble());
                } else if (r.isLong()) {
                    value = String.valueOf(r.asLong());
                } else {
                    throw new CWLException("The expression must return a bool, string or number",
                            253);
                }
            }
            if (value != null) {
                entry = entry.replace(matcher.group(), value);
            }
        }
        logger.debug("After the entry of IninitalWorkDir:\n{}", entry);
        return entry;
    }

    private static String buildBracePattern(int times) {
        String base = "\\{[^\\{\\}]*(\\{[^\\{\\}]*\\}[^\\{\\}]*)*\\}";
        for (int i = 1; i<=times; i++) {
            base = String.format("\\{[^\\{\\}]*(%s[^\\{\\}]*)*\\}", base);
        }
        return base;
    }

    private static String evalDirentEntry(List<String> scriptLibs, String entry) throws CWLException {
        Pattern pattern = Pattern.compile("\\$\\([^\\(\\)]*(\\(.*?\\)[^\\(\\)]*)*\\)\\s*[, ]*");
        Matcher matcher = pattern.matcher(entry);
        String express = null;
        JSResultWrapper r = null;
        String value = null;
        while (matcher.find()) {
            express = matcher.group().trim();
            r = JSEvaluator.evaluate(scriptLibs, express);
            if (!r.isNull()) {
                if (r.isBool()) {
                    value = String.valueOf(r.asBool());
                } else if (r.isString()) {
                    value = r.asString();
                } else if (r.isDouble()) {
                    value = String.valueOf(r.asDouble());
                } else if (r.isLong()) {
                    value = String.valueOf(r.asLong());
                } else {
                    throw new CWLException("The expression must return a bool, string or number",
                            253);
                }
            }
            entry = entry.replace(matcher.group(), value);
        }
        return entry;
    }

    private static void evalEnvVarReq(InlineJavascriptRequirement jsRequirement,
            Map<String, String> runtime,
            CWLProcess processObj,
            EnvVarRequirement envVarRequirement) throws CWLException {
        for (EnvironmentDef envDef : envVarRequirement.getEnvDef()) {
            String expression = envDef.getEnvValue().getExpression();
            if (expression != null) {
                List<String> scriptLibs = new ArrayList<>();
                if (jsRequirement != null && jsRequirement.getExpressionLib() != null
                        && !jsRequirement.getExpressionLib().isEmpty()) {
                    scriptLibs.addAll(jsRequirement.getExpressionLib());
                }
                String runtimeContext = JSEvaluator.toRuntimeContext(runtime);
                if (runtimeContext != null) {
                    scriptLibs.add(runtimeContext);
                }
                @SuppressWarnings("unchecked")
                String inputsContext = JSEvaluator.toInputsContext(
                        (List<CommandInputParameter>) processObj.getInputs());
                if (inputsContext.length() != 0) {
                    scriptLibs.add(inputsContext);
                }
                envDef.getEnvValue().setValue(toEnvVarReqValue(scriptLibs, expression));
            }
        }
    }

    private static String toEnvVarReqValue(List<String> scriptLibs, String expression) throws CWLException {
        String result = null;
        JSResultWrapper r = JSEvaluator.evaluate(scriptLibs, expression);
        if (!r.isNull()) {
            if (r.isBool()) {
                result = String.valueOf(r.asBool());
            } else if (r.isString()) {
                result = r.asString();
            } else if (r.isDouble()) {
                result = String.valueOf(r.asDouble());
            } else if (r.isLong()) {
                result = String.valueOf(r.asLong());
            } else {
                throw new CWLException("cwl.expression.envvarreq.invalid", 253);
            }
        }
        return result;
    }
}

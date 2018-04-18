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
package com.ibm.spectrumcomputing.cwl.exec.util.evaluator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.FileFormat;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;

/**
 * Base class for {@link CommandOutputsEvaluator} and {@link InputsEvaluator}
 */
class CommandEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(CommandEvaluator.class);

    protected CommandEvaluator() {}

    protected static void evalFormat(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<? extends CWLParameter> inputs,
            FileFormat format) throws CWLException {
        if (format != null && format.getFormat() != null) {
            String expr = format.getFormat().getExpression();
            if (expr != null) {
                List<String> context = JSEvaluator.constructEvalContext(jsReq);
                context.add(JSEvaluator.toRuntimeContext(runtime));
                context.add(JSEvaluator.toInputsContext(inputs));
                JSResultWrapper r = JSEvaluator.evaluate(context, expr);
                if (!r.isNull()) {
                    if (r.isString()) {
                        format.getFormat().setExpression(r.asString());
                    } else {
                        logger.debug("The format expression ({}) does not return a string, ignore it", expr);
                    }
                }
            }
        }
    }

    protected static List<Path> toSecondaryPaths(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<? extends CWLParameter> inputs,
            CWLFile cwlFile,
            List<CWLFieldValue> secondaryFiles) throws CWLException {
        List<Path> newSecondaryFiles = new ArrayList<>();
        if (secondaryFiles != null) {
            for (CWLFieldValue secondaryFileExpr : secondaryFiles) {
                if (secondaryFileExpr.getExpression() != null) {
                    newSecondaryFiles.addAll(
                            evalSecondaryFileExpr(jsReq, runtime, inputs, cwlFile, secondaryFileExpr.getExpression()));
                } else if (secondaryFileExpr.getValue() != null) {
                    String suffix = secondaryFileExpr.getValue();
                    if (suffix.startsWith("^")) {
                        Path secondaryPath = Paths.get(IOUtil.removeFileExt(cwlFile.getPath(), suffix));
                        newSecondaryFiles.add(secondaryPath);
                    } else {
                        Path secondaryPath = Paths.get(cwlFile.getPath() + suffix);
                        newSecondaryFiles.add(secondaryPath);
                    }
                }
            }
        }
        return newSecondaryFiles;
    }

    protected static void resetSecondaryFiles(CWLFile cwlFile, List<Path> secondaryPaths) {
        List<CWLFileBase> secondaryFiles = cwlFile.getSecondaryFiles();
        if (secondaryFiles == null) {
            secondaryFiles = new ArrayList<>();
        }
        logger.debug("Secondary file parent path: {}, {}, src={}", cwlFile.getPath(), secondaryFiles.size(),
                cwlFile.getSrcPath());
        for (Path sp : secondaryPaths) {
            Path secondaryFilePath = Paths.get(sp.toString());
            if (!secondaryFilePath.toFile().exists()) {
                logger.debug("Seconary file: \"{}\"", secondaryFilePath);
                secondaryFilePath = Paths.get(cwlFile.getPath()).getParent().resolve(secondaryFilePath);
                if (!secondaryFilePath.toFile().exists() && cwlFile.getSrcPath() != null) {
                    // try to find the secondary file path from primary file
                    // path
                    secondaryFilePath = Paths.get(cwlFile.getSrcPath()).getParent()
                            .resolve(secondaryFilePath.getFileName());
                }
            }
            if (!hasSecondaryFile(secondaryFiles, secondaryFilePath)) {
                if (secondaryFilePath.toFile().isDirectory()) {
                    logger.debug("Add secondary dir: \"{}\"", secondaryFilePath);
                    CWLDirectory cwlDir = IOUtil.toCWLDirectory(secondaryFilePath);
                    IOUtil.traverseDirListing(secondaryFilePath.toString(), cwlDir.getListing(), true);
                    secondaryFiles.add(cwlDir);
                } else {
                    logger.debug("Add secondary file: \"{}\"", secondaryFilePath);
                    secondaryFiles.add(IOUtil.toCWLFile(secondaryFilePath, true));
                }
            }
        }
        cwlFile.setSecondaryFiles(secondaryFiles);
    } 

    private static List<Path> evalSecondaryFileExpr(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<? extends CWLParameter> inputs,
            CWLFile cwlFile,
            String expr) throws CWLException {
        List<Path> paths = new ArrayList<>();
        List<String> context = JSEvaluator.constructEvalContext(jsReq);
        context.add(JSEvaluator.toRuntimeContext(runtime));
        context.add(JSEvaluator.toInputsContext(inputs));
        context.add(JSEvaluator.toSelfContext(cwlFile));
        eval(paths, context, expr);
        logger.debug("Evaluate the secondary file express \"{}\" to {}", expr, paths);
        return paths;
    }

    private static void eval(List<Path> paths, List<String> context, String expr) throws CWLException {
        if (expr.startsWith("${")) {
            JSResultWrapper r = JSEvaluator.evaluate(context, expr);
            if (!r.isNull()) {
                resovleToPaths(paths, r);
            }
        } else {
            Pattern pattern = Pattern.compile("\\$\\([^\\(\\)]*(\\(.*?\\)[^\\(\\)]*)*\\)\\s*[, ]*");
            Matcher matcher = pattern.matcher(expr);
            String value = expr;
            while (matcher.find()) {
                String jsExpr = matcher.group().trim();
                JSResultWrapper r = JSEvaluator.evaluate(context, jsExpr);
                value = value.replace(matcher.group(), toFilePathValue(r));
            }
            paths.add(Paths.get(value));
        }
    }

    private static void resovleToPaths(List<Path> paths, JSResultWrapper r) {
        if (r.isString()) {
            paths.add(Paths.get(r.asString()));
        } else if (r.isArray()) {
            for (JSResultWrapper e : r.elements()) {
                resovleToPaths(paths, e);
            }
        } else if (r.isObject()) {
            for (String key : r.keys()) {
                if ("path".equals(key)) {
                    paths.add(Paths.get(r.getValue(key).asString()));
                    break;
                }
            }
        }
    }

    private static String toFilePathValue(JSResultWrapper r) {
        String value = null;
        if (!r.isNull()) {
            if (r.isString()) {
                value = r.asString();
            } else if (r.isObject()) {
                for (String key : r.keys()) {
                    if ("path".equals(key)) {
                        value = r.getValue(key).asString();
                        break;
                    }
                }
            }
        }
        return value;
    }

    private static boolean hasSecondaryFile(List<CWLFileBase> secondaryFiles, Path secondaryFilePath) {
        boolean has = false;
        for (CWLFileBase cwlFile : secondaryFiles) {
            if (cwlFile.getPath().equals(secondaryFilePath.toString())) {
                has = true;
                break;
            }
        }
        return has;
    }
}

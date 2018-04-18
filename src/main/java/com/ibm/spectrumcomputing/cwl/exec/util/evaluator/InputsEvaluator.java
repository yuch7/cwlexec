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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.FileType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;

/**
 * Utility methods for evaluating the CWLInputParameter secondaryFiles and format expression
 *
 */
public final class InputsEvaluator extends CommandEvaluator {

    private InputsEvaluator() {
    }

    /**
     * Evaluates CWLInputParameter secondrayFiles and format expression
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of the CWLInputParameter objects
     * @param runtime
     *            The runtime of the CWLInputParameter objects
     * @param inputs
     *            The list of the CWLInputParameter objects
     * @throws CWLException
     *             Failed to evaluate the expression
     */
    public static void eval(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<? extends CWLParameter> inputs) throws CWLException {
        if (inputs != null) {
            for (CWLParameter input : inputs) {
                evalSecondaryFiles(jsReq, runtime, inputs, input);
                evalFormat(jsReq, runtime, inputs, input.getFormat());
            }
        }
    }

    private static void evalSecondaryFiles(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<? extends CWLParameter> inputs,
            CWLParameter input) throws CWLException {
        CWLType type = input.getType().getType();
        Object value = input.getValue();
        if (value == null) {
            value = input.getDefaultValue();
        }
        if (type instanceof FileType) {
            evalCWLFile(jsReq, runtime, inputs, input, value);
        } else if (type instanceof InputArrayType) {
            CWLType items = ((InputArrayType) type).getItems().getType();
            if (items instanceof FileType) {
                evalCWLFileArray(jsReq, runtime, inputs, input, value);
            }
        }
    }

    private static void evalCWLFile(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<? extends CWLParameter> inputs,
            CWLParameter input,
            Object value) throws CWLException {
        if (value instanceof CWLFile) {
            CWLFile cwlFile = (CWLFile) value;
            List<Path> secondaryFilePaths = toSecondaryPaths(jsReq,
                    runtime,
                    inputs,
                    cwlFile,
                    input.getSecondaryFiles());
            resetSecondaryFiles(cwlFile, secondaryFilePaths);
        } else if (value instanceof ArrayList) {
            @SuppressWarnings("unchecked")
            List<CWLFile> cwlFiles = (List<CWLFile>) value;
            List<CWLFieldValue> secondaryFiles = input.getSecondaryFiles();
            for (CWLFile cwlFile : cwlFiles) {
                resetSecondaryFiles(cwlFile, toSecondaryPaths(jsReq,
                        runtime,
                        inputs,
                        (CWLFile) cwlFile,
                        secondaryFiles));
            }
        }
    }

    private static void evalCWLFileArray(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<? extends CWLParameter> inputs,
            CWLParameter input,
            Object value) throws CWLException {
        List<?> cwlFiles = (List<?>) value;
        List<CWLFieldValue> secondaryFiles = input.getSecondaryFiles();
        for (Object cwlFile : cwlFiles) {
            if (cwlFile instanceof CWLFile) {
                resetSecondaryFiles((CWLFile) cwlFile, toSecondaryPaths(jsReq,
                        runtime,
                        inputs,
                        (CWLFile) cwlFile,
                        secondaryFiles));
            } else if (cwlFile instanceof List<?>) {
                for (Object file : (List<?>) cwlFile) {
                    if (file instanceof CWLFile) {
                        resetSecondaryFiles((CWLFile) file, toSecondaryPaths(jsReq,
                                runtime,
                                inputs,
                                (CWLFile) file,
                                secondaryFiles));
                    }
                }
            }
        }
    }
}

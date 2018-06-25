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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.FileType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;

/**
 * Utility methods for evaluating CommandOutputParameter secondrayFiles and format
 * expression
 */
public final class CommandOutputsEvaluator extends CommandEvaluator {

    /**
     * Evaluates CommandOutputParameter secondrayFiles and format expression
     * 
     * @param jsReq
     *            The InlineJavascriptRequirement of the CommandOutputParameter
     * @param runtime
     *            The runtime of the CommandOutputParameter
     * @param inputs
     *            The inputs of the CommandOutputParameter
     * @param output
     *            A given CommandOutputParameter object
     * @throws CWLException
     *             Failed to evaluate the expression
     */
    public static void eval(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<CommandInputParameter> inputs,
            CommandOutputParameter output) throws CWLException {
        if (output != null) {
            evalSecondaryFiles(jsReq, runtime, inputs, output);
            evalFormat(jsReq, runtime, inputs, output.getFormat());
        }
    }

    private static void evalSecondaryFiles(InlineJavascriptRequirement jsReq,
            Map<String, String> runtime,
            List<CommandInputParameter> inputs,
            CommandOutputParameter output) throws CWLException {
        CWLType type = output.getType().getType();
        Object value = output.getValue();
        if (type instanceof FileType) {
            if (value instanceof CWLFile) {
                CWLFile cwlFile = (CWLFile) value;
                List<Path> secondaryFilePaths = toSecondaryPaths(jsReq,
                        runtime,
                        inputs,
                        cwlFile,
                        output.getSecondaryFiles());
                resetSecondaryFiles(cwlFile, secondaryFilePaths);
            } else { // for scatter, the value may be a file list
                @SuppressWarnings("unchecked")
                List<CWLFile> cwlFiles = (List<CWLFile>) value;
                List<CWLFieldValue> secondaryFiles = output.getSecondaryFiles();
                for (CWLFile cwlFile : cwlFiles) {
                    resetSecondaryFiles(cwlFile, toSecondaryPaths(jsReq,
                            runtime,
                            inputs,
                            (CWLFile) cwlFile,
                            secondaryFiles));
                }
            }
        } else if (type instanceof InputArrayType) {
            CWLType items = ((InputArrayType) type).getItems().getType();
            if (items instanceof FileType) {
                @SuppressWarnings("unchecked")
                List<CWLFile> cwlFiles = (List<CWLFile>) value;
                List<CWLFieldValue> secondaryFiles = output.getSecondaryFiles();
                for (CWLFile cwlFile : cwlFiles) {
                    resetSecondaryFiles(cwlFile, toSecondaryPaths(jsReq,
                            runtime,
                            inputs,
                            (CWLFile) cwlFile,
                            secondaryFiles));
                }
            }
        }
    }
}

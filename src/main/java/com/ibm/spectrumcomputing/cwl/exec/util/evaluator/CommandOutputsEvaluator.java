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

import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.input.CommandInputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.output.CommandOutputParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.FileType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.InlineJavascriptRequirement;
import com.ibm.spectrumcomputing.cwl.model.process.tool.ExpressionTool;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;

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
    
    /**
     * Evaluates expression of ExpressionTool
     * @param instance
     * 				A CWL process ExpressionTool instance
     * @param inputs
     * 				The inputs of the CommandInputParameter
     * @param outputType
     * 				A given CommandOutputParameter type
     * @param outputId
     * 				The output id of the CommandOutputParameter
     * @return The evaluated expression object
     * @throws CWLException
     * 				Failed to evaluate the expression
     */
    public static Object evalExpression(CWLCommandInstance instance, List<CommandInputParameter> inputs, CWLType outputType, String outputId) throws CWLException {
		String expression = ((ExpressionTool) instance.getProcess()).getExpression();
		Path tmpDir = Paths.get(instance.getRuntime().get(CommonUtil.RUNTIME_TMP_DIR));
		List<String> scriptLibs = new ArrayList<>();
		scriptLibs.add(JSEvaluator.toInputsContext((List<CommandInputParameter>) inputs));
		JSResultWrapper r = JSEvaluator.evaluate(scriptLibs, expression);
		return evalExpressionValue(r, instance.getOwner(), inputs, outputType, outputId, tmpDir, false);
    }

    private static Object evalExpressionValue(JSResultWrapper r, String owner, List<CommandInputParameter> inputs, CWLType outputType, String outputId, Path tmpDir, boolean inArray) throws CWLException {

    	if (outputType.getSymbol() == CWLTypeSymbol.DIRECTORY) {
			CWLDirectory dir = inArray?r.asCWLDirectory():r.getValue(outputId).asCWLDirectory();
			return evalExpressionDirectory(owner, inputs, tmpDir, dir);
		} else if (outputType.getSymbol() == CWLTypeSymbol.INT || outputType.getSymbol() == CWLTypeSymbol.LONG) {
			return inArray?r.asLong():r.getValue(outputId).asLong();
		} else if (outputType.getSymbol() == CWLTypeSymbol.FILE) {
			CWLFile file = inArray?r.asCWLFile():r.getValue(outputId).asCWLFile();
			evalExpressionFile(tmpDir, file);
			return file;
		} else if (outputType.getSymbol() == CWLTypeSymbol.DOUBLE || outputType.getSymbol() == CWLTypeSymbol.FLOAT) {
			return inArray?r.asDouble():r.getValue(outputId).asDouble();
		} else if (outputType.getSymbol() == CWLTypeSymbol.STRING || outputType.getSymbol() == CWLTypeSymbol.ENUM) {
			return inArray?r.asString():r.getValue(outputId).asString();
		} else if(outputType.getSymbol() == CWLTypeSymbol.BOOLEAN) {
			return inArray?r.asBool():r.getValue(outputId).asBool();
		} else if(outputType.getSymbol() == CWLTypeSymbol.ANY) {
			return inArray?r.getValue():r.getValue(outputId).getValue();
		} else if(outputType.getSymbol() == CWLTypeSymbol.ARRAY) {
			 List<Object> values = new ArrayList<>();
	         CWLType items = ((OutputArrayType) outputType).getItems().getType();
	         for (JSResultWrapper e : r.elements()) {
	        	 inArray = true;
	        	 if(e.getValue(outputId)!=null && !e.getValue(outputId).isNull() && e.getValue(outputId).isArray()) {
	        		 for(JSResultWrapper sube: e.getValue(outputId).elements()) {
	        			 values.add(evalExpressionValue(sube, owner, inputs, items, outputId, tmpDir, inArray));	 
	        		 }
	        	 } else if(!e.isNull() && e.isArray()){
	        		 for(JSResultWrapper sube: e.elements()) {
	        			 values.add(evalExpressionValue(sube, owner, inputs, items, outputId, tmpDir, inArray));	 
	        		 }
	        	 } else {
		        	 values.add(evalExpressionValue(e, owner, inputs, items, outputId, tmpDir, inArray));
	        	 }
	         }
	         return values;
		}
		return null;
    }
    
	private static void evalExpressionFile(Path tmpDir, CWLFile file) throws CWLException {
		file.setPath(tmpDir + File.separator + file.getBasename());
		if (file.getContents() != null) {
			IOUtil.write64Kib(new File(file.getPath()), file.getContents());
		}
	}

	private static CWLDirectory evalExpressionDirectory(String owner, List<CommandInputParameter> inputs, Path tmpDir,
			CWLDirectory dir) throws CWLException {
		String path = tmpDir + File.separator + dir.getBasename();
		IOUtil.mkdirs(owner, Paths.get(path));
		CWLDirectory directory = IOUtil.toCWLDirectory(Paths.get(path));
		for (CommandInputParameter input : inputs) {
			evalDirectory(owner,path,input.getValue(),directory);
		}
		return directory;
	}
	
	private static void evalDirectory(String owner, String path, Object inputObj, CWLDirectory directory) throws CWLException {
		if (inputObj instanceof List) {
			@SuppressWarnings("unchecked")
			List<CWLFileBase> list = (List<CWLFileBase>) inputObj;
			for (int i=0; i<list.size();i++) {
				Object obj = list.get(i);
				if (obj instanceof CWLFile) {
					CWLFile file = (CWLFile) (obj);
					IOUtil.copy(owner, Paths.get(file.getPath()), Paths.get(path));
					CWLFile newFile = IOUtil.toCWLFile(Paths.get(path + File.separator + file.getBasename()), false);
					list.set(i, newFile);
				} else if(obj instanceof CWLDirectory) {
					evalDirectory(owner, path, obj, directory);
				}
			}
			directory.setListing(list);
		} else if (inputObj instanceof CWLFile) {
			CWLFile file = (CWLFile) inputObj;
			IOUtil.copy(owner, Paths.get(file.getPath()), Paths.get(path));
		} else if (inputObj instanceof CWLDirectory) {
			CWLDirectory inputDir = (CWLDirectory) inputObj;
			if (!inputDir.getListing().isEmpty()) {
				evalDirectory(owner, path, inputDir.getListing(), directory);
			}
		}
	}
}

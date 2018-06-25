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
package com.ibm.spectrumcomputing.cwl.parser;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.spectrumcomputing.cwl.model.CWLFieldValue;
import com.ibm.spectrumcomputing.cwl.model.conf.PostFailureScript;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.CWLVersion;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.FileFormat;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.ParameterType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandLineBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.CommandOutputBinding;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.binding.OutputBindingGlob;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.AnyType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.BooleanType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.DirectoryType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.DoubleType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.FileType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.FloatType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.IntType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.LongType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.StringType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputEnumType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputEnumType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.output.OutputRecordType;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * Base class for CommandLineToolParser, WorkflowParser, InputSettingsParser
 * and RequirementParser
 */
class BaseParser {

    // messages key
    public static final String CWL_PARSER_FIELD_REQUIRED = "cwl.parser.field.required";
    public static final String CWL_PARSER_INVALID_FIELD = "cwl.parser.invalid.field";
    public static final String CWL_PARSER_INVALID_TYPE = "cwl.parser.invalid.type";
    private static final String ARRAY_RECORD_OR_ENUM = "array, record or enum";
    private static final String STRING_OR_ARRAY = "string or array";

    // field names
    public static final String CLASS = "class";
    protected static final String CWL_VERSION = "cwlVersion";
    protected static final String LABEL = "label";
    protected static final String INPUT_BINDING = "inputBinding";
    protected static final String OUTPUT_BINDING = "outputBinding";
    protected static final String INPUTS = "inputs";
    protected static final String REQUIREMENTS = "requirements";
    protected static final String IMPORT = "$import";
    private static final String SYMBOLS = "symbols";

    protected BaseParser() {
    }

    protected static Map<String, CWLType> schemaRefTypes = new HashMap<>();

    protected static boolean hasFileType(ParameterType parameterType, boolean isInput) {
        boolean hasFile = false;
        if (parameterType != null) {
            CWLType type = parameterType.getType();
            List<CWLType> types = parameterType.getTypes();
            if (type != null) {
                hasFile = hasFileType(type, isInput);
            } else if (types != null) {
                for (CWLType t : types) {
                    if (hasFileType(t, isInput)) {
                        hasFile = true;
                        break;
                    }
                }
            }
        }
        return hasFile;
    }

    protected static boolean hasNullType(ParameterType parameterType) {
        if (parameterType != null) {
            CWLType type = parameterType.getType();
            List<CWLType> types = parameterType.getTypes();
            if (type != null) {
                if (type.getSymbol() == CWLTypeSymbol.NULL) {
                    return true;
                }
            } else if (types != null) {
                for (CWLType t : types) {
                    if (t.getSymbol() == CWLTypeSymbol.NULL) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected static String processStringField(String key, JsonNode stringNode) throws CWLException {
        String str = null;
        if (stringNode != null) {
            if (stringNode.isTextual()) {
                str = stringNode.asText();
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, CWLTypeSymbol.STRING),
                        251);
            }
        }
        return str;
    }

    protected static List<String> processStringArrayField(String key, JsonNode arrayNode) throws CWLException {
        List<String> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            array.add(processStringField(key, elements.next()));
        }
        return array;
    }

    protected static List<String> processStringOrStringArrayField(String key, JsonNode node) throws CWLException {
        List<String> strs = new ArrayList<>();
        if (node.isTextual()) { // string
            strs.add(node.asText());
        } else if (node.isArray()) {// string array
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                JsonNode element = elements.next();
                if (element.isTextual()) {
                    strs.add(element.asText());
                } else {
                    throw new CWLException(
                            ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, CWLTypeSymbol.STRING),
                            251);
                }
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, STRING_OR_ARRAY),
                    251);
        }
        return strs;
    }

    protected static Integer processIntegerField(String key, JsonNode integerNode) throws CWLException {
        Integer integer = null;
        if (integerNode != null) {
            if (integerNode.isInt()) {
                integer = Integer.valueOf(integerNode.asInt());
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, CWLTypeSymbol.INT),
                        251);
            }
        }
        return integer;
    }

    protected static List<Integer> processIntegerArrayField(String key, JsonNode arrayNode) throws CWLException {
        List<Integer> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            array.add(processIntegerField(key, elements.next()));
        }
        return array;
    }

    protected static Long processLongField(String key, JsonNode longNode) throws CWLException {
        Long longVal = null;
        if (longNode != null) {
            if (longNode.isLong()) {
                longVal = Long.valueOf(longNode.asLong());
            } else if (longNode.isInt()) {
                longVal = Long.valueOf(longNode.asInt());
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, "long"), 251);
            }
        }
        return longVal;
    }

    protected static List<Long> processLongArrayField(String key, JsonNode arrayNode) throws CWLException {
        List<Long> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            array.add(processLongField(key, elements.next()));
        }
        return array;
    }

    protected static Float processFloatField(String key, JsonNode floatNode) throws CWLException {
        Float floatVal = null;
        if (floatNode != null) {
            if (floatNode.isDouble()) {
                floatVal = Float.valueOf(floatNode.floatValue());
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, "float"), 251);
            }
        }
        return floatVal;
    }

    protected static List<Float> processFloatArrayField(String key, JsonNode arrayNode) throws CWLException {
        List<Float> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            array.add(processFloatField(key, elements.next()));
        }
        return array;
    }

    protected static Double processDoubleField(String key, JsonNode doubleNode) throws CWLException {
        Double doubleVal = null;
        if (doubleNode != null) {
            if (doubleNode.isDouble()) {
                doubleVal = Double.valueOf(doubleNode.asDouble());
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, "double"),
                        251);
            }
        }
        return doubleVal;
    }

    protected static List<Double> processDoubleArrayField(String key, JsonNode arrayNode) throws CWLException {
        List<Double> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            array.add(processDoubleField(key, elements.next()));
        }
        return array;
    }

    protected static Boolean processBooleanField(String key, JsonNode booleanNode) throws CWLException {
        Boolean booleanVal = null;
        if (booleanNode != null) {
            if (booleanNode.isBoolean()) {
                booleanVal = Boolean.valueOf(booleanNode.asBoolean());
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, CWLTypeSymbol.BOOLEAN),
                        251);
            }
        }
        return booleanVal;
    }

    protected static List<Boolean> processBooleanArrayField(String key, JsonNode arrayNode) throws CWLException {
        List<Boolean> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            array.add(processBooleanField(key, elements.next()));
        }
        return array;
    }

    protected static Object processAnyFiled(JsonNode anyNode) {
        Object value = null;
        if (anyNode != null) {
            value = anyNode;
        }
        return value;
    }

    protected static List<Object> processAnyArrayField(JsonNode arrayNode) {
        List<Object> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            array.add(processAnyFiled(elements.next()));
        }
        return array;
    }

    protected static CWLFieldValue processExpressionField(String key, JsonNode exprNode) throws CWLException {
        CWLFieldValue expr = null;
        if (exprNode != null) {
            if (exprNode.isTextual()) {
                String text = exprNode.asText();
                expr = new CWLFieldValue();
                if (text.startsWith("$") || hasExpr(text)) {
                    expr.setExpression(text);
                } else {
                    expr.setValue(text);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, "string or expression"),
                        251);
            }
        }
        return expr;
    }

    protected static CWLFieldValue processWorkDirEntryField(String key, JsonNode exprNode) throws CWLException {
        CWLFieldValue expr = null;
        if (exprNode != null) {
            if (exprNode.isTextual()) {
                String text = exprNode.asText();
                expr = new CWLFieldValue();
                if (text.startsWith("$")) {
                    expr.setExpression(text);
                } else {
                    expr.setValue(text);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, "string or expression"),
                        251);
            }
        }
        return expr;
    }

    protected static CommandLineBinding processCommandLineBinding(ParameterType parameterType,
            JsonNode bindingNode) throws CWLException {
        CommandLineBinding commandLineBinding = new CommandLineBinding();
        // loadContents, only valid when type: File or is an array of items:File
        JsonNode loadContentsNode = bindingNode.get("loadContents");
        if (hasFileType(parameterType, true) && loadContentsNode != null) {
            commandLineBinding.setLoadContents(processBooleanField("load", loadContentsNode));
        }
        // position
        JsonNode positionNode = bindingNode.get("position");
        if (positionNode != null && positionNode.isInt()) {
            commandLineBinding.setPosition(positionNode.asInt());
        }
        // prefix
        JsonNode prefixNode = bindingNode.get("prefix");
        if (prefixNode != null && prefixNode.isTextual()) {
            commandLineBinding.setPrefix(prefixNode.asText());
        }
        // separate
        JsonNode separateNode = bindingNode.get("separate");
        if (separateNode != null && separateNode.isBoolean()) {
            commandLineBinding.setSeparate(separateNode.asBoolean());
        }
        // itemSeparator
        JsonNode itemSeparatorNode = bindingNode.get("itemSeparator");
        if (itemSeparatorNode != null && itemSeparatorNode.isTextual()) {
            commandLineBinding.setItemSeparator(itemSeparatorNode.asText());
        }
        // valueFrom
        JsonNode valueFromNode = bindingNode.get("valueFrom");
        if (valueFromNode != null) {
            commandLineBinding.setValueFrom(processExpressionField("valueFrom", valueFromNode));
        }
        // shellQuote
        JsonNode shellQuoteNode = bindingNode.get("shellQuote");
        if (shellQuoteNode != null && shellQuoteNode.isBoolean()) {
            commandLineBinding.setShellQuote(shellQuoteNode.asBoolean());
        }
        return commandLineBinding;
    }

    protected static CommandOutputBinding processCommandOutputBinding(JsonNode bindingNode) throws CWLException {
        CommandOutputBinding outputBinding = new CommandOutputBinding();
        // process glob
        JsonNode globNode = bindingNode.get("glob");
        if (globNode != null) {
            OutputBindingGlob glob = new OutputBindingGlob();
            if (globNode.isTextual()) {
                glob.setGlobExpr(processExpressionField("glob", globNode));
            } else if (globNode.isArray()) {
                glob.setPatterns(processStringArrayField("glob", globNode));
            }
            outputBinding.setGlob(glob);
        }
        // process loadContents
        JsonNode loadContentsNode = bindingNode.get("loadContents");
        if (loadContentsNode != null && loadContentsNode.isBoolean()) {
            outputBinding.setLoadContents(loadContentsNode.asBoolean());
        }
        // process outputEval
        JsonNode outputEvalNode = bindingNode.get("outputEval");
        if (outputEvalNode != null) {
            outputBinding.setOutputEval(processExpressionField("outputEval", outputEvalNode));
        }
        return outputBinding;
    }

    protected static ParameterType processParameterType(boolean isInput,
            String id,
            String typeSymbol) throws CWLException {
        ParameterType type = new ParameterType();
        if (typeSymbol.endsWith("?")) {
            // Type <T> ending with ? will be transformed to [<T>, "null"]
            String symbol = typeSymbol.replace("?", "");
            if (symbol.endsWith("[]")) {
                symbol = symbol.replaceAll("\\[\\]", "");
                ParameterType items = new ParameterType();
                items.setType(toBasicType(id, symbol));
                if (isInput) {
                    type.setTypes(Arrays.asList(new InputArrayType(items), new NullType()));
                } else {
                    type.setTypes(Arrays.asList(new OutputArrayType(items), new NullType()));
                }
            } else {
                type.setTypes(Arrays.asList(toBasicType(id, symbol), new NullType()));
            }
        } else if (typeSymbol.endsWith("[]")) {
            ParameterType items = new ParameterType();
            items.setType(toBasicType(id, typeSymbol.replaceAll("\\[\\]", "")));
            if (isInput) {
                type.setType(new InputArrayType(items));
            } else {
                type.setType(new OutputArrayType(items));
            }
        } else {
            type.setType(toBasicType(id, typeSymbol));
        }
        return type;
    }

    protected static ParameterType processInputParameterType(String id, JsonNode typeNode) throws CWLException {
        if (typeNode.isTextual()) {
            return processParameterType(true, id, typeNode.asText());
        } else if (typeNode.isArray()) {
            /*
             * If the parameter type is an array, the types have an OR
             * relationship , it means the type of parameter value can be any
             * type in types. so we need to determine the exact type after the
             * value was loaded
             */
            ParameterType type = new ParameterType();
            List<CWLType> types = new ArrayList<>();
            Iterator<JsonNode> typeNodes = typeNode.elements();
            while (typeNodes.hasNext()) {
                JsonNode tNode = typeNodes.next();
                if (tNode.isObject()) {
                    types.add(toInputSchemaType(id, tNode));
                } else if (tNode.isTextual()) {
                    types.add(toBasicType(id, tNode.asText()));
                } else {
                    throw new CWLException(ResourceLoader.getMessage("cwl.parser.invalid.array.type", id),
                            251);
                }
            }
            type.setTypes(types);
            return type;
        } else if (typeNode.isObject()) {
            ParameterType type = new ParameterType();
            type.setType(toInputSchemaType(id, typeNode));
            return type;
        } else {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, "string or object"),
                    251);
        }
    }

    protected static ParameterType processOutputParameterType(String id, JsonNode typeNode) throws CWLException {
        if (typeNode.isTextual()) {
            return processParameterType(false, id, typeNode.asText());
        } else if (typeNode.isArray()) {
            /*
             * If the parameter type is an array, it is an OR relationship, it
             * means the type of parameter value can be any type in types. so we
             * need to determine the exact type after the value was loaded
             */
            ParameterType type = new ParameterType();
            List<CWLType> types = new ArrayList<>();
            Iterator<JsonNode> typeNodes = typeNode.elements();
            while (typeNodes.hasNext()) {
                JsonNode tNode = typeNodes.next();
                if (tNode.isObject()) {
                    types.add(toOutputSchemaType(id, tNode));
                } else if (tNode.isTextual()) {
                    types.add(toBasicType(id, tNode.asText()));
                } else {
                    throw new CWLException(ResourceLoader.getMessage("cwl.parser.invalid.array.type", id), 251);
                }
            }
            type.setTypes(types);
            return type;
        } else if (typeNode.isObject()) {
            ParameterType type = new ParameterType();
            type.setType(toOutputSchemaType(id, typeNode));
            return type;
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, "CWLType, Schema type or array"),
                    251);
        }
    }

    protected static boolean processStreamableField(String id, JsonNode streamableNode) throws CWLException {
        if (streamableNode != null) {
            if (streamableNode.isBoolean()) {
                return streamableNode.asBoolean();
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, CWLTypeSymbol.BOOLEAN),
                        251);
            }
        }
        return false;
    }

    protected static List<CWLFieldValue> processSecondaryFilesField(String id,
            JsonNode secondaryFilesNode) throws CWLException {
        List<CWLFieldValue> exprs = new ArrayList<>();
        if (secondaryFilesNode != null) {
            if (secondaryFilesNode.isTextual()) {
                exprs.add(processExpressionField(id, secondaryFilesNode));
            } else if (secondaryFilesNode.isArray()) {
                Iterator<JsonNode> elements = secondaryFilesNode.elements();
                while (elements.hasNext()) {
                    exprs.add(processExpressionField(id, elements.next()));
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, STRING_OR_ARRAY),
                        251);
            }
        }
        return exprs;
    }

    protected static FileFormat processFormatField(String id, JsonNode formatNode) throws CWLException {
        if (formatNode != null) {
            FileFormat format = new FileFormat();
            if (formatNode.isTextual()) {
                format.setFormat(processExpressionField(id, formatNode));
            } else if (formatNode.isArray()) {
                List<String> formats = new ArrayList<>();
                Iterator<JsonNode> elements = formatNode.elements();
                while (elements.hasNext()) {
                    JsonNode e = elements.next();
                    if (e.isTextual()) {
                        formats.add(e.asText());
                    } else {
                        throw new CWLException(
                                ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, STRING_OR_ARRAY),
                                251);
                    }
                }
                format.setFormats(formats);
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, STRING_OR_ARRAY),
                        251);
            }
            return format;
        }
        return null;
    }

    protected static CWLVersion processCWLVersion(JsonNode node) throws CWLException {
        CWLVersion version = null;
        String versionSymbol = processStringField(CWL_VERSION, node.get(CWL_VERSION));
        if (versionSymbol != null) {
            versionSymbol = versionSymbol.replace("cwl:", "").replace("https://w3id.org/cwl/cwl#", "");
            for (CWLVersion supported : CWLVersion.values()) {
                if (supported.getSymbol().equals(versionSymbol)) {
                    version = supported;
                    break;
                }
            }
        } else {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, CWL_VERSION), 251);
        }
        if (version == null) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.field.unsupported", CWL_VERSION, versionSymbol),
                    33);
        }
        return version;
    }

    protected static CWLFile processCWLFile(String parentPath,
            String id,
            JsonNode fileNode,
            boolean nochecksum) throws CWLException {
        CWLFile cwlFile = null;
        if (fileNode.isObject()) {
            cwlFile = new CWLFile();
            setCWLFileBase(parentPath, id, cwlFile, fileNode);
            setCWLFileAttr(cwlFile, Paths.get(cwlFile.getPath()));
            String filePath = cwlFile.getPath();
            if (filePath != null && filePath.startsWith(IOUtil.FILE_PREFIX)) {
                filePath = filePath.substring(7);
            }
            File file = new File(filePath);
            setPhysicalFileAttr(file, cwlFile, fileNode, nochecksum);
            // secondaryFiles
            JsonNode secondaryFilesNode = fileNode.get("secondaryFiles");
            if (secondaryFilesNode != null && secondaryFilesNode.isArray()) {
                cwlFile.setSecondaryFiles(toSecondaryFiles(id, cwlFile, secondaryFilesNode, nochecksum));
            }
            // format
            JsonNode formatNode = fileNode.get("format");
            if (formatNode != null && formatNode.isTextual()) {
                cwlFile.setFormat(formatNode.asText());
            }
        } else {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, "file object"), 251);
        }
        return cwlFile;
    }

    protected static CWLDirectory processCWLDirectory(String parentPath,
            String id,
            JsonNode dirNode,
            boolean nocecksum) throws CWLException {
        CWLDirectory dir = null;
        if (dirNode.isObject()) {
            dir = new CWLDirectory();
            JsonNode listingNode = dirNode.get("listing");
            setCWLDirBase(parentPath, id, dir, dirNode, listingNode);
            List<CWLFileBase> listing = new ArrayList<>();
            if (listingNode != null && listingNode.isArray()) {
                Path path = Paths.get(dir.getPath());
                Iterator<JsonNode> elements = listingNode.elements();
                while (elements.hasNext()) {
                    JsonNode secondaryFileNode = elements.next();
                    String secondaryFileClazz = secondaryFileNode.get(CLASS).asText();
                    if (CWLTypeSymbol.FILE.symbol().equals(secondaryFileClazz)) {
                        listing.add(processCWLFile(path.getParent().toString(), id, secondaryFileNode, nocecksum));
                    } else if (CWLTypeSymbol.DIRECTORY.symbol().equals(secondaryFileClazz)) {
                        listing.add(processCWLDirectory(path.getParent().toString(), id, secondaryFileNode, nocecksum));
                    }
                }
            } else {
                IOUtil.traverseDirListing(dir.getLocation(), listing, true);
            }
            dir.setListing(listing);
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, "directory", "directory object"), 251);
        }
        return dir;
    }

    protected static String processLocation(String parentPath, JsonNode locationNode) throws CWLException {
        String location = null;
        if (locationNode != null && locationNode.isTextual()) {
            location = locationNode.asText();
            if (location.startsWith(IOUtil.HTTP_PREFIX) ||
                    location.startsWith(IOUtil.HTTPS_PREFIX) ||
                    location.startsWith(IOUtil.FTP_PREFIX)) {
                throw new CWLException(ResourceLoader.getMessage("cwl.parser.remote.file.location.unsupported"),
                        33);
            } else {
                Path locationPath = Paths.get(location.replaceFirst(IOUtil.FILE_PREFIX, ""));
                if (!locationPath.isAbsolute()) {
                    locationPath = Paths.get(parentPath, location);
                    location = locationPath.toString();
                }
            }
        }
        return location;
    }

    protected static PostFailureScript processPostFailureScript(String key, JsonNode node) throws CWLException {
        PostFailureScript pfs = null;
        if (node != null) {
            if (node.isObject()) {
                pfs = toPostFailureScript(key, node);
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, key, "Object"), 251);
            }
        }
        return pfs;
    }

    private static PostFailureScript toPostFailureScript(String key, JsonNode node) throws CWLException {
        PostFailureScript pfs = new PostFailureScript();
        if (node.get("script") != null) {
            pfs.setScript(node.get("script").asText());
        } else {
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, key), 251);
        }
        if (node.get("timeout") != null) {
            pfs.setTimeout(node.get("timeout").asInt());
        } else {
            pfs.setTimeout(10);
        }
        if (node.get("retry") != null) {
            pfs.setRetry(node.get("retry").asInt());
        } else {
            pfs.setRetry(1);
        }
        return pfs;
    }

    private static CWLType findSchemaType(String typeSymbol) {
        CWLType schemaType = null;
        if (typeSymbol != null) {
            int refIndex = typeSymbol.indexOf('#');
            if (refIndex != -1) {
                String typeName = typeSymbol.substring(refIndex + 1);
                schemaType = schemaRefTypes.get(typeName);
            }
        }
        return schemaType;
    }

    private static boolean hasFileType(CWLType type, boolean isInput) {
        if (type.getSymbol() == CWLTypeSymbol.FILE) {
            return true;
        } else if (type.getSymbol() == CWLTypeSymbol.ARRAY) {
            return hasFileType(getArrayItems(type, isInput), isInput);
        }
        return false;
    }

    private static boolean hasExpr(String expr) {
        boolean result = false;
        if (expr != null) {
            Pattern pattern = Pattern.compile("\\$\\(.*?\\)");
            Matcher matcher = pattern.matcher(expr);
            result = matcher.find();
        }
        return result;
    }

    private static ParameterType getArrayItems(CWLType arrayType, boolean isInput) {
        ParameterType items = null;
        if (isInput) {
            items = ((InputArrayType) arrayType).getItems();
        } else {
            items = ((OutputArrayType) arrayType).getItems();
        }
        return items;
    }

    private static List<InputRecordField> processRecordArray(String id, JsonNode fieldNodes) throws CWLException {
        List<InputRecordField> fields = new ArrayList<>();
        Iterator<JsonNode> elements = fieldNodes.elements();
        while (elements.hasNext()) {
            JsonNode fieldNode = elements.next();
            String name = processStringField(id, fieldNode.get("name"));
            fields.add(toInputRecordField(id, name, fieldNode));
        }
        return fields;
    }

    private static List<InputRecordField> processRecordMap(String id, JsonNode fieldNodes) throws CWLException {
        List<InputRecordField> fields = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> recordFields = fieldNodes.fields();
        while (recordFields.hasNext()) {
            Entry<String, JsonNode> field = recordFields.next();
            String name = field.getKey();
            JsonNode fileldNode = field.getValue();
            fields.add(toInputRecordField(id, name, fileldNode));
        }
        return fields;
    }

    private static String processBaseName(JsonNode basenameNode) throws CWLException {
        String basename = null;
        if (basenameNode != null && basenameNode.isTextual()) {
            basename = basenameNode.asText();
            if (basename.contains(File.separator)) {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_FIELD, "file#basename",
                        "Must not contain a slash /"),
                        251);
            }
        }
        return basename;
    }

    private static Path processPath(String parentPath,
            String basename,
            String inputId,
            JsonNode pathNode) throws CWLException {
        Path path = null;
        if (pathNode != null && pathNode.isTextual()) {
            try {
                path = Paths.get(pathNode.asText());
                if (basename != null && !path.getFileName().toString().contains(basename)) {
                    throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_FIELD, inputId,
                            "basename does not match with the path"),
                            251);
                }
                if (!path.isAbsolute()) {
                    path = Paths.get(parentPath, pathNode.asText());
                }
            } catch (InvalidPathException e) {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, inputId, "a valid local path"), 251);
            }
        }
        return path;
    }

    private static CWLType toBasicType(String id, String typeSymbol) throws CWLException {
        switch (CWLTypeSymbol.toCWLTypeSymbol(typeSymbol)) {
        case NULL:
            return new NullType();
        case BOOLEAN:
            return new BooleanType();
        case INT:
            return new IntType();
        case LONG:
            return new LongType();
        case FLOAT:
            return new FloatType();
        case DOUBLE:
            return new DoubleType();
        case STRING:
            return new StringType();
        case FILE:
            return new FileType();
        case DIRECTORY:
            return new DirectoryType();
        case ANY:
            return new AnyType();
        default:
            CWLType refType = findSchemaType(typeSymbol);
            if (refType != null) {
                return refType;
            }
            throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, "a CWLType"), 251);
        }
    }

    private static CWLFileBase setCWLFileBase(String parentPath,
            String id,
            CWLFileBase cwlFile,
            JsonNode fileNode) throws CWLException {
        // base name
        String basename = processBaseName(fileNode.get("basename"));
        cwlFile.setBasename(basename);
        // local host path
        Path path = processPath(parentPath, basename, id, fileNode.get("path"));
        // an IRI location
        String location = processLocation(parentPath, fileNode.get("location"));
        if (location == null && path != null) {
            location = path.toString();
        }
        if (location == null) {
            JsonNode contentsNode = fileNode.get("contents");
            if (contentsNode != null && contentsNode.isTextual()) {
                String contents = contentsNode.asText();
                Path tmpLocationPath = Paths.get(parentPath, id + "-" + CommonUtil.getRandomStr());
                IOUtil.write64Kib(tmpLocationPath.toFile(), contents);
                location = tmpLocationPath.toString();
            }
        }
        if (location == null) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.field.required.in", "path, location or contents", id), 251);
        }
        cwlFile.setLocation(location);
        cwlFile.setPath(toPath(location, basename));
        return cwlFile;
    }

    private static CWLFileBase setCWLDirBase(String parentPath,
            String id,
            CWLFileBase cwlDir,
            JsonNode dirNode,
            JsonNode listingNode) throws CWLException {
        // base name
        String basename = processBaseName(dirNode.get("basename"));
        cwlDir.setBasename(basename);
        // local host path
        Path path = processPath(parentPath, basename, id, dirNode.get("path"));
        // an IRI location
        String location = processLocation(parentPath, dirNode.get("location"));
        if (location == null && path != null) {
            location = path.toString();
        }
        if (location == null && listingNode != null) {
            location = parentPath;
        }
        if (location == null) {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.field.required.in", "path, location or listing", id), 251);
        }
        cwlDir.setLocation(location);
        if (dirNode.get("location") == null && dirNode.get("path") == null) {
            cwlDir.setPath(Paths.get(location).resolve(basename).toString());
        } else {
            cwlDir.setPath(toPath(location, basename));
        }
        return cwlDir;
    }

    private static String toPath(String location, String basename) {
        String locationPath = location;
        if (location.startsWith(IOUtil.FILE_PREFIX)) {
            locationPath = location.substring(7);
        }
        Path path = Paths.get(locationPath);
        if (basename != null && !basename.equals(path.getFileName().toString())) {
            path = path.getParent().resolve(basename);
        }
        return path.toString();
    }

    private static void setCWLFileAttr(CWLFile cwlFile, Path path) {
        String filename = path.getFileName().toString();
        cwlFile.setDirname(path.getParent().toString());
        cwlFile.setBasename(filename);
        if (filename.indexOf('.') != -1) {
            cwlFile.setNameroot(filename.substring(0, filename.lastIndexOf('.')));
            cwlFile.setNameext(filename.substring(filename.lastIndexOf('.')));
        }
    }

    private static void setPhysicalFileAttr(File file, CWLFile cwlFile, JsonNode fileNode, boolean nochecksum) {
        // checksum
        JsonNode checksumNode = fileNode.get("checksum");
        if (checksumNode != null && checksumNode.isTextual()) {
            cwlFile.setChecksum(checksumNode.asText());
        } else if (file.exists() && !nochecksum) {
            String path = cwlFile.getPath();
            if (path != null && path.startsWith(IOUtil.FILE_PREFIX)) {
                path = path.substring(7);
            }
            cwlFile.setChecksum("sha1$" + IOUtil.md5(path));
        }
        // size
        JsonNode sizeNode = fileNode.get("size");
        if (sizeNode != null && sizeNode.isLong()) {
            cwlFile.setSize(sizeNode.asLong());
        } else if (file.exists()) {
            cwlFile.setSize(file.length());
        }
    }

    private static CWLType toInputSchemaType(String id, JsonNode typeNode) throws CWLException {
        JsonNode typeSymbolNode = typeNode.get("type");
        if (typeSymbolNode != null) {
            if (typeSymbolNode.isTextual()) {
                String typeSymbol = typeSymbolNode.asText();
                switch (typeSymbol) {
                case "array":
                    return toInputArrayType(id, typeNode);
                case "enum":
                    return toInputEnumType(id, typeNode);
                case "record":
                    return toInputRecordType(id, typeNode);
                default:
                    throw new CWLException(
                            ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, ARRAY_RECORD_OR_ENUM),
                            251);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, ARRAY_RECORD_OR_ENUM),
                        251);
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.field.required.in", id, "type"),
                    251);
        }
    }

    private static InputArrayType toInputArrayType(String id, JsonNode typeNode) throws CWLException {
        // processing items
        JsonNode itemsNode = typeNode.get("items");
        ParameterType items = processInputParameterType(id, itemsNode);
        InputArrayType inputArrayType = new InputArrayType(items);
        // processing label
        JsonNode labelNode = typeNode.get(LABEL);
        if (labelNode != null && labelNode.isTextual()) {
            inputArrayType.setLabel(labelNode.asText());
        }
        // processing inputBinding
        JsonNode inputBindingNode = typeNode.get(INPUT_BINDING);
        if (inputBindingNode != null && inputBindingNode.isObject()) {
            inputArrayType.setInputBinding(processCommandLineBinding(items, inputBindingNode));
        }
        return inputArrayType;
    }

    private static InputEnumType toInputEnumType(String id, JsonNode typeNode) throws CWLException {
        JsonNode symbolsNode = typeNode.get(SYMBOLS);
        if (symbolsNode == null) {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, "array<string>"),
                    251);
        }
        List<String> symbols = processStringArrayField(SYMBOLS, symbolsNode);
        InputEnumType inputEnumType = new InputEnumType(symbols);
        // processing label
        JsonNode labelNode = typeNode.get(LABEL);
        if (labelNode != null && labelNode.isTextual()) {
            inputEnumType.setLabel(labelNode.asText());
        }
        // processing inputBinding
        JsonNode inputBindingNode = typeNode.get(INPUT_BINDING);
        if (inputBindingNode != null && inputBindingNode.isObject()) {
            inputEnumType.setInputBinding(processCommandLineBinding(null, inputBindingNode));
        }
        return inputEnumType;
    }

    private static InputRecordType toInputRecordType(String id, JsonNode typeNode) throws CWLException {
        InputRecordType recordType = new InputRecordType();
        recordType.setName(processStringField(id, typeNode.get("name")));
        recordType.setLabel(processStringField(id, typeNode.get(LABEL)));
        JsonNode fieldNodes = typeNode.get("fields");
        if (fieldNodes != null) {
            if (fieldNodes.isArray()) {
                recordType.setFields(processRecordArray(id, fieldNodes));
            } else {

                recordType.setFields(processRecordMap(id, fieldNodes));
            }
        }
        return recordType;
    }

    private static InputRecordField toInputRecordField(String id,
            String name,
            JsonNode fileldNode) throws CWLException {
        InputRecordField inputRecordField = new InputRecordField();
        inputRecordField.setName(name);
        inputRecordField.setLabel(processStringField(id, fileldNode.get(LABEL)));
        inputRecordField.setDoc(processStringField(id, fileldNode.get("doc")));
        inputRecordField.setRecordType(processInputParameterType(id, fileldNode.get("type")));
        JsonNode bindingNode = fileldNode.get(INPUT_BINDING);
        if (bindingNode != null) {
            inputRecordField.setInputBinding(processCommandLineBinding(
                    inputRecordField.getRecordType(), bindingNode));
        }
        return inputRecordField;
    }

    private static CWLType toOutputSchemaType(String id, JsonNode typeNode) throws CWLException {
        JsonNode typeSymbolNode = typeNode.get("type");
        if (typeSymbolNode != null) {
            if (typeSymbolNode.isTextual()) {
                String typeSymbol = typeSymbolNode.asText();
                switch (typeSymbol) {
                case "array":
                    return toOutputArrayType(id, typeNode);
                case "enum":
                    return toOutputEnumType(id, typeNode);
                case "record":
                    return toOutputRecordType(id, typeNode);
                default:
                    throw new CWLException(
                            ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, ARRAY_RECORD_OR_ENUM),
                            251);
                }
            } else {
                throw new CWLException(
                        ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, ARRAY_RECORD_OR_ENUM),
                        251);
            }
        } else {
            throw new CWLException(
                    ResourceLoader.getMessage("cwl.parser.field.required.in", id, "type"),
                    251);
        }
    }

    private static OutputArrayType toOutputArrayType(String id, JsonNode typeNode) throws CWLException {
        // processing items
        JsonNode itemsNode = typeNode.get("items");
        ParameterType items = processOutputParameterType(id, itemsNode);
        OutputArrayType outputArrayType = new OutputArrayType(items);
        // processing label
        JsonNode labelNode = typeNode.get(LABEL);
        if (labelNode != null && labelNode.isTextual()) {
            outputArrayType.setLabel(labelNode.asText());
        }
        // processing inputBinding
        JsonNode outputBindingNode = typeNode.get(OUTPUT_BINDING);
        if (outputBindingNode != null && outputBindingNode.isObject()) {
            outputArrayType.setOutputBinding(processCommandOutputBinding(outputBindingNode));
        }
        return outputArrayType;
    }

    private static OutputEnumType toOutputEnumType(String id, JsonNode typeNode) throws CWLException {
        JsonNode symbolsNode = typeNode.get(SYMBOLS);
        if (symbolsNode == null) {
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, id, "array<string>"),
                    251);
        }
        List<String> symbols = processStringArrayField(SYMBOLS, symbolsNode);
        OutputEnumType outputEnumType = new OutputEnumType(symbols);
        // processing label
        JsonNode labelNode = typeNode.get(LABEL);
        if (labelNode != null && labelNode.isTextual()) {
            outputEnumType.setLabel(labelNode.asText());
        }
        // processing outputBinding
        JsonNode outputBindingNode = typeNode.get(OUTPUT_BINDING);
        if (outputBindingNode != null && outputBindingNode.isObject()) {
            outputEnumType.setOutputBinding(processCommandOutputBinding(outputBindingNode));
        }
        return outputEnumType;
    }

    private static OutputRecordType toOutputRecordType(String id, JsonNode typeNode) throws CWLException {
        OutputRecordType outputRecordType = new OutputRecordType();
        outputRecordType.setName(processStringField(id, typeNode.get("name")));
        outputRecordType.setLabel(processStringField(id, typeNode.get(LABEL)));
        JsonNode fieldNodes = typeNode.get("fields");
        if (fieldNodes != null && fieldNodes.isArray()) {
            List<OutputRecordField> fields = new ArrayList<>();
            Iterator<JsonNode> elements = fieldNodes.elements();
            while (elements.hasNext()) {
                JsonNode fileldNode = elements.next();
                OutputRecordField outputRecordField = new OutputRecordField();
                outputRecordField.setName(processStringField(id, fileldNode.get("name")));
                outputRecordField.setDoc(processStringField(id, fileldNode.get("doc")));
                outputRecordField.setRecordType(processInputParameterType(id, fileldNode.get("type")));
                JsonNode bindingNode = fileldNode.get(OUTPUT_BINDING);
                if (bindingNode != null) {
                    outputRecordField.setOutputBinding(processCommandOutputBinding(bindingNode));
                }
                fields.add(outputRecordField);
            }
            outputRecordType.setFields(fields);
        }
        return outputRecordType;
    }

    private static List<CWLFileBase> toSecondaryFiles(String id,
            CWLFile parent,
            JsonNode secondaryFilesNode,
            boolean nocecksum) throws CWLException {
        List<CWLFileBase> secondaryFiles = new ArrayList<>();
        Path path = Paths.get(parent.getPath());
        Iterator<JsonNode> elements = secondaryFilesNode.elements();
        while (elements.hasNext()) {
            JsonNode secondaryFileNode = elements.next();
            String secondaryFileClazz = secondaryFileNode.get(CLASS).asText();
            if (CWLTypeSymbol.FILE.symbol().equals(secondaryFileClazz)) {
                secondaryFiles.add(processCWLFile(path.getParent().toString(), id, secondaryFileNode, nocecksum));
            } else if (CWLTypeSymbol.DIRECTORY.symbol().equals(secondaryFileClazz)) {
                secondaryFiles.add(processCWLDirectory(path.getParent().toString(), id, secondaryFileNode, nocecksum));
            }
        }
        return secondaryFiles;
    }
}

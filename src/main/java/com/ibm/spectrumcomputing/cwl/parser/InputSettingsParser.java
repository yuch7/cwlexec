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
package com.ibm.spectrumcomputing.cwl.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.spectrumcomputing.cwl.model.Pair;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.ParameterType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullValue;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputEnumType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordField;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordType;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/*
 * Parses CWL input settings file and bind the input setting value to an input
 * parameter
 */
final class InputSettingsParser extends BaseParser {

    private static final String CANNOT_FIND_THE_RECORD_VALUE = "Cannot find the record value";
    private static final String VALID_CWL_TYPE = "a valid CWL type";

    private InputSettingsParser() {
    }

    protected static void processInputSettings(String parentPath,
            JsonNode settingsNode,
            List<? extends CWLParameter> inputs) throws CWLException {
        for (CWLParameter input : inputs) {
            String inputId = input.getId();
            JsonNode inputNode = settingsNode.get(inputId);
            Object value = bindInputSettingValue(parentPath, input, inputNode);
            if (value == null) {
                ParameterType type = input.getType();
                if (type == null || type.getType().getSymbol() != CWLTypeSymbol.NULL) {
                    throw new CWLException(
                            ResourceLoader.getMessage("cwl.parser.fail.to.bind.value", inputId,
                                    "The value cannot be found"),
                            252);
                }
            }
            input.setValue(value);
        }
    }

    protected static Object bindInputSettingValue(String parentPath,
            CWLParameter input,
            JsonNode inputNode) throws CWLException {
        ParameterType inputType = input.getType();
        Object value = null;
        if (inputNode != null) {
            CWLType type = inputType.getType();
            List<CWLType> types = inputType.getTypes();
            if (types != null) {
                // On top level, the parameter type should be fixed
                value = fixParameterType(input, findValueByTypes(parentPath, types, inputNode));
            } else if (type != null) {
                value = findValueByType(parentPath, type, input.getId(), inputNode);
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_FIELD_REQUIRED, input.getId()), 252);
            }
        }
        if (value == null) {
            value = input.getDefaultValue();
            if (value == NullValue.NULL) {
                value = null;
            }
        }
        return value;
    }

    private static Object fixParameterType(CWLParameter input,
            List<Pair<CWLType, Object>> typePairs) throws CWLException {
        Object value = null;
        if (typePairs.size() == 1) {
            input.getType().setType(typePairs.get(0).getKey());
            value = typePairs.get(0).getValue();
        } else {
            value = processRecordTypes(typePairs);
        }
        return value;
    }

    private static InputRecordField findInputRecordField(List<InputRecordField> fields, String fieldName) {
        InputRecordField field = null;
        for (InputRecordField f : fields) {
            if (fieldName.equals(f.getName())) {
                field = f;
                break;
            }
        }
        return field;
    }

    private static Object findValueByType(String parentPath,
            CWLType type,
            String inputId,
            JsonNode inputNode) throws CWLException {
        Object value = null;
        CWLTypeSymbol typeSymbol = type.getSymbol();
        switch (typeSymbol) {
        case NULL:
            return NullValue.NULL;
        case LONG:
            value = processLongField(inputId, inputNode);
            break;
        case INT:
            value = processIntegerField(inputId, inputNode);
            break;
        case FLOAT:
            value = processFloatField(inputId, inputNode);
            break;
        case DOUBLE:
            value = processDoubleField(inputId, inputNode);
            break;
        case BOOLEAN:
            value = processBooleanField(inputId, inputNode);
            break;
        case STRING:
            value = processStringField(inputId, inputNode);
            break;
        case FILE:
            value = processCWLFile(parentPath, inputId, inputNode, true);
            break;
        case DIRECTORY:
            value = processCWLDirectory(parentPath, inputId, inputNode, true);
            break;
        case ARRAY:
            ParameterType itemType = ((InputArrayType) type).getItems();
            CWLType item = itemType.getType();
            List<CWLType> items = itemType.getTypes();
            if (item != null) {
                value = processInputArray(parentPath, item, inputId, inputNode);
            } else if (items != null) {
                value = tryToFindSingleValue(findValueByTypes(parentPath, items, inputNode));
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, "type", VALID_CWL_TYPE),
                        251);
            }
            break;
        case ENUM:
            List<String> symbols = ((InputEnumType) type).getSymbols();
            value = processEnumField(symbols, inputId, inputNode);
            break;
        case RECORD:
            value = processRecordField(parentPath, (InputRecordType) type, inputId, inputNode);
            break;
        case ANY:
            value = processAnyFiled(inputNode);
            break;
        default:
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, inputId, VALID_CWL_TYPE),
                    251);
        }
        return value;
    }

    private static List<Pair<CWLType, Object>> findValueByTypes(String parentPath,
            List<CWLType> types,
            JsonNode inputNode) throws CWLException {
        List<Pair<CWLType, Object>> pairs = new ArrayList<>();
        for (CWLType type : types) {
            Object value = null;
            try {
                value = findValueByType(parentPath, type, "multi-items", inputNode);
            } catch (CWLException e) {
                // do nothing, retry again
            }
            if (!isEmptyValue(value)) {
                pairs.add(new Pair<CWLType, Object>(type, value));
            }
        }
        return pairs;
    }

    @SuppressWarnings("rawtypes")
    private static boolean isEmptyValue(Object value) {
        boolean isEmpty = false;
        if (value == null) {
            isEmpty = true;
        }
        if (value == NullValue.NULL) {
            isEmpty = true;
        }
        if (value instanceof List) {
            isEmpty = ((List) value).isEmpty();
        }
        return isEmpty;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List processInputArray(String parentPath,
            CWLType itemType,
            String inputId,
            JsonNode arrayNode) throws CWLException {
        List values = null;
        CWLTypeSymbol typeSymbol = itemType.getSymbol();
        switch (typeSymbol) {
        case LONG:
            values = processLongArrayField(inputId, arrayNode);
            break;
        case INT:
            values = processIntegerArrayField(inputId, arrayNode);
            break;
        case FLOAT:
            values = processFloatArrayField(inputId, arrayNode);
            break;
        case DOUBLE:
            values = processDoubleArrayField(inputId, arrayNode);
            break;
        case BOOLEAN:
            values = processBooleanArrayField(inputId, arrayNode);
            break;
        case STRING:
            values = processStringArrayField(inputId, arrayNode);
            break;
        case FILE:
            values = processCWLFileArrayField(parentPath, inputId, arrayNode);
            break;
        case DIRECTORY:
            values = processCWLDirectoryArrayField(parentPath, inputId, arrayNode);
            break;
        case ARRAY:
            ParameterType subArrayItemType = ((InputArrayType) itemType).getItems();
            values = new ArrayList<>();
            Iterator<JsonNode> subArrays = arrayNode.elements();
            while (subArrays.hasNext()) {
                JsonNode subArrayNode = subArrays.next();
                values.add(processInputArray(parentPath, subArrayItemType.getType(), inputId, subArrayNode));
            }
            break;
        case RECORD:
            values = processRecordArrayField(parentPath, (InputRecordType) itemType, inputId, arrayNode);
            break;
        case ANY:
            values = processAnyArrayField(arrayNode);
            break;
        default:
            throw new CWLException(
                    ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, "type", VALID_CWL_TYPE),
                    251);
        }
        return values;
    }

    private static String processEnumField(List<String> symbols,
            String inputId,
            JsonNode enumNode) throws CWLException {
        String symbol = processStringField(inputId, enumNode);
        for (String s : symbols) {
            if (s.equals(symbol)) {
                return symbol;
            }
        }
        throw new CWLException(ResourceLoader.getMessage(CWL_PARSER_INVALID_TYPE, inputId, symbols.toString()),
                251);
    }

    private static List<InputRecordField> processRecordTypes(
            List<Pair<CWLType, Object>> typePairs) throws CWLException {
        List<InputRecordField> recordFields = new ArrayList<>();
        if (typePairs.size() > 1 && typePairs.get(0).getKey().getSymbol() == CWLTypeSymbol.RECORD) {
            for (Pair<CWLType, Object> typePair : typePairs) {
                if (typePair.getValue() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<InputRecordField> records = (List<InputRecordField>) typePair.getValue();
                    for (InputRecordField record : records) {
                        recordFields.add(record);
                    }
                }
            }
        } else {
            throw new CWLException("too many types for one paramter", 251);
        }
        return recordFields;
    }

    private static Object processRecordField(String parentPath,
            InputRecordType recordType,
            String inputId,
            JsonNode recordNode) throws CWLException {
        if (recordNode.isArray()) {
            List<JsonNode> recordItemNodes = new ArrayList<>();
            Iterator<JsonNode> elements = recordNode.elements();
            while (elements.hasNext()) {
                recordItemNodes.add(elements.next());
            }
            return toRecords(parentPath, recordType, inputId, recordItemNodes);
        } else {
            return toRecord(parentPath, recordType, inputId, recordNode);
        }
    }

    private static List<InputRecordField> processRecordArrayField(String parentPath,
            InputRecordType recordType,
            String inputId,
            JsonNode recordNodes) throws CWLException {
        List<InputRecordField> recordFields = new ArrayList<>();
        Iterator<JsonNode> elements = recordNodes.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            InputRecordField subRecord = new InputRecordField();
            ParameterType subRecordType = new ParameterType();
            subRecordType.setType(recordType);
            subRecord.setRecordType(subRecordType);
            List<InputRecordField> subRecordFields = new ArrayList<>();
            Iterator<Entry<String, JsonNode>> fields = element.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValueNode = field.getValue();
                InputRecordField recordField = findInputRecordField(recordType.getFields(), fieldName);
                if (recordField != null) {
                    addRecord(parentPath, inputId, recordField, fieldValueNode, subRecordFields);
                } else {
                    throw new CWLException(CANNOT_FIND_THE_RECORD_VALUE, 251);
                }
            }
            subRecord.setValue(subRecordFields);
            recordFields.add(subRecord);
        }
        return recordFields;
    }

    private static List<CWLFile> processCWLFileArrayField(String parentPath,
            String key,
            JsonNode arrayNode) throws CWLException {
        List<CWLFile> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            array.add(processCWLFile(parentPath, key, element, true));
        }
        return array;
    }

    private static List<CWLDirectory> processCWLDirectoryArrayField(String parentPath,
            String key,
            JsonNode arrayNode) throws CWLException {
        List<CWLDirectory> array = new ArrayList<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            array.add(processCWLDirectory(parentPath, key, element, true));
        }
        return array;
    }

    private static List<InputRecordField> toRecords(String parentPath,
            InputRecordType recordType,
            String inputId,
            List<JsonNode> recordItemNodes) {
        List<InputRecordField> results = new ArrayList<>();
        List<InputRecordField> allRecordFields = recordType.getFields();
        for (JsonNode recordItemNode : recordItemNodes) {
            Iterator<Entry<String, JsonNode>> recordFieldNode = recordItemNode.fields();
            List<InputRecordField> recordFields = new ArrayList<>();
            tryToRecords(parentPath, inputId, recordFieldNode, recordFields, allRecordFields, results);
        }
        return results.isEmpty() ? null : results;
    }

    private static List<InputRecordField> toRecord(String parentPath,
            InputRecordType recordType,
            String inputId,
            JsonNode recordNode) throws CWLException {
        List<Entry<String, JsonNode>> recordNodes = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> recordFieldNodes = recordNode.fields();
        while (recordFieldNodes.hasNext()) {
            recordNodes.add(recordFieldNodes.next());
        }
        List<InputRecordField> fields = recordType.getFields();
        List<InputRecordField> recordFields = new ArrayList<>();
        for (InputRecordField recordField : fields) {
            for (Entry<String, JsonNode> recordFieldNode : recordNodes) {
                if (recordFieldNode.getKey().equals(recordField.getName())) {
                    Object value = null;
                    JsonNode valueNode = recordFieldNode.getValue();
                    ParameterType parameterType = recordField.getRecordType();
                    if (parameterType.getType() != null) {
                        value = findValueByType(parentPath, parameterType.getType(), inputId, valueNode);
                        recordField.setValue(value);
                        recordFields.add(copyRecordField(recordField));
                    }
                }
            }
        }
        return recordFields.isEmpty() ? null : recordFields;
    }

    private static Object tryToFindSingleValue(List<Pair<CWLType, Object>> typePairs) {
        if (!typePairs.isEmpty()) {
            return typePairs.size() == 1 ? typePairs.get(0).getValue() : typePairs;
        }
        return null;
    }

    private static void tryToRecords(String parentPath,
            String inputId,
            Iterator<Entry<String, JsonNode>> recordFieldNode,
            List<InputRecordField> recordFields,
            List<InputRecordField> allRecordFields,
            List<InputRecordField> results) {
        boolean hasException = false;
        while (recordFieldNode.hasNext()) {
            Entry<String, JsonNode> recordFieldNodes = recordFieldNode.next();
            for (InputRecordField recordField : allRecordFields) {
                if (recordFieldNodes.getKey().equals(recordField.getName())) {
                    try {
                        tryToRecord(parentPath, inputId, recordField, recordFieldNodes, recordFields);
                    } catch (CWLException e) {
                        hasException = true;
                    }
                }
            }
        }
        if (!hasException) {
            results.addAll(recordFields);
        }
    }

    private static void tryToRecord(String parentPath,
            String inputId,
            InputRecordField recordField,
            Entry<String, JsonNode> recordFieldNodes,
            List<InputRecordField> recordFields) throws CWLException {
        Object value = null;
        JsonNode valueNode = recordFieldNodes.getValue();
        ParameterType parameterType = recordField.getRecordType();
        if (parameterType.getType() != null) {
            value = findValueByType(parentPath, parameterType.getType(), inputId, valueNode);
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Pair<CWLType, Object>> typePairs = (List<Pair<CWLType, Object>>) value;
                for (Pair<CWLType, Object> pair : typePairs) {
                    recordField.setValue(pair.getValue());
                    recordFields.add(copyRecordField(recordField));
                }
            } else {
                recordField.setValue(value);
                recordFields.add(copyRecordField(recordField));
            }
        } else if (parameterType.getTypes() != null) {
            List<Pair<CWLType, Object>> typePairs = findValueByTypes(parentPath, parameterType.getTypes(),
                    valueNode);
            for (Pair<CWLType, Object> typePair : typePairs) {
                parameterType.setType(typePair.getKey());
                recordField.setValue(typePair.getValue());
                recordFields.add(copyRecordField(recordField));
            }
        }
    }

    private static void addRecord(String parentPath,
            String inputId,
            InputRecordField recordField,
            JsonNode fieldValueNode,
            List<InputRecordField> subRecordFields) throws CWLException {
        ParameterType parameterType = recordField.getRecordType();
        if (parameterType != null) {
            if (parameterType.getType() != null) {
                addRecordValue(parentPath, inputId, parameterType, recordField, fieldValueNode);
                subRecordFields.add(copyRecordField(recordField));
            } else if (parameterType.getTypes() != null) {
                List<Pair<CWLType, Object>> typePairs = findValueByTypes(parentPath, parameterType.getTypes(),
                        fieldValueNode);
                for (Pair<CWLType, Object> typePair : typePairs) {
                    parameterType.setType(typePair.getKey());
                    recordField.setValue(typePair.getValue());
                    subRecordFields.add(copyRecordField(recordField));
                }
            } else {
                throw new CWLException(CANNOT_FIND_THE_RECORD_VALUE, 251);
            }
        } else {
            throw new CWLException(CANNOT_FIND_THE_RECORD_VALUE, 251);
        }
    }

    private static void addRecordValue(String parentPath,
            String inputId,
            ParameterType parameterType,
            InputRecordField recordField,
            JsonNode fieldValueNode) throws CWLException {
        Object value = findValueByType(parentPath, parameterType.getType(), inputId, fieldValueNode);
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Pair<CWLType, Object>> typePairs = (List<Pair<CWLType, Object>>) value;
            List<InputRecordField> twoDimensionRecordFields = new ArrayList<>();
            for (Pair<CWLType, Object> pair : typePairs) {
                InputRecordType type = (InputRecordType) pair.getKey();
                InputRecordField twoDimensionRecord = new InputRecordField();
                ParameterType twoDimensionParameterType = new ParameterType();
                twoDimensionParameterType.setType(type);
                twoDimensionRecord.setRecordType(twoDimensionParameterType);
                twoDimensionRecord.setValue(pair.getValue());
                twoDimensionRecordFields.add(twoDimensionRecord);
            }
            recordField.setValue(twoDimensionRecordFields);
        } else {
            recordField.setValue(value);
        }
    }

    private static InputRecordField copyRecordField(InputRecordField recordField) {
        InputRecordField newRecordField = new InputRecordField();
        newRecordField.setDoc(recordField.getDoc());
        newRecordField.setInputBinding(recordField.getInputBinding());
        newRecordField.setLabel(recordField.getLabel());
        newRecordField.setName(recordField.getName());
        newRecordField.setRecordType(recordField.getRecordType());
        newRecordField.setValue(recordField.getValue());
        return newRecordField;
    }
}
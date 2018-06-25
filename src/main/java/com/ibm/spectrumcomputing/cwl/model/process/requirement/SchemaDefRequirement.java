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
package com.ibm.spectrumcomputing.cwl.model.process.requirement;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputArrayType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputEnumType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input.InputRecordType;

/**
 * Represents SchemaDefRequirement, this requirement consists of a list of type
 * definitions, the definitions can be {@link InputArrayType},
 * {@link InputEnumType} or {@link InputRecordType}
 */
@JsonInclude(Include.NON_NULL)
public class SchemaDefRequirement extends Requirement {

    private final List<InputArrayType> arrayTypes = new ArrayList<>();
    private final List<InputEnumType> enumTypes = new ArrayList<>();
    private final List<InputRecordType> recordTypes = new ArrayList<>();

    /**
     * Returns a list of {@link InputArrayType}
     * 
     * @return A list of {@link InputArrayType}
     */
    public List<InputArrayType> getArrayTypes() {
        return arrayTypes;
    }

    /**
     * Returns a list of {@link InputEnumType}
     * 
     * @return A list of {@link InputEnumType}
     */
    public List<InputEnumType> getEnumTypes() {
        return enumTypes;
    }

    /**
     * Returns a list of {@link InputRecordType}
     * 
     * @return A list of {@link InputRecordType}
     */
    public List<InputRecordType> getRecordTypes() {
        return recordTypes;
    }

    /**
     * Adds a schema type, the schema type can be {@link InputArrayType},
     * {@link InputEnumType} or {@link InputRecordType}
     * 
     * @param schemaType
     *            A given schema type
     * 
     * @throws CWLException
     *             The schema type is not {@link InputArrayType},
     *             {@link InputEnumType} or {@link InputRecordType}
     */
    public void addSchemaType(CWLType schemaType) throws CWLException {
        if (schemaType instanceof InputArrayType) {
            arrayTypes.add((InputArrayType) schemaType);
        } else if (schemaType instanceof InputEnumType) {
            enumTypes.add((InputEnumType) schemaType);
        } else if (schemaType instanceof InputRecordType) {
            recordTypes.add((InputRecordType) schemaType);
        } else {
            throw new CWLException("Unsupported schema type", 33);
        }
    }

    /**
     * Always "SchemaDefRequirement"
     */
    @Override
    public String getClazz() {
        return "SchemaDefRequirement";
    }

}

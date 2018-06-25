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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.type.input;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;

/**
 * Represents an input record type
 */
@JsonInclude(Include.NON_NULL)
public class InputRecordType extends CWLType {

    private List<InputRecordField> fields;

    private String name;

    /**
     * Return the fields of this record
     * 
     * @return A list of record fields
     */
    public List<InputRecordField> getFields() {
        return fields;
    }

    /**
     * Sets the fields of this record
     * 
     * @param fields
     *            A list of record fields
     */
    public void setFields(List<InputRecordField> fields) {
        this.fields = fields;
    }

    /**
     * Returns the name of this type
     * 
     * @return The name of this type
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this type
     * 
     * @param name
     *            The name of this type
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Always RECORD
     */
    @Override
    public CWLTypeSymbol getSymbol() {
        return CWLTypeSymbol.RECORD;
    }
}

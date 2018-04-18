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
package com.ibm.spectrumcomputing.cwl.model.process.parameter;

/**
 * Represents a symbol of a CWL type
 */
public enum CWLTypeSymbol {
    /**
     * Indicates the parameter is no value
     */
    NULL("null"),
    /**
     * Indicates the parameter is a boolean value
     */
    BOOLEAN("boolean"),
    /**
     * Indicates the parameter is a 32-bit signed integer
     */
    INT("int"),
    /**
     * Indicates the parameter is a 64-bit signed integer
     */
    LONG("long"),
    /**
     * Indicates the parameter is a single precision (32-bit) IEEE 754
     * floating-point number
     */
    FLOAT("float"),
    /**
     * Indicates the parameter is a double precision (64-bit) IEEE 754
     * floating-point number
     */
    DOUBLE("double"),
    /**
     * Indicates the parameter is an unicode character sequence
     */
    STRING("string"),
    /**
     * Indicates the parameter is a File object
     */
    FILE("File"),
    /**
     * Indicates the parameter is a Directory object
     */
    DIRECTORY("Directory"),
    /**
     * Indicates the parameter is a record object
     */
    RECORD("record"),
    /**
     * Indicates the parameter is a enum object
     */
    ENUM("enum"),
    /**
     * Indicates the parameter is an array object
     */
    ARRAY("array"),
    /**
     * Indicates the parameter can be an any of object
     */
    ANY("Any"),
    /**
     * The parameter type is not a certain type, it needs to determine on
     * runtime
     */
    UNKNOWN("unknown");

    private final String symbol;

    private CWLTypeSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the symbol of this type
     */
    @Override
    public String toString() {
        return this.symbol;
    }

    /**
     * Returns the symbol of this type
     * 
     * @return The symbol of this type
     */
    public String symbol() {
        return this.symbol;
    }

    /**
     * To a CWL type by its symbol
     * 
     * @param symbol
     *            A symbol of a CWL type
     * @return A CWL type symbol
     */
    public static CWLTypeSymbol toCWLTypeSymbol(String symbol) {
        CWLTypeSymbol typeSymbol = UNKNOWN;
        if (symbol != null) {
            for (CWLTypeSymbol ts : CWLTypeSymbol.values()) {
                if (ts.symbol.equals(symbol)) {
                    typeSymbol = ts;
                    break;
                }
            }
        }
        return typeSymbol;
    }
}

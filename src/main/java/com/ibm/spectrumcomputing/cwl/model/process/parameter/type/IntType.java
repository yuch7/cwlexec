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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.type;

import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLType;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLTypeSymbol;

/**
 * Represents a integer type, indicates the parameter is a 32-bit signed integer
 */
public class IntType extends CWLType {

    /**
     * Always INT
     */
    @Override
    public CWLTypeSymbol getSymbol() {
        return CWLTypeSymbol.INT;
    }
}

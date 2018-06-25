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
package com.ibm.spectrumcomputing.cwl.model.process.parameter.method;

/**
 * Represents the scatter method
 */
public enum ScatterMethod {
    /**
     * A dotproduct method specifies that each of the input arrays are aligned and
     * one element taken from each array to construct each job. It is an error if
     * all input arrays are not the same length.
     */
    DOTPRODUCT,
    /**
     * A nested_crossproduct method specifies the Cartesian product of the inputs,
     * producing a job for every combination of the scattered inputs. The output
     * must be nested arrays for each level of scattering, in the order that the
     * input arrays are listed in the scatter field.
     */
    NESTED_CROSSPRODUCT,
    /**
     * A flat_crossproduct method specifies the Cartesian product of the inputs,
     * producing a job for every combination of the scattered inputs. The output
     * arrays must be flattened to a single level, but otherwise listed in the order
     * that the input arrays are listed in the scatter field.
     */
    FLAT_CROSSPRODUCT;
}

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
 * Represents a LinkMergeMethod
 */
public enum LinkMergeMethod {
    /**
     * The input must be an array consisting of exactly one entry for each input
     * link. If "merge_nested" is specified with a single link, the value from
     * the link must be wrapped in a single-item list.
     */
    MERGE_NESTED("merge_nested"),
    /**
     * <ul>
     * <li>The source and sink parameters must be compatible types, or the
     * source type must be compatible with single element from the "items" type
     * of the destination array parameter.</li>
     * <li>Source parameters which are arrays are concatenated. Source
     * parameters which are single element types are appended as single
     * elements.</li>
     * </ul>
     */
    MERGE_FLATTENED("merge_flattened");

    private final String symbol;

    private LinkMergeMethod(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the symbol for a LinkMergeMethod
     */
    @Override
    public String toString() {
        return this.symbol;
    }

    /**
     * Finds a LinkMergeMethod by its symbol
     * 
     * @param symbol
     *            A LinkMergeMethod symbol
     * @return If not found, a null value will be returned
     */
    public static LinkMergeMethod findMethod(String symbol) {
        LinkMergeMethod linkMergeMethod = null;
        if (symbol != null) {
            for (LinkMergeMethod method : LinkMergeMethod.values()) {
                if (method.symbol.equals(symbol)) {
                    return method;
                }
            }
        }
        return linkMergeMethod;
    }
}

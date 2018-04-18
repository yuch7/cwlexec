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
package com.ibm.spectrumcomputing.cwl.model.exception;

/**
 * Signals that an exception has occurred when run a CWL process. 
 */
public final class CWLException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int exceptionCode;

    /**
     * Signals that an exception has occurred when run a CWL process.
     * 
     * @param msg
     *            The exception messages
     * @param exceptionCode
     *            The exception code to indicate the type of exception
     *            <ul>
     *            <li>250 - Failed to validate the CWL document</li>
     *            <li>251 - Failed to parse CWL document</li>
     *            <li>253 - Failed to evaluate a CWL expression</li>
     *            <li>255 - A system exception, e.g. Failed to operate I/O,
     *            Failed to execute a command, etc.</li>
     *            </ul>
     */
    public CWLException(String msg, int exceptionCode) {
        super(msg);
        this.exceptionCode = exceptionCode;
    }

    /**
     * @return The code of this exception
     */
    public int getExceptionCode() {
        return exceptionCode;
    }
}

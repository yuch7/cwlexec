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
package com.ibm.spectrumcomputing.cwl.exec.service;

import java.util.List;

import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLCommandInstance;

/*
 * Depends on specific runtime environment to build CWL process instance execution
 * command
 */
interface CWLCommandService {

    /**
     * Depends on specific runtime environment to build an execution command for
     * a CWL CommandLineTool instance
     * 
     * @param instance
     *            A CWL CommandLineTool instance
     * @return An execution command
     * @throws CWLException
     *             Failed to build command
     */
    public List<String> buildCommand(CWLCommandInstance instance) throws CWLException;

    /**
     * Depends on specific runtime environment to build the execution commands for
     * a CWL scatter step
     *
     * @param instance
     *            A CWL scatter step instance
     * @throws CWLException
     *             Failed to build scatter command
     */
    public void buildScatterCommand(CWLCommandInstance instance) throws CWLException;
}

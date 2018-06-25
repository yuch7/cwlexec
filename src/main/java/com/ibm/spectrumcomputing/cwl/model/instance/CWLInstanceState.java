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
package com.ibm.spectrumcomputing.cwl.model.instance;

/**
 * The state of CWL process instance
 */
public enum CWLInstanceState {
    /**
     * The CWL instance is waiting to run
     */
    WAITING,
    /**
     * The CWL instance is pending in HPC execution platform
     */
    PENDING,
    /**
     * The CWL instance is running
     */
    RUNNING,
    /**
     * The CWL instance is suspended in HPC execution platform
     */
    SUSPENDED,
    /**
     * The CWL instance is done
     */
    DONE,
    /**
     * The CWL instance is exited
     */
    EXITED,
    /**
     * The CWL instance was killed
     */
    KILLED,
    /**
     * An unknown state from the HPC execution platform
     */
    UNKNOWN
}

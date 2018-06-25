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
package com.ibm.spectrumcomputing.cwl;

import com.ibm.spectrumcomputing.cwl.exec.CWLExecLauncher;

/**
 * The entrance of cwlexec
 */
public class Application {

    /**
     * The main method of cwlexec 
     * 
     * @param args
     *            The cwlexec command arguments
     */
    public static void main(String[] args) {
        CWLExecLauncher launcher = new CWLExecLauncher(args);
        launcher.launchCWLExec();
    }
}

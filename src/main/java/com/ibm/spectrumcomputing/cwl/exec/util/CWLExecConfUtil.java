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
package com.ibm.spectrumcomputing.cwl.exec.util;

import com.ibm.spectrumcomputing.cwl.model.conf.FlowExecConf;
import com.ibm.spectrumcomputing.cwl.model.conf.PostFailureScript;
import com.ibm.spectrumcomputing.cwl.model.conf.StepExecConf;

/**
 * Utility methods for finding CWL execution configuration
 */
public final class CWLExecConfUtil {

    private CWLExecConfUtil() {
    }

    /**
     * Finds the application profile (app) configuration argument from a given
     * FlowExecConf object by a CWL Workflow step name
     * 
     * @param flowExecConf
     *            A FlowExecConf object
     * 
     * @param stepName
     *            The name of a CWL Workflow step
     * @return If the configuration argument is not found, a null value will be
     *         returned
     */
    public static String getApp(FlowExecConf flowExecConf, String stepName) {
        String app = null;
        if (flowExecConf != null && stepName != null) {
            app = flowExecConf.getApp();
            if (flowExecConf.getSteps() != null) {
                StepExecConf stepExecConf = flowExecConf.getSteps().get(stepName);
                if (stepExecConf != null) {
                    String stepApp = stepExecConf.getApp();
                    if (stepApp != null) {
                        app = stepApp;
                    }
                }
            }
        }
        return app;
    }

    /**
     * Finds the resource requirement (res_req) configuration argument from a
     * given FlowExecConf object by a CWL Workflow step name
     * 
     * @param flowExecConf
     *            A FlowExecConf object
     * @param stepName
     *            The name of a CWL Workflow step
     * @return If the configuration argument is not found, a null value will be
     *         returned
     */
    public static String getResource(FlowExecConf flowExecConf, String stepName) {
        String resource = null;
        if (flowExecConf != null && stepName != null) {
            resource = flowExecConf.getResource();
            if (flowExecConf.getSteps() != null) {
                StepExecConf stepExecConf = flowExecConf.getSteps().get(stepName);
                if (stepExecConf != null) {
                    String stepResource = stepExecConf.getResource();
                    if (stepResource != null) {
                        resource = stepResource;
                    }
                }
            }
        }
        return resource;
    }

    /**
     * Finds the queue configuration argument from a given FlowExecConf object
     * by a CWL Workflow step name
     * 
     * @param flowExecConf
     *            A FlowExecConf object
     * @param stepName
     *            The name of a CWL Workflow step
     * @return If the configuration argument is not found, a null value will be
     *         returned
     */
    public static String getQueue(FlowExecConf flowExecConf, String stepName) {
        String queue = null;
        if (flowExecConf != null && stepName != null) {
            queue = flowExecConf.getQueue();
            if (flowExecConf.getSteps() != null) {
                StepExecConf stepExecConf = flowExecConf.getSteps().get(stepName);
                if (stepExecConf != null) {
                    String stepQueue = stepExecConf.getQueue();
                    if (stepQueue != null) {
                        queue = stepQueue;
                    }
                }
            }
        }
        return queue;
    }

    /**
     * Finds the number of processors configuration argument from a given FlowExecConf
     * object by a CWL Workflow step name
     *
     * @param flowExecConf
     *            A FlowExecConf object
     * @param stepName
     *            The name of a CWL workflow step
     * @return If the configuration argument is not found, a null value will be
     *         returned
     */
    public static String getProcessors(FlowExecConf flowExecConf, String stepName) {
	String processors = null;
	if (flowExecConf != null && stepName != null) {
	    processors = flowExecConf.getProcessors();
	    if (flowExecConf.getSteps() != null) {
		StepExecConf stepExecConf = flowExecConf.getSteps().get(stepName);
		if (stepExecConf != null) {
		    String stepProcessors = stepExecConf.getProcessors();
		    if (stepProcessors != null) {
			processors = stepProcessors;
		    }
		}
	    }
	}
	return processors;
    }
	    
    /**
     * Finds the project configuration argument from a given FlowExecConf object
     * by a CWL Workflow step name
     * 
     * @param flowExecConf
     *            A FlowExecConf object
     * @param stepName
     *            The name of a CWL Workflow step
     * @return If the configuration argument is not found, a null value will be
     *         returned
     */
    public static String getProject(FlowExecConf flowExecConf, String stepName) {
        String project = null;
        if (flowExecConf != null && stepName != null) {
            project = flowExecConf.getProject();
            if (flowExecConf.getSteps() != null) {
                StepExecConf stepExecConf = flowExecConf.getSteps().get(stepName);
                if (stepExecConf != null) {
                    String stepProject = stepExecConf.getProject();
                    if (stepProject != null) {
                        project = stepProject;
                    }
                }
            }
        }
        return project;
    }

    /**
     * Finds the rerunnable configuration argument from a given FlowExecConf
     * object by a CWL Workflow step name
     * 
     * @param flowExecConf
     *            A FlowExecConf object
     * @param stepName
     *            The name of a CWL Workflow step
     * @return If the configuration argument is not found, a null value will be
     *         returned
     */
    public static boolean isRerunnable(FlowExecConf flowExecConf, String stepName) {
        boolean rerunnable = false;
        if (flowExecConf != null && stepName != null) {
            rerunnable = flowExecConf.isRerunnable();
            if (flowExecConf.getSteps() != null) {
                StepExecConf stepExecConf = flowExecConf.getSteps().get(stepName);
                if (stepExecConf != null) {
                    rerunnable = stepExecConf.isRerunnable();
                }
            }
        }
        return rerunnable;
    }

    /**
     * Finds the post failure script configuration argument from a given
     * FlowExecConf object by a CWL Workflow step name
     * 
     * @param flowExecConf
     *            A FlowExecConf object
     * @param stepName
     *            The name of a CWL Workflow step
     * @return If the configuration argument is not found, a null value will be
     *         returned
     */
    public static PostFailureScript getPostFailureScript(FlowExecConf flowExecConf, String stepName) {
        PostFailureScript pfscript = null;
        if (flowExecConf != null && stepName != null) {
            pfscript = flowExecConf.getPostFailureScript();
            if (flowExecConf.getSteps() != null) {
                StepExecConf stepExecConf = flowExecConf.getSteps().get(stepName);
                if (stepExecConf != null) {
                    PostFailureScript steppfscript = stepExecConf.getPostFailureScript();
                    if (steppfscript != null) {
                        pfscript = steppfscript;
                    }
                }
            }
        }
        return pfscript;
    }

}

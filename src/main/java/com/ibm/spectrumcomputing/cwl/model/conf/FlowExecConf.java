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
package com.ibm.spectrumcomputing.cwl.model.conf;

import java.util.Map;

/**
 * Represents the cwlexec execution configuration on main process level
 */
public class FlowExecConf {
    private String app;
    private String resource;
    private String queue;
    private String project;
    private boolean rerunnable;
    private String processors;
    private PostFailureScript pfscript;
    private Map<String, StepExecConf> steps;

    /**
     * Returns the LSF application profile option
     * 
     * @return The LSF application profile option
     */
    public String getApp() {
        return app;
    }

    /**
     * Sets the LSF application profile (-app) option
     * 
     * @param app
     *            The LSF application profile option
     */
    public void setApp(String app) {
        this.app = app;
    }

    /**
     * Returns the LSF resource requirement (-R) option
     * 
     * @return The LSF resource requirement option
     */
    public String getResource() {
        return resource;
    }

    /**
     * Sets the LSF resource requirement (-R) option
     * 
     * @param resource
     *            The LSF resource requirement option
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * Returns the LSF queue (-q) option
     * 
     * @return The LSF queue option
     */
    public String getQueue() {
        return queue;
    }

    /**
     * Sets the LSF queue (-q) option
     * 
     * @param queue
     *            The LSF queue option
     */
    public void setQueue(String queue) {
        this.queue = queue;
    }

    /**
     * Returns the LSF project (-P) option
     * 
     * @return The LSF project option
     */
    public String getProject() {
        return project;
    }

    /**
     * Sets the LSF project (-P) option
     * 
     * @param project
     *            The LSF project option
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Returns the LSF rerunnable (-r) option
     * 
     * @return The LSF rerunnable option
     */
    public boolean isRerunnable() {
        return rerunnable;
    }

    /**
     * Sets the LSF rerunnable (-r) option
     * 
     * @param rerunnable
     *            The LSF rerunnable option
     */
    public void setRerunnable(boolean rerunnable) {
        this.rerunnable = rerunnable;
    }

    /**
     * Returns the LSF processors (-n) option
     * 
     * @return The LSF processors option
     */
    public String getProcessors() {
        return processors;
    }
    /**
     * Sets the LSF processors (-n) option
     *
     * @param processors
     *        The LSF processors requirement option
     */
    public void setProcessors(String processors) {
        this.processors = processors;
    }
    /**
     * Returns the step process configurations
     *
     * @return Step process configurations, the name of step is the key, the
     *         configuration of step is the value
     */
    public Map<String, StepExecConf> getSteps() {
        return steps;
    }

    /**
     * Sets the step process configurations
     * 
     * @param steps
     *            The step process configurations, the name of step is the key,
     *            the configuration of step is the value
     */
    public void setSteps(Map<String, StepExecConf> steps) {
        this.steps = steps;
    }

    /**
     * Returns a post failure script configuration
     * 
     * @return The post failure script configuration
     */
    public PostFailureScript getPostFailureScript() {
        return pfscript;
    }

    /**
     * Sets the post failure script configuration
     * 
     * @param postFailureScript The post failure script configuration
     */
    public void setPostFailureScript(PostFailureScript postFailureScript) {
        this.pfscript = postFailureScript;
    }
}

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
package com.ibm.spectrumcomputing.cwl.model.conf;

/**
 * Represents the cwlexec execution configuration on step process level
 */
public class StepExecConf {

    private String queue;
    private String project;
    private boolean rerunnable;
    private String app;
    private String resource;
    private PostFailureScript pfscript;

    /**
     * Returns the LSF application profile option for a given step
     * 
     * @return The LSF application profile option
     */
    public String getApp() {
        return app;
    }

    /**
     * Sets the LSF application profile (-app) option for a given step
     * 
     * @param app
     *            The LSF application profile option
     */
    public void setApp(String app) {
        this.app = app;
    }

    /**
     * Returns the LSF resource requirement (-R) option for a given step
     * 
     * @return The LSF resource requirement option
     */
    public String getResource() {
        return resource;
    }

    /**
     * Sets the LSF resource requirement (-R) option for a given step
     * 
     * @param resource
     *            The LSF resource requirement option
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * Returns the LSF queue (-q) option for a given step
     * 
     * @return The LSF queue option
     */
    public String getQueue() {
        return queue;
    }

    /**
     * Sets the LSF queue (-q) option for a given step
     * 
     * @param queue
     *            The LSF queue option
     */
    public void setQueue(String queue) {
        this.queue = queue;
    }

    /**
     * Returns the LSF project (-P) option for a given step
     * 
     * @return The LSF project option
     */
    public String getProject() {
        return project;
    }

    /**
     * Sets the LSF project (-P) option for a given step
     * 
     * @param project
     *            The LSF project option
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Returns the LSF rerunnable (-r) option for a given step
     * 
     * @return The LSF rerunnable option
     */
    public boolean isRerunnable() {
        return rerunnable;
    }

    /**
     * Sets the LSF rerunnable (-r) option for a given step
     * 
     * @param rerunnable
     *            The LSF rerunnable option
     */
    public void setRerunnable(boolean rerunnable) {
        this.rerunnable = rerunnable;
    }

    /**
     * Returns a post failure script configuration for a given step
     * 
     * @return The post failure script configuration
     */
    public PostFailureScript getPostFailureScript() {
        return pfscript;
    }

    /**
     * Sets the post failure script configuration for a given step
     * 
     * @param postFailureScript
     *            The post failure script configuration
     */
    public void setPostFailureScript(PostFailureScript postFailureScript) {
        this.pfscript = postFailureScript;
    }
}

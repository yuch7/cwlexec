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
 * Represents the post failure script configuration
 */
public class PostFailureScript {

    private String script;
    private int timeout;
    private int retry;

    /**
     * Returns the post failure script
     * 
     * @return The post failure script
     */
    public String getScript() {
        return script;
    }

    /**
     * Sets the post failure script
     * 
     * @param script
     *            The post failure script
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Returns the timeout for execution the post failure script
     * 
     * @return The timeout for execution the post failure script
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout for execution the post failure script
     * 
     * @param timeout
     *            The timeout for execution the post failure script
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns the retry times for execution the post failure script
     * 
     * @return The retry times for execution the post failure script
     */
    public int getRetry() {
        return retry;
    }

    /**
     * Sets the retry times for execution the post failure script
     * 
     * @param retry
     *            The retry times for execution the post failure script
     */
    public void setRetry(int retry) {
        this.retry = retry;
    }
}

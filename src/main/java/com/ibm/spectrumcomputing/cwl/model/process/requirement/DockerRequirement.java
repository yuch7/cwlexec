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
package com.ibm.spectrumcomputing.cwl.model.process.requirement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents DockerRequirement, this requirement indicates that a workflow
 * component should be run in a Docker container, and specifies how to fetch or
 * build the image.
 */
@JsonInclude(Include.NON_NULL)
public class DockerRequirement extends Requirement {

    private String dockerPull;
    private String dockerLoad;
    private String dockerFile;
    private String dockerImport;
    private String dockerImageId;
    private String dockerOutputDirectory;

    /**
     * Returns a Docker image to retrieve using docker pull
     * 
     * @return A Docker image id
     */
    public String getDockerPull() {
        return dockerPull;
    }

    /**
     * Sets a Docker image to retrieve using docker pull
     * 
     * @param dockerPull
     *            A Docker image id
     */
    public void setDockerPull(String dockerPull) {
        this.dockerPull = dockerPull;
    }

    /**
     * Returns a HTTP URL from which to download a Docker image using docker
     * load.
     * 
     * @return A HTTP URL
     */
    public String getDockerLoad() {
        return dockerLoad;
    }

    /**
     * Sets a HTTP URL from which to download a Docker image using docker load.
     * 
     * @param dockerLoad
     *            A HTTP URL
     */
    public void setDockerLoad(String dockerLoad) {
        this.dockerLoad = dockerLoad;
    }

    /**
     * Returns the contents of a Dockerfile which will be built using docker
     * build
     * 
     * @return The contents of a Dockerfile
     */
    public String getDockerFile() {
        return dockerFile;
    }

    /**
     * Sets the contents of a Dockerfile which will be built using docker build
     * 
     * @param dockerFile
     *            The contents of a Dockerfile
     */
    public void setDockerFile(String dockerFile) {
        this.dockerFile = dockerFile;
    }

    /**
     * Returns a HTTP URL to download and gunzip a Docker images using docker
     * import
     * 
     * @return A HTTP URL
     */
    public String getDockerImport() {
        return dockerImport;
    }

    /**
     * Sets a HTTP URL to download and gunzip a Docker images using docker
     * import
     * 
     * @param dockerImport
     *            A HTTP URL
     */
    public void setDockerImport(String dockerImport) {
        this.dockerImport = dockerImport;
    }

    /**
     * Returns an image id that will be used for docker run. May be a
     * human-readable image name or the image identifier hash. May be skipped if
     * dockerPull is specified, in which case the dockerPull image id must be
     * used.
     * 
     * @return A Docker image id
     */
    public String getDockerImageId() {
        return dockerImageId;
    }

    /**
     * Sets an image id that will be used for docker run. May be a
     * human-readable image name or the image identifier hash. May be skipped if
     * dockerPull is specified, in which case the dockerPull image id must be
     * used.
     * 
     * @param dockerImageId
     *            A Docker image id
     */
    public void setDockerImageId(String dockerImageId) {
        this.dockerImageId = dockerImageId;
    }

    /**
     * Returns the designated output directory to a specific location inside the
     * Docker container.
     * 
     * @return A designated output directory
     */
    public String getDockerOutputDirectory() {
        return dockerOutputDirectory;
    }

    /**
     * Sets the designated output directory to a specific location inside the
     * Docker container.
     * 
     * @param dockerOutputDirectory
     *            A designated output directory
     */
    public void setDockerOutputDirectory(String dockerOutputDirectory) {
        this.dockerOutputDirectory = dockerOutputDirectory;
    }

    /**
     * Always "DockerRequirement"
     */
    @Override
    public String getClazz() {
        return "DockerRequirement";
    }
}

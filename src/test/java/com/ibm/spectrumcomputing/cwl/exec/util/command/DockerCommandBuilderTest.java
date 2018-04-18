package com.ibm.spectrumcomputing.cwl.exec.util.command;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.exec.util.command.DockerCommandBuilder;
import com.ibm.spectrumcomputing.cwl.model.process.requirement.DockerRequirement;

public class DockerCommandBuilderTest {

    @Test
    public void findImageId() {
        //node
        DockerRequirement dockerRequirement = new DockerRequirement();
        dockerRequirement.setDockerPull("node");
        String imageId = DockerCommandBuilder.findImageId(dockerRequirement);
        assertEquals("node:latest", imageId);
        //node:latest
        dockerRequirement = new DockerRequirement();
        dockerRequirement.setDockerImageId("ubuntu:14.04");
        imageId = DockerCommandBuilder.findImageId(dockerRequirement);
        assertEquals("ubuntu:14.04", imageId);
        //sha256:c1d02ac1d9b4de08d3a39fdacde10427d1c4d8505172d31dd2b4ef78048559f8
        dockerRequirement = new DockerRequirement();
        dockerRequirement.setDockerPull("ubuntu@sha256:c1d02ac1d9b4de08d3a39fdacde10427d1c4d8505172d31dd2b4ef78048559f8");
        imageId = DockerCommandBuilder.findImageId(dockerRequirement);
        assertEquals("ubuntu@sha256:c1d02ac1d9b4de08d3a39fdacde10427d1c4d8505172d31dd2b4ef78048559f8", imageId);
        //myregistry.local:5000/testing/test-image
        dockerRequirement = new DockerRequirement();
        dockerRequirement.setDockerPull("myregistry.local:5000/testing/test-image");
        imageId = DockerCommandBuilder.findImageId(dockerRequirement);
        assertEquals("myregistry.local:5000/testing/test-image:latest", imageId);
        //myregistry.local:5000/testing/test-image:10.2
        dockerRequirement = new DockerRequirement();
        dockerRequirement.setDockerPull("myregistry.local:5000/testing/test-image:10.2");
        imageId = DockerCommandBuilder.findImageId(dockerRequirement);
        assertEquals("myregistry.local:5000/testing/test-image:10.2", imageId);
    }
}

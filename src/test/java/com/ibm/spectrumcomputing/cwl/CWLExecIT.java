package com.ibm.spectrumcomputing.cwl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.junit.Test;

public class CWLExecIT {

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;
        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }
        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }

    @Test
    public void startIT() throws Exception {
        final String sCurrentUser = System.getProperty("user.name");
        final String sCurrentPath = System.getProperty("user.dir") + "/src/test/integration-test";
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", "./run.sh");
        builder.directory(new File(sCurrentPath));
        System.out.println("Current Execution User: " + sCurrentUser);
        System.out.println("Current Execution Path: " + sCurrentPath);
        Process process = builder.start();
        StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobblerInput);
        StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobblerError);
        int exitCode = process.waitFor();
        System.out.println("exit code:" + exitCode);
        assert exitCode == 0;
    }
}

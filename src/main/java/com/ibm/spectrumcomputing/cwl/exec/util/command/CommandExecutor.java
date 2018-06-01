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
package com.ibm.spectrumcomputing.cwl.exec.util.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.model.instance.CWLScatterHolder;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

/**
 * Utility for executing a given command
 */
public class CommandExecutor {

    private CommandExecutor() {
    }

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

    /*
     * Reads the stdout and stderr of a command
     */
    private static final class STDIOReader extends Thread {
        private final String type;
        private final InputStream is;
        private final StringBuilder stdout;
        private final StringBuilder stderr;

        public STDIOReader(InputStream is, String type) {
            this.type = type;
            this.is = is;
            this.stdout = new StringBuilder();
            this.stderr = new StringBuilder();
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    if ("stdout".equals(type)) {
                        this.stdout.append(line);
                    } else {
                        this.stderr.append(line);
                    }
                }
            } catch (IOException e) {
                logger.error("Read command input failed", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }

        public String getStdout() {
            return stdout.toString();
        }

        public String getStderr() {
            return stderr.toString();
        }
    }

    /**
     * Execute a command with default execution environment
     * 
     * @param commands
     *            A command in array form
     * @return The command execution result
     */
    public static CommandExecutionResult run(List<String> commands) {
        return run(commands, null, null, -1);
    }

    /**
     * Execute a command with specified execution environment
     * 
     * @param commands
     *            A command in array form
     * @param customerEnv
     *            The specified execution environment
     * @return The command execution result
     */
    public static CommandExecutionResult run(List<String> commands, Map<String, String> customerEnv) {
        return run(commands, customerEnv, null, -1);
    }

    /**
     * Execute a command with specified execution environment and timeout
     * 
     * @param commands
     *            A command in array form
     * @param customerEnv
     *            The specified execution environment
     * @param timeout
     *            The specified execution timeout, in seconds
     * @return The command execution result
     */
    public static CommandExecutionResult run(List<String> commands, Map<String, String> customerEnv, int timeout) {
        return run(commands, customerEnv, null, timeout);
    }

    /**
     * Execute a command with specified execution environment and working
     * director
     * 
     * @param commands
     *            A command in array form
     * @param customerEnv
     *            The specified execution environment
     * @param workDir
     *            The specified working director
     * @return The command execution result
     */
    public static CommandExecutionResult run(List<String> commands, Map<String, String> customerEnv, Path workDir) {
        return run(commands, customerEnv, workDir, -1);
    }

    /**
     * Execute a command with customer settings
     * 
     * @param commands
     *            A command in array form
     * @param customerEnv
     *            The specified customer execution environment
     * @param workDir
     *            The specified command working director
     * @param timeout
     *            The specified command execution timeout in seconds
     * @return The command execution result
     */
    public static CommandExecutionResult run(List<String> commands,
            Map<String, String> customerEnv,
            Path workDir,
            int timeout) {
        Process proc = null;
        int exitCode = -1;
        boolean executed = true;
        String outMsg = null;
        String errMsg = null;
        try {
            ProcessBuilder procBuilder = new ProcessBuilder(commands);
            Map<String, String> env = procBuilder.environment();
            if (customerEnv != null) {
                for (Entry<String, String> envVar : customerEnv.entrySet()) {
                    env.put(envVar.getKey(), envVar.getValue());
                }
            }
            if (workDir != null) {
                procBuilder.directory(workDir.toFile());
            }
            proc = procBuilder.start();
            STDIOReader stdoutReader = new STDIOReader(proc.getInputStream(), "stdout");
            STDIOReader stderrReader = new STDIOReader(proc.getErrorStream(), "stderr");
            stdoutReader.start();
            stderrReader.start();
            boolean isTimeoutFailed = false;
            if (timeout != -1) {
                if (proc.waitFor(timeout, TimeUnit.SECONDS)) {
                    exitCode = proc.exitValue();
                } else {
                    executed = false;
                    isTimeoutFailed = true;
                    errMsg = ResourceLoader.getMessage("cwl.exec.command.execute.timeout");
                    logger.debug("Execute command {} failed (timeout={})", commands, timeout);
                }
            } else {
                proc.waitFor();
                exitCode = proc.exitValue();
            }

            if (!isTimeoutFailed) {
                stdoutReader.join();
                stderrReader.join();
                outMsg = stdoutReader.getStdout();
                errMsg = stderrReader.getStderr();
            }
        } catch (IOException e) {
            executed = false;
            errMsg = e.getMessage();
            logger.debug("Execute command {} failed ({}).", commands, errMsg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
        return new CommandExecutionResult(commands, executed, exitCode, outMsg, errMsg);
    }

    /**
     * Execute scatter commands
     *
     * @param scatterHolders
     *            The scatter holders that contains the scatter command
     * @return The command execution result
     */
    public static List<CommandExecutionResult> runScatter(List<CWLScatterHolder> scatterHolders) {
        return runScatter(scatterHolders, null, null);
    }

    /**
     * Execute scatter commands with specified execution environment
     *
     * @param scatterHolders
     *            The scatter holders that contains the scatter command
     * @param customerEnv
     *            The specified execution environment
     * @return The command execution result
     */
    public static List<CommandExecutionResult> runScatter(List<CWLScatterHolder> scatterHolders,
            Map<String, String> customerEnv) {
        return runScatter(scatterHolders, customerEnv, null);
    }

    /**
     * Execute scatter commands with specified execution environment and working
     * directory
     * 
     * @param scatterHolders
     *            The scatter holders that contains the scatter command
     * @param customerEnv
     *            The specified execution environment
     * @param workDir
     *            The specified command working directory
     * @return The command execution result
     */
    public static List<CommandExecutionResult> runScatter(List<CWLScatterHolder> scatterHolders,
            Map<String, String> customerEnv,
            Path workDir) {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ScatterTotalCommandTask task = new ScatterTotalCommandTask(scatterHolders, customerEnv, workDir);
        return forkJoinPool.invoke(task);
    }
}
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
package com.ibm.spectrumcomputing.cwl.exec;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.exec.service.CWLExecService;
import com.ibm.spectrumcomputing.cwl.exec.service.CWLServiceFactory;
import com.ibm.spectrumcomputing.cwl.exec.util.CWLExecUtil;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.persistence.CWLMainProcessRecord;
import com.ibm.spectrumcomputing.cwl.parser.util.IOUtil;
import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * Launches the {@link CWLExec} by different cwlexec command options
 */
public class CWLExecLauncher {

    private static final Path DEFAULT_WORKDIR = Paths.get(System.getProperty("user.home"), "cwl-workdir");
    private static final Path DEFAULT_OUTDIR = Paths.get(System.getProperty("user.dir"));
    
    private String owner;
    private String[] args;
    private Options options;
    private String execConfPath;
    private Map<Option, Integer> optionIndex = new HashMap<>();

    /**
     * Creates a launcher by default
     */
    public CWLExecLauncher() {
    }

    /**
     * Creates a launcher by cwlexec command options
     * 
     * @param cmdOptions
     *            the cwlexec command options
     */
    public CWLExecLauncher(String[] cmdOptions) {
        this.owner = System.getProperty("user.name");
        this.args = cmdOptions;
        this.options = this.getCommandOptions();
    }

    /**
     * Launches the {@link CWLExec} by different cwlexec command options
     */
    public void launchCWLExec() {
        if (this.args != null && this.args.length > 0) {
            try {
                CommandLine commandLine = this.parseCommands(this.options, this.args);
                if (commandLine.hasOption("r")) {
                    this.rerunCommand(commandLine);
                } else if (commandLine.hasOption("l")) {
                    this.listWorkflows(commandLine);
                } else if (commandLine.hasOption("h")) {
                    this.showHelp();
                } else if (commandLine.hasOption("v")) {
                    this.showVersion();
                } else {
                    this.runCommand(commandLine);
                }
            } catch (Exception e) {
                CWLExecUtil.printStderrMsg(String.format("%s%n", e.getMessage()));
                this.showHelp();
            }
        } else {
            this.showHelp();
        }
    }

    protected Options getCommandOptions() {
        Options cmdOptions = new Options();
        Option help = new Option("h", "help", false, ResourceLoader.getMessage("cwl.command.help.option"));
        optionIndex.put(help, 1);
        cmdOptions.addOption(help);
        Option workdir = Option.builder("w").longOpt("workdir").hasArg().argName("workDir")
                .desc(ResourceLoader.getMessage("cwl.command.workdir.option", DEFAULT_WORKDIR.toString()))
                .build();
        optionIndex.put(workdir, 2);
        cmdOptions.addOption(workdir);
        Option outdir = Option.builder("o").longOpt("outdir").hasArg().argName("outputsDir")
                .desc(ResourceLoader.getMessage("cwl.command.output.option", DEFAULT_OUTDIR.toString()))
                .build();
        optionIndex.put(outdir, 3);
        cmdOptions.addOption(outdir);
        Option execConfig = Option.builder("c").longOpt("exec-config").hasArg().argName("configPath")
                .desc(ResourceLoader.getMessage("cwl.command.config.option")).build();
        optionIndex.put(execConfig, 4);
        cmdOptions.addOption(execConfig);
        Option rerun = Option.builder("r").longOpt("rerun").hasArg().argName("workflowId")
                .desc(ResourceLoader.getMessage("cwl.command.rerun.option")).build();
        optionIndex.put(rerun, 5);
        cmdOptions.addOption(rerun);
        Option quiet = new Option("q", "quiet", false, ResourceLoader.getMessage("cwl.command.quiet.option"));
        optionIndex.put(quiet, 6);
        cmdOptions.addOption(quiet);
        Option debug = new Option("X", "debug", false, ResourceLoader.getMessage("cwl.command.logger.option"));
        optionIndex.put(debug, 7);
        cmdOptions.addOption(debug);
        Option list = Option.builder("l").longOpt("list").hasArg(false).argName("workflowId")
                .desc(ResourceLoader.getMessage("cwl.command.list.option")).build();
        optionIndex.put(list, 8);
        cmdOptions.addOption(list);
        Option version = new Option("v", "version", false, ResourceLoader.getMessage("cwl.command.version.option"));
        optionIndex.put(version, 9);
        cmdOptions.addOption(version);
        Option link = new Option("L", "link", false, ResourceLoader.getMessage("cwl.command.linkinput.option"));
        optionIndex.put(link, 10);
        cmdOptions.addOption(link);
        return cmdOptions;
    }

    private CommandLine parseCommands(Options options, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption("o")) {
            handleOutdirOption(commandLine);
        }
        if (commandLine.hasOption("w")) {
            handleWorkdirOption(commandLine);
        }
        if (commandLine.hasOption("c")) {
            handleExecConfigOption(commandLine);
        }
        if (commandLine.hasOption("X")) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = loggerContext.getLogger("com.ibm.spectrumcomputing.cwl");
            logger.setLevel(Level.DEBUG);
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.detachAndStopAllAppenders();
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("%d{HH:mm:ss.SSS} %contextName [%thread] %-5level %logger{36} - %msg%n");
            encoder.start();
            ConsoleAppender<ILoggingEvent> debugAppender = new ConsoleAppender<>();
            debugAppender.setContext(loggerContext);
            debugAppender.setEncoder(encoder);
            debugAppender.start();
            root.addAppender(debugAppender);
        }
        if (commandLine.hasOption("q")) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger("com.ibm.spectrumcomputing.cwl").setLevel(Level.WARN);
        }
        if (commandLine.hasOption("L")) {
            System.setProperty(IOUtil.USING_SYMBOL_LINK, "True");
        }
        if (System.getProperty(IOUtil.WORK_TOP_DIR) == null) {
            System.setProperty(IOUtil.WORK_TOP_DIR, mkDefaultWorkDir());
        }
        if (System.getProperty(IOUtil.OUTPUT_TOP_DIR) == null) {
            System.setProperty(IOUtil.OUTPUT_TOP_DIR, DEFAULT_OUTDIR.toString());
        }
        return commandLine;
    }

    private void handleOutdirOption(CommandLine commandLine) {
        String outputTopDir = commandLine.getOptionValue("o");
        if (outputTopDir == null) {
            CWLExecUtil.printStderrMsg(ResourceLoader.getMessage("cwl.outputdir.empty"));
            System.exit(255);
        }
        File outputTop = new File(outputTopDir);
        if (!outputTop.exists()) {
            CWLExecUtil.printStderrMsg(ResourceLoader.getMessage("cwl.outputdir.nonexistent", outputTopDir));
            System.exit(255);
        }
        System.setProperty(IOUtil.OUTPUT_TOP_DIR, outputTopDir);
    }

    private void handleWorkdirOption(CommandLine commandLine) {
        String workTopDir = commandLine.getOptionValue("w");
        if (workTopDir == null) {
            CWLExecUtil.printStderrMsg(ResourceLoader.getMessage("cwl.workdir.empty"));
            System.exit(255);
        }
        File workTop = new File(workTopDir);
        if (!workTop.exists()) {
            CWLExecUtil.printStderrMsg(ResourceLoader.getMessage("cwl.workdir.nonexistent", workTopDir));
            System.exit(255);
        }
        System.setProperty(IOUtil.WORK_TOP_DIR, workTopDir);
    }

    private void handleExecConfigOption(CommandLine commandLine) {
        String confPathValue = commandLine.getOptionValue("c");
        if (confPathValue == null) {
            CWLExecUtil.printStderrMsg(ResourceLoader.getMessage("cwl.configuration.empty"));
            System.exit(255);
        }
        Path confPath = Paths.get(confPathValue.trim());
        if (!confPath.isAbsolute()) {
            confPath = Paths.get(System.getProperty("user.dir"), confPathValue.trim());
        }
        File confFile = confPath.toFile();
        if (!confFile.exists()) {
            CWLExecUtil.printStderrMsg(
                    ResourceLoader.getMessage("cwl.configuration.nonexistent", confFile.getAbsoluteFile()));
            System.exit(255);
        }
        this.execConfPath = confFile.getAbsolutePath();
    }

    private String mkDefaultWorkDir() {
        try {
            IOUtil.mkdirs(System.getProperty("user.name"), DEFAULT_WORKDIR);
        } catch (CWLException e) {
            CWLExecUtil.printStderrMsg(e.getMessage());
            System.exit(255);
        }
        return DEFAULT_WORKDIR.toString();
    }

    private void listWorkflows(CommandLine commandLine) {
        CWLExec.cwlexec().start();
        CWLExecService engineService = CWLServiceFactory.getService(CWLExecService.class);
        List<String> argList = commandLine.getArgList();
        if(argList != null && argList.size() == 1){
            CWLMainProcessRecord wfRecord = engineService.findWorkflow(argList.get(0));
            if (wfRecord != null) {
                outputForY(wfRecord);
                System.exit(0);
            } else {
                CWLExecUtil.printStderrMsg(ResourceLoader.getMessage("cwl.workflow.not.found", argList.get(0)));
                System.exit(1);
            }
        } else{
            List<CWLMainProcessRecord> wfRecords = engineService.findFinishedCWLProcesses();
            if (wfRecords != null && !wfRecords.isEmpty()) {
                outputForX(wfRecords);
                System.exit(0);
            } else {
                CWLExecUtil.printStderrMsg(ResourceLoader.getMessage("cwl.unfinished.workflows.not.found"));
                System.exit(1);
            }
        }
        CWLExec.cwlexec().stop(true);
    }

    private void runCommand(CommandLine commandLine) {
        List<String> argList = commandLine.getArgList();
        int exitCode = 255;
        if (argList != null) {
            try {
                CWLExec.cwlexec().start();
                CWLExecService engineService = CWLServiceFactory.getService(CWLExecService.class);
                if (argList.size() == 1) {
                    bindShutdownHook();
                    exitCode = engineService.submit(owner, argList.get(0), null, this.execConfPath).getExitCode();
                } else if (argList.size() == 2) {
                    bindShutdownHook();
                    exitCode = engineService.submit(owner, argList.get(0), argList.get(1), this.execConfPath)
                            .getExitCode();
                } else {
                    this.printHelp();
                }
            } catch (CWLException ce) {
                CWLExecUtil.printStderrMsg(ce.getMessage());
                exitCode = ce.getExceptionCode();
            } catch (Exception e) {
                CWLExecUtil.printStderrMsg(e.getMessage());
            }
        } else {
            this.printHelp();
        }
        System.exit(exitCode);
    }

    private void rerunCommand(CommandLine commandLine) {
        int exitCode = 255;
        String workflowId = commandLine.getOptionValue("r");
        try {
            CWLExec.cwlexec().start();
            CWLExecService engineService = CWLServiceFactory.getService(CWLExecService.class);
            if (engineService.hasRunningJobs(workflowId)) {
                CWLExecUtil.printStderrMsg(ResourceLoader.getMessage("cwl.exec.workflow.rerun.tip"));
                Scanner sc = new Scanner(System.in);
                String answer = sc.nextLine();
                if (!"Y".equalsIgnoreCase(answer)) {
                    sc.close();
                    System.exit(exitCode);
                } else {
                    sc.close();
                }
            }
            bindShutdownHook();
            exitCode = engineService.rerun(workflowId).getExitCode();
        } catch (CWLException e) {
            CWLExecUtil.printStderrMsg(e.getMessage());
            exitCode = e.getExceptionCode();
        }
        System.exit(exitCode);
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator((o1, o2) -> optionIndex.get(o1) - optionIndex.get(o2));
        formatter.printHelp(200, ResourceLoader.getMessage("cwl.command.usage"), null, options, null);
    }

    private void outputForX(List<CWLMainProcessRecord> wfRecords) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = "%-40s %-20s %-20s %-20s %-20s %-10s %-20s %n";
        Object[] columns = {
                ResourceLoader.getMessage("cwl.command.column.id"),
                ResourceLoader.getMessage("cwl.command.column.name"),
                ResourceLoader.getMessage("cwl.command.column.submit.time"),
                ResourceLoader.getMessage("cwl.command.column.start.time"),
                ResourceLoader.getMessage("cwl.command.column.end.time"),
                ResourceLoader.getMessage("cwl.command.column.state"),
                ResourceLoader.getMessage("cwl.command.column.exit.code")
        };
        // print the column at the first line
        CWLExecUtil.formatPrint(format, columns);
        // print all of the work flow records
        if (wfRecords != null) {
            for (CWLMainProcessRecord wfRecord : wfRecords) {
                Long submitTime = wfRecord.getSubmitTime();
                Long startTime = wfRecord.getStartTime();
                Long endTime = wfRecord.getEndTime();
                String submitTimeStr = (submitTime != null) ? dateFormat.format(submitTime) : "";
                String startTimeStr = (startTime != null) ? dateFormat.format(startTime) : "";
                String endTimeStr = (endTime != null) ? dateFormat.format(endTime) : "";
                CWLExecUtil.formatPrint(format, wfRecord.getId(), wfRecord.getName(), submitTimeStr, startTimeStr, endTimeStr,
                        wfRecord.getState(), wfRecord.getExitCode());
            }
        }
    }

    private void outputForY(CWLMainProcessRecord wfRecord) {
        String columnFormat = "%1$20s: %2$s %n";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long submitTime = wfRecord.getSubmitTime();
        Long startTime = wfRecord.getStartTime();
        Long endTime = wfRecord.getEndTime();
        String submitTimeStr = (submitTime != null) ? dateFormat.format(submitTime) : "";
        String startTimeStr = (startTime != null) ? dateFormat.format(startTime) : "";
        String endTimeStr = (endTime != null) ? dateFormat.format(endTime) : "";
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.id"), wfRecord.getId());
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.name"), wfRecord.getName());
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.submit.time"), submitTimeStr);
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.start.time"), startTimeStr);
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.end.time"), endTimeStr);
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.state"), wfRecord.getState());
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.exit.code"), wfRecord.getExitCode());
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.work.dir"), wfRecord.getWorkDir());
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.output.dir"), wfRecord.getOutputsDir());
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.desc.path"), wfRecord.getDescPath());
        CWLExecUtil.formatPrint(columnFormat, ResourceLoader.getMessage("cwl.command.column.input.settings"),
                wfRecord.getInputsPath());
    }


    private void showHelp() {
        printHelp();
        System.exit(1);
    }

    private void showVersion() {
        CWLExecUtil.printStderrMsg("0.1");
        System.exit(0);
    }

    private void bindShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> CWLExec.cwlexec().stop(false)));
    }
}

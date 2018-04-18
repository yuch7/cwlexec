package com.ibm.spectrumcomputing.cwl.exec;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.exec.CWLExecLauncher;

public class CWLEngineLauncherTest extends CWLExecTestBase {

    public CommandLineParser parser = new DefaultParser();

    @Test
    public void parseCommands0() {
        try {
            CWLExecLauncher launcher = new CWLExecLauncher();
            Options options = launcher.getCommandOptions();
            // case 0
            String[] args = new String[3];
            args[0] = "run";
            args[1] = "/opt/cwl/workflow.cwl";
            args[2] = "/opt/cwl/workflow.settings";
            CommandLine commandLine = parser.parse(options, args);
            List<String> argList = commandLine.getArgList();
            Assert.assertNotNull(argList);
            Assert.assertEquals(3, argList.size());
            Assert.assertFalse(commandLine.hasOption("o"));
            Assert.assertNull(commandLine.getOptionValue("o"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void parseCommands1() {
        try {
            CWLExecLauncher launcher = new CWLExecLauncher();
            Options options = launcher.getCommandOptions();
            // case 0
            String[] args = new String[1];
            args[0] = "run";
            CommandLine commandLine = parser.parse(options, args);
            List<String> argList = commandLine.getArgList();
            Assert.assertNotNull(argList);
            Assert.assertEquals(1, argList.size());
            Assert.assertFalse(commandLine.hasOption("o"));
            Assert.assertNull(commandLine.getOptionValue("o"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void parseCommands2() {
        try {
            CWLExecLauncher launcher = new CWLExecLauncher();
            Options options = launcher.getCommandOptions();
            // case 0
            String[] args = new String[4];
            args[0] = "run";
            args[1] = "-o /opt/cw/workflow.output";
            args[2] = "/opt/cwl/workflow.cwl";
            args[3] = "/opt/cwl/workflow.settings";
            CommandLine commandLine = parser.parse(options, args);
            List<String> argList = commandLine.getArgList();
            Assert.assertNotNull(argList);
            Assert.assertEquals(3, argList.size());
            Assert.assertTrue(commandLine.hasOption("o"));
            Assert.assertNotNull(commandLine.getOptionValue("o"));
            Assert.assertEquals("/opt/cw/workflow.output", commandLine.getOptionValue("o").trim());
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void parseCommands3() {
        try {
            CWLExecLauncher launcher = new CWLExecLauncher();
            Options options = launcher.getCommandOptions();
            // case 0
            String[] args = new String[5];
            args[0] = "run";
            args[1] = "-w /opt/cw/workflow.workdir";
            args[2] = "-o /opt/cw/workflow.output";
            args[3] = "/opt/cwl/workflow.cwl";
            args[4] = "/opt/cwl/workflow.settings";
            CommandLine commandLine = parser.parse(options, args);
            List<String> argList = commandLine.getArgList();
            Assert.assertNotNull(argList);
            Assert.assertEquals(3, argList.size());
            Assert.assertTrue(commandLine.hasOption("o"));
            Assert.assertTrue(commandLine.hasOption("w"));
            Assert.assertNotNull(commandLine.getOptionValue("o"));
            Assert.assertNotNull(commandLine.getOptionValue("w"));
            Assert.assertEquals("/opt/cw/workflow.output", commandLine.getOptionValue("o").trim());
            Assert.assertEquals("/opt/cw/workflow.workdir", commandLine.getOptionValue("w").trim());
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void parseCommands4() {
        try {
            CWLExecLauncher launcher = new CWLExecLauncher();
            Options options = launcher.getCommandOptions();
            // case 0
            String[] args = new String[1];
            args[0] = "list";
            CommandLine commandLine = parser.parse(options, args);
            List<String> argList = commandLine.getArgList();
            Assert.assertNotNull(argList);
            Assert.assertEquals(1, argList.size());
            Assert.assertNull(commandLine.getOptionValue("o"));
            Assert.assertNull(commandLine.getOptionValue("w"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void parseCommands5() {
        try {
            CWLExecLauncher launcher = new CWLExecLauncher();
            Options options = launcher.getCommandOptions();
            // case 0
            String[] args = new String[2];
            args[0] = "info";
            args[1] = "12345";
            CommandLine commandLine = parser.parse(options, args);
            List<String> argList = commandLine.getArgList();
            Assert.assertNotNull(argList);
            Assert.assertEquals(2, argList.size());
            Assert.assertNull(commandLine.getOptionValue("o"));
            Assert.assertNull(commandLine.getOptionValue("w"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void parseCommands6() {
        try {
            CWLExecLauncher launcher = new CWLExecLauncher();
            Options options = launcher.getCommandOptions();
            // case 0
            String[] args = new String[2];
            args[0] = "-l";
            args[1] = "12345";
            CommandLine commandLine = parser.parse(options, args);
            List<String> argList = commandLine.getArgList();
            Assert.assertNotNull(argList);
            Assert.assertEquals(1, argList.size());
            Assert.assertEquals("12345", argList.get(0));
            Assert.assertTrue(commandLine.hasOption("l"));
            Assert.assertNull(commandLine.getOptionValue("w"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void parseCommands7() {
        try {
            CWLExecLauncher launcher = new CWLExecLauncher();
            Options options = launcher.getCommandOptions();
            // case 0
            String[] args = new String[1];
            args[0] = "-l";
            CommandLine commandLine = parser.parse(options, args);
            List<String> argList = commandLine.getArgList();
            Assert.assertNotNull(argList);
            Assert.assertEquals(0, argList.size());
            Assert.assertTrue(commandLine.hasOption("l"));
            Assert.assertNull(commandLine.getOptionValue("w"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }
}

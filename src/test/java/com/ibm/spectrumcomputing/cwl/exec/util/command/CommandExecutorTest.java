package com.ibm.spectrumcomputing.cwl.exec.util.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutionResult;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutor;
import com.ibm.spectrumcomputing.cwl.model.instance.CWLScatterHolder;

public class CommandExecutorTest extends CWLExecTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutorTest.class);

    @Test
    public void runBadCommand() {
        List<String> commands = new ArrayList<>();
        commands.add("badcommand");
        CommandExecutionResult result = CommandExecutor.run(commands);
        assertFalse(result.isExecuted());
    }

    @Test
    public void runCommandWithShellMetacharacters() {
        if (!is_win) {
            List<String> commands = Arrays.asList("echo", "test test");
            CommandExecutionResult result = CommandExecutor.run(commands);
            assertEquals("test test", result.getOutMsg());
        } else {
            logger.warn("The CommandExecutorTest#runCommandWithShellMetacharacters is unsupported on Windows");
        }
    }

    @Test
    public void runWinEcho() {
        if (is_win) {
            List<String> commands = new ArrayList<>();
            commands.add("cmd.exe");
            commands.add("/c");
            commands.add("echo");
            commands.add("\"test\"");
            CommandExecutionResult result = CommandExecutor.run(commands);
            assertTrue(result.isExecuted());
            assertEquals(0, result.getExitCode());
         } else {
             logger.warn("The CommandExecutorTest#runWinEcho is unsupported on Unix");
         }
    }

    @Test
    public void runSort() {
        if (!is_win) {
            List<String> commands = Arrays.asList("sort",
                    DEF_ROOT_PATH + "files/SRR1031972.bedGraph", 
                    "-k",
                    "1,1",
                    "-k",
                    "2,2n");
            CommandExecutionResult result = CommandExecutor.run(commands);
            assertTrue(result.isExecuted());
            assertEquals(0, result.getExitCode());
        } else {
            logger.warn("The CommandExecutorTest#runSort is unsupported on Windows");
        }
    }

    @Test
    public void runUnixId() {
        if (!is_win) {
            List<String> commands = Arrays.asList("id", "-u");
            CommandExecutionResult result = CommandExecutor.run(commands);
            assertTrue(result.isExecuted());
            assertEquals(0, result.getExitCode());
        } else {
            logger.warn("The CommandExecutorTest#runUnixId is unsupported on Windows");
        }
    }

    @Test
    public void runScatterCommands() {
        if (!is_win) {
            List<CWLScatterHolder> scatterHolders = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                List<String> commands = Arrays.asList("echo", "test", String.valueOf(i));
                CWLScatterHolder scatterHolder = new CWLScatterHolder();
                scatterHolder.setCommand(commands);
                scatterHolders.add(scatterHolder);
            }
            List<CommandExecutionResult> results = CommandExecutor.runScatter(scatterHolders);
            assertEquals("test 0", results.get(0).getOutMsg());
            assertEquals("test 1", results.get(1).getOutMsg());
            assertEquals("test 2", results.get(2).getOutMsg());
            assertEquals("test 3", results.get(3).getOutMsg());
        } else {
            logger.warn("The CommandExecutorTest#runScatterCommands is unsupported on Windows");
        }
    }
}

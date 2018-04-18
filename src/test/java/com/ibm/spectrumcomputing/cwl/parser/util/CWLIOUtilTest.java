package com.ibm.spectrumcomputing.cwl.parser.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutionResult;
import com.ibm.spectrumcomputing.cwl.exec.util.command.CommandExecutor;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;

public class CWLIOUtilTest extends CWLExecTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CWLIOUtilTest.class);

    @Test
    public void removeFileExt() {
        String primary = "test0.test1.test2";
        String secondary = "^^.2.ebwt";
        String removed = IOUtil.removeFileExt(primary, secondary);
        assertEquals("test0.2.ebwt", removed);
        primary = "test0.test1";
        secondary = "^.ebwt";
        removed = IOUtil.removeFileExt(primary, secondary);
        assertEquals("test0.ebwt", removed);
        primary = "test0.test1";
        secondary = "^^.2.ebwt";
        removed = IOUtil.removeFileExt(primary, secondary);
        assertEquals("test0.2.ebwt", removed);
    }

    @Test
    public void splitDescFilePath() {
        String descFilePath0 = "/home/weliu/test/test.cwl";
        String[] parts = IOUtil.splitDescFilePath(descFilePath0);
        assertEquals(descFilePath0, parts[0]);
        assertNull(parts[1]);
        String descFilePath1 = "/home/weliu/test/test.cwl#main";
        parts =  IOUtil.splitDescFilePath(descFilePath1);
        assertEquals(descFilePath0, parts[0]);
        assertEquals("main", parts[1]);
        String descFilePath2 = "http://test/home/weliu/test/test#test.cwl#main";
        parts =  IOUtil.splitDescFilePath(descFilePath2);
        assertEquals("http://test/home/weliu/test/test#test.cwl", parts[0]);
        assertEquals("main", parts[1]);
    }

    @Test
    public void glob() {
        if (!is_win) {
            String filePath = "*.cwl";
            List<Path> matched = IOUtil.glob(filePath, Paths.get(DEF_ROOT_PATH));
            assertEquals(47, matched.size());
            filePath = "input-*.cwl";
            matched = IOUtil.glob(filePath, Paths.get(DEF_ROOT_PATH));
            assertEquals(5, matched.size());
            filePath = "./inputs/input-types.cwl";
            matched = IOUtil.glob(filePath, Paths.get(DEF_ROOT_PATH));
            assertEquals(1, matched.size());
            matched = IOUtil.glob("files/**", Paths.get(DEF_ROOT_PATH + "files"));
            assertEquals(14, matched.size());
        } else {
            logger.warn("The glob method cannot be supported on Windows.");
        }
    }

//    @Test
//    public void downloadRemoteCwlFile() throws CWLException {
//        String filePath = "https://raw.githubusercontent.com/common-workflow-language/common-workflow-language/master/v1.0/examples/tar.cwl";
//        String tmpDir = runtime.get(CWLUtil.RUNTIME_TMP_DIR);
//        File file = CWLIOUtil.yieldFile(filePath, tmpDir, new String[] { ".cwl" }, false);
//        assertNotNull(file);
//        assertTrue(file.exists());
//        assertEquals("tar.cwl", file.getName());
//    }
//
//    @Test
//    public void downloadAndRenameRemoteFile() throws CWLException {
//        String filePath = "https://raw.githubusercontent.com/common-workflow-language/common-workflow-language/master/v1.0/examples/tar-job.yml";
//        String tmpDir = runtime.get(CWLUtil.RUNTIME_TMP_DIR);
//        File file = CWLIOUtil.yieldFile(filePath, tmpDir, null, true);
//        assertNotNull(file);
//        assertTrue(file.exists());
//        assertNotEquals("tar-job.yml", file.getName());
//    }
//
//    @Test
//    public void downloadRemoteFileWithExtConstraints() throws CWLException {
//        String filePath = "https://raw.githubusercontent.com/common-workflow-language/common-workflow-language/master/v1.0/examples/tar-job.yml";
//        String tmpDir = runtime.get(CWLUtil.RUNTIME_TMP_DIR);
//        thrown.expect(CWLException.class);
//        thrown.expectMessage("The file");
//        CWLIOUtil.yieldFile(filePath, tmpDir, new String[] { ".json", ".cwl" }, true);
//    }

    @Test
    public void findFileNameRoot() {
        String name = IOUtil.findFileNameRoot("/user/test/test.cwl");
        assertEquals("test", name);
        name = IOUtil.findFileNameRoot("/user/test/test1.cwl#main");
        assertEquals("test1", name);
    }

    @Test
    public void createCommandScript() throws CWLException, IOException {
        if (is_win) {
            logger.warn("The CWLIOUtilTest#createCommandScript is unsupported on Windows");
            return;
        }
        Path scriptPath = Paths.get(System.getProperty("java.io.tmpdir"), CommonUtil.getRandomStr());
        assertFalse(Files.exists(scriptPath));
        IOUtil.createCommandScript(scriptPath, null);
        assertTrue(Files.exists(scriptPath));
        IOUtil.createCommandScript(scriptPath, "echo \"test\"");
        CommandExecutionResult result = CommandExecutor.run(Arrays.asList(scriptPath.toString()));
        assertEquals(0, result.getExitCode());
        assertEquals("test", result.getOutMsg());
        Files.delete(scriptPath);
    }

    @Test
    public void readLSFOutputFile() {
        if (is_win) {
            logger.warn("The CWLIOUtilTest#readLSFOutputFile is unsupported on Windows");
            return;
        }
        Path outFile = Paths.get(DEF_ROOT_PATH + "files/640_out");
        StringBuilder sb = IOUtil.readLSFOutputFile(outFile);
        assertEquals(1320, sb.length());
    }
}

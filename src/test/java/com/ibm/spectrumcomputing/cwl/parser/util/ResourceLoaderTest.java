package com.ibm.spectrumcomputing.cwl.parser.util;

import static org.junit.Assert.*;
import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.parser.util.ResourceLoader;

public class ResourceLoaderTest {

    @Test
    public void getMessageWithoutKey() {
        String msg = ResourceLoader.getMessage(null);
        assertNull(msg);
        msg = ResourceLoader.getMessage("");
        assertEquals("", msg);
    }

    @Test
    public void getNonexistentMessage() {
        String msg = ResourceLoader.getMessage("nonexistent.msg");
        assertEquals("nonexistent.msg", msg);
    }

    @Test
    public void getMessageWithArguments() {
        String msg = ResourceLoader.getMessage("cwl.parser.fail.to.process", "test1", "test2");
        assertEquals("Failed to process CWL description file \"test1\", test2.", msg);
    }
}

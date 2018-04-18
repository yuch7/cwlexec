package com.ibm.spectrumcomputing.cwl.parser.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ibm.spectrumcomputing.cwl.CWLExecTestBase;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

public class CWLUtilTest extends CWLExecTestBase {

    @Test
    public void asJsonStr() {
        String json = CommonUtil.asJsonStr(null, null);
        assertNull(json);
        json = CommonUtil.asJsonStr("test", null);
        assertEquals("{\"test\":null}", json);
        json = CommonUtil.asJsonStr("test", "test");
        assertEquals("{\"test\":\"test\"}", json);
    }

    @Test
    public void getRandomStr() {
        List<String> randoms = new ArrayList<>();
        boolean repeated = false;
        for (int i = 0; i < 10000; i++) {
            String random = CommonUtil.getRandomStr();
            if (!randoms.contains(random)) {
                randoms.add(random);
            } else {
                repeated = true;
                break;
            }
        }
        assertFalse(repeated);
    }
}

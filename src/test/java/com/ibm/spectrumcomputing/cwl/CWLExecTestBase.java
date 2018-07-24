package com.ibm.spectrumcomputing.cwl;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.parser.util.CommonUtil;

public abstract class CWLExecTestBase {

    private final static String RESOURCE_PATH = CWLExecTestBase.class.getResource("/").getPath();

    protected final static String DEF_ROOT_PATH = RESOURCE_PATH + "definitions/";
    protected final static String CONFORMANCE_PATH = RESOURCE_PATH + "conformance/";
    protected final static Map<String, String> runtime = new HashMap<>();
    protected final String owner = System.getProperty("user.name");
    protected final boolean is_win = (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1);

    static {
        runtime.put(CommonUtil.RUNTIME_OUTPUT_DIR, System.getProperty("java.io.tmpdir") + "/test");
        runtime.put(CommonUtil.RUNTIME_TMP_DIR, System.getProperty("java.io.tmpdir") + "/test");
        runtime.put("cores", "1");
        runtime.put("ram", "0");
        File tmpDir = new File(runtime.get(CommonUtil.RUNTIME_TMP_DIR));
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected CWLParameter findParameter(String id, List<? extends CWLParameter> params) {
        CWLParameter parameter = null;
        if (id != null && params != null) {
            for (CWLParameter p : params) {
                if (id.equals(p.getId())) {
                    parameter = p;
                    break;
                }
            }
        }
        return parameter;
    }

    protected Properties testDatabaseConfig() {
        Properties properties = new Properties();
        properties.put("hibernate.connection.url", "jdbc:h2:mem:cwlengine-test");
        properties.put("hibernate.connection.username", "sa");
        properties.put("hibernate.connection.password", "");
        properties.put("hibernate.connection.driver_class", "org.h2.Driver");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        return properties;
    }
}

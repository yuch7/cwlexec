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
package com.ibm.spectrumcomputing.cwl.parser.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.CWLParameter;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.NullValue;

/**
 * Utility methods for
 * <ul> 
 *  <li>Formating an object to JSON string</li>
 *  <li>Finding a parameter</li>
 *  <li>Producing a random number</li>
 * </ul>
 */
public class CommonUtil {

    /**
     * The key for runtime outdir
     */
    public static final String RUNTIME_OUTPUT_DIR = "outdir";
    /**
     * The key for runtime tmpdir
     */
    public static final String RUNTIME_TMP_DIR = "tmpdir";

    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    private CommonUtil() {
    }

    /**
     * Returns a JSON string by given key and object
     * 
     * @param key
     *            the key of the object
     * @param obj
     *            an object
     * @return a JSON string represents the key and object
     */
    public static String asJsonStr(String key, Object obj) {
        String jsonStr = null;
        if (key != null && key.length() != 0) {
            Map<String, Object> propertis = new HashMap<>();
            if (NullValue.NULL.equals(obj)) {
                propertis.put(key, null);
            } else {
                propertis.put(key, obj);
            }
            try {
                jsonStr = (new ObjectMapper()).writeValueAsString(propertis);
            } catch (JsonProcessingException e) {
                logger.warn("Cannot process the {}:{} to json string.", key, obj);
            }
        }
        return jsonStr;
    }

    /**
     * Returns a JSON string by given object
     * 
     * @param obj
     *            an object
     * @return a JSON string represents the object
     */
    public static String asJsonStr(Object obj) {
        String jsonStr = null;
        if (obj != null) {
            try {
                jsonStr = (new ObjectMapper()).writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                logger.warn("Cannot process the {} to json string.", obj);
            }
        }
        return jsonStr;
    }

    /**
     * Returns a JSON string with pretty format by given object
     * 
     * @param obj
     *            an object
     * @return A pretty JSON string represents the object
     */
    public static String asPrettyJsonStr(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.warn("Cannot process the {} to json string, {}", obj.getClass().getName(), e.getMessage());
        }
        return null;
    }

    /**
     * Find a CWL parameter by given id
     * 
     * @param id
     *            A given parameter id
     * @param params
     *            The CWL parameters (inputs or outputs)
     * @return If the parameter is not found, a null value will be returned
     */
    public static CWLParameter findParameter(String id, List<? extends CWLParameter> params) {
        CWLParameter parameter = null;
        if (id != null && params != null) {
            id = id.startsWith("#") ? id.substring(1) : id;
            for (CWLParameter p : params) {
                String parameterId = p.getId();
                if (parameterId != null && id.equals(resovleParameterId(parameterId))) {
                    parameter = p;
                    break;
                }
            }
        }
        return parameter;
    }

    /**
     * Produce the random string and make sure there is no duplicate string
     * after run 10000 times without interruption
     * 
     * @return A random string
     */
    public static String getRandomStr() {
        return Long.toHexString(System.nanoTime()).substring(3);
    }

    private static String resovleParameterId(String parameterId) {
        return parameterId.startsWith("#") ? parameterId.substring(1) : parameterId;
    }
}

/*
 * Copyright International Business Machines Corp, 2018.
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
package com.ibm.spectrumcomputing.cwl.exec.util.evaluator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;

/*
 * Presents the JavaScript expression result value
 */
class JSResultWrapper {

    private static final Logger logger = LoggerFactory.getLogger(JSResultWrapper.class);

    /*
     * See
     * https://docs.oracle.com/javase/8/docs/jdk/api/nashorn/jdk/nashorn/api/
     * scripting/ScriptObjectMirror.html
     */
    private static final String SCRIPT_OBJECT_MIRROR_CLASSNAME = "jdk.nashorn.api.scripting.ScriptObjectMirror";
    private static final String CLASS = "class";
    private static final String IS_EXTENSIBLE = "isExtensible";
    private static final String IS_ARRAY = "isArray";
    private final Object result;

    enum ResultType {
        STRING, LONG, DOUBLE, BOOL, ARRAY, OBJECT, NULL, UNKNOWN
    }

    protected JSResultWrapper(Object result) {
        this.result = result;
    }

    protected ResultType getType() {
        if (this.result == null) {
            return ResultType.NULL;
        } else if (this.result instanceof String) {
            return ResultType.STRING;
        } else if (this.result instanceof Integer || this.result instanceof Long) {
            return ResultType.LONG;
        } else if (this.result instanceof Float || this.result instanceof Double) {
            return ResultType.DOUBLE;
        } else if (this.result instanceof Boolean) {
            return ResultType.BOOL;
        } else if (invokeBoolMethod(this.result, IS_ARRAY)) {
            return ResultType.ARRAY;
        } else if (invokeBoolMethod(this.result, IS_EXTENSIBLE)) {
            return ResultType.OBJECT;
        } else {
            return ResultType.UNKNOWN;
        }
    }

    protected boolean isNull() {
        return this.result == null || invokeBoolMethod(this.result, "isEmpty");
    }

    protected boolean isString() {
        return (this.result instanceof String);
    }

    protected String asString() {
        return (String) this.result;
    }

    protected boolean isCWLFile() {
        boolean isCWLFile = false;
        if (!this.isNull() && this.isObject()) {
            for (String key : this.keys()) {
                if (CLASS.equals(key)) {
                    isCWLFile = "File".equals(this.getValue(key).asString());
                    break;
                }
            }
        }
        return isCWLFile;
    }

    protected CWLFile asCWLFile() throws CWLException {
        CWLFile cwlFile = new CWLFile();
        for (String key : this.keys()) {
            switch (key) {
            case "location":
                cwlFile.setLocation(this.getValue(key).asString());
                break;
            case "path":
                cwlFile.setPath(this.getValue(key).asString());
                break;
            case "basename":
                cwlFile.setBasename(this.getValue(key).asString());
                break;
            case "dirname":
                cwlFile.setDirname(this.getValue(key).asString());
                break;
            case "nameroot":
                cwlFile.setNameroot(this.getValue(key).asString());
                break;
            case "nameext":
                cwlFile.setNameext(this.getValue(key).asString());
                break;
            case "checksum":
                cwlFile.setChecksum(this.getValue(key).asString());
                break;
            case "secondaryFiles":
                cwlFile.setSecondaryFiles(new ArrayList<>());
                for (JSResultWrapper list : this.getValue(key).elements()) {
                    if (list.isCWLFile()) {
                        cwlFile.getSecondaryFiles().add(list.asCWLFile());
                    } else if (list.isCWLDirectory()) {
                        cwlFile.getSecondaryFiles().add(list.asCWLDirectory());
                    }
                }
                break;
            case "size":
                cwlFile.setSize(this.getValue(key).asLong());
                break;
            case "format":
                cwlFile.setFormat(this.getValue(key).asString());
                break;
            case "contents":
                cwlFile.setContents(this.getValue(key).asString());
                break;
            default:
                break;
            }
        }
        return cwlFile;
    }

    protected boolean isCWLDirectory() {
        boolean isCWLDir = false;
        if (!this.isNull() && this.isObject()) {
            for (String key : this.keys()) {
                if (CLASS.equals(key)) {
                    isCWLDir = "Directory".equals(this.getValue(key).asString());
                    break;
                }
            }
        }
        return isCWLDir;
    }

    protected CWLDirectory asCWLDirectory() throws CWLException {
        CWLDirectory cwlDirectory = new CWLDirectory();
        for (String key : this.keys()) {
            switch (key) {
            case "location":
                cwlDirectory.setLocation(this.getValue(key).asString());
                break;
            case "path":
                cwlDirectory.setPath(this.getValue(key).asString());
                break;
            case "basename":
                cwlDirectory.setBasename(this.getValue(key).asString());
                break;
            case "listing":
                cwlDirectory.setListing(new ArrayList<>());
                for (JSResultWrapper list : this.getValue(key).elements()) {
                    if (list.isCWLFile()) {
                        cwlDirectory.getListing().add(list.asCWLFile());
                    } else if (list.isCWLDirectory()) {
                        cwlDirectory.getListing().add(list.asCWLDirectory());
                    }
                }
                break;
            default:
                break;
            }
        }
        return cwlDirectory;
    }

    protected boolean isLong() {
        return (this.result instanceof Integer) || (this.result instanceof Long);
    }

    protected long asLong() {
        if (this.result instanceof Integer) {
            return ((Integer) this.result).longValue();
        } else if (this.result instanceof Double) {
            //The bug of nashorn, after evluated, the nashorn always return double
            return ((Double) this.result).longValue();
        } else {
            return ((Long) this.result);
        }
    }

    protected boolean isDouble() {
        return (this.result instanceof Float) || (this.result instanceof Double);
    }

    protected double asDouble() {
        if (this.result instanceof Float) {
            return ((Float) this.result).doubleValue();
        } else {
            return (Double) this.result;
        }
    }

    protected boolean isBool() {
        return this.result instanceof Boolean;
    }

    protected boolean asBool() {
        return (Boolean) this.result;
    }

    protected boolean isArray() {
        return invokeBoolMethod(this.result, IS_ARRAY);
    }

    /*
     * If the result is an array (a properties structure), return the array elements
     */
    protected List<JSResultWrapper> elements() {
        List<JSResultWrapper> elements = null;
        Class<?> clazz = this.result.getClass();
        String className = clazz.getName();
        if (SCRIPT_OBJECT_MIRROR_CLASSNAME.equals(className)) {
            try {
                @SuppressWarnings("unchecked")
                Collection<Object> values = (Collection<Object>) clazz.getMethod("values").invoke(this.result);
                if (values != null) {
                    elements = new ArrayList<>();
                    for (Object v : values) {
                        elements.add(new JSResultWrapper(v));
                    }
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                logger.warn("Fail to invoke method  keySet ({})", e.getMessage());
            }
        }
        return elements;
    }

    protected boolean isObject() {
        return invokeBoolMethod(this.result, IS_EXTENSIBLE);
    }

    /*
     * If the result is an object (a properties structure), return the property keys
     */
    protected List<String> keys() {
        List<String> keys = null;
        if (invokeBoolMethod(this.result, IS_EXTENSIBLE)) {
            Class<?> clazz = this.result.getClass();
            String className = clazz.getName();
            if (SCRIPT_OBJECT_MIRROR_CLASSNAME.equals(className)) {
                try {
                    @SuppressWarnings("unchecked")
                    Set<String> propertyKeys = (Set<String>) clazz.getMethod("keySet").invoke(this.result);
                    if (propertyKeys != null) {
                        keys = new ArrayList<>();
                        for (String key : propertyKeys) {
                            keys.add(key);
                        }
                    }
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    logger.warn("Fail to invoke method  keySet ({})", e.getMessage());
                }
            }
        }
        return keys;
    }

    /*
     * If the result is an object (a properties structure), return the value by
     * given key
     */
    protected JSResultWrapper getValue(String key) {
        JSResultWrapper value = null;
        if (key != null && key.length() != 0) {
            Class<?> clazz = this.result.getClass();
            String className = clazz.getName();
            if (SCRIPT_OBJECT_MIRROR_CLASSNAME.equals(className)) {
                try {
                    Object obj = clazz.getMethod("get", Object.class).invoke(this.result, key);
                    value = new JSResultWrapper(obj);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    logger.warn("Fail to invoke method get ({})", e.getMessage());
                }
            }
        }
        return value;
    }

    protected Object getValue() {
        if (!isNull() && isObject()) {
            return toObject(keys());
        } else {
            return result;
        }
    }

    /*
     * Get the string representation
     */
    protected String getStringRepresentation() {
        if (this.result instanceof String) {
            return String.valueOf(this.result);
        } else if (this.result instanceof Integer || this.result instanceof Long) {
            return String.valueOf(this.result);
        } else if (this.result instanceof Float || this.result instanceof Double) {
            return String.valueOf(this.result).replaceAll("0+?$", "").replaceAll("[.]$", "");
        } else if (this.result instanceof Boolean) {
            return String.valueOf(this.result);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        if (this.result == null) {
            return "A null object";
        } else if (this.result instanceof String || this.result instanceof Integer || this.result instanceof Long
                || this.result instanceof Float || this.result instanceof Double || this.result instanceof Boolean) {
            return String.valueOf(this.result);
        } else if (invokeBoolMethod(this.result, IS_ARRAY)) {
            return "An array";
        } else if (invokeBoolMethod(this.result, IS_EXTENSIBLE)) {
            return "An object";
        } else {
            return "Unknown object type";
        }
    }

    private Map<String, Object> toObject(List<String> keys) {
        Map<String, Object> obj = new HashMap<>();
        for (String key : keys) {
            JSResultWrapper r = getValue(key);
            if (!r.isNull() && r.isArray()) {
                List<Object> list = new ArrayList<>();
                for (JSResultWrapper e : r.elements()) {
                    list.add(e.getValue());
                }
                obj.put(key, list);
            } else {
                obj.put(key, r.getValue());
            }
        }
        return obj;
    }

    private boolean invokeBoolMethod(Object obj, String method) {
        boolean is = false;
        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        if (SCRIPT_OBJECT_MIRROR_CLASSNAME.equals(className)) {
            try {
                is = (boolean) clazz.getMethod(method).invoke(obj);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                logger.warn("Fail to invoke method {} ({})", method, e.getMessage());
            }
        }
        return is;
    }
}

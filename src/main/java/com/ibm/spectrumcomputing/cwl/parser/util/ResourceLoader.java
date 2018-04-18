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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the I18n messages from the
 * src/main/resources/locale/messages.properties
 */
public final class ResourceLoader {

    private ResourceLoader() {
    }

    private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class);
    private static final ResourceBundle messageRes = ResourceBundle.getBundle("locale/messages", Locale.getDefault());

    /**
     * Gets a string for the given key from
     * src/main/resources/locale/messages.properties
     * 
     * @param key
     *            The key for the desired string
     * @return The string for the given key
     */
    public static String getMessage(String key) {
        if (key == null || key.length() == 0) {
            return key;
        }
        try {
            String msg = messageRes.getString(key);
            if (msg != null) {
                return msg;
            }
        } catch (Exception e) {
            logger.warn("The {} is missing. ({})", key, e.getMessage());
        }
        return key;
    }

    /**
     * Gets a string with the given arguments for the given key from
     * src/main/resources/locale/messages.properties
     * 
     * @param key
     *            The key for the desired string
     * @param args
     *            The arguments will be in the message
     * @return The string with the given arguments for the given key
     */
    public static String getMessage(String key, Object... args) {
        String msg = getMessage(key);
        if (msg != null) {
            Object[] msgArgs = {};
            if (args != null) {
                msgArgs = new Object[args.length];
                System.arraycopy(args, 0, msgArgs, 0, args.length);
            }
            return MessageFormat.format(msg, msgArgs);
        }
        return key;
    }
}

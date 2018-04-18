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
package com.ibm.spectrumcomputing.cwl.model;

/**
 * Represents a key-value object
 *
 * @param <K> The key of this object
 * @param <V> The value of this object
 */
public class Pair<K, V> {
    private final K key;
    private final V value;

    /**
     * Constructs a pair object
     * 
     * @param key The key of this object
     * @param value The value of this object
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return Returns the key corresponding to this object.
     */
    public K getKey() {
        return key;
    }

    /**
     * @return Returns the value corresponding to this entry.
     */
    public V getValue() {
        return value;
    }
}

/*
 *    Copyright 2009-2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

class StrictMap<V> extends ConcurrentHashMap<String, V> {

  private static final long serialVersionUID = -4950446264854982944L;
  private final String name;
  private BiFunction<V, V, String> conflictMessageProducer;
  private static final Object AMBIGUITY_INSTANCE = new Object();

  public StrictMap(String name, int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    this.name = name;
  }

  public StrictMap(String name, int initialCapacity) {
    super(initialCapacity);
    this.name = name;
  }

  public StrictMap(String name) {
    this.name = name;
  }

  public StrictMap(String name, Map<String, ? extends V> m) {
    super(m);
    this.name = name;
  }

  /**
   * Assign a function for producing a conflict error message when contains value with the same key.
   * <p>
   * function arguments are 1st is saved value and 2nd is target value.
   *
   * @param conflictMessageProducer
   *          A function for producing a conflict error message
   *
   * @return a conflict error message
   *
   * @since 3.5.0
   */
  public StrictMap<V> conflictMessageProducer(BiFunction<V, V, String> conflictMessageProducer) {
    this.conflictMessageProducer = conflictMessageProducer;
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public V put(String key, V value) {
    if (containsKey(key)) {
      throw new IllegalArgumentException(name + " already contains key " + key
          + (conflictMessageProducer == null ? "" : conflictMessageProducer.apply(super.get(key), value)));
    }
    if (key.contains(".")) {
      final String shortKey = getShortName(key);
      if (super.get(shortKey) == null) {
        super.put(shortKey, value);
      } else {
        super.put(shortKey, (V) AMBIGUITY_INSTANCE);
      }
    }
    return super.put(key, value);
  }

  @Override
  public boolean containsKey(Object key) {
    if (key == null) {
      return false;
    }

    return super.get(key) != null;
  }

  @Override
  public V get(Object key) {
    V value = super.get(key);
    if (value == null) {
      throw new IllegalArgumentException(name + " does not contain value for " + key);
    }
    if (AMBIGUITY_INSTANCE == value) {
      throw new IllegalArgumentException(key + " is ambiguous in " + name
          + " (try using the full name including the namespace, or rename one of the entries)");
    }
    return value;
  }

  private String getShortName(String key) {
    final String[] keyParts = key.split("\\.");
    return keyParts[keyParts.length - 1];
  }
}

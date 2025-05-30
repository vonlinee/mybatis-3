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
package org.apache.ibatis.internal.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extension of the {@code Map} interface that stores multiple values.
 *
 * @param <K>
 *          the key type
 * @param <V>
 *          the value element type
 */
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

  /**
   * Return the count of the given key.
   *
   * @param key
   *          the key
   *
   * @return the count of the given key.
   */
  int count(K key);

  /**
   * Return the first value for the given key.
   *
   * @param key
   *          the key
   *
   * @return the first value for the specified key, or {@code null} if none
   */
  @Nullable
  V getFirst(K key);

  /**
   * Return the first value for the given key.
   *
   * @param key
   *          the key
   * @param index
   *          the index of list
   *
   * @return the value at the specified index in the values for the specified key, or {@code null} if none
   */
  @Nullable
  V get(K key, int index);

  /**
   * Add the given single value to the current list of values for the given key.
   *
   * @param key
   *          the key
   * @param value
   *          the value to be added
   */
  boolean add(K key, @Nullable V value);

  /**
   * Add all the values of the given list to the current list of values for the given key.
   *
   * @param key
   *          they key
   * @param values
   *          the values to be added
   */
  boolean addAll(K key, List<? extends V> values);

  /**
   * Add all the values of the given {@code MultiValueMap} to the current values.
   *
   * @param values
   *          the values to be added
   */
  void addAll(MultiValueMap<K, V> values);

  /**
   * {@link #add(Object, Object) Add} the given value, only when the map does not {@link #containsKey(Object) contain}
   * the given key.
   *
   * @param key
   *          the key
   * @param value
   *          the value to be added
   */
  default void addIfAbsent(K key, @Nullable V value) {
    if (!containsKey(key)) {
      add(key, value);
    }
  }

  /**
   * Set the given single value under the given key.
   *
   * @param key
   *          the key
   * @param value
   *          the value to set
   */
  List<V> set(K key, @Nullable V value);

  /**
   * Set the given single value under the given key.
   *
   * @param key
   *          the key
   * @param values
   *          the values to set
   */
  List<V> setValues(K key, Collection<V> values);

  /**
   * Set the given single value under the given key.
   *
   * @param key
   *          the key
   * @param index
   *          the index
   * @param value
   *          the value to set
   */
  V set(K key, int index, @Nullable V value);

  /**
   * Set the given values under.
   *
   * @param values
   *          the values.
   */
  void setAll(Map<K, V> values);

  /**
   * Return a {@code Map} with the first values contained in this {@code MultiValueMap}.
   *
   * @return a single value representation of this map
   */
  Map<K, V> toSingleValueMap();

  default List<V> flattenValues() {
    return this.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  /**
   * @param collector
   *          the {@code Collector} describing the reduction
   *
   * @return the result of the reduction
   *
   * @param <C>
   *          result
   */
  default <C extends Collection<V>> C flattenValues(@NotNull Collector<V, ?, C> collector) {
    return this.values().stream().flatMap(Collection::stream).collect(collector);
  }

  default void flatForEach(@NotNull BiConsumer<? super K, ? super V> action) {
    for (Entry<K, List<V>> entry : entrySet()) {
      if (entry.getValue() == null) {
        action.accept(entry.getKey(), null);
      } else {
        for (V value : entry.getValue()) {
          action.accept(entry.getKey(), value);
        }
      }
    }
  }
}

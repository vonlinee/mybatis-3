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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CollectionUtils {

  private CollectionUtils() {
  }

  /**
   * Default load factor for {@link HashMap}/{@link LinkedHashMap} variants.
   *
   * @see #newHashMap(int)
   * @see #newLinkedHashMap(int)
   */
  static final float DEFAULT_LOAD_FACTOR = 0.75f;

  /**
   * Return {@code true} if the supplied Collection is {@code null} or empty. Otherwise, return {@code false}.
   *
   * @param collection
   *          the Collection to check
   *
   * @return whether the given Collection is empty
   */
  public static boolean isEmpty(@Nullable Collection<?> collection) {
    return (collection == null || collection.isEmpty());
  }

  /**
   * Return {@code true} if the supplied Map is {@code null} or empty. Otherwise, return {@code false}.
   *
   * @param map
   *          the Map to check
   *
   * @return whether the given Map is empty
   */
  public static boolean isEmpty(@Nullable Map<?, ?> map) {
    return (map == null || map.isEmpty());
  }

  /**
   * Retrieve the first element of the given List, accessing the zero index.
   *
   * @param list
   *          the List to check (maybe {@code null} or empty)
   *
   * @return the first element, or {@code null} if none
   */
  @Nullable
  public static <T> T getFirst(@Nullable List<T> list) {
    return isEmpty(list) ? null : list.get(0);
  }

  /**
   * Instantiate a new {@link HashMap} with an initial capacity that can accommodate the specified number of elements
   * without any immediate resize/rehash operations to be expected.
   * <p>
   * This differs from the regular {@link HashMap} constructor which takes an initial capacity relative to a load factor
   * but is effectively aligned with the JDK's {@link java.util.concurrent.ConcurrentHashMap#ConcurrentHashMap(int)}.
   *
   * @param expectedSize
   *          the expected number of elements (with a corresponding capacity to be derived so that no resize/rehash
   *          operations are needed)
   *
   * @see #newLinkedHashMap(int)
   *
   * @since 5.3
   */
  public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
    return new HashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
  }

  /**
   * Instantiate a new {@link LinkedHashMap} with an initial capacity that can accommodate the specified number of
   * elements without any immediate resize/rehash operations to be expected.
   * <p>
   * This differs from the regular {@link LinkedHashMap} constructor which takes an initial capacity relative to a load
   * factor but is aligned with Spring's own {@link LinkedCaseInsensitiveMap} and {@link LinkedMultiValueMap}
   * constructor semantics as of 5.3.
   *
   * @param expectedSize
   *          the expected number of elements (with a corresponding capacity to be derived so that no resize/rehash
   *          operations are needed)
   *
   * @see #newHashMap(int)
   *
   * @since 5.3
   */
  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
    return new LinkedHashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
  }

  private static int computeMapInitialCapacity(int expectedSize) {
    return (int) Math.ceil(expectedSize / (double) DEFAULT_LOAD_FACTOR);
  }

  /**
   * Check whether the given Iterator contains the given element.
   *
   * @param iterator
   *          the Iterator to check
   * @param element
   *          the element to look for
   *
   * @return {@code true} if found, {@code false} otherwise
   */
  public static boolean contains(@Nullable Iterator<?> iterator, Object element) {
    if (iterator != null) {
      while (iterator.hasNext()) {
        Object candidate = iterator.next();
        if (ObjectUtils.nullSafeEquals(candidate, element)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check whether the given Enumeration contains the given element.
   *
   * @param enumeration
   *          the Enumeration to check
   * @param element
   *          the element to look for
   *
   * @return {@code true} if found, {@code false} otherwise
   */
  public static boolean contains(@Nullable Enumeration<?> enumeration, Object element) {
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        Object candidate = enumeration.nextElement();
        if (ObjectUtils.nullSafeEquals(candidate, element)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Return {@code true} if any element in '{@code candidates}' is contained in '{@code source}'; otherwise returns
   * {@code false}.
   *
   * @param source
   *          the source Collection
   * @param candidates
   *          the candidates to search for
   *
   * @return whether any of the candidates has been found
   */
  public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
    return findFirstMatch(source, candidates) != null;
  }

  /**
   * Return the first element in '{@code candidates}' that is contained in '{@code source}'. If no element in
   * '{@code candidates}' is present in '{@code source}' returns {@code null}. Iteration order is {@link Collection}
   * implementation specific.
   *
   * @param source
   *          the source Collection
   * @param candidates
   *          the candidates to search for
   *
   * @return the first present object, or {@code null} if not found
   */
  @Nullable
  public static <E> E findFirstMatch(Collection<?> source, Collection<E> candidates) {
    if (isEmpty(source) || isEmpty(candidates)) {
      return null;
    }
    for (E candidate : candidates) {
      if (source.contains(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * Marshal the elements from the given enumeration into an array of the given type. Enumeration elements must be
   * assignable to the type of the given array. The array returned will be a different instance than the array given.
   */
  public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
    ArrayList<A> elements = new ArrayList<>();
    while (enumeration.hasMoreElements()) {
      elements.add(enumeration.nextElement());
    }
    return elements.toArray(array);
  }

  /**
   * Adapt a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param targetMap
   *          the original map
   *
   * @return the adapted multi-value map (wrapping the original map)
   *
   * @since 3.1
   */
  public static <K, V> MultiValueMap<K, V> toMultiValueMap(Map<K, List<V>> targetMap) {
    return new MultiValueMapAdapter<>(targetMap);
  }

  /**
   * null key, null value is not supported
   *
   * @param <E>
   *          the type of the input elements
   * @param <K>
   *          the output type of the key mapping function
   * @param <V>
   *          the output type of the value mapping function
   * @param collection
   *          input elements
   * @param keyMapper
   *          a mapping function to produce keys
   * @param valueMapper
   *          a mapping function to produce values
   *
   * @return map
   */
  public static <E, K, V> Map<K, V> toMap(@Nullable Collection<E> collection,
      @NotNull Function<? super E, ? extends K> keyMapper, @NotNull Function<? super E, ? extends V> valueMapper) {
    if (isEmpty(collection)) {
      return Collections.emptyMap();
    }
    return collection.stream().collect(Collectors.toMap(keyMapper, valueMapper, (k1, k2) -> k1));
  }

  public static <K, V> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry<>(key, value);
  }
}

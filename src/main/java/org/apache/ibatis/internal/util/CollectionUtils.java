/*
 *    Copyright 2009-2026 the original author or authors.
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

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CollectionUtils {

  private static final Map.Entry<Type, Class<?>> NULL_ENTRY = new AbstractMap.SimpleImmutableEntry<>(null, null);

  /**
   * Default load factor for {@link HashMap}/{@link LinkedHashMap} variants.
   *
   * @see #newHashMap(int)
   * @see #newLinkedHashMap(int)
   */
  static final float DEFAULT_LOAD_FACTOR = 0.75f;

  private CollectionUtils() {
    // Prevent Instantiation of Static Class
  }

  public static boolean isEmpty(Object[] array) {
    return array == null || array.length == 0;
  }

  public static boolean isNotEmpty(Object[] array) {
    return !isEmpty(array);
  }

  public static boolean isEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  public static boolean isNotEmpty(Collection<?> collection) {
    return !isEmpty(collection);
  }

  public static boolean isEmpty(Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  public static boolean isNotEmpty(Map<?, ?> map) {
    return !isEmpty(map);
  }

  @Nullable
  public static <T> T getFirst(@Nullable List<T> list) {
    if (isEmpty(list)) {
      return null;
    }
    return list.get(0);
  }

  @Nullable
  public static <T> T getFirst(@Nullable Set<T> set) {
    if (isEmpty(set)) {
      return null;
    }
    return set.iterator().next();
  }

  @Nullable
  public static <T> T getFirst(@Nullable T[] array) {
    if (array == null || array.length == 0) {
      return null;
    }
    return array[0];
  }

  @NotNull
  @SafeVarargs
  public static <T> List<T> asList(@Nullable T... elements) {
    if (elements == null || elements.length == 0) {
      return Collections.emptyList();
    }
    if (elements.length == 1) {
      return Collections.singletonList(elements[0]);
    }
    return Arrays.asList(elements);
  }

  @NotNull
  @SafeVarargs
  public static <T> List<T> asArrayList(@Nullable T... elements) {
    if (elements == null || elements.length == 0) {
      return new ArrayList<>();
    }
    return new ArrayList<>(Arrays.asList(elements));
  }

  /**
   * @param list
   *          list
   *
   * @return whether the give list is unmodifiable
   *
   * @see Arrays#asList(Object[])
   * @see Collections#singletonList(Object)
   * @see Collections#unmodifiableList(List)
   */
  public static boolean isUnmodifiable(List<?> list) {
    if (list == null) {
      return false;
    }
    final String typeName = list.getClass().getName();
    return "java.util.Collections$EmptyList".equals(typeName) || "java.util.Arrays$ArrayList".equals(typeName)
        || "java.util.Collections$SingletonList".equals(typeName)
        || typeName.startsWith("java.util.Collections$Unmodifiable")
        || typeName.startsWith("java.util.ImmutableCollections"); // java9+
  }

  public static boolean isModifiable(List<?> list) {
    if (list == null) {
      return false;
    }
    return !isUnmodifiable(list);
  }

  public static boolean isUnmodifiable(Map<?, ?> map) {
    if (map == null) {
      return false;
    }
    final String typeName = map.getClass().getName();
    return "java.util.Collections.EmptyMap".equals(typeName) || "java.util.Collections$SingletonMap".equals(typeName)
        || typeName.startsWith("java.util.Collections$Unmodifiable")
        || typeName.startsWith("java.util.ImmutableCollections"); // java9+
  }

  public static boolean isUnmodifiable(Set<?> s) {
    if (s == null) {
      return false;
    }
    final String name = s.getClass().getName();
    return "java.util.Collections$UnmodifiableSet".equals(name) || "java.util.Collections$EmptySet".equals(name)
        || "java.util.Collections$SingletonSet".equals(name) || name.startsWith("java.util.ImmutableCollections"); // java9+
  }

  public static boolean isModifiable(Set<?> s) {
    if (s == null) {
      return false;
    }
    return !isUnmodifiable(s);
  }

  /**
   * null safe {@link Collections#unmodifiableList}
   *
   * @param list
   *          raw list
   * @param <T>
   *          type
   *
   * @return unmodifiable list
   */
  @NotNull
  public static <T> List<T> unmodifiableList(@Nullable List<T> list) {
    if (isEmpty(list)) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(list);
  }

  /**
   * <li>Collections that do not support modification operations (such as add, remove and clear) are referred to as
   * unmodifiable. Collections that are not unmodifiable are modifiable.</li>
   * <li>Collections that additionally guarantee that no change in the Collection object will be visible are referred to
   * as immutable. Collections that are not immutable are mutable.</li> lock down a list to immutable
   *
   * @param list
   *          list
   *
   * @return unmodifiable list
   *
   * @see Collections#unmodifiableList(List)
   */
  @NotNull
  public static <T> List<T> immutableList(@Nullable List<T> list) {
    if (isEmpty(list)) {
      return Collections.emptyList();
    }
    if (isUnmodifiable(list)) {
      return list;
    }
    if (list.size() == 1) {
      return Collections.singletonList(list.get(0));
    }
    return Collections.unmodifiableList(list);
  }

  /**
   * null safe {@link Collections#unmodifiableMap}
   *
   * @param map
   *          raw map
   * @param <K>
   *          type of key
   * @param <V>
   *          type of value
   *
   * @return unmodifiable map
   */
  @NotNull
  public static <K, V> Map<K, V> unmodifiableMap(@Nullable Map<K, V> map) {
    if (isEmpty(map)) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * lock down a map to immutable
   *
   * @param map
   *          map
   *
   * @return unmodifiable map
   *
   * @see Collections#unmodifiableMap(Map)
   */
  @NotNull
  public static <K, V> Map<K, V> immutableMap(@Nullable Map<K, V> map) {
    if (isEmpty(map)) {
      return Collections.emptyMap();
    }
    if (isUnmodifiable(map)) {
      return map;
    }
    if (map.size() == 1) {
      Map.Entry<K, V> firstEntry = map.entrySet().iterator().next();
      return Collections.singletonMap(firstEntry.getKey(), firstEntry.getValue());
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * null safe {@link Collections#unmodifiableSet}
   *
   * @param s
   *          raw set
   * @param <T>
   *          type of elements
   *
   * @return unmodifiable set
   */
  @NotNull
  public static <T> Set<T> unmodifiableSet(@Nullable Set<? extends T> s) {
    if (isEmpty(s)) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(s);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Set<T> immutableSet(@Nullable Set<? extends T> s) {
    if (isEmpty(s)) {
      return Collections.emptySet();
    }
    if (isUnmodifiable(s)) {
      return (Set<T>) s;
    }
    if (s.size() == 1) {
      return Collections.singleton(s.iterator().next());
    }
    return Collections.unmodifiableSet(s);
  }

  /**
   * if the given list is a subtype of {@link ArrayList}, then call the {@link ArrayList#trimToSize()} on it.
   *
   * @param list
   *          list, maybe a {@link ArrayList}
   *
   * @see ArrayList#trimToSize()
   * @see Collections#emptyList()
   */
  @NotNull
  public static <E> List<E> trimToSize(@Nullable List<E> list) {
    if (list == null || list.isEmpty()) {
      return Collections.emptyList();
    }
    if (list instanceof ArrayList<?>) {
      ((ArrayList<?>) list).trimToSize();
    }
    return list;
  }

  /**
   * Usage: <blockquote>
   *
   * <pre>
   * List&lt;String> list = new ArrayList&lt;>();
   * String[] array = CollectionUtils.toArray(list, String[]::new);
   * </pre>
   *
   * </blockquote>
   *
   * @param collection
   *          collection
   * @param generator
   *          array generator
   * @param <E>
   *          array component type
   *
   * @return array of collection
   */
  @NotNull
  public static <E> E[] toArray(@Nullable Collection<E> collection, @NotNull IntFunction<E[]> generator) {
    Objects.requireNonNull(generator, "array generator cannot be null");
    return collection == null ? generator.apply(0) : collection.toArray(generator);
  }

  @NotNull
  public static <E, T> List<T> toList(@Nullable E[] array, Function<E, T> mapper) {
    return toList(array, arr -> new ArrayList<>(arr.length), mapper);
  }

  @NotNull
  public static <T> List<T> filterToList(@Nullable T[] array, Predicate<T> predicate) {
    if (isEmpty(array)) {
      return Collections.emptyList();
    }
    List<T> list = new ArrayList<>();
    for (T e : array) {
      if (predicate.test(e)) {
        list.add(e);
      }
    }
    return list;
  }

  public static <T> List<T> filterToList(Collection<T> collection, Predicate<T> predicate) {
    if (isEmpty(collection)) {
      return Collections.emptyList();
    }
    List<T> list = new ArrayList<>();
    for (T t : collection) {
      if (predicate.test(t)) {
        list.add(t);
      }
    }
    return list;
  }

  @NotNull
  public static <E, T> List<T> toLinkedList(@Nullable E[] array, Function<E, T> mapper) {
    return toList(array, arr -> new LinkedList<>(), mapper);
  }

  @NotNull
  public static <E, T> List<T> toList(@Nullable E[] array, @NotNull Function<E[], List<T>> listCreator,
      @NotNull Function<E, T> mapper) {
    if (ObjectUtils.isEmpty(array)) {
      return Collections.emptyList();
    }
    List<T> set = listCreator.apply(array);
    for (E e : array) {
      set.add(mapper.apply(e));
    }
    return set;
  }

  @NotNull
  public static <E, T> List<T> toList(@Nullable Collection<E> collection, Function<E, T> mapper) {
    return toList(collection, coll -> new ArrayList<>(coll.size()), mapper);
  }

  @NotNull
  public static <E, C extends Collection<E>, T> List<T> toList(@Nullable C collection,
      @NotNull Function<@NotNull C, List<T>> listCreator, @NotNull Function<E, T> mapper) {
    if (isEmpty(collection)) {
      return Collections.emptyList();
    }
    List<T> set = listCreator.apply(collection);
    for (E e : collection) {
      set.add(mapper.apply(e));
    }
    return set;
  }

  @NotNull
  public static <E, T> Set<T> toSet(@Nullable E[] array, Function<E, T> mapper) {
    return toSet(Arrays.asList(array), mapper);
  }

  @NotNull
  public static <E, T> Set<T> toSet(@Nullable Collection<E> collection, Function<E, T> mapper) {
    return toSet(collection, c -> new HashSet<>(c.size()), mapper);
  }

  @NotNull
  public static <E, T> Set<T> toLinkedHashSet(@Nullable Collection<E> collection, Function<E, T> mapper) {
    return toSet(collection, c -> new LinkedHashSet<>(c.size()), mapper);
  }

  @NotNull
  public static <E, T> Set<T> toSet(@Nullable E[] array, Function<E[], Set<T>> setCreator, Function<E, T> mapper) {
    if (ObjectUtils.isEmpty(array)) {
      return Collections.emptySet();
    }
    Set<T> set = setCreator.apply(array);
    for (E e : array) {
      set.add(mapper.apply(e));
    }
    return set;
  }

  @NotNull
  public static <E, C extends Collection<E>, T> Set<T> toSet(@Nullable C collection,
      Function<@NotNull C, Set<T>> setCreator, Function<E, T> mapper) {
    if (isEmpty(collection)) {
      return Collections.emptySet();
    }
    Set<T> set = setCreator.apply(collection);
    for (E e : collection) {
      set.add(mapper.apply(e));
    }
    return set;
  }

  public static <E, K> Map<K, E> toMap(@Nullable Collection<E> collection, Function<E, K> keyMapper) {
    if (isEmpty(collection)) {
      return Collections.emptyMap();
    }
    return toMap(collection, arr -> new HashMap<>(collection.size()), keyMapper, Function.identity());
  }

  public static <E, K> Map<K, E> toLinkedHashMap(@Nullable Collection<E> collection, Function<E, K> keyMapper) {
    if (isEmpty(collection)) {
      return Collections.emptyMap();
    }
    return toMap(collection, arr -> new LinkedHashMap<>(collection.size()), keyMapper, Function.identity());
  }

  public static <E, K> Map<K, E> toMap(@Nullable E[] array, Function<E, K> keyMapper) {
    return toMap(array, arr -> new HashMap<>(arr.length), keyMapper, Function.identity());
  }

  public static <E, K> Map<K, E> toLinkedHashMap(@Nullable E[] array, Function<E, K> keyMapper) {
    return toMap(array, arr -> new LinkedHashMap<>(arr.length), keyMapper, Function.identity());
  }

  public static <E, K, V> Map<K, V> toMap(@Nullable E[] array, Function<E, K> keyMapper, Function<E, V> valueMapper) {
    return toMap(array, arr -> new HashMap<>(arr.length), keyMapper, valueMapper);
  }

  public static <E, K, V> Map<K, V> toMap(@Nullable E[] array, Function<E[], Map<K, V>> mapCreator,
      Function<E, K> keyMapper, Function<E, V> valueMapper) {
    if (ObjectUtils.isEmpty(array)) {
      return Collections.emptyMap();
    }
    Map<K, V> map = mapCreator.apply(array);
    for (E e : array) {
      map.put(keyMapper.apply(e), valueMapper.apply(e));
    }
    return map;
  }

  public static <E, C extends Collection<E>, K, V> Map<K, V> toMap(@Nullable C collection, Function<E, K> keyMapper,
      Function<E, V> valueMapper) {
    return toMap(collection, coll -> new HashMap<>(coll.size()), keyMapper, valueMapper);
  }

  public static <E, C extends Collection<E>, K, V> Map<K, V> toMap(@Nullable C collection,
      Function<@NotNull C, Map<K, V>> mapCreator, Function<E, K> keyMapper, Function<E, V> valueMapper) {
    if (isEmpty(collection)) {
      return Collections.emptyMap();
    }
    Map<K, V> map = mapCreator.apply(collection);
    for (E e : collection) {
      map.put(keyMapper.apply(e), valueMapper.apply(e));
    }
    return map;
  }

  // ======================= grouping by ================================

  public static <E, C extends Collection<E>, K> Map<K, List<E>> groupingBy(@Nullable C collection,
      Function<? super E, ? extends K> classifier) {
    if (isEmpty(collection)) {
      return Collections.emptyMap();
    }
    return collection.stream().collect(Collectors.groupingBy(classifier));
  }

  public static <E, C extends Collection<E>, K, V> Map<? extends K, List<V>> groupingBy(@Nullable C collection,
      Function<? super E, ? extends K> classifier, Collector<? super E, ?, List<V>> downstream) {
    if (isEmpty(collection)) {
      return Collections.emptyMap();
    }
    return collection.stream().collect(Collectors.groupingBy(classifier, downstream));
  }

  public static <E, C extends Collection<E>, K, V> Map<? extends K, List<V>> groupingBy(@Nullable C collection,
      Function<? super E, ? extends K> classifier, Function<C, Map<K, List<V>>> mapFactory,
      Collector<? super E, ?, List<V>> downstream) {
    if (isEmpty(collection)) {
      return Collections.emptyMap();
    }
    return collection.stream()
        .collect(Collectors.groupingBy(classifier, () -> mapFactory.apply(collection), downstream));
  }

  /**
   * to keep support on java8
   *
   * @param key
   *          key
   * @param value
   *          value
   * @param <K>
   *          key type
   * @param <V>
   *          value type
   *
   * @return Map.Entry
   */
  public static <K, V> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry<>(key, value);
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
   */
  public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
    if (expectedSize <= 0) {
      throw new IllegalArgumentException("Illegal expected size: " + expectedSize);
    }
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
   */
  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
    if (expectedSize < 0) {
      throw new IllegalArgumentException("Illegal expected size: " + expectedSize);
    }
    return new LinkedHashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
  }

  /**
   * @param expectedSize
   *          the expected size
   *
   * @return the exact capacity needed to hold expected size without triggering a resize based on the load factor
   */
  private static int computeMapInitialCapacity(int expectedSize) {
    return (int) Math.ceil(expectedSize / (double) DEFAULT_LOAD_FACTOR);
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Map.Entry<K, V> nullEntry() {
    return (Map.Entry<K, V>) NULL_ENTRY;
  }

  /**
   * Safely retrieves an element from the given list at the specified index. If the list is null, the index is negative,
   * or the index is out of bounds, this method returns null instead of throwing an exception.
   * <p>
   * Example usage:
   * </p>
   *
   * <pre>{@code
   * List<String> list = Arrays.asList("apple", "banana", "cherry");
   * String result = CollectionUtils.safeGet(list, 1); // returns "banana"
   * String nullResult = CollectionUtils.safeGet(list, 5); // returns null
   * }</pre>
   *
   * @param list
   *          the list from which to retrieve the element
   * @param index
   *          the index of the element to retrieve
   * @param <E>
   *          the type of elements in the list
   *
   * @return the element at the specified index, or null if the list is null, the index is negative, or the index is out
   *         of bounds
   */
  @Nullable
  public static <E> E get(List<E> list, int index) {
    if (list == null || index < 0 || index >= list.size()) {
      return null;
    }
    return list.get(index);
  }

  /**
   * Returns the index of the first occurrence of the specified target string in the list, ignoring case considerations.
   * If the target string is not found, returns -1.
   * <p>
   * Usage example:
   * </p>
   *
   * <pre>{@code
   * List<String> list = Arrays.asList("apple", "Banana", "CHERRY");
   * int index = CollectionUtils.indexOfIgnoreCase(list, "banana");
   * // index will be 1
   * }</pre>
   *
   * @param list
   *          the list of strings to search through
   * @param target
   *          the string to search for (case-insensitive)
   *
   * @return the index of the first occurrence of the target string, or -1 if not found
   */
  public static int indexOfIgnoreCase(List<String> list, @NotNull String target) {
    if (isEmpty(list)) {
      return -1;
    }
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).equalsIgnoreCase(target)) {
        return i;
      }
    }
    return -1;
  }
}

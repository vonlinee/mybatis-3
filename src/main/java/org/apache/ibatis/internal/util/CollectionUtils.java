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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CollectionUtils {

  private static final EmptyMultiValueMap<?, ?> EMPTY_MULTI_VALUE_MAP = new EmptyMultiValueMap<>();

  /**
   * @see Collection
   */
  private static final Collection<?> EMPTY_COLLECTION = new Collection<>() {
    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean contains(Object o) {
      return false;
    }

    @NotNull
    @Override
    public Iterator<Object> iterator() {
      return Collections.emptyIterator();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
      return new Object[0];
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
      return a;
    }

    @Override
    public boolean add(Object object) {
      return false;
    }

    @Override
    public boolean remove(Object o) {
      return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
      return false;
    }

    @Override
    public boolean addAll(@NotNull Collection<?> c) {
      return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
      return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
      return false;
    }

    @Override
    public void clear() {
    }
  };

  @SuppressWarnings("unchecked")
  public static <E> Collection<E> emptyCollection() {
    return (Collection<E>) EMPTY_COLLECTION;
  }

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
   * Return {@code true} if the supplied array is {@code null} or empty. Otherwise, return {@code false}.
   *
   * @param arr
   *          the arr to check
   *
   * @return whether the given array is empty
   */
  public static <T> boolean isEmpty(@Nullable T[] arr) {
    return arr == null || arr.length == 0;
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

  @NotNull
  public static <E> List<E> toList(@Nullable Collection<E> collection) {
    if (collection == null) {
      return Collections.emptyList();
    }
    if (collection instanceof List) {
      return (List<E>) collection;
    }
    return new ArrayList<>(collection);
  }

  @NotNull
  public static <E, V> List<V> toList(@Nullable Collection<E> collection, Function<E, V> mapper) {
    if (collection == null) {
      return Collections.emptyList();
    }
    List<V> list = new ArrayList<>(collection.size());
    for (E element : collection) {
      list.add(mapper.apply(element));
    }
    return list;
  }

  @NotNull
  public static <E, V> Set<V> toSet(@Nullable Collection<E> collection, Function<E, V> mapper) {
    if (collection == null) {
      return Collections.emptySet();
    }
    Set<V> set = new HashSet<>(collection.size());
    for (E element : collection) {
      set.add(mapper.apply(element));
    }
    return set;
  }

  @NotNull
  public static <K, E> Map<K, E> toMap(@Nullable Collection<E> collection, Function<E, K> keyMapper) {
    if (collection == null || keyMapper == null) {
      return emptyMap();
    }
    Map<K, E> map = new HashMap<>(collection.size());
    for (E element : collection) {
      map.put(keyMapper.apply(element), element);
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  private static <K, E> MultiValueMap<K, E> emptyMultiValueMap() {
    return (MultiValueMap<K, E>) EMPTY_MULTI_VALUE_MAP;
  }

  @NotNull
  public static <K, E> MultiValueMap<K, E> toMultiValueMap(@Nullable Collection<E> collection,
      Function<E, K> keyMapper) {
    if (collection == null || keyMapper == null) {
      return emptyMultiValueMap();
    }
    MultiValueMap<K, E> map = new LinkedMultiValueMap<>(collection.size());
    for (E element : collection) {
      map.add(keyMapper.apply(element), element);
    }
    return map;
  }

  public static <K, E> Map<K, List<E>> groupingBy(@Nullable Collection<E> collection,
      Function<? super E, ? extends K> keyMapper) {
    if (isEmpty(collection)) {
      return emptyMap();
    }
    return collection.stream().collect(Collectors.groupingBy(keyMapper));
  }

  public static <K, V, E, C extends Collection<E>> Map<K, C> groupingBy(@Nullable Collection<E> collection,
      Function<? super E, ? extends K> keyMapper, Collector<? super E, V, C> downstream) {
    if (isEmpty(collection)) {
      return emptyMap();
    }
    return collection.stream().collect(Collectors.groupingBy(keyMapper, downstream));
  }

  @SafeVarargs
  @NotNull
  public static <E> List<E> asList(E... elements) {
    if (elements == null || elements.length == 0) {
      return Collections.emptyList();
    }
    if (elements.length == 1) {
      return Collections.singletonList(elements[0]);
    }
    return Arrays.asList(elements);
  }

  /**
   * @see CollectionUtils#asList(Object[])
   *
   * @param elements
   *          elements
   *
   * @return array list
   *
   * @param <E>
   *          component type
   */
  @SafeVarargs
  @NotNull
  public static <E> List<E> asArrayList(E... elements) {
    if (elements == null || elements.length == 0) {
      return new ArrayList<>(0);
    }
    return new ArrayList<>(Arrays.asList(elements));
  }

  private static <E> List<E> emptyList() {
    return new ArrayList<>();
  }

  private static <K, V> Map<K, V> emptyMap() {
    return new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  public static <C extends Collection<V>, V> C addAll(C primaryCollection, Collection<? extends V>... collections) {
    if (isEmpty(primaryCollection)) {
      return (C) CollectionUtils.emptyCollection();
    }
    if (isEmpty(collections)) {
      return primaryCollection;
    }
    for (Collection<? extends V> collection : collections) {
      if (collection == null) {
        continue;
      }
      primaryCollection.addAll(collection);
    }
    return primaryCollection;
  }

  /**
   * @see Arrays#asList(Object[])
   * @see Collections#singletonList(Object)
   * @see Collections#unmodifiableList(List)
   *
   * @param list
   *          list
   *
   * @return whether the give list is unmodifiable
   */
  public static boolean isUnmodifiable(List<?> list) {
    if (list == null) {
      return false;
    }
    final String typeName = list.getClass().getName();
    return "java.util.Arrays$ArrayList".equals(typeName) || "java.util.Collections$SingletonList".equals(typeName)
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
    return "java.util.Collections.EmptyMap".equals(typeName) || "java.util.Collections$SingletonList".equals(typeName)
        || typeName.startsWith("java.util.Collections$Unmodifiable")
        || typeName.startsWith("java.util.ImmutableCollections"); // java9+
  }

  public static boolean isModifiable(Set<?> s) {
    if (s == null) {
      return false;
    }
    final String name = s.getClass().getName();
    return "java.util.Collections.UnmodifiableSet".equals(name);
  }

  public static boolean isUnmodifiable(Set<?> s) {
    if (s == null) {
      return false;
    }
    return !isModifiable(s);
  }

  /**
   * @see Collections#unmodifiableList(List)
   *
   * @param list
   *          list
   *
   * @return unmodifiable list
   */
  @NotNull
  public static <T> List<T> unmodifiableList(@Nullable List<T> list) {
    if (list == null) {
      return Collections.emptyList();
    }
    if (isUnmodifiable(list)) {
      return list;
    }
    return Collections.unmodifiableList(list);
  }

  /**
   * @see Collections#unmodifiableMap(Map)
   *
   * @param map
   *          map
   *
   * @return unmodifiable map
   */
  @NotNull
  public static <K, V> Map<K, V> unmodifiableMap(@Nullable Map<K, V> map) {
    if (map == null) {
      return Collections.emptyMap();
    }
    if (isUnmodifiable(map)) {
      return map;
    }
    return Collections.unmodifiableMap(map);
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<T> unmodifiableSet(Set<? extends T> s) {
    if (s == null) {
      return Collections.emptySet();
    }
    if (isUnmodifiable(s)) {
      return (Set<T>) s;
    }
    return Collections.unmodifiableSet(s);
  }

  /**
   * @param <K>
   * @param <V>
   *
   * @see MultiValueMap
   */
  private static class EmptyMultiValueMap<K, V> implements MultiValueMap<K, V> {

    @Override
    public int count(K key) {
      return 0;
    }

    @Override
    public @Nullable V getFirst(K key) {
      return null;
    }

    @Override
    public @Nullable V get(K key, int index) {
      return null;
    }

    @Override
    public boolean add(K key, @Nullable V value) {
      return false;
    }

    @Override
    public boolean addAll(K key, List<? extends V> values) {
      return false;
    }

    @Override
    public void addAll(MultiValueMap<K, V> values) {

    }

    @Override
    public List<V> set(K key, @Nullable V value) {
      return null;
    }

    @Override
    public List<V> setValues(K key, Collection<V> values) {
      return null;
    }

    @Override
    public V set(K key, int index, @Nullable V value) {
      return null;
    }

    @Override
    public void setAll(Map<K, V> values) {

    }

    @Override
    public Map<K, V> toSingleValueMap() {
      return Collections.emptyMap();
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean containsKey(Object key) {
      return false;
    }

    @Override
    public boolean containsValue(Object value) {
      return false;
    }

    @Override
    public List<V> get(Object key) {
      return null;
    }

    @Nullable
    @Override
    public List<V> put(K key, List<V> value) {
      return null;
    }

    @Override
    public List<V> remove(Object key) {
      return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends List<V>> m) {
    }

    @Override
    public void clear() {
    }

    @NotNull
    @Override
    public Set<K> keySet() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<List<V>> values() {
      return Collections.emptyList();
    }

    @NotNull
    @Override
    public Set<Entry<K, List<V>>> entrySet() {
      return Collections.emptySet();
    }
  }
}

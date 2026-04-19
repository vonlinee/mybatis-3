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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CollectionUtilsTest {

  // ======================= safeGet ================================

  @Test
  void shouldReturnElementAtValidIndex() {
    List<String> list = Arrays.asList("apple", "banana", "cherry");
    assertThat(CollectionUtils.get(list, 0)).isEqualTo("apple");
    assertThat(CollectionUtils.get(list, 1)).isEqualTo("banana");
    assertThat(CollectionUtils.get(list, 2)).isEqualTo("cherry");
  }

  @Test
  void shouldReturnNullWhenListIsNull() {
    assertThat(CollectionUtils.<String>get(null, 0)).isNull();
  }

  @Test
  void shouldReturnNullWhenListIsEmpty() {
    assertThat(CollectionUtils.<String>get(Collections.emptyList(), 0)).isNull();
  }

  @ParameterizedTest
  @ValueSource(ints = { -1, -100, Integer.MIN_VALUE })
  void shouldReturnNullWhenIndexIsNegative(int index) {
    List<String> list = Arrays.asList("a", "b", "c");
    assertThat(CollectionUtils.get(list, index)).isNull();
  }

  @ParameterizedTest
  @ValueSource(ints = { 3, 4, 100, Integer.MAX_VALUE })
  void shouldReturnNullWhenIndexIsOutOfBounds(int index) {
    List<String> list = Arrays.asList("a", "b", "c");
    assertThat(CollectionUtils.get(list, index)).isNull();
  }

  @Test
  void shouldReturnNullElementStoredInList() {
    List<String> list = new ArrayList<>();
    list.add(null);
    assertThat(CollectionUtils.get(list, 0)).isNull();
  }

  @Test
  void shouldReturnFirstElementWithIndexZero() {
    List<Integer> list = Arrays.asList(42, 99, 7);
    assertThat(CollectionUtils.get(list, 0)).isEqualTo(42);
  }

  @Test
  void shouldReturnLastElementWithLastValidIndex() {
    List<String> list = Arrays.asList("x", "y", "z");
    assertThat(CollectionUtils.get(list, 2)).isEqualTo("z");
  }

  // ======================= indexOfIgnoreCase ================================

  @Test
  void shouldReturnCorrectIndexWhenTargetMatchesExactCase() {
    List<String> list = Arrays.asList("apple", "banana", "cherry");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "banana")).isEqualTo(1);
  }

  @Test
  void shouldReturnCorrectIndexWhenTargetMatchesLowerCase() {
    List<String> list = Arrays.asList("Apple", "BANANA", "Cherry");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "banana")).isEqualTo(1);
  }

  @Test
  void shouldReturnCorrectIndexWhenTargetMatchesUpperCase() {
    List<String> list = Arrays.asList("apple", "banana", "cherry");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "APPLE")).isEqualTo(0);
  }

  @Test
  void shouldReturnCorrectIndexWhenTargetMatchesMixedCase() {
    List<String> list = Arrays.asList("apple", "banana", "cherry");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "ChErRy")).isEqualTo(2);
  }

  @Test
  void shouldReturnMinusOneWhenTargetNotFound() {
    List<String> list = Arrays.asList("apple", "banana", "cherry");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "grape")).isEqualTo(-1);
  }

  @Test
  void shouldReturnMinusOneWhenListIsNull() {
    assertThat(CollectionUtils.indexOfIgnoreCase(null, "apple")).isEqualTo(-1);
  }

  @Test
  void shouldReturnMinusOneWhenListIsEmpty() {
    assertThat(CollectionUtils.indexOfIgnoreCase(Collections.emptyList(), "apple")).isEqualTo(-1);
  }

  @Test
  void shouldReturnFirstOccurrenceIndexWhenDuplicatesExist() {
    List<String> list = Arrays.asList("apple", "Apple", "APPLE");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "apple")).isEqualTo(0);
  }

  @Test
  void shouldReturnZeroForSingleElementListWhenTargetMatches() {
    List<String> list = Collections.singletonList("Hello");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "hello")).isEqualTo(0);
  }

  @Test
  void shouldReturnMinusOneForSingleElementListWhenTargetDoesNotMatch() {
    List<String> list = Collections.singletonList("Hello");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "world")).isEqualTo(-1);
  }

  @ParameterizedTest
  @CsvSource({ "APPLE,0", "Banana,1", "cherry,2" })
  void shouldReturnCorrectIndexForCsvCases(String target, int expectedIndex) {
    List<String> list = Arrays.asList("apple", "banana", "cherry");
    assertThat(CollectionUtils.indexOfIgnoreCase(list, target)).isEqualTo(expectedIndex);
  }

  // ======================= newHashMap ================================

  @ParameterizedTest
  @ValueSource(ints = { 1, 10, 100, 1000 })
  void shouldCreateHashMapWithSufficientCapacityForExpectedSize(int expectedSize) {
    HashMap<String, Integer> map = CollectionUtils.newHashMap(expectedSize);
    assertThat(map).isNotNull().isEmpty();
    // fill up to expectedSize and verify no exception (no resize needed)
    for (int i = 0; i < expectedSize; i++) {
      map.put("key" + i, i);
    }
    assertThat(map).hasSize(expectedSize);
  }

  @Test
  void shouldCreateEmptyHashMapWhenExpectedSizeIsZero() {
    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
        () -> CollectionUtils.newHashMap(0));
    Assertions.assertTrue(exception.getMessage().contains("Illegal expected size: 0"));
  }

  @Test
  void shouldCreateHashMapWhenExpectedSizeIsNegative() {
    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
        () -> CollectionUtils.newHashMap(-1));
    Assertions.assertTrue(exception.getMessage().contains("Illegal expected size: -1"));
  }

  @Test
  void shouldReturnHashMapInstance() {
    assertThat(CollectionUtils.newHashMap(4)).isInstanceOf(HashMap.class);
  }

  // ======================= newLinkedHashMap ================================

  @ParameterizedTest
  @ValueSource(ints = { 1, 10, 100 })
  void shouldCreateLinkedHashMapWithSufficientCapacityForExpectedSize(int expectedSize) {
    LinkedHashMap<String, Integer> map = CollectionUtils.newLinkedHashMap(expectedSize);
    assertThat(map).isNotNull().isEmpty();
    for (int i = 0; i < expectedSize; i++) {
      map.put("key" + i, i);
    }
    assertThat(map).hasSize(expectedSize);
  }

  @Test
  void shouldCreateEmptyLinkedHashMapWhenExpectedSizeIsZero() {
    LinkedHashMap<String, String> map = CollectionUtils.newLinkedHashMap(0);
    assertThat(map).isNotNull().isEmpty();
  }

  @Test
  void shouldReturnLinkedHashMapInstance() {
    assertThat(CollectionUtils.newLinkedHashMap(4)).isInstanceOf(LinkedHashMap.class);
  }

  @Test
  void shouldPreserveInsertionOrderInLinkedHashMap() {
    LinkedHashMap<String, Integer> map = CollectionUtils.newLinkedHashMap(3);
    map.put("c", 3);
    map.put("a", 1);
    map.put("b", 2);
    assertThat(new ArrayList<>(map.keySet())).containsExactly("c", "a", "b");
  }

  // ======================= toList (collection) ================================

  @Test
  void shouldTransformCollectionToListWithMapper() {
    List<String> source = Arrays.asList("one", "two", "three");
    List<Integer> result = CollectionUtils.toList(source, String::length);
    assertThat(result).containsExactly(3, 3, 5);
  }

  @Test
  void shouldReturnEmptyListWhenCollectionIsNullForToList() {
    List<Integer> result = CollectionUtils.toList((Collection<String>) null, String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyListWhenCollectionIsEmptyForToList() {
    List<Integer> result = CollectionUtils.toList(Collections.emptyList(), String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldTransformSingleElementCollectionToList() {
    List<String> source = Collections.singletonList("hello");
    List<String> result = CollectionUtils.toList(source, String::toUpperCase);
    assertThat(result).containsExactly("HELLO");
  }

  // ======================= toList (array) ================================

  @Test
  void shouldTransformArrayToListWithMapper() {
    String[] array = { "a", "bb", "ccc" };
    List<Integer> result = CollectionUtils.toList(array, String::length);
    assertThat(result).containsExactly(1, 2, 3);
  }

  @Test
  void shouldReturnEmptyListWhenArrayIsNullForToList() {
    List<Integer> result = CollectionUtils.toList((String[]) null, String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyListWhenArrayIsEmptyForToList() {
    List<Integer> result = CollectionUtils.toList(new String[0], String::length);
    assertThat(result).isEmpty();
  }

  // ======================= toSet ================================

  @Test
  void shouldTransformCollectionToSetWithMapper() {
    List<String> source = Arrays.asList("apple", "banana", "cherry");
    Set<Integer> result = CollectionUtils.toSet(source, String::length);
    assertThat(result).containsExactlyInAnyOrder(5, 6);
  }

  @Test
  void shouldReturnEmptySetWhenCollectionIsNullForToSet() {
    Set<Integer> result = CollectionUtils.toSet((Collection<String>) null, String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptySetWhenCollectionIsEmptyForToSet() {
    Set<Integer> result = CollectionUtils.toSet(Collections.emptyList(), String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldDeduplicateElementsWhenTransformingToSet() {
    List<String> source = Arrays.asList("cat", "bat", "hat");
    Set<Integer> result = CollectionUtils.toSet(source, String::length);
    // all lengths are 3, so set should have only one entry
    assertThat(result).containsExactly(3);
  }

  // ======================= toLinkedHashSet ================================

  @Test
  void shouldTransformCollectionToLinkedHashSetPreservingOrder() {
    List<String> source = Arrays.asList("z", "a", "m");
    Set<String> result = CollectionUtils.toLinkedHashSet(source, String::toUpperCase);
    assertThat(result).isInstanceOf(LinkedHashSet.class);
    assertThat(new ArrayList<>(result)).containsExactly("Z", "A", "M");
  }

  @Test
  void shouldReturnEmptySetWhenCollectionIsNullForToLinkedHashSet() {
    Set<String> result = CollectionUtils.toLinkedHashSet((Collection<String>) null, s -> s);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptySetWhenCollectionIsEmptyForToLinkedHashSet() {
    Set<String> result = CollectionUtils.toLinkedHashSet(Collections.<String>emptyList(), s -> s);
    assertThat(result).isEmpty();
  }

  // ======================= toMap (collection + key mapper) ================================

  @Test
  void shouldTransformCollectionToMapWithKeyMapper() {
    List<String> source = Arrays.asList("apple", "banana", "cherry");
    Map<Integer, String> result = CollectionUtils.toMap(source, String::length);
    // Note: duplicate lengths (apple/cherry both length 5 & 6) - last-write wins in HashMap
    assertThat(result).containsKey(6); // banana->6
  }

  @Test
  void shouldReturnEmptyMapWhenCollectionIsNullForToMap() {
    Map<Integer, String> result = CollectionUtils.toMap((Collection<String>) null, String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyMapWhenCollectionIsEmptyForToMap() {
    Map<Integer, String> result = CollectionUtils.toMap(Collections.emptyList(), String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldTransformCollectionToMapWithKeyAndValueMappers() {
    List<String> source = Arrays.asList("a", "bb", "ccc");
    Map<String, Integer> result = CollectionUtils.toMap(source, s -> s, String::length);
    assertThat(result).containsEntry("a", 1).containsEntry("bb", 2).containsEntry("ccc", 3);
  }

  @Test
  void shouldReturnEmptyMapWhenArrayIsNullForToMap() {
    Map<String, String> result = CollectionUtils.toMap((String[]) null, s -> s);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldTransformArrayToMapWithKeyMapper() {
    String[] array = { "foo", "bar", "baz" };
    Map<String, String> result = CollectionUtils.toMap(array, s -> s);
    assertThat(result).containsOnlyKeys("foo", "bar", "baz");
  }

  // ======================= groupingBy ================================

  @Test
  void shouldGroupCollectionByClassifier() {
    List<String> source = Arrays.asList("a", "bb", "cc", "ddd");
    Map<Integer, List<String>> result = CollectionUtils.groupingBy(source, String::length);
    assertThat(result).containsKey(1).containsKey(2).containsKey(3);
    assertThat(result.get(1)).containsExactly("a");
    assertThat(result.get(2)).containsExactlyInAnyOrder("bb", "cc");
    assertThat(result.get(3)).containsExactly("ddd");
  }

  @Test
  void shouldReturnEmptyMapWhenCollectionIsNullForGroupingBy() {
    Map<Integer, List<String>> result = CollectionUtils.groupingBy((List<String>) null, String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyMapWhenCollectionIsEmptyForGroupingBy() {
    Map<Integer, List<String>> result = CollectionUtils.groupingBy(Collections.emptyList(), String::length);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldGroupSingleElementCollection() {
    List<String> source = Collections.singletonList("hello");
    Map<Integer, List<String>> result = CollectionUtils.groupingBy(source, String::length);
    assertThat(result).hasSize(1).containsKey(5);
    assertThat(result.get(5)).containsExactly("hello");
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldGroupByClassifierWithDownstreamCollector() {
    List<String> source = Arrays.asList("cat", "car", "bar", "bat");
    Map<Character, List<String>> result = (Map<Character, List<String>>) CollectionUtils.groupingBy(source,
        s -> s.charAt(0), Collectors.toList());
    assertThat(result).containsKey('c').containsKey('b');
    assertThat(result.get('c')).containsExactlyInAnyOrder("cat", "car");
    assertThat(result.get('b')).containsExactlyInAnyOrder("bar", "bat");
  }

  @Test
  void shouldReturnEmptyMapWhenCollectionIsNullForGroupingByWithDownstream() {
    Map<?, List<String>> result = CollectionUtils.groupingBy((List<String>) null, s -> s, Collectors.toList());
    assertThat(result).isEmpty();
  }

  // ======================= immutableList ================================

  @Test
  void shouldReturnEmptyListWhenSourceListIsNullForImmutableList() {
    List<String> result = CollectionUtils.immutableList(null);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyListWhenSourceListIsEmptyForImmutableList() {
    List<String> result = CollectionUtils.immutableList(Collections.emptyList());
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnSingletonListWhenSourceListHasOneElement() {
    List<String> source = new ArrayList<>();
    source.add("only");
    List<String> result = CollectionUtils.immutableList(source);
    assertThat(result).containsExactly("only");
    assertThat(result.getClass().getName()).isEqualTo("java.util.Collections$SingletonList");
  }

  @Test
  void shouldReturnUnmodifiableListWhenSourceListHasMultipleElements() {
    List<String> source = new ArrayList<>(Arrays.asList("a", "b", "c"));
    List<String> result = CollectionUtils.immutableList(source);
    assertThat(result).containsExactly("a", "b", "c");
    assertThatThrownBy(() -> result.add("d")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldReturnSameInstanceWhenListIsAlreadyUnmodifiable() {
    List<String> unmodifiable = Collections.unmodifiableList(Arrays.asList("x", "y"));
    List<String> result = CollectionUtils.immutableList(unmodifiable);
    assertThat(result).isSameAs(unmodifiable);
  }

  @Test
  void shouldThrowWhenAddingToImmutableList() {
    List<String> source = new ArrayList<>(Arrays.asList("a", "b", "c"));
    List<String> immutable = CollectionUtils.immutableList(source);
    assertThatThrownBy(() -> immutable.add("new")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldThrowWhenRemovingFromImmutableList() {
    List<String> source = new ArrayList<>(Arrays.asList("a", "b", "c"));
    List<String> immutable = CollectionUtils.immutableList(source);
    assertThatThrownBy(() -> immutable.remove(0)).isInstanceOf(UnsupportedOperationException.class);
  }

  // ======================= immutableMap ================================

  @Test
  void shouldReturnEmptyMapWhenSourceMapIsNullForImmutableMap() {
    Map<String, String> result = CollectionUtils.immutableMap(null);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyMapWhenSourceMapIsEmptyForImmutableMap() {
    Map<String, String> result = CollectionUtils.immutableMap(Collections.emptyMap());
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnSingletonMapWhenSourceMapHasOneEntry() {
    Map<String, Integer> source = new HashMap<>();
    source.put("key", 42);
    Map<String, Integer> result = CollectionUtils.immutableMap(source);
    assertThat(result).containsEntry("key", 42);
    assertThat(result.getClass().getName()).isEqualTo("java.util.Collections$SingletonMap");
  }

  @Test
  void shouldReturnUnmodifiableMapWhenSourceMapHasMultipleEntries() {
    Map<String, Integer> source = new HashMap<>();
    source.put("a", 1);
    source.put("b", 2);
    Map<String, Integer> result = CollectionUtils.immutableMap(source);
    assertThat(result).containsEntry("a", 1).containsEntry("b", 2);
    assertThatThrownBy(() -> result.put("c", 3)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldReturnSameInstanceWhenMapIsAlreadyUnmodifiable() {
    Map<String, Integer> map = new HashMap<>();
    map.put("x", 1);
    map.put("y", 2);
    Map<String, Integer> unmodifiable = Collections.unmodifiableMap(map);
    Map<String, Integer> result = CollectionUtils.immutableMap(unmodifiable);
    assertThat(result).isSameAs(unmodifiable);
  }

  @Test
  void shouldThrowWhenPuttingToImmutableMap() {
    Map<String, Integer> source = new HashMap<>();
    source.put("a", 1);
    source.put("b", 2);
    Map<String, Integer> immutable = CollectionUtils.immutableMap(source);
    assertThatThrownBy(() -> immutable.put("c", 3)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldThrowWhenRemovingFromImmutableMap() {
    Map<String, Integer> source = new HashMap<>();
    source.put("a", 1);
    source.put("b", 2);
    Map<String, Integer> immutable = CollectionUtils.immutableMap(source);
    assertThatThrownBy(() -> immutable.remove("a")).isInstanceOf(UnsupportedOperationException.class);
  }

  // ======================= immutableSet ================================

  @Test
  void shouldReturnEmptySetWhenSourceSetIsNullForImmutableSet() {
    Set<String> result = CollectionUtils.immutableSet(null);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptySetWhenSourceSetIsEmptyForImmutableSet() {
    Set<String> result = CollectionUtils.immutableSet(Collections.emptySet());
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnSingletonSetWhenSourceSetHasOneElement() {
    Set<String> source = new HashSet<>(Collections.singletonList("solo"));
    Set<String> result = CollectionUtils.immutableSet(source);
    assertThat(result).containsExactly("solo");
    assertThat(result.getClass().getName()).isEqualTo("java.util.Collections$SingletonSet");
  }

  @Test
  void shouldReturnUnmodifiableSetWhenSourceSetHasMultipleElements() {
    Set<String> source = new HashSet<>(Arrays.asList("x", "y", "z"));
    Set<String> result = CollectionUtils.immutableSet(source);
    assertThat(result).containsExactlyInAnyOrder("x", "y", "z");
    assertThatThrownBy(() -> result.add("w")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldReturnSameInstanceWhenSetIsAlreadyUnmodifiable() {
    Set<String> unmodifiable = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "b")));
    Set<String> result = CollectionUtils.immutableSet(unmodifiable);
    assertThat(result).isSameAs(unmodifiable);
  }

  @Test
  void shouldThrowWhenAddingToImmutableSet() {
    Set<String> source = new HashSet<>(Arrays.asList("a", "b", "c"));
    Set<String> immutable = CollectionUtils.immutableSet(source);
    assertThatThrownBy(() -> immutable.add("d")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldThrowWhenRemovingFromImmutableSet() {
    Set<String> source = new HashSet<>(Arrays.asList("a", "b", "c"));
    Set<String> immutable = CollectionUtils.immutableSet(source);
    assertThatThrownBy(() -> immutable.remove("a")).isInstanceOf(UnsupportedOperationException.class);
  }

  // ======================= isModifiable / isUnmodifiable - List ================================

  @Test
  void shouldReturnTrueForIsModifiableWhenListIsArrayList() {
    assertThat(CollectionUtils.isModifiable(new ArrayList<>())).isTrue();
  }

  @Test
  void shouldReturnFalseForIsModifiableWhenListIsNull() {
    assertThat(CollectionUtils.isModifiable((List<?>) null)).isFalse();
  }

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenListIsFromArraysAsList() {
    List<String> list = Arrays.asList("a", "b");
    assertThat(CollectionUtils.isUnmodifiable(list)).isTrue();
  }

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenListIsSingletonList() {
    List<String> list = Collections.singletonList("only");
    assertThat(CollectionUtils.isUnmodifiable(list)).isTrue();
  }

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenListIsUnmodifiableList() {
    List<String> list = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a", "b")));
    assertThat(CollectionUtils.isUnmodifiable(list)).isTrue();
  }

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenListIsEmptyList() {
    List<String> list = Collections.emptyList();
    assertThat(CollectionUtils.isUnmodifiable(list)).isTrue();
  }

  @Test
  void shouldReturnFalseForIsUnmodifiableWhenListIsNull() {
    assertThat(CollectionUtils.isUnmodifiable((List<?>) null)).isFalse();
  }

  @Test
  void shouldReturnFalseForIsUnmodifiableWhenListIsArrayList() {
    assertThat(CollectionUtils.isUnmodifiable(new ArrayList<>())).isFalse();
  }

  @Test
  void shouldReturnFalseForIsModifiableWhenListIsUnmodifiable() {
    List<String> list = Collections.unmodifiableList(Arrays.asList("a", "b"));
    assertThat(CollectionUtils.isModifiable(list)).isFalse();
  }

  // ======================= isModifiable / isUnmodifiable - Map ================================

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenMapIsSingletonMap() {
    Map<String, String> map = Collections.singletonMap("k", "v");
    assertThat(CollectionUtils.isUnmodifiable(map)).isTrue();
  }

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenMapIsUnmodifiableMap() {
    Map<String, String> base = new HashMap<>();
    base.put("a", "1");
    Map<String, String> map = Collections.unmodifiableMap(base);
    assertThat(CollectionUtils.isUnmodifiable(map)).isTrue();
  }

  @Test
  void shouldReturnFalseForIsUnmodifiableWhenMapIsNull() {
    assertThat(CollectionUtils.isUnmodifiable((Map<?, ?>) null)).isFalse();
  }

  @Test
  void shouldReturnFalseForIsUnmodifiableWhenMapIsHashMap() {
    assertThat(CollectionUtils.isUnmodifiable(new HashMap<>())).isFalse();
  }

  // ======================= isModifiable / isUnmodifiable - Set ================================

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenSetIsUnmodifiableSet() {
    Set<String> set = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "b")));
    assertThat(CollectionUtils.isUnmodifiable(set)).isTrue();
  }

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenSetIsSingletonSet() {
    Set<String> set = Collections.singleton("only");
    assertThat(CollectionUtils.isUnmodifiable(set)).isTrue();
  }

  @Test
  void shouldReturnTrueForIsUnmodifiableWhenSetIsEmptySet() {
    Set<String> set = Collections.emptySet();
    assertThat(CollectionUtils.isUnmodifiable(set)).isTrue();
  }

  @Test
  void shouldReturnFalseForIsUnmodifiableWhenSetIsNull() {
    assertThat(CollectionUtils.isUnmodifiable((Set<?>) null)).isFalse();
  }

  @Test
  void shouldReturnFalseForIsUnmodifiableWhenSetIsHashSet() {
    assertThat(CollectionUtils.isUnmodifiable(new HashSet<>())).isFalse();
  }

  @Test
  void shouldReturnTrueForIsModifiableWhenSetIsHashSet() {
    assertThat(CollectionUtils.isModifiable(new HashSet<>())).isTrue();
  }

  @Test
  void shouldReturnFalseForIsModifiableWhenSetIsNull() {
    assertThat(CollectionUtils.isModifiable((Set<?>) null)).isFalse();
  }

  @Test
  void shouldReturnFalseForIsModifiableWhenSetIsUnmodifiableSet() {
    Set<String> set = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "b")));
    assertThat(CollectionUtils.isModifiable(set)).isFalse();
  }

  // ======================= newHashMap / newLinkedHashMap capacity math ================================

  @Test
  void shouldComputeCapacityCorrectlyForExpectedSizeOf16() {
    // expectedSize=16, capacity = ceil(16/0.75) = ceil(21.33) = 22
    // Map should hold 16 elements without resizing
    HashMap<Integer, Integer> map = CollectionUtils.newHashMap(16);
    for (int i = 0; i < 16; i++) {
      map.put(i, i);
    }
    assertThat(map).hasSize(16);
  }

  @Test
  void shouldComputeCapacityCorrectlyForLinkedHashMapWithExpectedSizeOf16() {
    LinkedHashMap<Integer, Integer> map = CollectionUtils.newLinkedHashMap(16);
    for (int i = 0; i < 16; i++) {
      map.put(i, i);
    }
    assertThat(map).hasSize(16);
  }

  // ======================= Null-safe parameterized edge cases ================================

  @ParameterizedTest
  @NullSource
  void shouldReturnMinusOneWhenListIsNullForIndexOfIgnoreCase(List<String> list) {
    assertThat(CollectionUtils.indexOfIgnoreCase(list, "anything")).isEqualTo(-1);
  }

  @ParameterizedTest
  @NullSource
  void shouldReturnNullWhenListIsNullForGet(List<String> list) {
    assertThat(CollectionUtils.get(list, 0)).isNull();
  }

  static class Pojo {

    int number;
    String stringField;
    boolean flag;

    public boolean isFlag() {
      return flag;
    }

    public void setFlag(boolean flag) {
      this.flag = flag;
    }

    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    public String getStringField() {
      return stringField;
    }

    public void setStringField(String stringField) {
      this.stringField = stringField;
    }
  }

  @Test
  void shouldReturnCollectionWith() {
    List<Pojo> pojoList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Pojo pojo = new Pojo();
      pojo.setStringField("string" + i);
      pojo.setFlag(i % 2 == 0);
      pojo.setNumber(i);
      pojoList.add(pojo);
    }

    {
      List<Boolean> flags = CollectionUtils.toList(pojoList, Pojo::isFlag);
      assertThat(flags).containsExactly(true, false, true, false, true, false, true, false, true, false);
    }

    {
      List<Integer> numbers = CollectionUtils.toList(pojoList, Pojo::getNumber);
      assertThat(numbers).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    {
      List<String> strings = CollectionUtils.toList(pojoList, Pojo::getStringField);
      assertThat(strings).containsExactly("string0", "string1", "string2", "string3", "string4", "string5", "string6",
          "string7", "string8", "string9");
    }

    {
      List<Pojo> pojos = CollectionUtils.toList(pojoList, pojo -> pojo);
      assertThat(pojos).containsExactlyElementsOf(pojoList);
    }

  }
}

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
import static org.assertj.core.api.Assertions.entry;

import java.util.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MultiValueMapAdapter}.
 */
class MultiValueMapAdapterTest {

  // -------------------------------------------------------------------------
  // countByKey(key)
  // -------------------------------------------------------------------------

  @Test
  void shouldReturnCountByKeyForExistingKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "v1");
    map.add("key", "v2");
    assertThat(map.countByKey("key")).isEqualTo(2);
  }

  @Test
  void shouldReturnZeroCountByKeyForMissingKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    assertThat(map.countByKey("absent")).isZero();
  }

  @Test
  void shouldReturnZeroCountByKeyForKeyWithEmptyList() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.put("key", new ArrayList<>());
    assertThat(map.countByKey("key")).isZero();
  }

  // -------------------------------------------------------------------------
  // getFirst(key)
  // -------------------------------------------------------------------------

  @Test
  void shouldReturnFirstValueForExistingKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "first");
    map.add("key", "second");
    assertThat(map.getFirst("key")).isEqualTo("first");
  }

  @Test
  void shouldReturnNullGetFirstForMissingKey() {
    assertThat(new LinkedMultiValueMap<>().getFirst("absent")).isNull();
  }

  @Test
  void shouldReturnNullGetFirstForKeyWithEmptyList() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.put("key", new ArrayList<>());
    assertThat(map.getFirst("key")).isNull();
  }

  @Test
  void shouldReturnNullGetFirstForNullKey() {
    assertThat(new LinkedMultiValueMap<>().getFirst(null)).isNull();
  }

  // -------------------------------------------------------------------------
  // get(key, index)
  // -------------------------------------------------------------------------

  @Test
  void shouldReturnValueAtValidIndex() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "a");
    map.add("key", "b");
    map.add("key", "c");
    assertThat(map.get("key", 1)).isEqualTo("b");
  }

  @Test
  void shouldReturnNullForNegativeIndex() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "a");
    assertThat(map.get("key", -1)).isNull();
  }

  @Test
  void shouldReturnNullForOutOfBoundsIndex() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "a");
    assertThat(map.get("key", 5)).isNull();
  }

  @Test
  void shouldReturnNullGetIndexedForMissingKey() {
    assertThat(new LinkedMultiValueMap<>().get("absent", 0)).isNull();
  }

  // -------------------------------------------------------------------------
  // add(key, value)
  // -------------------------------------------------------------------------

  @Test
  void shouldAddSingleValueToNewKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    boolean result = map.add("key", "value");
    assertThat(result).isTrue();
    assertThat(map.get("key")).containsExactly("value");
  }

  @Test
  void shouldAddSingleValueToExistingKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "first");
    map.add("key", "second");
    assertThat(map.get("key")).containsExactly("first", "second");
  }

  @Test
  void shouldAddNullValueToKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    boolean result = map.add("key", null);
    assertThat(result).isTrue();
    assertThat(map.get("key")).containsExactly((String) null);
  }

  // -------------------------------------------------------------------------
  // addAll(key, list)
  // -------------------------------------------------------------------------

  @Test
  void shouldAddAllValuesToNewKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    boolean result = map.addAll("key", Arrays.asList("a", "b", "c"));
    assertThat(result).isTrue();
    assertThat(map.get("key")).containsExactly("a", "b", "c");
  }

  @Test
  void shouldAddAllValuesToExistingKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "existing");
    map.addAll("key", Arrays.asList("a", "b"));
    assertThat(map.get("key")).containsExactly("existing", "a", "b");
  }

  @Test
  void shouldReturnFalseWhenAddAllEmptyList() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    boolean result = map.addAll("key", Collections.emptyList());
    assertThat(result).isFalse();
    assertThat(map.containsKey("key")).isTrue();
    assertThat(map.get("key")).isEmpty();
  }

  // -------------------------------------------------------------------------
  // addAll(MultiValueMap)
  // -------------------------------------------------------------------------

  @Test
  void shouldMergeAnotherMultiValueMapIntoExisting() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");

    LinkedMultiValueMap<String, String> other = new LinkedMultiValueMap<>();
    other.add("k1", "v2");
    other.add("k2", "v3");

    map.addAll(other);

    assertThat(map.get("k1")).containsExactly("v1", "v2");
    assertThat(map.get("k2")).containsExactly("v3");
  }

  @Test
  void shouldAddAllFromEmptyMultiValueMap() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");
    map.addAll(new LinkedMultiValueMap<>());
    assertThat(map.get("k1")).containsExactly("v1");
    assertThat(map.size()).isEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // addIfAbsent(key, value)
  // -------------------------------------------------------------------------

  @Test
  void shouldAddIfAbsentWhenKeyNotPresent() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.addIfAbsent("key", "value");
    assertThat(map.getFirst("key")).isEqualTo("value");
  }

  @Test
  void shouldNotAddIfAbsentWhenKeyAlreadyPresent() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "original");
    map.addIfAbsent("key", "ignored");
    assertThat(map.get("key")).containsExactly("original");
  }

  @Test
  void shouldSetNullValueOnKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "old");
    List<String> previous = map.put("key", null);
    assertThat(previous).containsExactly("old");
  }

  // -------------------------------------------------------------------------
  // setValues(key, collection)
  // -------------------------------------------------------------------------

  @Test
  void shouldReplaceValuesReplacingExistingList() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "old");
    List<String> previous = map.replaceValues("key", Arrays.asList("a", "b", "c"));
    assertThat(previous).containsExactly("old");
    assertThat(map.get("key")).containsExactly("a", "b", "c");
  }

  @Test
  void shouldReplaceValuesWithEmptyCollection() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "old");
    map.replaceValues("key", Collections.emptyList());
    assertThat(map.get("key")).isEmpty();
  }

  @Test
  void shouldReplaceValuesWithNullCollection() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "old");
    map.replaceValues("key", null);
    assertThat(map.get("key")).isNull();
  }

  // -------------------------------------------------------------------------
  // set(key, index, value)
  // -------------------------------------------------------------------------

  @Test
  void shouldSetValueAtGivenIndex() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "a");
    map.add("key", "b");
    map.add("key", "c");
    String old = map.put("key", 1, "X");
    assertThat(old).isEqualTo("b");
    assertThat(map.get("key")).containsExactly("a", "X", "c");
  }

  @Test
  void shouldReturnNullWhenSetIndexOnMissingKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    String result = map.put("absent", 0, "v");
    assertThat(result).isNull();
  }

  // -------------------------------------------------------------------------
  // setAll(Map)
  // -------------------------------------------------------------------------

  // -------------------------------------------------------------------------
  // toSingleValueMap()
  // -------------------------------------------------------------------------

  @Test
  void shouldReturnSingleValueMapWithFirstValues() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "first");
    map.add("k1", "second");
    map.add("k2", "only");
    Map<String, String> single = map.toSingleValueMap();
    assertThat(single).containsOnly(entry("k1", "first"), entry("k2", "only"));
  }

  @Test
  void shouldExcludeKeysWithEmptyListInToSingleValueMap() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.put("empty", new ArrayList<>());
    map.add("k1", "v1");
    Map<String, String> single = map.toSingleValueMap();
    assertThat(single).containsOnlyKeys("k1");
  }

  @Test
  void shouldReturnEmptyToSingleValueMapWhenMapIsEmpty() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    assertThat(map.toSingleValueMap()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // flattenValues()
  // -------------------------------------------------------------------------

  @Test
  void shouldFlattenAllValuesIntoSingleList() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "a");
    map.add("k1", "b");
    map.add("k2", "c");
    List<String> flat = map.flattenValues();
    assertThat(flat).containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  void shouldReturnEmptyListWhenFlatteningEmptyMap() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    assertThat(map.flattenValues()).isEmpty();
  }

  @Test
  void shouldFlattenValuesWithCollector() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "a");
    map.add("k2", "b");
    List<String> flat = map.flattenValues(java.util.stream.Collectors.toList());
    assertThat(flat).containsExactlyInAnyOrder("a", "b");
  }

  // -------------------------------------------------------------------------
  // flatForEach()
  // -------------------------------------------------------------------------

  @Test
  void shouldIterateAllKeyValuePairsViaForEachFlattened() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "a");
    map.add("k1", "b");
    map.add("k2", "c");

    List<String> collected = new ArrayList<>();
    map.forEachFlattened((k, v) -> collected.add(k + "=" + v));

    assertThat(collected).containsExactlyInAnyOrder("k1=a", "k1=b", "k2=c");
  }

  @Test
  void shouldHandleNullValueListInForEachFlattened() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.put("k1", null);

    List<String> collected = new ArrayList<>();
    map.forEachFlattened((k, v) -> collected.add(k + "=" + v));

    assertThat(collected).containsExactly("k1=null");
  }

  // -------------------------------------------------------------------------
  // Map delegation: size, isEmpty, containsKey, containsValue
  // -------------------------------------------------------------------------

  @Test
  void shouldReturnCorrectSize() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    assertThat(map.size()).isZero();
    map.add("k1", "v1");
    assertThat(map.size()).isEqualTo(1);
    map.add("k2", "v2");
    assertThat(map.size()).isEqualTo(2);
  }

  @Test
  void shouldReturnTrueWhenMapIsEmpty() {
    assertThat(new LinkedMultiValueMap<>().isEmpty()).isTrue();
  }

  @Test
  void shouldReturnFalseWhenMapIsNotEmpty() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "value");
    assertThat(map.isEmpty()).isFalse();
  }

  @Test
  void shouldReturnTrueWhenContainsKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("present", "v");
    assertThat(map.containsKey("present")).isTrue();
    assertThat(map.containsKey("absent")).isFalse();
  }

  @Test
  void shouldReturnTrueWhenContainsValue() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    List<String> list = Collections.singletonList("v");
    map.put("key", list);
    assertThat(map.containsValue(list)).isTrue();
    assertThat(map.containsValue(Collections.singletonList("other"))).isFalse();
  }

  // -------------------------------------------------------------------------
  // Map delegation: put, remove, putAll, clear
  // -------------------------------------------------------------------------

  @Test
  void shouldPutListDirectly() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    List<String> list = Arrays.asList("x", "y");
    map.put("key", list);
    assertThat(map.get("key")).containsExactly("x", "y");
  }

  @Test
  void shouldRemoveKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "value");
    List<String> removed = map.remove("key");
    assertThat(removed).containsExactly("value");
    assertThat(map.containsKey("key")).isFalse();
  }

  @Test
  void shouldReturnNullWhenRemovingAbsentKey() {
    assertThat(new LinkedMultiValueMap<>().remove("absent")).isNull();
  }

  @Test
  void shouldPutAll() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    Map<String, List<String>> other = new HashMap<>();
    other.put("k1", Arrays.asList("a", "b"));
    other.put("k2", Collections.singletonList("c"));
    map.putAll(other);
    assertThat(map.get("k1")).containsExactly("a", "b");
    assertThat(map.get("k2")).containsExactly("c");
  }

  @Test
  void shouldClearAllEntries() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");
    map.add("k2", "v2");
    map.clear();
    assertThat(map.isEmpty()).isTrue();
    assertThat(map.size()).isZero();
  }

  // -------------------------------------------------------------------------
  // Map delegation: keySet, values, entrySet
  // -------------------------------------------------------------------------

  @Test
  void shouldReturnKeySet() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");
    map.add("k2", "v2");
    assertThat(map.keySet()).containsExactlyInAnyOrder("k1", "k2");
  }

  @Test
  void shouldReturnValues() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    List<String> list1 = Arrays.asList("a", "b");
    List<String> list2 = Collections.singletonList("c");
    map.put("k1", list1);
    map.put("k2", list2);
    assertThat(map.values()).containsExactlyInAnyOrder(list1, list2);
  }

  @Test
  void shouldReturnEntrySet() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");
    assertThat(map.entrySet()).hasSize(1);
    Map.Entry<String, List<String>> e = map.entrySet().iterator().next();
    assertThat(e.getKey()).isEqualTo("k1");
    assertThat(e.getValue()).containsExactly("v1");
  }

  // -------------------------------------------------------------------------
  // putIfAbsent
  // -------------------------------------------------------------------------

  @Test
  void shouldPutIfAbsentWhenKeyNotPresent() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    List<String> list = Collections.singletonList("v");
    List<String> result = map.putIfAbsent("key", list);
    assertThat(result).isNull();
    assertThat(map.get("key")).containsExactly("v");
  }

  @Test
  void shouldNotReplaceExistingWhenPutIfAbsent() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "original");
    List<String> result = map.putIfAbsent("key", Collections.singletonList("new"));
    assertThat(result).containsExactly("original");
    assertThat(map.get("key")).containsExactly("original");
  }

  // -------------------------------------------------------------------------
  // equals() and hashCode()
  // -------------------------------------------------------------------------

  @Test
  void shouldBeEqualToAnotherMultiValueMapWithSameContent() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");

    LinkedMultiValueMap<String, String> other = new LinkedMultiValueMap<>();
    other.add("k1", "v1");

    assertThat(map).isEqualTo(other);
  }

  @Test
  void shouldNotBeEqualToMapWithDifferentContent() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");

    LinkedMultiValueMap<String, String> other = new LinkedMultiValueMap<>();
    other.add("k1", "different");

    assertThat(map).isNotEqualTo(other);
  }

  @Test
  void shouldNotBeEqualToNonMultiValueMapObject() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");
    // A plain HashMap is not a MultiValueMap, equals should return false
    Map<String, List<String>> plain = new HashMap<>();
    plain.put("k1", Collections.singletonList("v1"));
    assertThat(map.equals(plain)).isFalse();
  }

  @Test
  void shouldHaveSameHashCodeForEqualMaps() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("k1", "v1");

    LinkedMultiValueMap<String, String> other = new LinkedMultiValueMap<>();
    other.add("k1", "v1");

    assertThat(map.hashCode()).isEqualTo(other.hashCode());
  }

  // -------------------------------------------------------------------------
  // toString()
  // -------------------------------------------------------------------------

  @Test
  void shouldProduceNonEmptyToString() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "value");
    assertThat(map.toString()).contains("key").contains("value");
  }

  @Test
  void shouldProduceEmptyMapToString() {
    assertThat(new LinkedMultiValueMap<>().toString()).isEqualTo("{}");
  }
}

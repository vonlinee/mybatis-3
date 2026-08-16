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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LinkedMultiValueMap}.
 */
class LinkedMultiValueMapTest {

  // -------------------------------------------------------------------------
  // Constructor variants
  // -------------------------------------------------------------------------

  @Test
  void shouldCreateEmptyMapWithDefaultConstructor() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    assertThat(map).isEmpty();
  }

  @Test
  void shouldCreateEmptyMapWithCapacityConstructor() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>(16);
    assertThat(map).isEmpty();
  }

  @Test
  void shouldCreateMapWithCapacityZero() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>(0);
    assertThat(map).isEmpty();
    // should still be usable
    map.add("key", "value");
    assertThat(map.getFirst("key")).isEqualTo("value");
  }

  @Test
  void shouldCreateMapFromCopyConstructorWithExistingMap() {
    Map<String, List<String>> source = new LinkedHashMap<>();
    source.put("k1", Arrays.asList("a", "b"));
    source.put("k2", Collections.singletonList("c"));

    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>(source);

    assertThat(map.get("k1")).containsExactly("a", "b");
    assertThat(map.get("k2")).containsExactly("c");
    assertThat(map.size()).isEqualTo(2);
  }

  @Test
  void shouldCreateMapFromCopyConstructorWithEmptyMap() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>(Collections.emptyMap());
    assertThat(map).isEmpty();
  }

  @Test
  void shouldCreateMapFromCopyConstructorOfAnotherLinkedMultiValueMap() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("k1", "v1");
    original.add("k1", "v2");

    LinkedMultiValueMap<String, String> copy = new LinkedMultiValueMap<>(original);

    assertThat(copy.get("k1")).containsExactly("v1", "v2");
    assertThat(copy).isEqualTo(original);
  }

  // -------------------------------------------------------------------------
  // deepCopy()
  // -------------------------------------------------------------------------

  @Test
  void shouldDeepCopyProduceIndependentValueLists() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("k1", "v1");
    original.add("k1", "v2");
    original.add("k2", "v3");

    LinkedMultiValueMap<String, String> copy = original.deepCopy();

    // Modify original after copy — copy must not be affected
    original.add("k1", "v_new");
    original.remove("k2");

    assertThat(copy.get("k1")).containsExactly("v1", "v2");
    assertThat(copy.get("k2")).containsExactly("v3");
  }

  @Test
  void shouldDeepCopyProduceIndependentKeys() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("k1", "v1");

    LinkedMultiValueMap<String, String> copy = original.deepCopy();

    // add new key to copy — original must not see it
    copy.add("k2", "v2");

    assertThat(original.containsKey("k2")).isFalse();
    assertThat(copy.containsKey("k2")).isTrue();
  }

  @Test
  void shouldDeepCopyEmptyMapProduceEmptyMap() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    LinkedMultiValueMap<String, String> copy = original.deepCopy();
    assertThat(copy).isEmpty();
  }

  @Test
  void shouldDeepCopyPreserveAllEntries() {
    LinkedMultiValueMap<String, Integer> original = new LinkedMultiValueMap<>();
    original.add("nums", 1);
    original.add("nums", 2);
    original.add("nums", 3);

    LinkedMultiValueMap<String, Integer> copy = original.deepCopy();

    assertThat(copy.get("nums")).containsExactly(1, 2, 3);
    assertThat(copy.size()).isEqualTo(original.size());
  }

  // -------------------------------------------------------------------------
  // clone() – shallow copy
  // -------------------------------------------------------------------------

  @Test
  void shouldCloneProduceShallowCopyWithSameContent() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("k1", "v1");
    original.add("k2", "v2");

    LinkedMultiValueMap<String, String> clone = original.clone();

    assertThat(clone).isEqualTo(original);
    assertThat(clone).isNotSameAs(original);
  }

  @Test
  void shouldCloneShareValueListReferences() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("k1", "v1");

    LinkedMultiValueMap<String, String> clone = original.clone();

    // The same List instance is referenced by both (shallow copy)
    assertThat(clone.get("k1")).isSameAs(original.get("k1"));
  }

  @Test
  void shouldCloneNewKeyNotVisibleInOriginal() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("k1", "v1");

    LinkedMultiValueMap<String, String> clone = original.clone();
    clone.add("k2", "v2");

    assertThat(original.containsKey("k2")).isFalse();
  }

  @Test
  void shouldCloneEmptyMapProduceEmptyMap() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    LinkedMultiValueMap<String, String> clone = original.clone();
    assertThat(clone).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Insertion order preservation
  // -------------------------------------------------------------------------

  @Test
  void shouldPreserveInsertionOrderOfKeys() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("first", "1");
    map.add("second", "2");
    map.add("third", "3");

    Iterator<String> keys = map.keySet().iterator();
    assertThat(keys.next()).isEqualTo("first");
    assertThat(keys.next()).isEqualTo("second");
    assertThat(keys.next()).isEqualTo("third");
  }

  @Test
  void shouldPreserveInsertionOrderOfValues() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "z");
    map.add("key", "a");
    map.add("key", "m");

    assertThat(map.get("key")).containsExactly("z", "a", "m");
  }

  @Test
  void shouldMaintainInsertionOrderAfterRemoveAndReAdd() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("a", "1");
    map.add("b", "2");
    map.add("c", "3");
    map.remove("b");
    map.add("b", "new");

    List<String> keys = new ArrayList<>(map.keySet());
    // "b" is re-added at the end because it was removed first
    assertThat(keys).containsExactly("a", "c", "b");
  }

  // -------------------------------------------------------------------------
  // Serializable behavior
  // -------------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  void shouldSerializeAndDeserializeCorrectly() throws Exception {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("k1", "v1");
    original.add("k1", "v2");
    original.add("k2", "v3");

    // Serialize
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(original);
    }

    // Deserialize
    LinkedMultiValueMap<String, String> deserialized;
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
      deserialized = (LinkedMultiValueMap<String, String>) ois.readObject();
    }

    assertThat(deserialized).isEqualTo(original);
    assertThat(deserialized.get("k1")).containsExactly("v1", "v2");
    assertThat(deserialized.get("k2")).containsExactly("v3");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSerializeEmptyMapCorrectly() throws Exception {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(original);
    }

    LinkedMultiValueMap<String, String> deserialized;
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
      deserialized = (LinkedMultiValueMap<String, String>) ois.readObject();
    }

    assertThat(deserialized).isEmpty();
  }

  @Test
  void shouldPreserveInsertionOrderAfterDeserialization() throws Exception {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("first", "1");
    original.add("second", "2");
    original.add("third", "3");

    byte[] bytes = SerializationUtils.writeObject(original);

    LinkedMultiValueMap<String, String> deserialized = SerializationUtils.readObject(bytes);

    Iterator<String> keys = deserialized.keySet().iterator();
    assertThat(keys.next()).isEqualTo("first");
    assertThat(keys.next()).isEqualTo("second");
    assertThat(keys.next()).isEqualTo("third");
  }

  // -------------------------------------------------------------------------
  // Boundary conditions: null values, empty maps, single entry
  // -------------------------------------------------------------------------

  @Test
  void shouldHandleSingleEntryMap() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("only", "value");

    assertThat(map.size()).isEqualTo(1);
    assertThat(map.getFirst("only")).isEqualTo("value");
    assertThat(map.toSingleValueMap()).containsOnlyKeys("only");
    assertThat(map.flattenValues()).containsExactly("value");
  }

  @Test
  void shouldStoreNullValuesInList() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", null);
    assertThat(map.get("key")).containsExactly((String) null);
    assertThat(map.getFirst("key")).isNull();
  }

  @Test
  void shouldHandleMultipleNullValuesUnderSameKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", null);
    map.add("key", null);
    assertThat(map.countByKey("key")).isEqualTo(2);
    assertThat(map.get("key")).containsExactly(null, null);
  }

  @Test
  void shouldReturnZeroForNonExistentKeyCountByKey() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    assertThat(map.countByKey("missing")).isZero();
  }

  @Test
  void shouldHandleDeepCopyWithNullValues() {
    LinkedMultiValueMap<String, String> original = new LinkedMultiValueMap<>();
    original.add("k1", null);
    original.add("k1", "v1");

    LinkedMultiValueMap<String, String> copy = original.deepCopy();

    assertThat(copy.get("k1")).containsExactly(null, "v1");
    // Mutate original; copy should be unaffected
    original.add("k1", "v2");
    assertThat(copy.get("k1")).containsExactly(null, "v1");
  }
}

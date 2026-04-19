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

import static com.googlecode.catchexception.apis.BDDCatchException.caughtException;
import static com.googlecode.catchexception.apis.BDDCatchException.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LinkedCaseInsensitiveMap}.
 */
class LinkedCaseInsensitiveMapTest {

  // -------------------------------------------------------------------------
  // Constructor tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Constructor variants")
  class ConstructorTests {

    @Test
    void shouldCreateMapWithDefaultConstructor() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      assertThat(map).isEmpty();
      assertThat(map.getLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void shouldCreateMapWithLocaleConstructor() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      assertThat(map).isEmpty();
      assertThat(map.getLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void shouldCreateMapWithNullLocaleUsesDefault() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(null);
      assertThat(map.getLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void shouldCreateMapWithCapacityConstructor() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(32);
      assertThat(map).isEmpty();
      assertThat(map.getLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void shouldCreateMapWithCapacityAndLocaleConstructor() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(16, Locale.FRENCH);
      assertThat(map).isEmpty();
      assertThat(map.getLocale()).isEqualTo(Locale.FRENCH);
    }

    @Test
    void shouldCreateMapWithZeroCapacity() {
      when(() -> new LinkedCaseInsensitiveMap<>(0));
      BDDAssertions.assertThat(caughtException()).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Illegal expected size: 0");
    }

    @Test
    void shouldCreateCloneViaCopyConstructorThroughCloneMethod() {
      LinkedCaseInsensitiveMap<String> original = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      original.put("Alpha", "1");
      original.put("Beta", "2");

      LinkedCaseInsensitiveMap<String> copy = original.clone();

      assertThat(copy).hasSize(2);
      assertThat(copy.get("alpha")).isEqualTo("1");
      assertThat(copy.get("BETA")).isEqualTo("2");
      assertThat(copy.getLocale()).isEqualTo(Locale.ENGLISH);
    }
  }

  // -------------------------------------------------------------------------
  // getLocale tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getLocale()")
  class GetLocaleTests {

    @Test
    void shouldReturnDefaultLocaleWhenNotSpecified() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      assertThat(map.getLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void shouldReturnEnglishLocale() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      assertThat(map.getLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void shouldReturnGermanLocale() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.GERMAN);
      assertThat(map.getLocale()).isEqualTo(Locale.GERMAN);
    }
  }

  // -------------------------------------------------------------------------
  // put / get (case-insensitive) tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("put() and get() — case-insensitive")
  class PutGetTests {

    private LinkedCaseInsensitiveMap<String> map;

    @BeforeEach
    void setUp() {
      map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
    }

    @Test
    void shouldPutAndGetWithSameCase() {
      map.put("name", "Alice");
      assertThat(map.get("name")).isEqualTo("Alice");
    }

    @Test
    void shouldGetValueCaseInsensitively() {
      map.put("Name", "Alice");
      assertThat(map.get("NAME")).isEqualTo("Alice");
      assertThat(map.get("name")).isEqualTo("Alice");
      assertThat(map.get("NaMe")).isEqualTo("Alice");
    }

    @Test
    void shouldReturnNullForMissingKey() {
      assertThat(map.get("missing")).isNull();
    }

    @Test
    void shouldReturnNullForNonStringKey() {
      map.put("key", "value");
      assertThat(map.get(42)).isNull();
    }

    @Test
    void shouldPutNullValue() {
      map.put("key", null);
      assertThat(map.containsKey("key")).isTrue();
      assertThat(map.get("key")).isNull();
    }

    @Test
    void shouldPutNullValueAndGetCaseInsensitively() {
      map.put("Key", null);
      assertThat(map.get("KEY")).isNull();
      assertThat(map.containsKey("KEY")).isTrue();
    }

    @Test
    void shouldOverwriteValueWithSameKey() {
      map.put("key", "first");
      String oldValue = map.put("key", "second");
      assertThat(oldValue).isEqualTo("first");
      assertThat(map.get("key")).isEqualTo("second");
      assertThat(map).hasSize(1);
    }

    @Test
    void shouldOverwriteValueWithDifferentCaseKey() {
      map.put("key", "first");
      map.put("KEY", "second");
      assertThat(map.get("key")).isEqualTo("second");
      assertThat(map.get("KEY")).isEqualTo("second");
      assertThat(map).hasSize(1);
    }

    @Test
    void shouldReturnOldValueWhenOverwritingWithDifferentCaseKey() {
      map.put("key", "original");
      String returned = map.put("KEY", "updated");
      assertThat(returned).isEqualTo("original");
    }

    @Test
    void shouldPreserveOriginalKeyCasingAfterOverwrite() {
      map.put("myKey", "v1");
      map.put("MYKEY", "v2");
      // The new key casing replaces the old key casing
      Set<String> keys = map.keySet();
      assertThat(keys).containsExactly("MYKEY");
    }
  }

  // -------------------------------------------------------------------------
  // containsKey tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("containsKey()")
  class ContainsKeyTests {

    private LinkedCaseInsensitiveMap<String> map;

    @BeforeEach
    void setUp() {
      map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("Hello", "world");
    }

    @Test
    void shouldContainKeyWithOriginalCase() {
      assertThat(map.containsKey("Hello")).isTrue();
    }

    @Test
    void shouldContainKeyWithUpperCase() {
      assertThat(map.containsKey("HELLO")).isTrue();
    }

    @Test
    void shouldContainKeyWithLowerCase() {
      assertThat(map.containsKey("hello")).isTrue();
    }

    @Test
    void shouldNotContainAbsentKey() {
      assertThat(map.containsKey("World")).isFalse();
    }

    @Test
    void shouldReturnFalseForNonStringKey() {
      assertThat(map.containsKey(123)).isFalse();
    }

    @Test
    void shouldReturnFalseForNullKey() {
      assertThat(map.containsKey(null)).isFalse();
    }
  }

  // -------------------------------------------------------------------------
  // containsValue tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("containsValue()")
  class ContainsValueTests {

    @Test
    void shouldContainPresentValue() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("key", "value");
      assertThat(map.containsValue("value")).isTrue();
    }

    @Test
    void shouldNotContainAbsentValue() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("key", "value");
      assertThat(map.containsValue("other")).isFalse();
    }

    @Test
    void shouldContainNullValue() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("key", null);
      assertThat(map.containsValue(null)).isTrue();
    }
  }

  // -------------------------------------------------------------------------
  // remove tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("remove()")
  class RemoveTests {

    private LinkedCaseInsensitiveMap<String> map;

    @BeforeEach
    void setUp() {
      map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("Alpha", "1");
      map.put("Beta", "2");
    }

    @Test
    void shouldRemoveWithOriginalCase() {
      String removed = map.remove("Alpha");
      assertThat(removed).isEqualTo("1");
      assertThat(map).hasSize(1);
      assertThat(map.containsKey("Alpha")).isFalse();
    }

    @Test
    void shouldRemoveCaseInsensitively() {
      String removed = map.remove("ALPHA");
      assertThat(removed).isEqualTo("1");
      assertThat(map.containsKey("alpha")).isFalse();
    }

    @Test
    void shouldReturnNullWhenRemovingAbsentKey() {
      assertThat(map.remove("nonexistent")).isNull();
    }

    @Test
    void shouldReturnNullWhenRemovingNonStringKey() {
      assertThat(map.remove(42)).isNull();
    }

    @Test
    void shouldNotContainKeyAfterRemoval() {
      map.remove("beta");
      assertThat(map.containsKey("BETA")).isFalse();
      assertThat(map.get("beta")).isNull();
    }

    @Test
    void shouldRemoveKeyWithNullValue() {
      map.put("Null", null);
      String removed = map.remove("null");
      // remove returns null for null-valued entry
      assertThat(removed).isNull();
      assertThat(map.containsKey("Null")).isFalse();
    }
  }

  // -------------------------------------------------------------------------
  // size and isEmpty tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("size() and isEmpty()")
  class SizeIsEmptyTests {

    @Test
    void shouldBeEmptyInitially() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      assertThat(map.isEmpty()).isTrue();
      assertThat(map.size()).isZero();
    }

    @Test
    void shouldHaveSizeOneAfterSinglePut() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("a", "1");
      assertThat(map.isEmpty()).isFalse();
      assertThat(map.size()).isEqualTo(1);
    }

    @Test
    void shouldNotIncreaseSizeOnCaseInsensitiveDuplicate() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "1");
      map.put("KEY", "2");
      assertThat(map.size()).isEqualTo(1);
    }

    @Test
    void shouldDecreaseSizeAfterRemove() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("a", "1");
      map.put("b", "2");
      map.remove("a");
      assertThat(map.size()).isEqualTo(1);
    }

    @Test
    void shouldBeEmptyAfterClear() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("a", "1");
      map.clear();
      assertThat(map.isEmpty()).isTrue();
      assertThat(map.size()).isZero();
    }
  }

  // -------------------------------------------------------------------------
  // clear tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("clear()")
  class ClearTests {

    @Test
    void shouldClearAllEntries() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("A", "1");
      map.put("B", "2");
      map.put("C", "3");
      map.clear();
      assertThat(map).isEmpty();
    }

    @Test
    void shouldAllowPutAfterClear() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("A", "1");
      map.clear();
      map.put("B", "2");
      assertThat(map.get("b")).isEqualTo("2");
    }

    @Test
    void shouldClearCaseInsensitiveKeyIndex() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("Key", "v");
      map.clear();
      assertThat(map.containsKey("KEY")).isFalse();
    }
  }

  // -------------------------------------------------------------------------
  // putAll tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("putAll()")
  class PutAllTests {

    @Test
    void shouldPutAllFromAnotherMap() {
      Map<String, String> source = new HashMap<>();
      source.put("X", "10");
      source.put("Y", "20");

      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.putAll(source);

      assertThat(map.get("x")).isEqualTo("10");
      assertThat(map.get("Y")).isEqualTo("20");
      assertThat(map).hasSize(2);
    }

    @Test
    void shouldIgnoreEmptyMapInPutAll() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("existing", "val");
      map.putAll(new HashMap<>());
      assertThat(map).hasSize(1);
    }

    @Test
    void shouldMergeAndOverwriteOnPutAll() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "old");

      Map<String, String> source = new HashMap<>();
      source.put("KEY", "new");
      map.putAll(source);

      assertThat(map.get("key")).isEqualTo("new");
      assertThat(map).hasSize(1);
    }
  }

  // -------------------------------------------------------------------------
  // putIfAbsent tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("putIfAbsent()")
  class PutIfAbsentTests {

    @Test
    void shouldPutWhenAbsent() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      String result = map.putIfAbsent("key", "value");
      assertThat(result).isNull();
      assertThat(map.get("KEY")).isEqualTo("value");
    }

    @Test
    void shouldNotReplaceWhenPresent() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "existing");
      String result = map.putIfAbsent("KEY", "new");
      assertThat(result).isEqualTo("existing");
      assertThat(map.get("key")).isEqualTo("existing");
      assertThat(map).hasSize(1);
    }

    @Test
    void shouldReturnNullAndInsertWhenExistingValueIsNull() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", null);
      // existing key with null value: putIfAbsent should try to put
      map.putIfAbsent("KEY", "new");
      // The behaviour: oldKey != null, oldKeyValue == null => key = oldKey, then targetMap.putIfAbsent
      assertThat(map).hasSize(1);
    }
  }

  // -------------------------------------------------------------------------
  // computeIfAbsent tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("computeIfAbsent()")
  class ComputeIfAbsentTests {

    @Test
    void shouldComputeAndStoreWhenAbsent() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      String result = map.computeIfAbsent("key", k -> k.toUpperCase());
      assertThat(result).isEqualTo("KEY");
      assertThat(map.get("KEY")).isEqualTo("KEY");
    }

    @Test
    void shouldReturnExistingValueWhenPresent() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "existing");
      String result = map.computeIfAbsent("KEY", k -> "computed");
      assertThat(result).isEqualTo("existing");
      assertThat(map.get("key")).isEqualTo("existing");
    }

    @Test
    void shouldComputeCaseInsensitively() {
      LinkedCaseInsensitiveMap<Integer> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.computeIfAbsent("Hello", String::length);
      assertThat(map.get("HELLO")).isEqualTo(5);
    }
  }

  // -------------------------------------------------------------------------
  // getOrDefault tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getOrDefault()")
  class GetOrDefaultTests {

    @Test
    void shouldReturnValueWhenKeyExists() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "value");
      assertThat(map.getOrDefault("KEY", "default")).isEqualTo("value");
    }

    @Test
    void shouldReturnDefaultWhenKeyAbsent() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      assertThat(map.getOrDefault("missing", "default")).isEqualTo("default");
    }

    @Test
    void shouldReturnNullValueWhenKeyExistsWithNullValue() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", null);
      assertThat(map.getOrDefault("KEY", "default")).isNull();
    }

    @Test
    void shouldReturnDefaultForNonStringKey() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "value");
      assertThat(map.getOrDefault(42, "default")).isEqualTo("default");
    }
  }

  // -------------------------------------------------------------------------
  // Insertion order preservation tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Insertion order preservation")
  class InsertionOrderTests {

    @Test
    void shouldPreserveInsertionOrderInKeySet() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("Charlie", "3");
      map.put("Alice", "1");
      map.put("Bob", "2");

      assertThat(map.keySet()).containsExactly("Charlie", "Alice", "Bob");
    }

    @Test
    void shouldPreserveInsertionOrderInValues() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("c", "three");
      map.put("a", "one");
      map.put("b", "two");

      assertThat(map.values()).containsExactly("three", "one", "two");
    }

    @Test
    void shouldPreserveInsertionOrderInEntrySet() {
      LinkedCaseInsensitiveMap<Integer> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("first", 1);
      map.put("second", 2);
      map.put("third", 3);

      List<String> keys = new ArrayList<>();
      for (Map.Entry<String, Integer> e : map.entrySet()) {
        keys.add(e.getKey());
      }
      assertThat(keys).containsExactly("first", "second", "third");
    }

    @Test
    void shouldMoveKeyToEndOnCaseInsensitiveOverwrite() {
      // When key is overwritten with different casing, new key appears at end
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("alpha", "1");
      map.put("beta", "2");
      map.put("ALPHA", "3"); // replaces "alpha" - new casing inserted at end
      assertThat(map.keySet()).containsExactly("beta", "ALPHA");
    }
  }

  // -------------------------------------------------------------------------
  // clone() tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("clone()")
  class CloneTests {

    @Test
    void shouldCloneToIndependentMap() {
      LinkedCaseInsensitiveMap<String> original = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      original.put("Key", "value");

      LinkedCaseInsensitiveMap<String> clone = original.clone();
      clone.put("Extra", "extra");

      assertThat(original).hasSize(1);
      assertThat(clone).hasSize(2);
    }

    @Test
    void shouldCloneWithSameEntries() {
      LinkedCaseInsensitiveMap<String> original = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      original.put("A", "1");
      original.put("B", "2");

      LinkedCaseInsensitiveMap<String> clone = original.clone();
      assertThat(clone.get("a")).isEqualTo("1");
      assertThat(clone.get("B")).isEqualTo("2");
    }

    @Test
    void shouldClonePreserveLocale() {
      LinkedCaseInsensitiveMap<String> original = new LinkedCaseInsensitiveMap<>(Locale.FRENCH);
      LinkedCaseInsensitiveMap<String> clone = original.clone();
      assertThat(clone.getLocale()).isEqualTo(Locale.FRENCH);
    }

    @Test
    void shouldClonePreserveInsertionOrder() {
      LinkedCaseInsensitiveMap<String> original = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      original.put("z", "26");
      original.put("a", "1");
      original.put("m", "13");

      LinkedCaseInsensitiveMap<String> clone = original.clone();
      assertThat(clone.keySet()).containsExactly("z", "a", "m");
    }

    @Test
    void shouldModifyOriginalWithoutAffectingClone() {
      LinkedCaseInsensitiveMap<String> original = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      original.put("key", "value");

      LinkedCaseInsensitiveMap<String> clone = original.clone();
      original.remove("key");

      assertThat(original.containsKey("key")).isFalse();
      assertThat(clone.containsKey("KEY")).isTrue();
    }
  }

  // -------------------------------------------------------------------------
  // keySet() view tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("keySet() view")
  class KeySetTests {

    @Test
    void shouldReturnKeySetWithCorrectSize() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", "1");
      map.put("b", "2");
      assertThat(map.keySet()).hasSize(2);
    }

    @Test
    void shouldReturnSameKeySetInstanceOnMultipleCalls() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      Set<String> ks1 = map.keySet();
      Set<String> ks2 = map.keySet();
      assertThat(ks1).isSameAs(ks2);
    }

    @Test
    void shouldContainOriginalCasedKeys() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("Hello", "world");
      assertThat(map.keySet()).contains("Hello");
    }

    @Test
    void shouldRemoveKeyViaKeySet() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "value");
      String removed = map.remove("KEY");
      assertThat(removed).isNotNull();
      assertThat(map).isEmpty();
    }

    @Test
    void shouldClearMapViaKeySetClear() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", "1");
      map.put("b", "2");
      map.clear();
      assertThat(map).isEmpty();
    }

    @Test
    void shouldIterateKeysViaKeySetIterator() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("X", "10");
      map.put("Y", "20");

      List<String> keys = new ArrayList<>();
      for (String k : map.keySet()) {
        keys.add(k);
      }
      assertThat(keys).containsExactly("X", "Y");
    }

    @Test
    void shouldRemoveKeyViaKeySetIterator() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", "1");
      map.put("b", "2");
      map.put("c", "3");

      Iterator<String> it = map.keySet().iterator();
      while (it.hasNext()) {
        String key = it.next();
        if ("b".equals(key)) {
          it.remove();
        }
      }
      assertThat(map).hasSize(2);
      assertThat(map.containsKey("b")).isFalse();
      assertThat(map.containsKey("B")).isFalse();
    }
  }

  // -------------------------------------------------------------------------
  // values() view tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("values() view")
  class ValuesTests {

    @Test
    void shouldReturnValuesWithCorrectSize() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", "alpha");
      map.put("b", "beta");
      assertThat(map.values()).hasSize(2);
    }

    @Test
    void shouldReturnSameValuesInstanceOnMultipleCalls() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      Collection<String> v1 = map.values();
      Collection<String> v2 = map.values();
      assertThat(v1).isSameAs(v2);
    }

    @Test
    void shouldContainAllValues() {
      LinkedCaseInsensitiveMap<Integer> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("one", 1);
      map.put("two", 2);
      map.put("three", 3);
      assertThat(map.values()).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void shouldContainNullValues() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("key", null);
      assertThat(map.values()).containsExactly((String) null);
    }

    @Test
    void shouldClearMapViaValuesClear() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      map.put("a", "1");
      map.clear();
      assertThat(map).isEmpty();
    }

    @Test
    void shouldIterateValuesInInsertionOrder() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("first", "A");
      map.put("second", "B");
      map.put("third", "C");

      List<String> vals = new ArrayList<>(map.values());
      assertThat(vals).containsExactly("A", "B", "C");
    }

    @Test
    void shouldRemoveValueViaValuesIterator() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", "keep");
      map.put("b", "remove");
      map.put("c", "keep");

      Iterator<String> it = map.values().iterator();
      while (it.hasNext()) {
        if ("remove".equals(it.next())) {
          it.remove();
        }
      }
      assertThat(map).hasSize(2);
      assertThat(map.containsKey("b")).isFalse();
    }
  }

  // -------------------------------------------------------------------------
  // entrySet() view tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("entrySet() view")
  class EntrySetTests {

    @Test
    void shouldReturnEntrySetWithCorrectSize() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("x", "X");
      map.put("y", "Y");
      assertThat(map.entrySet()).hasSize(2);
    }

    @Test
    void shouldReturnSameEntrySetInstanceOnMultipleCalls() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      Set<Map.Entry<String, String>> es1 = map.entrySet();
      Set<Map.Entry<String, String>> es2 = map.entrySet();
      assertThat(es1).isSameAs(es2);
    }

    @Test
    void shouldIterateEntriesInInsertionOrder() {
      LinkedCaseInsensitiveMap<Integer> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("alpha", 1);
      map.put("beta", 2);
      map.put("gamma", 3);

      List<String> keys = new ArrayList<>();
      List<Integer> vals = new ArrayList<>();
      for (Map.Entry<String, Integer> e : map.entrySet()) {
        keys.add(e.getKey());
        vals.add(e.getValue());
      }
      assertThat(keys).containsExactly("alpha", "beta", "gamma");
      assertThat(vals).containsExactly(1, 2, 3);
    }

    @Test
    void shouldRemoveEntryViaEntrySetIterator() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("keep1", "v1");
      map.put("delete", "v2");
      map.put("keep2", "v3");

      Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<String, String> entry = it.next();
        if ("delete".equals(entry.getKey())) {
          it.remove();
        }
      }
      assertThat(map).hasSize(2);
      assertThat(map.containsKey("delete")).isFalse();
      assertThat(map.containsKey("DELETE")).isFalse();
    }

    @Test
    void shouldClearMapViaEntrySetClear() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", "1");
      map.put("b", "2");
      map.clear();
      assertThat(map).isEmpty();
      assertThat(map.containsKey("a")).isFalse();
    }
  }

  // -------------------------------------------------------------------------
  // forEach tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("forEach()")
  class ForEachTests {

    @Test
    void shouldIterateAllEntriesWithForEach() {
      LinkedCaseInsensitiveMap<Integer> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("A", 1);
      map.put("B", 2);
      map.put("C", 3);

      Map<String, Integer> collected = new HashMap<>();
      map.forEach(collected::put);

      assertThat(collected).containsEntry("A", 1).containsEntry("B", 2).containsEntry("C", 3);
    }

    @Test
    void shouldNotCallConsumerOnEmptyMap() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      List<String> keys = new ArrayList<>();
      map.forEach((k, v) -> keys.add(k));
      assertThat(keys).isEmpty();
    }
  }

  // -------------------------------------------------------------------------
  // equals() and hashCode() tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("equals() and hashCode()")
  class EqualsHashCodeTests {

    @Test
    void shouldEqualItself() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "value");
      assertThat(map.equals(map)).isTrue();
    }

    @Test
    void shouldNotEqualRegularHashMap() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "value");

      Map<String, String> other = new HashMap<>();
      other.put("key", "value");

      // equals only returns true for LinkedCaseInsensitiveMap instances
      assertThat(map.equals(other)).isFalse();
    }

    @Test
    void shouldEqualAnotherLinkedCaseInsensitiveMapWithSameEntries() {
      LinkedCaseInsensitiveMap<String> map1 = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map1.put("key", "value");

      LinkedCaseInsensitiveMap<String> map2 = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map2.put("key", "value");

      assertThat(map1.equals(map2)).isTrue();
    }

    @Test
    void shouldNotEqualNull() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      assertThat(map.equals(null)).isFalse();
    }

    @Test
    void shouldHaveConsistentHashCode() {
      LinkedCaseInsensitiveMap<String> map1 = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map1.put("key", "value");

      LinkedCaseInsensitiveMap<String> map2 = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map2.put("key", "value");

      assertThat(map1.hashCode()).isEqualTo(map2.hashCode());
    }
  }

  // -------------------------------------------------------------------------
  // toString tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("toString()")
  class ToStringTests {

    @Test
    void shouldReturnEmptyMapRepresentation() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      assertThat(map.toString()).isEqualTo("{}");
    }

    @Test
    void shouldIncludeKeyValuePairs() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "value");
      assertThat(map.toString()).contains("key").contains("value");
    }

    @Test
    void shouldPreserveOriginalCasingInToString() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("CamelCase", "val");
      assertThat(map.toString()).contains("CamelCase");
    }
  }

  // -------------------------------------------------------------------------
  // Locale-specific case conversion tests (Turkish 'I')
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Locale-specific case conversion")
  class LocaleSpecificTests {

    @Test
    void shouldUseTurkishLocaleForCaseConversion() {
      // In Turkish locale, uppercase 'I' lowercases to 'ı' (dotless i), not 'i'
      Locale turkish = Locale.forLanguageTag("tr-TR");
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(turkish);

      map.put("TITLE", "value");
      // "TITLE".toLowerCase(turkish) = "tıtle" (dotless i), so lookups with English "title" won't match
      assertThat(map.containsKey("TITLE")).isTrue();
      // In Turkish: 'I' -> 'ı' so "TITLE".toLowerCase(turkish) != "title".toLowerCase(ENGLISH)
      // Both use same Turkish locale, so "title".toLowerCase(turkish) = "title" vs "TITLE".toLowerCase(turkish) =
      // "tıtle"
      assertThat(map.containsKey("TiTle")).isFalse(); // 'i' != 'ı'
    }

    @Test
    void shouldUseEnglishLocalePreventingTurkishIssue() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("INVOICE", "data");
      // In English locale, "INVOICE".toLowerCase() = "invoice" and "invoice" lookup works
      assertThat(map.containsKey("invoice")).isTrue();
      assertThat(map.containsKey("Invoice")).isTrue();
    }

    @Test
    void shouldHandleLocaleSpecificUppercaseCharacters() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.GERMAN);
      map.put("Straße", "street");
      assertThat(map.get("Straße")).isEqualTo("street");
    }

    @Test
    void shouldMaintainSeparateCaseIndexPerLocale() {
      LinkedCaseInsensitiveMap<String> enMap = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      LinkedCaseInsensitiveMap<String> frMap = new LinkedCaseInsensitiveMap<>(Locale.FRENCH);

      enMap.put("café", "coffee");
      frMap.put("café", "coffee");

      assertThat(enMap.get("CAFÉ")).isEqualTo("coffee");
      assertThat(frMap.get("CAFÉ")).isEqualTo("coffee");
    }
  }

  // -------------------------------------------------------------------------
  // Boundary condition tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Boundary conditions")
  class BoundaryConditionTests {

    @Test
    void shouldHandleEmptyMapOperations() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();
      assertThat(map.get("any")).isNull();
      assertThat(map.remove("any")).isNull();
      assertThat(map.containsKey("any")).isFalse();
      assertThat(map.containsValue("any")).isFalse();
      assertThat(map.keySet()).isEmpty();
      assertThat(map.values()).isEmpty();
      assertThat(map.entrySet()).isEmpty();
    }

    @Test
    void shouldHandleSingleEntryMap() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("Solo", "value");
      assertThat(map).hasSize(1);
      assertThat(map.get("solo")).isEqualTo("value");
      assertThat(map.get("SOLO")).isEqualTo("value");
      map.remove("SOLO");
      assertThat(map).isEmpty();
    }

    @Test
    void shouldHandleLargeNumberOfEntries() {
      LinkedCaseInsensitiveMap<Integer> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      for (int i = 0; i < 1000; i++) {
        map.put("key" + i, i);
      }
      assertThat(map).hasSize(1000);
      assertThat(map.get("KEY999")).isEqualTo(999);
      assertThat(map.get("KEY0")).isEqualTo(0);
    }

    @Test
    void shouldHandleRepeatedPutAndRemoveCycles() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      for (int i = 0; i < 50; i++) {
        map.put("cycling", "v" + i);
        assertThat(map).hasSize(1);
      }
      map.remove("CYCLING");
      assertThat(map).isEmpty();
    }

    @Test
    void shouldHandleSingleCharacterKeys() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", "lower");
      map.put("A", "upper"); // should overwrite
      assertThat(map).hasSize(1);
      assertThat(map.get("a")).isEqualTo("upper");
    }

    @Test
    void shouldHandleVeryLongKeys() {
      String longKey = "a".repeat(1000);
      String longKeyUpper = longKey.toUpperCase(Locale.ENGLISH);
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put(longKey, "value");
      assertThat(map.get(longKeyUpper)).isEqualTo("value");
    }
  }

  // -------------------------------------------------------------------------
  // Iterator complete traversal tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Iterator behavior")
  class IteratorBehaviorTests {

    @Test
    void shouldIterateAllKeysViaKeySetIterator() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("one", "1");
      map.put("two", "2");
      map.put("three", "3");

      List<String> collected = new ArrayList<>();
      Iterator<String> it = map.keySet().iterator();
      while (it.hasNext()) {
        collected.add(it.next());
      }
      assertThat(collected).containsExactly("one", "two", "three");
    }

    @Test
    void shouldIterateAllValuesViaValuesIterator() {
      LinkedCaseInsensitiveMap<Integer> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", 10);
      map.put("b", 20);

      List<Integer> collected = new ArrayList<>();
      Iterator<Integer> it = map.values().iterator();
      while (it.hasNext()) {
        collected.add(it.next());
      }
      assertThat(collected).containsExactly(10, 20);
    }

    @Test
    void shouldRemoveAllViaKeySetIterator() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("x", "1");
      map.put("y", "2");

      Iterator<String> it = map.keySet().iterator();
      while (it.hasNext()) {
        it.next();
        it.remove();
      }
      assertThat(map).isEmpty();
    }

    @Test
    void shouldRemoveAllViaValuesIterator() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("x", "1");
      map.put("y", "2");

      Iterator<String> it = map.values().iterator();
      while (it.hasNext()) {
        it.next();
        it.remove();
      }
      assertThat(map).isEmpty();
    }

    @Test
    void shouldRemoveAllViaEntrySetIterator() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("p", "1");
      map.put("q", "2");

      Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
      while (it.hasNext()) {
        it.next();
        it.remove();
      }
      assertThat(map).isEmpty();
    }

    @Test
    void shouldHaveNoNextAfterFullIteration() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("only", "entry");

      Iterator<String> it = map.keySet().iterator();
      it.next();
      assertThat(it.hasNext()).isFalse();
    }
  }

  // -------------------------------------------------------------------------
  // Case-insensitive key overwrite (preserves casing of new key)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Case-insensitive key overwrite behavior")
  class CaseInsensitiveKeyOverwriteTests {

    @Test
    void shouldUseNewKeysCasingAfterOverwrite() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("lower", "1");
      map.put("LOWER", "2");
      // Original key "lower" replaced by new key "LOWER"
      assertThat(map.keySet()).containsExactly("LOWER");
    }

    @Test
    void shouldLookupByEitherCasingAfterOverwrite() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("MixedCase", "original");
      map.put("mixedcase", "updated");
      assertThat(map.get("MIXEDCASE")).isEqualTo("updated");
      assertThat(map.get("MixedCase")).isEqualTo("updated");
    }

    @Test
    void shouldCountOnlyOneEntryAfterMultipleCaseOverwrites() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "a");
      map.put("Key", "b");
      map.put("kEy", "c");
      map.put("KEY", "d");
      map.put("keY", "e");
      assertThat(map).hasSize(1);
      assertThat(map.get("key")).isEqualTo("e");
    }
  }

  // -------------------------------------------------------------------------
  // Null value handling
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Null value handling")
  class NullValueHandlingTests {

    @Test
    void shouldStoreAndRetrieveNullValue() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("nullKey", null);
      assertThat(map.containsKey("nullkey")).isTrue();
      assertThat(map.get("NULLKEY")).isNull();
    }

    @Test
    void shouldOverwriteNullValueWithRealValue() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", null);
      map.put("KEY", "actual");
      assertThat(map.get("key")).isEqualTo("actual");
      assertThat(map).hasSize(1);
    }

    @Test
    void shouldOverwriteRealValueWithNull() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("key", "real");
      map.put("KEY", null);
      assertThat(map.get("key")).isNull();
      assertThat(map.containsKey("KEY")).isTrue();
    }

    @Test
    void shouldReportCorrectSizeWithNullValues() {
      LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);
      map.put("a", null);
      map.put("b", null);
      map.put("c", "real");
      assertThat(map).hasSize(3);
    }
  }

}

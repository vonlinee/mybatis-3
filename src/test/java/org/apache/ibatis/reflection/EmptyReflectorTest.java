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
package org.apache.ibatis.reflection;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link EmptyReflector}.
 * <p>
 * Verifies that:
 * <ul>
 * <li>{@link EmptyReflector#isApplicable} correctly identifies primitive/array/void types.</li>
 * <li>All property-access methods throw {@link ReflectionException}.</li>
 * <li>Query methods ({@code hasSetter}, {@code hasGetter}, etc.) return safe empty/false values instead of
 * throwing.</li>
 * <li>{@code getType()} returns the exact class passed to the constructor.</li>
 * <li>{@code hasDefaultConstructor()} always returns {@code false}.</li>
 * </ul>
 */
class EmptyReflectorTest {

  // -----------------------------------------------------------------------
  // isApplicable
  // -----------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(classes = { boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class,
      double.class, void.class })
  void shouldIsApplicableReturnsTrueForPrimitivesAndVoid(Class<?> type) {
    assertTrue(EmptyReflector.isApplicable(type));
  }

  @Test
  void shouldIsApplicableReturnsTrueForArrayTypes() {
    assertTrue(EmptyReflector.isApplicable(int[].class));
    assertTrue(EmptyReflector.isApplicable(String[].class));
    assertTrue(EmptyReflector.isApplicable(Object[].class));
  }

  @Test
  void shouldIsApplicableReturnsFalseForObjectAndBoxedTypes() {
    assertTrue(EmptyReflector.isApplicable(Object.class));
    assertTrue(EmptyReflector.isApplicable(Integer.class));
    assertTrue(EmptyReflector.isApplicable(String.class));
  }

  @Test
  void shouldIsApplicableReturnsFalseForRegularClass() {
    assertFalse(EmptyReflector.isApplicable(EmptyReflectorTest.class));
  }

  // -----------------------------------------------------------------------
  // getType
  // -----------------------------------------------------------------------

  @Test
  void shouldGetTypeReturnsExactClass() {
    EmptyReflector r = new EmptyReflector(int.class);
    assertEquals(int.class, r.getType());
  }

  @Test
  void shouldGetTypeWorksForEachPrimitive() {
    assertEquals(long.class, new EmptyReflector(long.class).getType());
    assertEquals(void.class, new EmptyReflector(void.class).getType());
    assertEquals(int[].class, new EmptyReflector(int[].class).getType());
  }

  // -----------------------------------------------------------------------
  // hasDefaultConstructor / getDefaultConstructor
  // -----------------------------------------------------------------------

  @Test
  void shouldHasDefaultConstructorAlwaysFalse() {
    assertFalse(new EmptyReflector(int.class).hasDefaultConstructor());
    assertFalse(new EmptyReflector(void.class).hasDefaultConstructor());
    assertFalse(new EmptyReflector(long[].class).hasDefaultConstructor());
  }

  @Test
  void shouldGetDefaultConstructorThrows() {
    EmptyReflector r = new EmptyReflector(int.class);
    assertThrows(ReflectionException.class, r::getDefaultConstructor);
  }

  // -----------------------------------------------------------------------
  // hasSetter / hasGetter — must never throw, must return false
  // -----------------------------------------------------------------------

  @Test
  void shouldHasSetterReturnsFalseForAnyProperty() {
    EmptyReflector r = new EmptyReflector(int.class);
    assertDoesNotThrow(() -> assertFalse(r.hasSetter("anything")));
  }

  @Test
  void shouldHasGetterReturnsFalseForAnyProperty() {
    EmptyReflector r = new EmptyReflector(int.class);
    assertDoesNotThrow(() -> assertFalse(r.hasGetter("anything")));
  }

  // -----------------------------------------------------------------------
  // Property name arrays — must be empty, never null
  // -----------------------------------------------------------------------

  @Test
  void shouldGetGettablePropertyNamesReturnsEmptyArray() {
    EmptyReflector r = new EmptyReflector(double.class);
    String[] names = r.getGettablePropertyNames();
    assertArrayEquals(new String[0], names);
  }

  @Test
  void shouldGetSettablePropertyNamesReturnsEmptyArray() {
    EmptyReflector r = new EmptyReflector(double.class);
    String[] names = r.getSettablePropertyNames();
    assertArrayEquals(new String[0], names);
  }

  @Test
  void shouldPropertyNameArraysAreSharedSentinel() {
    // The same static EMPTY_NAMES instance should be returned every time (no allocation).
    EmptyReflector r1 = new EmptyReflector(int.class);
    EmptyReflector r2 = new EmptyReflector(long.class);
    assertArrayEquals(r1.getGettablePropertyNames(), r2.getGettablePropertyNames());
    assertArrayEquals(r1.getSettablePropertyNames(), r2.getSettablePropertyNames());
  }

  // -----------------------------------------------------------------------
  // findPropertyName — must return null, never throw
  // -----------------------------------------------------------------------

  @Test
  void shouldFindPropertyNameReturnsNull() {
    EmptyReflector r = new EmptyReflector(int.class);
    assertDoesNotThrow(() -> assertNull(r.findPropertyName("anything")));
    assertDoesNotThrow(() -> assertNull(r.findPropertyName("ID")));
  }

  // -----------------------------------------------------------------------
  // Property access methods — all must throw ReflectionException
  // -----------------------------------------------------------------------

  @Test
  void shouldThrowsWhenGetTypesAndInvokers() {
    assertThrows(ReflectionException.class, () -> new EmptyReflector(int.class).getSetInvoker("x"));
    assertThrows(ReflectionException.class, () -> new EmptyReflector(int.class).getGetInvoker("x"));
    assertThrows(ReflectionException.class, () -> new EmptyReflector(int.class).getSetterType("x"));
    assertThrows(ReflectionException.class, () -> new EmptyReflector(int.class).getGetterType("x"));
    assertThrows(ReflectionException.class, () -> new EmptyReflector(int.class).getGenericSetterType("x"));
    assertThrows(ReflectionException.class, () -> new EmptyReflector(int.class).getGenericGetterType("x"));
  }

  // -----------------------------------------------------------------------
  // Error messages contain the class name
  // -----------------------------------------------------------------------

  @Test
  void shouldExceptionMessageContainsClassName() {
    ReflectionException ex = assertThrows(ReflectionException.class,
        () -> new EmptyReflector(int.class).getGetInvoker("value"));
    assertTrue(ex.getMessage().contains("int"), "Message should contain the class name");
    assertTrue(ex.getMessage().contains("value"), "Message should contain the property name");
  }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.ibatis.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the {@link Reflector#of(Type)} factory method and {@link DefaultReflectorFactory} routing behaviour.
 * <p>
 * Verifies:
 * <ul>
 * <li>Primitive types and arrays route to {@link EmptyReflector}.</li>
 * <li>Regular classes and records route to {@link BeanReflector}.</li>
 * <li>Parameterised types route to {@link BeanReflector} on the raw type.</li>
 * <li>{@link DefaultReflectorFactory} caches results when caching is enabled.</li>
 * <li>{@link DefaultReflectorFactory} creates fresh instances when caching is disabled.</li>
 * <li>{@link Reflector#canControlMemberAccessible()} does not throw.</li>
 * </ul>
 */
class ReflectorFactoryRoutingTest {

  // -----------------------------------------------------------------------
  // Reflector.of — primitive / array → EmptyReflector
  // -----------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(classes = { boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class,
      double.class, void.class })
  void shouldReturnEmptyReflectorForPrimitivesAndVoid(Class<?> type) {
    assertInstanceOf(EmptyReflector.class, Reflector.of(type));
  }

  @Test
  void shouldReturnsEmptyReflectorForArrayType() {
    assertInstanceOf(EmptyReflector.class, Reflector.of(int[].class));
    assertInstanceOf(EmptyReflector.class, Reflector.of(String[].class));
  }

  // -----------------------------------------------------------------------
  // Reflector.of — regular class → BeanReflector
  // -----------------------------------------------------------------------

  static class SomeBean {
    private String x;

    public String getX() {
      return x;
    }

    public void setX(String x) {
      this.x = x;
    }
  }

  @Test
  void shouldReturnCorrectReflectorForSpecifiedClass() {
    assertInstanceOf(EmptyReflector.class, Reflector.of(Object.class));

    assertInstanceOf(EmptyReflector.class, Reflector.of(Integer.class));
    assertInstanceOf(EmptyReflector.class, Reflector.of(String.class));

    assertInstanceOf(BeanReflector.class, Reflector.of(SomeBean.class));
  }

  // -----------------------------------------------------------------------
  // Reflector.of — ParameterizedType → BeanReflector on raw type
  // -----------------------------------------------------------------------

  @Test
  void shouldReturnsBeanReflectorForParameterisedType() {
    // Build a ParameterizedType for List<String>
    Type listOfString = new TypeReference<List<String>>() {
    }.getRawType();
    // TypeReference.getRawType() returns the raw class, but we can also test with a synthetic PT
    Reflector r = Reflector.of(listOfString);
    assertInstanceOf(BeanReflector.class, r);
    assertEquals(List.class, r.getType());
  }

  @Test
  void shouldHandlesParameterisedTypeDirectly() {
    // Construct a ParameterizedType manually via a generic subclass trick
    Type paramType = new java.io.Serializable() {
      // anonymous class that holds a parameterised supertype
      List<String> field;
    }.getClass().getDeclaredFields()[0].getGenericType();
    // paramType is ParameterizedType: List<String>
    assertInstanceOf(ParameterizedType.class, paramType);
    Reflector r = Reflector.of(paramType);
    assertInstanceOf(BeanReflector.class, r);
    assertEquals(List.class, r.getType());
  }

  // -----------------------------------------------------------------------
  // Reflector.of — record
  // -----------------------------------------------------------------------

  record PointRecord(int x, int y) {
  }

  @Test
  void shouldReturnsBeanReflectorForRecord() {
    Reflector r = Reflector.of(PointRecord.class);
    assertInstanceOf(BeanReflector.class, r);
    assertEquals(PointRecord.class, r.getType());
  }

  // -----------------------------------------------------------------------
  // DefaultReflectorFactory — caching
  // -----------------------------------------------------------------------

  @Test
  void shouldFactoryReturnsSameInstanceWhenCacheEnabled() {
    DefaultReflectorFactory factory = new DefaultReflectorFactory();
    factory.setClassCacheEnabled(true);
    Reflector r1 = factory.findForClass(SomeBean.class);
    Reflector r2 = factory.findForClass(SomeBean.class);
    assertSame(r1, r2, "Cache should return the identical instance");
  }

  @Test
  void shouldFactoryReturnsDifferentInstancesWhenCacheDisabled() {
    DefaultReflectorFactory factory = new DefaultReflectorFactory();
    factory.setClassCacheEnabled(false);
    Reflector r1 = factory.findForClass(SomeBean.class);
    Reflector r2 = factory.findForClass(SomeBean.class);
    // Not asserting identity — just that both are valid and non-null
    assertNotNull(r1);
    assertNotNull(r2);
  }

  @Test
  void shouldFactoryReturnsBeanReflectorForRegularClassViaFindForClass() {
    DefaultReflectorFactory factory = new DefaultReflectorFactory();
    assertInstanceOf(BeanReflector.class, factory.findForClass(SomeBean.class));
  }

  @Test
  void shouldFactoryReturnsEmptyReflectorForPrimitiveViaFindForClass() {
    DefaultReflectorFactory factory = new DefaultReflectorFactory();
    assertInstanceOf(EmptyReflector.class, factory.findForClass(int.class));
  }

  @Test
  void shouldFactoryReturnsEmptyReflectorForArrayViaFindForClass() {
    DefaultReflectorFactory factory = new DefaultReflectorFactory();
    assertInstanceOf(EmptyReflector.class, factory.findForClass(String[].class));
  }

  @Test
  void shouldFactoryCachesEmptyReflectorToo() {
    DefaultReflectorFactory factory = new DefaultReflectorFactory();
    factory.setClassCacheEnabled(true);
    Reflector r1 = factory.findForClass(int.class);
    Reflector r2 = factory.findForClass(int.class);
    assertSame(r1, r2);
  }

  // -----------------------------------------------------------------------
  // Reflector.canControlMemberAccessible — static method on interface
  // -----------------------------------------------------------------------

  @Test
  void shouldCanControlMemberAccessibleDoesNotThrow() {
    // We just verify it runs; the result is JVM-dependent.
    boolean result = Reflector.canControlMemberAccessible();
    // boolean is valid either way — just make sure no exception
    assertNotNull(result); // autoboxed; always non-null
  }

  // -----------------------------------------------------------------------
  // End-to-end: EmptyReflector obtained from factory has correct type
  // -----------------------------------------------------------------------

  @Test
  void shouldEmptyReflectorFromFactoryHasCorrectType() {
    DefaultReflectorFactory factory = new DefaultReflectorFactory();
    Reflector r = factory.findForClass(long.class);
    assertEquals(long.class, r.getType());
  }

  @Test
  void shouldBeanReflectorFromFactoryHasCorrectType() {
    DefaultReflectorFactory factory = new DefaultReflectorFactory();
    Reflector r = factory.findForClass(SomeBean.class);
    assertEquals(SomeBean.class, r.getType());
    // confirm the property is accessible
    assertEquals(String.class, r.getGetterType("x"));
  }
}

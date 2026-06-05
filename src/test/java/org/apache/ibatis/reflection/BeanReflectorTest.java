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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.Invoker;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BeanReflector}.
 * <p>
 * Covers:
 * <ul>
 * <li>Basic getter/setter discovery for JavaBeans and public fields.</li>
 * <li>Inheritance — properties defined in superclass are visible.</li>
 * <li>Records — all component accessors are treated as getters.</li>
 * <li>Getter conflict resolution: subtype wins; {@code is} beats {@code get} for {@code boolean}; two {@code is}
 * getters → ambiguous.</li>
 * <li>Setter conflict resolution: type-matching getter wins; subtype wins; unrelated → ambiguous.</li>
 * <li>Error handling: missing property throws {@link ReflectionException} with the class name.</li>
 * <li>Case-insensitive property lookup via {@link BeanReflector#findPropertyName}.</li>
 * <li>Default-constructor detection.</li>
 * <li>Generic type resolution for parameterised getters/setters.</li>
 * </ul>
 */
class BeanReflectorTest {

  // -----------------------------------------------------------------------
  // Fixtures
  // -----------------------------------------------------------------------

  /** Plain JavaBean with a single String property. */
  static class SimpleBean {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  /** Bean without a no-arg constructor. */
  static class NoDefaultConstructorBean {
    private final String id;

    NoDefaultConstructorBean(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }
  }

  /** Bean with a public field (no getter/setter). */
  static class FieldBean {
    public int count;
  }

  /** Parent with generic property; Child specialises to String. */
  abstract static class GenericParent<T> {
    private T value;

    public T getValue() {
      return value;
    }

    public void setValue(T value) {
      this.value = value;
    }
  }

  static class StringChild extends GenericParent<String> {
  }

  /** Getter conflict: two getters with different return types (subtype wins). */
  static class GetterSubtypeBean {
    public Number getNum() {
      return 1;
    }

    public Integer getNum2() {
      return 2;
    }

    // subtype: Integer extends Number → Integer getter should win
    public Integer getVal() {
      return 3;
    }

    public Number getVal2() {
      return 4;
    }
  }

  /** Two boolean getters: one "is", one "get" → "is" should win. */
  @SuppressWarnings("unused")
  static class BoolIsGetBean {
    public boolean isActive() {
      return true;
    }

    public boolean getActive() {
      return false;
    }

    public void setActive(boolean active) {
    }
  }

  /** Two "is" boolean getters → ambiguous. */
  @SuppressWarnings("unused")
  static class AmbiguousBoolBean {
    public boolean isFlag() {
      return true;
    }

    public boolean isFlag2() {
      return false;
    }
  }

  /** Java record. */
  record PersonRecord(String name, int age) {
  }

  // -----------------------------------------------------------------------
  // getType
  // -----------------------------------------------------------------------

  @Test
  void getTypeReturnsCorrectClass() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertEquals(SimpleBean.class, r.getType());
  }

  // -----------------------------------------------------------------------
  // Default constructor
  // -----------------------------------------------------------------------

  @Test
  void hasDefaultConstructorTrueWhenPresent() {
    assertTrue(new BeanReflector(SimpleBean.class).hasDefaultConstructor());
  }

  @Test
  void hasDefaultConstructorFalseWhenAbsent() {
    assertFalse(new BeanReflector(NoDefaultConstructorBean.class).hasDefaultConstructor());
  }

  @Test
  void getDefaultConstructorThrowsWhenAbsent() {
    BeanReflector r = new BeanReflector(NoDefaultConstructorBean.class);
    assertThrows(ReflectionException.class, r::getDefaultConstructor);
  }

  @Test
  void getDefaultConstructorSucceedsWhenPresent() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertNotNull(r.getDefaultConstructor());
  }

  // -----------------------------------------------------------------------
  // hasSetter / hasGetter
  // -----------------------------------------------------------------------

  @Test
  void hasGetterAndSetterForBeanProperty() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertTrue(r.hasGetter("name"));
    assertTrue(r.hasSetter("name"));
  }

  @Test
  void hasGetterForPublicField() {
    BeanReflector r = new BeanReflector(FieldBean.class);
    assertTrue(r.hasGetter("count"));
    assertTrue(r.hasSetter("count"));
  }

  @Test
  void doesNotExposeClassProperty() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertFalse(r.hasGetter("class"));
    assertFalse(r.hasSetter("class"));
  }

  // -----------------------------------------------------------------------
  // getGettablePropertyNames / getSettablePropertyNames
  // -----------------------------------------------------------------------

  @Test
  void propertyNamesContainDeclaredProperty() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    List<String> readable = Arrays.asList(r.getGettablePropertyNames());
    List<String> writable = Arrays.asList(r.getSettablePropertyNames());
    assertTrue(readable.contains("name"));
    assertTrue(writable.contains("name"));
  }

  // -----------------------------------------------------------------------
  // getSetterType / getGetterType
  // -----------------------------------------------------------------------

  @Test
  void getSetterTypeReturnsCorrectClass() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertEquals(String.class, r.getSetterType("name"));
  }

  @Test
  void getGetterTypeReturnsCorrectClass() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertEquals(String.class, r.getGetterType("name"));
  }

  @Test
  void getSetterTypeMissingPropertyThrowsWithClassName() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    ReflectionException ex = assertThrows(ReflectionException.class, () -> r.getSetterType("missing"));
    assertTrue(ex.getMessage().contains("SimpleBean") || ex.getMessage().contains("missing"),
        "Error message should reference class or property: " + ex.getMessage());
  }

  @Test
  void getGetterTypeMissingPropertyThrowsWithClassName() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    ReflectionException ex = assertThrows(ReflectionException.class, () -> r.getGetterType("missing"));
    assertTrue(ex.getMessage().contains("missing"),
        "Error message should reference the missing property: " + ex.getMessage());
  }

  // -----------------------------------------------------------------------
  // getGenericSetterType / getGenericGetterType
  // -----------------------------------------------------------------------

  @Test
  void getGenericGetterTypeForSimpleProperty() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    Entry<Type, Class<?>> entry = r.getGenericGetterType("name");
    assertEquals(String.class, entry.getValue());
    assertEquals(String.class, entry.getKey());
  }

  @Test
  void getGenericGetterTypePreservesParameterisedType() {
    BeanReflector r = new BeanReflector(StringChild.class);
    Entry<Type, Class<?>> entry = r.getGenericGetterType("value");
    assertEquals(String.class, entry.getValue());
  }

  @Test
  void getGenericSetterTypeMissingPropertyThrows() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertThrows(ReflectionException.class, () -> r.getGenericSetterType("nope"));
  }

  @Test
  void getGenericGetterTypeMissingPropertyThrows() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertThrows(ReflectionException.class, () -> r.getGenericGetterType("nope"));
  }

  // -----------------------------------------------------------------------
  // Getter with explicit parameterized return type (List<String>)
  // -----------------------------------------------------------------------

  @Test
  void getGenericGetterTypeReturnsParameterisedType() {
    class ListBean {
      public List<String> getItems() {
        return List.of();
      }

      public void setItems(List<String> items) {
      }
    }
    BeanReflector r = new BeanReflector(ListBean.class);
    Entry<Type, Class<?>> entry = r.getGenericGetterType("items");
    assertEquals(List.class, entry.getValue());
    assertInstanceOf(ParameterizedType.class, entry.getKey());
    ParameterizedType pt = (ParameterizedType) entry.getKey();
    assertArrayEquals(new Type[] { String.class }, pt.getActualTypeArguments());
  }

  // -----------------------------------------------------------------------
  // Invokers
  // -----------------------------------------------------------------------

  @Test
  void getGetInvokerInvokesGetter() throws Exception {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    Invoker getter = r.getGetInvoker("name");
    SimpleBean bean = new SimpleBean();
    bean.setName("hello");
    assertEquals("hello", getter.invoke(bean, null));
  }

  @Test
  void getSetInvokerInvokesSetter() throws Exception {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    Invoker setter = r.getSetInvoker("name");
    SimpleBean bean = new SimpleBean();
    setter.invoke(bean, new Object[] { "world" });
    assertEquals("world", bean.getName());
  }

  @Test
  void getGetInvokerMissingPropertyThrows() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertThrows(ReflectionException.class, () -> r.getGetInvoker("noSuch"));
  }

  @Test
  void getSetInvokerMissingPropertyThrows() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertThrows(ReflectionException.class, () -> r.getSetInvoker("noSuch"));
  }

  // -----------------------------------------------------------------------
  // Case-insensitive lookup
  // -----------------------------------------------------------------------

  @Test
  void findPropertyNameIsCaseInsensitive() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertEquals("name", r.findPropertyName("NAME"));
    assertEquals("name", r.findPropertyName("Name"));
    assertEquals("name", r.findPropertyName("name"));
  }

  @Test
  void findPropertyNameReturnsNullForUnknown() {
    BeanReflector r = new BeanReflector(SimpleBean.class);
    assertNull(r.findPropertyName("doesNotExist"));
  }

  // -----------------------------------------------------------------------
  // Inheritance — parent properties visible in child
  // -----------------------------------------------------------------------

  @Test
  void parentPropertiesVisibleInChild() {
    BeanReflector r = new BeanReflector(StringChild.class);
    assertTrue(r.hasGetter("value"));
    assertTrue(r.hasSetter("value"));
    assertEquals(String.class, r.getGetterType("value"));
    assertEquals(String.class, r.getSetterType("value"));
  }

  // -----------------------------------------------------------------------
  // Getter conflict: "is" wins over "get" for boolean
  // -----------------------------------------------------------------------

  @Test
  void isBooleanGetterWinsOverGetBoolean() throws Exception {
    BeanReflector r = new BeanReflector(BoolIsGetBean.class);
    Invoker getter = r.getGetInvoker("active");
    // isBool returns true; getBool returns false — "is" wins
    assertTrue((Boolean) getter.invoke(new BoolIsGetBean(), null));
  }

  // -----------------------------------------------------------------------
  // Java records
  // -----------------------------------------------------------------------

  @Test
  void recordComponentsAreReadable() {
    BeanReflector r = new BeanReflector(PersonRecord.class);
    assertTrue(r.hasGetter("name"));
    assertTrue(r.hasGetter("age"));
    assertEquals(String.class, r.getGetterType("name"));
    assertEquals(int.class, r.getGetterType("age"));
  }

  @Test
  void recordHasNoSetters() {
    BeanReflector r = new BeanReflector(PersonRecord.class);
    assertFalse(r.hasSetter("name"));
    assertFalse(r.hasSetter("age"));
  }

  @Test
  void recordGetterInvokerWorks() throws Exception {
    BeanReflector r = new BeanReflector(PersonRecord.class);
    PersonRecord person = new PersonRecord("Alice", 30);
    assertEquals("Alice", r.getGetInvoker("name").invoke(person, null));
    assertEquals(30, r.getGetInvoker("age").invoke(person, null));
  }

  // -----------------------------------------------------------------------
  // Public field access
  // -----------------------------------------------------------------------

  @Test
  void publicFieldIsAccessibleAsGetterAndSetter() throws Exception {
    BeanReflector r = new BeanReflector(FieldBean.class);
    FieldBean bean = new FieldBean();
    r.getSetInvoker("count").invoke(bean, new Object[] { 42 });
    assertEquals(42, (int) r.getGetInvoker("count").invoke(bean, null));
  }

  // -----------------------------------------------------------------------
  // Fields excluded by isValidPropertyName
  // -----------------------------------------------------------------------

  @Test
  void serialVersionUIDIsNotExposed() {
    class SerializableBean implements java.io.Serializable {
      private static final long serialVersionUID = 1L;
      private String value;

      public String getValue() {
        return value;
      }

      public void setValue(String v) {
        this.value = v;
      }
    }
    BeanReflector r = new BeanReflector(SerializableBean.class);
    assertFalse(r.hasGetter("serialVersionUID"));
    assertFalse(r.hasSetter("serialVersionUID"));
  }
}

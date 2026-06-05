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

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Map.Entry;

import org.apache.ibatis.internal.util.ClassUtils;
import org.apache.ibatis.reflection.invoker.Invoker;

/**
 * Lightweight {@link Reflector} implementation for types that carry no bean properties: primitive types ({@code int},
 * {@code long}, …), {@code void}, and other non-bean types.
 * <p>
 * Unlike {@link BeanReflector}, this implementation allocates <strong>no</strong> backing {@code HashMap}s,
 * {@code String[]} arrays, or constructor look-ups. The only field held is the {@code Class<?>} reference itself,
 * making instances extremely cheap to create and cache.
 * <p>
 * All property-access methods throw {@link ReflectionException} immediately, because these types genuinely have no
 * settable/gettable properties.
 * <h3>Applicability</h3> {@link #isApplicable(Class)} defines the set of types handled by this implementation. The
 * {@link Reflector#of(Type)} factory uses it to choose the right implementation.
 *
 * @since 3.6.0
 */
final class EmptyReflector implements Reflector {

  private static final String[] EMPTY_NAMES = new String[0];

  private final Class<?> clazz;

  EmptyReflector(Class<?> clazz) {
    this.clazz = clazz;
  }

  /**
   * Returns {@code true} for types that have no bean-style properties and therefore do not need a full
   * {@link BeanReflector}:
   * <ul>
   * <li>All eight Java primitives ({@code boolean}, {@code byte}, {@code char}, {@code short}, {@code int},
   * {@code long}, {@code float}, {@code double}).</li>
   * <li>{@code void.class}</li>
   * <li>Array types — arrays do not expose getter/setter properties.</li>
   * </ul>
   *
   * @param clazz
   *          the raw class to test
   *
   * @return {@code true} if {@link EmptyReflector} should be used for this class
   */
  static boolean isApplicable(Class<?> clazz) {
    return ClassUtils.isSimpleValueType(clazz) || clazz.isArray() || void.class.equals(clazz) || Object.class == clazz;
  }

  // -----------------------------------------------------------------------
  // Reflector interface
  // -----------------------------------------------------------------------

  @Override
  public Class<?> getType() {
    return clazz;
  }

  @Override
  public Constructor<?> getDefaultConstructor() {
    throw new ReflectionException("There is no default constructor for " + clazz
        + " (type has no bean properties and does not support reflective instantiation)");
  }

  @Override
  public boolean hasDefaultConstructor() {
    return false;
  }

  @Override
  public Invoker getSetInvoker(String propertyName) {
    throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + clazz + "'");
  }

  @Override
  public Invoker getGetInvoker(String propertyName) {
    throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
  }

  @Override
  public Class<?> getSetterType(String propertyName) {
    throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + clazz + "'");
  }

  @Override
  public Entry<Type, Class<?>> getGenericSetterType(String propertyName) {
    throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + clazz + "'");
  }

  @Override
  public Class<?> getGetterType(String propertyName) {
    throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
  }

  @Override
  public Entry<Type, Class<?>> getGenericGetterType(String propertyName) {
    throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
  }

  @Override
  public String[] getGettablePropertyNames() {
    return EMPTY_NAMES;
  }

  @Override
  public String[] getSettablePropertyNames() {
    return EMPTY_NAMES;
  }

  @Override
  public boolean hasSetter(String propertyName) {
    return false;
  }

  @Override
  public boolean hasGetter(String propertyName) {
    return false;
  }

  @Override
  public String findPropertyName(String name) {
    return null;
  }
}

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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.Invoker;

/**
 * Represents a cached snapshot of a class's reflection metadata, providing fast access to property names, getter/setter
 * invokers, and type information.
 * <p>
 * Two implementations exist:
 * <ul>
 * <li>{@link BeanReflector} — full implementation for ordinary JavaBeans, records, and classes with properties. Caches
 * method and field invokers in {@code HashMap}s.</li>
 * <li>{@link EmptyReflector} — lightweight implementation for primitive types, {@code void}, and other types that carry
 * no bean properties. Allocates no maps or arrays.</li>
 * </ul>
 * <p>
 * Use {@link ReflectorFactory#findForClass(Type)} to obtain an instance rather than constructing one directly. The
 * static helper {@link #of(Type)} performs the same routing logic without a factory.
 *
 * @author Clinton Begin
 */
public interface Reflector {

  /**
   * Returns the raw {@link Class} that this reflector describes.
   *
   * @return the reflected class
   */
  Class<?> getType();

  /**
   * Returns the no-argument constructor of the reflected class.
   *
   * @return the default constructor
   *
   * @throws ReflectionException
   *           if the class has no default constructor
   */
  Constructor<?> getDefaultConstructor();

  /**
   * Returns {@code true} if the reflected class has a no-argument constructor.
   *
   * @return whether a default constructor exists
   */
  boolean hasDefaultConstructor();

  /**
   * Returns the {@link Invoker} for the setter of the given property.
   *
   * @param propertyName
   *          the property name (case-sensitive)
   *
   * @return the setter invoker
   *
   * @throws ReflectionException
   *           if no setter exists for the property
   */
  Invoker getSetInvoker(String propertyName);

  /**
   * Returns the {@link Invoker} for the getter of the given property.
   *
   * @param propertyName
   *          the property name (case-sensitive)
   *
   * @return the getter invoker
   *
   * @throws ReflectionException
   *           if no getter exists for the property
   */
  Invoker getGetInvoker(String propertyName);

  /**
   * Returns the erased {@link Class} of the setter parameter for the given property.
   *
   * @param propertyName
   *          the property name
   *
   * @return the setter parameter class
   *
   * @throws ReflectionException
   *           if no setter exists for the property
   */
  Class<?> getSetterType(String propertyName);

  /**
   * Returns the generic {@link Type} and erased {@link Class} of the setter parameter.
   *
   * @param propertyName
   *          the property name
   *
   * @return a map entry whose key is the generic type and value is the erased class
   *
   * @throws ReflectionException
   *           if no setter exists for the property
   */
  Entry<Type, Class<?>> getGenericSetterType(String propertyName);

  /**
   * Returns the erased {@link Class} of the getter return type for the given property.
   *
   * @param propertyName
   *          the property name
   *
   * @return the getter return class
   *
   * @throws ReflectionException
   *           if no getter exists for the property
   */
  Class<?> getGetterType(String propertyName);

  /**
   * Returns the generic {@link Type} and erased {@link Class} of the getter return type.
   *
   * @param propertyName
   *          the property name
   *
   * @return a map entry whose key is the generic type and value is the erased class
   *
   * @throws ReflectionException
   *           if no getter exists for the property
   */
  Entry<Type, Class<?>> getGenericGetterType(String propertyName);

  /**
   * Returns all readable property names.
   *
   * @return array of readable property names
   */
  String[] getGettablePropertyNames();

  /**
   * Returns all writable property names.
   *
   * @return array of writable property names
   */
  String[] getSettablePropertyNames();

  /**
   * Returns {@code true} if the reflected class has a setter for the given property.
   *
   * @param propertyName
   *          the property name
   *
   * @return whether a setter exists
   */
  boolean hasSetter(String propertyName);

  /**
   * Returns {@code true} if the reflected class has a getter for the given property.
   *
   * @param propertyName
   *          the property name
   *
   * @return whether a getter exists
   */
  boolean hasGetter(String propertyName);

  /**
   * Performs a case-insensitive lookup for the canonical property name.
   *
   * @param name
   *          the property name to look up (any case)
   *
   * @return the canonical property name, or {@code null} if not found
   */
  String findPropertyName(String name);

  // -----------------------------------------------------------------------
  // Static factory and utilities
  // -----------------------------------------------------------------------

  /**
   * Creates the most appropriate {@link Reflector} implementation for {@code type}.
   * <ul>
   * <li>For primitive types, {@code void}, and other non-bean types, returns an {@link EmptyReflector} that allocates
   * no backing data structures.</li>
   * <li>For all other types, returns a {@link BeanReflector} with full property metadata.</li>
   * </ul>
   *
   * @param type
   *          the type to reflect
   *
   * @return an appropriate {@link Reflector} instance
   */
  static Reflector of(Type type) {
    Class<?> rawClass;
    if (type instanceof ParameterizedType) {
      rawClass = (Class<?>) ((ParameterizedType) type).getRawType();
    } else {
      rawClass = (Class<?>) type;
    }
    if (EmptyReflector.isApplicable(rawClass)) {
      return new EmptyReflector(rawClass);
    }
    return new BeanReflector(type);
  }
}

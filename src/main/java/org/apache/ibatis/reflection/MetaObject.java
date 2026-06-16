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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 */
public class MetaObject {

  private final Object originalObject;
  private final ObjectWrapper objectWrapper;
  private final ObjectFactory objectFactory;
  private final ObjectWrapperFactory objectWrapperFactory;
  private final ReflectorFactory reflectorFactory;

  private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory,
      ReflectorFactory reflectorFactory) {
    this.originalObject = object;
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;

    if (object instanceof ObjectWrapper) {
      this.objectWrapper = (ObjectWrapper) object;
    } else if (objectWrapperFactory.hasWrapperFor(object)) {
      this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
    } else if (object instanceof Map) {
      this.objectWrapper = new MapWrapper(this, (Map) object);
    } else if (object instanceof Collection) {
      this.objectWrapper = new CollectionWrapper(this, (Collection) object);
    } else {
      this.objectWrapper = new BeanWrapper(this, object);
    }
  }

  /**
   * Creates a MetaObject for the given object using default system factories.
   *
   * @param object
   *          the object to wrap in a MetaObject
   *
   * @return a MetaObject wrapping the given object, or a {@link SystemMetaObject#NULL_META_OBJECT} if the object is
   *         null
   */
  public static MetaObject forObject(Object object) {
    return SystemMetaObject.forObject(object);
  }

  public static MetaObject forObject(Object object, ObjectFactory objectFactory,
      ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    if (object == null) {
      return SystemMetaObject.NULL_META_OBJECT;
    }
    return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  public static ObjectFactory systemObjectFactory() {
    return SystemMetaObject.DEFAULT_OBJECT_FACTORY;
  }

  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  public ObjectWrapperFactory getObjectWrapperFactory() {
    return objectWrapperFactory;
  }

  public ReflectorFactory getReflectorFactory() {
    return reflectorFactory;
  }

  public Object getOriginalObject() {
    return originalObject;
  }

  public String findProperty(String propName, boolean useCamelCaseMapping) {
    return objectWrapper.findProperty(propName, useCamelCaseMapping);
  }

  public String[] getGetterNames() {
    return objectWrapper.getGetterNames();
  }

  public String[] getSetterNames() {
    return objectWrapper.getSetterNames();
  }

  public Class<?> getSetterType(String name) {
    return objectWrapper.getSetterType(name);
  }

  public Entry<Type, Class<?>> getGenericSetterType(String name) {
    return objectWrapper.getGenericSetterType(name);
  }

  public Class<?> getGetterType(String name) {
    return objectWrapper.getGetterType(name);
  }

  public Entry<Type, Class<?>> getGenericGetterType(String name) {
    return objectWrapper.getGenericGetterType(name);
  }

  public boolean hasSetter(String name) {
    return objectWrapper.hasSetter(name);
  }

  public boolean hasGetter(String name) {
    return objectWrapper.hasGetter(name);
  }

  public Object getValue(String name) {
    return objectWrapper.get(name);
  }

  public void setValue(String name, Object value) {
    objectWrapper.set(name, value);
  }

  public MetaObject metaObjectForProperty(String name) {
    Object value = getValue(name);
    return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  public ObjectWrapper getObjectWrapper() {
    return objectWrapper;
  }

  public boolean isCollection() {
    return objectWrapper.isCollection();
  }

  public void add(Object element) {
    objectWrapper.add(element);
  }

  public <E> void addAll(List<E> list) {
    objectWrapper.addAll(list);
  }

  public boolean hasProperty(String name) {
    return objectWrapper.hasProperty(name);
  }

  public boolean isNull() {
    return this == SystemMetaObject.NULL_META_OBJECT;
  }

  /**
   * @see MetaObject#getOrCreateCollection(String, Class)
   *
   * @param property
   *          property name
   *
   * @return the collection value assigned to the specified property
   */
  public <T> T getOrCreateCollection(String property) {
    return getOrCreateCollection(property, null);
  }

  /**
   * Returns the collection value assigned to the specified property, creating and assigning one when the current value
   * is {@code null}.
   * <p>
   * If the property already contains a collection, that existing instance is returned. If the property value is
   * {@code null}, this method determines the collection type from {@code expectedType} when it is specified, or from
   * the property's setter type otherwise. When the resolved type is a collection type according to the configured
   * {@link ObjectFactory}, a new collection instance is created, assigned to the property and returned.
   * </p>
   * <p>
   * This method returns {@code null} when the current property value is not a collection, or when the property is
   * {@code null} and the resolved type is not a collection type.
   * </p>
   * <p>
   * The generic return type is intended to reduce casts at call sites. This method verifies that values are
   * collections, but it does not enforce that an existing collection value has the exact runtime type represented by
   * {@code expectedType}.
   * </p>
   *
   * @param <T>
   *          the expected return type
   * @param property
   *          the property name whose collection value should be returned or created
   * @param expectedType
   *          the expected collection type to instantiate when the property is {@code null}; if {@code null}, the
   *          property's setter type is used
   *
   * @return the existing or newly created collection, or {@code null} if the property does not represent a collection
   *
   * @throws ReflectionException
   *           if a collection type is resolved but the configured {@link ObjectFactory} cannot instantiate it
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getOrCreateCollection(String property, @Nullable Class<T> expectedType) {
    Object propertyValue = this.getValue(property);
    if (propertyValue == null) {
      Class<?> type = expectedType;
      if (type == null) {
        type = this.getSetterType(property);
      }
      try {
        if (objectFactory.isCollection(type)) {
          propertyValue = objectFactory.create(type);
          this.setValue(property, propertyValue);
          return (T) propertyValue;
        }
      } catch (Exception e) {
        throw new ReflectionException(
            "Error instantiating collection property for result '" + property + "'.  Cause: " + e, e);
      }
    } else if (objectFactory.isCollection(propertyValue.getClass())) {
      return (T) propertyValue;
    }
    return null;
  }
}

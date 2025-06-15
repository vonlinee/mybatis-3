package org.apache.ibatis.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.ibatis.reflection.invoker.Invoker;

/**
 * reflection data for single type
 */
public interface Reflector {

  Class<?> getType();

  Constructor<?> getDefaultConstructor();

  boolean hasDefaultConstructor();

  Invoker getSetInvoker(String propertyName);

  Invoker getGetInvoker(String propertyName);

  Class<?> getSetterType(String propertyName);

  Map.Entry<Type, Class<?>> getGenericSetterType(String propertyName);

  Class<?> getGetterType(String propertyName);

  Map.Entry<Type, Class<?>> getGenericGetterType(String propertyName);

  String[] getGettablePropertyNames();

  String[] getSettablePropertyNames();

  boolean hasSetter(String propertyName);

  boolean hasGetter(String propertyName);

  String findPropertyName(String name);
}

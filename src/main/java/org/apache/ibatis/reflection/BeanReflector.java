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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.internal.util.ClassUtils;
import org.apache.ibatis.reflection.invoker.AmbiguousMethodInvoker;
import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;

/**
 * Full {@link Reflector} implementation for ordinary JavaBeans, records, and any class that exposes readable or
 * writable properties via getter/setter conventions or fields.
 * <p>
 * This implementation caches all property metadata in {@code HashMap}s at construction time so that subsequent look-ups
 * are O(1). Use {@link EmptyReflector} for primitive types and other types that carry no bean properties to avoid
 * allocating these maps unnecessarily.
 *
 * @author Clinton Begin
 */
class BeanReflector implements Reflector {

  private final Type type;
  private final Class<?> clazz;
  private final String[] readablePropertyNames;
  private final String[] writablePropertyNames;
  private final Map<String, Invoker> setMethods = new HashMap<>();
  private final Map<String, Invoker> getMethods = new HashMap<>();
  private final Map<String, Entry<Type, Class<?>>> setTypes = new HashMap<>();
  private final Map<String, Entry<Type, Class<?>>> getTypes = new HashMap<>();
  private final Constructor<?> defaultConstructor;

  private final Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

  private static final Entry<Type, Class<?>> NULL_ENTRY = new AbstractMap.SimpleImmutableEntry<>(null, null);

  public BeanReflector(Type type) {
    this.type = type;
    if (type instanceof ParameterizedType) {
      this.clazz = (Class<?>) ((ParameterizedType) type).getRawType();
    } else {
      this.clazz = (Class<?>) type;
    }
    this.defaultConstructor = ClassUtils.getDefaultConstructor(clazz);
    Method[] classMethods = ClassUtils.getUniqueMethodsInHierarchy(clazz);
    if (ClassUtils.isRecord(clazz)) {
      addRecordGetMethods(classMethods);
    } else {
      addGetMethods(classMethods);
      addSetMethods(classMethods);
      addFields(clazz);
    }
    readablePropertyNames = getMethods.keySet().toArray(new String[0]);
    writablePropertyNames = setMethods.keySet().toArray(new String[0]);
    for (String propName : readablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    for (String propName : writablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
  }

  // -----------------------------------------------------------------------
  // Property discovery — called only during construction
  // -----------------------------------------------------------------------

  private void addRecordGetMethods(Method[] methods) {
    Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 0)
        .forEach(m -> addGetMethod(m.getName(), m, false));
  }

  private void addGetMethods(Method[] methods) {
    Map<String, List<Method>> conflictingGetters = new HashMap<>();
    Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 0 && BeanUtils.isGetter(m.getName()))
        .forEach(m -> addMethodConflict(conflictingGetters, BeanUtils.methodToProperty(m.getName()), m));
    resolveGetterConflicts(conflictingGetters);
  }

  private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
    for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
      Method winner = null;
      String propName = entry.getKey();
      boolean isAmbiguous = false;
      for (Method candidate : entry.getValue()) {
        if (winner == null) {
          winner = candidate;
          continue;
        }
        Class<?> winnerType = winner.getReturnType();
        Class<?> candidateType = candidate.getReturnType();
        if (candidateType.equals(winnerType)) {
          if (!boolean.class.equals(candidateType)) {
            isAmbiguous = true;
            break;
          }
          // Both return boolean. Prefer an "is" getter over a "get" getter.
          // If both start with "is" they are genuinely ambiguous.
          boolean winnerIsIs = winner.getName().startsWith("is");
          boolean candidateIsIs = candidate.getName().startsWith("is");
          if (candidateIsIs && !winnerIsIs) {
            winner = candidate;
          } else if (winnerIsIs == candidateIsIs) {
            // Both are "is" or both are "get" — truly ambiguous.
            isAmbiguous = true;
            break;
          }
          // else: winner is already an "is" getter, keep it
        } else if (candidateType.isAssignableFrom(winnerType)) {
          // OK getter type is descendant
        } else if (winnerType.isAssignableFrom(candidateType)) {
          winner = candidate;
        } else {
          isAmbiguous = true;
          break;
        }
      }
      addGetMethod(propName, winner, isAmbiguous);
    }
  }

  private void addGetMethod(String name, Method method, boolean isAmbiguous) {
    MethodInvoker invoker = isAmbiguous ? new AmbiguousMethodInvoker(method, MessageFormat.format(
        "Illegal overloaded getter method with ambiguous type for property ''{0}'' in class ''{1}''. This breaks the JavaBeans specification and can cause unpredictable results.",
        name, method.getDeclaringClass().getName())) : new MethodInvoker(method);
    getMethods.put(name, invoker);
    Type returnType = TypeParameterResolver.resolveReturnType(method, type);
    getTypes.put(name, Map.entry(returnType, typeToClass(returnType)));
  }

  private void addSetMethods(Method[] methods) {
    Map<String, List<Method>> conflictingSetters = new HashMap<>();
    Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 1 && BeanUtils.isSetter(m.getName()))
        .forEach(m -> addMethodConflict(conflictingSetters, BeanUtils.methodToProperty(m.getName()), m));
    resolveSetterConflicts(conflictingSetters);
  }

  private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
    if (BeanUtils.isValidPropertyName(name)) {
      List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
      list.add(method);
    }
  }

  private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
    for (Entry<String, List<Method>> entry : conflictingSetters.entrySet()) {
      String propName = entry.getKey();
      List<Method> setters = entry.getValue();
      Class<?> getterType = getTypes.getOrDefault(propName, NULL_ENTRY).getValue();
      boolean isGetterAmbiguous = getMethods.get(propName) instanceof AmbiguousMethodInvoker;
      boolean isSetterAmbiguous = false;
      Method match = null;
      for (Method setter : setters) {
        if (!isGetterAmbiguous && setter.getParameterTypes()[0].equals(getterType)) {
          // should be the best match
          match = setter;
          break;
        }
        if (!isSetterAmbiguous) {
          match = pickBetterSetter(match, setter, propName);
          isSetterAmbiguous = match == null;
        }
      }
      if (match != null) {
        addSetMethod(propName, match);
      }
    }
  }

  private Method pickBetterSetter(Method setter1, Method setter2, String property) {
    if (setter1 == null) {
      return setter2;
    }
    Class<?> paramType1 = setter1.getParameterTypes()[0];
    Class<?> paramType2 = setter2.getParameterTypes()[0];
    if (paramType1.isAssignableFrom(paramType2)) {
      return setter2;
    }
    if (paramType2.isAssignableFrom(paramType1)) {
      return setter1;
    }
    MethodInvoker invoker = new AmbiguousMethodInvoker(setter1,
        MessageFormat.format(
            "Ambiguous setters defined for property ''{0}'' in class ''{1}'' with types ''{2}'' and ''{3}''.", property,
            setter2.getDeclaringClass().getName(), paramType1.getName(), paramType2.getName()));
    setMethods.put(property, invoker);
    Type[] paramTypes = TypeParameterResolver.resolveParamTypes(setter1, type);
    setTypes.put(property, Map.entry(paramTypes[0], typeToClass(paramTypes[0])));
    return null;
  }

  private void addSetMethod(String name, Method method) {
    MethodInvoker invoker = new MethodInvoker(method);
    setMethods.put(name, invoker);
    Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
    setTypes.put(name, Map.entry(paramTypes[0], typeToClass(paramTypes[0])));
  }

  private Class<?> typeToClass(Type src) {
    if (src instanceof Class) {
      return (Class<?>) src;
    } else if (src instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) src).getRawType();
    } else if (src instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) src).getGenericComponentType();
      if (componentType instanceof Class) {
        return Array.newInstance((Class<?>) componentType, 0).getClass();
      } else {
        Class<?> componentClass = typeToClass(componentType);
        return Array.newInstance(componentClass, 0).getClass();
      }
    }
    return Object.class;
  }

  private void addFields(Class<?> clazz) {
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      if (!setMethods.containsKey(field.getName())) {
        // issue #379 - removed the check for final because JDK 1.5 allows
        // modification of final fields through reflection (JSR-133). (JGB)
        // pr #16 - final static can only be set by the classloader
        int modifiers = field.getModifiers();
        if (!Modifier.isFinal(modifiers) || !Modifier.isStatic(modifiers)) {
          addSetField(field);
        }
      }
      if (!getMethods.containsKey(field.getName())) {
        addGetField(field);
      }
    }
    if (clazz.getSuperclass() != null) {
      addFields(clazz.getSuperclass());
    }
  }

  private void addSetField(Field field) {
    if (BeanUtils.isValidPropertyName(field.getName())) {
      setMethods.put(field.getName(), new SetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      setTypes.put(field.getName(), Map.entry(fieldType, typeToClass(fieldType)));
    }
  }

  private void addGetField(Field field) {
    if (BeanUtils.isValidPropertyName(field.getName())) {
      getMethods.put(field.getName(), new GetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      getTypes.put(field.getName(), Map.entry(fieldType, typeToClass(fieldType)));
    }
  }

  // -----------------------------------------------------------------------
  // Reflector interface implementation
  // -----------------------------------------------------------------------

  @Override
  public Class<?> getType() {
    return clazz;
  }

  @Override
  public Constructor<?> getDefaultConstructor() {
    if (defaultConstructor != null) {
      return defaultConstructor;
    }
    throw new ReflectionException("There is no default constructor for " + clazz);
  }

  @Override
  public boolean hasDefaultConstructor() {
    return defaultConstructor != null;
  }

  @Override
  public Invoker getSetInvoker(String propertyName) {
    Invoker method = setMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + clazz + "'");
    }
    return method;
  }

  @Override
  public Invoker getGetInvoker(String propertyName) {
    Invoker method = getMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
    }
    return method;
  }

  @Override
  public Class<?> getSetterType(String propertyName) {
    Entry<Type, Class<?>> entry = setTypes.get(propertyName);
    if (entry == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + clazz + "'");
    }
    return entry.getValue();
  }

  @Override
  public Entry<Type, Class<?>> getGenericSetterType(String propertyName) {
    Entry<Type, Class<?>> entry = setTypes.get(propertyName);
    if (entry == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + clazz + "'");
    }
    return entry;
  }

  @Override
  public Class<?> getGetterType(String propertyName) {
    Entry<Type, Class<?>> entry = getTypes.get(propertyName);
    if (entry == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
    }
    return entry.getValue();
  }

  @Override
  public Entry<Type, Class<?>> getGenericGetterType(String propertyName) {
    Entry<Type, Class<?>> entry = getTypes.get(propertyName);
    if (entry == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
    }
    return entry;
  }

  @Override
  public String[] getGettablePropertyNames() {
    return readablePropertyNames;
  }

  @Override
  public String[] getSettablePropertyNames() {
    return writablePropertyNames;
  }

  @Override
  public boolean hasSetter(String propertyName) {
    return setMethods.containsKey(propertyName);
  }

  @Override
  public boolean hasGetter(String propertyName) {
    return getMethods.containsKey(propertyName);
  }

  @Override
  public String findPropertyName(String name) {
    return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
  }
}

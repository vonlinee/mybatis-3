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

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReflectionUtils {

  private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES = Map.of(boolean.class, false, byte.class, (byte) 0,
      short.class, (short) 0, int.class, 0, long.class, 0L, float.class, 0F, double.class, 0D, char.class, '\0');

  private ReflectionUtils() {
    // Prevent Instantiation
  }

  public static List<String> getParamNames(Method method) {
    return getParameterNames(method);
  }

  public static List<String> getParamNames(Constructor<?> constructor) {
    return getParameterNames(constructor);
  }

  public static List<String> getParameterNames(Executable executable) {
    if (executable == null) {
      return Collections.emptyList();
    }
    List<String> parameterNames = new ArrayList<>();
    for (Parameter parameter : executable.getParameters()) {
      parameterNames.add(parameter.getName());
    }
    return parameterNames;
  }

  /**
   * Checks whether you can control member accessible.
   *
   * @return If you can control member accessible, it return {@literal true}
   *
   * @since 3.5.0
   */
  public static boolean canControlMemberAccessible() {
    return checkReflectionPermission();
  }

  @NotNull
  public static List<Integer> findParameterIndexesByType(@NotNull Method method, @NotNull Class<?> parameterType) {
    Objects.requireNonNull(method, "method is null");
    Objects.requireNonNull(parameterType, "parameterType is null");
    List<Integer> indexes = new ArrayList<>();
    Class<?>[] parameterTypes = method.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      if (parameterTypes[i] == parameterType) {
        indexes.add(i);
      }
    }
    return indexes;
  }

  /**
   * @param annotation
   *          annotation type
   * @param attributeName
   *          attribute name
   *
   * @return value
   *
   * @throws ClassCastException
   *           type mismatch
   */
  @SuppressWarnings("unchecked")
  public static <T> T getAnnotationAttributeValue(@NotNull Annotation annotation, @NotNull String attributeName) {
    Objects.requireNonNull(annotation, "annotation is null");
    Objects.requireNonNull(attributeName, "attributeName is null");
    try {
      return (T) annotation.annotationType().getMethod(attributeName).invoke(annotation);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new ReflectionException(e);
    }
  }

  /**
   * Instantiate a class using its 'primary' constructor (for Kotlin classes, potentially having default arguments
   * declared) or its default constructor (for regular Java classes, expecting a standard no-arg setup).
   * <p>
   * Note that this method tries to set the constructor accessible if given a non-accessible (that is, non-public)
   * constructor.
   *
   * @param clazz
   *          the class to instantiate
   *
   * @return the new instance
   *
   * @throws ReflectiveOperationException
   *           if the bean cannot be instantiated. The cause may notably indicate a {@link NoSuchMethodException} if no
   *           primary/default constructor was found, a {@link NoClassDefFoundError} or other {@link LinkageError} in
   *           case of an unresolvable class definition (e.g. due to a missing dependency at runtime), or an exception
   *           thrown from the constructor invocation itself.
   *
   * @see Constructor#newInstance
   */
  public static <T> T instantiateClass(Class<T> clazz) throws ReflectiveOperationException {
    Objects.requireNonNull(clazz, "Class must not be null");
    if (clazz.isInterface()) {
      throw new ReflectiveOperationException("Specified class " + clazz + "  is an interface");
    }
    if (clazz.isPrimitive()) {
      throw new ReflectiveOperationException("Specified class " + clazz + "  is primitive");
    }
    if (clazz.isAnnotation()) {
      throw new ReflectiveOperationException("Specified class " + clazz + "  is annotation");
    }
    if (Modifier.isAbstract(clazz.getModifiers())) {
      throw new ReflectiveOperationException("Specified class " + clazz + "  is abstract");
    }
    Constructor<T> constructor;
    try {
      constructor = clazz.getDeclaredConstructor();
    } catch (NoSuchMethodException ex) {
      throw new ReflectiveOperationException("No default constructor found in " + clazz, ex);
    } catch (LinkageError err) {
      throw new ReflectiveOperationException("Unresolvable class definition of " + clazz, err);
    }
    return instantiateClass(constructor);
  }

  /**
   * Convenience method to instantiate a class using the given constructor.
   * <p>
   * Note that this method tries to set the constructor accessible if given a non-accessible (that is, non-public)
   * constructor, and supports Kotlin classes with optional parameters and default values.
   *
   * @param constructor
   *          the constructor to instantiate
   * @param args
   *          the constructor arguments to apply (use {@code null} for an unspecified parameter, Kotlin optional
   *          parameters and Java primitive types are supported)
   *
   * @return the new instance
   *
   * @throws ReflectiveOperationException
   *           if the bean cannot be instantiated
   *
   * @see Constructor#newInstance
   */
  public static <T> T instantiateClass(Constructor<T> constructor, Object... args) throws ReflectiveOperationException {
    Objects.requireNonNull(constructor, "Constructor must not be null");
    try {
      ReflectionUtils.makeAccessible(constructor);
      int parameterCount = constructor.getParameterCount();
      if (parameterCount == 0) {
        return constructor.newInstance();
      }
      if (args.length >= parameterCount) {
        throw new ReflectiveOperationException("Can't specify more arguments than constructor parameters");
      }
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      Object[] argsWithDefaultValues = new Object[args.length];
      for (int i = 0; i < args.length; i++) {
        if (args[i] == null) {
          Class<?> parameterType = parameterTypes[i];
          argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
        } else {
          argsWithDefaultValues[i] = args[i];
        }
      }
      return constructor.newInstance(argsWithDefaultValues);
    } catch (InstantiationException ex) {
      throw new ReflectiveOperationException("Is it an abstract class?", ex);
    } catch (IllegalAccessException ex) {
      throw new ReflectiveOperationException("Is the constructor accessible?", ex);
    } catch (IllegalArgumentException ex) {
      throw new ReflectiveOperationException("Illegal arguments for constructor", ex);
    } catch (InvocationTargetException ex) {
      throw new ReflectiveOperationException("Constructor threw exception", ex.getTargetException());
    }
  }

  /**
   * Make the given constructor accessible, explicitly setting it accessible if necessary. The
   * {@code setAccessible(true)} method is only called when actually necessary, to avoid unnecessary conflicts.
   *
   * @param constructor
   *          the constructor to make accessible
   *
   * @see java.lang.reflect.Constructor#setAccessible
   */
  @SuppressWarnings("deprecation")
  public static void makeAccessible(@NotNull Constructor<?> constructor) {
    if ((!Modifier.isPublic(constructor.getModifiers())
        || !Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) && !constructor.isAccessible()) {
      constructor.setAccessible(true);
    }
  }

  public static void trySetAccessible(AccessibleObject ao) {
    trySetAccessible(ao, true);
  }

  public static void trySetAccessible(AccessibleObject ao, boolean accessible) {
    try {
      if (!ao.isAccessible()) {
        ao.setAccessible(accessible);
      }
    } catch (Throwable ignore) {
    }
  }

  public static Field[] getAllFields(Class<?> clazz) {
    List<Field> fieldList = new ArrayList<>();
    while (clazz != null) {
      fieldList.addAll(Arrays.asList(clazz.getDeclaredFields()));
      clazz = clazz.getSuperclass();
    }
    Field[] fields = new Field[fieldList.size()];
    return fieldList.toArray(fields);
  }

  /**
   * @see Method#toGenericString()
   * @see Method#toString()
   *
   * @param method
   *          method
   *
   * @return method signature as string
   */
  public static String getMethodSignatureAsString(Method method) {
    Objects.requireNonNull(method, "method is null");
    StringBuilder sb = new StringBuilder();
    Class<?> returnType = method.getReturnType();
    sb.append(returnType.getName()).append('#');
    sb.append(method.getName());
    Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      sb.append(i == 0 ? ':' : ',').append(parameters[i].getName());
    }
    return sb.toString();
  }

  /**
   * Checks whether you can control member accessible.
   *
   * @return If you can control member accessible, it return {@literal true}
   */
  public static boolean checkReflectionPermission() {
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (null != securityManager) {
        securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
      }
    } catch (SecurityException e) {
      return false;
    }
    return true;
  }

  @Nullable
  public static Class<?> determineClassOfType(@Nullable Type src) {
    if (src == null) {
      return null;
    }
    if (src instanceof Class) {
      return (Class<?>) src;
    } else if (src instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) src).getRawType();
    } else if (src instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) src).getGenericComponentType();
      if (componentType instanceof Class) {
        return Array.newInstance((Class<?>) componentType, 0).getClass();
      } else {
        Class<?> componentClass = determineClassOfType(componentType);
        return Array.newInstance(componentClass, 0).getClass();
      }
    }
    return Object.class;
  }

  @SuppressWarnings("unchecked")
  public static <E> Object convertToArray(List<E> list, Class<?> arrayComponentType) {
    Objects.requireNonNull(list, "list is null");
    Objects.requireNonNull(arrayComponentType, "componentType is null");
    Object array = Array.newInstance(arrayComponentType, list.size());
    if (!arrayComponentType.isPrimitive()) {
      return list.toArray((E[]) array);
    }
    for (int i = 0; i < list.size(); i++) {
      Array.set(array, i, list.get(i));
    }
    return array;
  }
}

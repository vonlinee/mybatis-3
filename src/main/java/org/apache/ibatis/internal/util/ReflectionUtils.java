/*
 *    Copyright 2009-2025 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

/**
 * Simple utility class for working with the reflection API and handling reflection exceptions.
 * <p>
 * Only intended for internal use.
 */
public final class ReflectionUtils {

  private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES = Map.of(boolean.class, false, byte.class, (byte) 0,
      short.class, (short) 0, int.class, 0, long.class, 0L, float.class, 0F, double.class, 0D, char.class, '\0');

  private ReflectionUtils() {
  }

  public static List<String> getParamNames(Method method) {
    return getParameterNames(method);
  }

  public static List<String> getParamNames(Constructor<?> constructor) {
    return getParameterNames(constructor);
  }

  private static List<String> getParameterNames(Executable executable) {
    return Arrays.stream(executable.getParameters()).map(Parameter::getName).collect(Collectors.toList());
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
}

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Simple utility class for working with the reflection API and handling reflection exceptions.
 * <p>
 * Only intended for internal use.
 */
public final class ReflectionUtils {

  private ReflectionUtils() {
  }

  /**
   * Creates an instance of the specified class using its default constructor.
   *
   * @param clazz
   *          the Class object representing the class to instantiate
   * @param <T>
   *          the type of the class to be instantiated
   *
   * @return a new instance of the specified class, or null if instantiation fails
   *
   * @throws IllegalAccessException
   *           if the default constructor is not accessible
   * @throws InstantiationException
   *           if the class that declares the underlying constructor represents an abstract class
   * @throws ExceptionInInitializerError
   *           if the initialization of the class fails
   */
  @SuppressWarnings("unchecked")
  public static <T> T createInstance(Class<?> clazz) throws ReflectiveOperationException {
    Objects.requireNonNull(clazz, "class must not be null");
    return (T) clazz.getDeclaredConstructor().newInstance();
  }

  public static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
    Set<Class<?>> interfaces = new HashSet<>();
    while (type != null) {
      for (Class<?> c : type.getInterfaces()) {
        if (signatureMap.containsKey(c)) {
          interfaces.add(c);
        }
      }
      type = type.getSuperclass();
    }
    return interfaces.toArray(new Class<?>[0]);
  }

  @SuppressWarnings("unchecked")
  public static <T> T createJdkProxy(@NotNull Class<T> interfaceType, @NotNull InvocationHandler ih) {
    Objects.requireNonNull(interfaceType, "interface type must not be null");
    Objects.requireNonNull(ih, "invocation handler must not be null");
    if (!interfaceType.isInterface()) {
      throw new UnsupportedOperationException(
          "not supported type " + interfaceType + " when create proxy based on jdk");
    }
    return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[] { interfaceType }, ih);
  }
}

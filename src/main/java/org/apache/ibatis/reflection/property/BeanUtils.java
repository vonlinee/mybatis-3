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
package org.apache.ibatis.reflection.property;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ibatis.internal.util.ReflectionUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.reflection.ReflectionException;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 */
public final class BeanUtils {

  private BeanUtils() {
    // Prevent Instantiation of Static Class
  }

  public static String methodToProperty(String name) {
    if (name.startsWith("is")) {
      name = name.substring(2);
    } else if (name.startsWith("get") || name.startsWith("set")) {
      name = name.substring(3);
    } else {
      throw new ReflectionException(
          "Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
    }

    if (name.length() == 1 || name.length() > 1 && !Character.isUpperCase(name.charAt(1))) {
      name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }

    return name;
  }

  public static boolean isProperty(String name) {
    return isGetter(name) || isSetter(name);
  }

  public static boolean isGetter(String name) {
    return name.startsWith("get") && name.length() > 3 || name.startsWith("is") && name.length() > 2;
  }

  public static boolean isSetter(String name) {
    return name.startsWith("set") && name.length() > 3;
  }

  public static void copyProperties(Class<?> type, Object sourceBean, Object destinationBean) {
    Class<?> parent = type;
    while (parent != null) {
      final Field[] fields = parent.getDeclaredFields();
      for (Field field : fields) {
        try {
          try {
            field.set(destinationBean, field.get(sourceBean));
          } catch (IllegalAccessException e) {
            if (!ReflectionUtils.canControlMemberAccessible()) {
              throw e;
            }
            field.setAccessible(true);
            field.set(destinationBean, field.get(sourceBean));
          }
        } catch (Exception e) {
          // Nothing useful to do, will only fail on final fields, which will be ignored.
        }
      }
      parent = parent.getSuperclass();
    }
  }

  /**
   * Simple introspection algorithm for basic set/get/is accessor methods, building corresponding JavaBeans property
   * descriptors for them.
   * <p>
   * This just supports the basic JavaBeans conventions, without indexed properties or any customizers, and without
   * other BeanInfo metadata. For standard JavaBeans introspection, use the JavaBeans Introspector.
   *
   * @param beanClass
   *          the target class to introspect
   *
   * @return a collection of property descriptors
   *
   * @throws IntrospectionException
   *           from introspecting the given bean class
   *
   * @see java.beans.Introspector#getBeanInfo(Class)
   */
  public static Collection<? extends PropertyDescriptor> getPropertyDescriptors(Class<?> beanClass)
      throws IntrospectionException {
    Map<String, LazyPropertyDescriptor> pdMap = new TreeMap<>();
    for (Method method : beanClass.getMethods()) {
      String methodName = method.getName();
      boolean setter;
      int nameIndex;
      if (methodName.startsWith("set") && method.getParameterCount() == 1) {
        setter = true;
        nameIndex = 3;
      } else if (methodName.startsWith("get") && method.getParameterCount() == 0
          && method.getReturnType() != Void.TYPE) {
        setter = false;
        nameIndex = 3;
      } else if (methodName.startsWith("is") && method.getParameterCount() == 0
          && method.getReturnType() == boolean.class) {
        setter = false;
        nameIndex = 2;
      } else {
        continue;
      }

      String propertyName = BeanUtils.uncapitalizeAsProperty(methodName.substring(nameIndex));
      if (propertyName.isEmpty()) {
        continue;
      }

      LazyPropertyDescriptor pd = pdMap.get(propertyName);
      if (pd != null) {
        if (setter) {
          Method writeMethod = pd.getWriteMethod();
          if (writeMethod == null
              || writeMethod.getParameterTypes()[0].isAssignableFrom(method.getParameterTypes()[0])) {
            pd.setWriteMethod(method);
          } else {
            pd.addWriteMethod(method);
          }
        } else {
          Method readMethod = pd.getReadMethod();
          if (readMethod == null
              || (readMethod.getReturnType() == method.getReturnType() && method.getName().startsWith("is"))) {
            pd.setReadMethod(method);
          }
        }
      } else {
        pd = new LazyPropertyDescriptor(propertyName, (!setter ? method : null), (setter ? method : null));
        pdMap.put(propertyName, pd);
      }
    }

    return pdMap.values();
  }

  private static class LazyPropertyDescriptor extends PropertyDescriptor {

    @Nullable
    private Method readMethod;

    @Nullable
    private Method writeMethod;

    private final List<Method> alternativeWriteMethods = new ArrayList<>();

    public LazyPropertyDescriptor(String propertyName, @Nullable Method readMethod, @Nullable Method writeMethod)
        throws IntrospectionException {
      super(propertyName, readMethod, writeMethod);
    }

    @Override
    public void setReadMethod(@Nullable Method readMethod) {
      this.readMethod = readMethod;
    }

    @Override
    @Nullable
    public Method getReadMethod() {
      return this.readMethod;
    }

    @Override
    public void setWriteMethod(@Nullable Method writeMethod) {
      this.writeMethod = writeMethod;
    }

    public void addWriteMethod(Method writeMethod) {
      if (this.writeMethod != null) {
        this.alternativeWriteMethods.add(this.writeMethod);
        this.writeMethod = null;
      }
      this.alternativeWriteMethods.add(writeMethod);
    }

    @Override
    @Nullable
    public Method getWriteMethod() {
      if (this.writeMethod == null && !this.alternativeWriteMethods.isEmpty()) {
        if (this.readMethod == null) {
          return this.alternativeWriteMethods.get(0);
        } else {
          for (Method method : this.alternativeWriteMethods) {
            if (this.readMethod.getReturnType().isAssignableFrom(method.getParameterTypes()[0])) {
              this.writeMethod = method;
              break;
            }
          }
        }
      }
      return this.writeMethod;
    }
  }

  /**
   * Uncapitalize a {@code String} in JavaBeans property format, changing the first letter to lower case as per
   * {@link Character#toLowerCase(char)}, unless the initial two letters are upper case in direct succession.
   *
   * @param str
   *          the {@code String} to uncapitalize
   *
   * @return the uncapitalized {@code String}
   *
   * @see java.beans.Introspector#decapitalize(String)
   */
  public static String uncapitalizeAsProperty(String str) {
    if (StringUtils.isEmpty(str)
        || (str.length() > 1 && Character.isUpperCase(str.charAt(0)) && Character.isUpperCase(str.charAt(1)))) {
      return str;
    }
    return StringUtils.changeFirstCharacterCase(str, false);
  }
}

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
package org.apache.ibatis.binding;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.ibatis.session.SqlSession;

class DefaultMapperMethod extends MapperMethod {
  private final MethodHandle methodHandle;

  private static final Method privateLookupInMethod;

  static {
    try {
      privateLookupInMethod = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          "There is no 'privateLookupIn(Class, Lookup)' method in java.lang.invoke.MethodHandles.", e);
    }
  }

  public DefaultMapperMethod(Method method) {
    try {
      methodHandle = getMethodHandleJava9(method);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
    return methodHandle.bindTo(proxy).invokeWithArguments(args);
  }

  private MethodHandle getMethodHandleJava9(Method method)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final Class<?> declaringClass = method.getDeclaringClass();
    return ((MethodHandles.Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup()))
        .findSpecial(declaringClass, method.getName(),
            MethodType.methodType(method.getReturnType(), method.getParameterTypes()), declaringClass);
  }
}

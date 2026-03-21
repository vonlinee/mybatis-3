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
package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -4724728412955527868L;
  private final SqlSession sqlSession;
  private final Class<T> mapperInterface;
  private final MapperMethod.Lookup methodLookup;

  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, MapperMethod.Lookup methodLookup) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodLookup = methodLookup;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, args);
      }
      MapperMethod mapperMethod = methodLookup.findMethod(proxy, mapperInterface, method, args, sqlSession);
      if (mapperMethod == null) {
        throw new BindingException(
            "Invalid bound statement (not found): " + mapperInterface.getName() + "." + method.getName());
      }
      return mapperMethod.invoke(proxy, method, args, sqlSession);
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }
}

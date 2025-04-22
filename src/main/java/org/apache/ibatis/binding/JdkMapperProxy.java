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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * all mapper proxy will hold a SqlSession instance
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
class JdkMapperProxy implements MapperProxy, InvocationHandler, Serializable {

  private static final long serialVersionUID = -4724728412955527868L;
  private final SqlSession sqlSession;
  private final Class<?> mapperInterface;
  private final MapperProxyFactory proxyFactory;

  public JdkMapperProxy(SqlSession sqlSession, Class<?> mapperInterface, MapperProxyFactory proxyFactory) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.proxyFactory = proxyFactory;
  }

  @Override
  public MapperProxyFactory getMapperProxyFactory() {
    return proxyFactory;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (Object.class.equals(method.getDeclaringClass())) {
      return method.invoke(this, args);
    }
    final MapperMethodInvoker invoker = proxyFactory.lookupInvoker(sqlSession, mapperInterface, proxy, method, args);
    if (invoker == null) {
      throw new BindingException("cannot find invoker for method " + method);
    }
    try {
      return invoker.invoke(proxy, method, args, sqlSession);
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }
}

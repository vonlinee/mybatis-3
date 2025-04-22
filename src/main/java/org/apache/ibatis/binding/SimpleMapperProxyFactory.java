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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

/**
 * @author Lasse Voss
 */
class SimpleMapperProxyFactory implements MapperProxyFactory {

  private final Map<Method, MapperMethodInvoker> methodCache = new ConcurrentHashMap<>();

  public Map<Method, MapperMethodInvoker> getMethodCache() {
    return methodCache;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T newInstance(Class<T> type, SqlSession sqlSession) {
    final JdkMapperProxy mapperProxy = new JdkMapperProxy(sqlSession, type, this);
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, mapperProxy);
  }

  @Override
  public MapperMethodInvoker lookupInvoker(SqlSession sqlSession, Class<?> targetClass, Object proxy, Method method,
      Object[] args) throws Throwable {
    try {
      return methodCache.computeIfAbsent(method, m -> {
        if (m.isDefault()) {
          return new DefaultMethodInvoker(method);
        }
        return new PlainMethodInvoker(targetClass, method, sqlSession.getConfiguration());
      });
    } catch (RuntimeException re) {
      Throwable cause = re.getCause();
      throw cause == null ? re : cause;
    }
  }
}

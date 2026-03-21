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

import static org.apache.ibatis.builder.annotation.MapperAnnotationBuilder.getQualifiedStatementId;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.jetbrains.annotations.Nullable;

public class MappedStatementMethodLookup implements MapperMethod.Lookup {

  @Override
  @Nullable
  public MapperMethod findMethod(Object proxy, Class<?> mapperInterface, Method method, Object[] args,
      SqlSession sqlSession) {
    if (method.getAnnotation(Flush.class) != null) {
      return FlushSessionMethod.INSTANCE;
    }
    final Configuration config = sqlSession.getConfiguration();
    final SqlCommand command = createSqlCommand(config, mapperInterface, method);
    if (command.getType() == SqlCommandType.UNKNOWN) {
      return null;
    }
    MethodSignature methodSignature = new MethodSignature(config, mapperInterface, method);
    return new MappedSqlMethod(command, methodSignature);
  }

  private SqlCommand createSqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
    MappedStatement ms = resolveMappedStatement(configuration, mapperInterface, method);
    String name;
    SqlCommandType type;
    if (ms == null) {
      name = null;
      type = SqlCommandType.UNKNOWN;
    } else {
      name = ms.getId();
      type = ms.getSqlCommandType();
      if (type == SqlCommandType.UNKNOWN) {
        throw new BindingException("Unknown execution method for: " + name);
      }
    }
    return new SqlCommand(name, type);
  }

  private MappedStatement resolveMappedStatement(Configuration configuration, Class<?> mapperInterface, Method method) {
    final Class<?> declaringClass = method.getDeclaringClass();
    return resolveMappedStatement(mapperInterface, method, declaringClass, configuration);
  }

  private MappedStatement resolveMappedStatement(Class<?> mapperInterface, Method method, Class<?> declaringClass,
      Configuration configuration) {
    String statementId = getQualifiedStatementId(mapperInterface, method);
    if (configuration.hasStatement(statementId)) {
      return configuration.getMappedStatement(statementId);
    }
    if (mapperInterface.equals(declaringClass)) {
      return null;
    }
    for (Class<?> superInterface : mapperInterface.getInterfaces()) {
      if (declaringClass.isAssignableFrom(superInterface)) {
        MappedStatement ms = resolveMappedStatement(superInterface, method, declaringClass, configuration);
        if (ms != null) {
          return ms;
        }
      }
    }
    return null;
  }

  private static final class FlushSessionMethod implements MapperMethod {

    static final FlushSessionMethod INSTANCE = new FlushSessionMethod();

    @Override
    public boolean isDefault() {
      return false;
    }

    @Override
    public SqlCommandType getSqlCommandType() {
      return SqlCommandType.FLUSH;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) {
      return sqlSession.flushStatements();
    }
  }
}

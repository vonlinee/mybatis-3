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
package org.apache.ibatis.session.defaults;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.JdbcStatement;
import org.apache.ibatis.session.SqlSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MapperStatement<S extends JdbcStatement<S>> implements JdbcStatement<S> {

  protected SqlSession sqlSession;

  protected Executor executor;

  @NotNull
  protected final MappedStatement ms;

  protected BoundSql boundSql;

  @Nullable
  protected Object parameter;

  MapperStatement(@NotNull SqlSession sqlSession, @NotNull Executor executor, @NotNull MappedStatement statement) {
    this.sqlSession = sqlSession;
    this.executor = executor;
    this.ms = statement;
  }

  @Override
  public final @NotNull SqlCommandType getSqlCommandType() {
    return ms.getSqlCommandType();
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull S bind(Object parameter) {
    this.parameter = parameter;
    return (S) this;
  }

  protected Object wrapCollection(final Object object) {
    return ParamNameResolver.wrapToMapIfCollection(object, null);
  }

  public MappedStatement getMappedStatement() {
    return ms;
  }

  @Nullable
  public Object getParameterObject() {
    return parameter;
  }

  @Override
  public String getSql() {
    if (boundSql == null) {
      return null;
    }
    return boundSql.getSql();
  }

  public String getStatementId() {
    return ms.getId();
  }

  public String getResource() {
    return ms.getResource();
  }
}

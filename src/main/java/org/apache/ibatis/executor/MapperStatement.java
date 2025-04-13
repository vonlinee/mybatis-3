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
package org.apache.ibatis.executor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.scripting.BoundSql;
import org.apache.ibatis.scripting.MappedStatement;
import org.apache.ibatis.scripting.SqlCommandType;
import org.apache.ibatis.scripting.StatementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MapperStatement implements JdbcStatement {

  @NotNull
  protected final MappedStatement ms;
  @NotNull
  protected final Configuration configuration;
  protected final SqlCommandType sqlCommandType;

  protected BoundSql boundSql;

  @Nullable
  protected Object parameterObject;

  protected MapperStatement(MappedStatement ms) {
    this.ms = ms;
    this.sqlCommandType = ms.getSqlCommandType();
    this.configuration = ms.getConfiguration();
  }

  public String getId() {
    return ms.getId();
  }

  @Override
  public SqlCommandType getSqlCommandType() {
    return sqlCommandType;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public MappedStatement getMappedStatement() {
    return ms;
  }

  @Nullable
  public Object getParameterObject() {
    return parameterObject;
  }

  public Log getStatementLog() {
    return ms.getStatementLog();
  }

  public boolean isCall() {
    return ms.getStatementType() == StatementType.CALLABLE;
  }

  public List<ParameterMapping> getParameterMappings() {
    Objects.requireNonNull(boundSql, "bound sql is not set");
    return Collections.unmodifiableList(boundSql.getParameterMappings());
  }
}

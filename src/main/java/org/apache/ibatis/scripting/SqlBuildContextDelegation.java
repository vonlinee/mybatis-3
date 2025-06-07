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
package org.apache.ibatis.scripting;

import java.util.List;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.sql.dialect.SQLDialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlBuildContextDelegation implements SqlBuildContext {

  @NotNull
  private final SqlBuildContext delegate;

  public SqlBuildContextDelegation(@NotNull SqlBuildContext delegate) {
    this.delegate = delegate;
  }

  @NotNull
  public SqlBuildContext getDelegate() {
    return delegate;
  }

  @Override
  public @NotNull Configuration getConfiguration() {
    return delegate.getConfiguration();
  }

  @Override
  public @NotNull ContextMap createBindings(@Nullable Object parameterObject) {
    return delegate.createBindings(parameterObject);
  }

  @Override
  public @NotNull ContextMap getBindings() {
    return delegate.getBindings();
  }

  @Override
  public void bind(@NotNull String name, Object value) {
    delegate.bind(name, value);
  }

  @Override
  public void appendSql(String sql) {
    delegate.appendSql(sql);
  }

  @Override
  public String getSql() {
    return delegate.getSql();
  }

  @Override
  public List<ParameterMapping> getParameterMappings() {
    return delegate.getParameterMappings();
  }

  @Override
  public String parseParam(String sql) {
    return delegate.parseParam(sql);
  }

  @Override
  public Object getParameterObject() {
    return delegate.getParameterObject();
  }

  @Override
  public Class<?> getParameterType() {
    return delegate.getParameterType();
  }

  @Override
  public ParamNameResolver getParamNameResolver() {
    return delegate.getParamNameResolver();
  }

  @Override
  public boolean isParamExists() {
    return delegate.isParamExists();
  }

  @Override
  public SQLDialect dialect() {
    return delegate.dialect();
  }
}

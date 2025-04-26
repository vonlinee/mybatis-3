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
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.ParamNameResolver;

public final class BufferedSqlBuildContext implements SqlBuildContext {

  private final SqlBuildContext delegate;
  private final StringBuilder sqlBuffer;

  public BufferedSqlBuildContext(SqlBuildContext delegate) {
    this.delegate = delegate;
    sqlBuffer = new StringBuilder();
  }

  @Override
  public void appendSql(String sql) {
    sqlBuffer.append(sql);
  }

  @Override
  public String getSql() {
    return sqlBuffer.toString();
  }

  @Override
  public Map<String, Object> getBindings() {
    return delegate.getBindings();
  }

  @Override
  public void bind(String name, Object value) {
    delegate.bind(name, value);
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
  public boolean isParamExists() {
    return delegate.isParamExists();
  }

  @Override
  public Class<?> getParameterType() {
    return delegate.getParameterType();
  }

  @Override
  public ParamNameResolver getParamNameResolver() {
    return delegate.getParamNameResolver();
  }
}

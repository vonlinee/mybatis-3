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
package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.ParameterMappingTokenHandler;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.ContextMap;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.sql.dialect.SQLDialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 */
public class DynamicContext implements SqlBuildContext {

  protected final ContextMap bindings;
  private final StringJoiner sqlBuilder = new StringJoiner(" ");

  private final Configuration configuration;
  private final Object parameterObject;
  private final Class<?> parameterType;
  private final ParamNameResolver paramNameResolver;
  private final boolean paramExists;

  private SQLDialect dialect;

  private GenericTokenParser tokenParser;
  private ParameterMappingTokenHandler tokenHandler;

  public DynamicContext(Configuration configuration, Object parameterObject, @Nullable Class<?> parameterType,
      @Nullable ParamNameResolver paramNameResolver, boolean paramExists) {
    this.configuration = configuration;
    if (configuration.getEnvironment() != null) {
      this.dialect = configuration.getEnvironment().getDialect();
    }
    this.parameterObject = parameterObject;
    this.paramExists = paramExists;
    this.parameterType = parameterType;
    this.paramNameResolver = paramNameResolver;
    this.bindings = createBindings(parameterObject);
  }

  public DynamicContext(@NotNull SqlBuildContext delegate) {
    this.configuration = delegate.getConfiguration();
    this.parameterObject = delegate.getParameterObject();
    this.paramExists = delegate.isParamExists();
    this.parameterType = delegate.getParameterType();
    this.paramNameResolver = delegate.getParamNameResolver();
    this.bindings = createBindings(parameterObject);
  }

  public void setDialect(SQLDialect dialect) {
    this.dialect = dialect;
  }

  @Override
  public @NotNull ContextMap createBindings(@Nullable Object parameterObject) {
    ContextMap bindings;
    if (parameterObject == null || parameterObject instanceof Map) {
      bindings = new ContextMap(null, false);
    } else {
      MetaObject metaObject = configuration.newMetaObject(parameterObject);
      boolean existsTypeHandler = configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass());
      bindings = new ContextMap(metaObject, existsTypeHandler);
    }
    bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
    bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
    return bindings;
  }

  @Override
  public @NotNull Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public @NotNull ContextMap getBindings() {
    return bindings;
  }

  @Override
  public void bind(@NotNull String name, Object value) {
    bindings.bind(name, value);
  }

  @Override
  public void appendSql(String sql) {
    sqlBuilder.add(sql);
  }

  @Override
  public String getSql() {
    return sqlBuilder.toString().trim();
  }

  private void initTokenParser(List<ParameterMapping> parameterMappings) {
    if (tokenParser == null) {
      tokenHandler = new ParameterMappingTokenHandler(parameterMappings != null ? parameterMappings : new ArrayList<>(),
          getConfiguration(), parameterObject, parameterType, bindings, paramNameResolver, paramExists);
      tokenParser = new GenericTokenParser("#{", "}", tokenHandler);
    }
  }

  @Override
  public List<ParameterMapping> getParameterMappings() {
    initTokenParser(null);
    return tokenHandler.getParameterMappings();
  }

  @Override
  public String parseParam(String sql) {
    initTokenParser(getParameterMappings());
    return tokenParser.parse(sql);
  }

  @Override
  public Object getParameterObject() {
    return parameterObject;
  }

  @Override
  public Class<?> getParameterType() {
    return parameterType;
  }

  @Override
  public ParamNameResolver getParamNameResolver() {
    return paramNameResolver;
  }

  @Override
  public boolean isParamExists() {
    return paramExists;
  }

  @Override
  public SQLDialect dialect() {
    return dialect;
  }
}

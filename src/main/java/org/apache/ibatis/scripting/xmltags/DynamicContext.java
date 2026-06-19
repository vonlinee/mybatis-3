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
package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.ibatis.builder.ParameterMappingTokenHandler;
import org.apache.ibatis.dialect.Dialect;
import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.ContextMap;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.session.Configuration;
import org.jetbrains.annotations.NotNull;

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
  private final ParamType paramType;

  private ParameterMappingTokenHandler tokenHandler;

  public DynamicContext(Configuration configuration, Class<?> parameterType, ParamNameResolver paramNameResolver,
      ParamType paramType) {
    this(configuration, null, parameterType, paramNameResolver, false, paramType);
  }

  public DynamicContext(SqlBuildContext delegate) {
    this(delegate.getConfiguration(), delegate.getParameterObject(), delegate.getParameterType(),
        delegate.getParamNameResolver(), delegate.isParamExists(), delegate.getParamType());
  }

  public DynamicContext(Configuration configuration, Object parameterObject, Class<?> parameterType,
      ParamNameResolver paramNameResolver, boolean paramExists, ParamType paramType) {
    this.configuration = configuration;
    this.bindings = createBindings(parameterObject);
    this.parameterObject = parameterObject;
    this.paramExists = paramExists;
    this.parameterType = parameterType;
    this.paramNameResolver = paramNameResolver;
    this.paramType = paramType;
  }

  @Override
  @NotNull
  public ContextMap createBindings(Object parameterObject) {
    final ContextMap bindings;
    if (parameterObject == null || parameterObject instanceof Map) {
      bindings = new ContextMap();
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
  public ParamType getParamType() {
    return paramType;
  }

  @Override
  public Map<String, Object> getBindings() {
    return bindings;
  }

  @Override
  public void bind(String name, Object value) {
    bindings.put(name, value);
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
    if (tokenHandler == null) {
      tokenHandler = new ParameterMappingTokenHandler(parameterMappings != null ? parameterMappings : new ArrayList<>(),
          configuration, parameterObject, parameterType, bindings, paramNameResolver, paramExists, paramType);
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
    return tokenHandler.parse(sql);
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
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public Environment getEnvironment() {
    return configuration.getEnvironment();
  }

  @Override
  public Dialect getDialect() {
    return configuration.getEnvironment().getDialect();
  }

  @Override
  public ExpressionEvaluator getExpressionEvaluator() {
    return configuration.getExpressionEvaluator();
  }
}

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
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
public class DynamicContext {

  public static final String PARAMETER_OBJECT_KEY = "_parameter";
  public static final String DATABASE_ID_KEY = "_databaseId";

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

  public DynamicContext(Configuration configuration, Object parameterObject, Class<?> parameterType,
      ParamNameResolver paramNameResolver, boolean paramExists, ParamType paramType) {
    if (parameterObject == null || parameterObject instanceof Map) {
      bindings = new ContextMap(null, false);
    } else {
      MetaObject metaObject = configuration.newMetaObject(parameterObject);
      boolean existsTypeHandler = configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass());
      bindings = new ContextMap(metaObject, existsTypeHandler);
    }
    bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
    bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
    this.configuration = configuration;
    this.parameterObject = parameterObject;
    this.paramExists = paramExists;
    this.parameterType = parameterType;
    this.paramNameResolver = paramNameResolver;
    this.paramType = paramType;
  }

  public DynamicContext(DynamicContext delegate) {
    this(delegate.getConfiguration(), delegate.getParameterObject(), delegate.getParameterType(),
        delegate.getParamNameResolver(), delegate.isParamExists(), delegate.getParamType());
  }

  public ParamType getParamType() {
    return paramType;
  }

  public Map<String, Object> getBindings() {
    return bindings;
  }

  public void bind(String name, Object value) {
    bindings.put(name, value);
  }

  public void appendSql(String sql) {
    sqlBuilder.add(sql);
  }

  public String getSql() {
    return sqlBuilder.toString().trim();
  }

  private void initTokenParser(List<ParameterMapping> parameterMappings) {
    if (tokenHandler == null) {
      tokenHandler = new ParameterMappingTokenHandler(parameterMappings != null ? parameterMappings : new ArrayList<>(),
          configuration, parameterObject, parameterType, bindings, paramNameResolver, paramExists, paramType);
    }
  }

  public List<ParameterMapping> getParameterMappings() {
    initTokenParser(null);
    return tokenHandler.getParameterMappings();
  }

  public String parseParam(String sql) {
    initTokenParser(getParameterMappings());
    return tokenHandler.parse(sql);
  }

  protected Object getParameterObject() {
    return parameterObject;
  }

  protected Class<?> getParameterType() {
    return parameterType;
  }

  protected ParamNameResolver getParamNameResolver() {
    return paramNameResolver;
  }

  protected boolean isParamExists() {
    return paramExists;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Environment getEnvironment() {
    return configuration.getEnvironment();
  }

  public Dialect getDialect() {
    return configuration.getEnvironment().getDialect();
  }

  public ExpressionEvaluator getExpressionEvaluator() {
    return configuration.getExpressionEvaluator();
  }
}

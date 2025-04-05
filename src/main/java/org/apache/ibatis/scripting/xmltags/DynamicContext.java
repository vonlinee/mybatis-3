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

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import ognl.OgnlContext;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.apache.ibatis.builder.ParameterMappingTokenHandler;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.parsing.TokenParser;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.ContextMap;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.session.Configuration;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 */
public class DynamicContext implements SqlBuildContext {

  static {
    OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
  }

  protected final ContextMap bindings;
  private final StringJoiner sqlBuilder = new StringJoiner(" ");

  private final Configuration configuration;
  private final Object parameterObject;
  private final Class<?> parameterType;
  private final ParamNameResolver paramNameResolver;
  private final boolean paramExists;

  private ParameterMappingTokenHandler tokenHandler;

  public DynamicContext(Configuration configuration, Class<?> parameterType, ParamNameResolver paramNameResolver) {
    this(configuration, null, parameterType, paramNameResolver, false);
  }

  public DynamicContext(Configuration configuration, Object parameterObject, Class<?> parameterType,
      ParamNameResolver paramNameResolver, boolean paramExists) {
    this.bindings = createBindings(configuration, parameterObject);
    this.configuration = configuration;
    this.parameterObject = parameterObject;
    this.paramExists = paramExists;
    this.parameterType = parameterType;
    this.paramNameResolver = paramNameResolver;
  }

  private ContextMap createBindings(Configuration configuration, Object parameterObject) {
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

  private void initTokenParser(@Nullable List<ParameterMapping> parameterMappings) {
    if (tokenHandler == null) {
      tokenHandler = new ParameterMappingTokenHandler(parameterMappings, configuration, parameterObject, parameterType,
          bindings, paramNameResolver, paramExists);
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
    return TokenParser.parse(sql, "#{", "}", tokenHandler);
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

  static class ContextAccessor implements PropertyAccessor {

    @Override
    public Object getProperty(OgnlContext context, Object target, Object name) {
      Map<?, ?> map = (Map<?, ?>) target;

      Object result = map.get(name);
      if (map.containsKey(name) || result != null) {
        return result;
      }

      Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
      if (parameterObject instanceof Map) {
        return ((Map<?, ?>) parameterObject).get(name);
      }

      return null;
    }

    @Override
    public void setProperty(OgnlContext context, Object target, Object name, Object value) {
      @SuppressWarnings("unchecked")
      Map<Object, Object> map = (Map<Object, Object>) target;
      map.put(name, value);
    }

    @Override
    public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }

    @Override
    public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }
  }
}

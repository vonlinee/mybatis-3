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
package org.apache.ibatis.scripting.defaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.builder.ParameterMappingTokenHandler;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;

/**
 * Static SqlSource. It is faster than {@link DynamicSqlSource} because mappings are calculated during startup.
 *
 * @since 3.2.0
 *
 * @author Eduardo Macarron
 */
public class RawSqlSource implements SqlSource {

  private final SqlSource sqlSource;

  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
    this(configuration, rootSqlNode, parameterType, null);
  }

  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    DynamicContext context = new DynamicContext(configuration, parameterType, paramNameResolver,
        configuration.getDefaultParamType());
    rootSqlNode.apply(context);
    String sql = context.getSql();
    sqlSource = SqlSourceBuilder.buildSqlSource(configuration, sql, context.getParameterMappings());
  }

  public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
    this(configuration, sql, parameterType, null);
  }

  public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    List<ParameterMapping> parameterMappings = new ArrayList<>();
    ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler(parameterMappings, configuration, null,
        parameterType, new HashMap<>(), paramNameResolver, false, configuration.getDefaultParamType());
    sqlSource = SqlSourceBuilder.buildSqlSource(configuration, tokenHandler.parse(sql), parameterMappings);
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject, ParamType paramType) {
    if (paramType == ParamType.NAMED) {
      throw new IllegalArgumentException("Named parameter type is not supported.");
    }
    return sqlSource.getBoundSql(parameterObject, paramType);
  }

}

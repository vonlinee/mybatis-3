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
package org.apache.ibatis.scripting.defaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.parsing.TokenParser;
import org.apache.ibatis.scripting.BoundSql;
import org.apache.ibatis.scripting.MethodParamMetadata;
import org.apache.ibatis.scripting.ParameterMappingTokenHandler;
import org.apache.ibatis.scripting.SqlSource;
import org.apache.ibatis.scripting.SqlUtils;
import org.apache.ibatis.scripting.StaticSqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;

/**
 * Static SqlSource. It is faster than {@link DynamicSqlSource} because mappings are calculated during startup.
 *
 * @author Eduardo Macarron
 *
 * @since 3.2.0
 */
public class RawSqlSource implements SqlSource {

  private final SqlSource sqlSource;

  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
    this(configuration, rootSqlNode, parameterType, null);
  }

  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType,
      MethodParamMetadata paramNameResolver) {
    DynamicContext context = new DynamicContext(configuration, parameterType, paramNameResolver);
    rootSqlNode.apply(context);
    String sql = context.getSql();

    sql = configuration.isShrinkWhitespacesInSql() ? SqlUtils.shrinkWhitespaces(sql) : sql;

    sqlSource = new StaticSqlSource(sql, context.getParameterMappings());
  }

  public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType,
      MethodParamMetadata paramNameResolver) {
    Class<?> clazz = parameterType == null ? Object.class : parameterType;
    List<ParameterMapping> parameterMappings = new ArrayList<>();
    ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler(parameterMappings, configuration,
        clazz, new HashMap<>(), paramNameResolver);
    String parsedSql = TokenParser.parse(sql, "#{", "}", tokenHandler);

    parsedSql = configuration.isShrinkWhitespacesInSql() ? SqlUtils.shrinkWhitespaces(parsedSql) : parsedSql;
    sqlSource = new StaticSqlSource(parsedSql, parameterMappings);
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return sqlSource.getBoundSql(parameterObject);
  }
}

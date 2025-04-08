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

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.scripting.BoundSql;
import org.apache.ibatis.scripting.MethodParamMetadata;
import org.apache.ibatis.scripting.SqlSource;
import org.apache.ibatis.scripting.SqlUtils;
import org.apache.ibatis.scripting.StaticSqlSource;

/**
 * @author Clinton Begin
 */
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;
  private final SqlNode rootSqlNode;
  private final MethodParamMetadata paramNameResolver;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this(configuration, rootSqlNode, null);
  }

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode, MethodParamMetadata paramNameResolver) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
    this.paramNameResolver = paramNameResolver;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    DynamicContext context = new DynamicContext(configuration, parameterObject, null, paramNameResolver, true);
    rootSqlNode.apply(context);
    String sql = context.getSql();
    StaticSqlSource staticSqlSource = new StaticSqlSource(
        configuration.isShrinkWhitespacesInSql() ? SqlUtils.shrinkWhitespaces(sql) : sql,
        context.getParameterMappings());
    BoundSql boundSql = staticSqlSource.getBoundSql(parameterObject);
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    return boundSql;
  }

}

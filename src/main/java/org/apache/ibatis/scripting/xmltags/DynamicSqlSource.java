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

import java.util.Map;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.ContextMap;
import org.apache.ibatis.scripting.SqlNode;

/**
 * @author Clinton Begin
 */
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    DynamicContext context = new DynamicContext(configuration, parameterObject, null, null, true);
    rootSqlNode.apply(context);
    String sql = context.getSql();
    if (configuration.isShrinkWhitespacesInSql()) {
      sql = SqlSourceBuilder.removeExtraWhitespaces(sql);
    }
    SqlSource sqlSource = new StaticSqlSource(sql, context.getParameterMappings());
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);

    ContextMap bindings = context.getBindings();
    if (!bindings.isEmpty()) {
      for (Map.Entry<String, Object> entry : bindings.entrySet()) {
        boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
      }
    }
    return boundSql;
  }

  public SqlNode getRootSqlNode() {
    return rootSqlNode;
  }
}

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

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.scripting.SqlBuildContext;

class ConditionConnectorSqlNode extends ConditionSqlNode {

  private final List<SqlNode> contents;
  private final String connector;

  public ConditionConnectorSqlNode(Configuration configuration, String connector, List<SqlNode> contents) {
    super(configuration);
    this.connector = connector;
    this.contents = contents;
  }

  @Override
  public String getConditionConnector() {
    return connector;
  }

  @Override
  public boolean apply(SqlBuildContext context) {
    if (testExpression != null) {
      if (!evaluator.evaluateBoolean(testExpression, context.getParameterObject())) {
        return false;
      }
    }
    DynamicContextWrapper wrapper = new DynamicContextWrapper(configuration, context);
    // remove leading and / or
    // nested
    context.appendSql(" ");
    context.appendSql(getConditionConnector());
    boolean res = false;
    context.appendSql(" (");
    for (SqlNode content : contents) {
      res = res | content.apply(wrapper);
    }
    context.appendSql(getNestedSqlFragments(wrapper.getSql()));
    context.appendSql(")");
    return true;
  }
}

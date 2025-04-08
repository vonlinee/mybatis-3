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
import org.apache.ibatis.scripting.SqlBuildContext;

/**
 * base class for <and/> | <or/> sql node
 *
 * @see ConditionSqlNode
 * @see ConditionConnectorSqlNode
 */
abstract class ConditionItemSqlNode extends ConditionSqlNode {

  protected final SqlNode sqlNode;

  ConditionItemSqlNode(Configuration configuration, SqlNode sqlNode) {
    super(configuration);
    this.sqlNode = sqlNode;
  }

  @Override
  public boolean apply(SqlBuildContext context) {
    DynamicContextWrapper wrapperContext = new DynamicContextWrapper(configuration, context);
    context.appendSql(" ");
    context.appendSql(getConditionConnector());
    context.appendSql(" ");
    boolean res = sqlNode.apply(wrapperContext);
    if (res) {
      context.appendSql(wrapperContext.getSql());
    }
    return res;
  }
}

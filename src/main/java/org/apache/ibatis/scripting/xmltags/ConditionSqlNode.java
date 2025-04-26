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

import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.scripting.BufferedSqlBuildContext;
import org.apache.ibatis.scripting.ExpressionEvaluator;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.session.Configuration;

/**
 * <p>
 * provide and/or sql fragment in xml mapping file.
 * </p>
 * there are 2 limitation about this tag: 1. contains multiple and / or 2. contains only single text node
 *
 * @author vonlinee
 *
 * @apiNote
 *
 * @since 2025-01-25 14:08
 **/
abstract class ConditionSqlNode extends MixedSqlNode {

  /**
   * TODO support evaluate test condition to decide whether append the sql.
   */
  protected String testExpression;
  protected final Configuration configuration;
  protected final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;

  public ConditionSqlNode(Configuration configuration, List<SqlNode> contents) {
    super(contents);
    this.configuration = configuration;
  }

  /**
   * and / or
   *
   * @return and / or
   *
   * @see AndSqlNode
   * @see OrSqlNode
   */
  public abstract String getConditionConnector();

  @Override
  public boolean apply(SqlBuildContext context) {
    if (testExpression != null) {
      if (!evaluator.evaluateBoolean(testExpression, context.getParameterObject())) {
        return false;
      }
    }
    boolean res;
    final BufferedSqlBuildContext bufferedContext = new BufferedSqlBuildContext(context);
    if (getContents().size() > 1) {
      res = super.apply(bufferedContext);
      // remove leading and / or
      // nested
      context.appendSql(" ");
      context.appendSql(getConditionConnector());
      context.appendSql(" (");
      context.appendSql(getNestedSqlFragments(bufferedContext.getSql()));
      context.appendSql(")");
    } else {
      res = super.apply(bufferedContext);
      context.appendSql(" ");
      context.appendSql(getConditionConnector());
      context.appendSql(" ");
      context.appendSql(bufferedContext.getSql());
    }
    return res;
  }

  protected String getNestedSqlFragments(String sql) {
    return StringUtils.deleteFirst(sql, getConditionConnector(), true);
  }
}

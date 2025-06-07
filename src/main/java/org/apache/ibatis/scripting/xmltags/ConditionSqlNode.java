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
import org.apache.ibatis.scripting.MixedSqlNode;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.scripting.SqlNode;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;

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
  protected final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;

  public ConditionSqlNode(List<SqlNode> contents) {
    super(contents);
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
    if (getChildCount() > 1) {
      BufferedSqlBuildContext wrapper = new BufferedSqlBuildContext(context);
      res = super.apply(wrapper);
      // remove leading and / or
      // nested
      context.appendSql(" ");
      context.appendSql(getConditionConnector());
      context.appendSql(" (");
      context.appendSql(getNestedSqlFragments(wrapper.getSql()));
      context.appendSql(")");
    } else {
      BufferedSqlBuildContext wrapperContext = new BufferedSqlBuildContext(context);
      res = super.apply(wrapperContext);
      context.appendSql(" ");
      context.appendSql(getConditionConnector());
      context.appendSql(" ");
      context.appendSql(wrapperContext.getSql());
    }
    return res;
  }

  protected String getNestedSqlFragments(String sql) {
    return StringUtils.deleteFirst(sql, getConditionConnector(), true);
  }
}

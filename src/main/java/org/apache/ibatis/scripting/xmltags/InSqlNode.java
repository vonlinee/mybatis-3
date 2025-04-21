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

import org.apache.ibatis.scripting.ExpressionEvaluator;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.session.Configuration;

/**
 * used when write sql with syntax like t.column in (1, 2, 3).
 *
 * @author vonlinee
 */
public class InSqlNode extends ForEachSqlNode {

  protected final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;

  /**
   * expression that evaluate to boolean value
   */
  private final String testExpression;

  public InSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String testExpression,
      String itemExpression, boolean nullable) {
    super(configuration, contents, collectionExpression, nullable, "index", itemExpression, "(", ")", ",");
    this.testExpression = testExpression;
  }

  @Override
  public boolean apply(SqlBuildContext context) {
    context.appendSql(" in ");
    return super.apply(context);
  }

  @Override
  public Iterable<?> getIterable(SqlBuildContext context) {
    if (this.testExpression == null) {
      return super.getIterable(context);
    }
    if (evaluator.evaluateBoolean(testExpression, context.getBindings())) {
      return super.getIterable(context);
    }
    return null;
  }
}

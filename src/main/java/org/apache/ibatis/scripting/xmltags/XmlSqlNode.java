package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.scripting.SqlNode;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;

abstract class XmlSqlNode implements SqlNode {

  protected ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;

  @Override
  public void setExpressionEvaluator(ExpressionEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @Override
  public final boolean isDynamic() {
    return true;
  }
}

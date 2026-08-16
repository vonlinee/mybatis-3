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
package org.apache.ibatis.scripting.expression;

/**
 * @author Clinton Begin
 */
public interface ExpressionEvaluator {

  Object getValue(String expression, Object root);

  boolean evaluateBoolean(String expression, Object parameterObject);

  default Iterable<?> evaluateIterable(String expression, Object parameterObject) {
    return evaluateIterable(expression, parameterObject, false);
  }

  default String postProcessExpression(String expression) {
    return expression;
  }

  /**
   * @since 3.5.9
   */
  Iterable<?> evaluateIterable(String expression, Object parameterObject, boolean nullable);
}

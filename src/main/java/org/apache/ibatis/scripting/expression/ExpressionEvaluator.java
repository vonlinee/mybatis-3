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
package org.apache.ibatis.scripting.expression;

import java.util.List;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.scripting.expression.ognl.OgnlExpressionEvaluator;
import org.jetbrains.annotations.NotNull;

/**
 * @author Clinton Begin
 *
 * @see ExpressionException
 */
public interface ExpressionEvaluator {

  ExpressionEvaluator INSTANCE = new OgnlExpressionEvaluator();

  /**
   * evaluate as any value
   *
   * @param expression
   *          expression that should evaluate a boolean value
   * @param parameterObject
   *          parameter object
   */
  Object getValue(String expression, Object parameterObject);

  /**
   * @param expression
   *          expression that should evaluate a boolean value
   * @param parameterObject
   *          parameter object
   */
  boolean evaluateBoolean(String expression, Object parameterObject);

  /**
   * @param expression
   *          expression that should evaluate a {@link Iterable}
   * @param parameterObject
   *          parameter object
   */
  Iterable<?> evaluateIterable(String expression, Object parameterObject);

  /**
   * @param expression
   *          expression that should evaluate a {@link Iterable}
   * @param parameterObject
   *          parameter object
   * @param nullable
   *          whether contains null value in {@link Iterable}
   *
   * @since 3.5.9
   */
  Iterable<?> evaluateIterable(String expression, Object parameterObject, boolean nullable);

  /**
   * @param expression
   *          expression
   *
   * @return parameters metadata
   *
   * @throws ExpressionException
   *           error when parse expression
   */
  List<ParameterMapping> collectParameters(@NotNull String expression);
}

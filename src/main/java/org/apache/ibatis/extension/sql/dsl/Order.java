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
package org.apache.ibatis.extension.sql.dsl;

public final class Order {

  private final Expression<?> expression;
  private final boolean ascending;

  private Order(Expression<?> expression, boolean ascending) {
    this.expression = expression;
    this.ascending = ascending;
  }

  public static Order asc(Expression<?> expression) {
    return new Order(expression, true);
  }

  public static Order desc(Expression<?> expression) {
    return new Order(expression, false);
  }

  Expression<?> expression() {
    return expression;
  }

  boolean ascending() {
    return ascending;
  }
}

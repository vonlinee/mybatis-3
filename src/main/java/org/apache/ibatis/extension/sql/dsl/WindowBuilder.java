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

import java.util.Objects;

public final class WindowBuilder {

  private final DslNodes.WindowNode node;

  WindowBuilder(Expression<?> expression) {
    this.node = new DslNodes.WindowNode((DslNodes.Node) expression);
  }

  public WindowBuilder partitionBy(Expression<?>... expressions) {
    for (Expression<?> expression : expressions) {
      if (expression != null) {
        node.partitionBy.add((DslNodes.Node) expression);
      }
    }
    return this;
  }

  public WindowBuilder orderBy(Order... orders) {
    for (Order order : orders) {
      if (order != null) {
        node.orderBy.add(order);
      }
    }
    return this;
  }

  public WindowBuilder orderBy(Expression<?>... expressions) {
    for (Expression<?> expression : expressions) {
      if (expression != null) {
        node.orderBy.add(Order.asc(expression));
      }
    }
    return this;
  }

  public WindowBuilder rowsBetween(WindowFrame start, WindowFrame end) {
    node.frameType = "ROWS";
    node.frameStart = Objects.requireNonNull(start, "start");
    node.frameEnd = Objects.requireNonNull(end, "end");
    return this;
  }

  public WindowBuilder rangeBetween(WindowFrame start, WindowFrame end) {
    node.frameType = "RANGE";
    node.frameStart = Objects.requireNonNull(start, "start");
    node.frameEnd = Objects.requireNonNull(end, "end");
    return this;
  }

  public WindowBuilder groupsBetween(WindowFrame start, WindowFrame end) {
    node.frameType = "GROUPS";
    node.frameStart = Objects.requireNonNull(start, "start");
    node.frameEnd = Objects.requireNonNull(end, "end");
    return this;
  }

  public Expression<Object> end() {
    if (node.frameType != null && (node.frameStart == null || node.frameEnd == null)) {
      throw new IllegalStateException("window frame requires both start and end");
    }
    return node;
  }
}

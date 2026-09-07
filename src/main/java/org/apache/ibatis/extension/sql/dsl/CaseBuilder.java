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

public final class CaseBuilder {

  private final DslNodes.CaseNode node = new DslNodes.CaseNode();

  public CaseBuilder when(Condition condition) {
    node.conditions.add((DslNodes.Node) condition);
    return this;
  }

  public CaseBuilder then(Object value) {
    node.values.add(DslNodes.valueOrExpression(value));
    return this;
  }

  public CaseBuilder otherwise(Object value) {
    node.otherwise = DslNodes.valueOrExpression(value);
    return this;
  }

  public Expression<Object> end() {
    if (node.conditions.size() != node.values.size()) {
      throw new IllegalStateException("CASE requires one THEN value for every WHEN condition");
    }
    return node;
  }
}

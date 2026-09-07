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

import org.apache.ibatis.session.Configuration;

public final class UpdateQuery extends DslNodes.StatementBase {

  UpdateQuery(Configuration configuration, Table<?> table) {
    super(configuration, DslNodes.update(table));
  }

  public UpdateQuery set(Column<?> column, Object value) {
    if (column != null) {
      ((DslNodes.UpdateNode) node).assignments
          .add(new DslNodes.AssignmentNode((DslNodes.Node) column, DslNodes.valueOrExpression(value)));
    }
    return this;
  }

  public UpdateQuery set(Expression<?> expression, Object value) {
    if (expression != null) {
      ((DslNodes.UpdateNode) node).assignments
          .add(new DslNodes.AssignmentNode((DslNodes.Node) expression, DslNodes.valueOrExpression(value)));
    }
    return this;
  }

  public UpdateQuery from(Table<?> table) {
    ((DslNodes.UpdateNode) node).from = DslNodes.tableSource(table);
    return this;
  }

  public UpdateQuery where(Condition condition) {
    ((DslNodes.UpdateNode) node).where = condition == null ? null : (DslNodes.Node) condition;
    return this;
  }

  public UpdateQuery returning(Expression<?>... expressions) {
    for (Expression<?> expression : expressions) {
      if (expression != null) {
        ((DslNodes.UpdateNode) node).returning.add((DslNodes.Node) expression);
      }
    }
    return this;
  }
}

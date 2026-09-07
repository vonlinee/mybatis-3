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

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.Configuration;

public final class InsertQuery extends DslNodes.StatementBase {

  InsertQuery(Configuration configuration, Table<?> table) {
    super(configuration, DslNodes.insert(table));
  }

  public InsertQuery columns(Column<?>... columns) {
    DslNodes.InsertNode insert = (DslNodes.InsertNode) node;
    for (Column<?> column : columns) {
      if (column != null) {
        insert.columns.add((DslNodes.Node) column);
      }
    }
    return this;
  }

  public InsertQuery values(Object... values) {
    DslNodes.InsertNode insert = (DslNodes.InsertNode) node;
    List<DslNodes.Node> row = new ArrayList<>();
    for (Object value : values) {
      row.add(DslNodes.valueOrExpression(value));
    }
    insert.rows.add(row);
    return this;
  }

  public InsertQuery valuesRows(List<Object[]> values) {
    for (Object[] rowValues : values) {
      values(rowValues);
    }
    return this;
  }

  public InsertQuery select(SelectQuery query) {
    ((DslNodes.InsertNode) node).select = query.node;
    return this;
  }

  public InsertQuery defaultValues() {
    ((DslNodes.InsertNode) node).defaultValues = true;
    return this;
  }

  public InsertQuery returning(Expression<?>... expressions) {
    DslNodes.InsertNode insert = (DslNodes.InsertNode) node;
    for (Expression<?> expression : expressions) {
      if (expression != null) {
        insert.returning.add((DslNodes.Node) expression);
      }
    }
    return this;
  }
}

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

/**
 * Builds the {@code WITH} prefix of a select statement before entering its {@code SELECT} clause.
 */
public final class WithQuery {

  private final List<DslNodes.CteNode> ctes = new ArrayList<>();
  private final Configuration configuration;

  WithQuery(Configuration configuration, String name, SqlStatement subquery, boolean recursive) {
    this.configuration = configuration;
    with(name, subquery, recursive);
  }

  public WithQuery with(String name, SqlStatement subquery) {
    return with(name, subquery, false);
  }

  public WithQuery withRecursive(String name, SqlStatement subquery) {
    return with(name, subquery, true);
  }

  public SelectQuery select(SelectItem... items) {
    return new SelectQuery(configuration, items, new ArrayList<>(ctes));
  }

  public SelectQuery select() {
    return select(new SelectItem[0]);
  }

  public SelectQuery select(Table<?> table) {
    return select(new DslNodes.RawExpressionNode((table.alias() == null ? table.tableName() : table.alias()) + ".*"));
  }

  public SelectQuery selectColumns(Table<?> table) {
    DslNodes.TableNode<?> node = (DslNodes.TableNode<?>) table;
    List<SelectItem> columns = new ArrayList<>();
    for (org.apache.ibatis.extension.metadata.ColumnInfo column : node.tableInfo().getColumns()) {
      columns.add((SelectItem) new DslNodes.ColumnNode<Object>(node, column.getColumnName()));
    }
    return select(columns.toArray(new SelectItem[0]));
  }

  private WithQuery with(String name, SqlStatement subquery, boolean recursive) {
    ctes.add(new DslNodes.CteNode(name, subquery, recursive));
    return this;
  }
}

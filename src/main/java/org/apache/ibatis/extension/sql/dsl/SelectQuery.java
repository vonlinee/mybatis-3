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
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.Configuration;

public final class SelectQuery extends DslNodes.StatementBase {

  SelectQuery(Configuration configuration, SelectItem[] items) {
    super(configuration, DslNodes.select(items));
  }

  SelectQuery(Configuration configuration, SelectItem[] items, List<DslNodes.CteNode> ctes) {
    this(configuration, items);
    ((DslNodes.SelectNode) node).ctes.addAll(ctes);
  }

  public SelectQuery distinct() {
    ((DslNodes.SelectNode) node).distinct = true;
    return this;
  }

  public SelectQuery distinctOn(Expression<?>... expressions) {
    if (expressions == null || expressions.length == 0) {
      throw new IllegalArgumentException("DISTINCT ON requires at least one expression");
    }
    DslNodes.SelectNode select = (DslNodes.SelectNode) node;
    for (Expression<?> expression : expressions) {
      if (expression != null) {
        select.distinctOn.add((DslNodes.Node) expression);
      }
    }
    if (select.distinctOn.isEmpty()) {
      throw new IllegalArgumentException("DISTINCT ON requires at least one expression");
    }
    return this;
  }

  public SelectQuery top(Object count) {
    ((DslNodes.SelectNode) node).top = DslNodes.valueOrExpression(count);
    return this;
  }

  public SelectQuery fetch(Object count) {
    DslNodes.SelectNode select = (DslNodes.SelectNode) node;
    select.fetch = DslNodes.valueOrExpression(count);
    select.fetchWithTies = false;
    return this;
  }

  public SelectQuery fetchWithTies(Object count) {
    DslNodes.SelectNode select = (DslNodes.SelectNode) node;
    select.fetch = DslNodes.valueOrExpression(count);
    select.fetchWithTies = true;
    return this;
  }

  public SelectQuery from(Table<?> table) {
    ((DslNodes.SelectNode) node).from = DslNodes.tableSource(table);
    return this;
  }

  public SelectQuery from(SqlStatement subquery, String alias) {
    ((DslNodes.SelectNode) node).from = DslNodes.subquerySource(subquery, alias);
    return this;
  }

  public SelectQuery with(String name, SqlStatement subquery) {
    return with(name, subquery, false);
  }

  public SelectQuery withRecursive(String name, SqlStatement subquery) {
    return with(name, subquery, true);
  }

  private SelectQuery with(String name, SqlStatement subquery, boolean recursive) {
    ((DslNodes.SelectNode) node).ctes.add(new DslNodes.CteNode(name, subquery, recursive));
    return this;
  }

  public SelectQuery where(Condition condition) {
    ((DslNodes.SelectNode) node).where = condition == null ? null : (DslNodes.Node) condition;
    return this;
  }

  public SelectQuery groupBy(Expression<?>... expressions) {
    DslNodes.SelectNode select = (DslNodes.SelectNode) node;
    for (Expression<?> expression : expressions) {
      if (expression != null) {
        select.groupBy.add((DslNodes.Node) expression);
      }
    }
    return this;
  }

  public SelectQuery having(Condition condition) {
    ((DslNodes.SelectNode) node).having = condition == null ? null : (DslNodes.Node) condition;
    return this;
  }

  public SelectQuery orderBy(Order... orders) {
    DslNodes.SelectNode select = (DslNodes.SelectNode) node;
    for (Order order : orders) {
      if (order != null) {
        select.orderBy.add(order);
      }
    }
    return this;
  }

  public SelectQuery orderBy(Expression<?>... expressions) {
    DslNodes.SelectNode select = (DslNodes.SelectNode) node;
    for (Expression<?> expression : expressions) {
      if (expression != null) {
        select.orderBy.add(Order.asc(expression));
      }
    }
    return this;
  }

  public SelectQuery limit(Object limit) {
    ((DslNodes.SelectNode) node).limit = DslNodes.value(limit);
    return this;
  }

  public SelectQuery offset(Object offset) {
    ((DslNodes.SelectNode) node).offset = DslNodes.value(offset);
    return this;
  }

  public SelectQuery forUpdate() {
    ((DslNodes.SelectNode) node).lock = "FOR UPDATE";
    return this;
  }

  public SelectQuery forShare() {
    ((DslNodes.SelectNode) node).lock = "FOR SHARE";
    return this;
  }

  public SelectQuery nowait() {
    ((DslNodes.SelectNode) node).lock = ((DslNodes.SelectNode) node).lock + " NOWAIT";
    return this;
  }

  public SelectQuery skipLocked() {
    ((DslNodes.SelectNode) node).lock = ((DslNodes.SelectNode) node).lock + " SKIP LOCKED";
    return this;
  }

  public SelectQuery join(JoinType type, Table<?> table, Condition condition) {
    return join(type, DslNodes.tableSource(table), condition, Collections.emptyList(), null);
  }

  public SelectQuery join(JoinType type, SqlStatement subquery, String alias, Condition condition) {
    return join(type, DslNodes.subquerySource(subquery, alias), condition, Collections.emptyList(), null);
  }

  public SelectQuery dialectJoin(String syntax, Table<?> table, Condition condition) {
    return join(JoinType.DIALECT, DslNodes.tableSource(table), condition, Collections.emptyList(), syntax);
  }

  public SelectQuery lateralJoin(SqlStatement subquery, String alias, Condition condition) {
    return join(JoinType.LATERAL, DslNodes.subquerySource(subquery, alias), condition, Collections.emptyList(), null);
  }

  public SelectQuery innerJoin(Table<?> table, Condition condition) {
    return join(JoinType.INNER, table, condition);
  }

  public SelectQuery leftJoin(Table<?> table, Condition condition) {
    return join(JoinType.LEFT, table, condition);
  }

  public SelectQuery rightJoin(Table<?> table, Condition condition) {
    return join(JoinType.RIGHT, table, condition);
  }

  public SelectQuery fullJoin(Table<?> table, Condition condition) {
    return join(JoinType.FULL, table, condition);
  }

  public SelectQuery crossJoin(Table<?> table) {
    return join(JoinType.CROSS, table, null);
  }

  public SelectQuery naturalJoin(Table<?> table) {
    return join(JoinType.NATURAL, table, null);
  }

  public SelectQuery joinUsing(JoinType type, Table<?> table, Column<?>... columns) {
    return joinUsing(type, DslNodes.tableSource(table), columns);
  }

  public SelectQuery joinUsing(JoinType type, SqlStatement subquery, String alias, Column<?>... columns) {
    return joinUsing(type, DslNodes.subquerySource(subquery, alias), columns);
  }

  public SelectQuery innerJoinUsing(Table<?> table, Column<?>... columns) {
    return joinUsing(JoinType.INNER, table, columns);
  }

  public SelectQuery leftJoinUsing(Table<?> table, Column<?>... columns) {
    return joinUsing(JoinType.LEFT, table, columns);
  }

  public SelectQuery rightJoinUsing(Table<?> table, Column<?>... columns) {
    return joinUsing(JoinType.RIGHT, table, columns);
  }

  public SelectQuery fullJoinUsing(Table<?> table, Column<?>... columns) {
    return joinUsing(JoinType.FULL, table, columns);
  }

  public SelectQuery union(SelectQuery other) {
    return set("UNION", other);
  }

  public SelectQuery unionAll(SelectQuery other) {
    return set("UNION ALL", other);
  }

  public SelectQuery intersect(SelectQuery other) {
    return set("INTERSECT", other);
  }

  public SelectQuery except(SelectQuery other) {
    return set("EXCEPT", other);
  }

  private SelectQuery set(String operator, SelectQuery other) {
    ((DslNodes.SelectNode) node).setOperations.add(new DslNodes.SetNode(operator, other.node));
    return this;
  }

  private SelectQuery join(JoinType type, DslNodes.Source source, Condition condition, List<DslNodes.Node> usingColumns,
      String syntax) {
    ((DslNodes.SelectNode) node).joins.add(new DslNodes.JoinNode(type, source,
        condition == null ? null : (DslNodes.Node) condition, usingColumns, syntax));
    return this;
  }

  private SelectQuery joinUsing(JoinType type, DslNodes.Source source, Column<?>... columns) {
    if (type == JoinType.CROSS || type == JoinType.NATURAL) {
      throw new IllegalArgumentException("USING is not valid for " + type + " JOIN");
    }
    if (columns == null || columns.length == 0) {
      throw new IllegalArgumentException("JOIN USING requires at least one column");
    }
    List<DslNodes.Node> usingColumns = new ArrayList<>(columns.length);
    for (Column<?> column : columns) {
      if (column == null) {
        throw new IllegalArgumentException("JOIN USING columns must not contain null");
      }
      usingColumns.add((DslNodes.Node) column);
    }
    ((DslNodes.SelectNode) node).joins.add(new DslNodes.JoinNode(type, source, null, usingColumns, null));
    return this;
  }
}

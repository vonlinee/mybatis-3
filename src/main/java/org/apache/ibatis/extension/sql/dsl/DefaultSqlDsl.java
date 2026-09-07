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
import java.util.Objects;

import org.apache.ibatis.session.Configuration;

public final class DefaultSqlDsl implements SqlDsl {

  private final Configuration configuration;

  public DefaultSqlDsl(Configuration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration");
  }

  @Override
  public SelectQuery select(SelectItem... items) {
    return new SelectQuery(configuration, items);
  }

  @Override
  public WithQuery with(String name, SqlStatement subquery) {
    return new WithQuery(configuration, name, subquery, false);
  }

  @Override
  public WithQuery withRecursive(String name, SqlStatement subquery) {
    return new WithQuery(configuration, name, subquery, true);
  }

  @Override
  public SelectQuery select() {
    return new SelectQuery(configuration, new SelectItem[0]);
  }

  @Override
  public SelectQuery select(Table<?> table) {
    return select(rawExpression((table.alias() == null ? table.tableName() : table.alias()) + ".*"));
  }

  @Override
  public SelectQuery selectColumns(Table<?> table) {
    DslNodes.TableNode<?> node = (DslNodes.TableNode<?>) table;
    List<SelectItem> columns = new ArrayList<>();
    for (org.apache.ibatis.extension.metadata.ColumnInfo column : node.tableInfo().getColumns()) {
      columns.add((SelectItem) new DslNodes.ColumnNode<Object>(node, column.getColumnName()));
    }
    return select(columns.toArray(new SelectItem[0]));
  }

  @Override
  public Alias alias(String name) {
    return expression -> new DslNodes.AliasNode(name, (DslNodes.Node) expression);
  }

  @Override
  public Expression<Object> ref(String name) {
    return new DslNodes.RefNode(name);
  }

  @Override
  public Condition when(boolean enabled, Condition condition) {
    return new DslNodes.WhenNode(enabled, (DslNodes.Node) condition);
  }

  @Override
  public Expression<Object> when(boolean enabled, Expression<?> expression) {
    return new DslNodes.WhenNode(enabled, (DslNodes.Node) expression);
  }

  @Override
  public Expression<Object> literal(Object value) {
    return new DslNodes.ValueNode(value);
  }

  @Override
  public Expression<Object> rawExpression(String sql) {
    return new DslNodes.RawExpressionNode(sql);
  }

  @Override
  public Table<Object> rawTable(String sql) {
    return new RawTable(sql);
  }

  @Override
  public Order asc(Expression<?> expression) {
    return Order.asc(expression);
  }

  @Override
  public Order desc(Expression<?> expression) {
    return Order.desc(expression);
  }

  @Override
  public Expression<Object> count(Expression<?> expression) {
    return function("COUNT", expression);
  }

  @Override
  public Expression<Object> sum(Expression<?> expression) {
    return function("SUM", expression);
  }

  @Override
  public Expression<Object> avg(Expression<?> expression) {
    return function("AVG", expression);
  }

  @Override
  public Expression<Object> min(Expression<?> expression) {
    return function("MIN", expression);
  }

  @Override
  public Expression<Object> max(Expression<?> expression) {
    return function("MAX", expression);
  }

  @Override
  public Expression<Object> coalesce(Object... expressions) {
    List<DslNodes.Node> nodes = new ArrayList<>();
    for (Object expression : expressions) {
      nodes.add(DslNodes.valueOrExpression(expression));
    }
    return new DslNodes.FunctionNode("COALESCE", nodes);
  }

  @Override
  public Expression<Object> function(String name, Expression<?>... expressions) {
    List<DslNodes.Node> nodes = new ArrayList<>();
    for (Expression<?> expression : expressions) {
      nodes.add((DslNodes.Node) expression);
    }
    return new DslNodes.FunctionNode(name, nodes);
  }

  @Override
  public CaseBuilder caseWhen() {
    return new CaseBuilder();
  }

  @Override
  public Condition and(Condition... conditions) {
    return new DslNodes.CompoundCondition("AND", nodes(conditions));
  }

  @Override
  public Condition or(Condition... conditions) {
    return new DslNodes.CompoundCondition("OR", nodes(conditions));
  }

  @Override
  public Condition exists(SqlStatement statement) {
    return new DslNodes.ExistsCondition(statement);
  }

  @Override
  public Expression<Object> scalar(SqlStatement statement) {
    return new DslNodes.SubqueryNode(statement);
  }

  @Override
  public InsertQuery insertInto(Table<?> table) {
    return new InsertQuery(configuration, table);
  }

  @Override
  public UpdateQuery update(Table<?> table) {
    return new UpdateQuery(configuration, table);
  }

  @Override
  public DeleteQuery deleteFrom(Table<?> table) {
    return new DeleteQuery(configuration, table);
  }

  private static DslNodes.Node[] nodes(Condition[] conditions) {
    DslNodes.Node[] nodes = new DslNodes.Node[conditions.length];
    for (int i = 0; i < conditions.length; i++) {
      nodes[i] = (DslNodes.Node) conditions[i];
    }
    return nodes;
  }

  private static final class RawTable implements Table<Object>, DslNodes.Source {
    private final String sql;

    RawTable(String sql) {
      this.sql = sql;
    }

    @Override
    public Class<Object> javaType() {
      return Object.class;
    }

    @Override
    public String tableName() {
      return sql;
    }

    @Override
    public String alias() {
      return null;
    }

    @Override
    public Table<Object> as(String alias) {
      return new RawTable(sql + " " + alias);
    }

    @Override
    public void render(DslNodes.RenderContext context) {
      context.append(sql);
    }
  }

}

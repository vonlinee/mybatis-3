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

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.extension.CompositeTableInfoParser;
import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.extension.metadata.ColumnInfo;
import org.apache.ibatis.extension.metadata.TableInfo;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;

final class DslNodes {

  private DslNodes() {
  }

  interface Node {
    void render(RenderContext context);
  }

  interface Source extends Node {
    void render(RenderContext context);
  }

  static abstract class StatementBase implements SqlStatement {
    final Configuration configuration;
    final Node node;

    StatementBase(Configuration configuration, Node node) {
      this.configuration = Objects.requireNonNull(configuration, "configuration");
      this.node = node;
    }

    @Override
    public RenderedSql getRenderedSql(ParamType paramType, SqlFormat format) {
      return getRenderedSql(paramType, format, SqlDialect.STANDARD);
    }

    @Override
    public RenderedSql getRenderedSql(ParamType paramType, SqlFormat format, SqlDialect dialect) {
      RenderContext context = new RenderContext(configuration, paramType, format,
          dialect == null ? SqlDialect.STANDARD : dialect);
      node.render(context);
      return context.result();
    }
  }

  static final class RenderContext {
    final Configuration configuration;
    final ParamType paramType;
    final SqlFormat format;
    final SqlDialect dialect;
    final StringBuilder sql = new StringBuilder();
    final List<ParameterMapping> mappings = new ArrayList<>();
    final List<Object> values = new ArrayList<>();
    int parameterIndex;
    int indent;

    RenderContext(Configuration configuration, ParamType paramType, SqlFormat format, SqlDialect dialect) {
      this.configuration = configuration;
      this.paramType = paramType;
      this.format = format;
      this.dialect = dialect;
    }

    void append(String text) {
      sql.append(text);
    }

    void space() {
      if (sql.length() > 0 && sql.charAt(sql.length() - 1) != ' ') {
        sql.append(' ');
      }
    }

    void keyword(String keyword) {
      space();
      append(keyword);
      append(" ");
    }

    void identifier(String identifier) {
      append(dialect.quoteIdentifier(identifier));
    }

    void comma() {
      append(",");
      if (format == SqlFormat.PRETTY) {
        append("\n");
        for (int i = 0; i < indent; i++) {
          append("  ");
        }
      } else {
        space();
      }
    }

    void newline() {
      if (format == SqlFormat.PRETTY) {
        append("\n");
        for (int i = 0; i < indent; i++) {
          append("  ");
        }
      } else {
        space();
      }
    }

    String parameter(Object value) {
      if (value == null) {
        throw new IllegalArgumentException("SQL parameter must not be null; use isNull() or isNotNull()");
      }
      int index = parameterIndex++;
      String name = "__dsl_" + index;
      values.add(value);
      if (paramType == ParamType.INLINED) {
        return literal(value);
      }
      Class<?> javaType = value.getClass();
      mappings.add(new ParameterMapping.Builder(name, javaType).value(value).build());
      return paramType == ParamType.NAMED ? "#{" + name + "}" : "?";
    }

    String literal(Object value) {
      if (value == null) {
        return "NULL";
      }
      if (value instanceof CharSequence || value instanceof Character || value instanceof Enum<?>) {
        return "'" + value.toString().replace("'", "''") + "'";
      }
      if (value instanceof Boolean) {
        return ((Boolean) value) ? "TRUE" : "FALSE";
      }
      if (value instanceof LocalDate || value instanceof LocalDateTime) {
        return "'" + value + "'";
      }
      if (value instanceof Number) {
        return value.toString();
      }
      throw new IllegalArgumentException("Unsupported inline SQL value type: " + value.getClass().getName());
    }

    RenderedSql result() {
      return new RenderedSql(sql.toString().trim(), mappings, values);
    }
  }

  static final class TableNode<T> implements Table<T>, Source {
    private static final Map<Class<?>, TableInfo> TABLE_INFO_CACHE = new ConcurrentHashMap<>();
    private final Class<T> javaType;
    private final String tableName;
    private final TableInfo tableInfo;
    private final String alias;

    TableNode(Class<T> javaType) {
      this(javaType, null);
    }

    private TableNode(Class<T> javaType, String alias) {
      this.javaType = Objects.requireNonNull(javaType, "javaType");
      TableInfo parsed = TABLE_INFO_CACHE.get(javaType);
      if (parsed == null) {
        parsed = new CompositeTableInfoParser().parse(javaType);
        TABLE_INFO_CACHE.put(javaType, parsed);
      }
      this.tableInfo = parsed;
      this.tableName = parsed.getTableName();
      this.alias = alias;
    }

    @Override
    public Class<T> javaType() {
      return javaType;
    }

    @Override
    public String tableName() {
      return tableName;
    }

    @Override
    public String alias() {
      return alias;
    }

    @Override
    public Table<T> as(String alias) {
      if (alias == null || alias.trim().isEmpty()) {
        throw new IllegalArgumentException("table alias must not be blank");
      }
      return new TableNode<>(javaType, alias);
    }

    @Override
    public void render(RenderContext context) {
      context.identifier(tableName);
      if (alias != null) {
        context.space();
        context.identifier(alias);
      }
    }

    String qualify(String columnName) {
      return (alias == null ? tableName : alias) + "." + columnName;
    }

    TableInfo tableInfo() {
      return tableInfo;
    }
  }

  static final class ColumnNode<T> extends BaseExpression<T> implements Column<T> {
    final String name;
    final TableNode<?> table;

    ColumnNode(TableNode<?> table, String name) {
      this.table = table;
      this.name = Objects.requireNonNull(name, "column name");
    }

    @Override
    public void render(RenderContext context) {
      if (table == null) {
        context.identifier(name);
      } else {
        context.identifier(table.alias() == null ? table.tableName() : table.alias());
        context.append(".");
        context.identifier(name);
      }
    }
  }

  static final class RawExpressionNode extends BaseExpression<Object> implements SelectItem {
    final String sql;

    RawExpressionNode(String sql) {
      this.sql = Objects.requireNonNull(sql, "sql");
    }

    @Override
    public void render(RenderContext context) {
      context.append(sql);
    }
  }

  static final class AliasNode extends BaseExpression<Object> implements SelectItem {
    final String alias;
    final Node expression;

    AliasNode(String alias, Node expression) {
      this.alias = Objects.requireNonNull(alias, "alias");
      this.expression = Objects.requireNonNull(expression, "expression");
    }

    @Override
    public void render(RenderContext context) {
      expression.render(context);
      context.space();
      context.append("AS ");
      context.identifier(alias);
    }
  }

  static final class ValueNode extends BaseExpression<Object> {
    final Object value;

    ValueNode(Object value) {
      this.value = value;
    }

    @Override
    public void render(RenderContext context) {
      context.append(context.parameter(value));
    }
  }

  static final class RefNode extends BaseExpression<Object> implements SelectItem {
    final String name;

    RefNode(String name) {
      this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public void render(RenderContext context) {
      context.identifier(name);
    }
  }

  static final class BinaryCondition extends BaseExpression<Boolean> implements Condition {
    final Node left;
    final String operator;
    final Node right;

    BinaryCondition(Node left, String operator, Node right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    public void render(RenderContext context) {
      left.render(context);
      context.space();
      context.append(operator);
      context.space();
      right.render(context);
    }
  }

  static final class UnaryCondition extends BaseExpression<Boolean> implements Condition {
    final Node expression;
    final String operator;

    UnaryCondition(String operator, Node expression) {
      this.operator = operator;
      this.expression = expression;
    }

    @Override
    public void render(RenderContext context) {
      context.append(operator);
      context.space();
      expression.render(context);
    }
  }

  static final class CompoundCondition extends BaseExpression<Boolean> implements Condition {
    final String operator;
    final List<Node> conditions;

    CompoundCondition(String operator, Node... conditions) {
      this.operator = operator;
      this.conditions = Arrays.asList(conditions);
    }

    @Override
    public void render(RenderContext context) {
      List<Node> activeConditions = new ArrayList<>();
      collectCompoundConditions(this, operator, activeConditions);
      if (activeConditions.isEmpty()) {
        return;
      }
      if (activeConditions.size() == 1) {
        activeConditions.get(0).render(context);
        return;
      }
      context.append("(");
      for (int i = 0; i < activeConditions.size(); i++) {
        if (i > 0) {
          context.space();
          context.append(operator);
          context.space();
        }
        Node condition = activeConditions.get(i);
        if (condition instanceof CompoundCondition) {
          condition.render(context);
        } else {
          context.append("(");
          condition.render(context);
          context.append(")");
        }
      }
      context.append(")");
    }
  }

  static final class InCondition extends BaseExpression<Boolean> implements Condition {
    final Node expression;
    final List<Node> values;

    InCondition(Node expression, List<Node> values) {
      this.expression = expression;
      this.values = values;
    }

    @Override
    public void render(RenderContext context) {
      if (values.isEmpty()) {
        throw new IllegalArgumentException("IN requires at least one value");
      }
      expression.render(context);
      context.append(" IN (");
      for (int i = 0; i < values.size(); i++) {
        if (i > 0) {
          context.append(", ");
        }
        values.get(i).render(context);
      }
      context.append(")");
    }
  }

  static final class IsNullCondition extends BaseExpression<Boolean> implements Condition {
    final Node expression;
    final boolean negated;

    IsNullCondition(Node expression, boolean negated) {
      this.expression = expression;
      this.negated = negated;
    }

    @Override
    public void render(RenderContext context) {
      expression.render(context);
      context.append(negated ? " IS NOT NULL" : " IS NULL");
    }
  }

  static final class WhenNode extends BaseExpression<Object> implements Condition {
    final boolean enabled;
    final Node node;

    WhenNode(boolean enabled, Node node) {
      this.enabled = enabled;
      this.node = node;
    }

    @Override
    public void render(RenderContext context) {
      if (enabled) {
        node.render(context);
      }
    }
  }

  static final class FunctionNode extends BaseExpression<Object> implements SelectItem {
    final String name;
    final List<Node> arguments;

    FunctionNode(String name, List<Node> arguments) {
      this.name = name;
      this.arguments = arguments;
    }

    @Override
    public void render(RenderContext context) {
      context.append(name);
      context.append("(");
      for (int i = 0; i < arguments.size(); i++) {
        if (i > 0) {
          context.append(", ");
        }
        arguments.get(i).render(context);
      }
      context.append(")");
    }
  }

  static final class WindowNode extends BaseExpression<Object> implements SelectItem {
    final Node expression;
    final List<Node> partitionBy = new ArrayList<>();
    final List<Order> orderBy = new ArrayList<>();
    String frameType;
    WindowFrame frameStart;
    WindowFrame frameEnd;

    WindowNode(Node expression) {
      this.expression = expression;
    }

    @Override
    public void render(RenderContext context) {
      expression.render(context);
      context.keyword("OVER");
      context.append("(");
      boolean hasClause = false;
      if (!partitionBy.isEmpty()) {
        context.append("PARTITION BY ");
        renderNodes(context, partitionBy);
        hasClause = true;
      }
      if (!orderBy.isEmpty()) {
        if (hasClause) {
          context.space();
        }
        context.append("ORDER BY ");
        renderOrders(context, orderBy);
        hasClause = true;
      }
      if (frameType != null) {
        if (hasClause) {
          context.space();
        }
        context.append(frameType);
        context.space();
        context.append("BETWEEN ");
        context.append(frameStart.sql());
        context.space();
        context.append("AND ");
        context.append(frameEnd.sql());
      }
      context.append(")");
    }
  }

  static final class CaseNode extends BaseExpression<Object> implements SelectItem {
    final List<Node> conditions = new ArrayList<>();
    final List<Node> values = new ArrayList<>();
    Node otherwise;

    @Override
    public void render(RenderContext context) {
      context.append("CASE");
      for (int i = 0; i < conditions.size(); i++) {
        context.keyword("WHEN");
        conditions.get(i).render(context);
        context.keyword("THEN");
        values.get(i).render(context);
      }
      if (otherwise != null) {
        context.keyword("ELSE");
        otherwise.render(context);
      }
      context.keyword("END");
    }
  }

  static final class SourceNode implements Source {
    final Node node;
    final String alias;

    SourceNode(Node node, String alias) {
      this.node = node;
      this.alias = alias;
    }

    @Override
    public void render(RenderContext context) {
      node.render(context);
      if (alias != null) {
        context.space();
        context.identifier(alias);
      }
    }
  }

  static final class CteNode {
    final String name;
    final SqlStatement statement;
    final boolean recursive;

    CteNode(String name, SqlStatement statement, boolean recursive) {
      this.name = name;
      this.statement = statement;
      this.recursive = recursive;
    }
  }

  static final class JoinNode {
    final JoinType type;
    final Source source;
    final Node condition;
    final List<Node> usingColumns;
    final String syntax;

    JoinNode(JoinType type, Source source, Node condition, List<Node> usingColumns, String syntax) {
      this.type = type;
      this.source = source;
      this.condition = condition;
      this.usingColumns = usingColumns;
      this.syntax = syntax;
    }
  }

  static final class SetNode {
    final String operator;
    final Node statement;

    SetNode(String operator, Node statement) {
      this.operator = operator;
      this.statement = statement;
    }
  }

  static final class AssignmentNode {
    final Node column;
    final Node value;

    AssignmentNode(Node column, Node value) {
      this.column = column;
      this.value = value;
    }
  }

  static final class SelectNode implements Node {
    final List<Node> items = new ArrayList<>();
    final List<CteNode> ctes = new ArrayList<>();
    final List<JoinNode> joins = new ArrayList<>();
    final List<Node> groupBy = new ArrayList<>();
    final List<Node> distinctOn = new ArrayList<>();
    final List<Order> orderBy = new ArrayList<>();
    final List<SetNode> setOperations = new ArrayList<>();
    Node from;
    Node where;
    Node having;
    Node limit;
    Node offset;
    Node top;
    Node fetch;
    boolean fetchWithTies;
    String lock;
    boolean distinct;

    SelectNode(SelectItem[] items) {
      if (items != null) {
        for (SelectItem item : items) {
          if (item instanceof Node) {
            this.items.add((Node) item);
          }
        }
      }
    }

    @Override
    public void render(RenderContext context) {
      renderCtes(context, ctes);
      context.append("SELECT");
      if (!distinctOn.isEmpty()) {
        context.keyword("DISTINCT ON");
        context.append("(");
        renderNodes(context, distinctOn);
        context.append(")");
      } else if (distinct) {
        context.space();
        context.append("DISTINCT");
      }
      if (top != null) {
        context.keyword("TOP");
        top.render(context);
      } else if (context.dialect.paginationStyle() == SqlDialect.PaginationStyle.TOP && limit != null
          && offset == null) {
        context.keyword("TOP");
        limit.render(context);
      }
      context.space();
      List<Node> activeItems = active(items);
      if (activeItems.isEmpty()) {
        context.append("*");
      } else {
        for (int i = 0; i < activeItems.size(); i++) {
          if (i > 0) {
            context.comma();
          }
          activeItems.get(i).render(context);
        }
      }
      if (from != null) {
        context.keyword("FROM");
        from.render(context);
        for (JoinNode join : joins) {
          renderJoin(context, join);
        }
      } else if (!joins.isEmpty()) {
        throw new IllegalStateException("JOIN requires FROM");
      }
      if (where != null && hasActive(where)) {
        context.keyword("WHERE");
        where.render(context);
      }
      List<Node> activeGroupBy = active(groupBy);
      if (!activeGroupBy.isEmpty()) {
        context.keyword("GROUP BY");
        renderNodes(context, activeGroupBy);
      }
      if (having != null && hasActive(having)) {
        context.keyword("HAVING");
        having.render(context);
      }
      List<Order> activeOrderBy = new ArrayList<>();
      for (Order order : orderBy) {
        if (isActive((Node) order.expression())) {
          activeOrderBy.add(order);
        }
      }
      if (!activeOrderBy.isEmpty()) {
        context.keyword("ORDER BY");
        renderOrders(context, activeOrderBy);
      }
      renderPagination(context);
      if (lock != null) {
        context.keyword(lock);
      }
      for (SetNode set : setOperations) {
        context.keyword(set.operator);
        set.statement.render(context);
      }
    }

    private void renderPagination(RenderContext context) {
      if (context.dialect.paginationStyle() == SqlDialect.PaginationStyle.TOP) {
        if (offset != null) {
          context.keyword("OFFSET");
          offset.render(context);
          context.keyword("ROWS");
          if (fetch != null || limit != null) {
            context.keyword("FETCH NEXT");
            (fetch == null ? limit : fetch).render(context);
            context.keyword(fetchWithTies ? "ROWS WITH TIES" : "ROWS ONLY");
          }
        } else if (top != null && fetch != null) {
          context.keyword("FETCH FIRST");
          fetch.render(context);
          context.keyword(fetchWithTies ? "ROWS WITH TIES" : "ROWS ONLY");
        }
      } else if (context.dialect.paginationStyle() == SqlDialect.PaginationStyle.OFFSET_FETCH) {
        if (offset != null) {
          context.keyword("OFFSET");
          offset.render(context);
          context.keyword("ROWS");
        }
        if (fetch != null || limit != null) {
          context.keyword(offset == null ? "FETCH FIRST" : "FETCH NEXT");
          (fetch == null ? limit : fetch).render(context);
          context.keyword(fetchWithTies ? "ROWS WITH TIES" : "ROWS ONLY");
        }
      } else {
        if (limit != null) {
          context.keyword("LIMIT");
          limit.render(context);
        }
        if (offset != null) {
          context.keyword("OFFSET");
          offset.render(context);
        }
        if (fetch != null) {
          context.keyword("FETCH FIRST");
          fetch.render(context);
          context.keyword(fetchWithTies ? "ROWS WITH TIES" : "ROWS ONLY");
        }
      }
    }
  }

  static final class InsertNode implements Node {
    final TableNode<?> table;
    final List<Node> columns = new ArrayList<>();
    final List<List<Node>> rows = new ArrayList<>();
    final List<Node> returning = new ArrayList<>();
    Node select;
    boolean defaultValues;

    InsertNode(TableNode<?> table) {
      this.table = table;
    }

    @Override
    public void render(RenderContext context) {
      context.append("INSERT INTO ");
      table.render(context);
      if (!columns.isEmpty()) {
        context.space();
        context.append("(");
        renderInsertColumns(context, columns);
        context.append(")");
      }
      if (defaultValues) {
        context.keyword("DEFAULT VALUES");
      } else if (select != null) {
        context.space();
        select.render(context);
      } else {
        if (rows.isEmpty()) {
          throw new IllegalStateException("INSERT requires values, SELECT, or DEFAULT VALUES");
        }
        context.keyword("VALUES");
        for (int i = 0; i < rows.size(); i++) {
          if (i > 0) {
            context.comma();
          }
          context.append("(");
          renderNodes(context, rows.get(i));
          context.append(")");
        }
      }
      renderReturning(context, returning);
    }
  }

  static final class UpdateNode implements Node {
    final TableNode<?> table;
    final List<AssignmentNode> assignments = new ArrayList<>();
    final List<Node> returning = new ArrayList<>();
    Node from;
    Node where;

    UpdateNode(TableNode<?> table) {
      this.table = table;
    }

    @Override
    public void render(RenderContext context) {
      if (assignments.isEmpty()) {
        throw new IllegalStateException("UPDATE requires at least one assignment");
      }
      context.append("UPDATE ");
      table.render(context);
      context.keyword("SET");
      for (int i = 0; i < assignments.size(); i++) {
        if (i > 0) {
          context.comma();
        }
        assignments.get(i).column.render(context);
        context.space();
        context.append("=");
        context.space();
        assignments.get(i).value.render(context);
      }
      if (from != null) {
        context.keyword("FROM");
        from.render(context);
      }
      if (where != null) {
        context.keyword("WHERE");
        where.render(context);
      }
      renderReturning(context, returning);
    }
  }

  static final class DeleteNode implements Node {
    final TableNode<?> table;
    final List<Source> using = new ArrayList<>();
    final List<Node> returning = new ArrayList<>();
    Node where;

    DeleteNode(TableNode<?> table) {
      this.table = table;
    }

    @Override
    public void render(RenderContext context) {
      context.append("DELETE FROM ");
      table.render(context);
      if (!using.isEmpty()) {
        context.keyword("USING");
        for (int i = 0; i < using.size(); i++) {
          if (i > 0) {
            context.comma();
          }
          using.get(i).render(context);
        }
      }
      if (where != null) {
        context.keyword("WHERE");
        where.render(context);
      }
      renderReturning(context, returning);
    }
  }

  static void renderCtes(RenderContext context, List<CteNode> ctes) {
    if (ctes.isEmpty()) {
      return;
    }
    context.append("WITH");
    boolean recursive = false;
    for (CteNode cte : ctes) {
      recursive |= cte.recursive;
    }
    if (recursive) {
      context.space();
      context.append("RECURSIVE");
    }
    context.space();
    for (int i = 0; i < ctes.size(); i++) {
      if (i > 0) {
        context.comma();
      }
      CteNode cte = ctes.get(i);
      context.identifier(cte.name);
      context.space();
      context.append("AS (");
      renderStatement(context, cte.statement);
      context.append(")");
    }
    context.space();
  }

  static void renderJoin(RenderContext context, JoinNode join) {
    context.keyword(join.syntax != null ? join.syntax : joinKeyword(join.type));
    context.space();
    join.source.render(context);
    if (!join.usingColumns.isEmpty()) {
      context.keyword("USING");
      context.append("(");
      renderUnqualifiedColumns(context, join.usingColumns);
      context.append(")");
    } else if (join.condition != null && join.type != JoinType.CROSS && join.type != JoinType.NATURAL) {
      context.keyword("ON");
      join.condition.render(context);
    }
  }

  static String joinKeyword(JoinType type) {
    switch (type) {
      case INNER:
        return "INNER JOIN";
      case LEFT:
        return "LEFT JOIN";
      case RIGHT:
        return "RIGHT JOIN";
      case FULL:
        return "FULL JOIN";
      case CROSS:
        return "CROSS JOIN";
      case NATURAL:
        return "NATURAL JOIN";
      case LATERAL:
        return "JOIN LATERAL";
      default:
        return "JOIN";
    }
  }

  static void renderReturning(RenderContext context, List<Node> returning) {
    if (!returning.isEmpty()) {
      context.keyword("RETURNING");
      renderNodes(context, returning);
    }
  }

  static void renderNodes(RenderContext context, List<? extends Node> nodes) {
    for (int i = 0; i < nodes.size(); i++) {
      if (i > 0) {
        context.comma();
      }
      nodes.get(i).render(context);
    }
  }

  static void renderOrders(RenderContext context, List<Order> orders) {
    for (int i = 0; i < orders.size(); i++) {
      if (i > 0) {
        context.comma();
      }
      ((Node) orders.get(i).expression()).render(context);
      context.space();
      context.append(orders.get(i).ascending() ? "ASC" : "DESC");
    }
  }

  static void renderUnqualifiedColumns(RenderContext context, List<? extends Node> columns) {
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        context.comma();
      }
      Node column = columns.get(i);
      if (column instanceof ColumnNode) {
        context.identifier(((ColumnNode<?>) column).name);
      } else {
        column.render(context);
      }
    }
  }

  static void renderInsertColumns(RenderContext context, List<? extends Node> columns) {
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        context.comma();
      }
      Node column = columns.get(i);
      if (column instanceof ColumnNode) {
        context.identifier(((ColumnNode<?>) column).name);
      } else {
        column.render(context);
      }
    }
  }

  static void renderStatement(RenderContext context, SqlStatement statement) {
    if (statement instanceof StatementBase) {
      ((StatementBase) statement).node.render(context);
    } else {
      context.append(statement.getRenderedSql(context.paramType, context.format, context.dialect).sql());
    }
  }

  static Source tableSource(Table<?> table) {
    return new SourceNode((Node) table, null);
  }

  static Source subquerySource(SqlStatement statement, String alias) {
    return new SourceNode(new SubqueryNode(statement), alias);
  }

  static Node value(Object value) {
    return new ValueNode(value);
  }

  static Node valueOrExpression(Object value) {
    return value instanceof Node ? (Node) value : value(value);
  }

  static SelectNode select(SelectItem[] items) {
    return new SelectNode(items);
  }

  static InsertNode insert(Table<?> table) {
    return new InsertNode((TableNode<?>) table);
  }

  static UpdateNode update(Table<?> table) {
    return new UpdateNode((TableNode<?>) table);
  }

  static DeleteNode delete(Table<?> table) {
    return new DeleteNode((TableNode<?>) table);
  }

  static abstract class BaseExpression<T> implements Expression<T>, Node {
    @Override
    public Condition eq(Object value) {
      return new BinaryCondition((Node) this, "=", valueOrExpression(value));
    }

    @Override
    public Condition ne(Object value) {
      return new BinaryCondition((Node) this, "<>", valueOrExpression(value));
    }

    @Override
    public Condition gt(Object value) {
      return new BinaryCondition((Node) this, ">", valueOrExpression(value));
    }

    @Override
    public Condition ge(Object value) {
      return new BinaryCondition((Node) this, ">=", valueOrExpression(value));
    }

    @Override
    public Condition lt(Object value) {
      return new BinaryCondition((Node) this, "<", valueOrExpression(value));
    }

    @Override
    public Condition le(Object value) {
      return new BinaryCondition((Node) this, "<=", valueOrExpression(value));
    }

    @Override
    public Condition like(Object value) {
      return new BinaryCondition((Node) this, "LIKE", valueOrExpression(value));
    }

    @Override
    public Condition in(Object... values) {
      List<Node> nodes = new ArrayList<>();
      if (values != null) {
        for (Object value : values) {
          nodes.add(valueOrExpression(value));
        }
      }
      return new InCondition((Node) this, nodes);
    }

    @Override
    public Condition in(Collection<?> values) {
      List<Node> nodes = new ArrayList<>();
      if (values != null) {
        for (Object value : values) {
          nodes.add(valueOrExpression(value));
        }
      }
      return new InCondition((Node) this, nodes);
    }

    @Override
    public Condition isNull() {
      return new IsNullCondition((Node) this, false);
    }

    @Override
    public Condition isNotNull() {
      return new IsNullCondition((Node) this, true);
    }
  }

  static final class SubqueryNode extends BaseExpression<Object> implements SelectItem {
    final SqlStatement statement;

    SubqueryNode(SqlStatement statement) {
      this.statement = Objects.requireNonNull(statement, "statement");
    }

    @Override
    public void render(RenderContext context) {
      context.append("(");
      renderStatement(context, statement);
      context.append(")");
    }
  }

  static final class ExistsCondition extends BaseExpression<Boolean> implements Condition {
    final SqlStatement statement;

    ExistsCondition(SqlStatement statement) {
      this.statement = Objects.requireNonNull(statement, "statement");
    }

    @Override
    public void render(RenderContext context) {
      context.append("EXISTS ");
      new SubqueryNode(statement).render(context);
    }
  }

  static String propertyName(SFunction<?, ?> function) {
    try {
      Method writeReplace = function.getClass().getDeclaredMethod("writeReplace");
      writeReplace.setAccessible(true);
      SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(function);
      String method = lambda.getImplMethodName();
      if (method.startsWith("get") && method.length() > 3) {
        return decapitalize(method.substring(3));
      }
      if (method.startsWith("is") && method.length() > 2) {
        return decapitalize(method.substring(2));
      }
      throw new IllegalArgumentException("Only getter method references are supported: " + method);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalArgumentException("Unable to inspect getter method reference", e);
    }
  }

  static String decapitalize(String value) {
    return Character.toLowerCase(value.charAt(0)) + value.substring(1);
  }

  static String resolveColumnName(Class<?> type, String property) {
    try {
      Method getter = type.getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1));
      javax.persistence.Column annotation = getter.getAnnotation(javax.persistence.Column.class);
      if (annotation != null && !annotation.name().isEmpty()) {
        return annotation.name();
      }
    } catch (NoSuchMethodException ignored) {
      // Fall through to field and convention based metadata.
    }
    TableInfo tableInfo = new CompositeTableInfoParser().parse(type);
    for (ColumnInfo column : tableInfo.getColumns()) {
      if (property.equals(column.getFieldName())) {
        return column.getColumnName();
      }
    }
    try {
      Field field = type.getDeclaredField(property);
      javax.persistence.Column annotation = field.getAnnotation(javax.persistence.Column.class);
      return annotation != null && !annotation.name().isEmpty() ? annotation.name() : snake(property);
    } catch (NoSuchFieldException e) {
      try {
        Method getter = type.getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1));
        javax.persistence.Column annotation = getter.getAnnotation(javax.persistence.Column.class);
        return annotation != null && !annotation.name().isEmpty() ? annotation.name() : snake(property);
      } catch (NoSuchMethodException ignored) {
        return snake(property);
      }
    }
  }

  static String resolveTableName(Class<?> type) {
    javax.persistence.Table annotation = type.getAnnotation(javax.persistence.Table.class);
    if (annotation != null && !annotation.name().isEmpty()) {
      return annotation.name();
    }
    return snake(type.getSimpleName());
  }

  static String snake(String value) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isUpperCase(c) && i > 0) {
        result.append('_');
      }
      result.append(Character.toLowerCase(c));
    }
    return result.toString();
  }

  static boolean isActive(Node node) {
    return !(node instanceof WhenNode) || ((WhenNode) node).enabled;
  }

  static boolean hasActive(Node node) {
    if (node instanceof WhenNode) {
      WhenNode when = (WhenNode) node;
      return when.enabled && hasActive(when.node);
    }
    if (node instanceof UnaryCondition) {
      return hasActive(((UnaryCondition) node).expression);
    }
    if (node instanceof CompoundCondition) {
      for (Node condition : ((CompoundCondition) node).conditions) {
        if (hasActive(condition)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  static void collectCompoundConditions(Node node, String operator, List<Node> result) {
    if (node instanceof WhenNode) {
      WhenNode when = (WhenNode) node;
      if (when.enabled) {
        collectCompoundConditions(when.node, operator, result);
      }
      return;
    }
    if (node instanceof CompoundCondition && operator.equals(((CompoundCondition) node).operator)) {
      for (Node condition : ((CompoundCondition) node).conditions) {
        collectCompoundConditions(condition, operator, result);
      }
      return;
    }
    if (hasActive(node)) {
      result.add(node);
    }
  }

  static List<Node> active(List<Node> nodes) {
    List<Node> result = new ArrayList<>();
    for (Node node : nodes) {
      if (isActive(node)) {
        result.add(node);
      }
    }
    return result;
  }
}

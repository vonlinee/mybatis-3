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

public interface SqlDsl {

  static SqlDsl using(Configuration configuration) {
    return new DefaultSqlDsl(configuration);
  }

  static <T> Table<T> table(Class<T> type) {
    return new DslNodes.TableNode<T>(type);
  }

  SelectQuery select(SelectItem... items);

  SelectQuery select();

  SelectQuery select(Table<?> table);

  SelectQuery selectColumns(Table<?> table);

  static <T, R> Column<R> column(Table<T> table, SFunction<T, R> getter) {
    DslNodes.TableNode<T> node = (DslNodes.TableNode<T>) table;
    String property = DslNodes.propertyName(getter);
    return new DslNodes.ColumnNode<R>(node, DslNodes.resolveColumnName(node.javaType(), property));
  }

  static Column<Object> column(String name) {
    return new DslNodes.ColumnNode<Object>(null, name);
  }

  Alias alias(String name);

  Expression<Object> ref(String name);

  Condition when(boolean enabled, Condition condition);

  Expression<Object> when(boolean enabled, Expression<?> expression);

  Expression<Object> literal(Object value);

  Expression<Object> rawExpression(String sql);

  Table<Object> rawTable(String sql);

  Order asc(Expression<?> expression);

  Order desc(Expression<?> expression);

  Expression<Object> count(Expression<?> expression);

  Expression<Object> sum(Expression<?> expression);

  Expression<Object> avg(Expression<?> expression);

  Expression<Object> min(Expression<?> expression);

  Expression<Object> max(Expression<?> expression);

  Expression<Object> coalesce(Object... expressions);

  Expression<Object> function(String name, Expression<?>... expressions);

  CaseBuilder caseWhen();

  Condition and(Condition... conditions);

  Condition or(Condition... conditions);

  Condition exists(SqlStatement statement);

  Expression<Object> scalar(SqlStatement statement);

  InsertQuery insertInto(Table<?> table);

  UpdateQuery update(Table<?> table);

  DeleteQuery deleteFrom(Table<?> table);

  WithQuery with(String name, SqlStatement subquery);

  WithQuery withRecursive(String name, SqlStatement subquery);
}

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

import static org.apache.ibatis.extension.sql.dsl.SqlDsl.column;
import static org.apache.ibatis.extension.sql.dsl.SqlDsl.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class SqlDslAdvancedSyntaxTest {

  private final Configuration configuration = new Configuration();
  private final SqlDsl dsl = SqlDsl.using(configuration);

  private static String sql(String text) {
    return text.replaceAll("\\s+", " ").replaceAll("\\( ", "(").replaceAll(" \\)", ")").trim();
  }

  // @formatter:off
  @Test
  void rendersJoinUsingWithUnqualifiedColumnsAndSubquerySource() {
    Table<User> u = table(User.class).as("u");
    Table<OrderRecord> o = table(OrderRecord.class).as("o");

    assertEquals(
        sql("""
            SELECT u.id
            FROM user_record u
            LEFT JOIN order_record o USING (tenant_id, id)
            INNER JOIN (SELECT o.user_id, o.tenant_id FROM order_record o) recent USING (tenant_id)
            """),
        dsl.select(column(u, User::getId))
            .from(u)
            .leftJoinUsing(
                o,
                column(u, User::getTenantId),
                column(u, User::getId))
            .joinUsing(
                JoinType.INNER,
                dsl.select(
                    column(o, OrderRecord::getUserId),
                    column(o, OrderRecord::getTenantId))
                    .from(o),
                "recent",
                column(u, User::getTenantId))
            .getSql(ParamType.INLINED));
  }

  @Test
  void rendersWindowExpressionAsSelectItem() {
    Table<OrderRecord> o = table(OrderRecord.class).as("o");
    assertEquals(
        sql("""
            SELECT o.user_id,
                   SUM(o.amount) OVER (
                       PARTITION BY o.user_id
                       ORDER BY o.created_at DESC
                       ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                   ) AS running_total
            FROM order_record o
            """),
        dsl.select(
            column(o, OrderRecord::getUserId),
            dsl.alias("running_total").as(
                dsl.sum(column(o, OrderRecord::getAmount))
                    .over()
                    .partitionBy(column(o, OrderRecord::getUserId))
                    .orderBy(dsl.desc(column(o, OrderRecord::getCreatedAt)))
                    .rowsBetween(
                        WindowFrame.UNBOUNDED_PRECEDING,
                        WindowFrame.CURRENT_ROW)
                    .end()))
            .from(o)
            .getSql(ParamType.INLINED));
  }

  @Test
  void rendersDistinctOnAndFetchVariants() {
    Table<User> u = table(User.class).as("u");

    assertEquals(
        sql("""
            SELECT DISTINCT ON (u.tenant_id) u.tenant_id, u.id
            FROM user_record u
            ORDER BY u.tenant_id ASC, u.id DESC
            """),
        dsl.select(
            column(u, User::getTenantId),
            column(u, User::getId))
            .distinctOn(column(u, User::getTenantId))
            .from(u)
            .orderBy(
                dsl.asc(column(u, User::getTenantId)),
                dsl.desc(column(u, User::getId)))
            .getSql(ParamType.INLINED));

    assertEquals(
        sql("""
            SELECT u.id
            FROM user_record u
            ORDER BY u.id ASC
            FETCH FIRST 3 ROWS WITH TIES
            """),
        dsl.select(column(u, User::getId))
            .from(u)
            .orderBy(column(u, User::getId))
            .fetchWithTies(3)
            .getSql(ParamType.INLINED));
  }

  @Test
  void rendersOracleOffsetFetchAndSqlServerTopWithStableParameters() {
    Table<User> u = table(User.class).as("u");

    RenderedSql oracle = dsl.select(column(u, User::getId))
        .from(u)
        .orderBy(column(u, User::getId))
        .offset(10)
        .limit(20)
        .getRenderedSql(ParamType.INDEXED, SqlDialect.ORACLE);
    assertEquals(sql("""
        SELECT "u"."id"
        FROM "user_record" "u"
        ORDER BY "u"."id" ASC
        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
        """), oracle.sql());
    assertEquals(Arrays.asList(10, 20), oracle.parameterValues());

    RenderedSql sqlServer = dsl.select(column(u, User::getId))
        .from(u)
        .limit(5)
        .getRenderedSql(ParamType.INDEXED, SqlDialect.SQL_SERVER);
    assertEquals(sql("""
        SELECT TOP ? [u].[id]
        FROM [user_record] [u]
        """), sqlServer.sql());
    assertEquals(Arrays.asList(5), sqlServer.parameterValues());

    assertEquals(
        sql("""
            SELECT TOP 7 [u].[id]
            FROM [user_record] [u]
            """),
        dsl.select(column(u, User::getId))
            .from(u)
            .top(7)
            .getSql(ParamType.INLINED, SqlDialect.SQL_SERVER));
  }

  @Test
  void rendersMySqlAndCustomIdentifierStrategies() {
    Table<User> u = table(User.class).as("u");

    assertEquals(sql("""
        SELECT `u`.`id`
        FROM `user_record` `u`
        LIMIT 3
        """),
        dsl.select(column(u, User::getId))
            .from(u)
            .limit(3)
            .getSql(ParamType.INLINED, SqlDialect.MYSQL));

    SqlDialect lowerCase = SqlDialect.custom(identifier -> "<" + identifier.toLowerCase() + ">",
        SqlDialect.PaginationStyle.LIMIT_OFFSET);
    assertEquals(sql("""
        SELECT <u>.<id>
        FROM <user_record> <u>
        LIMIT 3
        """),
        dsl.select(column(u, User::getId))
            .from(u)
            .limit(3)
            .getSql(ParamType.INLINED, lowerCase));
  }

  @Test
  void rejectsInvalidAdvancedSyntaxShapes() {
    Table<User> u = table(User.class).as("u");
    Table<OrderRecord> o = table(OrderRecord.class).as("o");

    assertThrows(IllegalArgumentException.class,
        () -> dsl.select()
            .from(u)
            .leftJoinUsing(o)
            .getSql(ParamType.INLINED));
    assertThrows(IllegalArgumentException.class,
        () -> dsl.select()
            .from(u)
            .joinUsing(JoinType.NATURAL, o, column(u, User::getTenantId)));
    assertThrows(
        IllegalArgumentException.class,
        () -> dsl.select()
            .from(u)
            .distinctOn());
    assertThrows(IllegalArgumentException.class, () -> WindowFrame.preceding(-1));
    assertThrows(NullPointerException.class,
        () -> dsl.count(column(u, User::getId)).over().rowsBetween(null, WindowFrame.CURRENT_ROW));
  }
  // @formatter:on

  @Entity
  @javax.persistence.Table(name = "user_record")
  public static class User {
    @Id
    private Integer id;
    private Integer tenantId;
    private String name;

    public Integer getId() {
      return id;
    }

    public Integer getTenantId() {
      return tenantId;
    }

    public String getName() {
      return name;
    }
  }

  @Entity
  @javax.persistence.Table(name = "order_record")
  public static class OrderRecord {
    @Id
    private Integer id;
    private Integer tenantId;
    private Integer userId;
    private Integer amount;
    private String createdAt;

    public Integer getId() {
      return id;
    }

    public Integer getTenantId() {
      return tenantId;
    }

    public Integer getUserId() {
      return userId;
    }

    public Integer getAmount() {
      return amount;
    }

    public String getCreatedAt() {
      return createdAt;
    }
  }
}

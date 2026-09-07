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

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class SqlDslWithSyntaxTest {

  private final Configuration configuration = new Configuration();
  private final SqlDsl dsl = SqlDsl.using(configuration);

  private static String sql(String text) {
    return text.replaceAll("\\s+", " ").replaceAll("\\( ", "(").replaceAll(" \\)", ")").trim();
  }

  // @formatter:off
  @Test
  void rendersWithBeforeSelectInDslOrder() {
    Table<OrderRecord> o = table(OrderRecord.class).as("o");
    Table<UserRecord> u = table(UserRecord.class).as("u");

    assertEquals(
        sql("""
            WITH recent_orders AS (
                SELECT o.user_id, o.amount
                FROM order_record o
                WHERE o.status = 'PAID'
            )
            SELECT u.id
            FROM user_record u
            WHERE u.id IN (SELECT recent_orders.user_id FROM recent_orders)
            """),
        dsl.with(
            "recent_orders",
            dsl.select(
                column(o, OrderRecord::getUserId),
                column(o, OrderRecord::getAmount))
                .from(o)
                .where(column(o, OrderRecord::getStatus).eq("PAID")))
            .select(column(u, UserRecord::getId))
            .from(u)
            .where(column(u, UserRecord::getId).in(
                dsl.rawExpression("SELECT recent_orders.user_id FROM recent_orders")))
            .getSql(ParamType.INLINED));
  }

  @Test
  void rendersMultipleCtesBeforeSelect() {
    Table<OrderRecord> o = table(OrderRecord.class).as("o");
    Table<UserRecord> u = table(UserRecord.class).as("u");

    assertEquals(
        sql("""
            WITH paid_orders AS (
                SELECT o.user_id
                FROM order_record o
                WHERE o.status = 'PAID'
            ),
            active_users AS (
                SELECT u.id
                FROM user_record u
                WHERE u.enabled = TRUE
            )
            SELECT u.id
            FROM user_record u
            """),
        dsl.with(
            "paid_orders",
            dsl.select(column(o, OrderRecord::getUserId))
                .from(o)
                .where(column(o, OrderRecord::getStatus).eq("PAID")))
            .with(
                "active_users",
                dsl.select(column(u, UserRecord::getId))
                    .from(u)
                    .where(column(u, UserRecord::getEnabled).eq(true)))
            .select(column(u, UserRecord::getId))
            .from(u)
            .getSql(ParamType.INLINED));
  }

  @Test
  void rendersRecursiveWithBeforeSelect() {
    Table<NumberRecord> n = table(NumberRecord.class).as("n");

    assertEquals(
        sql("""
            WITH RECURSIVE numbers AS (
                SELECT n.value
                FROM number_record n
                WHERE n.value > 0
            )
            SELECT n.value
            FROM number_record n
            """),
        dsl.withRecursive(
            "numbers",
            dsl.select(column(n, NumberRecord::getValue))
                .from(n)
                .where(column(n, NumberRecord::getValue).gt(0)))
            .select(column(n, NumberRecord::getValue))
            .from(n)
            .getSql(ParamType.INLINED));
  }

  @Test
  void rendersCteParametersBeforeOuterQueryParameters() {
    Table<OrderRecord> o = table(OrderRecord.class).as("o");
    Table<UserRecord> u = table(UserRecord.class).as("u");

    RenderedSql rendered = dsl.with(
        "recent_orders",
        dsl.select(column(o, OrderRecord::getUserId))
            .from(o)
            .where(column(o, OrderRecord::getStatus).eq("PAID")))
        .select(column(u, UserRecord::getId))
        .from(u)
        .where(column(u, UserRecord::getTenantId).eq(7))
        .getRenderedSql(ParamType.INDEXED);

    assertEquals(
        sql("""
            WITH recent_orders AS (
                SELECT o.user_id
                FROM order_record o
                WHERE o.status = ?
            )
            SELECT u.id
            FROM user_record u
            WHERE u.tenant_id = ?
            """),
        rendered.sql());
    assertEquals(java.util.Arrays.asList("PAID", 7), rendered.parameterValues());
  }
  // @formatter:on

  @Entity
  @javax.persistence.Table(name = "order_record")
  public static class OrderRecord {
    @Id
    private Integer id;
    private Integer userId;
    private Integer amount;
    private Integer tenantId;
    private String status;

    public Integer getId() {
      return id;
    }

    public Integer getUserId() {
      return userId;
    }

    public Integer getAmount() {
      return amount;
    }

    public Integer getTenantId() {
      return tenantId;
    }

    public String getStatus() {
      return status;
    }
  }

  @Entity
  @javax.persistence.Table(name = "user_record")
  public static class UserRecord {
    @Id
    private Integer id;
    private Integer tenantId;
    private boolean enabled;

    public Integer getId() {
      return id;
    }

    public Integer getTenantId() {
      return tenantId;
    }

    public boolean getEnabled() {
      return enabled;
    }
  }

  @Entity
  @javax.persistence.Table(name = "number_record")
  public static class NumberRecord {
    @Id
    private Integer id;
    private Integer value;

    public Integer getValue() {
      return value;
    }
  }
}

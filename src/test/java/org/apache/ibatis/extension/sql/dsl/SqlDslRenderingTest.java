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

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class SqlDslRenderingTest {

  private final Configuration configuration = new Configuration();
  private final SqlDsl dsl = SqlDsl.using(configuration);

  private static String sql(String text) {
    return text.replaceAll("\\s+", " ").replaceAll("\\( ", "(").replaceAll(" \\)", ")").trim();
  }

  // @formatter:off
  @Test
  void rendersComplexSelectWithCteJoinsNestedConditionsCaseAndWindowLikeExpressions() {
    Table<UserRecord> u = table(UserRecord.class).as("u");
    Table<OrderRecord> o = table(OrderRecord.class).as("o");
    Table<CustomerRecord> c = table(CustomerRecord.class).as("c");

    assertEquals(
        sql("""
            WITH recent_orders AS (
                SELECT o.user_id, SUM(o.amount)
                FROM order_record o
                WHERE o.created_at >= '2026-01-01'
            )
            SELECT u.id, u.name,
                   CASE
                       WHEN o.amount > 1000 THEN 'HIGH'
                       WHEN o.vip = TRUE THEN 'VIP'
                       ELSE 'NORMAL'
                   END AS customer_type,
                   COUNT(o.id) AS order_count,
                   (SELECT SUM(o.amount)
                    FROM order_record o
                    WHERE o.user_id = u.id)
            FROM user_record u
            LEFT JOIN order_record o ON o.user_id = u.id
            NATURAL JOIN customer_record c
            JOIN LATERAL (SELECT o.id FROM order_record o) lo
            WHERE ((u.tenant_id = 7)
              AND ((u.name LIKE '%ann%') OR (u.status IN ('ACTIVE', 'PENDING')))
              AND (EXISTS (SELECT o.id
                           FROM order_record o
                           WHERE o.user_id = u.id)))
            GROUP BY u.id, u.name
            HAVING COUNT(o.id) > 0
            ORDER BY u.name DESC, u.id ASC
            LIMIT 20 OFFSET 40
            FOR UPDATE SKIP LOCKED
            """),
        dsl.with(
            "recent_orders",
            dsl.select(
                column(o, OrderRecord::getUserId),
                dsl.sum(column(o, OrderRecord::getAmount)))
                .from(o)
                .where(column(o, OrderRecord::getCreatedAt).ge("2026-01-01")))
            .select(
            column(u, UserRecord::getId),
            column(u, UserRecord::getName),
            dsl.alias("customer_type").as(
                dsl.caseWhen()
                    .when(column(o, OrderRecord::getAmount).gt(1000))
                    .then("HIGH")
                    .when(column(o, OrderRecord::getVip).eq(true))
                    .then("VIP")
                    .otherwise("NORMAL")
                    .end()),
            dsl.alias("order_count").as(dsl.count(column(o, OrderRecord::getId))),
            dsl.scalar(
                dsl.select(dsl.sum(column(o, OrderRecord::getAmount)))
                    .from(o)
                    .where(column(o, OrderRecord::getUserId).eq(column(u, UserRecord::getId)))))
            .from(u)
            .leftJoin(
                o,
                column(o, OrderRecord::getUserId).eq(column(u, UserRecord::getId)))
            .naturalJoin(c)
            .lateralJoin(
                dsl.select(column(o, OrderRecord::getId)).from(o),
                "lo",
                null)
            .where(
                column(u, UserRecord::getTenantId).eq(7)
                    .and(
                        column(u, UserRecord::getName).like("%ann%")
                            .or(column(u, UserRecord::getStatus).in(Arrays.asList("ACTIVE", "PENDING"))))
                    .and(
                        dsl.exists(
                            dsl.select(column(o, OrderRecord::getId))
                                .from(o)
                                .where(column(o, OrderRecord::getUserId).eq(column(u, UserRecord::getId))))))
            .groupBy(
                column(u, UserRecord::getId),
                column(u, UserRecord::getName))
            .having(dsl.count(column(o, OrderRecord::getId)).gt(0))
            .orderBy(
                dsl.desc(column(u, UserRecord::getName)),
                dsl.asc(column(u, UserRecord::getId)))
            .limit(20)
            .offset(40)
            .forUpdate()
            .skipLocked()
            .getSql(ParamType.INLINED, SqlFormat.COMPACT));
  }

  @Test
  void rendersIndexedNamedAndInlineParametersInStableOrder() {
    Table<UserRecord> u = table(UserRecord.class).as("u");
    RenderedSql indexed = dsl.select(column(u, UserRecord::getId))
        .from(u)
        .where(
            column(u, UserRecord::getTenantId).eq(8)
                .and(column(u, UserRecord::getName).like("O'Reilly"))
                .and(column(u, UserRecord::getId).in(1, 2, 3)))
        .limit(10)
        .getRenderedSql(ParamType.INDEXED);
    assertEquals(sql("""
        SELECT u.id
        FROM user_record u
        WHERE ((u.tenant_id = ?)
          AND (u.name LIKE ?)
          AND (u.id IN (?, ?, ?)))
        LIMIT ?
        """), indexed.sql());
    assertEquals(Arrays.asList(8, "O'Reilly", 1, 2, 3, 10), indexed.parameterValues());
    assertEquals(Arrays.asList("__dsl_0", "__dsl_1", "__dsl_2", "__dsl_3", "__dsl_4", "__dsl_5"),
        indexed.parameterMappings().stream().map(mapping -> mapping.getProperty()).collect(Collectors.toList()));

    RenderedSql named = dsl.select(column(u, UserRecord::getId))
        .from(u)
        .where(
            column(u, UserRecord::getTenantId).eq(8)
                .and(column(u, UserRecord::getName).like("O'Reilly"))
                .and(column(u, UserRecord::getId).in(1, 2, 3)))
        .limit(10)
        .getRenderedSql(ParamType.NAMED);
    assertEquals(
        sql("""
            SELECT u.id
            FROM user_record u
            WHERE ((u.tenant_id = #{__dsl_0})
              AND (u.name LIKE #{__dsl_1})
              AND (u.id IN (#{__dsl_2}, #{__dsl_3}, #{__dsl_4})))
            LIMIT #{__dsl_5}
            """),
        named.sql());

    RenderedSql inline = dsl.select(column(u, UserRecord::getId))
        .from(u)
        .where(
            column(u, UserRecord::getTenantId).eq(8)
                .and(column(u, UserRecord::getName).like("O'Reilly"))
                .and(column(u, UserRecord::getId).in(1, 2, 3)))
        .limit(10)
        .getRenderedSql(ParamType.INLINED);
    assertEquals(sql("""
        SELECT u.id
        FROM user_record u
        WHERE ((u.tenant_id = 8)
          AND (u.name LIKE 'O''Reilly')
          AND (u.id IN (1, 2, 3)))
        LIMIT 10
        """), inline.sql());
  }

  @Test
  void rendersDmlSetBasedSubqueryAndReturning() {
    Table<UserRecord> u = table(UserRecord.class).as("u");

    assertEquals(
        sql("""
            INSERT INTO user_record u
                (id, tenant_id, name)
            VALUES (?, ?, ?), (?, ?, ?)
            RETURNING u.id
            """),
        dsl.insertInto(u)
            .columns(
                column(u, UserRecord::getId),
                column(u, UserRecord::getTenantId),
                column(u, UserRecord::getName))
            .values(1, 9, "A")
            .values(2, 9, "B")
            .returning(column(u, UserRecord::getId))
            .getSql(ParamType.INDEXED));

    assertEquals(
        sql("""
            UPDATE user_record u
            SET u.name = ?, u.status = ?
            FROM customer_record c
            WHERE u.id = c.id
            RETURNING u.id
            """),
        dsl.update(u)
            .set(column(u, UserRecord::getName), "new")
            .set(column(u, UserRecord::getStatus), "ACTIVE")
            .from(table(CustomerRecord.class).as("c"))
            .where(column(u, UserRecord::getId).eq(dsl.rawExpression("c.id")))
            .returning(column(u, UserRecord::getId))
            .getSql(ParamType.INDEXED));

    assertEquals(
        sql("""
            DELETE FROM user_record u
            USING customer_record c
            WHERE u.id = c.id
            RETURNING u.id
            """),
        dsl.deleteFrom(u)
            .using(table(CustomerRecord.class).as("c"))
            .where(column(u, UserRecord::getId).eq(dsl.rawExpression("c.id")))
            .returning(column(u, UserRecord::getId))
            .getSql(ParamType.INDEXED));
  }

  @Test
  void rendersDefaultValuesSelectColumnsAndRawDialectJoin() {
    Table<UserRecord> u = table(UserRecord.class).as("u");

    assertEquals(sql("""
        INSERT INTO user_record u
        DEFAULT VALUES
        """),
        dsl.insertInto(u)
            .defaultValues()
            .getSql(ParamType.INLINED));
    assertEquals(
        sql("""
            SELECT u.id, u.tenant_id, u.name, u.status, u.vip, u.created_at
            FROM user_record u
            """),
        dsl.selectColumns(u)
            .from(u)
            .getSql(ParamType.INLINED));
    assertEquals(sql("""
        SELECT *
        FROM user_record u
        HASH JOIN customer_record c
        ON u.id = c.id
        """),
        dsl.select()
            .from(u)
            .dialectJoin(
                "HASH JOIN",
                table(CustomerRecord.class).as("c"),
                column(u, UserRecord::getId).eq(dsl.rawExpression("c.id")))
            .getSql(ParamType.INLINED));
  }
  // @formatter:on

  @Entity
  @javax.persistence.Table(name = "user_record")
  public static class UserRecord {
    @Id
    private Integer id;
    private Integer tenantId;
    private String name;
    private String status;
    private Boolean vip;
    private String createdAt;

    public Integer getId() {
      return id;
    }

    public Integer getTenantId() {
      return tenantId;
    }

    public String getName() {
      return name;
    }

    public String getStatus() {
      return status;
    }

    public Boolean getVip() {
      return vip;
    }

    public String getCreatedAt() {
      return createdAt;
    }
  }

  @Entity
  @javax.persistence.Table(name = "order_record")
  public static class OrderRecord {
    @Id
    private Integer id;
    private Integer userId;
    private Integer amount;
    private Boolean vip;
    private String createdAt;

    public Integer getId() {
      return id;
    }

    public Integer getUserId() {
      return userId;
    }

    public Integer getAmount() {
      return amount;
    }

    public Boolean getVip() {
      return vip;
    }

    public String getCreatedAt() {
      return createdAt;
    }
  }

  @Entity
  @javax.persistence.Table(name = "customer_record")
  public static class CustomerRecord {
    @Id
    private Integer id;

    public Integer getId() {
      return id;
    }
  }
}

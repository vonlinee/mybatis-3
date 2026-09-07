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

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class SqlDslObjectApiTest {

  private final Configuration configuration = new Configuration();
  private final SqlDsl dsl = new DefaultSqlDsl(configuration);

  private static String sql(String text) {
    return text.replaceAll("\\s+", " ").replaceAll("\\( ", "(").replaceAll(" \\)", ")").trim();
  }

  // @formatter:off
  @Test
  void getSqlReturnsSqlDirectlyFromInterfaceBasedDsl() {
    Table<User> u = table(User.class).as("u");

    assertEquals(
        sql("""
            SELECT u.id
            FROM user_record u
            WHERE u.tenant_id = 7
            """),
        dsl.select(column(u, User::getId))
            .from(u)
            .where(column(u, User::getTenantId).eq(7))
            .getSql(ParamType.INLINED));
  }

  @Test
  void getRenderedSqlExposesParameterMetadataSeparately() {
    Table<User> u = table(User.class).as("u");

    RenderedSql rendered = dsl.select(column(u, User::getId))
        .from(u)
        .where(column(u, User::getTenantId).eq(7))
        .getRenderedSql(ParamType.INDEXED);

    assertEquals(
        sql("""
            SELECT u.id
            FROM user_record u
            WHERE u.tenant_id = ?
            """),
        rendered.sql());
    assertEquals(Arrays.asList(7), rendered.parameterValues());
  }
  // @formatter:on

  @Entity
  @javax.persistence.Table(name = "user_record")
  public static class User {
    @Id
    private Integer id;
    private Integer tenantId;

    public Integer getId() {
      return id;
    }

    public Integer getTenantId() {
      return tenantId;
    }
  }
}

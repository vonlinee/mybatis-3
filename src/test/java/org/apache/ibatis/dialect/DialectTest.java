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
package org.apache.ibatis.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class DialectTest {

  @Test
  void shouldGetDbTypeFromConnectionUrl() {
    assertEquals(DbType.MYSQL, DbType.fromConnectionUrl("jdbc:mysql://localhost:3306/test"));
    assertEquals(DbType.MARIADB, DbType.fromConnectionUrl("jdbc:mariadb://localhost:3306/test"));
    assertEquals(DbType.POSTGRESQL, DbType.fromConnectionUrl("jdbc:postgresql://localhost:5432/test"));
    assertEquals(DbType.ORACLE, DbType.fromConnectionUrl("jdbc:oracle:thin:@localhost:1521:orcl"));
    assertEquals(DbType.SQL_SERVER, DbType.fromConnectionUrl("jdbc:sqlserver://localhost;databaseName=test"));
    assertEquals(DbType.H2, DbType.fromConnectionUrl("jdbc:h2:mem:test"));
    assertEquals(DbType.HSQLDB, DbType.fromConnectionUrl("jdbc:hsqldb:mem:test"));
    assertEquals(DbType.SQLITE, DbType.fromConnectionUrl("jdbc:sqlite:test.db"));
    assertEquals(DbType.GAUSSDB, DbType.fromConnectionUrl("jdbc:opengauss://localhost:8000/postgres"));
    assertEquals(DbType.DB2, DbType.fromConnectionUrl("jdbc:db2://localhost:50000/TEST"));
    assertEquals(DbType.DERBY, DbType.fromConnectionUrl("jdbc:derby:testdb;create=true"));
    assertEquals(DbType.OTHER, DbType.fromConnectionUrl("jdbc:unknown://localhost:8000/postgres"));
  }

  @Test
  void testFromConnectionUrlByUrl() {
    assertInstanceOf(MySqlDialect.class, Dialect.fromConnectionUrl("jdbc:mysql://localhost:3306/test"));
    assertInstanceOf(PostgresqlDialect.class, Dialect.fromConnectionUrl("jdbc:postgresql://localhost:5432/test"));
    assertInstanceOf(GaussDBDialect.class, Dialect.fromConnectionUrl("jdbc:opengauss://localhost:8000/postgres"));
    assertInstanceOf(OracleDialect.class, Dialect.fromConnectionUrl("jdbc:oracle:thin:@localhost:1521:orcl"));
    assertInstanceOf(AnsiDialect.class, Dialect.fromConnectionUrl("jdbc:sqlserver://localhost;"));
    assertInstanceOf(AnsiDialect.class, Dialect.fromConnectionUrl("jdbc:derby:testdb"));
    assertInstanceOf(MariaDbDialect.class, Dialect.fromConnectionUrl("jdbc:mariadb://localhost:3306/test"));
    assertInstanceOf(Db2Dialect.class, Dialect.fromConnectionUrl("jdbc:db2://localhost:50000/TEST"));
    assertInstanceOf(HsqldbDialect.class, Dialect.fromConnectionUrl("jdbc:hsqldb:mem:test"));
    assertInstanceOf(SqliteDialect.class, Dialect.fromConnectionUrl("jdbc:sqlite:test.db"));
  }

}

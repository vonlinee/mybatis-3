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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Representation of a SQL dialect.
 */
public interface Dialect {

  Dialect ANSI = new AnsiDialect();
  Dialect MYSQL = new MySqlDialect();
  Dialect ORACLE = new OracleDialect();
  Dialect SQL_SERVER = new SqlServerDialect();
  Dialect DB2 = new Db2Dialect();
  Dialect H2 = new H2Dialect();
  Dialect HSQLDB = new HsqldbDialect();
  Dialect MARIADB = new MariaDbDialect();
  Dialect POSTGRESQL = new PostgresqlDialect();
  Dialect SQLITE = new SqliteDialect();
  Dialect DERBY = new DerbyDialect();
  Dialect GAUSSDB = new GaussDBDialect();

  /**
   * Returns the {@link LimitClause} used by this dialect.
   *
   * @return the limit clause
   */
  LimitClause limit();

  static Dialect fromDataSource(DataSource dataSource) {
    if (dataSource == null) {
      return Dialect.ANSI;
    }
    try (Connection connection = dataSource.getConnection()) {
      return fromConnection(connection);
    } catch (SQLException e) {
      return Dialect.ANSI;
    }
  }

  static Dialect fromConnection(Connection connection) throws SQLException {
    String url = connection.getMetaData().getURL();
    return Dialect.fromConnectionUrl(url);
  }

  static Dialect fromConnectionUrl(String jdbcUrl) {
    DbType dbType = DbType.fromConnectionUrl(jdbcUrl);
    switch (dbType) {
      case MYSQL:
        return Dialect.MYSQL;
      case MARIADB:
        return Dialect.MARIADB;
      case POSTGRESQL:
        return Dialect.POSTGRESQL;
      case H2:
        return Dialect.H2;
      case GAUSSDB:
        return Dialect.GAUSSDB;
      case HSQLDB:
        return Dialect.HSQLDB;
      case SQLITE:
        return Dialect.SQLITE;
      case ORACLE:
        return Dialect.ORACLE;
      case SQL_SERVER:
        return Dialect.SQL_SERVER;
      case DERBY:
        return Dialect.DERBY;
      case DB2:
        return Dialect.DB2;
      default:
        return Dialect.ANSI;
    }
  }
}

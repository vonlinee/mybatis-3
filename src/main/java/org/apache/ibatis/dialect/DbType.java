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

/**
 * Supported database platforms.
 */
public enum DbType {
  MYSQL, MARIADB, ORACLE, DB2, H2, HSQLDB, SQLITE, POSTGRESQL, GAUSSDB, SQL_SERVER, DERBY, OTHER, UNKNOWN;

  public static DbType fromConnectionUrl(String jdbcUrl) {
    if (jdbcUrl == null) {
      return DbType.OTHER;
    }
    final String url = jdbcUrl.toLowerCase();
    if (url.contains(":mysql:")) {
      return DbType.MYSQL;
    }
    if (url.contains(":mariadb:")) {
      return DbType.MARIADB;
    }
    if (url.contains(":oracle:")) {
      return DbType.ORACLE;
    }
    if (url.contains(":postgresql:")) {
      return DbType.POSTGRESQL;
    }
    if (url.contains(":sqlserver:")) {
      return DbType.SQL_SERVER;
    }
    if (url.contains(":h2:")) {
      return DbType.H2;
    }
    if (url.contains(":hsqldb:")) {
      return DbType.HSQLDB;
    }
    if (url.contains(":sqlite:")) {
      return DbType.SQLITE;
    }
    if (url.contains(":gaussdb:") || url.contains(":opengauss:")) {
      return DbType.GAUSSDB;
    }
    if (url.contains(":db2:")) {
      return DbType.DB2;
    }
    if (url.contains(":derby:")) {
      return DbType.DERBY;
    }
    return DbType.OTHER;
  }
}

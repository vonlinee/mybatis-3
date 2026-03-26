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
package org.apache.ibatis.internal.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import org.apache.ibatis.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/**
 * @author vonline
 */
public final class JdbcUtils {

  public static final int NO_ROW_OFFSET = 0;
  public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;

  private JdbcUtils() {
    // Prevent Instantiation
  }

  @Nullable
  public static ResultSet getFirstResultSet(Statement stmt) throws SQLException {
    Objects.requireNonNull(stmt, "statement is null");
    ResultSet rs = null;
    SQLException e1 = null;

    try {
      rs = stmt.getResultSet();
    } catch (SQLException e) {
      // Oracle throws ORA-17283 for implicit cursor
      e1 = e;
    }

    try {
      while (rs == null) {
        // move forward to get the first resultSet in case the driver
        // doesn't return the resultSet as the first result (HSQLDB)
        if (stmt.getMoreResults()) {
          rs = stmt.getResultSet();
        } else if (stmt.getUpdateCount() == -1) {
          // no more results. Must be no resultSet
          break;
        }
      }
    } catch (SQLException e) {
      throw e1 != null ? e1 : e;
    }

    return rs;
  }

  public static ResultSet getNextResultSet(Statement stmt) {
    Objects.requireNonNull(stmt, "statement is null");
    // Making this method tolerant of bad JDBC drivers
    try {
      // We stopped checking DatabaseMetaData#supportsMultipleResultSets()
      // because Oracle driver (incorrectly) returns false

      // Crazy Standard JDBC way of determining if there are more results
      // DO NOT try to 'improve' the condition even if IDE tells you to!
      // It's important that getUpdateCount() is called here.
      if (!(!stmt.getMoreResults() && stmt.getUpdateCount() == -1)) {
        ResultSet rs = stmt.getResultSet();
        if (rs == null) {
          return getNextResultSet(stmt);
        } else {
          return rs;
        }
      }
    } catch (Exception e) {
      // Intentionally ignored.
    }
    return null;
  }

  public static void closeQuietly(Statement statement) {
    IOUtils.closeQuietly(statement);
  }

  public static void closeQuietly(Connection connection) {
    IOUtils.closeQuietly(connection);
  }

  public static void closeQuietly(ResultSet rs) {
    IOUtils.closeQuietly(rs);
  }

  public static void closeQuietly(Transaction tx) {
    if (tx != null) {
      try {
        tx.close();
      } catch (SQLException ignore) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  public static void absolute(ResultSet rs, int offset) throws SQLException {
    Objects.requireNonNull(rs, "resultSet is null");
    if (offset < NO_ROW_OFFSET) {
      return;
    }
    if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
      if (offset != NO_ROW_OFFSET) {
        rs.absolute(offset);
      }
    } else {
      for (int i = 0; i < offset; i++) {
        if (!rs.next()) {
          break;
        }
      }
    }
  }
}

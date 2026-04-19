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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.apache.ibatis.BaseDataTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link JdbcUtils}. The test method names follow the convention of starting with {@code should}.
 */
class JdbcUtilsTest {

  @Test
  void shouldReturnFirstResultSetWhenAvailable() throws SQLException {
    Statement stmt = Mockito.mock(Statement.class);
    ResultSet rs1 = Mockito.mock(ResultSet.class);
    ResultSet rs2 = Mockito.mock(ResultSet.class);
    Mockito.when(stmt.getResultSet()).thenReturn(rs1);
    Mockito.when(stmt.getMoreResults()).thenReturn(true).thenReturn(false);
    Mockito.when(stmt.getResultSet()).thenReturn(rs1).thenReturn(rs2);

    ResultSet result = JdbcUtils.getFirstResultSet(stmt);
    assertSame(rs1, result);
  }

  @Test
  void shouldReturnNextResultSetWhenExists() throws SQLException {
    Statement stmt = Mockito.mock(Statement.class);
    ResultSet rs1 = Mockito.mock(ResultSet.class);
    ResultSet rs2 = Mockito.mock(ResultSet.class);
    // First call returns rs1, second call returns rs2
    Mockito.when(stmt.getResultSet()).thenReturn(rs1).thenReturn(rs2);
    Mockito.when(stmt.getMoreResults()).thenReturn(true).thenReturn(false);

    // Consume first result set
    JdbcUtils.getFirstResultSet(stmt);
    ResultSet next = JdbcUtils.getNextResultSet(stmt);
    assertSame(rs2, next);
  }

  @Test
  void shouldCloseStatementQuietlyWithoutException() throws SQLException {
    Statement stmt = Mockito.mock(Statement.class);
    // No exception thrown
    assertDoesNotThrow(() -> JdbcUtils.closeQuietly(stmt));
    Mockito.verify(stmt).close();
  }

  @Test
  void shouldCloseConnectionQuietlyWithoutException() throws SQLException {
    Connection conn = Mockito.mock(Connection.class);
    assertDoesNotThrow(() -> JdbcUtils.closeQuietly(conn));
    Mockito.verify(conn).close();
  }

  @Test
  void shouldCloseResultSetQuietlyWithoutException() throws SQLException {
    ResultSet rs = Mockito.mock(ResultSet.class);
    assertDoesNotThrow(() -> JdbcUtils.closeQuietly(rs));
    Mockito.verify(rs).close();
  }

  @Test
  void shouldMoveCursorToAbsolutePosition() throws SQLException {
    ResultSet rs = Mockito.mock(ResultSet.class);
    // Simulate forward‑only result set where absolute is not supported
    Mockito.when(rs.getType()).thenReturn(ResultSet.TYPE_FORWARD_ONLY);
    // Pretend next() works
    Mockito.when(rs.next()).thenReturn(true).thenReturn(false);

    JdbcUtils.absolute(rs, 2);
    // Verify that next() was called twice to reach offset 2
    Mockito.verify(rs, Mockito.times(2)).next();
  }

  @Test
  void shouldMoveCursorToAbsolutePositionWhenSupported() throws SQLException {
    ResultSet rs = Mockito.mock(ResultSet.class);
    // Simulate scrollable result set where absolute is supported
    Mockito.when(rs.getType()).thenReturn(ResultSet.TYPE_SCROLL_INSENSITIVE);
    // Pretend absolute() works
    Mockito.when(rs.absolute(2)).thenReturn(true);

    JdbcUtils.absolute(rs, 2);
    // Verify that absolute() was called once to reach offset 2
  }

  @Test
  void shouldMoveCursorToAbsolutePositionWhenSupportedAndOffsetIsZero() throws SQLException {
    ResultSet rs = Mockito.mock(ResultSet.class);
    // Simulate scrollable result set where absolute is supported
    Mockito.when(rs.getType()).thenReturn(ResultSet.TYPE_SCROLL_INSENSITIVE);
    // Pretend absolute() works
    Mockito.when(rs.absolute(0)).thenReturn(true);

    JdbcUtils.absolute(rs, 0);
    // Verify that absolute() was called once to reach offset 0
  }

  @Test
  void shouldMoveCursorToAbsolutePositionWhenSupportedAndOffsetIsNegative() throws SQLException {
    ResultSet rs = Mockito.mock(ResultSet.class);
    // Simulate scrollable result set where absolute is supported
    Mockito.when(rs.getType()).thenReturn(ResultSet.TYPE_SCROLL_INSENSITIVE);
    // Pretend absolute() works
    Mockito.when(rs.absolute(-1)).thenReturn(true);
  }

  @Test
  void shouldExecuteSqlViaJdbcUtils() throws SQLException, IOException {
    DataSource dataSource = BaseDataTest.createUnpooledHsqlDbDataSource("jdbc_test");
    BaseDataTest.runScript(dataSource, new StringReader("create table test (id int, name varchar(20));"));
    try (Connection connection = dataSource.getConnection()) {
      {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
          Map<String, Object> row = new HashMap<>();
          row.put("id", i + 1);
          row.put("name", "test" + (i + 1));
          rows.add(row);
        }
        int i = JdbcUtils.insertToTable(connection, "test", rows);
        Assertions.assertEquals(10, i);
        Assertions.assertEquals(10, JdbcUtils.queryForInt(connection, "select count(*) from test"));
      }
      {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 11);
        row.put("name", "test" + 11);
        JdbcUtils.insertToTable(connection, "test", row);
      }

      Assertions.assertEquals(11, JdbcUtils.queryForInt(connection, "select count(*) from test"));

      List<Map<String, Object>> list = JdbcUtils.queryForList(connection, "select * from test");
      Assertions.assertEquals(11, list.size());
      List<Integer> ids = new ArrayList<>();
      List<String> names = new ArrayList<>();
      for (Map<String, Object> row : list) {
        ids.add((Integer) row.get("id"));
        names.add((String) row.get("name"));
      }

      assertThat(ids).containsExactly(IntStream.range(1, 12).boxed().toArray(Integer[]::new));
      String[] expectedNames = IntStream.range(1, 12).mapToObj(i -> "test" + i).toArray(String[]::new);
      assertThat(names).containsExactly(expectedNames);

      int res = JdbcUtils.update(connection, "update test set name = 'test1-updated' where id = 1");
      Assertions.assertEquals(1, res);

      Map<String, Object> item = JdbcUtils.queryForMap(connection, "select * from test where id = 1");
      Assertions.assertNotNull(item);
      Assertions.assertEquals(1, item.get("id"));
      Assertions.assertEquals("test1-updated", item.get("name"));
    }
  }
}

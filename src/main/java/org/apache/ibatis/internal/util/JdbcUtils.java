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

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.sql.DataSource;

import org.apache.ibatis.executor.result.BeanClassRowMapper;
import org.apache.ibatis.executor.result.MapRowMapper;
import org.apache.ibatis.executor.result.RowMapper;
import org.apache.ibatis.executor.result.SingleColumnRowMapper;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.NamingStrategy;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.SimpleTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vonline
 */
public final class JdbcUtils {

  public static final int NO_ROW_OFFSET = 0;
  public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;

  private static final int[] EMPTY_BATCH_RESULT = new int[0];

  static final Log log = LogFactory.getLog(JdbcUtils.class);

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

  public static boolean isAutoCommit(Connection connection) {
    boolean autoCommit;
    try {
      autoCommit = connection.getAutoCommit();
    } catch (SQLException e) {
      // Failover to true, as most poor drivers
      // or databases won't support transactions
      autoCommit = true;
    }
    return autoCommit;
  }

  /**
   * Check whether the given SQL type is numeric.
   *
   * @param sqlType
   *          the SQL type to be checked
   *
   * @return whether the type is numeric
   */
  public static boolean isNumeric(int sqlType) {
    return (Types.BIT == sqlType || Types.BIGINT == sqlType || Types.DECIMAL == sqlType || Types.DOUBLE == sqlType
        || Types.FLOAT == sqlType || Types.INTEGER == sqlType || Types.NUMERIC == sqlType || Types.REAL == sqlType
        || Types.SMALLINT == sqlType || Types.TINYINT == sqlType);
  }

  /**
   * Determine the column name to use. The column name is determined based on a lookup using ResultSetMetaData.
   * <p>
   * This method's implementation takes into account clarifications expressed in the JDBC 4.0 specification:
   * <p>
   * <i>columnLabel - the label for the column specified with the SQL AS clause. If the SQL AS clause was not specified,
   * then the label is the name of the column</i>.
   *
   * @param resultSetMetaData
   *          the current meta-data to use
   * @param columnIndex
   *          the index of the column for the lookup
   *
   * @return the column name to use
   *
   * @throws SQLException
   *           in case of lookup failure
   */
  public static String findColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
    String name = resultSetMetaData.getColumnLabel(columnIndex);
    if (StringUtils.isNotEmpty(name)) {
      name = resultSetMetaData.getColumnName(columnIndex);
    }
    return name;
  }

  /**
   * Retrieve a JDBC column value from a ResultSet, using the most appropriate value type. The returned value should be
   * a detached value object, not having any ties to the active ResultSet: in particular, it should not be a Blob or
   * Clob object but rather a byte array or String representation, respectively.
   * <p>
   * Uses the {@code getObject(index)} method, but includes additional "hacks" to get around Oracle 10g returning a
   * non-standard object for its TIMESTAMP datatype and a {@code java.sql.Date} for DATE columns leaving out the time
   * portion: These columns will explicitly be extracted as standard {@code java.sql.Timestamp} object.
   *
   * @param rs
   *          is the ResultSet holding the data
   * @param index
   *          is the column index
   *
   * @return the value object
   *
   * @throws SQLException
   *           if thrown by the JDBC API
   *
   * @see Blob
   * @see Clob
   * @see Timestamp
   */
  @Nullable
  public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {
    Object obj = rs.getObject(index);
    String className = null;
    if (obj != null) {
      className = obj.getClass().getName();
    }
    if (obj instanceof Blob) {
      Blob blob = (Blob) obj;
      obj = blob.getBytes(1, (int) blob.length());
    } else if (obj instanceof Clob) {
      Clob clob = (Clob) obj;
      obj = clob.getSubString(1, (int) clob.length());
    } else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
      obj = rs.getTimestamp(index);
    } else if (className != null && className.startsWith("oracle.sql.DATE")) {
      String metaDataClassName = rs.getMetaData().getColumnClassName(index);
      if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
        obj = rs.getTimestamp(index);
      } else {
        obj = rs.getDate(index);
      }
    } else if (obj instanceof Date) {
      if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
        obj = rs.getTimestamp(index);
      }
    }
    return obj;
  }

  /**
   * Retrieve a JDBC column value from a ResultSet, using the specified value type.
   * <p>
   * Uses the specifically typed ResultSet accessor methods, falling back to {@link #getResultSetValue(ResultSet, int)}
   * for unknown types.
   * <p>
   * Note that the returned value may not be assignable to the specified required type, in case of an unknown type.
   * Calling code needs to deal with this case appropriately, e.g. throwing a corresponding exception.
   *
   * @param rs
   *          is the ResultSet holding the data
   * @param index
   *          is the column index
   * @param requiredType
   *          the required value type (maybe {@code null})
   *
   * @return the value object (possibly not of the specified required type, with further conversion steps necessary)
   *
   * @throws SQLException
   *           if thrown by the JDBC API
   *
   * @see #getResultSetValue(ResultSet, int)
   */
  @Nullable
  public static Object getResultSetValue(ResultSet rs, int index, @Nullable Class<?> requiredType) throws SQLException {
    if (requiredType == null) {
      return getResultSetValue(rs, index);
    }
    Object value;
    // Explicitly extract typed value, as far as possible.
    if (String.class == requiredType) {
      return rs.getString(index);
    } else if (boolean.class == requiredType || Boolean.class == requiredType) {
      value = rs.getBoolean(index);
    } else if (byte.class == requiredType || Byte.class == requiredType) {
      value = rs.getByte(index);
    } else if (short.class == requiredType || Short.class == requiredType) {
      value = rs.getShort(index);
    } else if (int.class == requiredType || Integer.class == requiredType) {
      value = rs.getInt(index);
    } else if (long.class == requiredType || Long.class == requiredType) {
      value = rs.getLong(index);
    } else if (float.class == requiredType || Float.class == requiredType) {
      value = rs.getFloat(index);
    } else if (double.class == requiredType || Double.class == requiredType || Number.class == requiredType) {
      value = rs.getDouble(index);
    } else if (BigDecimal.class == requiredType) {
      return rs.getBigDecimal(index);
    } else if (Date.class == requiredType) {
      return rs.getDate(index);
    } else if (Time.class == requiredType) {
      return rs.getTime(index);
    } else if (Timestamp.class == requiredType || java.util.Date.class == requiredType) {
      return rs.getTimestamp(index);
    } else if (byte[].class == requiredType) {
      return rs.getBytes(index);
    } else if (Blob.class == requiredType) {
      return rs.getBlob(index);
    } else if (Clob.class == requiredType) {
      return rs.getClob(index);
    } else if (requiredType.isEnum()) {
      // Enums can either be represented through a String or an enum index value:
      // leave enum type conversion up to the caller (e.g. a ConversionService)
      // but make sure that we return nothing other than a String or an Integer.
      Object obj = rs.getObject(index);
      if (obj instanceof String) {
        return obj;
      } else if (obj instanceof Number) {
        Number number = (Number) obj;
        // Defensively convert any Number to an Integer (as needed by our
        // ConversionService's IntegerToEnumConverterFactory) for use as index
        return NumberUtils.convertNumberToTargetClass(number, Integer.class);
      } else {
        // e.g. on Postgres: getObject returns a PGObject but we need a String
        return rs.getString(index);
      }
    } else {
      // Some unknown type desired -> rely on getObject.
      try {
        return rs.getObject(index, requiredType);
      } catch (AbstractMethodError err) {
        log.debug("JDBC driver does not implement JDBC 4.1 'getObject(int, Class)' method, cause: " + err.getMessage());
      } catch (SQLFeatureNotSupportedException ex) {
        log.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method, cause: " + ex.getMessage());
      } catch (SQLException ex) {
        log.debug(
            "JDBC driver has limited support for JDBC 4.1 'getObject(int, Class)' method, cause: " + ex.getMessage());
      }
      String typeName = requiredType.getSimpleName();
      switch (typeName) {
        case "LocalDate":
          return rs.getDate(index);
        case "LocalTime":
          return rs.getTime(index);
        case "LocalDateTime":
          return rs.getTimestamp(index);
        // Fall back to getObject without type specification, again
        // left up to the caller to convert the value if necessary.
        default:
          return getResultSetValue(rs, index);
      }
    }

    // Perform was-null check if necessary (for results that the JDBC driver returns as primitives).
    return (rs.wasNull() ? null : value);
  }

  private static <T> List<T> extractSingleColumn(ResultSet rs, Class<T> type) throws SQLException {
    return extractRows(rs, new SingleColumnRowMapper<>(type));
  }

  private static <T> List<T> extractBeanList(ResultSet rs, Class<T> type) throws SQLException {
    return extractRows(rs, new BeanClassRowMapper<>(type));
  }

  private static List<Map<String, Object>> extractMapList(ResultSet rs) throws SQLException {
    return extractRows(rs, new MapRowMapper());
  }

  private static List<Map<String, Object>> extractMapList(ResultSet rs, NamingStrategy namingStrategy)
      throws SQLException {
    return extractRows(rs, new MapRowMapper(namingStrategy));
  }

  private static List<Map<String, Object>> extractMapList(ResultSet rs, Function<String, String> namingStrategy)
      throws SQLException {
    return extractRows(rs, new MapRowMapper(namingStrategy));
  }

  /**
   * @param connection
   *          connection
   * @param sql
   *          sql
   * @param type
   *          type
   * @param <T>
   *          mapped result type
   *
   * @return result
   *
   * @throws SQLException
   *           data access error
   *
   * @see SimpleTypeRegistry#isSimpleType(Class)
   */
  public static <T> List<T> queryForList(@NotNull Connection connection, @NotNull String sql, @NotNull Class<T> type)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        if (SimpleTypeRegistry.isSimpleType(type)) {
          return extractSingleColumn(rs, type);
        } else {
          return extractBeanList(rs, type);
        }
      }
    }
  }

  @Nullable
  public static Map<String, Object> queryForMap(@NotNull Connection connection, @NotNull String sql, Object... args)
      throws SQLException {
    return queryForMap(connection, sql, null, args);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> T queryForObject(@NotNull Connection connection, Class<?> type, @NotNull String sql, Object... args)
      throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      setParameters(stmt, Arrays.asList(args));
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return (T) new BeanClassRowMapper<>(type).mapRow(rs, 1);
        }
      }
    }
    return null;
  }

  @Nullable
  public static Map<String, Object> queryForMap(@NotNull Connection connection, @NotNull String sql,
      @Nullable NamingStrategy namingStrategy, Object... args) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      setParameters(stmt, Arrays.asList(args));
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          MapRowMapper rowMapper = namingStrategy == null ? new MapRowMapper() : new MapRowMapper(namingStrategy);
          return rowMapper.mapRow(rs, 1);
        }
      }
    }
    return null;
  }

  /**
   * map rows
   *
   * @param connection
   *          connection
   * @param sql
   *          sql
   *
   * @return row list
   *
   * @throws SQLException
   *           data access error
   */
  public static List<Map<String, Object>> queryForList(@NotNull Connection connection, @NotNull String sql)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        return extractMapList(rs);
      }
    }
  }

  public static List<Map<String, Object>> queryForList(@NotNull Connection connection, @NotNull String sql,
      NamingStrategy namingStrategy) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        return extractMapList(rs, namingStrategy);
      }
    }
  }

  /**
   * map rows
   *
   * @param connection
   *          connection
   * @param sql
   *          sql
   * @param columNameMapping
   *          mapping from column name to key of map
   *
   * @return row list
   *
   * @throws SQLException
   *           data access error
   */
  public static List<Map<String, Object>> queryForList(@NotNull Connection connection, @NotNull String sql,
      Function<String, String> columNameMapping) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        return extractMapList(rs, columNameMapping);
      }
    }
  }

  public static int queryForInt(@NotNull Connection connection, @NotNull String countSql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(countSql)) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  /**
   * Execute an update (INSERT, UPDATE, or DELETE) SQL statement using the provided DataSource.
   * <p>
   * This method obtains a connection from the given DataSource, creates a PreparedStatement with the provided SQL, sets
   * the given parameters, and executes the update operation. The connection is automatically closed after the operation
   * completes.
   *
   * @param dataSource
   *          the DataSource to obtain a database connection from
   * @param sql
   *          the SQL update statement to execute
   * @param parameters
   *          the parameters to set on the PreparedStatement
   *
   * @return the number of rows affected by the update
   *
   * @throws SQLException
   *           if a database access error occurs, the connection cannot be obtained, or the update operation fails
   *
   * @see #update(DataSource, String, List)
   */
  public static int update(DataSource dataSource, String sql, Object... parameters) throws SQLException {
    return update(dataSource, sql, Arrays.asList(parameters));
  }

  /**
   * Execute an update (INSERT, UPDATE, or DELETE) SQL statement using the provided DataSource.
   * <p>
   * This method obtains a connection from the given DataSource, creates a PreparedStatement with the provided SQL, sets
   * the given parameters, and executes the update operation. The connection is automatically closed after the operation
   * completes.
   *
   * @param dataSource
   *          the DataSource to obtain a database connection from
   * @param sql
   *          the SQL update statement to execute
   * @param parameters
   *          the list of parameters to set on the PreparedStatement
   *
   * @return the number of rows affected by the update
   *
   * @throws SQLException
   *           if a database access error occurs, the connection cannot be obtained, or the update operation fails
   *
   * @see #update(Connection, String, List)
   */
  public static int update(DataSource dataSource, String sql, List<Object> parameters) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      return update(conn, sql, parameters);
    }
  }

  /**
   * Execute an update (INSERT, UPDATE, or DELETE) SQL statement using the provided connection.
   * <p>
   * This method creates a PreparedStatement with the provided SQL, sets the given parameters, and executes the update
   * operation. The PreparedStatement is automatically closed after the operation completes.
   *
   * @param conn
   *          the database connection to use for executing the update
   * @param sql
   *          the SQL update statement to execute
   * @param parameters
   *          the list of parameters to set on the PreparedStatement
   *
   * @return the number of rows affected by the update
   *
   * @throws SQLException
   *           if a database access error occurs, the connection is closed, or the update operation fails
   */
  public static int update(Connection conn, String sql, List<Object> parameters) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      setParameters(stmt, parameters);
      return stmt.executeUpdate();
    }
  }

  public static int update(Connection conn, String sql) throws SQLException {
    return update(conn, sql, Collections.emptyList());
  }

  public static List<Map<String, Object>> queryForList(DataSource dataSource, String sql, Object... parameters)
      throws SQLException {
    return queryForList(dataSource, sql, Arrays.asList(parameters));
  }

  public static List<Map<String, Object>> queryForList(DataSource dataSource, String sql, List<Object> parameters)
      throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      return queryForList(conn, sql, parameters);
    }
  }

  /**
   * Execute a query and return the result as a list of maps.
   * <p>
   * This method executes the provided SQL query using a PreparedStatement with the given parameters. Each row in the
   * ResultSet is converted to a Map where column names (using getColumnLabel) are used as keys and column values (using
   * getObject) are used as values. The resulting maps are added to a list which is returned.
   * <p>
   * This method ensures proper resource management by closing the ResultSet and PreparedStatement in a finally block,
   * even if an exception occurs during execution.
   *
   * @param conn
   *          the database connection to use for executing the query
   * @param sql
   *          the SQL query to execute
   * @param parameters
   *          the list of parameters to set on the PreparedStatement
   *
   * @return a List of Maps representing the rows in the ResultSet, where each Map represents a row with column names as
   *         keys and column values as values
   *
   * @throws SQLException
   *           if a database access error occurs, the connection is closed, the SQL statement is invalid, or any other
   *           SQL-related error happens during execution
   */
  public static List<Map<String, Object>> queryForList(Connection conn, String sql, List<Object> parameters)
      throws SQLException {
    List<Map<String, Object>> rows = new ArrayList<>();
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.prepareStatement(sql);
      setParameters(stmt, parameters);
      rs = stmt.executeQuery();
      ResultSetMetaData rsMeta = rs.getMetaData();
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0, size = rsMeta.getColumnCount(); i < size; ++i) {
          String columName = rsMeta.getColumnLabel(i + 1);
          Object value = rs.getObject(i + 1);
          row.put(columName, value);
        }
        rows.add(row);
      }
    } finally {
      JdbcUtils.closeQuietly(rs);
      JdbcUtils.closeQuietly(stmt);
    }
    return rows;
  }

  /**
   * Set parameters to the given PreparedStatement.
   * <p>
   * This method sets the parameters to the given PreparedStatement according to the order of the parameters in the
   * list.
   *
   * @param stmt
   *          the PreparedStatement to set parameters
   * @param parameters
   *          the parameters to set
   *
   * @throws SQLException
   *           if a database access error occurs or this method is called on a closed PreparedStatement
   */
  private static void setParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
    for (int i = 0, size = parameters.size(); i < size; ++i) {
      Object param = parameters.get(i);
      stmt.setObject(i + 1, param);
    }
  }

  /**
   * Insert data into the specified table using the provided DataSource.
   * <p>
   * This method obtains a connection from the given DataSource, generates an INSERT SQL statement based on the provided
   * table name and data, then executes the statement. The connection is automatically closed after the operation
   * completes.
   *
   * @param dataSource
   *          the DataSource to obtain a database connection from
   * @param tableName
   *          the name of the table to insert data into
   * @param data
   *          a map containing column names as keys and corresponding values to insert
   *
   * @throws SQLException
   *           if a database access error occurs, the connection cannot be obtained, or the insert operation fails
   *
   * @see #insertToTable(Connection, String, Map)
   */
  public static void insertToTable(DataSource dataSource, String tableName, Map<String, Object> data)
      throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      insertToTable(conn, tableName, data);
    }
  }

  /**
   * Insert data into the specified table using the provided connection.
   * <p>
   * This method generates an INSERT SQL statement based on the given table name and data, then executes the statement
   * with the provided connection.
   *
   * @param conn
   *          the database connection to use for the insert operation
   * @param tableName
   *          the name of the table to insert data into
   * @param row
   *          a map containing column names as keys and corresponding values to insert, the order between keys and
   *          values should perfectly match each other.
   *
   * @throws SQLException
   *           if a database access error occurs or the insert operation fails
   */
  public static int insertToTable(Connection conn, String tableName, Map<String, Object> row) throws SQLException {
    String sql = getInsertToTableSql(tableName, row.keySet());
    List<Object> parameters = new ArrayList<>(row.values());
    return update(conn, sql, parameters);
  }

  public static int insertToTable(Connection conn, String tableName, List<Map<String, Object>> rows)
      throws SQLException {
    if (CollectionUtils.isEmpty(rows)) {
      return -1;
    }
    List<String> columns = new ArrayList<>(rows.get(0).keySet());
    String sql = getBatchInsertToTableSql(tableName, columns, rows.size());
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      int paramIdx = 1;
      for (Map<String, Object> row : rows) {
        for (String column : columns) {
          stmt.setObject(paramIdx++, row.get(column));
        }
      }
      return stmt.executeUpdate();
    }
  }

  /**
   * Generate an INSERT SQL statement for the given table name and column names.
   *
   * @param tableName
   *          the name of the table to insert into
   * @param columns
   *          the collection of column names
   *
   * @return the generated INSERT SQL statement
   */
  public static String getInsertToTableSql(String tableName, Collection<String> columns) {
    StringBuilder sql = createInsertToTableSqlBuilder(tableName, columns);
    final int nameCount = columns.size();
    for (int i = 0; i < nameCount; ++i) {
      if (i != 0) {
        sql.append(", ");
      }
      sql.append("?");
    }
    sql.append(")");
    return sql.toString();
  }

  private static StringBuilder createInsertToTableSqlBuilder(String tableName, Collection<String> columns) {
    StringBuilder sql = new StringBuilder().append("insert into ").append(tableName).append("(");
    int nameCount = 0;
    for (String name : columns) {
      if (nameCount > 0) {
        sql.append(", ");
      }
      sql.append(name);
      nameCount++;
    }
    sql.append(") values (");
    return sql;
  }

  /**
   * Generate an INSERT SQL statement for the given table name, column names, and row count.
   * <p>
   * This method creates an SQL INSERT statement that can insert one or multiple rows at once into the specified table.
   * When rowCount is 1, it behaves the same as {@link #getInsertToTableSql(String, Collection)}. When rowCount is
   * greater than 1, it generates a batch insert SQL statement using the
   * {@link #getBatchInsertToTableSql(String, Collection, int)} method.
   *
   * @param tableName
   *          the name of the table to insert into
   * @param columns
   *          the collection of column names to insert data into
   * @param rowCount
   *          the number of rows to insert (must be non-negative)
   *
   * @return the generated INSERT SQL statement with appropriate parameter placeholders, or an empty string if rowCount
   *         is less than or equal to zero
   *
   * @see #getInsertToTableSql(String, Collection)
   * @see #getBatchInsertToTableSql(String, Collection, int)
   *
   * @throws IllegalArgumentException
   *           if rowCount is less than or equal to zero
   */
  public static String getInsertToTableSql(final String tableName, final Collection<String> columns,
      final int rowCount) {
    if (rowCount <= 0) {
      throw new IllegalArgumentException("row count must be greater than zero");
    }
    if (rowCount == 1) {
      return getInsertToTableSql(tableName, columns);
    }
    return getBatchInsertToTableSql(tableName, columns, rowCount);
  }

  /**
   * Generate a batch INSERT SQL statement for the given table name, column names, and row count.
   * <p>
   * This method creates an SQL INSERT statement that can insert multiple rows at once into the specified table. The
   * generated SQL uses parameter placeholders (?) for each column value in each row.
   *
   * @param tableName
   *          the name of the table to insert into
   * @param columns
   *          the collection of column names to insert data into
   * @param rowCount
   *          the number of rows to insert in the batch
   *
   * @return the generated batch INSERT SQL statement with appropriate parameter placeholders
   */
  public static String getBatchInsertToTableSql(final String tableName, final Collection<String> columns,
      final int rowCount) {
    StringBuilder sql = createInsertToTableSqlBuilder(tableName, columns);
    final int nameCount = columns.size();
    for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
      if (rowIdx > 0) {
        sql.append(", (");
      }
      for (int colIdx = 0; colIdx < nameCount; ++colIdx) {
        if (colIdx != 0) {
          sql.append(", ");
        }
        sql.append("?");
      }
      sql.append(")");
    }
    return sql.toString();
  }

  /**
   * Extract rows from a ResultSet using the provided RowMapper.
   * <p>
   * This method iterates through the ResultSet, mapping each row to an object using the provided RowMapper, and
   * collects the results into a List.
   *
   * @param rs
   *          the ResultSet to extract rows from
   * @param rowMapper
   *          the RowMapper to use for mapping each row to an object
   * @param <T>
   *          the type of objects in the resulting list
   *
   * @return a List containing the mapped objects
   *
   * @throws SQLException
   *           if a database access error occurs or the ResultSet is closed
   */
  private static <T> List<T> extractRows(ResultSet rs, RowMapper<T> rowMapper) throws SQLException {
    int rowNum = 0;
    List<T> list = new ArrayList<>();
    while (rs.next()) {
      list.add(rowMapper.mapRow(rs, rowNum++));
    }
    return list;
  }
}

/*
 *    Copyright 2009-2025 the original author or authors.
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
package org.apache.ibatis.executor.statement;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.ibatis.executor.result.BeanClassRowMapper;
import org.apache.ibatis.executor.result.MapRowMapper;
import org.apache.ibatis.executor.result.RowMapper;
import org.apache.ibatis.executor.result.SingleColumnRowMapper;
import org.apache.ibatis.internal.util.NumberUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.internal.util.TypeUtils;
import org.apache.ibatis.jdbc.RuntimeSqlException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.sql.dialect.Db2Dialect;
import org.apache.ibatis.sql.dialect.HsqlDbDialect;
import org.apache.ibatis.sql.dialect.MariaDbDialect;
import org.apache.ibatis.sql.dialect.MySqlDialect;
import org.apache.ibatis.sql.dialect.OracleDialect;
import org.apache.ibatis.sql.dialect.PostgresDialect;
import org.apache.ibatis.sql.dialect.SQLDialect;
import org.apache.ibatis.sql.dialect.SqlServerDialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for {@link Statement}.
 *
 * @author Kazuki Shimizu
 *
 * @since 3.4.0
 */
public final class JdbcUtils {

  private static final Log logger = LogFactory.getLog(JdbcUtils.class);

  private JdbcUtils() {
    // NOP
  }

  /**
   * Apply a transaction timeout.
   * <p>
   * Update a query timeout to apply a transaction timeout.
   *
   * @param statement
   *          a target statement
   * @param queryTimeout
   *          a query timeout
   * @param transactionTimeout
   *          a transaction timeout
   *
   * @throws SQLException
   *           if a database access error occurs, this method is called on a closed <code>Statement</code>
   */
  public static void applyTransactionTimeout(Statement statement, Integer queryTimeout, Integer transactionTimeout)
      throws SQLException {
    if (transactionTimeout == null) {
      return;
    }
    if (queryTimeout == null || queryTimeout == 0 || transactionTimeout < queryTimeout) {
      statement.setQueryTimeout(transactionTimeout);
    }
  }

  public static PreparedStatement prepare(Connection connection, String sql, boolean useGeneratedKeySupport)
      throws SQLException {
    PreparedStatement ps;
    if (useGeneratedKeySupport) {
      ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    } else {
      ps = connection.prepareStatement(sql);
    }
    return ps;
  }

  public static void closeSilently(Statement statement) {
    if (statement != null) {
      try {
        statement.close();
      } catch (SQLException ignore) {
      }
    }
  }

  public static void closeSilently(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException ignore) {
      }
    }
  }

  public static void closeSilently(ResultSet resultSet) {
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException ignore) {
      }
    }
  }

  public static void rollbackSilently(Connection connection) {
    try {
      if (!connection.getAutoCommit()) {
        connection.rollback();
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  public static void commit(Connection connection) throws RuntimeSqlException {
    try {
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    } catch (Throwable t) {
      throw new RuntimeSqlException("Could not commit transaction. Cause: " + t, t);
    }
  }

  public static void setAutoCommit(Connection connection, boolean autoCommit) {
    try {
      if (autoCommit != connection.getAutoCommit()) {
        connection.setAutoCommit(autoCommit);
      }
    } catch (Throwable t) {
      throw new RuntimeSqlException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
    }
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
        logger.debug("JDBC driver does not implement JDBC 4.1 'getObject(int, Class)' method", err);
      } catch (SQLFeatureNotSupportedException ex) {
        logger.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method", ex);
      } catch (SQLException ex) {
        logger.debug("JDBC driver has limited support for JDBC 4.1 'getObject(int, Class)' method", ex);
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

  public static <T> List<T> extractSingleColumn(ResultSet rs, Class<T> type) throws SQLException {
    return extractRows(rs, new SingleColumnRowMapper<>(type));
  }

  public static <T> List<T> toBeanList(ResultSet rs, Class<T> type) throws SQLException {
    return extractRows(rs, new BeanClassRowMapper<>(type));
  }

  public static List<Map<String, Object>> toMapList(ResultSet rs) throws SQLException {
    return extractRows(rs, new MapRowMapper());
  }

  public static List<Map<String, Object>> toMapList(ResultSet rs, Function<String, String> namingStrategy)
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
   * @see TypeUtils#isSimpleType(Class)
   */
  public static <T> List<T> queryForList(@NotNull Connection connection, @NotNull String sql, @NotNull Class<T> type)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        if (TypeUtils.isSimpleType(type)) {
          return extractSingleColumn(rs, type);
        } else {
          return toBeanList(rs, type);
        }
      }
    }
  }

  /**
   * map rows Map<String, Object>
   *
   * @param connection
   *          connection
   * @param sql
   *          sql
   *
   * @return List<Map < String, Object>>
   *
   * @throws SQLException
   *           data access error
   */
  public static List<Map<String, Object>> queryForMapList(@NotNull Connection connection, @NotNull String sql)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        return toMapList(rs);
      }
    }
  }

  /**
   * map rows Map<String, Object>
   *
   * @param connection
   *          connection
   * @param sql
   *          sql
   * @param columNameMapping
   *          mapping from column name to key of map
   *
   * @return List<Map < String, Object>>
   *
   * @throws SQLException
   *           data access error
   */
  public static List<Map<String, Object>> queryForMapList(@NotNull Connection connection, @NotNull String sql,
      Function<String, String> columNameMapping) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        return toMapList(rs, columNameMapping);
      }
    }
  }

  public static <T> List<T> extractRows(ResultSet rs, RowMapper<T> rowMapper) throws SQLException {
    int rowNum = 0;
    List<T> list = new ArrayList<>();
    while (rs.next()) {
      list.add(rowMapper.mapRow(rs, rowNum++));
    }
    return list;
  }

  public static SQLDialect dialect(String dialectClassName) {
    if (StringUtils.isBlank(dialectClassName)) {
      return SQLDialect.DEFAULT;
    }
    if (HsqlDbDialect.class.getName().equalsIgnoreCase(dialectClassName)) {
      return HsqlDbDialect.INSTANCE;
    } else if (Db2Dialect.class.getName().equalsIgnoreCase(dialectClassName)) {
      return Db2Dialect.INSTANCE;
    } else if (MariaDbDialect.class.getName().equalsIgnoreCase(dialectClassName)) {
      return MariaDbDialect.INSTANCE;
    } else if (MySqlDialect.class.getName().equalsIgnoreCase(dialectClassName)) {
      return MySqlDialect.INSTANCE;
    } else if (OracleDialect.class.getName().equalsIgnoreCase(dialectClassName)) {
      return OracleDialect.INSTANCE;
    } else if (PostgresDialect.class.getName().equalsIgnoreCase(dialectClassName)) {
      return PostgresDialect.INSTANCE;
    } else if (SqlServerDialect.class.getName().equalsIgnoreCase(dialectClassName)) {
      return SqlServerDialect.INSTANCE;
    }
    return SQLDialect.DEFAULT;
  }
}

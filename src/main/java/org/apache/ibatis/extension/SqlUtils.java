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
package org.apache.ibatis.extension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.ibatis.extension.metadata.ColumnInfo;
import org.apache.ibatis.extension.metadata.ColumnMetadata;
import org.apache.ibatis.extension.metadata.TableInfo;
import org.apache.ibatis.extension.metadata.TableMetadata;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.reflection.MetaObject;

/**
 * Utility class for generating SQL statements for common database operations. This class provides methods to generate
 * SELECT, INSERT, UPDATE, DELETE and other SQL scripts based on TableInfo metadata.
 *
 * @see TableInfo
 * @see ColumnInfo
 * @see TableMetadata
 * @see ColumnMetadata
 */
public final class SqlUtils {

  private SqlUtils() {
  }

  static void assertHasPrimaryKeys(TableInfo tableInfo) {
    ColumnInfo[] pkCols = tableInfo.getPrimaryKeyColumns();
    if (pkCols.length == 0) {
      throw new IllegalArgumentException("No primary column found for table " + tableInfo.getTableName());
    }
  }

  /**
   * Generates a SELECT SQL script to query a record by its primary key(s). This method constructs a SELECT statement
   * that retrieves all columns from the specified table where the primary key column(s) match the provided
   * parameter(s). The generated SQL will have parameters in MyBatis format (e.g., #{id} or #{fieldName}).
   * <p>
   * Example usage:
   * </p>
   *
   * <pre>{@code
   * TableInfo tableInfo = TableInfo.of(User.class);
   * String selectSql = SqlUtils.getSelectByIdScriptSql(tableInfo);
   * }</pre>
   * <p>
   * when the table has a single primary key named 'id', Result might be:
   * </p>
   *
   * <pre>{@code
   * SELECT id, name, email AS userEmail FROM users WHERE id = #{id}
   * }</pre>
   *
   * @param tableInfo
   *          the table information containing metadata about the table and its columns
   *
   * @return a string representing the SELECT SQL statement with MyBatis script tags
   *
   * @throws IllegalArgumentException
   *           if the table has no primary keys defined
   */
  public static String getSelectByIdScriptSql(TableInfo tableInfo) {
    assertHasPrimaryKeys(tableInfo);
    ColumnInfo[] pkCols = tableInfo.getPrimaryKeyColumns();
    final StringBuilder sqlBuilder = new StringBuilder("SELECT ");
    final List<ColumnInfo> columns = tableInfo.getColumns();
    final int columnCount = columns.size();
    for (int i = 0; i < columnCount; i++) {
      ColumnInfo col = columns.get(i);
      if (i > 0) {
        sqlBuilder.append(", ");
      }
      sqlBuilder.append(col.getColumnName());
      if (!Objects.equals(col.getColumnName(), col.getFieldName())) {
        sqlBuilder.append(" AS ").append(col.getFieldName());
      }
    }
    sqlBuilder.append(" FROM ").append(tableInfo.getTableName()).append(" WHERE ")
        .append(SqlUtils.getIdConditionSql(pkCols));
    return sqlBuilder.toString();
  }

  private static void appendColumns(StringBuilder sqlBuilder, List<ColumnInfo> columns) {
    final int columnCount = columns.size();
    for (int i = 0; i < columnCount; i++) {
      ColumnInfo col = columns.get(i);
      if (i > 0) {
        sqlBuilder.append(", ");
      }
      sqlBuilder.append(col.getColumnName());
      if (!Objects.equals(col.getColumnName(), col.getFieldName())) {
        sqlBuilder.append(" AS ").append(col.getFieldName());
      }
    }
  }

  public static String getSelectByIdsScriptSql(TableInfo tableInfo) {
    assertHasPrimaryKeys(tableInfo);
    ColumnInfo[] pkCols = tableInfo.getPrimaryKeyColumns();
    final StringBuilder sqlBuilder = new StringBuilder("<script>SELECT ");
    appendColumns(sqlBuilder, tableInfo.getColumns());
    sqlBuilder.append(" FROM ").append(tableInfo.getTableName()).append(" WHERE ")
        .append(SqlUtils.getInScriptSql(pkCols[0])).append("</script>");
    return sqlBuilder.toString();
  }

  public static String getInScriptSql(ColumnInfo col) {
    return col.getColumnName() + " IN " + getForeachScriptForInCondition("item");
  }

  /**
   * Generates a SQL condition string for primary key(s) used in WHERE clauses. This method creates appropriate SQL
   * condition based on the number of primary key columns: - Single primary key: "columnName = #{fieldName}" - Multiple
   * primary keys: "columnName1 = #{field1} AND columnName2 = #{field2} ..."
   * <p>
   * Example usage:
   * </p>
   *
   * <pre>{@code
   * // For single primary key
   * ColumnInfo[] singlePk = { new ColumnInfo("id", "id") };
   * String condition = SqlUtils.getIdConditionSql(singlePk);
   * // Result: "id = #{id}"
   *
   * // For composite primary key
   * ColumnInfo[] compositePk = { new ColumnInfo("user_id", "userId"), new ColumnInfo("role_id", "roleId") };
   * String condition = SqlUtils.getIdConditionSql(compositePk);
   * // Result: "user_id = #{userId} AND role_id = #{roleId}"
   * }</pre>
   *
   * @param idCols
   *          array of ColumnInfo objects representing the primary key columns
   *
   * @return a string representing the SQL condition for primary key matching
   *
   * @throws IllegalArgumentException
   *           if the idCols array is null or empty
   */
  public static String getIdConditionSql(ColumnInfo[] idCols) {
    if (idCols == null || idCols.length == 0) {
      throw new IllegalArgumentException("columns for id is null or empty");
    }
    String idCondition;
    if (idCols.length == 1) {
      idCondition = String.format("%s = #{id}", idCols[0].getColumnName());
    } else {
      StringBuilder idConditionBuilder = new StringBuilder();
      for (int i = 0; i < idCols.length; i++) {
        if (i > 0) {
          idConditionBuilder.append(" AND ");
        }
        idConditionBuilder.append(idCols[i].getColumnName()).append(" = ").append(asParameterExpression(idCols[i]));
      }
      idCondition = idConditionBuilder.toString();
    }
    return idCondition;
  }

  public static String asParameterExpression(ColumnInfo columnInfo) {
    Objects.requireNonNull(columnInfo, "column is null");
    return "#{" + columnInfo.getFieldName() + "}";
  }

  public static String asParameterExpression(ColumnInfo columnInfo, String prefix) {
    Objects.requireNonNull(columnInfo, "column is null");
    return "#{" + prefix + "." + columnInfo.getFieldName() + "}";
  }

  /**
   * Generates a MyBatis batch insert SQL script with dynamic column handling. This method creates an INSERT statement
   * that can handle batch insertion of multiple records using MyBatis's foreach functionality. It automatically skips
   * auto-increment columns and uses the specified prefix to reference properties in the collection items.
   * <p>
   * Example usage:
   * </p>
   *
   * <pre>{@code
   * TableInfo tableInfo = TableInfo.of(User.class);
   * String batchInsertSql = SqlUtils.getBatchInsertScriptSql(tableInfo, "item");
   * }</pre>
   * <p>
   * when inserting User objects from a collection, Result might be:
   * </p>
   *
   * <pre>{@code
   * <script>
   *   INSERT INTO users (id, name, email) VALUES
   *   <foreach collection="collection" item="item" separator=",">
   *     (#{item.id}, #{item.name}, #{item.email})
   *   </foreach>
   * </script>"
   * }
   * </pre>
   *
   * @param tableInfo
   *          the table information containing metadata about the table and its columns
   * @param prefix
   *          the prefix to use when referencing object properties in the collection (typically "item")
   *
   * @return a string representing the batch insert SQL statement with MyBatis script tags
   */
  public static String getBatchInsertScriptSql(TableInfo tableInfo, String prefix) {
    StringBuilder columns = new StringBuilder();
    StringBuilder values = new StringBuilder();
    for (ColumnInfo col : tableInfo.getColumns()) {
      if (col.isAutoIncrement()) {
        continue;
      }
      if (columns.length() != 0) {
        columns.append(", ");
        values.append(", ");
      }
      columns.append(col.getColumnName());
      // For batch, we iterate using given prefix, so access property via "prefix.field"
      values.append(SqlUtils.asParameterExpression(col, prefix));
    }

    // Wrap in <script> and <foreach>
    return String.format("<script>INSERT INTO %s (%s) VALUES "
        + "<foreach collection=\"collection\" item=\"item\" separator=\",\">" + "(%s)" + "</foreach></script>",
        tableInfo.getTableName(), columns, values);
  }

  /**
   * Generates a MyBatis update SQL script to update a record by its primary key(s). This method constructs an UPDATE
   * statement that updates non-primary key columns in the specified table where the primary key column(s) match the
   * provided parameter(s). The generated SQL uses MyBatis script tags and conditional logic to only update fields that
   * are not null.
   * <p>
   * Example usage:
   * </p>
   *
   * <pre>{@code
   * TableInfo tableInfo = TableInfo.of(User.class);
   * String updateSql = SqlUtils.getUpdateByIdScriptSql(tableInfo);
   * }</pre>
   * <p>
   * when updating User objects by ID, Result might be:
   * </p>
   *
   * <pre>{@code
   * <script>
   *   UPDATE users
   *   <set>
   *     <if test="name != null">name = #{name},</if>
   *     <if test="email != null">email = #{email},</if>
   *   </set>
   *  WHERE id = #{id}
   * </script>
   * }</pre>
   *
   * @param tableInfo
   *          the table information containing metadata about the table and its columns
   *
   * @return a string representing the UPDATE SQL statement with MyBatis script tags
   *
   * @throws IllegalArgumentException
   *           if the table has no primary keys defined
   */
  public static String getUpdateByIdScriptSql(TableInfo tableInfo) {
    StringBuilder setClause = new StringBuilder();
    setClause.append("<set>");
    for (ColumnInfo col : tableInfo.getColumns()) {
      // Do not update the PK itself
      if (col.isPrimaryKey()) {
        continue;
      }
      // <if test="field != null">col = #{field},</if>
      setClause.append(String.format("<if test=\"%s != null\">%s = #{%s},</if>", col.getFieldName(),
          col.getColumnName(), col.getFieldName()));
    }
    setClause.append("</set>");

    String whereClause = "WHERE " + SqlUtils.getIdConditionSql(tableInfo.getPrimaryKeyColumns());
    return String.format("<script>UPDATE %s %s %s</script>", tableInfo.getTableName(), setClause, whereClause);
  }

  /**
   * Generates a MyBatis delete SQL script to delete a record by its primary key(s). This method constructs a DELETE
   * statement that removes records from the specified table where the primary key column(s) match the provided
   * parameter(s). The generated SQL includes MyBatis script tags for dynamic SQL processing.
   * <p>
   * Example usage:
   * </p>
   *
   * <pre>{@code
   * TableInfo tableInfo = TableInfo.of(User.class);
   * String deleteSql = SqlUtils.getDeleteByIdScriptSql(tableInfo);
   * }</pre>
   * <p>
   * when deleting User objects by ID, Result might be:
   * </p>
   *
   * <pre>{@code
   * <script>DELETE FROM users WHERE id = #{id}</script>
   * }</pre>
   *
   * @param tableInfo
   *          the table information containing metadata about the table and its columns
   *
   * @return a string representing the DELETE SQL statement with MyBatis script tags
   *
   * @throws IllegalArgumentException
   *           if the table has no primary keys defined
   */
  public static String getDeleteByIdScriptSql(TableInfo tableInfo) {
    String whereClause = "WHERE " + SqlUtils.getIdConditionSql(tableInfo.getPrimaryKeyColumns());
    return String.format("<script>DELETE FROM %s %s</script>", tableInfo.getTableName(), whereClause);
  }

  public static String getDeleteByIdsScriptSql(TableInfo tableInfo) {
    return "<script>DELETE FROM " + tableInfo.getTableName() + " WHERE "
        + SqlUtils.getInScriptSql(tableInfo.getPrimaryKeyColumns()[0]) + "</script>";
  }

  public static String getForeachScriptForInCondition(String item) {
    return getForeachScriptForInCondition(item, null);
  }

  public static String getForeachScriptForInCondition(String item, String field) {
    field = field == null ? item : item + "." + field;
    return "<foreach collection=\"collection\" item=\"" + item + "\" separator=\",\" open=\"(\" close=\")\">#{" + field
        + "}</foreach>";
  }

  /**
   * Generates a standard INSERT SQL statement for inserting a single record into the table. This method creates an
   * INSERT statement that includes all non-auto-increment columns from the specified table. Auto-increment columns are
   * automatically excluded from the generated SQL, allowing the database to handle their values.
   * <p>
   * Example usage:
   * </p>
   *
   * <pre>{@code
   * TableInfo tableInfo = TableInfo.of(User.class);
   * String insertSql = SqlUtils.getInsertIntoTableSql(tableInfo);
   * }</pre>
   * <p>
   * when inserting a User object, excluding any auto-increment columns, Result might be:
   * </p>
   *
   * <pre>{@code
   * INSERT INTO users (id, name, email) VALUES (#{id}, #{name}, #{email})
   * }</pre>
   *
   * @param tableInfo
   *          the table information containing metadata about the table and its columns
   *
   * @return a string representing the INSERT SQL statement with MyBatis parameter placeholders
   */
  public static String getInsertIntoTableSql(TableInfo tableInfo) {
    StringBuilder columns = new StringBuilder();
    StringBuilder values = new StringBuilder();
    for (ColumnInfo col : tableInfo.getColumns()) {
      // skip AutoIncrement columns (let DB handle them)
      if (col.isAutoIncrement()) {
        continue;
      }
      if (columns.length() != 0) {
        columns.append(", ");
        values.append(", ");
      }
      columns.append(col.getColumnName());
      values.append(SqlUtils.asParameterExpression(col));
    }
    return String.format("INSERT INTO %s (%s) VALUES (%s)", tableInfo.getTableName(), columns, values);
  }

  /**
   * Generates a SELECT ALL SQL statement.
   *
   * @param tableInfo
   *          the table information
   *
   * @return an unparameterized SELECT all SQL string
   */
  public static String getSelectAllScriptSql(TableInfo tableInfo) {
    StringBuilder sqlBuilder = new StringBuilder("SELECT ");
    appendColumns(sqlBuilder, tableInfo.getColumns());
    sqlBuilder.append(" FROM ").append(tableInfo.getTableName());
    return sqlBuilder.toString();
  }

  /**
   * Generates a COUNT ALL SQL statement.
   *
   * @param tableInfo
   *          the table information
   *
   * @return a SELECT COUNT(*) SQL string
   */
  public static String getCountAllScriptSql(TableInfo tableInfo) {
    return "SELECT COUNT(*) FROM " + tableInfo.getTableName();
  }

  /**
   * Generates an EXISTS SQL statement checking if a record exists by its primary key.
   *
   * @param tableInfo
   *          the table information
   *
   * @return a SQL string that can be used to check existence
   */
  public static String getExistsByIdScriptSql(TableInfo tableInfo) {
    return "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM " + tableInfo.getTableName() + " WHERE "
        + getIdConditionSql(tableInfo.getPrimaryKeyColumns());
  }

  /**
   * Generates a DELETE ALL SQL statement.
   *
   * @param tableInfo
   *          the table information
   *
   * @return a simple DELETE FROM table string
   */
  public static String getDeleteAllScriptSql(TableInfo tableInfo) {
    return "DELETE FROM " + tableInfo.getTableName();
  }

  /**
   * Generates an UPDATE SQL script that only updates specified fields dynamically using OGNL conditions.
   *
   * @param tableInfo
   *          the table information
   *
   * @return a string representing the UPDATE SQL statement with MyBatis script tags
   */
  public static String getConditionalFieldUpdateSql(TableInfo tableInfo) {
    StringBuilder sql = new StringBuilder("<script>UPDATE ");
    sql.append(tableInfo.getTableName());
    sql.append(" <set>");

    for (ColumnInfo col : tableInfo.getColumns()) {
      if (col.isPrimaryKey()) {
        continue;
      }
      // <if test="fields != null and fields.contains('propertyName')"> columnName =
      // #{entity.propertyName}, </if>
      sql.append(
          String.format("<if test=\"fields != null and fields.contains(&quot;%s&quot;)\">%s = #{entity.%s},</if>",
              col.getFieldName(), col.getColumnName(), col.getFieldName()));
    }

    sql.append("</set> WHERE ");
    ColumnInfo[] pkCols = tableInfo.getPrimaryKeyColumns();
    for (int i = 0; i < pkCols.length; i++) {
      if (i > 0) {
        sql.append(" AND ");
      }
      sql.append(pkCols[i].getColumnName()).append(" = ").append("#{entity.").append(pkCols[i].getFieldName())
          .append("}");
    }
    sql.append("</script>");
    return sql.toString();
  }

  /**
   * Replaces indexed placeholders (?) in a SQL string with formatted values. It intelligently ignores '?' characters
   * that are inside SQL string literals or quoted identifiers.
   *
   * @param sql
   *          The raw SQL string with ? placeholders.
   * @param args
   *          The array of arguments to inline.
   * @param formatter
   *          The formatter to convert objects to SQL string literals.
   *
   * @return The fully inlined SQL string.
   */
  public static String inlineParams(String sql, Object[] args, SqlValueFormatter formatter) {
    if (sql == null) {
      return null;
    }
    Object[] safeArgs = args == null ? new Object[0] : args;
    StringBuilder result = new StringBuilder(sql.length() + 50);

    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean inBacktick = false;

    int paramIndex = 0;
    char[] chars = sql.toCharArray();

    for (char c : chars) {
      // Toggle state for single quotes (string literals)
      if (c == '\'' && !inDoubleQuote && !inBacktick) {
        inSingleQuote = !inSingleQuote;
        result.append(c);
      } else if (c == '"' && !inSingleQuote && !inBacktick) { // Toggle state for double quotes (identifiers in
        // Postgres/Oracle)
        inDoubleQuote = !inDoubleQuote;
        result.append(c);
      } // Toggle state for backticks (identifiers in MySQL)
      else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
        inBacktick = !inBacktick;
        result.append(c);
      } else if (c == '?' && !inSingleQuote && !inDoubleQuote && !inBacktick) { // Handle placeholder
        if (paramIndex >= safeArgs.length) {
          throw new IllegalArgumentException(
              "Not enough parameters provided. Expected more than " + paramIndex + " for the given SQL.");
        }
        String formattedValue = formatter.format(safeArgs[paramIndex]);
        result.append(formattedValue);
        paramIndex++;
      } else { // Regular character
        result.append(c);
      }
    }

    // Final verification
    if (paramIndex < safeArgs.length) {
      throw new IllegalArgumentException(
          "Too many parameters provided. SQL expected " + paramIndex + " but got " + safeArgs.length + ".");
    }

    return result.toString();
  }

  public static String inlineParams(String sql, MetaObject parameter, SqlValueFormatter formatter) {
    GenericTokenParser tokenParser = new GenericTokenParser("#{", "}", content -> {
      if (parameter.hasProperty(content)) {
        return formatter.format(parameter.getValue(content));
      }
      return null;
    });
    return tokenParser.parse(sql);
  }

  /**
   * Qualifies each column in a comma-separated column list with a table name.
   * <p>
   * Whitespace in the column list is preserved. Empty or {@code null} table names leave the column list unchanged.
   *
   * @param table
   *          the table name to prepend to each non-empty column
   * @param columns
   *          a comma-separated list of column names
   *
   * @return the column list with the table name prepended to each non-empty column
   */
  public static String qualifyColumns(String table, String columns) {
    return qualifyColumns(table, Arrays.asList(columns.split(",")));
  }

  private static String qualifyColumns(String table, Collection<String> columns) {
    StringBuilder sql = new StringBuilder();
    int colIdx = 0;
    for (String column : columns) {
      boolean tableAppended = false;
      for (int i = 0; i < column.length(); i++) {
        char c = column.charAt(i);
        if (c == ' ') {
          sql.append(c);
        } else {
          if (!tableAppended && table != null) {
            sql.append(table).append(".");
            tableAppended = true;
          }
          sql.append(c);
        }
      }
      if (colIdx <= columns.size() - 2) {
        sql.append(",");
      }
      colIdx++;
    }
    return sql.toString();
  }

}

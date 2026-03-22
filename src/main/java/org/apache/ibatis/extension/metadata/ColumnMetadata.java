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
package org.apache.ibatis.extension.metadata;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * metadata of a column in a rdbms database table
 *
 * @see java.sql.DatabaseMetaData#getColumns(String, String, String, String)
 */
public class ColumnMetadata implements Comparable<ColumnMetadata>, Serializable {

  private static final long serialVersionUID = 2660942930445956401L;

  /**
   * TABLE_CAT String => table catalog (maybe null)
   */
  @Nullable
  private String tableCatalog;

  /**
   * TABLE_SCHEM String => table schema (maybe null)
   */
  @Nullable
  private String tableSchema;

  /**
   * TABLE_NAME String => table name
   */
  private String tableName;

  /**
   * COLUMN_NAME String => column name
   */
  private String columnName;

  /**
   * DATA_TYPE int => SQL type from java.sql.Type
   *
   * @see java.sql.Types
   */
  private Integer dataType;

  /**
   * TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified
   */
  private String typeName;

  /**
   * The length of a signed number will decrease by 1, for example, bigint (20), where columnSize=19 COLUMN_SIZE int =>
   * column size.
   *
   * @see java.sql.DatabaseMetaData#getColumns(String, String, String, String)
   */
  private Integer columnSize;

  /**
   * BUFFER_LENGTH is not used.
   */
  private Integer bufferLength;

  /**
   * DECIMAL_DIGITS int => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not
   * applicable, such as the {@link java.sql.Types#INTEGER}.
   */
  private Integer decimalDigits;

  /**
   * Numeric Precision Radix. radix means binary, decimal, hexadecimal, etc. Numeric Precision means how many digits are
   * in the representation of the number Scale: how many digits are after the radix point
   * <p>
   * NUM_PREC_RADIX int => Radix (typically either 10 or 2) (i.e. decimal or binary)
   * <p>
   * <a href=
   * "https://stackoverflow.com/questions/28835640/what-does-numeric-precision-radix-mean-in-the-sql-server-metadata">what-does-numeric-precision-radix-mean-in-the-sql-server-metadata</a>
   */
  private Integer numericPrecisionRadix;

  /**
   * NULLABLE int => is NULL allowed.
   * <li>0 - Indicates that the column might not allow NULL values.</li>
   * <li>1 - Indicates that the column definitely allows NULL values.</li>
   * <li>2 - Indicates that the nullability of columns is unknown.</li>
   *
   * @see java.sql.DatabaseMetaData#columnNoNulls
   * @see java.sql.DatabaseMetaData#columnNullable
   * @see java.sql.DatabaseMetaData#columnNullableUnknown
   */
  private Integer nullable;

  /**
   * REMARKS String => comment describing column (may be null)
   */
  @Nullable
  private String remarks;

  /**
   * COLUMN_DEF String => default value for the column, which should be interpreted as a string when the value is
   * enclosed in single quotes (maybe null)
   */
  @Nullable
  private String columnDef;

  /**
   * SQL_DATA_TYPE int => unused
   */
  private Integer sqlDataType;

  /**
   * SQL_DATETIME_SUB int => unused
   */
  private Integer sqlDatetimeSub;

  /**
   * CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column
   */
  private Integer charOctetLength;

  /**
   * ORDINAL_POSITION int => index of column in table (starting at 1)
   */
  private Integer ordinalPosition;

  /**
   * IS_NULLABLE String => ISO rules are used to determine the nullability for a column.
   * <li>YES --- if the column can include NULLs</li>
   * <li>NO --- if the column cannot include NULLs</li>
   * <li>empty string --- if the nullability for the column is unknown</li>
   *
   * @see <a href=
   *      "https://stackoverflow.com/questions/26490427/jdbc-getcolumns-differences-between-is-nullable-and-nullable">JDBC
   *      getColumns differences between "IS_NULLABLE" and "NULLABLE"</a>
   * @see ColumnMetadata#nullable
   *      https://stackoverflow.com/questions/26490427/jdbc-getcolumns-differences-between-is-nullable-and-nullable
   */
  private String nullableDescription;

  /**
   * SCOPE_CATALOG String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
   */
  private String scopeCatalog;

  /**
   * SCOPE_SCHEMA String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
   */
  private String scopeSchema;

  /**
   * SCOPE_TABLE String => table name that this the scope of a reference attribute (null if the DATA_TYPE isn't REF)
   */
  @Nullable
  private String scopeTable;

  /**
   * SOURCE_DATA_TYPE short => source type of distinct type or user-generated Ref type, SQL type from java.sql.Types
   * (null if DATA_TYPE isn't DISTINCT or user-generated REF)
   */
  @Nullable
  private Short sourceDataType;

  /**
   * IS_AUTOINCREMENT String => Indicates whether this column is auto incremented YES --- if the column is auto
   * incremented NO --- if the column is not auto incremented empty string --- if it cannot be determined whether the
   * column is auto incremented
   */
  private String autoIncrement;

  /**
   * IS_GENERATEDCOLUMN String => Indicates whether this is a generated column YES --- if this a generated column NO ---
   * if this not a generated column empty string --- if it cannot be determined whether this is a generated column
   */
  private String generatedColumn;

  // ==========================================================================

  /**
   * Data type, platform independent data type, such as MySQL "bigint (19) unsigned"
   */
  private String dataTypeDescriptor;

  /**
   * The data type names of different database platforms, such as varchar in MySQL, varchar2 in Oracle, etc., are not
   * values that can be obtained from JDBC metadata
   *
   * @see java.sql.DatabaseMetaData#getColumns(String, String, String, String)
   */
  private String platformDataType;

  /**
   * Code name, usually only character types have values
   */
  private String charsetName;

  /**
   * Sorting Rule Name
   */
  private String collationName;

  /**
   * index type of the column
   */
  private String columnKey;

  /**
   * Is it a primary key
   */
  private boolean primaryKey = false;

  /**
   * extra attributes
   */
  @Nullable
  private Map<String, Object> attributes;

  /**
   * isAutoincrement cannot be null
   *
   * @return if this column is autoincrement, true, or else false
   */
  public boolean isAutoIncrement() {
    return "YES".equals(autoIncrement);
  }

  /**
   * add attribute
   *
   * @param name
   *          name
   * @param value
   *          value
   */
  public void addAttribute(String name, Object value) {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    attributes.put(name, value);
  }

  public void initialize(ResultSet resultSet) throws SQLException {
    this.tableCatalog = resultSet.getString("TABLE_CAT");
    this.tableSchema = resultSet.getString("TABLE_SCHEM");
    this.tableName = resultSet.getString("TABLE_NAME");
    this.columnName = resultSet.getString("COLUMN_NAME");
    this.dataType = resultSet.getInt("DATA_TYPE");
    this.typeName = resultSet.getString("TYPE_NAME");
    this.columnSize = resultSet.getInt("COLUMN_SIZE");
    this.bufferLength = resultSet.getInt("BUFFER_LENGTH");
    this.decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
    this.numericPrecisionRadix = resultSet.getInt("NUM_PREC_RADIX");
    this.nullable = resultSet.getInt("NULLABLE");
    this.remarks = resultSet.getString("REMARKS");
    this.columnDef = resultSet.getString("COLUMN_DEF");
    this.sqlDataType = resultSet.getInt("SQL_DATA_TYPE");
    this.sqlDatetimeSub = resultSet.getInt("SQL_DATETIME_SUB");
    this.charOctetLength = resultSet.getInt("CHAR_OCTET_LENGTH");
    this.ordinalPosition = resultSet.getInt("ORDINAL_POSITION");
    this.nullableDescription = resultSet.getString("IS_NULLABLE");
    this.scopeCatalog = resultSet.getString("SCOPE_CATALOG");
    this.scopeSchema = resultSet.getString("SCOPE_SCHEMA");
    this.scopeTable = resultSet.getString("SCOPE_TABLE");
    this.sourceDataType = resultSet.getShort("SOURCE_DATA_TYPE");
    this.autoIncrement = resultSet.getString("IS_AUTOINCREMENT");
    this.generatedColumn = resultSet.getString("IS_GENERATEDCOLUMN");
  }

  @Nullable
  public String getTableCatalog() {
    return tableCatalog;
  }

  public void setTableCatalog(@Nullable String tableCatalog) {
    this.tableCatalog = tableCatalog;
  }

  @Nullable
  public String getTableSchema() {
    return tableSchema;
  }

  public void setTableSchema(@Nullable String tableSchema) {
    this.tableSchema = tableSchema;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public Integer getDataType() {
    return dataType;
  }

  public void setDataType(Integer dataType) {
    this.dataType = dataType;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public Integer getColumnSize() {
    return columnSize;
  }

  public void setColumnSize(Integer columnSize) {
    this.columnSize = columnSize;
  }

  public Integer getBufferLength() {
    return bufferLength;
  }

  public void setBufferLength(Integer bufferLength) {
    this.bufferLength = bufferLength;
  }

  public Integer getDecimalDigits() {
    return decimalDigits;
  }

  public void setDecimalDigits(Integer decimalDigits) {
    this.decimalDigits = decimalDigits;
  }

  public Integer getNumericPrecisionRadix() {
    return numericPrecisionRadix;
  }

  public void setNumericPrecisionRadix(Integer numericPrecisionRadix) {
    this.numericPrecisionRadix = numericPrecisionRadix;
  }

  public Integer getNullable() {
    return nullable;
  }

  public void setNullable(Integer nullable) {
    this.nullable = nullable;
  }

  @Nullable
  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(@Nullable String remarks) {
    this.remarks = remarks;
  }

  @Nullable
  public String getColumnDef() {
    return columnDef;
  }

  public void setColumnDef(@Nullable String columnDef) {
    this.columnDef = columnDef;
  }

  public Integer getSqlDataType() {
    return sqlDataType;
  }

  public void setSqlDataType(Integer sqlDataType) {
    this.sqlDataType = sqlDataType;
  }

  public Integer getSqlDatetimeSub() {
    return sqlDatetimeSub;
  }

  public void setSqlDatetimeSub(Integer sqlDatetimeSub) {
    this.sqlDatetimeSub = sqlDatetimeSub;
  }

  public Integer getCharOctetLength() {
    return charOctetLength;
  }

  public void setCharOctetLength(Integer charOctetLength) {
    this.charOctetLength = charOctetLength;
  }

  public Integer getOrdinalPosition() {
    return ordinalPosition;
  }

  public void setOrdinalPosition(Integer ordinalPosition) {
    this.ordinalPosition = ordinalPosition;
  }

  public String getNullableDescription() {
    return nullableDescription;
  }

  public void setNullableDescription(String nullableDescription) {
    this.nullableDescription = nullableDescription;
  }

  /**
   * avoid to overwrite setNullable(Integer) by define this method as setNullable(Boolean)
   *
   * @param nullable
   *          nullable
   */
  public void markNullable(Boolean nullable) {
    if (nullable == null) {
      this.nullable = 2;
      this.nullableDescription = "";
      return;
    }
    this.nullableDescription = nullable ? "YES" : "NO";
    if (nullable) {
      this.nullable = 1;
    } else {
      this.nullable = 0;
    }
  }

  public String getScopeCatalog() {
    return scopeCatalog;
  }

  public void setScopeCatalog(String scopeCatalog) {
    this.scopeCatalog = scopeCatalog;
  }

  public String getScopeSchema() {
    return scopeSchema;
  }

  public void setScopeSchema(String scopeSchema) {
    this.scopeSchema = scopeSchema;
  }

  @Nullable
  public String getScopeTable() {
    return scopeTable;
  }

  public void setScopeTable(@Nullable String scopeTable) {
    this.scopeTable = scopeTable;
  }

  @Nullable
  public Short getSourceDataType() {
    return sourceDataType;
  }

  public void setSourceDataType(@Nullable Short sourceDataType) {
    this.sourceDataType = sourceDataType;
  }

  public String getAutoIncrement() {
    return autoIncrement;
  }

  public void setAutoIncrement(String autoIncrement) {
    this.autoIncrement = autoIncrement;
  }

  public String getGeneratedColumn() {
    return generatedColumn;
  }

  public void setGeneratedColumn(String generatedColumn) {
    this.generatedColumn = generatedColumn;
  }

  public String getDataTypeDescriptor() {
    return dataTypeDescriptor;
  }

  public void setDataTypeDescriptor(String dataTypeDescriptor) {
    this.dataTypeDescriptor = dataTypeDescriptor;
  }

  public String getPlatformDataType() {
    return platformDataType;
  }

  public void setPlatformDataType(String platformDataType) {
    this.platformDataType = platformDataType;
  }

  public String getCharsetName() {
    return charsetName;
  }

  public void setCharsetName(String charsetName) {
    this.charsetName = charsetName;
  }

  public String getCollationName() {
    return collationName;
  }

  public void setCollationName(String collationName) {
    this.collationName = collationName;
  }

  public String getColumnKey() {
    return columnKey;
  }

  public void setColumnKey(String columnKey) {
    this.columnKey = columnKey;
  }

  public boolean isPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(boolean primaryKey) {
    this.primaryKey = primaryKey;
  }

  @Nullable
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(@Nullable Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public boolean isNullable() {
    return "YES".equals(nullableDescription);
  }

  public boolean isNullableUnknown() {
    return "".equals(nullableDescription);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ColumnMetadata))
      return false;
    // @formatter:off
    ColumnMetadata that = (ColumnMetadata) o;
    return primaryKey == that.primaryKey
      && Objects.equals(tableCatalog, that.tableCatalog)
      && Objects.equals(tableSchema, that.tableSchema)
      && Objects.equals(tableName, that.tableName)
      && Objects.equals(columnName, that.columnName)
      && Objects.equals(dataType, that.dataType)
      && Objects.equals(typeName, that.typeName)
      && Objects.equals(columnSize, that.columnSize)
      && Objects.equals(bufferLength, that.bufferLength)
      && Objects.equals(decimalDigits, that.decimalDigits)
      && Objects.equals(numericPrecisionRadix, that.numericPrecisionRadix)
      && Objects.equals(nullable, that.nullable)
      && Objects.equals(remarks, that.remarks)
      && Objects.equals(columnDef, that.columnDef)
      && Objects.equals(sqlDataType, that.sqlDataType)
      && Objects.equals(sqlDatetimeSub, that.sqlDatetimeSub)
      && Objects.equals(charOctetLength, that.charOctetLength)
      && Objects.equals(ordinalPosition, that.ordinalPosition)
      && Objects.equals(nullableDescription, that.nullableDescription)
      && Objects.equals(scopeCatalog, that.scopeCatalog)
      && Objects.equals(scopeSchema, that.scopeSchema)
      && Objects.equals(scopeTable, that.scopeTable)
      && Objects.equals(sourceDataType, that.sourceDataType)
      && Objects.equals(autoIncrement, that.autoIncrement)
      && Objects.equals(generatedColumn, that.generatedColumn)
      && Objects.equals(dataTypeDescriptor, that.dataTypeDescriptor)
      && Objects.equals(platformDataType, that.platformDataType)
      && Objects.equals(charsetName, that.charsetName)
      && Objects.equals(collationName, that.collationName)
      && Objects.equals(columnKey, that.columnKey);
    // @formatter:on
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableCatalog, tableSchema, tableName, columnName, dataType, typeName, columnSize, bufferLength,
        decimalDigits, numericPrecisionRadix, nullable, remarks, columnDef, sqlDataType, sqlDatetimeSub,
        charOctetLength, ordinalPosition, nullableDescription, scopeCatalog, scopeSchema, scopeTable, sourceDataType,
        autoIncrement, generatedColumn, dataTypeDescriptor, platformDataType, charsetName, collationName, columnKey,
        primaryKey);
  }

  @Override
  public int compareTo(@NotNull ColumnMetadata o) {
    // Comparison chain to prefer ordinal position (DB order), then name
    return Comparator.comparing(ColumnMetadata::getOrdinalPosition, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(ColumnMetadata::getColumnName, Comparator.nullsLast(String::compareTo)).compare(this, o);
  }
}

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
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC specification</a>
 *
 * @see ColumnMetadata
 * @see java.sql.DatabaseMetaData#getTables(String, String, String, String[])
 */
public class TableMetadata implements Comparable<TableMetadata>, Serializable {

  private static final long serialVersionUID = 2005427479800838110L;

  /**
   * TABLE_CAT String => table catalog (may be null)
   */
  private String tableCatalog;

  /**
   * TABLE_SCHEM String => table schema (maybe null)
   */
  private String tableSchema;

  /**
   * TABLE_NAME String => table name
   */
  private String tableName;

  /**
   * TABLE_TYPE String => table type. Typical types are:
   * <ul>
   * <li>"TABLE"</li>
   * <li>"VIEW"</li>
   * <li>"SYSTEM TABLE"</li>
   * <li>"GLOBAL TEMPORARY"</li>
   * <li>"LOCAL TEMPORARY"</li>
   * <li>"ALIAS"</li>
   * <li>"SYNONYM"</li>
   * </ul>
   *
   * @see TableType
   */
  private String tableType;

  /**
   * String => explanatory comment on the table (maybe null)
   */
  @Nullable
  private String remarks;

  /**
   * String => the types catalog (maybe null)
   */
  @Nullable
  private String typeCatalog;

  /**
   * String => the types schema (maybe null)
   */
  @Nullable
  private String typeSchema;

  /**
   * TYPE_NAME String => type name (maybe null)
   */
  @Nullable
  private String typeName;

  /**
   * SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (maybe null)
   */
  @Nullable
  private String selfReferencingColumnName;

  /**
   * REF_GENERATION String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are
   * <ul>
   * <li>"SYSTEM"</li>
   * <li>"USER"</li>
   * <li>"DERIVED"</li>
   * </ul>
   */
  @Nullable
  private String refGeneration;

  /**
   * attributes
   */
  private Map<String, Object> attributes;

  public void initialize(ResultSet resultSet) throws SQLException {
    this.tableCatalog = resultSet.getString("TABLE_CAT");
    this.tableSchema = resultSet.getString("TABLE_SCHEM");
    this.tableName = resultSet.getString("TABLE_NAME");
    this.tableType = resultSet.getString("TABLE_TYPE");
    this.remarks = resultSet.getString("REMARKS");
    this.typeCatalog = resultSet.getString("TYPE_CAT");
    this.typeSchema = resultSet.getString("TYPE_SCHEM");
    this.typeName = resultSet.getString("TYPE_NAME");
    this.selfReferencingColumnName = resultSet.getString("SELF_REFERENCING_COL_NAME");
    this.refGeneration = resultSet.getString("REF_GENERATION");
  }

  public String getTableCatalog() {
    return tableCatalog;
  }

  public void setTableCatalog(String tableCatalog) {
    this.tableCatalog = tableCatalog;
  }

  public String getTableSchema() {
    return tableSchema;
  }

  public void setTableSchema(String tableSchema) {
    this.tableSchema = tableSchema;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getTableType() {
    return tableType;
  }

  public void setTableType(String tableType) {
    this.tableType = tableType;
  }

  @Nullable
  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(@Nullable String remarks) {
    this.remarks = remarks;
  }

  @Nullable
  public String getTypeCatalog() {
    return typeCatalog;
  }

  public void setTypeCatalog(@Nullable String typeCatalog) {
    this.typeCatalog = typeCatalog;
  }

  @Nullable
  public String getTypeSchema() {
    return typeSchema;
  }

  public void setTypeSchema(@Nullable String typeSchema) {
    this.typeSchema = typeSchema;
  }

  @Nullable
  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(@Nullable String typeName) {
    this.typeName = typeName;
  }

  @Nullable
  public String getSelfReferencingColumnName() {
    return selfReferencingColumnName;
  }

  public void setSelfReferencingColumnName(@Nullable String selfReferencingColumnName) {
    this.selfReferencingColumnName = selfReferencingColumnName;
  }

  @Nullable
  public String getRefGeneration() {
    return refGeneration;
  }

  public void setRefGeneration(@Nullable String refGeneration) {
    this.refGeneration = refGeneration;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof TableMetadata))
      return false;
    TableMetadata that = (TableMetadata) o;
    // @formatter:off
    return Objects.equals(tableCatalog, that.tableCatalog)
      && Objects.equals(tableSchema, that.tableSchema)
      && Objects.equals(tableName, that.tableName)
      && Objects.equals(tableType, that.tableType)
      && Objects.equals(remarks, that.remarks)
      && Objects.equals(typeCatalog, that.typeCatalog)
      && Objects.equals(typeSchema, that.typeSchema)
      && Objects.equals(typeName, that.typeName)
      && Objects.equals(selfReferencingColumnName, that.selfReferencingColumnName)
      && Objects.equals(refGeneration, that.refGeneration);
    // @formatter:on
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableCatalog, tableSchema, tableName, tableType, remarks, typeCatalog, typeSchema, typeName,
        selfReferencingColumnName, refGeneration);
  }

  @Override
  public int compareTo(@NotNull TableMetadata o) {
    return Comparator.comparing(TableMetadata::getTableCatalog, Comparator.nullsFirst(String::compareTo))
        .thenComparing(TableMetadata::getTableSchema, Comparator.nullsFirst(String::compareTo))
        .thenComparing(TableMetadata::getTableName, Comparator.nullsFirst(String::compareTo)).compare(this, o);
  }
}

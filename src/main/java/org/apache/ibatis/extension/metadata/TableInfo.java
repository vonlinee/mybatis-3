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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.apache.ibatis.extension.CompositeTableInfoParser;

public class TableInfo {

  private final TableMetadata metadata;
  private final TableType tableType;
  private Class<?> entityClass;
  private String entityName;
  private List<ColumnInfo> columns = new ArrayList<>();

  public TableInfo(TableMetadata metadata) {
    this.metadata = metadata;
    this.tableType = TableType.lookup(metadata.getTableType());
  }

  public List<ColumnInfo> getColumns() {
    return columns;
  }

  public ColumnInfo[] getPrimaryKeyColumns() {
    return columns.stream().filter(ColumnInfo::isPrimaryKey).toArray(ColumnInfo[]::new);
  }

  public ColumnInfo[] getAutoIncrementColumns() {
    return columns.stream().filter(ColumnInfo::isAutoIncrement).toArray(ColumnInfo[]::new);
  }

  public void setColumns(List<ColumnInfo> columns) {
    this.columns = columns;
  }

  public TableMetadata getMetadata() {
    return metadata;
  }

  public TableType getTableType() {
    return tableType;
  }

  public Class<?> getEntityClass() {
    return entityClass;
  }

  public void setEntityClass(Class<?> entityClass) {
    this.entityClass = entityClass;
  }

  public String getTableName() {
    return metadata.getTableName();
  }

  public int getColumnCount() {
    return columns.size();
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  /**
   * Helper method to add a single column.
   *
   * @param column
   *          the column info to add
   */
  public void addColumn(ColumnInfo column) {
    if (this.columns == null) {
      this.columns = new ArrayList<>();
    }
    this.columns.add(column);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof TableInfo))
      return false;
    TableInfo tableInfo = (TableInfo) o;
    // Equality based on underlying metadata and the mapped entity
    return Objects.equals(metadata, tableInfo.metadata) && Objects.equals(entityName, tableInfo.entityName)
        && Objects.equals(entityClass, tableInfo.entityClass) && Objects.equals(columns, tableInfo.columns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, entityName, entityClass, columns);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TableInfo.class.getSimpleName() + "[", "]").add("table='" + getTableName() + "'")
        .add("entity='" + entityName + "'").add("type=" + tableType)
        .add("columns=" + (columns != null ? columns.size() : 0)).toString();
  }

  public static TableInfo of(Class<?> type) {
    return new CompositeTableInfoParser().parse(type);
  }
}

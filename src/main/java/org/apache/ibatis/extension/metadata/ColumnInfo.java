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

import java.util.Objects;
import java.util.StringJoiner;

public class ColumnInfo {

  private String columnName;
  private Class<?> javaType;
  private String fieldName;
  private final ColumnMetadata metadata;

  public ColumnInfo(ColumnMetadata metadata) {
    this.columnName = metadata.getColumnName();
    this.metadata = metadata;
  }

  public ColumnMetadata getMetadata() {
    return metadata;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setJavaType(Class<?> type) {
    this.javaType = type;
  }

  public Class<?> getJavaType() {
    return javaType;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public boolean isPrimaryKey() {
    return metadata.isPrimaryKey();
  }

  public boolean isAutoIncrement() {
    return metadata.isAutoIncrement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ColumnInfo))
      return false;
    ColumnInfo that = (ColumnInfo) o;
    // We compare metadata first as it is the source of truth
    return Objects.equals(metadata, that.metadata) && Objects.equals(columnName, that.columnName)
        && Objects.equals(fieldName, that.fieldName) && Objects.equals(javaType, that.javaType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName, javaType, fieldName, metadata);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ColumnInfo.class.getSimpleName() + "[", "]").add("columnName='" + columnName + "'")
        .add("fieldName='" + fieldName + "'").add("javaType=" + (javaType != null ? javaType.getSimpleName() : "null"))
        .toString();
  }
}

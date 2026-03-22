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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.ibatis.extension.metadata.*;

/**
 * Strategy to parse classes annotated with JPA annotations.
 */
public class JpaTableInfoParser extends AbstractTableInfoParser {

  @Override
  public TableInfo parse(Class<?> clazz) {
    TableMetadata tableMetadata = new TableMetadata();

    // 1. Resolve Table Name
    String tableName = getTableName(clazz);
    String catalog = null;
    String schema = null;

    if (clazz.isAnnotationPresent(Table.class)) {
      Table table = clazz.getAnnotation(Table.class);
      if (!table.name().isEmpty())
        tableName = table.name();
      if (!table.catalog().isEmpty())
        catalog = table.catalog();
      if (!table.schema().isEmpty())
        schema = table.schema();
    }

    tableMetadata.setTableName(tableName);
    tableMetadata.setTableCatalog(catalog);
    tableMetadata.setTableSchema(schema);
    tableMetadata.setTableType(TableType.TABLE.name());

    TableInfo tableInfo = new TableInfo(tableMetadata);
    tableInfo.setEntityClass(clazz);
    tableInfo.setEntityName(clazz.getSimpleName());
    tableInfo.setColumns(parseColumns(clazz));

    return tableInfo;
  }

  private List<ColumnInfo> parseColumns(Class<?> clazz) {
    List<ColumnInfo> columns = new ArrayList<>();
    int ordinal = 1;

    for (Field field : clazz.getDeclaredFields()) {
      if (isIgnoredField(field)) {
        continue;
      }

      ColumnMetadata meta = new ColumnMetadata();
      // Defaults
      String columnName = getColumnName(field);
      boolean isNullable = !field.getType().isPrimitive();
      int length = 255;
      int scale = 0;
      int precision = 0;

      // JPA Overrides
      if (field.isAnnotationPresent(Column.class)) {
        Column col = field.getAnnotation(Column.class);
        if (!col.name().isEmpty()) {
          columnName = col.name();
        }
        isNullable = col.nullable();
        length = col.length();
        precision = col.precision();
        scale = col.scale();
      }

      // PK Logic
      if (field.isAnnotationPresent(Id.class)) {
        meta.setPrimaryKey(true);
        meta.setColumnKey("PRI");
      }

      // Auto Increment Logic
      if (field.isAnnotationPresent(GeneratedValue.class)) {
        meta.setAutoIncrement("YES");
      } else {
        meta.setAutoIncrement("NO");
      }

      meta.setColumnName(columnName);
      meta.setTypeName(resolveSqlTypeName(field.getType()));
      meta.setDataType(resolveSqlType(field.getType()));
      meta.markNullable(isNullable);
      meta.setOrdinalPosition(ordinal++);

      if (precision > 0) {
        meta.setColumnSize(precision);
        meta.setDecimalDigits(scale);
      } else {
        meta.setColumnSize(length);
      }

      ColumnInfo colInfo = new ColumnInfo(meta);
      colInfo.setFieldName(field.getName());
      colInfo.setJavaType(field.getType());
      columns.add(colInfo);
    }
    return columns;
  }
}

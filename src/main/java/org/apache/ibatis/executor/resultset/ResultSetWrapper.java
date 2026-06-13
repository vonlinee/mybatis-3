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
package org.apache.ibatis.executor.resultset;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Iwao AVE!
 */
public class ResultSetWrapper {

  private final ResultSet resultSet;
  private final TypeHandlerRegistry typeHandlerRegistry;
  private final List<String> columnNames = new ArrayList<>();
  private final List<String> classNames = new ArrayList<>();
  private final List<JdbcType> jdbcTypes = new ArrayList<>();
  private final Map<String, Map<Type, TypeHandler<?>>> typeHandlerMap = new HashMap<>();
  private final Map<String, Set<String>> mappedColumnNamesMap = new HashMap<>();
  private final Map<String, List<String>> unMappedColumnNamesMap = new HashMap<>();

  public ResultSetWrapper(ResultSet rs, Configuration configuration) throws SQLException {
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.resultSet = rs;
    final ResultSetMetaData metaData = rs.getMetaData();
    final int columnCount = metaData.getColumnCount();
    for (int i = 1; i <= columnCount; i++) {
      columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
      jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
      classNames.add(metaData.getColumnClassName(i));
    }
  }

  public ResultSet getResultSet() {
    return resultSet;
  }

  public int getColumnCount() {
    return columnNames.size();
  }

  public List<String> getColumnNames() {
    return this.columnNames;
  }

  public String getColumnName(int columnIndex) {
    return columnNames.get(columnIndex);
  }

  public List<String> getClassNames() {
    return Collections.unmodifiableList(classNames);
  }

  public List<JdbcType> getJdbcTypes() {
    return jdbcTypes;
  }

  public JdbcType getJdbcType(String columnName) {
    int columnIndex = getColumnIndex(columnName);
    return columnIndex == -1 ? null : jdbcTypes.get(columnIndex);
  }

  public JdbcType getJdbcType(int columnIndex) {
    return columnIndex == -1 ? null : jdbcTypes.get(columnIndex);
  }

  /**
   * Gets the type handler to use when reading the result set. Tries to get from the TypeHandlerRegistry by searching
   * for the property type. If not found it gets the column JDBC type and tries to get a handler for it.
   *
   * @param propertyType
   *          the property type
   * @param columnName
   *          the column name
   *
   * @return the type handler
   */
  public TypeHandler<?> getTypeHandler(Type propertyType, String columnName) {
    return typeHandlerMap.computeIfAbsent(columnName, k -> new HashMap<>()).computeIfAbsent(propertyType, k -> {
      int index = getColumnIndex(columnName);
      if (index == -1) {
        return ObjectTypeHandler.INSTANCE;
      }

      JdbcType jdbcType = getJdbcType(index);
      TypeHandler<?> handler = typeHandlerRegistry.getTypeHandler(k, jdbcType, null);
      if (handler != null) {
        return handler;
      }

      Class<?> javaType = getColumnType(index);
      if (!(k instanceof Class && ((Class<?>) k).isAssignableFrom(javaType))) {
        // Clearly incompatible
        return null;
      }

      handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType, null);
      if (handler == null) {
        handler = typeHandlerRegistry.getTypeHandler(jdbcType);
      }
      return handler == null ? ObjectTypeHandler.INSTANCE : handler;
    });
  }

  public Class<?> getColumnType(int columnIndex) {
    return resolveClass(classNames.get(columnIndex));
  }

  static Class<?> resolveClass(String className) {
    // #699 className could be null
    if (className != null) {
      return Resources.classForNameOrElseNull(className);
    }
    return null;
  }

  private int getColumnIndex(String columnName) {
    for (int i = 0; i < columnNames.size(); i++) {
      if (columnNames.get(i).equalsIgnoreCase(columnName)) {
        return i;
      }
    }
    return -1;
  }

  private void loadMappedAndUnmappedColumnNames(String mapKey, Set<String> mappedColumns) {
    Set<String> mappedColumnNames = new HashSet<>();
    List<String> unmappedColumnNames = new ArrayList<>();
    for (String columnName : columnNames) {
      final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
      if (mappedColumns.contains(upperColumnName)) {
        mappedColumnNames.add(upperColumnName);
      } else {
        unmappedColumnNames.add(columnName);
      }
    }
    mappedColumnNamesMap.put(mapKey, mappedColumnNames);
    unMappedColumnNamesMap.put(mapKey, unmappedColumnNames);
  }

  public Set<String> getMappedColumnNames(String mapKey, Supplier<Set<String>> mappedColumns) {
    Set<String> mappedColumnNames = mappedColumnNamesMap.get(mapKey);
    if (mappedColumnNames == null) {
      loadMappedAndUnmappedColumnNames(mapKey, mappedColumns.get());
      mappedColumnNames = mappedColumnNamesMap.get(mapKey);
    }
    return mappedColumnNames;
  }

  public List<String> getUnmappedColumnNames(String mapKey, Supplier<Set<String>> mappedColumns) {
    List<String> unMappedColumnNames = unMappedColumnNamesMap.get(mapKey);
    if (unMappedColumnNames == null) {
      loadMappedAndUnmappedColumnNames(mapKey, mappedColumns.get());
      unMappedColumnNames = unMappedColumnNamesMap.get(mapKey);
    }
    return unMappedColumnNames;
  }
}

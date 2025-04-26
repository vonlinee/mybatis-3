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
package org.apache.ibatis.executor.resultset;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

public class ResultSetTypeHandlerLookup implements TypeHandlerLookup {

  private final TypeHandlerRegistry typeHandlerRegistry;

  private final Map<String, Map<Type, TypeHandler<?>>> typeHandlerMap = new HashMap<>();

  public ResultSetTypeHandlerLookup(TypeHandlerRegistry typeHandlerRegistry) {
    this.typeHandlerRegistry = typeHandlerRegistry;
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
  @Override
  public TypeHandler<?> getTypeHandler(ResultSetWrapper rsw, Type propertyType, String columnName) {
    return typeHandlerMap.computeIfAbsent(columnName, k -> new HashMap<>()).computeIfAbsent(propertyType, k -> {
      int index = rsw.getColumnIndex(columnName);
      if (index == -1) {
        return ObjectTypeHandler.INSTANCE;
      }

      JdbcType jdbcType = rsw.getJdbcType(index);
      TypeHandler<?> handler = typeHandlerRegistry.getTypeHandler(k, jdbcType, null);
      if (handler != null) {
        return handler;
      }

      Class<?> javaType = rsw.getJavaType(index);
      if (javaType == null) {
        return null;
      }
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
}

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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.TypeHandler;
import org.jetbrains.annotations.Nullable;

public class UnMappedColumnAutoMapping {

  /**
   * column name
   */
  private final String column;

  /**
   * property
   */
  private final String property;

  /**
   * type handler
   */
  private final TypeHandler<?> typeHandler;

  /**
   * primitive
   */
  private final boolean primitive;

  public UnMappedColumnAutoMapping(String column, String property, TypeHandler<?> typeHandler, boolean primitive) {
    this.column = column;
    this.property = property;
    this.typeHandler = typeHandler;
    this.primitive = primitive;
  }

  @Nullable
  public Object getResult(ResultSet resultSet) throws SQLException {
    if (this.typeHandler == null) {
      return null;
    }
    return typeHandler.getResult(resultSet, this.column);
  }

  public String getColumn() {
    return column;
  }

  public boolean isPrimitive() {
    return primitive;
  }

  public String getProperty() {
    return property;
  }

  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }
}

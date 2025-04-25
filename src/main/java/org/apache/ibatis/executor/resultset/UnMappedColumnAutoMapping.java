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

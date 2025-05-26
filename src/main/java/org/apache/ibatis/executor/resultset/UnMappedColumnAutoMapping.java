package org.apache.ibatis.executor.resultset;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.TypeHandler;

class UnMappedColumnAutoMapping {
  private final String column;
  private final String property;
  private final TypeHandler<?> typeHandler;
  private final boolean primitive;

  public UnMappedColumnAutoMapping(String column, String property, TypeHandler<?> typeHandler, boolean primitive) {
    this.column = column;
    this.property = property;
    this.typeHandler = typeHandler;
    this.primitive = primitive;
  }

  public Object getResult(ResultSet resultSet) throws SQLException {
    return this.typeHandler.getResult(resultSet, this.column);
  }

  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  public String getProperty() {
    return property;
  }

  public String getColumn() {
    return column;
  }

  public boolean isPrimitive() {
    return primitive;
  }
}

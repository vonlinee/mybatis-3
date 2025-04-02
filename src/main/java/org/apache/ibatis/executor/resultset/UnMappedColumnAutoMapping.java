package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.type.TypeHandler;

class UnMappedColumnAutoMapping {
  public final String column;
  public final String property;
  public final TypeHandler<?> typeHandler;
  public final boolean primitive;

  public UnMappedColumnAutoMapping(String column, String property, TypeHandler<?> typeHandler, boolean primitive) {
    this.column = column;
    this.property = property;
    this.typeHandler = typeHandler;
    this.primitive = primitive;
  }
}

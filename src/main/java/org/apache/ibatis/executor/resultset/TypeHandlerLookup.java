package org.apache.ibatis.executor.resultset;

import java.lang.reflect.Type;

import org.apache.ibatis.type.TypeHandler;

public interface TypeHandlerLookup {

  /**
   * Gets the type handler to use when reading the result set. Tries to get from the TypeHandlerRegistry by searching
   * for the property type. If not found it gets the column JDBC type and tries to get a handler for it.
   *
   * @param rsw
   *          the result set wrapper
   * @param column
   *          the column name
   * @param propertyType
   *          the column mapped java type
   *
   * @return the type handler
   */
  TypeHandler<?> lookupTypeHandler(ResultSetWrapper rsw, String column, Type propertyType);
}

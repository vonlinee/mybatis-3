package org.apache.ibatis.executor.resultset;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

public class DefaultTypeHandlerLookup implements TypeHandlerLookup {

  private final TypeHandlerRegistry typeHandlerRegistry;

  private final Map<String, Map<Type, TypeHandler<?>>> typeHandlerMap = new HashMap<>();

  public DefaultTypeHandlerLookup(TypeHandlerRegistry typeHandlerRegistry) {
    this.typeHandlerRegistry = typeHandlerRegistry;
  }

  @Override
  public TypeHandler<?> lookupTypeHandler(ResultSetWrapper rsw, String columnName, Type propertyType) {
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

      Class<?> javaType = rsw.getColumnClass(index);
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

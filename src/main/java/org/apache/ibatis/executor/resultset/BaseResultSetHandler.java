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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.ibatis.binding.ParamMap;
import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Pagination;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class BaseResultSetHandler implements ResultSetHandler {

  Configuration configuration;
  TypeHandlerLookup typeHandlerLookup;
  TypeHandlerRegistry typeHandlerRegistry;
  ObjectFactory objectFactory;
  ReflectorFactory reflectorFactory;

  @Override
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();
    this.reflectorFactory = configuration.getReflectorFactory();
    this.typeHandlerLookup = new DefaultTypeHandlerLookup(configuration.getTypeHandlerRegistry());
  }

  @Override
  public TypeHandler<?> getTypeHandler(@NotNull ResultSetWrapper rsw, @Nullable Type propertyType,
      @NotNull String columnName) {
    return typeHandlerLookup.lookupTypeHandler(rsw, columnName, propertyType);
  }

  protected ResultSetWrapper getFirstResultSet(Statement stmt) throws SQLException {
    ResultSet rs = null;
    SQLException e1 = null;

    try {
      rs = stmt.getResultSet();
    } catch (SQLException e) {
      // Oracle throws ORA-17283 for implicit cursor
      e1 = e;
    }

    try {
      while (rs == null) {
        // move forward to get the first result set in case the driver
        // doesn't return the result set as the first result (HSQLDB)
        if (stmt.getMoreResults()) {
          rs = stmt.getResultSet();
        } else if (stmt.getUpdateCount() == -1) {
          // no more results. Must be no result set
          break;
        }
      }
    } catch (SQLException e) {
      throw e1 != null ? e1 : e;
    }

    return rs != null ? new ResultSetWrapper(rs, configuration.isUseColumnLabel()) : null;
  }

  protected ResultSetWrapper getNextResultSet(Statement stmt) {
    // Making this method tolerant of bad JDBC drivers
    try {
      // We stopped checking DatabaseMetaData#supportsMultipleResultSets()
      // because Oracle driver (incorrectly) returns false

      // Crazy Standard JDBC way of determining if there are more results
      // DO NOT try to 'improve' the condition even if IDE tells you to!
      // It's important that getUpdateCount() is called here.
      if (!(!stmt.getMoreResults() && stmt.getUpdateCount() == -1)) {
        ResultSet rs = stmt.getResultSet();
        if (rs == null) {
          return getNextResultSet(stmt);
        } else {
          return new ResultSetWrapper(rs, configuration.isUseColumnLabel());
        }
      }
    } catch (Exception e) {
      // Intentionally ignored.
    }
    return null;
  }

  protected boolean hasTypeHandlerForResultObject(ResultSetWrapper rsw, Class<?> resultType) {
    if (rsw.getColumnNames().size() == 1) {
      return typeHandlerRegistry.hasTypeHandler(resultType, rsw.getJdbcType(rsw.getColumnNames().get(0)));
    }
    return typeHandlerRegistry.hasTypeHandler(resultType);
  }

  protected void skipRows(ResultSet rs, RowBounds rowBounds) throws SQLException {
    if (rowBounds instanceof Pagination) {
      // TODO find a better way to implement this
      return;
    }

    if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
      if (rowBounds.getOffset() != RowBounds.NO_ROW_OFFSET) {
        rs.absolute(rowBounds.getOffset());
      }
    } else {
      for (int i = 0; i < rowBounds.getOffset(); i++) {
        if (!rs.next()) {
          break;
        }
      }
    }
  }

  protected Object instantiateParameterObject(Class<?> parameterType) {
    if (parameterType == null) {
      return new HashMap<>();
    }
    if (ParamMap.class.equals(parameterType)) {
      return new HashMap<>(); // issue #649
    } else {
      return objectFactory.create(parameterType);
    }
  }

  protected Object getDiscriminatorValue(ResultSetWrapper rsw, Discriminator discriminator, String columnPrefix)
      throws SQLException {
    final ResultMapping resultMapping = discriminator.getResultMapping();
    String column = prependPrefix(resultMapping.getColumn(), columnPrefix);
    TypeHandler<?> typeHandler = resultMapping.getTypeHandler();
    if (typeHandler == null) {
      typeHandler = typeHandlerRegistry.getTypeHandler(resultMapping.getJavaType(), rsw.getJdbcType(column));
    }
    if (typeHandler == null) {
      return null;
    }
    return typeHandler.getResult(rsw.getResultSet(), column);
  }

  protected String prependPrefix(String columnName, String prefix) {
    if (columnName == null || columnName.isEmpty() || prefix == null || prefix.isEmpty()) {
      return columnName;
    }
    return prefix + columnName;
  }
}

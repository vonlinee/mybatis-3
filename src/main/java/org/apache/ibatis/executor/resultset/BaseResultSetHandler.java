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
import java.sql.Statement;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.internal.util.JdbcUtils;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.jetbrains.annotations.Nullable;

public abstract class BaseResultSetHandler implements ResultSetHandler {

  protected final Configuration configuration;
  protected final TypeHandlerRegistry typeHandlerRegistry;
  protected final ObjectFactory objectFactory;
  protected final ReflectorFactory reflectorFactory;

  protected BaseResultSetHandler(Configuration configuration) {
    this.configuration = configuration;
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();
    this.reflectorFactory = configuration.getReflectorFactory();
  }

  protected final void skipRows(ResultSet rs, RowBounds rowBounds) throws SQLException {
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

  protected final ResultSetWrapper getFirstResultSet(Statement stmt) throws SQLException {
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
        // move forward to get the first resultSet in case the driver
        // doesn't return the resultSet as the first result (HSQLDB)
        if (stmt.getMoreResults()) {
          rs = stmt.getResultSet();
        } else if (stmt.getUpdateCount() == -1) {
          // no more results. Must be no resultSet
          break;
        }
      }
    } catch (SQLException e) {
      throw e1 != null ? e1 : e;
    }

    return rs != null ? new ResultSetWrapper(rs, configuration) : null;
  }

  @Nullable
  protected final ResultSetWrapper getNextResultSet(Statement stmt) {
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
          return new ResultSetWrapper(rs, configuration);
        }
      }
    } catch (Exception e) {
      // Intentionally ignored.
    }
    return null;
  }

  protected Object instantiateCollectionPropertyIfAppropriate(ResultMapping resultMapping, MetaObject metaObject) {
    final String propertyName = resultMapping.getProperty();
    Object propertyValue = metaObject.getValue(propertyName);
    if (propertyValue == null) {
      Class<?> type = resultMapping.getJavaType();
      if (type == null) {
        type = metaObject.getSetterType(propertyName);
      }
      try {
        if (objectFactory.isCollection(type)) {
          propertyValue = objectFactory.create(type);
          metaObject.setValue(propertyName, propertyValue);
          return propertyValue;
        }
      } catch (Exception e) {
        throw new ExecutorException(
            "Error instantiating collection property for result '" + resultMapping.getProperty() + "'.  Cause: " + e,
            e);
      }
    } else if (objectFactory.isCollection(propertyValue.getClass())) {
      return propertyValue;
    }
    return null;
  }

  protected boolean hasTypeHandlerForResultObject(ResultSetWrapper rsw, Class<?> resultType) {
    if (rsw.getColumnNames().size() == 1) {
      return typeHandlerRegistry.hasTypeHandler(resultType, rsw.getJdbcType(rsw.getColumnNames().get(0)));
    }
    return typeHandlerRegistry.hasTypeHandler(resultType);
  }

  @Override
  public void closeResultSet(ResultSet rs) {
    JdbcUtils.closeSilently(rs);
  }
}

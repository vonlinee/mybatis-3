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
package org.apache.ibatis.executor.result;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.executor.statement.JdbcUtils;
import org.apache.ibatis.jdbc.RuntimeSqlException;
import org.apache.ibatis.reflection.property.BeanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link RowMapper} implementation that converts a row into a new instance of the specified java bean class. The mapped
 * target class must be a top-level class or {@code static} nested class, and it must have a default or no-arg
 * constructor.
 * <p>
 * Column values are mapped based on matching the column name (as obtained from result set meta-data) to public setters
 * in the target class for the corresponding properties. The names are matched either directly or by transforming a name
 * separating the parts with underscores to the same name using "camel" case.
 * <p>
 * Mapping is provided for properties in the target class for many common types &mdash; for example: String, boolean,
 * Boolean, byte, Byte, short, Short, int, Integer, long, Long, float, Float, double, Double, BigDecimal,
 * {@code java.util.Date}, etc.
 * <p>
 *
 * @param <T>
 *          the result type
 */
public class BeanClassRowMapper<T> extends ComplexClassRowMapper<T> {

  /**
   * Map of the properties we provide mapping for.
   */
  @Nullable
  private Map<String, PropertyDescriptor> mappedProperties;

  /**
   * Create a new {@code BeanPropertyRowMapper}, accepting unpopulated properties in the target bean.
   *
   * @param beanClass
   *          the class that each row should be mapped to
   */
  public BeanClassRowMapper(Class<T> beanClass) {
    setTargetType(beanClass);
    initialize(beanClass);
  }

  /**
   * Initialize the mapping meta-data for the given class.
   *
   * @param targetClass
   *          the mapped class
   */
  @Override
  protected void initialize(Class<T> targetClass) {
    this.mappedProperties = new HashMap<>();
    try {
      for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(targetClass)) {
        if (pd.getWriteMethod() != null) {
          this.mappedProperties.put(pd.getName(), pd);
        }
      }
    } catch (IntrospectionException e) {
      throw new RuntimeSqlException(e);
    }
  }

  @Override
  protected T populate(@NotNull T row, @NotNull ResultSet rs, @NotNull ResultSetMetaData metaData, int rowNum,
      String column, String property, int columnCount, int index) throws SQLException {
    PropertyDescriptor pd = (this.mappedProperties != null ? this.mappedProperties.get(property) : null);
    if (pd != null) {
      Object value = null;
      try {
        value = getColumnValue(rs, column, index, pd);
        if (value == null && !isCallSetterOnNull()) {
          return row;
        }
        try {
          pd.getWriteMethod().invoke(row, value);
        } catch (Throwable ex) {
          if (value == null && isPrimitivesDefaultedForNullValue()) {
            reportTypeMismatch(row, column, pd.getName(), pd.getPropertyType(), ex);
          } else {
            throw ex;
          }
        }
      } catch (Throwable ex) {
        throw new SQLException("Unable to map value [" + value + "] from column '" + column + "' to property '"
            + property + "' [" + pd.getPropertyType() + "]", ex.getCause());
      }
    }
    return row;
  }

  /**
   * Retrieve a JDBC object value for the specified column.
   * <p>
   * The default implementation calls {@link JdbcUtils#getResultSetValue(ResultSet, int, Class)} using the type of the
   * specified {@link PropertyDescriptor}.
   * <p>
   * Subclasses may override this to check specific value types upfront, or to post-process values returned from
   * {@code getResultSetValue}.
   *
   * @param rs
   *          is the ResultSet holding the data
   * @param index
   *          is the column index
   * @param pd
   *          the bean property that each result object is expected to match
   *
   * @return the Object value
   *
   * @throws SQLException
   *           in case of extraction failure
   */
  @Nullable
  protected Object getColumnValue(ResultSet rs, String column, int index, PropertyDescriptor pd) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
  }

  /**
   * Retrieve a JDBC object value for the specified column.
   * <p>
   * The default implementation calls {@link JdbcUtils#getResultSetValue(ResultSet, int, Class)}.
   * <p>
   * Subclasses may override this to check specific value types upfront, or to post-process values returned from
   * {@code getResultSetValue}.
   *
   * @param rs
   *          is the ResultSet holding the data
   * @param column
   *          is the column name
   * @param index
   *          is the column index
   * @param paramType
   *          the target parameter type
   *
   * @return the Object value
   *
   * @throws SQLException
   *           in case of extraction failure
   */
  @Nullable
  protected Object getColumnValue(ResultSet rs, String column, int index, Class<?> paramType) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index, paramType);
  }
}

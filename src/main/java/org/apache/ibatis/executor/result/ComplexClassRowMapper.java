package org.apache.ibatis.executor.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

import org.apache.ibatis.executor.statement.JdbcUtils;
import org.apache.ibatis.internal.util.ReflectionUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param <T>
 *
 * @see org.apache.ibatis.internal.util.TypeUtils#isSimpleType(Class)
 */
public abstract class ComplexClassRowMapper<T> implements RowMapper<T> {

  /**
   * The target type
   */
  @Nullable
  protected Class<T> targetType;

  /**
   * Whether {@code NULL} database values should be ignored for primitive properties in the target class.
   *
   * @see #setPrimitivesDefaultedForNullValue(boolean)
   */
  private boolean primitivesDefaultedForNullValue = false;

  /**
   * whether call setter when the value is null
   */
  private boolean callSetterOnNull = false;

  public void setTargetType(@Nullable Class<T> targetType) {
    this.targetType = targetType;
  }

  /**
   * Get the class that we are mapping to.
   */
  @Nullable
  public final Class<T> getTargetType() {
    return this.targetType;
  }

  /**
   * Set whether a {@code NULL} database column value should be ignored when mapping to a corresponding primitive
   * property in the target class.
   * <p>
   * Default is {@code false}, throwing an exception when nulls are mapped to Java primitives.
   * <p>
   * If this flag is set to {@code true} and you use an <em>ignored</em> primitive property value from the mapped bean
   * to update the database, the value in the database will be changed from {@code NULL} to the current value of that
   * primitive property. That value may be the property's initial value (potentially Java's default value for the
   * respective primitive type), or it may be some other value set for the property in the default constructor (or
   * initialization block) or as a side effect of setting some other property in the mapped bean.
   */
  public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
    this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
  }

  /**
   * Get the value of the {@code primitivesDefaultedForNullValue} flag.
   *
   * @see #setPrimitivesDefaultedForNullValue(boolean)
   */
  public boolean isPrimitivesDefaultedForNullValue() {
    return this.primitivesDefaultedForNullValue;
  }

  public boolean isCallSetterOnNull() {
    return callSetterOnNull;
  }

  public void setCallSetterOnNull(boolean callSetterOnNull) {
    this.callSetterOnNull = callSetterOnNull;
  }

  protected void initialize(Class<T> targetClass) {
  }

  /**
   * Extract the values for all columns in the current row.
   * <p>
   * Utilizes public setters and result set meta-data.
   *
   * @see java.sql.ResultSetMetaData
   */
  @Nullable
  @Override
  public T mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
    final ResultSetMetaData metadata = rs.getMetaData();
    final int columnCount = metadata.getColumnCount();
    T row;
    try {
      row = createRowObject(rs, metadata);
    } catch (Throwable e) {
      throw new SQLException(e);
    }
    for (int index = 1; index <= columnCount; index++) {
      String column = getColumnName(metadata, index);
      String property = getProperty(column, index);
      row = populate(row, rs, metadata, rowNum, column, property, columnCount, index);
    }
    return row;
  }

  /**
   * extract the column name by column index
   *
   * @param metadata
   *          the metadata
   * @param columnIndex
   *          the columnIndex
   *
   * @return the converted name
   */
  protected String getColumnName(ResultSetMetaData metadata, int columnIndex) throws SQLException {
    return JdbcUtils.findColumnName(metadata, columnIndex);
  }

  /**
   * Convert the given column name in ResultSet to java property of class.
   * <p>
   * By default, conversions will happen within the US locale.
   *
   * @param columnName
   *          the column name
   * @param columnIndex
   *          the columnIndex
   *
   * @return the converted java property
   */
  protected String getProperty(String columnName, int columnIndex) {
    return StringUtils.underscoreToCamel(columnName);
  }

  /**
   * @param row
   *          row object
   * @param rowNum
   *          current row num
   * @param column
   *          column name
   * @param property
   *          the mapped property name of the column
   * @param columnCount
   *          total column count
   * @param columnIndex
   *          column index
   *
   * @return the populated object
   */
  protected abstract T populate(@NotNull T row, @NotNull ResultSet rs, @NotNull ResultSetMetaData metadata,
      final int rowNum, final String column, final String property, final int columnCount, final int columnIndex)
      throws SQLException;

  /**
   * Construct an instance of the mapped class for the current row.
   *
   * @param rs
   *          the ResultSet to map (pre-initialized for the current row)
   *
   * @return a corresponding instance of the mapped class
   */
  protected T createRowObject(ResultSet rs, ResultSetMetaData metadata) throws Throwable {
    Objects.requireNonNull(this.targetType, "Mapped class was not specified");
    return ReflectionUtils.instantiateClass(this.targetType);
  }

  /**
   * Convert the given name to lower case.
   * <p>
   * By default, conversions will happen within the US locale.
   *
   * @param name
   *          the original name
   *
   * @return the converted name
   */
  protected String lowerCaseName(String name) {
    return name.toLowerCase(Locale.US);
  }

  /**
   * Convert a name in camelCase to an underscored name in lower case.
   * <p>
   * Any upper case letters are converted to lower case with a preceding underscore.
   *
   * @param name
   *          the original name
   *
   * @return the converted name
   */
  protected String underscoreName(String name) {
    return StringUtils.camelToUnderscore(name);
  }

  /**
   * @param rowObject
   *          row
   * @param column
   *          column name
   * @param property
   *          property name
   * @param propertyType
   *          property type
   * @param ex
   *          error
   *
   * @see ComplexClassRowMapper#isCallSetterOnNull()
   */
  protected void reportTypeMismatch(Object rowObject, String column, String property, Class<?> propertyType,
      Throwable ex) {
    // NO-OP
  }
}

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
package org.apache.ibatis.mapping;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.Objects;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 */
public class ParameterMapping implements Comparator<ParameterMapping>, Serializable {

  private static final Object UNSET = new Object();

  private String property;
  private ParameterMode mode;
  private Class<?> javaType = Object.class;
  private JdbcType jdbcType;

  @Nullable
  private Integer numericScale;
  private TypeHandler<?> typeHandler;

  /**
   * name of type handler type
   */
  private String javaTypeName;

  /**
   * name of type handler type
   */
  private String typeHandlerName;
  private String resultMapId;
  private String jdbcTypeName;
  private String expression;
  private Object value = UNSET;

  public ParameterMapping() {
  }

  @Override
  public int compare(ParameterMapping o1, ParameterMapping o2) {
    if (o1.getProperty() == null) {
      return -1;
    } else if (o2.getProperty() == null) {
      return 0;
    }
    return o1.getProperty().compareTo(o2.getProperty());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ParameterMapping))
      return false;
    ParameterMapping that = (ParameterMapping) o;
    return Objects.equals(property, that.property) && mode == that.mode && Objects.equals(javaType, that.javaType)
        && jdbcType == that.jdbcType && Objects.equals(numericScale, that.numericScale)
        && Objects.equals(typeHandler, that.typeHandler) && Objects.equals(javaTypeName, that.javaTypeName)
        && Objects.equals(typeHandlerName, that.typeHandlerName) && Objects.equals(resultMapId, that.resultMapId)
        && Objects.equals(jdbcTypeName, that.jdbcTypeName) && Objects.equals(expression, that.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(property, mode, javaType, jdbcType, numericScale, typeHandler, javaTypeName, typeHandlerName,
        resultMapId, jdbcTypeName, expression);
  }

  public static class Builder {
    private final ParameterMapping parameterMapping = new ParameterMapping();

    public Builder(String property, TypeHandler<?> typeHandler) {
      parameterMapping.property = property;
      parameterMapping.typeHandler = typeHandler;
      parameterMapping.mode = ParameterMode.IN;
    }

    public Builder(String property, Class<?> javaType) {
      parameterMapping.property = property;
      parameterMapping.javaType = javaType;
      parameterMapping.mode = ParameterMode.IN;
    }

    public Builder mode(ParameterMode mode) {
      parameterMapping.mode = mode;
      return this;
    }

    public Builder javaType(Class<?> javaType) {
      parameterMapping.javaType = javaType;
      return this;
    }

    public Builder javaType(String javaType) {
      parameterMapping.javaTypeName = javaType;
      return this;
    }

    public Builder jdbcType(JdbcType jdbcType) {
      parameterMapping.jdbcType = jdbcType;
      return this;
    }

    public Builder numericScale(Integer numericScale) {
      parameterMapping.numericScale = numericScale;
      return this;
    }

    public Builder resultMapId(String resultMapId) {
      parameterMapping.resultMapId = resultMapId;
      return this;
    }

    public Builder typeHandler(TypeHandler<?> typeHandler) {
      parameterMapping.typeHandler = typeHandler;
      return this;
    }

    public Builder typeHandler(String typeHandler) {
      parameterMapping.typeHandlerName = typeHandler;
      return this;
    }

    public Builder jdbcTypeName(String jdbcTypeName) {
      parameterMapping.jdbcTypeName = jdbcTypeName;
      return this;
    }

    public Builder expression(String expression) {
      parameterMapping.expression = expression;
      return this;
    }

    public Builder value(Object value) {
      parameterMapping.value = value;
      return this;
    }

    public ParameterMapping build() {
      validate();
      return parameterMapping;
    }

    private void validate() {
      if (ResultSet.class.equals(parameterMapping.javaType) && parameterMapping.resultMapId == null) {
        throw new IllegalStateException("Missing result map in property '" + parameterMapping.property + "'.  "
            + "Parameters of type java.sql.ResultSet require a result map.");
      }
    }
  }

  public String getProperty() {
    return property;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the mode
   */
  public ParameterMode getMode() {
    return mode;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the java type
   */
  public Class<?> getJavaType() {
    return javaType;
  }

  /**
   * Used in the UnknownTypeHandler in case there is no handler for the property type.
   *
   * @return the jdbc type
   */
  public JdbcType getJdbcType() {
    return jdbcType;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the numeric scale
   */
  public Integer getNumericScale() {
    return numericScale;
  }

  /**
   * Used when setting parameters to the PreparedStatement.
   *
   * @return the type handler
   */
  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the result map id
   */
  public String getResultMapId() {
    return resultMapId;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the jdbc type name
   */
  public String getJdbcTypeName() {
    return jdbcTypeName;
  }

  /**
   * Expression 'Not used'.
   *
   * @return the expression
   */
  public String getExpression() {
    return expression;
  }

  public Object getValue() {
    return value;
  }

  public boolean hasValue() {
    return value != UNSET;
  }

  @Override
  public String toString() {
    return "ParameterMapping{" + "property='" + property + '\'' + ", mode=" + mode + ", javaType=" + javaType
        + ", jdbcType=" + jdbcType + ", numericScale=" + numericScale + ", resultMapId='" + resultMapId + '\''
        + ", jdbcTypeName='" + jdbcTypeName + '\'' + ", expression='" + expression + '\'' + ", value='" + value + '\''
        + '}';
  }

  public void setTypeHandler(TypeHandler<?> typeHandler) {
    this.typeHandler = typeHandler;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public void setNumericScale(@Nullable Integer numericScale) {
    this.numericScale = numericScale;
  }

  public void setMode(ParameterMode mode) {
    this.mode = mode;
  }

  public void setJdbcTypeName(String jdbcTypeName) {
    this.jdbcTypeName = jdbcTypeName;
  }

  public void setJdbcType(JdbcType jdbcType) {
    this.jdbcType = jdbcType;
  }

  public void setJavaType(Class<?> javaType) {
    this.javaType = javaType;
  }

  public String getJavaTypeName() {
    return javaTypeName;
  }

  public String getTypeHandlerName() {
    return typeHandlerName;
  }
}

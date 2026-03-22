/*
 *    Copyright 2009-2026 the original author or authors.
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
package org.apache.ibatis.extension;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;

import javax.persistence.Transient;

import org.apache.ibatis.type.SimpleTypeRegistry;

/**
 * Base class containing shared utilities for reflection and type mapping.
 */
public abstract class AbstractTableInfoParser implements TableInfoParser {

  /**
   * Resolves the {@link Types} integer code for a Java class.
   *
   * @param javaType
   *          the java class
   *
   * @return the java.sql.Types constant
   */
  protected int resolveSqlType(Class<?> javaType) {
    if (javaType == String.class)
      return Types.VARCHAR;
    if (javaType == Integer.class || javaType == int.class)
      return Types.INTEGER;
    if (javaType == Long.class || javaType == long.class)
      return Types.BIGINT;
    if (javaType == Boolean.class || javaType == boolean.class)
      return Types.BOOLEAN;
    if (javaType == Double.class || javaType == double.class)
      return Types.DOUBLE;
    if (javaType == Float.class || javaType == float.class)
      return Types.FLOAT;
    if (javaType == BigDecimal.class)
      return Types.DECIMAL;
    if (javaType == Date.class || javaType == java.sql.Date.class)
      return Types.DATE;
    if (javaType == LocalDateTime.class || javaType == Timestamp.class)
      return Types.TIMESTAMP;
    if (javaType == LocalDate.class)
      return Types.DATE;
    if (javaType == byte[].class)
      return Types.BLOB;
    return Types.OTHER;
  }

  /**
   * Resolves the SQL type name (e.g., "VARCHAR", "INTEGER").
   *
   * @param javaType
   *          the java class
   *
   * @return the string representation of the SQL type
   */
  protected String resolveSqlTypeName(Class<?> javaType) {
    int type = resolveSqlType(javaType);
    try {
      return JDBCType.valueOf(type).getName();
    } catch (Exception e) {
      return JDBCType.OTHER.getName();
    }
  }

  /**
   * Determines if a field should be skipped (static or transient keyword).
   */
  protected boolean isIgnoredField(Field field) {
    if (field.isAnnotationPresent(Transient.class)) {
      return true;
    }
    int modifiers = field.getModifiers();
    return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || isComplexType(field.getType())
        || isCollectionType(field.getType());
  }

  protected boolean isCollectionType(Class<?> type) {
    return Collection.class.isAssignableFrom(type);
  }

  protected boolean isComplexType(Class<?> type) {
    return !isScalarType(type);
  }

  static boolean isScalarType(Class<?> clazz) {
    return SimpleTypeRegistry.isSimpleType(clazz) || clazz.isEnum() || clazz.isPrimitive() || clazz == LocalDate.class
        || clazz == Timestamp.class || clazz == Time.class || clazz == LocalDateTime.class;
  }
}

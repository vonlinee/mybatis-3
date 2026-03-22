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

import org.apache.ibatis.extension.metadata.TableInfo;
import org.apache.ibatis.mapping.NamingStrategy;

/**
 * Strategy interface for parsing a Java class into database metadata.
 */
public interface TableInfoParser {

  /**
   * Parses the given class to extract table and column metadata.
   *
   * @param clazz
   *          the entity or POJO class to parse
   *
   * @return the populated TableInfo object
   */
  TableInfo parse(Class<?> clazz);

  /**
   * Get table name
   *
   * @param clazz
   *          the entity or POJO class to parse
   *
   * @return the table name
   */
  default String getTableName(Class<?> clazz) {
    return NamingStrategy.toSnakeCase(clazz.getSimpleName());
  }

  /**
   * Get column name
   *
   * @param field
   *          the field to parse
   *
   * @return the column name
   */
  default String getColumnName(Field field) {
    return NamingStrategy.toSnakeCase(field.getName());
  }
}

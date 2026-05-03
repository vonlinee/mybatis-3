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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.ibatis.type.SimpleTypeRegistry;
import org.jetbrains.annotations.Nullable;

public class SqlValueFormatterRegistry implements SqlValueFormatter {

  private final Map<Class<?>, SqlValueFormatter> formatterMap = new HashMap<>();

  static SqlValueFormatter simpleSqlValueFormatter = value -> {
    if (value == null) {
      return "NULL";
    }
    if (value instanceof CharSequence) {
      return "'" + value + "'";
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    throw new IllegalArgumentException("Unsupported value type: " + value.getClass().getName());
  };

  public SqlValueFormatterRegistry() {
    for (Class<?> simpleType : SimpleTypeRegistry.getSimpleTypes()) {
      register(simpleType, simpleSqlValueFormatter);
    }
  }

  /**
   * Registers a formatter for a specific class or interface.
   */
  public void register(Class<?> type, SqlValueFormatter formatter) {
    formatterMap.put(type, Objects.requireNonNull(formatter, "formatter is null"));
  }

  @Override
  public String format(@Nullable Object value) {
    // 1. Handle Nulls immediately
    if (value == null) {
      return "NULL";
    }
    Class<?> targetType = value.getClass();
    // 2. Find the appropriate formatter
    SqlValueFormatter formatter = resolveFormatter(targetType);
    if (formatter == null) {
      throw new IllegalArgumentException("No SQL formatter registered for type: " + targetType.getName());
    }
    // 3. Delegate the formatting
    return formatter.format(value);
  }

  /**
   * Resolves the formatter, handling class hierarchies (e.g., Number.class handling Integer.class)
   */
  private SqlValueFormatter resolveFormatter(Class<?> targetType) {
    // Fast path: Exact match already exists in the map
    if (formatterMap.containsKey(targetType)) {
      return formatterMap.get(targetType);
    }
    // Slow path: Check if any registered type is a superclass/interface of the target type
    for (Map.Entry<Class<?>, SqlValueFormatter> entry : formatterMap.entrySet()) {
      if (entry.getKey().isAssignableFrom(targetType)) {
        SqlValueFormatter foundFormatter = entry.getValue();
        // Cache the resolved exact type for future O(1) fast-path lookups
        formatterMap.put(targetType, foundFormatter);
        return foundFormatter;
      }
    }
    return null; // No matching formatter found
  }
}

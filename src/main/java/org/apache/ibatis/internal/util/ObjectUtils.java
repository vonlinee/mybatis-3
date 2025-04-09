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
package org.apache.ibatis.internal.util;

import java.util.regex.Pattern;

public final class ObjectUtils {

  public static String nullIfEmpty(String value) {
    return value == null || value.isEmpty() ? null : value;
  }

  public static String nullIfBlank(String value) {
    return value == null || value.trim().isEmpty() ? null : value;
  }

  public static Pattern parseExpression(String regex, String defaultValue) {
    return Pattern.compile(regex == null ? defaultValue : regex);
  }

  public static Boolean parseBoolean(String value, Boolean defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    return "true".equalsIgnoreCase(value) ? Boolean.TRUE : Boolean.FALSE;
  }

  public static Integer parseInteger(String value, Integer defaultValue) {
    return value == null ? defaultValue : Integer.valueOf(value);
  }

  public static boolean isEmpty(Object[] objects) {
    return objects == null || objects.length == 0;
  }
}

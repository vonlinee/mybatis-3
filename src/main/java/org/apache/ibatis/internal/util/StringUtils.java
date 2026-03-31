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
package org.apache.ibatis.internal.util;

import org.jetbrains.annotations.Nullable;

public final class StringUtils {

  private StringUtils() {
    // Prevent Instantiation of Static Class
  }

  public static boolean isEmpty(@Nullable String str) {
    return str == null || str.isEmpty();
  }

  public static boolean isNotEmpty(@Nullable String str) {
    return !isEmpty(str);
  }

  public static boolean isAllEmpty(@Nullable String... strings) {
    if (strings == null) {
      return true;
    }
    for (String str : strings) {
      if (isNotEmpty(str)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isAnyEmpty(@Nullable String... strings) {
    if (strings == null) {
      return true;
    }
    for (String str : strings) {
      if (isEmpty(str)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isBlank(@Nullable String str) {
    final int strLen;
    if (str == null || (strLen = str.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if ((!Character.isWhitespace(str.charAt(i)))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNotBlank(@Nullable String str) {
    return !isBlank(str);
  }

  public static boolean isAllBlank(@Nullable String... strings) {
    if (strings == null) {
      return true;
    }
    for (String str : strings) {
      if (isNotBlank(str)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isAnyBlank(@Nullable String... strings) {
    if (strings == null) {
      return true;
    }
    for (String str : strings) {
      if (isBlank(str)) {
        return true;
      }
    }
    return false;
  }
}

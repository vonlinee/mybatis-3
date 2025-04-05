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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StringUtils {

  public static final String EMPTY = "";
  public static final String[] EMPTY_STRING_ARRAY = new String[0];

  private StringUtils() {
    throw new UnsupportedOperationException("utility class cannot be instantiated.");
  }

  /**
   * Checks if a CharSequence is empty (""), null or whitespace only.
   * <p>
   * Whitespace is defined by {@link Character#isWhitespace(char)}.
   * </p>
   *
   * <pre>
   * StringUtils.isBlank(null)      = true
   * StringUtils.isBlank("")        = true
   * StringUtils.isBlank(" ")       = true
   * StringUtils.isBlank("bob")     = false
   * StringUtils.isBlank("  bob  ") = false
   * </pre>
   *
   * @param cs
   *          the CharSequence to check, may be null
   *
   * @return {@code true} if the CharSequence is null, empty or whitespace only
   */
  public static boolean isBlank(final CharSequence cs) {
    if (cs == null || cs.isEmpty()) {
      return true;
    }
    final int strLen = cs.length();
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean hasText(String str) {
    return !isBlank(str);
  }

  /**
   * Checks if a CharSequence is empty ("") or null.
   *
   * <pre>
   * StringUtils.isEmpty(null)      = true
   * StringUtils.isEmpty("")        = true
   * StringUtils.isEmpty(" ")       = false
   * StringUtils.isEmpty("bob")     = false
   * StringUtils.isEmpty("  bob  ") = false
   * </pre>
   * <p>
   * NOTE: This method changed in Lang version 2.0. It no longer trims the CharSequence. That functionality is available
   * in isBlank().
   * </p>
   *
   * @param cs
   *          the CharSequence to check, may be null
   *
   * @return {@code true} if the CharSequence is empty or null
   */
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.isEmpty();
  }

  public static boolean isAllEmpty(final String str1, final String str2) {
    return isEmpty(str1) && isEmpty(str2);
  }

  @NotNull
  public static String prepend(String str, String prefix) {
    if (str == null || str.isEmpty() || prefix == null || prefix.isEmpty()) {
      return EMPTY;
    }
    return prefix + prefix;
  }

  public static boolean containsAny(String cs, char... chars) {
    if (cs == null) {
      return false;
    }
    for (char aChar : chars) {
      if (cs.indexOf(aChar) >= 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Case in-sensitive find of the first index within a CharSequence from the specified position.
   * <p>
   * A {@code null} CharSequence will return {@code -1}. A negative start position is treated as zero. An empty ("")
   * search CharSequence always matches. A start position greater than the string length only matches an empty search
   * CharSequence.
   * </p>
   *
   * @param str
   *          the CharSequence to check, may be null
   * @param searchStr
   *          the CharSequences to find, may be null
   *
   * @return the first index of the search CharSequence (always &ge; startPos), -1 if no match or {@code null} string
   *         input
   *
   * @see StringUtils#indexOfIgnoreCase(String, String, int)
   */
  public static int indexOfIgnoreCase(final String str, final String searchStr) {
    return indexOfIgnoreCase(str, searchStr, 0);
  }

  /**
   * Case in-sensitive find of the first index within a CharSequence from the specified position.
   * <p>
   * A {@code null} CharSequence will return {@code -1}. A negative start position is treated as zero. An empty ("")
   * search CharSequence always matches. A start position greater than the string length only matches an empty search
   * CharSequence.
   * </p>
   *
   * @param str
   *          the CharSequence to check, may be null
   * @param searchStr
   *          the CharSequences to find, may be null
   * @param startPos
   *          the start position, negative treated as zero
   *
   * @return the first index of the search CharSequence (always &ge; startPos), -1 if no match or {@code null} string
   *         input
   */
  public static int indexOfIgnoreCase(final String str, final String searchStr, int startPos) {
    if (str == null || searchStr == null) {
      return -1;
    }
    if (startPos < 0) {
      startPos = 0;
    }
    final int endLimit = str.length() - searchStr.length() + 1;
    if (startPos > endLimit) {
      return -1;
    }
    if (searchStr.isEmpty()) {
      return startPos;
    }
    for (int i = startPos; i < endLimit; i++) {
      if (str.regionMatches(true, i, searchStr, 0, searchStr.length())) {
        return i;
      }
    }
    return -1;
  }

  @NotNull
  public static String[] splitToArray(String in) {
    if (isBlank(in)) {
      return EMPTY_STRING_ARRAY;
    }
    return in.split(",");
  }

  @NotNull
  public static String deleteFirst(String str, String target, boolean ignoreCase) {
    if (isEmpty(str) || isEmpty(target)) {
      return EMPTY;
    }
    int index = ignoreCase ? indexOfIgnoreCase(str, target) : str.indexOf(target);
    if (index > 0) {
      return deleteRange(str, index, index + target.length());
    }
    return str;
  }

  @NotNull
  public static String deleteRange(String str, int start, int end) {
    if (isEmpty(str)) {
      return EMPTY;
    }
    if (start < 0 || end > str.length() - 1) {
      throw new IllegalArgumentException(
          String.format("invalid range [%s, %s] to delete given string (%s)", start, end, start));
    }
    return str.substring(0, start) + str.substring(end, str.length() - 1);
  }

  @Nullable
  public static String[] delimitedStringToArray(@Nullable String in) {
    if (in == null || in.trim().isEmpty()) {
      return null;
    }
    return in.split(",");
  }

  public static boolean equalsAny(String str, String str1, String str2, String str3) {
    return Objects.equals(str, str1) || Objects.equals(str, str2) || Objects.equals(str, str3);
  }

  public static Set<String> splitToSet(String value, String defaultValue) {
    value = value == null ? defaultValue : value;
    return new HashSet<>(Arrays.asList(value.split(",")));
  }

  public static int skipUntil(String str, int p, final String endChars) {
    for (int i = p; i < str.length(); i++) {
      char c = str.charAt(i);
      if (endChars.indexOf(c) > -1) {
        return i;
      }
    }
    return str.length();
  }

  public static String trimmedStr(String str, int start, int end) {
    while (str.charAt(start) <= 0x20) {
      start++;
    }
    while (str.charAt(end - 1) <= 0x20) {
      end--;
    }
    return start >= end ? "" : str.substring(start, end);
  }

  public static int skipWS(String expression, int p) {
    for (int i = p; i < expression.length(); i++) {
      if (expression.charAt(i) > 0x20) {
        return i;
      }
    }
    return expression.length();
  }
}

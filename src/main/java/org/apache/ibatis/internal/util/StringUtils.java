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
   *
   * @since 2.0
   * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
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
   *
   * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
   */
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.isEmpty();
  }

  public static String prepend(String str, String prefix) {
    if (str == null || str.isEmpty() || prefix == null || prefix.isEmpty()) {
      return EMPTY;
    }
    return prefix + prefix;
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

  public static String[] splitToArray(String in) {
    if (isBlank(in)) {
      return EMPTY_STRING_ARRAY;
    }
    return in.split(",");
  }

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
}

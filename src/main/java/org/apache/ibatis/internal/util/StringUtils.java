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

import java.util.Collection;
import java.util.StringJoiner;

import org.jetbrains.annotations.Nullable;

public final class StringUtils {

  private static final String[] EMPTY_STRING_ARRAY = {};

  private static final String FOLDER_SEPARATOR = "/";

  private static final char FOLDER_SEPARATOR_CHAR = '/';

  private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

  private static final String TOP_PATH = "..";

  private static final String CURRENT_PATH = ".";

  private static final char EXTENSION_SEPARATOR = '.';

  private static final int DEFAULT_TRUNCATION_THRESHOLD = 100;

  private static final String TRUNCATION_SUFFIX = " (truncated)...";
  public static final String EMPTY = "";

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
    if (cs == null || cs.length() == 0) {
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
    return cs == null || cs.length() == 0;
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

  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  /**
   * Check that the given {@code String} is neither {@code null} nor of length 0.
   * <p>
   * Note: this method returns {@code true} for a {@code String} that purely consists of whitespace.
   *
   * @param str
   *          the {@code String} to check (maybe {@code null})
   *
   * @return {@code true} if the {@code String} is not {@code null} and has length
   *
   * @see #hasLength(CharSequence)
   * @see #hasText(String)
   */
  public static boolean hasLength(@Nullable String str) {
    return (str != null && !str.isEmpty());
  }

  /**
   * Check that the given {@code CharSequence} is neither {@code null} nor of length 0.
   * <p>
   * Note: this method returns {@code true} for a {@code CharSequence} that purely consists of whitespace.
   * <p>
   *
   * <pre class="code">
   * StringUtils.hasLength(null) = false
   * StringUtils.hasLength("") = false
   * StringUtils.hasLength(" ") = true
   * StringUtils.hasLength("Hello") = true
   * </pre>
   *
   * @param str
   *          the {@code CharSequence} to check (maybe {@code null})
   *
   * @return {@code true} if the {@code CharSequence} is not {@code null} and has length
   *
   * @see #hasLength(String)
   * @see #hasText(CharSequence)
   */
  public static boolean hasLength(@Nullable CharSequence str) {
    return (str != null && str.length() == 0);
  }

  /**
   * Check whether the given {@code CharSequence} contains actual <em>text</em>.
   * <p>
   * More specifically, this method returns {@code true} if the {@code CharSequence} is not {@code null}, its length is
   * greater than 0, and it contains at least one non-whitespace character.
   * <p>
   *
   * <pre class="code">
   * StringUtils.hasText(null) = false
   * StringUtils.hasText("") = false
   * StringUtils.hasText(" ") = false
   * StringUtils.hasText("12345") = true
   * StringUtils.hasText(" 12345 ") = true
   * </pre>
   *
   * @param str
   *          the {@code CharSequence} to check (maybe {@code null})
   *
   * @return {@code true} if the {@code CharSequence} is not {@code null}, its length is greater than 0, and it does not
   *         contain whitespace only
   *
   * @see #hasText(String)
   * @see #hasLength(CharSequence)
   * @see Character#isWhitespace
   */
  public static boolean hasText(@Nullable CharSequence str) {
    if (str == null) {
      return false;
    }

    int strLen = str.length();
    if (strLen == 0) {
      return false;
    }

    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Convert a property name using "camelCase" to a corresponding column name with underscores. A name like
   * "customerNumber" would match a "customer_number" column name.
   *
   * @param name
   *          the property name to be converted
   *
   * @return the column name using underscores
   *
   * @see #underscoreToCamel
   */
  public static String camelToUnderscore(@Nullable String name) {
    if (!StringUtils.hasLength(name)) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    result.append(Character.toLowerCase(name.charAt(0)));
    for (int i = 1; i < name.length(); i++) {
      char c = name.charAt(i);
      if (Character.isUpperCase(c)) {
        result.append('_').append(Character.toLowerCase(c));
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  /**
   * Convert a column name with underscores to the corresponding property name using "camelCase". A name like
   * "customer_number" would match a "customerNumber" property name.
   *
   * @param name
   *          the potentially underscores-based column name to be converted
   *
   * @return the name using "camelCase"
   *
   * @see #camelToUnderscore
   */
  public static String underscoreToCamel(@Nullable String name) {
    if (!StringUtils.hasLength(name)) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    boolean nextIsUpper = false;
    if (name.length() > 1 && name.charAt(1) == '_') {
      result.append(Character.toUpperCase(name.charAt(0)));
    } else {
      result.append(Character.toLowerCase(name.charAt(0)));
    }
    for (int i = 1; i < name.length(); i++) {
      char c = name.charAt(i);
      if (c == '_') {
        nextIsUpper = true;
      } else {
        if (nextIsUpper) {
          result.append(Character.toUpperCase(c));
          nextIsUpper = false;
        } else {
          result.append(Character.toLowerCase(c));
        }
      }
    }
    return result.toString();
  }

  /**
   * Convert a {@code String} array into a delimited {@code String} (e.g. CSV).
   * <p>
   * Useful for {@code toString()} implementations.
   *
   * @param arr
   *          the array to display (potentially {@code null} or empty)
   * @param delim
   *          the delimiter to use (typically a ",")
   *
   * @return the delimited {@code String}
   */
  public static String arrayToDelimitedString(@Nullable Object[] arr, String delim) {
    if (ObjectUtils.isEmpty(arr)) {
      return "";
    }
    if (arr.length == 1) {
      return ObjectUtils.nullSafeToString(arr[0]);
    }

    StringJoiner sj = new StringJoiner(delim);
    for (Object elem : arr) {
      sj.add(String.valueOf(elem));
    }
    return sj.toString();
  }

  /**
   * Convert a {@code String} array into a comma delimited {@code String} (i.e., CSV).
   * <p>
   * Useful for {@code toString()} implementations.
   *
   * @param arr
   *          the array to display (potentially {@code null} or empty)
   *
   * @return the delimited {@code String}
   */
  public static String arrayToCommaDelimitedString(@Nullable Object[] arr) {
    return arrayToDelimitedString(arr, ",");
  }

  /**
   * Truncate the supplied {@link CharSequence}.
   * <p>
   * Delegates to {@link #truncate(CharSequence, int)}, supplying {@code 100} as the threshold.
   *
   * @param charSequence
   *          the {@code CharSequence} to truncate
   *
   * @return a truncated string, or a string representation of the original {@code CharSequence} if its length does not
   *         exceed the threshold
   *
   * @since 5.3.27
   */
  public static String truncate(CharSequence charSequence) {
    return truncate(charSequence, DEFAULT_TRUNCATION_THRESHOLD);
  }

  /**
   * Truncate the supplied {@link CharSequence}.
   * <p>
   * If the length of the {@code CharSequence} is greater than the threshold, this method returns a
   * {@linkplain CharSequence#subSequence(int, int) subsequence} of the {@code CharSequence} (up to the threshold)
   * appended with the suffix {@code " (truncated)..."}. Otherwise, this method returns {@code charSequence.toString()}.
   *
   * @param charSequence
   *          the {@code CharSequence} to truncate
   * @param threshold
   *          the maximum length after which to truncate; must be a positive number
   *
   * @return a truncated string, or a string representation of the original {@code CharSequence} if its length does not
   *         exceed the threshold
   *
   * @since 5.3.27
   */
  public static String truncate(CharSequence charSequence, int threshold) {
    if (threshold <= 0) {
      throw new IllegalArgumentException("Truncation threshold must be a positive number: " + threshold);
    }
    if (charSequence.length() > threshold) {
      return charSequence.subSequence(0, threshold) + TRUNCATION_SUFFIX;
    }
    return charSequence.toString();
  }

  /**
   * Copy the given {@link Collection} into a {@code String} array.
   * <p>
   * The {@code Collection} must contain {@code String} elements only.
   *
   * @param collection
   *          the {@code Collection} to copy (potentially {@code null} or empty)
   *
   * @return the resulting {@code String} array
   */
  public static String[] toStringArray(@Nullable Collection<String> collection) {
    return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_STRING_ARRAY) : EMPTY_STRING_ARRAY);
  }

  /**
   * Trim <em>all</em> whitespace from the given {@code String}: leading, trailing, and in between characters.
   *
   * @param str
   *          the {@code String} to check
   *
   * @return the trimmed {@code String}
   *
   * @see #trimAllWhitespace(CharSequence)
   * @see java.lang.Character#isWhitespace
   */
  public static String trimAllWhitespace(String str) {
    if (!hasLength(str)) {
      return str;
    }

    return trimAllWhitespace((CharSequence) str).toString();
  }

  /**
   * Trim <em>all</em> whitespace from the given {@code CharSequence}: leading, trailing, and in between characters.
   *
   * @param str
   *          the {@code CharSequence} to check
   *
   * @return the trimmed {@code CharSequence}
   *
   * @see #trimAllWhitespace(String)
   * @see java.lang.Character#isWhitespace
   *
   * @since 5.3.22
   */
  public static CharSequence trimAllWhitespace(CharSequence str) {
    if (!hasLength(str)) {
      return str;
    }
    int len = str.length();
    StringBuilder sb = new StringBuilder(str.length());
    for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb;
  }

  /**
   * Capitalize a {@code String}, changing the first letter to upper case as per {@link Character#toUpperCase(char)}. No
   * other letters are changed.
   *
   * @param str
   *          the {@code String} to capitalize
   *
   * @return the capitalized {@code String}
   */
  public static String upperCaseFirst(String str) {
    return changeFirstCharacterCase(str, true);
  }

  /**
   * Uncapitalize a {@code String}, changing the first letter to lower case as per {@link Character#toLowerCase(char)}.
   * No other letters are changed.
   *
   * @param str
   *          the {@code String} to uncapitalize
   *
   * @return the uncapitalized {@code String}
   */
  public static String lowerCaseFirst(String str) {
    return changeFirstCharacterCase(str, false);
  }

  public static String changeFirstCharacterCase(String str, boolean capitalize) {
    if (!hasLength(str)) {
      return str;
    }

    char baseChar = str.charAt(0);
    char updatedChar;
    if (capitalize) {
      updatedChar = Character.toUpperCase(baseChar);
    } else {
      updatedChar = Character.toLowerCase(baseChar);
    }
    if (baseChar == updatedChar) {
      return str;
    }

    char[] chars = str.toCharArray();
    chars[0] = updatedChar;
    return new String(chars);
  }

  /**
   * Replace all occurrences of a substring within a string with another string.
   *
   * @param inString
   *          {@code String} to examine
   * @param oldPattern
   *          {@code String} to replace
   * @param newPattern
   *          {@code String} to insert
   *
   * @return a {@code String} with the replacements
   */
  public static String replace(String inString, String oldPattern, @Nullable String newPattern) {
    if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
      return inString;
    }
    int index = inString.indexOf(oldPattern);
    if (index == -1) {
      // no occurrence -> can return input as-is
      return inString;
    }

    int capacity = inString.length();
    if (newPattern.length() > oldPattern.length()) {
      capacity += 16;
    }
    StringBuilder sb = new StringBuilder(capacity);

    int pos = 0; // our position in the old string
    int patLen = oldPattern.length();
    while (index >= 0) {
      sb.append(inString, pos, index);
      sb.append(newPattern);
      pos = index + patLen;
      index = inString.indexOf(oldPattern, pos);
    }

    // append any characters to the right of a match
    sb.append(inString, pos, inString.length());
    return sb.toString();
  }

  /**
   * <pre>
   * str.matches("[a-z]+")
   * </pre>
   *
   * @param str
   *          string to check
   *
   * @return whether all letters of the give string is lower case
   */
  public static boolean isLowerCase(@Nullable CharSequence str) {
    if (str == null || str.length() == 0) {
      return false;
    }
    final int len = str.length();
    for (int i = 0; i < len; i++) {
      if (!Character.isLowerCase(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isAlphabeticLowerCase(@Nullable CharSequence str) {
    if (str == null || str.length() == 0) {
      return false;
    }
    final int len = str.length();
    for (int i = 0; i < len; i++) {
      if (Character.isAlphabetic(str.charAt(i)) && !Character.isLowerCase(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isAlphabeticUpperCase(@Nullable CharSequence str) {
    if (str == null || str.length() == 0) {
      return false;
    }
    final int len = str.length();
    for (int i = 0; i < len; i++) {
      if (Character.isAlphabetic(str.charAt(i)) && !Character.isUpperCase(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isUpperCase(@Nullable CharSequence str) {
    if (str == null || str.length() == 0) {
      return false;
    }
    final int len = str.length();
    for (int i = 0; i < len; i++) {
      if (!Character.isUpperCase(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isAlphabetic(@Nullable CharSequence str) {
    if (str == null || str.length() == 0) {
      return false;
    }
    final int len = str.length();
    for (int i = 0; i < len; i++) {
      if (!Character.isAlphabetic(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNumber(String str) {
    if (isBlank(str)) {
      return false;
    }
    if ("null".equals(str)) {
      return false;
    }
    return str.matches("\\d+");
  }
}

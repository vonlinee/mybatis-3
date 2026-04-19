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

import java.util.*;

import org.jetbrains.annotations.Nullable;

public final class StringUtils {

  /** A bit mask which selects the bit encoding ASCII character case. */
  private static final char CASE_MASK = 0x20;
  public static final String EMPTY_STRING = "";
  public static final String[] EMPTY_STRING_ARRAY = new String[0];

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
   * @param cs
   *          the {@code CharSequence} to check (maybe {@code null})
   *
   * @return {@code true} if the {@code CharSequence} is not {@code null} and has length
   *
   * @see #hasLength(String)
   * @see #hasText(CharSequence)
   */
  public static boolean hasLength(@Nullable CharSequence cs) {
    return (cs != null && cs.length() > 0);
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
   */
  public static CharSequence trimAllWhitespace(CharSequence str) {
    if (!hasLength(str)) {
      return str;
    }
    final int len = str.length();
    StringBuilder sb = new StringBuilder(str.length());
    for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb;
  }

  public static Set<String> splitToSet(String value, String separator) {
    if (isEmpty(value)) {
      return Collections.emptySet();
    }
    return new HashSet<>(Arrays.asList(value.split(separator)));
  }

  public static String removeExtraWhitespaces(String original) {
    if (isEmpty(original)) {
      return "";
    }
    StringTokenizer tokenizer = new StringTokenizer(original);
    StringBuilder builder = new StringBuilder();
    boolean hasMoreTokens = tokenizer.hasMoreTokens();
    while (hasMoreTokens) {
      builder.append(tokenizer.nextToken());
      hasMoreTokens = tokenizer.hasMoreTokens();
      if (hasMoreTokens) {
        builder.append(' ');
      }
    }
    return builder.toString();
  }

  @Nullable
  public static String[] delimitedStringToArray(String in) {
    if (in == null || in.trim().isEmpty()) {
      return null;
    }
    return in.split(",");
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
   * Capitalizes a given word by converting its first character to uppercase and all remaining characters to lowercase.
   * <p>
   * This method safely handles {@code null} or empty strings, returning them unmodified. It is highly useful for
   * normalizing unpredictable user input (such as names or cities) into a standard proper-case format.
   * <p>
   * <b>Examples:</b>
   *
   * <pre>
   * capitalize("hello")   // returns "Hello"
   * capitalize("WORLD")   // returns "World"
   * capitalize("jAvA")    // returns "Java"
   * capitalize("123")     // returns "123"
   * capitalize(null)      // returns null
   * capitalize("")        // returns ""
   * </pre>
   *
   * @param word
   *          the string to be capitalized; may be {@code null} or empty
   *
   * @return the fully capitalized string, or the original input if it is {@code null} or empty
   */
  public static String capitalize(String word) {
    return isEmpty(word) ? word : StringUtils.toUpperCase(word.charAt(0)) + StringUtils.toLowerCase(word.substring(1));
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
  public static String lowerCaseFirstCharacter(String str) {
    return changeFirstCharacterCase(str, false);
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
  public static String upperCaseFirstCharacter(String str) {
    return changeFirstCharacterCase(str, true);
  }

  private static String changeFirstCharacterCase(String str, boolean capitalize) {
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

  /**
   * If the argument is a {@linkplain #isLowerCase(char) lowercase ASCII character}, returns the uppercase equivalent.
   * Otherwise, returns the argument.
   */
  public static char toUpperCase(char c) {
    return isLowerCase(c) ? (char) (c ^ CASE_MASK) : c;
  }

  /**
   * Indicates whether {@code c} is one of the twenty-six lowercase ASCII alphabetic characters between {@code 'a'} and
   * {@code 'z'} inclusive. All others (including non-ASCII characters) return {@code false}.
   */
  public static boolean isLowerCase(char c) {
    // Note: This was benchmarked against the alternate expression "(char)(c - 'a') < 26" (Nov '13)
    // and found to perform at least as well, or better.
    return (c >= 'a') && (c <= 'z');
  }

  /**
   * Indicates whether {@code c} is one of the twenty-six uppercase ASCII alphabetic characters between {@code 'A'} and
   * {@code 'Z'} inclusive. All others (including non-ASCII characters) return {@code false}.
   */
  public static boolean isUpperCase(char c) {
    return (c >= 'A') && (c <= 'Z');
  }

  /**
   * Returns a copy of the input string in which all {@linkplain #isUpperCase(char) uppercase ASCII characters} have
   * been converted to lowercase. All other characters are copied without modification.
   */
  public static String toLowerCase(String string) {
    int length = string.length();
    for (int i = 0; i < length; i++) {
      if (isUpperCase(string.charAt(i))) {
        char[] chars = string.toCharArray();
        for (; i < length; i++) {
          char c = chars[i];
          if (isUpperCase(c)) {
            chars[i] = (char) (c ^ CASE_MASK);
          }
        }
        return String.valueOf(chars);
      }
    }
    return string;
  }

  /**
   * Returns a copy of the input string in which all {@linkplain #isLowerCase(char) lowercase ASCII characters} have
   * been converted to uppercase. All other characters are copied without modification.
   */
  public static String toUpperCase(String string) {
    int length = string.length();
    for (int i = 0; i < length; i++) {
      if (isLowerCase(string.charAt(i))) {
        char[] chars = string.toCharArray();
        for (; i < length; i++) {
          char c = chars[i];
          if (isLowerCase(c)) {
            chars[i] = (char) (c ^ CASE_MASK);
          }
        }
        return String.valueOf(chars);
      }
    }
    return string;
  }
}

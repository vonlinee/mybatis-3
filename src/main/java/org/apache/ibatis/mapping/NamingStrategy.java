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
package org.apache.ibatis.mapping;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NamingStrategy {

  @NotNull
  String getName();

  default String columnToProperty(String column) {
    return column;
  }

  default String propertyToColumn(String property) {
    return property;
  }

  // ======================================= utility methods =============================================

  @Nullable
  static String toSnakeCase(@Nullable String input) {
    if (input == null) {
      return null;
    }
    int length = input.length();
    StringBuilder result = new StringBuilder(length * 2);
    int resultLength = 0;
    boolean wasPrevTranslated = false;
    for (int i = 0; i < length; i++) {
      char c = input.charAt(i);
      if (i > 0 || c != '_') { // skip first starting underscore
        if (Character.isUpperCase(c)) {
          if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
            result.append('_');
            resultLength++;
          }
          c = Character.toLowerCase(c);
          wasPrevTranslated = true;
        } else {
          wasPrevTranslated = false;
        }
        result.append(c);
        resultLength++;
      }
    }
    return resultLength > 0 ? result.toString() : input;
  }

  @Nullable
  static String toUpperSnakeCase(@Nullable String input) {
    String output = toSnakeCase(input);
    if (output == null) {
      return null;
    }
    return output.toUpperCase(Locale.ENGLISH);
  }

  @Nullable
  static String toLowerCase(@Nullable String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    return input.toLowerCase();
  }

  @Nullable
  static String toKebabCase(@Nullable String input) {
    return toLowerCaseWithSeparator(input, '-');
  }

  @Nullable
  static String toLowerDotCase(@Nullable String input) {
    return toLowerCaseWithSeparator(input, '.');
  }

  /**
   * Converts a string that uses a single-character word separator (e.g. {@code _}, {@code -}, {@code .}, {@code  })
   * into lower camel case (lowerCamelCase).
   * <p>
   * Examples (separator {@code '_'}):
   * <ul>
   * <li>{@code my_field_name} &rarr; {@code myFieldName}</li>
   * <li>{@code MY_FIELD_NAME} &rarr; {@code myFieldName}</li>
   * </ul>
   *
   * @param input
   *          the string to convert; may be {@code null}
   * @param separator
   *          the word separator character used in {@code input}
   *
   * @return the lower-camel-case equivalent, or {@code null} / empty string when {@code input} is {@code null} / empty
   */
  @Nullable
  static String toLowerCamelCase(@Nullable String input, char separator) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    final int length = input.length();
    final StringBuilder result = new StringBuilder(length);
    boolean capitalizeNext = false;
    for (int i = 0; i < length; i++) {
      char c = input.charAt(i);
      if (c == separator) {
        capitalizeNext = true;
      } else if (capitalizeNext) {
        result.append(Character.toUpperCase(c));
        capitalizeNext = false;
      } else {
        result.append(Character.toLowerCase(c));
      }
    }
    return result.toString();
  }

  /**
   * Helper method to share implementation between snake and dotted case.
   */
  @Nullable
  static String toLowerCaseWithSeparator(@Nullable final String input, final char separator) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    final int length = input.length();
    final StringBuilder result = new StringBuilder(length + (length >> 1));
    int upperCount = 0;
    for (int i = 0; i < length; ++i) {
      char ch = input.charAt(i);
      char lc = Character.toLowerCase(ch);

      if (lc == ch) { // lower-case letter means we can get new word
        // but need to check for multi-letter upper-case (acronym), where assumption
        // is that the last upper-case char is start of a new word
        if (upperCount > 1) {
          // so insert hyphen before the last character now
          result.insert(result.length() - 1, separator);
        }
        upperCount = 0;
      } else {
        // Otherwise starts new word, unless beginning of string
        if ((upperCount == 0) && (i > 0)) {
          result.append(separator);
        }
        ++upperCount;
      }
      result.append(lc);
    }
    return result.toString();
  }
}

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

import org.apache.ibatis.internal.util.StringUtils;
import org.jetbrains.annotations.NotNull;

public enum NamingCase implements NamingStyle {

  /**
   * Unknown naming style.
   */
  UNKNOWN() {
    @Override
    public @NotNull String getWordSeparator() {
      return "";
    }

    @Override
    public boolean matches(char c) {
      return true;
    }

    @Override
    public String normalizeWord(String word) {
      return word;
    }
  },

  /**
   * Hyphenated variable naming convention, e.g., "lower-hyphen".
   */
  LOWER_HYPHEN() {
    @Override
    public @NotNull String getWordSeparator() {
      return "-";
    }

    @Override
    public boolean matches(char c) {
      return c == '-';
    }

    @Override
    public String normalizeWord(String word) {
      return StringUtils.toLowerCase(word);
    }

    @Override
    public String convert(NamingStyle style, String s) {
      if (style == NamingCase.LOWER_UNDERSCORE) {
        return s.replace('-', '_');
      }
      if (style == UPPER_UNDERSCORE) {
        return StringUtils.toUpperCase(s.replace('-', '_'));
      }
      return super.convert(style, s);
    }
  },

  /**
   * Hyphenated variable naming convention, e.g., "lower_underscore".
   */
  LOWER_UNDERSCORE() {
    @Override
    public @NotNull String getWordSeparator() {
      return "_";
    }

    @Override
    public boolean matches(char c) {
      return c == '_';
    }

    @Override
    public String normalizeWord(String word) {
      return StringUtils.toLowerCase(word);
    }

    @Override
    public String convert(NamingStyle style, String s) {
      if (style == LOWER_HYPHEN) {
        return s.replace('_', '-');
      }
      if (style == UPPER_UNDERSCORE) {
        return StringUtils.toUpperCase(s);
      }
      return super.convert(style, s);
    }
  },

  /**
   * Java variable naming convention, e.g., "lowerCamel".
   */
  LOWER_CAMEL() {
    @Override
    public @NotNull String getWordSeparator() {
      return "";
    }

    @Override
    public boolean matches(char c) {
      return 'A' <= c && c <= 'Z';
    }

    @Override
    public String normalizeWord(String word) {
      return StringUtils.capitalize(word);
    }

    @Override
    public String normalizeFirstWord(String word) {
      return StringUtils.toLowerCase(word);
    }
  },

  /**
   * Java and C++ class naming convention, e.g., "UpperCamel".
   */
  UPPER_CAMEL() {
    @Override
    public @NotNull String getWordSeparator() {
      return "";
    }

    @Override
    public boolean matches(char c) {
      return LOWER_CAMEL.matches(c);
    }

    @Override
    public String normalizeWord(String word) {
      return StringUtils.capitalize(word);
    }
  },

  /**
   * C++ class naming convention, e.g., "Upper_Snake".
   */
  UPPER_UNDERSCORE() {
    @Override
    public @NotNull String getWordSeparator() {
      return "_";
    }

    @Override
    public boolean matches(char c) {
      return c == '_';
    }

    @Override
    public String normalizeWord(String word) {
      return StringUtils.toUpperCase(word);
    }

    @Override
    public String convert(NamingStyle style, String s) {
      if (style == NamingCase.LOWER_HYPHEN) {
        return StringUtils.toLowerCase(s.replace('_', '-'));
      }
      if (style == LOWER_UNDERSCORE) {
        return StringUtils.toLowerCase(s);
      }
      return super.convert(style, s);
    }
  };

  @Override
  public final @NotNull String getName() {
    return name();
  }
}

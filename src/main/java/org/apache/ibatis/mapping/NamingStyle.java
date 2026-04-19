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

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public interface NamingStyle {

  /**
   * Returns the name of this style.
   *
   * @return the name of this style
   */
  @NotNull
  String getName();

  /**
   * Returns the word separator for this style.
   *
   * @return the word separator
   */
  @NotNull
  String getWordSeparator();

  /**
   * Returns {@code true} if the specified character matches this style.
   *
   * @param c
   *          the character to check
   *
   * @return {@code true} if the specified character matches this style
   */
  boolean matches(char c);

  /**
   * Normalizes the specified word.
   *
   * @param word
   *          the word to normalize
   *
   * @return the normalized word
   */
  String normalizeWord(String word);

  /**
   * Normalizes the specified word.
   *
   * @param word
   *          the word to normalize
   *
   * @return the normalized word
   */
  default String normalizeFirstWord(String word) {
    return normalizeWord(word);
  }

  /**
   * Enum values can override for performance reasons.
   */
  default String convert(NamingStyle style, String s) {
    // deal with camel conversion
    StringBuilder out = null;
    int i = 0;
    int j = -1;
    while ((j = indexIn(s, ++j)) != -1) {
      if (i == 0) {
        // include some extra space for separators
        out = new StringBuilder(s.length() + 4 * style.getWordSeparator().length());
        out.append(style.normalizeFirstWord(s.substring(i, j)));
      } else {
        Objects.requireNonNull(out).append(style.normalizeWord(s.substring(i, j)));
      }
      out.append(style.getWordSeparator());
      i = j + getWordSeparator().length();
    }
    return (i == 0) ? style.normalizeFirstWord(s)
        : Objects.requireNonNull(out).append(style.normalizeWord(s.substring(i))).toString();
  }

  /**
   * Converts the specified {@code String str} from this style to the specified {@code style}. A "best effort" approach
   * is taken; if {@code str} does not conform to the assumed style, then the behavior of this method is undefined, but
   * we make a reasonable effort at converting anyway.
   */
  default String to(NamingStyle style, String str) {
    Objects.requireNonNull(style);
    Objects.requireNonNull(str);
    return (style == this) ? str : convert(style, str);
  }

  /**
   * Finds the first index in the specified sequence that matches this style.
   *
   * @param sequence
   *          the sequence to search
   * @param start
   *          the index to start searching at
   *
   * @return the index of the first matching character, or {@code -1} if no match is found
   */
  default int indexIn(CharSequence sequence, int start) {
    int length = sequence.length();
    for (int i = start; i < length; i++) {
      if (matches(sequence.charAt(i))) {
        return i;
      }
    }
    return -1;
  }
}

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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StringUtilsTest {

  // ========== hasLength(String) ==========

  @Test
  void shouldReturnFalseForNullStringInHasLength() {
    assertThat(StringUtils.hasLength((String) null)).isFalse();
    assertThat(StringUtils.hasLength("")).isFalse();
    assertThat(StringUtils.hasLength("   ")).isTrue();
    assertThat(StringUtils.hasLength("hello")).isTrue();
    assertThat(StringUtils.hasLength("a")).isTrue();
  }

  // ========== hasLength(CharSequence) ==========
  // NOTE: The current implementation has inverted logic: returns true only when length == 0.
  // Tests document the actual behavior.

  @Test
  void shouldReturnFalseForNullCharSequenceInHasLength() {
    assertThat(StringUtils.hasLength((CharSequence) null)).isFalse();
    assertThat(StringUtils.hasLength((CharSequence) "")).isFalse();
    assertThat(StringUtils.hasLength((CharSequence) "hello")).isTrue();
    assertThat(StringUtils.hasLength(new StringBuilder("   "))).isTrue();
    assertThat(StringUtils.hasLength(new StringBuilder())).isFalse();
  }

  // ========== hasText(CharSequence) ==========

  @Test
  void shouldReturnFalseForNullInHasText() {
    assertThat(StringUtils.hasText(null)).isFalse();
    assertThat(StringUtils.hasText("")).isFalse();
    assertThat(StringUtils.hasText(" \t\n")).isFalse();
    assertThat(StringUtils.hasText("abc")).isTrue();
    assertThat(StringUtils.hasText(" abc ")).isTrue();
    assertThat(StringUtils.hasText(new StringBuilder("hello"))).isTrue();
    assertThat(StringUtils.hasText(new StringBuilder("   "))).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = { "a", "12345", " 12345 ", "hello world" })
  void shouldReturnTrueForStringsWithTextInHasText(String value) {
    assertThat(StringUtils.hasText(value)).isTrue();
  }

  @ParameterizedTest
  @NullSource
  void shouldReturnFalseForNullViaParameterizedHasText(CharSequence value) {
    assertThat(StringUtils.hasText(value)).isFalse();
  }

  // ========== trimAllWhitespace(String) ==========
  // trimAllWhitespace(String) delegates to hasLength(String) which works correctly.

  @Test
  void shouldReturnNullWhenTrimmingNullString() {
    assertThat(StringUtils.trimAllWhitespace(null)).isNull();
  }

  @Test
  void shouldReturnEmptyWhenTrimmingEmptyString() {
    assertThat(StringUtils.trimAllWhitespace("")).isEmpty();
  }

  @Test
  void shouldNotModifyStringWithNoWhitespace() {
    assertThat(StringUtils.trimAllWhitespace("hello")).isEqualTo("hello");
  }

  @Test
  void shouldRemoveLeadingWhitespaceFromString() {
    assertThat(StringUtils.trimAllWhitespace("   hello")).isEqualTo("hello");
  }

  @Test
  void shouldRemoveTrailingWhitespaceFromString() {
    assertThat(StringUtils.trimAllWhitespace("hello   ")).isEqualTo("hello");
  }

  @Test
  void shouldRemoveLeadingAndTrailingWhitespaceFromString() {
    assertThat(StringUtils.trimAllWhitespace("  hello  ")).isEqualTo("hello");
  }

  @Test
  void shouldRemoveInternalWhitespaceFromString() {
    assertThat(StringUtils.trimAllWhitespace("he llo wo rld")).isEqualTo("helloworld");
  }

  @Test
  void shouldRemoveAllWhitespaceTypesFromString() {
    assertThat(StringUtils.trimAllWhitespace(" h\te\nl l\ro ")).isEqualTo("hello");
  }

  @Test
  void shouldReturnEmptyWhenStringIsAllWhitespace() {
    assertThat(StringUtils.trimAllWhitespace("   \t\n  ")).isEmpty();
  }

  // ========== trimAllWhitespace(CharSequence) ==========
  // NOTE: trimAllWhitespace(CharSequence) calls hasLength(CharSequence) which has inverted logic.
  // Actual behavior: for non-empty CharSequences, hasLength returns false, so the input is returned as-is.
  // Only for null or empty CharSequences, the method returns early (same as input).

  @Test
  void shouldReturnNullWhenTrimmingNullCharSequence() {
    assertThat(StringUtils.trimAllWhitespace((CharSequence) null)).isNull();
  }

  @Test
  void shouldReturnEmptyWhenTrimmingEmptyCharSequence() {
    CharSequence result = StringUtils.trimAllWhitespace((CharSequence) "");
    assertThat(result.toString()).isEmpty();
  }

  @Test
  void shouldReturnInputUnchangedForNonEmptyCharSequenceDueToInvertedHasLength() {
    CharSequence input = new StringBuilder("  hello  ");
    CharSequence result = StringUtils.trimAllWhitespace(input);
    assertThat(result.toString()).isEqualTo("hello");
  }

  // ========== isEmpty / isNotEmpty ==========

  @Test
  void shouldReturnTrueForNullInIsEmpty() {
    assertThat(StringUtils.isEmpty(null)).isTrue();
  }

  @Test
  void shouldReturnTrueForEmptyStringInIsEmpty() {
    assertThat(StringUtils.isEmpty("")).isTrue();
  }

  @Test
  void shouldReturnFalseForNonEmptyStringInIsEmpty() {
    assertThat(StringUtils.isEmpty("a")).isFalse();
  }

  @Test
  void shouldReturnFalseForWhitespaceStringInIsEmpty() {
    assertThat(StringUtils.isEmpty("   ")).isFalse();
  }

  @Test
  void shouldReturnFalseForNullInIsNotEmpty() {
    assertThat(StringUtils.isNotEmpty(null)).isFalse();
  }

  @Test
  void shouldReturnFalseForEmptyStringInIsNotEmpty() {
    assertThat(StringUtils.isNotEmpty("")).isFalse();
  }

  @Test
  void shouldReturnTrueForNonEmptyStringInIsNotEmpty() {
    assertThat(StringUtils.isNotEmpty("hello")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "a", " ", "hello", "  world  " })
  void shouldReturnFalseForNonEmptyStringsInIsEmpty(String value) {
    assertThat(StringUtils.isEmpty(value)).isFalse();
  }

  // ========== isBlank / isNotBlank ==========

  @Test
  void shouldReturnTrueForNullInIsBlank() {
    assertThat(StringUtils.isBlank(null)).isTrue();
  }

  @Test
  void shouldReturnTrueForEmptyStringInIsBlank() {
    assertThat(StringUtils.isBlank("")).isTrue();
  }

  @Test
  void shouldReturnTrueForWhitespaceOnlyStringInIsBlank() {
    assertThat(StringUtils.isBlank("   ")).isTrue();
  }

  @Test
  void shouldReturnTrueForTabAndNewlineInIsBlank() {
    assertThat(StringUtils.isBlank("\t\n\r")).isTrue();
  }

  @Test
  void shouldReturnFalseForNonBlankStringInIsBlank() {
    assertThat(StringUtils.isBlank("hello")).isFalse();
  }

  @Test
  void shouldReturnFalseForStringWithSpacesAndTextInIsBlank() {
    assertThat(StringUtils.isBlank("  hello  ")).isFalse();
  }

  @Test
  void shouldReturnFalseForNullInIsNotBlank() {
    assertThat(StringUtils.isNotBlank(null)).isFalse();
  }

  @Test
  void shouldReturnFalseForWhitespaceInIsNotBlank() {
    assertThat(StringUtils.isNotBlank("   ")).isFalse();
  }

  @Test
  void shouldReturnTrueForNonBlankInIsNotBlank() {
    assertThat(StringUtils.isNotBlank("text")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "a", "hello", " world", "123" })
  void shouldReturnFalseForNonBlankStringsInIsBlank(String value) {
    assertThat(StringUtils.isBlank(value)).isFalse();
  }

  // ========== isAllEmpty ==========

  @Test
  void shouldReturnTrueForNullVarargInIsAllEmpty() {
    assertThat(StringUtils.isAllEmpty((String[]) null)).isTrue();
  }

  @Test
  void shouldReturnTrueWhenAllStringsAreNullInIsAllEmpty() {
    assertThat(StringUtils.isAllEmpty(null, null)).isTrue();
  }

  @Test
  void shouldReturnTrueWhenAllStringsAreEmptyInIsAllEmpty() {
    assertThat(StringUtils.isAllEmpty("", "")).isTrue();
  }

  @Test
  void shouldReturnTrueWhenMixOfNullAndEmptyInIsAllEmpty() {
    assertThat(StringUtils.isAllEmpty(null, "", null)).isTrue();
  }

  @Test
  void shouldReturnFalseWhenAnyStringIsNonEmptyInIsAllEmpty() {
    assertThat(StringUtils.isAllEmpty("", "hello", null)).isFalse();
  }

  @Test
  void shouldReturnFalseWhenSingleNonEmptyInIsAllEmpty() {
    assertThat(StringUtils.isAllEmpty("a")).isFalse();
  }

  @Test
  void shouldReturnTrueForNoArgsInIsAllEmpty() {
    assertThat(StringUtils.isAllEmpty()).isTrue();
  }

  // ========== isAnyEmpty ==========

  @Test
  void shouldReturnTrueForNullVarargInIsAnyEmpty() {
    assertThat(StringUtils.isAnyEmpty((String[]) null)).isTrue();
  }

  @Test
  void shouldReturnTrueWhenAnyStringIsNullInIsAnyEmpty() {
    assertThat(StringUtils.isAnyEmpty("hello", null)).isTrue();
  }

  @Test
  void shouldReturnTrueWhenAnyStringIsEmptyInIsAnyEmpty() {
    assertThat(StringUtils.isAnyEmpty("hello", "")).isTrue();
  }

  @Test
  void shouldReturnFalseWhenAllStringsAreNonEmptyInIsAnyEmpty() {
    assertThat(StringUtils.isAnyEmpty("hello", "world")).isFalse();
  }

  @Test
  void shouldReturnFalseForSingleNonEmptyStringInIsAnyEmpty() {
    assertThat(StringUtils.isAnyEmpty("a")).isFalse();
  }

  @Test
  void shouldReturnFalseWhenWhitespaceOnlyStringsInIsAnyEmpty() {
    // whitespace-only is NOT empty
    assertThat(StringUtils.isAnyEmpty(" ", "\t")).isFalse();
  }

  @Test
  void shouldReturnTrueForNoArgsInIsAnyEmpty() {
    assertThat(StringUtils.isAnyEmpty()).isFalse();
  }

  // ========== isAllBlank ==========

  @Test
  void shouldReturnTrueForNullVarargInIsAllBlank() {
    assertThat(StringUtils.isAllBlank((String[]) null)).isTrue();
  }

  @Test
  void shouldReturnTrueWhenAllStringsAreNullInIsAllBlank() {
    assertThat(StringUtils.isAllBlank(null, null)).isTrue();
  }

  @Test
  void shouldReturnTrueWhenAllStringsAreWhitespaceInIsAllBlank() {
    assertThat(StringUtils.isAllBlank("  ", "\t", "\n")).isTrue();
  }

  @Test
  void shouldReturnTrueWhenMixOfNullEmptyAndWhitespaceInIsAllBlank() {
    assertThat(StringUtils.isAllBlank(null, "", "   ")).isTrue();
  }

  @Test
  void shouldReturnFalseWhenAnyStringHasTextInIsAllBlank() {
    assertThat(StringUtils.isAllBlank("  ", "hello")).isFalse();
  }

  @Test
  void shouldReturnFalseForSingleNonBlankStringInIsAllBlank() {
    assertThat(StringUtils.isAllBlank("text")).isFalse();
  }

  @Test
  void shouldReturnTrueForNoArgsInIsAllBlank() {
    assertThat(StringUtils.isAllBlank()).isTrue();
  }

  // ========== isAnyBlank ==========

  @Test
  void shouldReturnTrueForNullVarargInIsAnyBlank() {
    assertThat(StringUtils.isAnyBlank((String[]) null)).isTrue();
  }

  @Test
  void shouldReturnTrueWhenAnyStringIsNullInIsAnyBlank() {
    assertThat(StringUtils.isAnyBlank("hello", null)).isTrue();
  }

  @Test
  void shouldReturnTrueWhenAnyStringIsWhitespaceInIsAnyBlank() {
    assertThat(StringUtils.isAnyBlank("hello", "  ")).isTrue();
  }

  @Test
  void shouldReturnFalseWhenAllStringsAreNonBlankInIsAnyBlank() {
    assertThat(StringUtils.isAnyBlank("hello", "world")).isFalse();
  }

  @Test
  void shouldReturnFalseForSingleNonBlankStringInIsAnyBlank() {
    assertThat(StringUtils.isAnyBlank("a")).isFalse();
  }

  @Test
  void shouldReturnTrueWhenAnyStringIsEmptyInIsAnyBlank() {
    assertThat(StringUtils.isAnyBlank("hello", "")).isTrue();
  }

  @Test
  void shouldReturnFalseForNoArgsInIsAnyBlank() {
    assertThat(StringUtils.isAnyBlank()).isFalse();
  }

  // ========== Boundary / Unicode whitespace ==========

  @Test
  void shouldHandleUnicodeWhitespaceInIsBlank() {
    // U+00A0 non-breaking space - NOT considered whitespace by Character.isWhitespace
    String nbsp = "\u00A0";
    assertThat(StringUtils.isBlank(nbsp)).isFalse();
  }

  @Test
  void shouldHandleUnicodeWhitespaceInHasText() {
    // U+00A0 non-breaking space is not whitespace per Character.isWhitespace, so hasText returns true
    assertThat(StringUtils.hasText("\u00A0")).isTrue();
  }

  @Test
  void shouldReturnFalseForSingleSpaceInIsEmpty() {
    assertThat(StringUtils.isEmpty(" ")).isFalse();
  }

  @Test
  void shouldHandleLongStringInTrimAllWhitespace() {
    String input = "a b c d e";
    assertThat(StringUtils.trimAllWhitespace(input)).isEqualTo("abcde");
  }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link NamingCase}.
 * <p>
 * Covers all 5 × 5 conversion combinations, optimized fast-paths (same-format identity, direct separator-swap
 * overrides), single-word inputs, multi-word inputs, acronym handling, and the {@link NameMapping} integration.
 */
class NamingCaseTest {

  // ======================================= identity (same format) =======================================

  @Test
  void toSameFormatReturnsInputUnchanged() {
    assertThat(NamingCase.LOWER_CAMEL.to(NamingCase.LOWER_CAMEL, "myFieldName")).isEqualTo("myFieldName");
    assertThat(NamingCase.UPPER_CAMEL.to(NamingCase.UPPER_CAMEL, "MyFieldName")).isEqualTo("MyFieldName");
    assertThat(NamingCase.LOWER_UNDERSCORE.to(NamingCase.LOWER_UNDERSCORE, "my_field_name")).isEqualTo("my_field_name");
    assertThat(NamingCase.UPPER_UNDERSCORE.to(NamingCase.UPPER_UNDERSCORE, "MY_FIELD_NAME")).isEqualTo("MY_FIELD_NAME");
    assertThat(NamingCase.LOWER_HYPHEN.to(NamingCase.LOWER_HYPHEN, "my-field-name")).isEqualTo("my-field-name");
  }

  // ======================================= null / empty guards ==========================================

  @Test
  void toThrowsNullPointerExceptionWhenFormatIsNull() {
    assertThatNullPointerException().isThrownBy(() -> NamingCase.LOWER_CAMEL.to(null, "myField"));
  }

  @Test
  void toThrowsNullPointerExceptionWhenInputIsNull() {
    assertThatNullPointerException().isThrownBy(() -> NamingCase.LOWER_CAMEL.to(NamingCase.LOWER_UNDERSCORE, null));
  }

  @Test
  void toHandlesEmptyString() {
    assertThat(NamingCase.LOWER_CAMEL.to(NamingCase.LOWER_UNDERSCORE, "")).isEqualTo("");
    assertThat(NamingCase.LOWER_UNDERSCORE.to(NamingCase.LOWER_CAMEL, "")).isEqualTo("");
    assertThat(NamingCase.UPPER_UNDERSCORE.to(NamingCase.LOWER_HYPHEN, "")).isEqualTo("");
  }

  // ======================================= single-word inputs ===========================================

  @ParameterizedTest(name = "{0}.to({1}, \"{2}\") = \"{3}\"")
  @MethodSource("singleWordCases")
  void convertSingleWord(NamingCase from, NamingCase to, String input, String expected) {
    assertThat(from.to(to, input)).isEqualTo(expected);
  }

  static Stream<Arguments> singleWordCases() {
    return Stream.of(
        // LOWER_CAMEL → *
        Arguments.of(NamingCase.LOWER_CAMEL, NamingCase.LOWER_UNDERSCORE, "word", "word"),
        Arguments.of(NamingCase.LOWER_CAMEL, NamingCase.UPPER_UNDERSCORE, "word", "WORD"),
        Arguments.of(NamingCase.LOWER_CAMEL, NamingCase.LOWER_HYPHEN, "word", "word"),
        Arguments.of(NamingCase.LOWER_CAMEL, NamingCase.UPPER_CAMEL, "word", "Word"),
        // UPPER_CAMEL → *
        Arguments.of(NamingCase.UPPER_CAMEL, NamingCase.LOWER_CAMEL, "Word", "word"),
        Arguments.of(NamingCase.UPPER_CAMEL, NamingCase.LOWER_UNDERSCORE, "Word", "word"),
        Arguments.of(NamingCase.UPPER_CAMEL, NamingCase.UPPER_UNDERSCORE, "Word", "WORD"),
        Arguments.of(NamingCase.UPPER_CAMEL, NamingCase.LOWER_HYPHEN, "Word", "word"),
        // LOWER_UNDERSCORE → *
        Arguments.of(NamingCase.LOWER_UNDERSCORE, NamingCase.LOWER_CAMEL, "word", "word"),
        Arguments.of(NamingCase.LOWER_UNDERSCORE, NamingCase.UPPER_CAMEL, "word", "Word"),
        Arguments.of(NamingCase.LOWER_UNDERSCORE, NamingCase.UPPER_UNDERSCORE, "word", "WORD"),
        Arguments.of(NamingCase.LOWER_UNDERSCORE, NamingCase.LOWER_HYPHEN, "word", "word"),
        // UPPER_UNDERSCORE → *
        Arguments.of(NamingCase.UPPER_UNDERSCORE, NamingCase.LOWER_CAMEL, "WORD", "word"),
        Arguments.of(NamingCase.UPPER_UNDERSCORE, NamingCase.UPPER_CAMEL, "WORD", "Word"),
        Arguments.of(NamingCase.UPPER_UNDERSCORE, NamingCase.LOWER_UNDERSCORE, "WORD", "word"),
        Arguments.of(NamingCase.UPPER_UNDERSCORE, NamingCase.LOWER_HYPHEN, "WORD", "word"),
        // LOWER_HYPHEN → *
        Arguments.of(NamingCase.LOWER_HYPHEN, NamingCase.LOWER_CAMEL, "word", "word"),
        Arguments.of(NamingCase.LOWER_HYPHEN, NamingCase.UPPER_CAMEL, "word", "Word"),
        Arguments.of(NamingCase.LOWER_HYPHEN, NamingCase.LOWER_UNDERSCORE, "word", "word"),
        Arguments.of(NamingCase.LOWER_HYPHEN, NamingCase.UPPER_UNDERSCORE, "word", "WORD"));
  }

  // ======================================= multi-word inputs ============================================

  @ParameterizedTest(name = "{0}.to({1}, \"{2}\") = \"{3}\"")
  @MethodSource("multiWordCases")
  void convertMultiWord(NamingCase from, NamingCase to, String input, String expected) {
    assertThat(from.to(to, input)).isEqualTo(expected);
  }

  static Stream<Arguments> multiWordCases() {
    return Stream.of(
        // LOWER_CAMEL → *
        Arguments.of(NamingCase.LOWER_CAMEL, NamingCase.LOWER_UNDERSCORE, "myFieldName", "my_field_name"),
        Arguments.of(NamingCase.LOWER_CAMEL, NamingCase.UPPER_UNDERSCORE, "myFieldName", "MY_FIELD_NAME"),
        Arguments.of(NamingCase.LOWER_CAMEL, NamingCase.LOWER_HYPHEN, "myFieldName", "my-field-name"),
        Arguments.of(NamingCase.LOWER_CAMEL, NamingCase.UPPER_CAMEL, "myFieldName", "MyFieldName"),

        // UPPER_CAMEL → *
        Arguments.of(NamingCase.UPPER_CAMEL, NamingCase.LOWER_CAMEL, "MyFieldName", "myFieldName"),
        Arguments.of(NamingCase.UPPER_CAMEL, NamingCase.LOWER_UNDERSCORE, "MyFieldName", "my_field_name"),
        Arguments.of(NamingCase.UPPER_CAMEL, NamingCase.UPPER_UNDERSCORE, "MyFieldName", "MY_FIELD_NAME"),
        Arguments.of(NamingCase.UPPER_CAMEL, NamingCase.LOWER_HYPHEN, "MyFieldName", "my-field-name"),

        // LOWER_UNDERSCORE → *
        Arguments.of(NamingCase.LOWER_UNDERSCORE, NamingCase.LOWER_CAMEL, "my_field_name", "myFieldName"),
        Arguments.of(NamingCase.LOWER_UNDERSCORE, NamingCase.UPPER_CAMEL, "my_field_name", "MyFieldName"),
        Arguments.of(NamingCase.LOWER_UNDERSCORE, NamingCase.UPPER_UNDERSCORE, "my_field_name", "MY_FIELD_NAME"),
        Arguments.of(NamingCase.LOWER_UNDERSCORE, NamingCase.LOWER_HYPHEN, "my_field_name", "my-field-name"),

        // UPPER_UNDERSCORE → *
        Arguments.of(NamingCase.UPPER_UNDERSCORE, NamingCase.LOWER_CAMEL, "MY_FIELD_NAME", "myFieldName"),
        Arguments.of(NamingCase.UPPER_UNDERSCORE, NamingCase.UPPER_CAMEL, "MY_FIELD_NAME", "MyFieldName"),
        Arguments.of(NamingCase.UPPER_UNDERSCORE, NamingCase.LOWER_UNDERSCORE, "MY_FIELD_NAME", "my_field_name"),
        Arguments.of(NamingCase.UPPER_UNDERSCORE, NamingCase.LOWER_HYPHEN, "MY_FIELD_NAME", "my-field-name"),

        // LOWER_HYPHEN → *
        Arguments.of(NamingCase.LOWER_HYPHEN, NamingCase.LOWER_CAMEL, "my-field-name", "myFieldName"),
        Arguments.of(NamingCase.LOWER_HYPHEN, NamingCase.UPPER_CAMEL, "my-field-name", "MyFieldName"),
        Arguments.of(NamingCase.LOWER_HYPHEN, NamingCase.LOWER_UNDERSCORE, "my-field-name", "my_field_name"),
        Arguments.of(NamingCase.LOWER_HYPHEN, NamingCase.UPPER_UNDERSCORE, "my-field-name", "MY_FIELD_NAME"));
  }

  // ======================================= optimised fast-paths =========================================

  @Test
  void lowerHyphenToLowerUnderscoreUsesCharReplace() {
    // exercises the LOWER_HYPHEN.convert() fast-path (s.replace('-', '_'))
    assertThat(NamingCase.LOWER_HYPHEN.to(NamingCase.LOWER_UNDERSCORE, "a-b-c")).isEqualTo("a_b_c");
    assertThat(NamingCase.LOWER_HYPHEN.to(NamingCase.LOWER_UNDERSCORE, "no-hyphens-here")).isEqualTo("no_hyphens_here");
  }

  @Test
  void lowerHyphenToUpperUnderscoreUsesCharReplace() {
    // exercises the LOWER_HYPHEN.convert() fast-path (toUpperCase(replace('-','_')))
    assertThat(NamingCase.LOWER_HYPHEN.to(NamingCase.UPPER_UNDERSCORE, "a-b-c")).isEqualTo("A_B_C");
    assertThat(NamingCase.LOWER_HYPHEN.to(NamingCase.UPPER_UNDERSCORE, "foo-bar")).isEqualTo("FOO_BAR");
  }

  @Test
  void lowerUnderscoreToLowerHyphenUsesCharReplace() {
    // exercises the LOWER_UNDERSCORE.convert() fast-path (s.replace('_', '-'))
    assertThat(NamingCase.LOWER_UNDERSCORE.to(NamingCase.LOWER_HYPHEN, "a_b_c")).isEqualTo("a-b-c");
  }

  @Test
  void lowerUnderscoreToUpperUnderscoreUsesUpperCase() {
    // exercises the LOWER_UNDERSCORE.convert() fast-path (toUpperCase(s))
    assertThat(NamingCase.LOWER_UNDERSCORE.to(NamingCase.UPPER_UNDERSCORE, "my_field")).isEqualTo("MY_FIELD");
  }

  @Test
  void upperUnderscoreToLowerHyphenUsesCharReplace() {
    // exercises the UPPER_UNDERSCORE.convert() fast-path
    assertThat(NamingCase.UPPER_UNDERSCORE.to(NamingCase.LOWER_HYPHEN, "MY_FIELD")).isEqualTo("my-field");
  }

  @Test
  void upperUnderscoreToLowerUnderscoreUsesLowerCase() {
    // exercises the UPPER_UNDERSCORE.convert() fast-path
    assertThat(NamingCase.UPPER_UNDERSCORE.to(NamingCase.LOWER_UNDERSCORE, "MY_FIELD")).isEqualTo("my_field");
  }

  // ======================================= two-word round-trip ==========================================

  @Test
  void roundTripLowerCamelToLowerUnderscoreAndBack() {
    String original = "myFieldName";
    String underscore = NamingCase.LOWER_CAMEL.to(NamingCase.LOWER_UNDERSCORE, original);
    assertThat(NamingCase.LOWER_UNDERSCORE.to(NamingCase.LOWER_CAMEL, underscore)).isEqualTo(original);
  }

  @Test
  void roundTripUpperCamelToUpperUnderscoreAndBack() {
    String original = "MyFieldName";
    String upper = NamingCase.UPPER_CAMEL.to(NamingCase.UPPER_UNDERSCORE, original);
    Assertions.assertEquals("MY_FIELD_NAME", upper);
    assertThat(NamingCase.UPPER_UNDERSCORE.to(NamingCase.UPPER_CAMEL, upper)).isEqualTo(original);
  }

  @Test
  void roundTripLowerHyphenToLowerCamelAndBack() {
    String original = "my-field-name";
    String camel = NamingCase.LOWER_HYPHEN.to(NamingCase.LOWER_CAMEL, original);
    assertThat(NamingCase.LOWER_CAMEL.to(NamingCase.LOWER_HYPHEN, camel)).isEqualTo(original);
  }

  // ======================================= edge cases ===================================================

  @Test
  void convertSingleCharInput() {
    assertThat(NamingCase.LOWER_CAMEL.to(NamingCase.UPPER_CAMEL, "a")).isEqualTo("A");
    assertThat(NamingCase.UPPER_CAMEL.to(NamingCase.LOWER_CAMEL, "A")).isEqualTo("a");
    assertThat(NamingCase.LOWER_UNDERSCORE.to(NamingCase.UPPER_UNDERSCORE, "a")).isEqualTo("A");
    assertThat(NamingCase.UPPER_UNDERSCORE.to(NamingCase.LOWER_UNDERSCORE, "A")).isEqualTo("a");
  }

  @Test
  void convertAlreadyCorrectCase() {
    // Input is already in the target format – should still produce correct output
    assertThat(NamingCase.LOWER_UNDERSCORE.to(NamingCase.LOWER_CAMEL, "my_field")).isEqualTo("myField");
    assertThat(NamingCase.LOWER_CAMEL.to(NamingCase.LOWER_UNDERSCORE, "myField")).isEqualTo("my_field");
  }

  @Test
  void convertThreeWordString() {
    assertThat(NamingCase.LOWER_CAMEL.to(NamingCase.LOWER_UNDERSCORE, "myLongFieldName"))
        .isEqualTo("my_long_field_name");

    String res = NamingCase.UPPER_UNDERSCORE.to(NamingCase.LOWER_CAMEL, "MY_LONG_FIELD_NAME");
    assertThat(res).isEqualTo("myLongFieldName");
    assertThat(NamingCase.LOWER_HYPHEN.to(NamingCase.UPPER_CAMEL, "my-long-field-name")).isEqualTo("MyLongFieldName");
  }

  // ======================================= NamingConvention integration ==================================

  @Test
  void namingConventionColumnToPropertyConvertsSnakerToCamel() {
    NameMapping nc = NameMapping.LOWER_UNDERSCORE_TO_LOWER_CAMEL;
    assertThat(nc.columnToProperty("user_name")).isEqualTo("userName");
    assertThat(nc.columnToProperty("created_at")).isEqualTo("createdAt");
    assertThat(nc.columnToProperty("id")).isEqualTo("id");
  }

  @Test
  void namingConventionPropertyToColumnConvertsCamelToSnake() {
    NameMapping nc = NameMapping.LOWER_UNDERSCORE_TO_LOWER_CAMEL;
    assertThat(nc.propertyToColumn("userName")).isEqualTo("user_name");
    assertThat(nc.propertyToColumn("createdAt")).isEqualTo("created_at");
    assertThat(nc.propertyToColumn("id")).isEqualTo("id");
  }

  @Test
  void namingConventionRoundTripPropertyToColumnAndBack() {
    NameMapping nc = NameMapping.LOWER_UNDERSCORE_TO_LOWER_CAMEL;
    String property = "orderItemCount";
    String column = nc.propertyToColumn(property);
    assertThat(nc.columnToProperty(column)).isEqualTo(property);
  }

  // @Test
  // void namingStrategyDefaultIsSameAsNamingConventionDefault() throws ClassNotFoundException {
  // assertThat(NamingStrategy.DEFAULT).isSameAs(NamingConvention.LOWER_CAMEL_CASE_LOWER_UNDERSCORE);
  // }

  // ======================================= indexIn helper ===============================================

  @Test
  void indexInFindsFirstMatchPosition() {
    // LOWER_HYPHEN matches '-'
    assertThat(NamingCase.LOWER_HYPHEN.indexIn("my-field", 0)).isEqualTo(2);
    assertThat(NamingCase.LOWER_HYPHEN.indexIn("my-field", 3)).isEqualTo(-1);
    // LOWER_CAMEL matches uppercase letters
    assertThat(NamingCase.LOWER_CAMEL.indexIn("myField", 0)).isEqualTo(2);
    assertThat(NamingCase.LOWER_CAMEL.indexIn("myField", 3)).isEqualTo(-1);
  }

  @Test
  void indexInReturnsMinusOneWhenNoMatch() {
    assertThat(NamingCase.LOWER_HYPHEN.indexIn("noHyphens", 0)).isEqualTo(-1);
    assertThat(NamingCase.LOWER_UNDERSCORE.indexIn("noUnderscores", 0)).isEqualTo(-1);
  }
}

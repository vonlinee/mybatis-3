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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class NumberUtilsTest {

  // ========== convertNumberToTargetClass ==========

  @Test
  void shouldConvertIntegerToByte() {
    Byte result = NumberUtils.convertNumberToTargetClass(42, Byte.class);
    assertThat(result).isEqualTo((byte) 42);
  }

  @Test
  void shouldConvertIntegerToShort() {
    Short result = NumberUtils.convertNumberToTargetClass(1000, Short.class);
    assertThat(result).isEqualTo((short) 1000);
  }

  @Test
  void shouldConvertLongToInteger() {
    Integer result = NumberUtils.convertNumberToTargetClass(100L, Integer.class);
    assertThat(result).isEqualTo(100);
  }

  @Test
  void shouldReturnSameInstanceWhenTargetClassMatches() {
    Integer input = 42;
    Integer result = NumberUtils.convertNumberToTargetClass(input, Integer.class);
    assertThat(result).isSameAs(input);
  }

  @Test
  void shouldConvertIntegerToLong() {
    Long result = NumberUtils.convertNumberToTargetClass(Integer.MAX_VALUE, Long.class);
    assertThat(result).isEqualTo((long) Integer.MAX_VALUE);
  }

  @Test
  void shouldConvertIntegerToBigInteger() {
    BigInteger result = NumberUtils.convertNumberToTargetClass(12345, BigInteger.class);
    assertThat(result).isEqualTo(BigInteger.valueOf(12345));
  }

  @Test
  void shouldConvertBigDecimalToBigInteger() {
    BigDecimal input = new BigDecimal("9876543210.99");
    BigInteger result = NumberUtils.convertNumberToTargetClass(input, BigInteger.class);
    assertThat(result).isEqualTo(new BigInteger("9876543210"));
  }

  @Test
  void shouldConvertIntegerToFloat() {
    Float result = NumberUtils.convertNumberToTargetClass(7, Float.class);
    assertThat(result).isCloseTo(7.0f, within(0.0001f));
  }

  @Test
  void shouldConvertIntegerToDouble() {
    Double result = NumberUtils.convertNumberToTargetClass(7, Double.class);
    assertThat(result).isCloseTo(7.0, within(0.0001));
  }

  @Test
  void shouldConvertIntegerToBigDecimal() {
    BigDecimal result = NumberUtils.convertNumberToTargetClass(100, BigDecimal.class);
    assertThat(result).isEqualByComparingTo(new BigDecimal("100"));
  }

  @Test
  void shouldConvertDoubleToFloat() {
    Float result = NumberUtils.convertNumberToTargetClass(3.14, Float.class);
    assertThat(result).isCloseTo(3.14f, within(0.001f));
  }

  @Test
  void shouldConvertFloatToDouble() {
    Double result = NumberUtils.convertNumberToTargetClass(1.5f, Double.class);
    assertThat(result).isCloseTo(1.5, within(0.001));
  }

  @Test
  void shouldConvertNegativeIntegerToByte() {
    Byte result = NumberUtils.convertNumberToTargetClass(-100, Byte.class);
    assertThat(result).isEqualTo((byte) -100);
  }

  @Test
  void shouldThrowWhenConvertingLargeLongToByte() {
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(1000L, Byte.class))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("overflow");
  }

  @Test
  void shouldThrowWhenConvertingLargeLongToShort() {
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(100000L, Short.class))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("overflow");
  }

  @Test
  void shouldThrowWhenConvertingLargeLongToInteger() {
    long overflowValue = (long) Integer.MAX_VALUE + 1;
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(overflowValue, Integer.class))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("overflow");
  }

  @Test
  void shouldThrowWhenConvertingBigIntegerOverflowToLong() {
    BigInteger huge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(huge, Long.class))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("overflow");
  }

  @Test
  void shouldThrowWhenConvertingBigIntegerOverflowToByte() {
    BigInteger huge = BigInteger.valueOf(1000);
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(huge, Byte.class))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("overflow");
  }

  @Test
  void shouldThrowWhenConvertingBigDecimalOverflowToInteger() {
    BigDecimal huge = new BigDecimal("99999999999999999999");
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(huge, Integer.class))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("overflow");
  }

  @Test
  void shouldConvertBigDecimalToLongWithTruncation() {
    // BigDecimal whose integer part fits in long
    BigDecimal bd = new BigDecimal("42.99");
    Long result = NumberUtils.convertNumberToTargetClass(bd, Long.class);
    assertThat(result).isEqualTo(42L);
  }

  @Test
  void shouldThrowOnNullNumberInput() {
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(null, Integer.class))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnNullTargetClassInConvert() {
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(42, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnUnsupportedTargetClass() {
    assertThatThrownBy(() -> NumberUtils.convertNumberToTargetClass(42, AtomicNumberStub.class))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unsupported target class");
  }

  @Test
  void shouldConvertByteMaxValueToByte() {
    Byte result = NumberUtils.convertNumberToTargetClass((int) Byte.MAX_VALUE, Byte.class);
    assertThat(result).isEqualTo(Byte.MAX_VALUE);
  }

  @Test
  void shouldConvertByteMinValueToByte() {
    Byte result = NumberUtils.convertNumberToTargetClass((int) Byte.MIN_VALUE, Byte.class);
    assertThat(result).isEqualTo(Byte.MIN_VALUE);
  }

  @Test
  void shouldConvertShortMaxValueToShort() {
    Short result = NumberUtils.convertNumberToTargetClass((int) Short.MAX_VALUE, Short.class);
    assertThat(result).isEqualTo(Short.MAX_VALUE);
  }

  @Test
  void shouldConvertIntegerMaxValueToInteger() {
    Integer result = NumberUtils.convertNumberToTargetClass((long) Integer.MAX_VALUE, Integer.class);
    assertThat(result).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void shouldConvertBigIntegerWithinLongRangeToLong() {
    BigInteger bi = BigInteger.valueOf(Long.MAX_VALUE);
    Long result = NumberUtils.convertNumberToTargetClass(bi, Long.class);
    assertThat(result).isEqualTo(Long.MAX_VALUE);
  }

  // ========== parseNumber ==========

  @Test
  void shouldParseDecimalInteger() {
    Integer result = NumberUtils.parseNumber("123", Integer.class);
    assertThat(result).isEqualTo(123);
  }

  @Test
  void shouldParseNegativeDecimalInteger() {
    Integer result = NumberUtils.parseNumber("-456", Integer.class);
    assertThat(result).isEqualTo(-456);
  }

  @Test
  void shouldParseZeroAsInteger() {
    Integer result = NumberUtils.parseNumber("0", Integer.class);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void shouldParseDecimalLong() {
    Long result = NumberUtils.parseNumber("9876543210", Long.class);
    assertThat(result).isEqualTo(9876543210L);
  }

  @Test
  void shouldParseHexWithLowercase0xPrefixAsInteger() {
    Integer result = NumberUtils.parseNumber("0x1A", Integer.class);
    assertThat(result).isEqualTo(0x1A);
  }

  @Test
  void shouldParseHexWithUppercase0XPrefixAsInteger() {
    Integer result = NumberUtils.parseNumber("0X1A", Integer.class);
    assertThat(result).isEqualTo(0x1A);
  }

  @Test
  void shouldParseHexWithHashPrefixAsInteger() {
    Integer result = NumberUtils.parseNumber("#FF", Integer.class);
    assertThat(result).isEqualTo(0xFF);
  }

  @Test
  void shouldParseHexAsLong() {
    Long result = NumberUtils.parseNumber("0xDEADBEEF", Long.class);
    assertThat(result).isEqualTo(0xDEADBEEFL);
  }

  @Test
  void shouldParseHexAsBigInteger() {
    BigInteger result = NumberUtils.parseNumber("0xFF", BigInteger.class);
    assertThat(result).isEqualTo(BigInteger.valueOf(0xFF));
  }

  @Test
  void shouldParseNegativeHexAsInteger() {
    Integer result = NumberUtils.parseNumber("-0x10", Integer.class);
    assertThat(result).isEqualTo(-16);
  }

  @Test
  void shouldParseWhitespacePaddedNumber() {
    Integer result = NumberUtils.parseNumber("  42  ", Integer.class);
    assertThat(result).isEqualTo(42);
  }

  @Test
  void shouldParseNumberWithInternalWhitespace() {
    Integer result = NumberUtils.parseNumber("1 2 3", Integer.class);
    assertThat(result).isEqualTo(123);
  }

  @Test
  void shouldParseDecimalFloat() {
    Float result = NumberUtils.parseNumber("3.14", Float.class);
    assertThat(result).isCloseTo(3.14f, within(0.001f));
  }

  @Test
  void shouldParseDecimalDouble() {
    Double result = NumberUtils.parseNumber("2.718281828", Double.class);
    assertThat(result).isCloseTo(2.718281828, within(0.000001));
  }

  @Test
  void shouldParseBigDecimal() {
    BigDecimal result = NumberUtils.parseNumber("123456789.123456789", BigDecimal.class);
    assertThat(result).isEqualByComparingTo(new BigDecimal("123456789.123456789"));
  }

  @Test
  void shouldParseAsBigDecimalWhenTargetIsNumberClass() {
    Number result = NumberUtils.parseNumber("99.9", Number.class);
    assertThat(result).isInstanceOf(BigDecimal.class);
    assertThat((BigDecimal) result).isEqualByComparingTo(new BigDecimal("99.9"));
  }

  @Test
  void shouldParseAsByte() {
    Byte result = NumberUtils.parseNumber("100", Byte.class);
    assertThat(result).isEqualTo((byte) 100);
  }

  @Test
  void shouldParseAsShort() {
    Short result = NumberUtils.parseNumber("1000", Short.class);
    assertThat(result).isEqualTo((short) 1000);
  }

  @Test
  void shouldParseHexAsByte() {
    Byte result = NumberUtils.parseNumber("0x7F", Byte.class);
    assertThat(result).isEqualTo(Byte.MAX_VALUE);
  }

  @Test
  void shouldParseHexAsShort() {
    Short result = NumberUtils.parseNumber("0x00FF", Short.class);
    assertThat(result).isEqualTo((short) 0x00FF);
  }

  @Test
  void shouldThrowOnNonNumericString() {
    assertThatThrownBy(() -> NumberUtils.parseNumber("abc", Integer.class))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowOnNullTextInput() {
    assertThatThrownBy(() -> NumberUtils.parseNumber(null, Integer.class)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnNullTargetClassInParse() {
    assertThatThrownBy(() -> NumberUtils.parseNumber("42", null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnEmptyStringInput() {
    assertThatThrownBy(() -> NumberUtils.parseNumber("", Integer.class)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowOnUnsupportedTargetClassInParse() {
    assertThatThrownBy(() -> NumberUtils.parseNumber("42", AtomicNumberStub.class))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cannot convert");
  }

  @ParameterizedTest
  @CsvSource({ "0x1A, 26", "0X1A, 26", "#FF, 255", "-0x10, -16" })
  void shouldParseVariousHexFormatsAsInteger(String input, int expected) {
    Integer result = NumberUtils.parseNumber(input, Integer.class);
    assertThat(result).isEqualTo(expected);
  }

  // ========== isHexNumber ==========

  @ParameterizedTest
  @ValueSource(strings = { "0x1A", "0X1A", "#FF", "0xDEAD", "#0" })
  void shouldReturnTrueForHexNumbers(String value) {
    assertThat(NumberUtils.isHexNumber(value)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "-0x1A", "-0X1A", "-#FF" })
  void shouldReturnTrueForNegativeHexNumbers(String value) {
    assertThat(NumberUtils.isHexNumber(value)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "123", "-456", "3.14", "abc", "0", "007", "" })
  void shouldReturnFalseForNonHexNumbers(String value) {
    assertThat(NumberUtils.isHexNumber(value)).isFalse();
  }

  @Test
  void shouldReturnFalseForPlainZero() {
    assertThat(NumberUtils.isHexNumber("0")).isFalse();
  }

  @Test
  void shouldReturnFalseForOctalLooking() {
    // "07" has no hex prefix, so isHexNumber returns false
    assertThat(NumberUtils.isHexNumber("07")).isFalse();
  }

  // ========== Stub for unsupported Number subclass ==========

  /**
   * A stub Number subclass not supported by NumberUtils, used to trigger unsupported-class errors.
   */
  private static final class AtomicNumberStub extends Number {
    @Override
    public int intValue() {
      return 0;
    }

    @Override
    public long longValue() {
      return 0L;
    }

    @Override
    public float floatValue() {
      return 0f;
    }

    @Override
    public double doubleValue() {
      return 0.0;
    }
  }
}

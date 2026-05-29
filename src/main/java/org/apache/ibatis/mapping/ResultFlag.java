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

/**
 * Bit-mask flags describing the role of a {@link ResultMapping} entry.
 * <p>
 * Each constant carries a unique, non-overlapping {@code int} mask, so multiple flags can be combined into a single
 * {@code int} via bitwise OR (for example, {@code ResultFlag.ID.mask() | ResultFlag.CONSTRUCTOR.mask()} marks an
 * argument as both an ID and a constructor parameter). Use {@link #of(ResultFlag...)} to build a mask from individual
 * flags and {@link #has(int, ResultFlag)} to test for a specific flag. Masks are declared as hex literals ({@code 0x1},
 * {@code 0x2}, ...) to make the bit pattern visually obvious.
 *
 * @author Clinton Begin
 */
public enum ResultFlag {
  /**
   * Marks the mapping as part of the row identifier (primary key columns).
   */
  @SuppressWarnings("PointlessBitwiseExpression")
  ID(1 << 0), // 0x1
  /**
   * Marks the mapping as a constructor argument used when instantiating the result object.
   */
  CONSTRUCTOR(1 << 1) // 0x2
  ;

  /**
   * Empty mask representing "no flags set".
   */
  public static final int NONE = 0;

  private final int mask;

  ResultFlag(int mask) {
    this.mask = mask;
  }

  /**
   * @return the unique bit-mask for this flag
   */
  public int mask() {
    return mask;
  }

  /**
   * Combines the given flags into a single bit-mask.
   *
   * @param flags
   *          flags to combine; may be empty
   *
   * @return the OR-combined mask, or {@link #NONE} if {@code flags} is empty
   */
  public static int of(ResultFlag... flags) {
    int result = NONE;
    if (flags != null) {
      for (ResultFlag flag : flags) {
        result |= flag.mask;
      }
    }
    return result;
  }

  /**
   * Returns a new mask with the given {@code flag} added (set).
   *
   * @param mask
   *          the existing bit-mask
   * @param flag
   *          the flag to add
   *
   * @return {@code mask | flag.mask()}
   */
  public static int add(int mask, ResultFlag flag) {
    return mask | flag.mask;
  }

  /**
   * Tests whether the given {@code mask} has the specified {@code flag} set.
   *
   * @param mask
   *          the bit-mask to test
   * @param flag
   *          the flag to check
   *
   * @return {@code true} if the flag bit is set in {@code mask}
   */
  public static boolean has(int mask, ResultFlag flag) {
    return (mask & flag.mask) != 0;
  }
}

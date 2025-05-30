/*
 *    Copyright 2009-2024 the original author or authors.
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
package org.apache.ibatis.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.ibatis.internal.util.ObjectUtils;
import org.junit.jupiter.api.Test;

class ArrayUtilTest {

  @Test
  void testHashCode() {
    Object arr = new long[] { 1 };
    assertEquals(Arrays.hashCode((long[]) arr), ObjectUtils.hashCode(arr));
    arr = new int[] { 1 };
    assertEquals(Arrays.hashCode((int[]) arr), ObjectUtils.hashCode(arr));
    arr = new short[] { 1 };
    assertEquals(Arrays.hashCode((short[]) arr), ObjectUtils.hashCode(arr));
    arr = new char[] { 1 };
    assertEquals(Arrays.hashCode((char[]) arr), ObjectUtils.hashCode(arr));
    arr = new byte[] { 1 };
    assertEquals(Arrays.hashCode((byte[]) arr), ObjectUtils.hashCode(arr));
    arr = new boolean[] { true };
    assertEquals(Arrays.hashCode((boolean[]) arr), ObjectUtils.hashCode(arr));
    arr = new float[] { 1f };
    assertEquals(Arrays.hashCode((float[]) arr), ObjectUtils.hashCode(arr));
    arr = new double[] { 1d };
    assertEquals(Arrays.hashCode((double[]) arr), ObjectUtils.hashCode(arr));
    arr = new Object[] { "str" };
    assertEquals(Arrays.hashCode((Object[]) arr), ObjectUtils.hashCode(arr));

    assertEquals(0, ObjectUtils.hashCode(null));
    assertEquals("str".hashCode(), ObjectUtils.hashCode("str"));
    assertEquals(Integer.valueOf(1).hashCode(), ObjectUtils.hashCode(1));
  }

  @Test
  void testequals() {
    assertTrue(ObjectUtils.equals(new long[] { 1 }, new long[] { 1 }));
    assertTrue(ObjectUtils.equals(new int[] { 1 }, new int[] { 1 }));
    assertTrue(ObjectUtils.equals(new short[] { 1 }, new short[] { 1 }));
    assertTrue(ObjectUtils.equals(new char[] { 1 }, new char[] { 1 }));
    assertTrue(ObjectUtils.equals(new byte[] { 1 }, new byte[] { 1 }));
    assertTrue(ObjectUtils.equals(new boolean[] { true }, new boolean[] { true }));
    assertTrue(ObjectUtils.equals(new float[] { 1f }, new float[] { 1f }));
    assertTrue(ObjectUtils.equals(new double[] { 1d }, new double[] { 1d }));
    assertTrue(ObjectUtils.equals(new Object[] { "str" }, new Object[] { "str" }));

    assertFalse(ObjectUtils.equals(new long[] { 1 }, new long[] { 2 }));
    assertFalse(ObjectUtils.equals(new int[] { 1 }, new int[] { 2 }));
    assertFalse(ObjectUtils.equals(new short[] { 1 }, new short[] { 2 }));
    assertFalse(ObjectUtils.equals(new char[] { 1 }, new char[] { 2 }));
    assertFalse(ObjectUtils.equals(new byte[] { 1 }, new byte[] { 2 }));
    assertFalse(ObjectUtils.equals(new boolean[] { true }, new boolean[] { false }));
    assertFalse(ObjectUtils.equals(new float[] { 1f }, new float[] { 2f }));
    assertFalse(ObjectUtils.equals(new double[] { 1d }, new double[] { 2d }));
    assertFalse(ObjectUtils.equals(new Object[] { "str" }, new Object[] { "rts" }));

    assertTrue(ObjectUtils.equals(null, null));
    assertFalse(ObjectUtils.equals(new long[] { 1 }, null));
    assertFalse(ObjectUtils.equals(null, new long[] { 1 }));

    assertTrue(ObjectUtils.equals(1, 1));
    assertTrue(ObjectUtils.equals("str", "str"));
  }

  @Test
  void testToString() {
    Object arr = new long[] { 1 };
    assertEquals(Arrays.toString((long[]) arr), ObjectUtils.toString(arr));
    arr = new int[] { 1 };
    assertEquals(Arrays.toString((int[]) arr), ObjectUtils.toString(arr));
    arr = new short[] { 1 };
    assertEquals(Arrays.toString((short[]) arr), ObjectUtils.toString(arr));
    arr = new char[] { 1 };
    assertEquals(Arrays.toString((char[]) arr), ObjectUtils.toString(arr));
    arr = new byte[] { 1 };
    assertEquals(Arrays.toString((byte[]) arr), ObjectUtils.toString(arr));
    arr = new boolean[] { true };
    assertEquals(Arrays.toString((boolean[]) arr), ObjectUtils.toString(arr));
    arr = new float[] { 1f };
    assertEquals(Arrays.toString((float[]) arr), ObjectUtils.toString(arr));
    arr = new double[] { 1d };
    assertEquals(Arrays.toString((double[]) arr), ObjectUtils.toString(arr));
    arr = new Object[] { "str" };
    assertEquals(Arrays.toString((Object[]) arr), ObjectUtils.toString(arr));

    assertEquals(Integer.toString(1), ObjectUtils.toString(1));
    assertEquals("null", ObjectUtils.toString(null));
  }

}

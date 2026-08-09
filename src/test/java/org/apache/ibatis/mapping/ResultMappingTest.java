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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResultMappingTest {

  // Issue 697: Association with both a resultMap and a select attribute should throw exception
  @Test
  void shouldThrowErrorWhenBothResultMapAndNestedSelectAreSet() {
    assertThrows(IllegalStateException.class, () -> new ResultMapping.Builder("prop", false)
        .nestedQueryId("nested query ID").nestedResultMapId("nested resultMap").build());
  }

  // Issue 4: column is mandatory on nested queries
  @Test
  void shouldFailWithAMissingColumnInNestedSelect() {
    assertThrows(IllegalStateException.class,
        () -> new ResultMapping.Builder("prop", false).nestedQueryId("nested query ID").build());
  }

  @Test
  void shouldFailIfSizeOfColumnsAndForeignColumnsDontMatch() {
    IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
        () -> new ResultMapping.Builder("books", false).resultSet("bookRS").column("id,x").foreignColumn("author_id")
            .nestedResultMapId("bookRM").build());
    assertEquals("There should be the same number of columns and foreignColumns in property books", ex.getMessage());
  }

  @Test
  void shouldNestedCursorNotRequireForeignColumns() {
    assertNotNull(new ResultMapping.Builder("books", false).jdbcType(JdbcType.CURSOR).nestedResultMapId("bookRM")
        .column("books").build());
  }

  @Test
  void shouldUseEmptyCollection() {
    {
      // @formatter:off
      ResultMap resultMap = ResultMap.builder(new Configuration(), "", HashMap.class)
        .addMapping("id", "id", int.class)
        .build();
      // @formatter:on

      List<Integer> singletonList = Collections.singletonList(1);

      Assertions.assertSame(resultMap.getIdResultMappings().getClass(), singletonList.getClass());
      Assertions.assertSame(resultMap.getResultMappings().getClass(), singletonList.getClass());
      Assertions.assertSame(resultMap.getPropertyResultMappings().getClass(), singletonList.getClass());
      Assertions.assertSame(resultMap.getConstructorResultMappings(), Collections.emptyList());

      Assertions.assertSame(resultMap.getMappedColumns().getClass(), Collections.singleton(1).getClass());
      Assertions.assertSame(resultMap.getMappedProperties().getClass(), Collections.singleton(1).getClass());
    }

    {
      // @formatter:off
      ResultMap resultMap = ResultMap.create("", HashMap.class);
      Assertions.assertSame(resultMap.getIdResultMappings(), Collections.emptyList());
      Assertions.assertSame(resultMap.getResultMappings(), Collections.emptyList());
      Assertions.assertSame(resultMap.getPropertyResultMappings(), Collections.emptyList());
      Assertions.assertSame(resultMap.getConstructorResultMappings(), Collections.emptyList());
      Assertions.assertSame(resultMap.getMappedColumns(), Collections.emptySet());
      Assertions.assertSame(resultMap.getMappedProperties(), Collections.emptySet());
    }
  }

  // ---------------------------------------------------------------------------
  // ResultFlag bit-enum tests
  // ---------------------------------------------------------------------------

  @Test
  void shouldDefineUniquePowerOfTwoMasksForEachFlag() {
    int idMask = ResultFlag.ID.mask();
    int ctorMask = ResultFlag.CONSTRUCTOR.mask();

    assertEquals(0x1, idMask);
    assertEquals(0x2, ctorMask);
    // each mask must be a single bit (power of two)
    assertEquals(0, idMask & (idMask - 1));
    assertEquals(0, ctorMask & (ctorMask - 1));
    // masks must not overlap
    assertEquals(0, idMask & ctorMask);
    assertEquals(0, ResultFlag.NONE);
  }

  @Test
  void shouldCombineFlagsViaBitwiseOr() {
    assertEquals(ResultFlag.NONE, ResultFlag.of());
    assertEquals(ResultFlag.NONE, ResultFlag.of((ResultFlag[]) null));
    assertEquals(ResultFlag.ID.mask(), ResultFlag.of(ResultFlag.ID));
    assertEquals(ResultFlag.ID.mask() | ResultFlag.CONSTRUCTOR.mask(),
        ResultFlag.of(ResultFlag.ID, ResultFlag.CONSTRUCTOR));
    // idempotent: passing the same flag twice yields the same mask
    assertEquals(ResultFlag.ID.mask(), ResultFlag.of(ResultFlag.ID, ResultFlag.ID));
  }

  @Test
  void shouldSetBitWithoutClearingOthersWhenAddingFlag() {
    int mask = ResultFlag.NONE;
    mask = ResultFlag.add(mask, ResultFlag.ID);
    assertEquals(ResultFlag.ID.mask(), mask);

    mask = ResultFlag.add(mask, ResultFlag.CONSTRUCTOR);
    assertEquals(ResultFlag.ID.mask() | ResultFlag.CONSTRUCTOR.mask(), mask);

    // adding an already-set flag is a no-op
    int sameMask = ResultFlag.add(mask, ResultFlag.ID);
    assertEquals(mask, sameMask);
  }

  @Test
  void shouldDetectSetBitsWithHas() {
    assertFalse(ResultFlag.has(ResultFlag.NONE, ResultFlag.ID));
    assertFalse(ResultFlag.has(ResultFlag.NONE, ResultFlag.CONSTRUCTOR));

    int idOnly = ResultFlag.ID.mask();
    assertTrue(ResultFlag.has(idOnly, ResultFlag.ID));
    assertFalse(ResultFlag.has(idOnly, ResultFlag.CONSTRUCTOR));

    int combined = ResultFlag.of(ResultFlag.ID, ResultFlag.CONSTRUCTOR);
    assertTrue(ResultFlag.has(combined, ResultFlag.ID));
    assertTrue(ResultFlag.has(combined, ResultFlag.CONSTRUCTOR));
  }

  @Test
  void shouldDefaultFlagsToNoneWhenNotSet() {
    ResultMapping rm = new ResultMapping.Builder("id", false, "id", int.class).build();
    assertEquals(ResultFlag.NONE, rm.getFlags());
    assertFalse(rm.hasFlag(ResultFlag.ID));
    assertFalse(rm.hasFlag(ResultFlag.CONSTRUCTOR));
  }

  @Test
  void shouldStoreIntFlagMaskOnResultMapping() {
    int mask = ResultFlag.of(ResultFlag.ID, ResultFlag.CONSTRUCTOR);
    ResultMapping rm = new ResultMapping.Builder("id", false, "id", int.class).flags(mask).build();

    assertEquals(mask, rm.getFlags());
    assertTrue(rm.hasFlag(ResultFlag.ID));
    assertTrue(rm.hasFlag(ResultFlag.CONSTRUCTOR));
  }

  @Test
  void shouldDelegateHasFlagToStaticHelper() {
    ResultMapping rm = new ResultMapping.Builder("id", false, "id", int.class)
        .flags(ResultFlag.ID.mask()).build();

    assertEquals(ResultFlag.has(rm.getFlags(), ResultFlag.ID), rm.hasFlag(ResultFlag.ID));
    assertEquals(ResultFlag.has(rm.getFlags(), ResultFlag.CONSTRUCTOR), rm.hasFlag(ResultFlag.CONSTRUCTOR));
  }

  @Test
  void shouldClassifyResultMappingsByBitMaskFlags() {
    Configuration config = new Configuration();
    // @formatter:off
    ResultMap resultMap = ResultMap.builder(config, "rm", HashMap.class)
        .addMapping("id",   "id",   int.class,    ResultFlag.ID)
        .addMapping("name", "name", String.class, ResultFlag.CONSTRUCTOR)
        .addMapping("pk",   "pk",   int.class,    ResultFlag.ID, ResultFlag.CONSTRUCTOR)
        .addMapping("note", "note", String.class)
        .build();
    // @formatter:on

    // 2 id mappings: "id" and "pk"
    assertEquals(2, resultMap.getIdResultMappings().size());
    // 2 constructor mappings: "name" and "pk"
    assertEquals(2, resultMap.getConstructorResultMappings().size());
    // remaining property mappings: "id" and "note" (note: "pk" is constructor, not property)
    assertEquals(2, resultMap.getPropertyResultMappings().size());
  }
}

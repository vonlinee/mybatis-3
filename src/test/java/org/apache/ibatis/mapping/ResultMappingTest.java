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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultMappingTest {
  @Mock
  private Configuration configuration;

  // Issue 697: Association with both a resultMap and a select attribute should throw exception
  @Test
  void shouldThrowErrorWhenBothResultMapAndNestedSelectAreSet() {
    assertThrows(IllegalStateException.class, () -> new ResultMapping.Builder(configuration, "prop")
        .nestedQueryId("nested query ID").nestedResultMapId("nested resultMap").build());
  }

  // Issue 4: column is mandatory on nested queries
  @Test
  void shouldFailWithAMissingColumnInNestedSelect() {
    assertThrows(IllegalStateException.class,
        () -> new ResultMapping.Builder(configuration, "prop").nestedQueryId("nested query ID").build());
  }

  @Test
  void shouldFailIfSizeOfColumnsAndForeignColumnsDontMatch() {
    IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
        () -> new ResultMapping.Builder(configuration, "books").resultSet("bookRS").column("id,x")
            .foreignColumn("author_id").nestedResultMapId("bookRM").build());
    assertEquals("There should be the same number of columns and foreignColumns in property books", ex.getMessage());
  }

  @Test
  void shouldNestedCursorNotRequireForeignColumns() {
    assertNotNull(new ResultMapping.Builder(configuration, "books").jdbcType(JdbcType.CURSOR)
        .nestedResultMapId("bookRM").column("books").build());
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
}

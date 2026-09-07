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
package org.apache.ibatis.extension.sql.dsl;

import static org.apache.ibatis.extension.sql.dsl.SqlDsl.column;
import static org.apache.ibatis.extension.sql.dsl.SqlDsl.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.persistence.Column;

import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class SqlDslEdgeCaseTest {

  private final Configuration configuration = new Configuration();
  private final SqlDsl dsl = SqlDsl.using(configuration);

  private static String sql(String text) {
    return text.replaceAll("\\s+", " ").replaceAll("\\( ", "(").replaceAll(" \\)", ")").trim();
  }

  // @formatter:off
  @Test
  void falseBranchesAreConstructedButNotRendered() {
    Table<PropertyEntity> p = table(PropertyEntity.class).as("p");

    assertEquals(sql("""
        SELECT p.id
        FROM property_entity p
        WHERE p.id IS NOT NULL
        """),
        dsl.select(
            dsl.when(false, column(p, PropertyEntity::getValue)),
            column(p, PropertyEntity::getId))
            .from(p)
            .where(
                dsl.when(false, column(p, PropertyEntity::getValue).eq(null))
                    .or(column(p, PropertyEntity::getId).isNotNull()))
            .getSql(ParamType.INLINED));
    assertEquals(sql("""
        SELECT p.id
        FROM property_entity p
        WHERE p.id IS NOT NULL
        """),
        dsl.select(column(p, PropertyEntity::getId))
            .from(p)
            .where(
                dsl.when(false, column(p, PropertyEntity::getValue).eq(null))
                    .not()
                    .or(column(p, PropertyEntity::getId).isNotNull()))
            .getSql(ParamType.INLINED));
  }

  @Test
  void keptNullComparisonAndEmptyInAreRejected() {
    Table<PropertyEntity> p = table(PropertyEntity.class).as("p");

    assertThrows(
        IllegalArgumentException.class,
        () -> dsl.select()
            .from(p)
            .where(column(p, PropertyEntity::getValue).eq(null))
            .getSql(ParamType.INLINED));
    assertThrows(IllegalArgumentException.class,
        () -> dsl.select()
            .from(p)
            .where(column(p, PropertyEntity::getId).in())
            .getSql(ParamType.INLINED));
  }

  @Test
  void missingDmlWhereIsAllowedButInvalidShapeIsRejected() {
    Table<PropertyEntity> p = table(PropertyEntity.class).as("p");
    assertEquals(sql("""
        UPDATE property_entity p
        SET p.db_value = 'x'
        """),
        dsl.update(p)
            .set(column(p, PropertyEntity::getValue), "x")
            .getSql(ParamType.INLINED));
    assertEquals(sql("""
        DELETE FROM property_entity p
        """),
        dsl.deleteFrom(p)
            .getSql(ParamType.INLINED));
    assertThrows(IllegalStateException.class, () -> dsl.update(p).getSql(ParamType.INLINED));
    assertThrows(IllegalStateException.class, () -> dsl.insertInto(p).getSql(ParamType.INLINED));
  }

  @Test
  void supportsGetterColumnAnnotationWithoutAddingDslAnnotations() {
    Table<PropertyEntity> p = table(PropertyEntity.class).as("p");
    assertEquals(sql("""
        SELECT p.db_value
        FROM property_entity p
        """),
        dsl.select(column(p, PropertyEntity::getValue))
            .from(p)
            .getSql(ParamType.INLINED));
  }
  // @formatter:on

  @javax.persistence.Table(name = "property_entity")
  public static class PropertyEntity {
    private Integer id;
    private String value;

    public Integer getId() {
      return id;
    }

    @Column(name = "db_value")
    public String getValue() {
      return value;
    }
  }
}

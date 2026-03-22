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
package org.apache.ibatis.extension;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.ibatis.extension.metadata.TableInfo;

/**
 * A Composite Parser that acts as a Factory/Delegate. It checks if the class is a JPA entity; if so, uses
 * JpaTableInfoParser. Otherwise, falls back to SimplePojoTableInfoParser.
 */
public class CompositeTableInfoParser implements TableInfoParser {

  private final TableInfoParser jpaParser = new JpaTableInfoParser();
  private final TableInfoParser pojoParser = new SimplePojoTableInfoParser();

  @Override
  public TableInfo parse(Class<?> clazz) {
    if (isJpaEntity(clazz)) {
      return jpaParser.parse(clazz);
    }
    return pojoParser.parse(clazz);
  }

  /**
   * Detection logic for JPA entities.
   */
  private boolean isJpaEntity(Class<?> clazz) {
    return clazz.isAnnotationPresent(Table.class) || clazz.isAnnotationPresent(Entity.class);
  }
}

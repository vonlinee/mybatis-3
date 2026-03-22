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
package org.apache.ibatis.extension.metadata;

import org.jetbrains.annotations.Nullable;

/**
 * The table types.
 */
public enum TableType {

  /**
   * The table type name for global temporary tables.
   */
  GLOBAL_TEMPORARY,

  /**
   * The table type name for local temporary tables.
   */
  LOCAL_TEMPORARY,

  /**
   * The table type name for linked tables.
   */
  TABLE_LINK,

  /**
   * The table type name for system tables. (aka. MetaTable)
   */
  SYSTEM_TABLE,

  /**
   * The table type name for regular data tables.
   */
  TABLE,

  /**
   * The table type name for views.
   */
  VIEW,

  /**
   * The table type name for table alias.
   */
  ALIAS,

  /**
   * The table type name for SYNONYM.
   */
  SYNONYM,

  /**
   * The table type name for external table engines.
   */
  EXTERNAL_TABLE_ENGINE,

  /**
   * The table type name for materialized views.
   */
  MATERIALIZED_VIEW;

  @Override
  public String toString() {
    if (this == EXTERNAL_TABLE_ENGINE) {
      return "EXTERNAL";
    } else if (this == SYSTEM_TABLE) {
      return "SYSTEM TABLE";
    } else if (this == TABLE_LINK) {
      return "TABLE LINK";
    } else if (this == MATERIALIZED_VIEW) {
      return "MATERIALIZED VIEW";
    } else {
      return super.toString();
    }
  }

  @Nullable
  public static TableType lookup(@Nullable String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    name = name.trim().replace(" ", "_");
    for (TableType type : values()) {
      if (type.name().equalsIgnoreCase(name)) {
        return type;
      }
    }
    return null;
  }
}

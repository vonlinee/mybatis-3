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

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.mapping.ParameterMapping;

public final class RenderedSql {

  private final String sql;
  private final List<ParameterMapping> parameterMappings;
  private final List<Object> parameterValues;

  public RenderedSql(String sql, List<ParameterMapping> parameterMappings, List<Object> parameterValues) {
    this.sql = sql;
    this.parameterMappings = Collections.unmodifiableList(parameterMappings);
    this.parameterValues = Collections.unmodifiableList(parameterValues);
  }

  public String sql() {
    return sql;
  }

  public List<ParameterMapping> parameterMappings() {
    return parameterMappings;
  }

  public List<Object> parameterValues() {
    return parameterValues;
  }

  @Override
  public String toString() {
    return sql;
  }
}

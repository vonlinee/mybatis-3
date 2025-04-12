/*
 *    Copyright 2009-2025 the original author or authors.
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
package org.apache.ibatis.scripting.defaults;

import java.util.Collections;

import org.apache.ibatis.scripting.BoundSql;
import org.apache.ibatis.scripting.SqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;

/**
 * Static SqlSource. It is faster than {@link DynamicSqlSource} because mappings are calculated during startup.
 *
 * @author Eduardo Macarron
 *
 * @since 3.2.0
 */
public class RawSqlSource implements SqlSource {

  private final String rawSql;

  public RawSqlSource(String sql) {
    this.rawSql = sql;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return new BoundSql(rawSql, Collections.emptyList(), parameterObject);
  }
}

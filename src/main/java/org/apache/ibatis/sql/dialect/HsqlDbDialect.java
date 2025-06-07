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
package org.apache.ibatis.sql.dialect;

public class HsqlDbDialect implements SQLDialect {

  public static final HsqlDbDialect INSTANCE = new HsqlDbDialect();

  protected HsqlDbDialect() {
  }

  private static final LimitClause LIMIT_CLAUSE = new LimitClause() {

    @Override
    public String getLimitSql(long limit) {
      return "LIMIT " + limit;
    }

    @Override
    public String getOffsetSql(long offset) {
      return "OFFSET " + offset;
    }

    @Override
    public String getLimitOffsetSql(long limit, long offset) {
      return getOffsetSql(offset) + " " + getLimitSql(limit);
    }
  };

  @Override
  public LimitClause getLimitOffsetClause() {
    return LIMIT_CLAUSE;
  }
}

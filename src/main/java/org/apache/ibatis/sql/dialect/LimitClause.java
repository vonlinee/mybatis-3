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

/**
 * A clause representing Dialect-specific {@code LIMIT}.
 */
public interface LimitClause {

  LimitClause DEFAULT = new LimitClause() {
    @Override
    public String getLimitSql(long limit) {
      return "";
    }

    @Override
    public String getOffsetSql(long offset) {
      return "";
    }

    @Override
    public String getLimitOffsetSql(long offset, long limit) {
      return "";
    }
  };

  /**
   * Returns the {@code LIMIT} clause to limit results.
   *
   * @param limit
   *          the maximum number of lines returned when the resulting SQL snippet is used.
   *
   * @return rendered limit clause.
   *
   * @see #getLimitOffsetSql(long, long)
   */
  String getLimitSql(long limit);

  /**
   * Returns the {@code OFFSET} clause to consume rows at a given offset.
   *
   * @param offset
   *          the numbers of rows that get skipped when the resulting SQL snippet is used.
   *
   * @return rendered offset clause.
   *
   * @see #getLimitOffsetSql(long, long)
   */
  String getOffsetSql(long offset);

  /**
   * Returns a combined {@code LIMIT/OFFSET} clause that limits results and starts consumption at the given
   * {@code offset}.
   *
   * @param offset
   *          the numbers of rows that get skipped when the resulting SQL snippet is used.
   * @param limit
   *          the maximum number of lines returned when the resulting SQL snippet is used.
   *
   * @return rendered limit clause.
   *
   * @see #getLimitSql(long)
   * @see #getOffsetSql(long)
   */
  String getLimitOffsetSql(long limit, long offset);
}

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
package org.apache.ibatis.dialect;

/**
 * Clause to limit the number of rows returned by a query.
 */
public interface LimitClause {

  /**
   * Modifies the given SQL string to append or wrap a limit and offset clause.
   *
   * @param originalSql
   *          the original query string
   * @param offset
   *          the number of rows to skip
   * @param limit
   *          the number of rows to return
   *
   * @return the modified query string
   */
  String getLimitOffset(long offset, long limit);
}

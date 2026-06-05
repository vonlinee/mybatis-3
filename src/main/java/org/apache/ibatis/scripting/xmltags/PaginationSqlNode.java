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
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.dialect.Dialect;
import org.apache.ibatis.extension.pagination.Pageable;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.jetbrains.annotations.NotNull;

public class PaginationSqlNode implements SqlNode {

  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public boolean apply(@NotNull SqlBuildContext context) {
    final Object parameterObject = context.getParameterObject();
    if (!(parameterObject instanceof Pageable)) {
      throw new RuntimeException("Parameter object is not a sub-type of " + Pageable.class);
    }
    final Pageable pageable = (Pageable) parameterObject;

    final Dialect dialect = context.getDialect();

    String paginationSql = dialect.limit().getLimitOffset(pageable.getOffset(), pageable.getPageSize());
    context.appendSql(paginationSql);
    return true;
  }
}

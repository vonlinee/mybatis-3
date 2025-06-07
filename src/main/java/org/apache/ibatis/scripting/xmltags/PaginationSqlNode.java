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
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.internal.util.ObjectUtils;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.session.Pagination;
import org.apache.ibatis.session.RowBounds;
import org.jetbrains.annotations.Nullable;

public class PaginationSqlNode extends XmlSqlNode {

  @Nullable
  private final String testExpression;

  @Nullable
  private final String pageNumVariable;

  @Nullable
  private final String pageSizeVariable;

  private final int defaultPageNum;

  private final int defaultPageSize;

  public PaginationSqlNode() {
    this(null, null, null, 0, 10);
  }

  public PaginationSqlNode(@Nullable String test, @Nullable String pageNumVariable, @Nullable String pageSizeVariable,
      int defaultPageNum, int defaultPageSize) {
    this.testExpression = test;
    this.pageNumVariable = pageNumVariable;
    this.pageSizeVariable = pageSizeVariable;
    this.defaultPageSize = defaultPageSize;
    this.defaultPageNum = defaultPageNum;
  }

  @Override
  public boolean apply(SqlBuildContext context) {
    if (this.testExpression != null) {
      if (!evaluator.evaluateBoolean(this.testExpression, context.getParameterObject())) {
        return false;
      }
    }
    final Object parameterObject = context.getParameterObject();
    if (parameterObject instanceof RowBounds) {
      RowBounds pagination = (RowBounds) parameterObject;
      context.appendSql(context.dialect().getLimitOffsetClauseSql(pagination.getOffset(), pagination.getLimit()));
      return true;
    } else if (parameterObject instanceof Pagination) {
      Pagination<?> pagination = (Pagination<?>) parameterObject;
      int offset = (pagination.getPageNum() - 1) * pagination.getPageSize();
      context.appendSql(context.dialect().getLimitOffsetClauseSql(offset, pagination.getPageSize()));
      return true;
    }

    int pageNum, limit;
    if (pageNumVariable == null) {
      pageNum = defaultPageNum;
    } else {
      Object value = evaluator.getValue(pageNumVariable, parameterObject);
      pageNum = ObjectUtils.parseInt(value, defaultPageNum);
    }

    if (pageSizeVariable == null) {
      limit = defaultPageSize;
    } else {
      Object value = evaluator.getValue(pageSizeVariable, parameterObject);
      limit = ObjectUtils.parseInt(value, defaultPageSize);
    }
    int offset = (pageNum - 1) * limit;
    String clause = context.dialect().getLimitOffsetClauseSql(offset, limit);
    context.appendSql(clause);
    return true;
  }
}

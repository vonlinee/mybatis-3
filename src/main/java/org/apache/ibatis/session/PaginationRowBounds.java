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
package org.apache.ibatis.session;

import java.util.Collections;
import java.util.List;

public class PaginationRowBounds<T> implements Pagination<T>, RowBounds {

  private static final int DEFAULT_PAGE_NUM = 0;
  private static final int DEFAULT_PAGE_SIZE = 20;

  private int pageNum = 1;
  private int pageSize = Integer.MAX_VALUE;
  private List<T> records;

  @Override
  public int getPageNum() {
    return pageNum;
  }

  @Override
  public void setPageNum(Integer pageNum) {
    this.pageNum = pageNum == null ? DEFAULT_PAGE_NUM : pageNum;
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
  }

  @Override
  public void setRecords(List<T> records) {
    this.records = records == null ? Collections.emptyList() : records;
  }

  public List<T> getRecords() {
    return records;
  }

  @Override
  public int getLimit() {
    return getPageSize();
  }

  /**
   * <blockquote>
   *
   * <pre>
   * offset = (page − 1) × pageSize
   * </pre>
   *
   * </blockquote>
   *
   * @return offset
   */
  @Override
  public int getOffset() {
    return (getPageNum() - 1) * getPageSize();
  }
}

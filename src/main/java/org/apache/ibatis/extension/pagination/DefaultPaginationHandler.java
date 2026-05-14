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
package org.apache.ibatis.extension.pagination;

import java.util.List;

public class DefaultPaginationHandler implements PaginationHandler {

  @Override
  public <T> Page<T> createPage(Integer pageNum, Integer pageSize, long total, List<T> list) {
    return new PageImpl<>(pageNum, pageSize, total, list);
  }

  static class PageImpl<T> implements Page<T> {
    private int pageNum;
    private int pageSize;
    private long totalCount;
    private List<T> list;

    public PageImpl(int pageNum, int pageSize, long totalCount, List<T> list) {
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.totalCount = totalCount;
      this.list = list;
    }

    @Override
    public void setPageNum(int pageNum) {
      this.pageNum = pageNum;
    }

    @Override
    public void setPageSize(int pageSize) {
      this.pageSize = pageSize;
    }

    @Override
    public void setTotal(long total) {
      this.totalCount = total;
    }

    @Override
    public void setRows(List<T> records) {
      this.list = records;
    }

    @Override
    public int getPageNum() {
      return pageNum;
    }

    @Override
    public int getPageSize() {
      return pageSize;
    }

    @Override
    public long getTotal() {
      return totalCount;
    }

    @Override
    public List<T> getRows() {
      return list;
    }

    @Override
    public boolean isEmpty() {
      return list.isEmpty();
    }
  }
}

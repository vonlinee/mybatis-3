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
package org.apache.ibatis.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.Page;
import org.jetbrains.annotations.NotNull;

public class DefaultPaginationHandler implements PaginationHandler {

  @Override
  public <T> @NotNull Page<T> createPage(Integer pageNum, Integer pageSize, Integer total, Collection<T> records) {
    PageImpl<T> page = new PageImpl<>();
    page.setPageNum(pageNum);
    page.setPageSize(pageSize);
    page.setTotal(total);

    if (records instanceof List) {
      page.setRecords((List<T>) records);
    } else {
      page.setRecords(new ArrayList<>(records));
    }
    return page;
  }

  static class PageImpl<T> implements Page<T> {

    int total;
    int pageNum;
    int pageSize;
    List<T> records;

    @Override
    public void setTotal(int total) {
      this.total = total;
    }

    @Override
    public int getTotal() {
      return total;
    }

    @Override
    public void setPageNum(int pageNum) {
      this.pageNum = pageNum;
    }

    @Override
    public int getPageNum() {
      return pageNum;
    }

    @Override
    public void setPageSize(int pageSize) {
      this.pageSize = pageSize;
    }

    @Override
    public int getPageSize() {
      return pageSize;
    }

    @Override
    public void setRecords(List<T> records) {
      this.records = records == null ? Collections.emptyList() : records;
    }

    @Override
    public List<T> getRecords() {
      return this.records;
    }
  }
}

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
package org.apache.ibatis.submitted.sql_render;

import java.time.LocalDateTime;
import java.util.List;

public class UserSearchRequest {

  // Matches 'statusList' in XML
  private List<String> statusList;

  // Matches 'searchKeyword' in XML
  private String searchKeyword;

  // Matches 'registeredAfter' in XML
  private LocalDateTime registeredAfter;

  // Matches 'registeredBefore' in XML
  private LocalDateTime registeredBefore;

  // Matches 'sortBy' in XML (e.g., "nameAsc", "newest")
  private String sortBy;

  // --- Getters and Setters ---

  public List<String> getStatusList() {
    return statusList;
  }

  public void setStatusList(List<String> statusList) {
    this.statusList = statusList;
  }

  public String getSearchKeyword() {
    return searchKeyword;
  }

  public void setSearchKeyword(String searchKeyword) {
    this.searchKeyword = searchKeyword;
  }

  public LocalDateTime getRegisteredAfter() {
    return registeredAfter;
  }

  public void setRegisteredAfter(LocalDateTime registeredAfter) {
    this.registeredAfter = registeredAfter;
  }

  public LocalDateTime getRegisteredBefore() {
    return registeredBefore;
  }

  public void setRegisteredBefore(LocalDateTime registeredBefore) {
    this.registeredBefore = registeredBefore;
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public void setSortBy(UserSortOrder sortBy) {
    this.sortBy = sortBy.name();
  }
}

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
package org.apache.ibatis.submitted.map_result;

import java.util.Map;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.MapResult;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.session.RowBounds;

public interface NoticeMapper {

  @MapKey("id")
  @Select("select id, status from notice")
  Map<Integer, Notice> getNoticeMap1();

  @MapKey("status")
  @Select("select id, status from notice")
  Map<Integer, Notice> getNoticeMap2();

  @MapResult(key = "status", value = "count")
  Map<Integer, Integer> groupStatus();

  @MapResult(key = "status", value = "count")
  Map<Integer, Integer> groupStatus(RowBounds rowBounds);

  @MapKey("id")
  @MapResult(key = "status", value = "count")
  Map<Integer, Integer> groupStatusWithConflictingKey();
}

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

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface Mapper {

  /**
   * Searches and filters users based on admin dashboard criteria. * @param request The search filters and sorting
   * preferences.
   *
   * @return A list of users matching the criteria.
   */
  List<User> searchUsers(UserSearchRequest request);

  List<User> searchUsersByStatus(@Param("status") String status);

  @Select("select * from users where status = #{status}")
  List<User> searchUsersByStatusWithoutParamName(String status);

  @Select("select * from users where id = #{id}")
  User getUserById(Integer id);

  @Select("select * from users where status = #{status} or email like concat('%', #{email},'%')")
  List<User> searchUsersByEmailAndName(String status, String email);
}

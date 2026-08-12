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
package org.apache.ibatis.submitted.pagination;

import java.io.Reader;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.extension.pagination.Page;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PaginationTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    // create a SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/pagination/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }

    // populate in-memory database
    BaseDataTest.runScript(sqlSessionFactory, "org/apache/ibatis/submitted/pagination/CreateDB.sql");
  }

  @Test
  void shouldSelectUsersPageWithPageableParamReturnList() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      // pageSize = 2, pageNum = 1 (second page, offset = 2)
      UserListParam userListParam = new UserListParam();
      userListParam.setPageNum(1);
      userListParam.setPageSize(2);
      Page<User> page = mapper.selectUserPage(userListParam);

      // page 1
      Assertions.assertEquals(1, page.getPageNum());
      Assertions.assertEquals(2, page.getPageSize());
      Assertions.assertEquals(6, page.getTotal());
      Assertions.assertEquals(2, page.getRows().size());
      Assertions.assertEquals(1, page.getRows().get(0).getId());
      Assertions.assertEquals("User1", page.getRows().get(0).getName());
      Assertions.assertEquals(2, page.getRows().get(1).getId());
      Assertions.assertEquals("User2", page.getRows().get(1).getName());

      // page 2
      userListParam.setPageNum(2);
      Page<User> page2 = mapper.selectUserPage(userListParam);
      Assertions.assertEquals(2, page2.getPageNum());
      Assertions.assertEquals(2, page2.getPageSize());
      Assertions.assertEquals(6, page2.getTotal());
      Assertions.assertEquals(2, page2.getRows().size());
      Assertions.assertEquals(3, page2.getRows().get(0).getId());
      Assertions.assertEquals("User3", page2.getRows().get(0).getName());
      Assertions.assertEquals(4, page2.getRows().get(1).getId());
      Assertions.assertEquals("User4", page2.getRows().get(1).getName());
    }
  }
}

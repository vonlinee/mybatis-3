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
package org.apache.ibatis.submitted.pagination;

import java.io.Reader;
import java.util.List;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @see org.apache.ibatis.scripting.xmltags.PaginationSqlNode
 */
class PaginationTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    // create a SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/pagination/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
      sqlSessionFactory.getConfiguration().setLogImpl(StdOutImpl.class);
    }

    // populate in-memory database
    BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
        "org/apache/ibatis/submitted/pagination/CreateDB.sql");
  }

  @Test
  void testPaginationQuery() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSessionFactory.getConfiguration().setNullableOnForEach(true);
      Mapper mapper = sqlSession.getMapper(Mapper.class);

      UserListParam param = new UserListParam();
      param.setPageNum(1);
      param.setPageSize(10);
      List<User> users = mapper.selectUsers1(param);

      Assertions.assertEquals(10, users.size());

      param.setPageNum(1);
      param.setPageSize(20);
      List<User> users1 = mapper.selectUsers1(param);
      Assertions.assertEquals(20, users1.size());
    }
  }
}

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
package org.apache.ibatis.submitted.in;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @see org.apache.ibatis.scripting.xmltags.InSqlNode
 */
class InTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    // create a SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/in/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
      sqlSessionFactory.getConfiguration().setLogImpl(StdOutImpl.class);
    }

    // populate in-memory database
    BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
        "org/apache/ibatis/submitted/in/CreateDB.sql");
  }

  @Test
  void testConditionalIn() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSessionFactory.getConfiguration().setNullableOnForEach(true);
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      List<User> friends = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        User user = new User();
        user.setId(i);
        friends.add(user);
      }
      User user = new User();
      user.setFriendList(friends);
      mapper.countUserWithConditionalIn(user);
    }
  }

  @Test
  void testConditionalInWithImplicitItem() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSessionFactory.getConfiguration().setNullableOnForEach(true);
      Mapper mapper = sqlSession.getMapper(Mapper.class);

      Map<String, Object> paramMap = new HashMap<>();
      paramMap.put("ids", Arrays.asList(1, 2, 3, 4, 5, 6));
      mapper.countUserWithImplicitItem(paramMap);
    }
  }

  @Test
  void testConditionalInWithImplicitItemWithParamPrefix() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSessionFactory.getConfiguration().setNullableOnForEach(true);
      Mapper mapper = sqlSession.getMapper(Mapper.class);

      Map<String, Object> paramMap = new HashMap<>();
      paramMap.put("ids", Arrays.asList(1, 2, 3, 4, 5, 6));
      mapper.countUserWithImplicitItem1(paramMap);
    }
  }
}

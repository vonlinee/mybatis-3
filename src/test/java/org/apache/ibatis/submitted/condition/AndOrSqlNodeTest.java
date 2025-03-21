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
package org.apache.ibatis.submitted.condition;

import java.io.Reader;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AndOrSqlNodeTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    // create a SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/condition/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }

    // populate in-memory database
    BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
        "org/apache/ibatis/submitted/foreach/CreateDB.sql");
  }

  @Test
  public void testAnd() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      TestMapper mapper = session.getMapper(TestMapper.class);
      User user = new User();
      user.setId(1);
      user.setName("zs");
      user.setAge(21);
      int count = mapper.countUserWithNullableIsFalse(user);
      System.out.println(count);
    }
  }

  @Test
  public void shouldApplyCondition() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      TestMapper mapper = session.getMapper(TestMapper.class);
      User user = new User();
      user.setId(1);
      user.setName("zs");
      user.setAge(21);
      int count = mapper.countUserWithTestAnd(user);

      System.out.println(count);
    }
  }
}

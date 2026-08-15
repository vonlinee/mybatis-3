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
package org.apache.ibatis.submitted.columns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ColumnsTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    sqlSessionFactory = BaseDataTest.createDefaultHsqlDbSqlSessionFactory("columns_tag");
    sqlSessionFactory.getConfiguration().setMapUnderscoreToCamelCase(true);
    sqlSessionFactory.getConfiguration().addMapper(Mapper.class);

    BaseDataTest.runScriptSql(sqlSessionFactory, """
        drop table users if exists;

        create table users (
          id int,
          username varchar(20),
          email varchar(50),
          phone varchar(20),
          status varchar(20),
          created_at varchar(10)
        );

        insert into users (id, username, email, phone, status, created_at)
        values (1, 'User1', 'user1@example.com', '13800138000', 'active', '2026-08-17');
        """);
  }

  @Test
  void shouldUseColumnsElementInXmlMapper() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      {
        User user = mapper.findUser();
        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("User1", user.getUsername());
        assertEquals("user1@example.com", user.getEmail());
        assertEquals("13800138000", user.getPhone());
        assertEquals("active", user.getStatus());
        assertEquals("2026-08-17", user.getCreatedAt());
      }

      {
        User user = mapper.findUser1();
        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("User1", user.getUsername());
        assertEquals("user1@example.com", user.getEmail());
        assertEquals("13800138000", user.getPhone());
        assertEquals("active", user.getStatus());
        assertEquals("2026-08-17", user.getCreatedAt());
      }
    }
  }

}

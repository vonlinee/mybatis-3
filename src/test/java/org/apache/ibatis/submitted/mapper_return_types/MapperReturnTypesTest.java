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
package org.apache.ibatis.submitted.mapper_return_types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MapperReturnTypesTest extends BaseDataTest {

  static SqlSessionFactory sqlSessionFactory;

  public interface Mapper {

    @Select("select * from users")
    Cursor<User> getAllUsersCursor();

    @Select("select * from users")
    Iterator<User> getAllUsersIterator();

    @Select("select * from users")
    Stream<User> getAllUsersStream();

    @Select("select * from users")
    Stream<User> getUsers(RowBounds rowBounds);
  }

  @BeforeAll
  static void setUp() throws Exception {
    sqlSessionFactory = createDefaultHsqlDbSqlSessionFactory("return_types");
    runScriptSql(sqlSessionFactory, """
          drop table users if exists;

          create table users (
            id int,
            name varchar(20)
          );

          insert into users values(1, 'User1');
          insert into users values(2, 'User2');
          insert into users values(3, 'User3');
          insert into users values(4, 'User4');
          insert into users values(5, 'User5');
        """);
    sqlSessionFactory.getConfiguration().addMapper(Mapper.class);
  }

  @Test
  void shouldSupportReturnIteratorInMapperMethod() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      Iterator<User> iterator = mapper.getAllUsersIterator();
      int i = 0;
      while (iterator.hasNext()) {
        User next = iterator.next();
        assertEquals((int) next.getId(), i + 1);
        assertEquals(next.getName(), "User" + (i + 1));
        i++;
      }
    }
  }

  @Test
  void shouldSupportReturnStreamInMapperMethod() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      final int[] ids = { 1, 2, 3, 4, 5 };
      try (Stream<User> stream = mapper.getAllUsersStream()) {
        stream.forEach(user -> {
          Assertions.assertEquals(user.getId(), ids[user.getId() - 1]);
          Assertions.assertEquals(user.getName(), "User" + (ids[user.getId() - 1]));
        });
      }
    }
  }

  @Test
  void shouldApplyRowBoundsToStreamInMapperMethod() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      try (Stream<User> stream = mapper.getUsers(new RowBounds(1, 2))) {
        List<Integer> ids = stream.map(User::getId).collect(Collectors.toList());
        assertEquals(List.of(2, 3), ids);
      }
    }
  }
}

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
package org.apache.ibatis.submitted.methodoverloading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OverloadStatementTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    sqlSessionFactory = BaseDataTest.createDefaultHsqlDbSqlSessionFactory("overload");
    sqlSessionFactory.getConfiguration().addMapper(Mapper.class);
    BaseDataTest.runScriptSql(sqlSessionFactory, """
        drop table users if exists;

        create table users (
          id int,
          name varchar(20)
        );

        insert into users (id, name) values(1, 'User1');
        """);
  }

  @Test
  void statementIdSupportsOverloadedMethods() {
    Configuration configuration = new Configuration();
    MapperAnnotationBuilder builder = new MapperAnnotationBuilder(configuration, OverloadedMapper.class);
    builder.parse();

    assertThat(configuration.hasStatement(OverloadedMapper.class.getName() + ".find#Integer")).isTrue();
    assertThat(configuration.hasStatement(OverloadedMapper.class.getName() + ".find#String")).isTrue();
    assertThat(configuration.hasStatement(OverloadedMapper.class.getName() + ".findByName")).isTrue();
  }

  @Test
  void statementIdRejectsEmptyAndQualifiedIds() {
    assertThatThrownBy(() -> new MapperAnnotationBuilder(new Configuration(), EmptyStatementIdMapper.class).parse())
        .isInstanceOf(BuilderException.class).hasMessageContaining("Statement ID cannot be an empty string");
    assertThatThrownBy(() -> new MapperAnnotationBuilder(new Configuration(), DottedStatementIdMapper.class).parse())
        .isInstanceOf(BuilderException.class).hasMessageContaining("Dots are not allowed in statement ID");
  }

  @Test
  void shouldReferenceXmlStatementByAltId() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      User user = mapper.select(1);
      assertEquals("User1", user.getName());
    }
  }

  @Test
  void shouldReferenceNestedSelectByAltId() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      User user = mapper.select("User1");
      assertEquals(Integer.valueOf(1), user.getId());
      assertNotNull(user.getFriend());
    }
  }

  @Test
  void testImplicitNaming() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      User user = new User();
      user.setId(2);
      user.setName("User2");
      mapper.insert(user);
      mapper.insert(new int[] { 3, 4 }, "User");
      sqlSession.commit();
    }
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      User user2 = mapper.select(2);
      assertEquals("User2", user2.getName());
      User user3 = mapper.select(3);
      assertEquals("User3", user3.getName());
      User user4 = mapper.select(4);
      assertEquals("User4", user4.getName());
    }
  }

  @Test
  void shouldFailIfIdIsEmpty() {
    try {
      Configuration config = new Configuration();
      config.addMapper(InvalidMapper1.class);
      fail("Should throw BuilderException");
    } catch (BuilderException e) {
      assertEquals(
          String.format("Statement ID cannot be an empty string. Check @StatementId on %s#%s with parameter(s) %s",
              InvalidMapper1.class.getName(), "select", "[java.lang.Integer id]"),
          e.getMessage());
    }
  }

  @Test
  void shouldFailIfIdContainsDot() {
    try {
      Configuration config = new Configuration();
      config.addMapper(InvalidMapper2.class);
      fail("Should throw BuilderException");
    } catch (BuilderException e) {
      assertEquals(
          String.format("Dots are not allowed in statement ID. Check @StatementId on %s#%s with parameter(s) %s",
              InvalidMapper2.class.getName(), "select", "[]"),
          e.getMessage());
    }
  }
}

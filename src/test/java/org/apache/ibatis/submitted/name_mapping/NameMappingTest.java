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
package org.apache.ibatis.submitted.name_mapping;

import java.util.Map;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NameMappingTest {

  protected static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    sqlSessionFactory = SqlSessionFactoryBuilder
        .buildFromResource("org/apache/ibatis/submitted/camelcase/MapperConfig.xml");
    sqlSessionFactory.getConfiguration().addMapper(Mapper.class);

    BaseDataTest.runScriptSql(sqlSessionFactory,
        "create table user (id int, first_name varchar(25), last_name varchar(25));");
    BaseDataTest.runScriptSql(sqlSessionFactory,
        "insert into user (id, first_name, last_name) values (1, 'camel', 'case');");
  }

  @Test
  void shouldMapWithXmlNameMapping() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Map<String, Object> row = sqlSession.getMapper(Mapper.class).selectFromXml();
      Assertions.assertTrue(row.containsKey("firstName"));
      Assertions.assertFalse(row.containsKey("FIRST_NAME"));
    }
  }

  @Test
  void shouldMapWithSelectNameMapping() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Map<String, Object> row = sqlSession.getMapper(Mapper.class).select();
      Assertions.assertTrue(row.containsKey("lastName"));
      Assertions.assertFalse(row.containsKey("LAST_NAME"));
    }
  }

  @Test
  void shouldMapWithSelectProviderNameMapping() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Map<String, Object> row = sqlSession.getMapper(Mapper.class).selectFromProvider();
      Assertions.assertTrue(row.containsKey("firstName"));
      Assertions.assertFalse(row.containsKey("FIRST_NAME"));
    }
  }
}

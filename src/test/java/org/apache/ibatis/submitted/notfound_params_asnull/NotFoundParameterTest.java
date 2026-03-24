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
package org.apache.ibatis.submitted.notfound_params_asnull;

import static com.googlecode.catchexception.apis.BDDCatchException.when;
import static org.assertj.core.api.BDDAssertions.then;

import com.googlecode.catchexception.apis.BDDCatchException;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NotFoundParameterTest extends BaseDataTest {

  static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  public static void setUp() throws Exception {
    sqlSessionFactory = createDefaultHsqlDbSqlSessionFactory("notfound_parameter_as_null");
    // populate in-memory database
    runScriptSql(sqlSessionFactory, "drop table users if exists;");
    runScriptSql(sqlSessionFactory, "create table users (id int, name varchar(20), email varchar(100));");
    runScriptSql(sqlSessionFactory, "insert into users values (1, 'Bob', 'bob@example.com');");
    sqlSessionFactory.getConfiguration().addMapper(Mapper.class);
  }

  @Test
  void shouldFailWhenNullValueWhenKeyNotFoundInParamMapIsDisabled() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSessionFactory.getConfiguration().setNullValueWhenKeyNotFoundInParamMap(false);
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      when(() -> mapper.findUser("bob@example.com"));
      then(BDDCatchException.caughtException()).isInstanceOf(PersistenceException.class)
          .hasRootCauseInstanceOf(BindingException.class)
          .hasRootCauseMessage("Parameter 'id' not found. Available parameters are [email, param1]");
    }
  }

  @Test
  void shouldOkWhenNullValueWhenKeyNotFoundInParamMapIsEnabled() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSessionFactory.getConfiguration().setNullValueWhenKeyNotFoundInParamMap(true);
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      Assertions.assertDoesNotThrow(() -> {
        String user = mapper.findUser("bob@example.com");
        Assertions.assertEquals("Bob", user);
      });
    }
  }

  @Test
  void shouldFailWhenParameterIsMissingWithPojoParam() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      when(() -> {
        MyParam myParam = new MyParam();
        myParam.setEmail("bob@example.com");
        String user = mapper.findUser(myParam);
      });
      then(BDDCatchException.caughtException()).isInstanceOf(PersistenceException.class)
          .hasRootCauseInstanceOf(ReflectionException.class)
          .hasRootCauseMessage("There is no getter for property named 'id' in '" + MyParam.class + "'");
    }
  }
}

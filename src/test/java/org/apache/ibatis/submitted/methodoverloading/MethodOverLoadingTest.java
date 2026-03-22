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

import static com.googlecode.catchexception.apis.BDDCatchException.when;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.catchexception.apis.BDDCatchException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for mapper method overloading
 */
class MethodOverLoadingTest extends BaseDataTest {

  /**
   * create a Mapper Interface attempting to use Java method overloading.
   */
  public interface OverloadedUserMapper {

    @Select("SELECT username FROM users WHERE id = #{id}")
    String findUser(Integer id);

    // This overloaded method shares the same name "findUser"
    @Select("SELECT username FROM users WHERE email = #{email}")
    String findUser(String email);
  }

  /**
   * same statement id is not allowed in XML
   */
  @Test
  public void shouldRejectsOverloadedMethodsInMapper() {
    Configuration config = new Configuration();
    // Attempting to register the mapper will trigger an exception
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      config.addMapper(OverloadedUserMapper.class);
    });
    // The exception proves MyBatis cannot distinguish between the two methods
    String expectedErrorMessage = "Mapped Statements collection already contains key ";
    assertTrue(exception.getMessage().contains(expectedErrorMessage));
    assertTrue(exception.getMessage().contains("findUser"));
  }

  private static class UserSqlProvider {

    /**
     * This method dynamically builds the SQL string based on which parameters are provided. It uses the same @Param
     * names as the Mapper interface.
     */
    public String buildFindUserSql(@Param("id") Integer id, @Param("email") String email) {
      // The SQL class handles the spacing, AND/OR logic, and WHERE clause formatting automatically
      return new SQL() {
        {
          SELECT("username");
          FROM("users");
          if (id != null) {
            WHERE("id = #{id}");
          }
          if (email != null) {
            WHERE("email = #{email}");
          }
        }
      }.toString();
    }
  }

  public interface UserMapperWithProviderAnnotation {

    @SelectProvider(type = UserSqlProvider.class, method = "buildFindUserSql")
    String findUser(@Param("id") Integer id);

    @SelectProvider(type = UserSqlProvider.class, method = "buildFindUserSql")
    String findUser(@Param("id") Integer id, @Param("email") String email);
  }

  @Test
  public void shouldRejectsOverloadedMethodsInMapperUsingProvider() {
    Configuration config = new Configuration();
    // Attempting to register the mapper will trigger an exception
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      config.addMapper(UserMapperWithProviderAnnotation.class);
    });
    String expectedErrorMessage = "Mapped Statements collection already contains key ";
    assertTrue(exception.getMessage().contains(expectedErrorMessage));
    assertTrue(exception.getMessage().contains("findUser"));
  }

  /**
   * same statement id is not allowed in XML
   */
  @Test
  public void shouldFailedWhenStatementIdIsDuplicateInXml() {
    Configuration config = new Configuration();
    String xmlLocation = "org/apache/ibatis/submitted/methodoverloading/XmlMapperWithConflictStatementId.xml";
    try (InputStream inputStream = Resources.getResourceAsStream(xmlLocation)) {
      XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, config, xmlLocation, new HashMap<>());
      Exception exception = assertThrows(BuilderException.class, builder::parse);
      String expectedErrorMessage = "Mapped Statements collection already contains key ";
      assertTrue(exception.getMessage().contains(expectedErrorMessage));
      assertTrue(exception.getMessage().contains("findUser"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void shouldFailWhenNoOverloadingExists() throws IOException, SQLException {
    SqlSessionFactory sqlSessionFactory = createDefaultHsqlDbSqlSessionFactory("method_overloading");
    // populate in-memory database
    runScriptSql(sqlSessionFactory, "drop table users if exists;");
    runScriptSql(sqlSessionFactory, "create table users (id int, name varchar(20), email varchar(100));");
    runScriptSql(sqlSessionFactory, "insert into users values (1, 'Bob', 'bob@example.com');");
    runScriptSql(sqlSessionFactory, "insert into users values (2, 'Alice', 'alice@example.com');");

    String xmlLocation = "org/apache/ibatis/submitted/methodoverloading/UserMapper.xml";
    try (InputStream inputStream = Resources.getResourceAsStream(xmlLocation)) {
      Configuration config = sqlSessionFactory.getConfiguration();
      XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, config, xmlLocation, new HashMap<>());
      builder.parse();
      try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        String userNameById = userMapper.findUser(1, null);
        Assertions.assertEquals("Bob", userNameById);
        String userNameByEmail = userMapper.findUser(null, "bob@example.com");
        Assertions.assertEquals("Bob", userNameByEmail);
        // Generates: SELECT username FROM users WHERE email = ?
        String userByBoth = userMapper.findUser(1, "bob@example.com");
        Assertions.assertEquals("Bob", userByBoth);

        // overloading methods is not support, so the following will throw an exception
        when(() -> userMapper.findUser(1));
        then(BDDCatchException.caughtException()).isInstanceOf(PersistenceException.class)
            .hasRootCauseInstanceOf(BindingException.class)
            .hasRootCauseMessage("Parameter 'email' not found. Available parameters are [id, param1]");

        // multiple methods will be mapped to the same SQL statement in XML
        // Solution:
        // 1. use default method
        // 2. pass null for the unused parameter explicitly (code above)
        when(() -> userMapper.findUser("bob@example.com"));
        then(BDDCatchException.caughtException()).isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Parameter 'id' not found. Available parameters are [email, param1]");
      }
    }
  }
}

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
package org.apache.ibatis.submitted.sql_render;

import static com.googlecode.catchexception.apis.BDDCatchException.caughtException;
import static com.googlecode.catchexception.apis.BDDCatchException.when;
import static org.assertj.core.api.BDDAssertions.then;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.internal.util.LinkedMultiValueMap;
import org.apache.ibatis.internal.util.MultiValueMap;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.DynamicSqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SqlRenderTest {

  private static SqlSessionFactory buildSqlSessionFactory() throws IOException, SQLException {
    SqlSessionFactory sqlSessionFactory = BaseDataTest.createDefaultHsqlDbSqlSessionFactory("sql_render");
    BaseDataTest.runScript(sqlSessionFactory, "org/apache/ibatis/submitted/sql_render/CreateDB.sql");
    sqlSessionFactory.getConfiguration().setLogImpl(StdOutImpl.class);
    sqlSessionFactory.getConfiguration().addMapper(Mapper.class);
    return sqlSessionFactory;
  }

  @Test
  void shouldRenderWithParamTypeViaDynamicSqlSource() throws SQLException, IOException {
    SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory();
    Mapper userMapper = sqlSessionFactory.getConfiguration().getMapper(Mapper.class, sqlSessionFactory.openSession());

    Configuration configuration = sqlSessionFactory.getConfiguration();
    MappedStatement statement = configuration
        .getMappedStatement("org.apache.ibatis.submitted.sql_render.Mapper.searchUsers");

    SqlSource sqlSource = statement.getSqlSource();
    Assertions.assertInstanceOf(DynamicSqlSource.class, sqlSource);

    // Given
    UserSearchRequest request = new UserSearchRequest();
    request.setStatusList(Arrays.asList("ACTIVE", "INACTIVE"));
    request.setSearchKeyword("example.com");
    request.setSortBy(UserSortOrder.NEWEST);

    // ParamType.INLINED
    {
      String expected = SqlSourceBuilder.removeExtraWhitespaces("""
           SELECT
              id, name, email, status, age, created_at
           FROM users
           WHERE status IN ( 'ACTIVE' , 'INACTIVE' )
               AND (name LIKE '%example.com%' OR email LIKE '%example.com%')
               ORDER BY id ASC
          """);
      BoundSql boundSql = sqlSource.getBoundSql(configuration, request, ParamType.INLINED);
      Assertions.assertEquals(expected, SqlSourceBuilder.removeExtraWhitespaces(boundSql.getSql()));
    }

    // ParamType.NAMED
    {
      String expected = SqlSourceBuilder.removeExtraWhitespaces("""
          SELECT
                  id, name, email, status, age, created_at
                  FROM users
                   WHERE  status IN
                           (
                              #{statusItem}
                           ,
                              #{statusItem}
                           )
                          AND (name LIKE #{searchPattern} OR email LIKE #{searchPattern})
                          ORDER BY id ASC
          """);
      BoundSql boundSql = sqlSource.getBoundSql(configuration, request, ParamType.NAMED);
      Assertions.assertEquals(expected, SqlSourceBuilder.removeExtraWhitespaces(boundSql.getSql()));
      assertParameterMappings(boundSql);
    }

    // ParamType.INDEXED
    {
      String expected = SqlSourceBuilder.removeExtraWhitespaces("""
          SELECT
                  id, name, email, status, age, created_at
                  FROM users
                   WHERE  status IN ( ? , ? )
                          AND (name LIKE ? OR email LIKE ?)
                          ORDER BY id ASC
          """);

      BoundSql boundSql = sqlSource.getBoundSql(configuration, request, ParamType.INDEXED);
      Assertions.assertEquals(expected, SqlSourceBuilder.removeExtraWhitespaces(boundSql.getSql()));
      assertParameterMappings(boundSql);
    }

    {
      List<User> users = userMapper.searchUsers(request);
      Assertions.assertEquals(4, users.size());
    }

    {
      configuration.setDefaultParamType(ParamType.INLINED);
      List<User> users = userMapper.searchUsers(request);
      Assertions.assertEquals(4, users.size());
    }
  }

  private void assertParameterMappings(BoundSql boundSql) {
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    Assertions.assertNotNull(parameterMappings);
    Assertions.assertEquals(4, parameterMappings.size());

    MultiValueMap<String, ParameterMapping> parameterMappingMap = new LinkedMultiValueMap<>();
    for (ParameterMapping parameterMapping : parameterMappings) {
      parameterMappingMap.add(parameterMapping.getProperty(), parameterMapping);
    }
    Assertions.assertEquals(2, parameterMappingMap.size());

    Assertions.assertTrue(parameterMappingMap.containsKey("statusItem"));
    Assertions.assertTrue(parameterMappingMap.containsKey("searchPattern"));
    Assertions.assertNotNull(parameterMappingMap.get("statusItem"));
    Assertions.assertNotNull(parameterMappingMap.get("searchPattern"));
    Assertions.assertEquals(2, parameterMappingMap.countByKey("statusItem"));
    Assertions.assertEquals(2, parameterMappingMap.countByKey("searchPattern"));

    ParameterMapping statusItem1 = parameterMappingMap.get("statusItem", 0);
    Assertions.assertNotNull(statusItem1);
    Assertions.assertTrue(statusItem1.hasValue());
    Assertions.assertEquals("ACTIVE", statusItem1.getValue());

    ParameterMapping statusItem2 = parameterMappingMap.get("statusItem", 1);
    Assertions.assertNotNull(statusItem2);
    Assertions.assertTrue(statusItem2.hasValue());
    Assertions.assertEquals("INACTIVE", statusItem2.getValue());

    ParameterMapping searchPattern1 = parameterMappingMap.get("searchPattern", 0);
    Assertions.assertNotNull(searchPattern1);
    Assertions.assertTrue(searchPattern1.hasValue());
    Assertions.assertEquals("%example.com%", searchPattern1.getValue());

    ParameterMapping searchPattern2 = parameterMappingMap.get("searchPattern", 1);
    Assertions.assertNotNull(searchPattern2);
    Assertions.assertTrue(searchPattern2.hasValue());
    Assertions.assertEquals("%example.com%", searchPattern2.getValue());
  }

  @Test
  void shouldRenderWithParamTypeViaRawSqlSource() throws NoSuchMethodException, SQLException, IOException {
    SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory();
    Mapper userMapper = sqlSessionFactory.getConfiguration().getMapper(Mapper.class, sqlSessionFactory.openSession());
    Configuration configuration = sqlSessionFactory.getConfiguration();
    configuration.setDefaultParamType(ParamType.INLINED);

    MappedStatement statement = configuration
        .getMappedStatement("org.apache.ibatis.submitted.sql_render.Mapper.searchUsersByStatus");
    SqlSource sqlSource = statement.getSqlSource();
    Assertions.assertInstanceOf(RawSqlSource.class, sqlSource);

    {
      List<User> users = userMapper.searchUsersByEmailAndName("ACTIVE", "example.com");

      System.out.println(users);
    }

    final String param1 = "INACTIVE";

    {
      List<User> users = userMapper.searchUsersByStatus(param1);
    }

    {
      List<User> users1 = userMapper.searchUsersByStatusWithoutParamName(param1);

    }

    ParamNameResolver paramNameResolver = new ParamNameResolver(Mapper.class,
        Mapper.class.getDeclaredMethod("searchUsersByStatus", String.class), configuration.isUseActualParamName());
    final Object request = paramNameResolver.getNamedParams(new Object[] { param1 },
        configuration.isNullValueWhenKeyNotFoundInParamMap());

    // ParamType.INLINED
    {
      String expected = SqlSourceBuilder.removeExtraWhitespaces("""
           SELECT * FROM users
           WHERE status = 'INACTIVE'
          """);
      BoundSql boundSql = sqlSource.getBoundSql(configuration, request, ParamType.INLINED);
      Assertions.assertEquals(expected, SqlSourceBuilder.removeExtraWhitespaces(boundSql.getSql()));
    }

    // ParamType.NAMED
    {
      when(() -> sqlSource.getBoundSql(configuration, request, ParamType.NAMED));
      then(caughtException()).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Named parameter type is not supported.");
    }

    // ParamType.INDEXED
    {
      String expected = SqlSourceBuilder.removeExtraWhitespaces("""
           SELECT * FROM users
           WHERE status = ?
          """);

      BoundSql boundSql = sqlSource.getBoundSql(configuration, request, ParamType.INDEXED);
      Assertions.assertEquals(expected, SqlSourceBuilder.removeExtraWhitespaces(boundSql.getSql()));
    }
  }

}

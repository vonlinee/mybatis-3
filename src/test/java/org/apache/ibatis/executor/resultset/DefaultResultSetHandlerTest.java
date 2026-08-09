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
package org.apache.ibatis.executor.resultset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

public class DefaultResultSetHandlerTest {

  /**
   * Contrary to the spec, some drivers require case-sensitive column names when getting result.
   *
   * @see <a href="https://github.com/mybatis/old-google-code-issues/issues/557">Issue 557</a>
   */
  @Test
  void shouldRetainColumnNameCase() {
    SqlSessionFactory sqlSessionFactory = createSqlSessionFactory();

    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Map<String, Object> result = sqlSession.getMapper(ResultSetHandlerMapper.class).selectRetainingColumnNameCase();

      assertEquals(100, result.get("cOlUmN1"));
    }
  }

  @Test
  void shouldThrowExceptionWithColumnName() {
    SqlSessionFactory sqlSessionFactory = createSqlSessionFactory();

    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      ResultSetHandlerMapper mapper = sqlSession.getMapper(ResultSetHandlerMapper.class);

      PersistenceException exception = assertThrows(PersistenceException.class,
          mapper::selectFailingConstructorArgument);
      assertTrue(exception.getMessage().contains("Could not process result for mapping: ResultMapping{"));
      assertTrue(exception.getMessage().contains("column='PROBLEM_COLUMN'"));
      assertTrue(
          exception.getCause().getCause().getMessage().contains("Error attempting to get column 'PROBLEM_COLUMN'"));
    }
  }

  private static SqlSessionFactory createSqlSessionFactory() {
    SqlSessionFactory sqlSessionFactory = BaseDataTest
        .createDefaultHsqlDbSqlSessionFactory("DefaultResultSetHandlerTest" + System.nanoTime());
    sqlSessionFactory.getConfiguration().addMapper(ResultSetHandlerMapper.class);
    return sqlSessionFactory;
  }

  interface ResultSetHandlerMapper {
    @Select("select * from (values(100)) as t(\"CoLuMn1\")")
    @Results(@Result(property = "cOlUmN1", column = "CoLuMn1", javaType = Integer.class))
    Map<String, Object> selectRetainingColumnNameCase();

    @Select("select * from (values(100)) as t(\"PROBLEM_COLUMN\")")
    @ConstructorArgs(@Arg(column = "PROBLEM_COLUMN", javaType = Integer.class, typeHandler = FailingIntegerTypeHandler.class))
    FailingConstructorArgument selectFailingConstructorArgument();
  }

  public static class FailingConstructorArgument {

    public FailingConstructorArgument() {
    }

    public FailingConstructorArgument(Integer value) {
      // Constructor intentionally present so the query uses constructor mappings.
    }
  }

  public static class FailingIntegerTypeHandler extends BaseTypeHandler<Integer> {
    public FailingIntegerTypeHandler() {
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Integer parameter, JdbcType jdbcType)
        throws SQLException {
      ps.setInt(i, parameter);
    }

    @Override
    public Integer getNullableResult(ResultSet rs, String columnName) throws SQLException {
      throw new SQLException("exception");
    }

    @Override
    public Integer getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
      throw new SQLException("exception");
    }

    @Override
    public Integer getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
      throw new SQLException("exception");
    }
  }
}

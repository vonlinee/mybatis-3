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
package org.apache.ibatis.executor.result;

import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.executor.statement.JdbcUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @see org.apache.ibatis.executor.statement.JdbcUtils
 */
class RowMapperTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    // create a SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("org/apache/ibatis/executor/result/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
      sqlSessionFactory.getConfiguration().setLogImpl(StdOutImpl.class);
    }

    // populate in-memory database
    BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
        "org/apache/ibatis/executor/result/CreateDB.sql");
  }

  @Test
  void testSingleColumn() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSessionFactory.getConfiguration().setNullableOnForEach(true);
      try (Connection connection = sqlSession.getConnection()) {
        List<String> list = JdbcUtils.queryForList(connection, "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES",
            String.class);

        List<Map<String, Object>> rows = JdbcUtils.queryForMapList(connection,
            "SELECT * FROM INFORMATION_SCHEMA.TABLES");

        List<Map<String, Object>> rows1 = JdbcUtils.queryForMapList(connection,
            "SELECT * FROM INFORMATION_SCHEMA.TABLES", StringUtils::underscoreToCamel);

      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * @see BeanClassRowMapper
   */
  @Test
  void shouldMapToJavaBean() {
    DataSource dataSource = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
    try (Connection connection = dataSource.getConnection()) {
      List<TableInfo> tables = JdbcUtils.queryForList(connection, "SELECT * FROM INFORMATION_SCHEMA.TABLES",
          TableInfo.class);

      // System.out.println(tables);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  static class TableInfo {

    private String tableCatalog;
    private String tableSchema;
    private String tableName;

    private int commitAction;

    public int getCommitAction() {
      return commitAction;
    }

    public void setCommitAction(int commitAction) {
      this.commitAction = commitAction;
    }

    public String getTableCatalog() {
      return tableCatalog;
    }

    public void setTableCatalog(String tableCatalog) {
      this.tableCatalog = tableCatalog;
    }

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }

    public String getTableSchema() {
      return tableSchema;
    }

    public void setTableSchema(String tableSchema) {
      this.tableSchema = tableSchema;
    }
  }
}

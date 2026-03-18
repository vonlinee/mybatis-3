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
package org.apache.ibatis.submitted.java_mapper_namespace;

import java.io.Reader;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JavaMapperNamespaceTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    try (Reader reader = Resources
        .getResourceAsReader("org/apache/ibatis/submitted/java_mapper_namespace/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }

    sqlSessionFactory.getConfiguration().addMapper(Mapper1.class);
  }

  static class TableInfo {
    private String tableCatalog;
    private String tableName;
    private String tableSchema;
    private Boolean isTyped;
    private String tableType;
    private Boolean isInsertableInto;

    public Boolean getInsertableInto() {
      return isInsertableInto;
    }

    public void setInsertableInto(Boolean insertableInto) {
      isInsertableInto = insertableInto;
    }

    public Boolean getTyped() {
      return isTyped;
    }

    public void setTyped(Boolean typed) {
      isTyped = typed;
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

    public String getTableType() {
      return tableType;
    }

    public void setTableType(String tableType) {
      this.tableType = tableType;
    }
  }

  @Mapper(namespace = "post")
  interface Mapper1 {

    @Results(id = "tableInfoResultMap", value = { @Result(property = "tableCatalog", column = "TABLE_CATALOG"),
        @Result(property = "tableName", column = "TABLE_NAME") })
    @Select("SELECT TABLE_CATALOG, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLES.TABLE_NAME = 'TABLES'")
    TableInfo selectTable1();

    Map<String, Object> selectTable();
  }

  @Test
  void shouldGetResultBySpecifyingNamespace() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Configuration configuration = sqlSessionFactory.getConfiguration();
      Assertions.assertTrue(configuration.hasStatement("post.selectTable"));
      Assertions.assertTrue(configuration.hasStatement("selectTable"));

      Assertions.assertTrue(configuration.getResultMapNames().contains("post.tableInfoResultMap"));
      Assertions.assertTrue(configuration.getResultMapNames().contains("tableInfoResultMap"));

      Mapper1 mapper = sqlSession.getMapper(Mapper1.class);

      Map<String, Object> stringObjectMap = mapper.selectTable();
      TableInfo tableInfos = mapper.selectTable1();

      Assertions.assertEquals(stringObjectMap.get("TABLE_CATALOG"), tableInfos.getTableCatalog());
      Assertions.assertEquals(stringObjectMap.get("TABLE_NAME"), tableInfos.getTableName());
    }
  }
}

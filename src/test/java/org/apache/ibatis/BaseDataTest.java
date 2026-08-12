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
package org.apache.ibatis;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

public abstract class BaseDataTest {

  public static final String BLOG_PROPERTIES = "org/apache/ibatis/databases/blog/blog-derby.properties";
  public static final String BLOG_DDL = "org/apache/ibatis/databases/blog/blog-derby-schema.sql";
  public static final String BLOG_DATA = "org/apache/ibatis/databases/blog/blog-derby-dataload.sql";

  public static final String JPETSTORE_PROPERTIES = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb.properties";
  public static final String JPETSTORE_DDL = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb-schema.sql";
  public static final String JPETSTORE_DATA = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb-dataload.sql";

  public static UnpooledDataSource createUnpooledDataSource(String resource) throws IOException {
    Properties props = Resources.getResourceAsProperties(resource);
    UnpooledDataSource ds = new UnpooledDataSource();
    ds.setDriver(props.getProperty("driver"));
    ds.setUrl(props.getProperty("url"));
    ds.setUsername(props.getProperty("username"));
    ds.setPassword(props.getProperty("password"));
    return ds;
  }

  public static PooledDataSource createPooledDataSource(String resource) throws IOException {
    Properties props = Resources.getResourceAsProperties(resource);
    PooledDataSource ds = new PooledDataSource();
    ds.setDriver(props.getProperty("driver"));
    ds.setUrl(props.getProperty("url"));
    ds.setUsername(props.getProperty("username"));
    ds.setPassword(props.getProperty("password"));
    return ds;
  }

  public static void runScript(SqlSessionFactory sqlSessionFactory, String resource) throws SQLException, IOException {
    DataSource ds = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
    runScript(ds, resource);
  }

  public static void runScriptSql(SqlSessionFactory sqlSessionFactory, String sqlScript)
      throws SQLException, IOException {
    DataSource ds = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
    runScript(ds, new StringReader(sqlScript));
  }

  public static void runScript(DataSource ds, Reader reader) throws IOException, SQLException {
    try (Connection connection = ds.getConnection()) {
      ScriptRunner runner = new ScriptRunner(connection);
      runner.setAutoCommit(true);
      runner.setStopOnError(false);
      runner.setLogWriter(null);
      runner.setErrorLogWriter(null);
      runScript(runner, reader);
    }
  }

  public static void runScript(DataSource ds, String resource) throws IOException, SQLException {
    try (Connection connection = ds.getConnection()) {
      ScriptRunner runner = new ScriptRunner(connection);
      runner.setAutoCommit(true);
      runner.setStopOnError(false);
      runner.setLogWriter(null);
      runner.setErrorLogWriter(null);
      runScript(runner, resource);
    }
  }

  public static void runScript(ScriptRunner runner, Reader reader) {
    runner.runScript(reader);
  }

  public static void runScript(ScriptRunner runner, String resource) throws IOException, SQLException {
    try (Reader reader = Resources.getResourceAsReader(resource)) {
      runner.runScript(reader);
    }
  }

  public static DataSource createBlogDataSource() throws IOException, SQLException {
    DataSource ds = createUnpooledDataSource(BLOG_PROPERTIES);
    runScript(ds, BLOG_DDL);
    runScript(ds, BLOG_DATA);
    return ds;
  }

  public static DataSource createJPetstoreDataSource() throws IOException, SQLException {
    DataSource ds = createUnpooledDataSource(JPETSTORE_PROPERTIES);
    runScript(ds, JPETSTORE_DDL);
    runScript(ds, JPETSTORE_DATA);
    return ds;
  }

  public static DataSource createUnpooledHsqlDbDataSource(String dataSourceName) {
    UnpooledDataSource dataSource = new UnpooledDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:" + dataSourceName);
    dataSource.setDriver("org.hsqldb.jdbcDriver");
    dataSource.setUsername("sa");
    return dataSource;
  }

  public static Environment createDefaultHsqlDbEnvironment(String dataSourceName) {
    DataSource dataSource = createUnpooledHsqlDbDataSource(dataSourceName);
    return new Environment("default", new JdbcTransactionFactory(), dataSource);
  }

  /**
   * this method is to replace the following code: <blockquote>
   *
   * <pre>
   * // create a SqlSessionFactory
   * try (Reader reader = Resources
   *     .getResourceAsReader("org/apache/ibatis/submitted/missing_id_property/MapperConfig.xml")) {
   *   sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
   * }
   *
   * // populate in-memory database
   * BaseDataTest.runScript(sqlSessionFactory, "org/apache/ibatis/submitted/missing_id_property/CreateDB.sql");
   * </pre>
   *
   * </blockquote>
   *
   * @param dataSourceName
   *          data source name
   *
   * @return SqlSessionFactory
   */
  public static SqlSessionFactory createDefaultHsqlDbSqlSessionFactory(String dataSourceName) {
    Environment environment = createDefaultHsqlDbEnvironment(dataSourceName);
    Configuration configuration = new Configuration(environment);
    return new SqlSessionFactoryBuilder().build(configuration);
  }
}

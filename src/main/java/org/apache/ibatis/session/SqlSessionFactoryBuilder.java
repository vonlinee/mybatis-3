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
package org.apache.ibatis.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.internal.util.IOUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

/**
 * Builds {@link SqlSession} instances.
 *
 * @author Clinton Begin
 */
public class SqlSessionFactoryBuilder {

  public SqlSessionFactory build(Reader reader) {
    return build(reader, null, null);
  }

  public SqlSessionFactory build(Reader reader, String environment) {
    return build(reader, environment, null);
  }

  public SqlSessionFactory build(Reader reader, Properties properties) {
    return build(reader, null, properties);
  }

  public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
    try {
      XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      IOUtils.closeQuietly(reader);
    }
  }

  public SqlSessionFactory build(InputStream inputStream) {
    return build(inputStream, null, null);
  }

  public SqlSessionFactory build(InputStream inputStream, String environment) {
    return build(inputStream, environment, null);
  }

  public SqlSessionFactory build(InputStream inputStream, Properties properties) {
    return build(inputStream, null, properties);
  }

  public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    try {
      XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      IOUtils.closeQuietly(inputStream);
    }
  }

  public SqlSessionFactory build(Configuration config) {
    return new DefaultSqlSessionFactory(config);
  }

  /**
   * Builds a {@link SqlSessionFactory} from a MyBatis XML configuration file located on the classpath.
   * <p>
   * This is a convenience static factory method that:
   * <ol>
   * <li>Resolves the given classpath {@code resource} via {@link Resources#getResourceAsReader(String)}, using the
   * default {@link ClassLoader} chain.</li>
   * <li>Creates a new {@link SqlSessionFactoryBuilder} and delegates to {@link #build(Reader)} to parse the XML
   * configuration into a {@link Configuration} instance.</li>
   * <li>Returns the resulting {@link SqlSessionFactory}, which can then be used to obtain {@link SqlSession}
   * instances.</li>
   * </ol>
   * <p>
   * The underlying {@link Reader} is closed automatically by {@link #build(Reader, String, Properties)} once parsing
   * completes (whether it succeeds or fails), so callers do not need to manage the reader lifecycle.
   * <p>
   * The configuration is parsed using the default environment declared in the XML (via the {@code <environments
   * default="...">;} attribute) and without any externally supplied {@link Properties}. To override the active
   * environment or supply additional properties, use {@link #build(Reader, String)}, {@link #build(Reader, Properties)}
   * or {@link #build(Reader, String, Properties)} directly instead.
   * <p>
   * Example usage:
   *
   * <pre>
   * SqlSessionFactory factory = SqlSessionFactoryBuilder.buildFromResource(&quot;mybatis-config.xml&quot;);
   * </pre>
   *
   * @param resource
   *          the classpath location of the MyBatis XML configuration file (for example,
   *          {@code "org/apache/ibatis/mybatis-config.xml"}); must not be {@code null}
   *
   * @return a fully built {@link SqlSessionFactory} ready to open {@link SqlSession} instances
   *
   * @throws IOException
   *           if the resource cannot be located on the classpath or cannot be read
   * @throws org.apache.ibatis.exceptions.PersistenceException
   *           if the XML configuration is malformed or otherwise fails to parse; the underlying cause is wrapped via
   *           {@link ExceptionFactory#wrapException(String, Exception)}
   *
   * @see Resources#getResourceAsReader(String)
   * @see #build(Reader)
   * @see #build(Reader, String, Properties)
   */
  public static SqlSessionFactory buildFromResource(String resource) throws IOException {
    return new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource));
  }
}

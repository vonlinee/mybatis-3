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
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;

/**
 * A utility class that provides static factory methods for building {@link SqlSource} instances.
 * <p>
 * This class serves as a central builder within the MyBatis SQL parsing pipeline, responsible for creating the
 * appropriate {@link SqlSource} implementation based on the nature of the input SQL:
 * <ul>
 * <li>{@link StaticSqlSource} — for fully resolved SQL strings with {@code ?} placeholders and their corresponding
 * {@link ParameterMapping} list.</li>
 * <li>{@link DynamicSqlSource} — for SQL represented as a {@link SqlNode} tree that contains dynamic elements (e.g.
 * {@code <if>}, {@code <choose>}, {@code <foreach>}) which must be re-evaluated on every execution.</li>
 * <li>{@link RawSqlSource} — for SQL represented as a {@link SqlNode} tree that is static (no dynamic tags) and can be
 * parsed once at build time.</li>
 * </ul>
 * <p>
 * The {@code buildStaticSqlSource} variants go a step further by fully evaluating a {@link SqlNode} tree or parsing
 * {@code #{}} parameter expressions in a raw SQL string, producing a {@link StaticSqlSource} that is ready for
 * immediate execution.
 * <p>
 * This class is not instantiable; all methods are static.
 *
 * @author Clinton Begin
 *
 * @see SqlSource
 * @see StaticSqlSource
 * @see DynamicSqlSource
 * @see RawSqlSource
 */
public final class SqlSourceBuilder {

  private SqlSourceBuilder() {
    throw new UnsupportedOperationException("This class should not be instantiated.");
  }

  /**
   * Builds a {@link StaticSqlSource} from a pre-parsed SQL string and its parameter mappings.
   * <p>
   * If {@link Configuration#isShrinkWhitespacesInSql()} is enabled, consecutive whitespace characters in the SQL string
   * are collapsed into a single space before the source is created.
   *
   * @param configuration
   *          the MyBatis {@link Configuration}
   * @param sql
   *          the SQL string, typically containing {@code ?} placeholders
   * @param parameterMappings
   *          the ordered list of {@link ParameterMapping}s corresponding to the {@code ?} placeholders in the SQL
   *          string
   *
   * @return a new {@link StaticSqlSource} wrapping the SQL and parameter mappings
   */
  public static SqlSource buildSqlSource(Configuration configuration, String sql,
      List<ParameterMapping> parameterMappings) {
    return new StaticSqlSource(
        configuration.isShrinkWhitespacesInSql() ? SqlSourceBuilder.removeExtraWhitespaces(sql) : sql,
        parameterMappings);
  }

  /**
   * Builds a {@link SqlSource} from a {@link SqlNode} tree without parameter type information.
   * <p>
   * This is a convenience overload that delegates to
   * {@link #buildSqlSource(Configuration, SqlNode, Class, ParamNameResolver)} with {@code null} for both
   * {@code parameterType} and {@code paramNameResolver}.
   *
   * @param configuration
   *          the MyBatis {@link Configuration}
   * @param rootSqlNode
   *          the root {@link SqlNode} representing the parsed SQL statement
   *
   * @return a {@link DynamicSqlSource} if the node tree is dynamic, otherwise a {@link RawSqlSource}
   */
  public static SqlSource buildSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    return buildSqlSource(configuration, rootSqlNode, null, null);
  }

  /**
   * Builds a {@link SqlSource} from a {@link SqlNode} tree, choosing the appropriate implementation based on whether
   * the SQL is dynamic.
   * <p>
   * If {@link SqlNode#isDynamic()} returns {@code true}, a {@link DynamicSqlSource} is created so that the
   * {@link SqlNode} tree is re-evaluated on every execution. Otherwise, a {@link RawSqlSource} is created, which parses
   * the SQL once at build time for better performance.
   *
   * @param configuration
   *          the MyBatis {@link Configuration}
   * @param rootSqlNode
   *          the root {@link SqlNode} representing the parsed SQL statement
   * @param parameterType
   *          the declared parameter type for the mapped statement, may be {@code null}
   * @param paramNameResolver
   *          the {@link ParamNameResolver} for resolving parameter names, may be {@code null}
   *
   * @return a {@link DynamicSqlSource} or {@link RawSqlSource} depending on the SQL's dynamism
   */
  public static SqlSource buildSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    SqlSource sqlSource;
    if (rootSqlNode.isDynamic()) {
      sqlSource = new DynamicSqlSource(rootSqlNode);
    } else {
      sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType, paramNameResolver);
    }
    return sqlSource;
  }

  /**
   * Builds a {@link StaticSqlSource} by fully evaluating a {@link SqlNode} tree.
   * <p>
   * The {@link SqlNode} tree is applied to a {@link DynamicContext}, which resolves all dynamic elements (such as
   * {@code <if>}, {@code <choose>}, {@code <foreach>}) and collects the resulting SQL text and
   * {@link ParameterMapping}s. The output is a {@link StaticSqlSource} that represents the fully rendered SQL at build
   * time.
   *
   * @param configuration
   *          the MyBatis {@link Configuration}
   * @param rootSqlNode
   *          the root {@link SqlNode} to evaluate
   * @param parameterType
   *          the declared parameter type, used to initialize the {@link DynamicContext}, may be {@code null}
   * @param paramNameResolver
   *          the {@link ParamNameResolver} for resolving parameter names, may be {@code null}
   *
   * @return a new {@link StaticSqlSource} containing the fully evaluated SQL and parameter mappings
   */
  public static SqlSource buildStaticSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    DynamicContext context = new DynamicContext(configuration, parameterType, paramNameResolver,
        configuration.getDefaultParamType());
    rootSqlNode.apply(context);
    String sql = context.getSql();
    return SqlSourceBuilder.buildSqlSource(configuration, sql, context.getParameterMappings());
  }

  /**
   * Builds a {@link StaticSqlSource} by parsing {@code #{}} parameter expressions in a raw SQL string.
   * <p>
   * A {@link ParameterMappingTokenHandler} is used to scan the SQL string for {@code #{}} placeholders, replacing each
   * one with a {@code ?} and collecting the corresponding {@link ParameterMapping}s. The resulting parsed SQL and
   * mappings are then wrapped in a {@link StaticSqlSource}.
   *
   * @param configuration
   *          the MyBatis {@link Configuration}
   * @param sql
   *          the raw SQL string potentially containing {@code #{}} parameter expressions
   * @param parameterType
   *          the declared parameter type, used for resolving Java types of parameters, may be {@code null}
   * @param paramNameResolver
   *          the {@link ParamNameResolver} for resolving parameter names, may be {@code null}
   *
   * @return a new {@link StaticSqlSource} with {@code ?} placeholders and collected parameter mappings
   */
  public static SqlSource buildStaticSqlSource(Configuration configuration, String sql, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    List<ParameterMapping> parameterMappings = new ArrayList<>();
    ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler(parameterMappings, configuration, null,
        parameterType, new HashMap<>(), paramNameResolver, false, configuration.getDefaultParamType());
    return SqlSourceBuilder.buildSqlSource(configuration, tokenHandler.parse(sql), parameterMappings);
  }

  /**
   * Removes extra whitespace characters from the given SQL string.
   * <p>
   * Consecutive whitespace characters are collapsed into a single space, and leading/trailing whitespace is trimmed.
   * This is delegated to {@link StringUtils#removeExtraWhitespaces(String)}.
   *
   * @param original
   *          the original SQL string
   *
   * @return the SQL string with normalized whitespace
   */
  public static String removeExtraWhitespaces(String original) {
    return StringUtils.removeExtraWhitespaces(original);
  }

}

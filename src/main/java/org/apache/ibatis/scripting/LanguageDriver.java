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
package org.apache.ibatis.scripting;

import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;
import org.jetbrains.annotations.Nullable;

public interface LanguageDriver {

  /**
   * @param configuration
   *          configuration
   *
   * @see LanguageDriverRegistry
   */
  default void setConfiguration(Configuration configuration) {
  }

  /**
   * Creates an {@link SqlSource} that will hold the statement read from a mapper xml file. It is called during startup,
   * when the mapped statement is read from a class or a xml file.
   *
   * @param configuration
   *          The MyBatis configuration
   * @param script
   *          XNode parsed from XML file
   * @param parameterType
   *          input parameter type got from a mapper method or specified in the parameterType xml attribute. Can be
   *          null.
   *
   * @return the sql source
   */
  SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);

  default SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    return createSqlSource(configuration, script, parameterType);
  }

  SqlSource createSqlSource(SqlNode rootSqlNode, Configuration configuration, @Nullable Class<?> parameterType,
      @Nullable ParamNameResolver paramNameResolver);

  /**
   * Creates an {@link SqlSource} that will hold the statement read from an annotation. It is called during startup,
   * when the mapped statement is read from a class or a xml file.
   *
   * @param configuration
   *          The MyBatis configuration
   * @param script
   *          The content of the annotation
   * @param parameterType
   *          input parameter type got from a mapper method or specified in the parameterType xml attribute. Can be
   *          null.
   *
   * @return the sql source
   */
  SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);

  default SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    return createSqlSource(configuration, script, parameterType);
  }

}

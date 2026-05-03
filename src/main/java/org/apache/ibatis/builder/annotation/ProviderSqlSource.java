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
package org.apache.ibatis.builder.annotation;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class ProviderSqlSource implements SqlSource {

  private final Configuration configuration;
  private final Class<?> mapperType;
  private final LanguageDriver languageDriver;
  private final Method mapperMethod;
  private final SqlProvider sqlProvider;
  private final ParamNameResolver paramNameResolver;

  /**
   * Instantiates a new provider sql source.
   *
   * @param configuration
   *          the configuration
   * @param mapperType
   *          the mapper type
   * @param mapperMethod
   *          the mapper method
   *
   * @since 3.5.3
   */
  public ProviderSqlSource(Configuration configuration, Class<?> mapperType, Method mapperMethod,
      SqlProvider sqlProvider) {
    this.mapperType = mapperType;
    this.configuration = configuration;
    this.mapperMethod = mapperMethod;
    Lang lang = mapperMethod == null ? null : mapperMethod.getAnnotation(Lang.class);
    this.languageDriver = configuration.getLanguageDriver(lang == null ? null : lang.value());
    this.sqlProvider = sqlProvider;
    this.paramNameResolver = sqlProvider.resolveParameterNames(mapperType, mapperMethod);
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject, ParamType paramType) {
    ProviderContext providerContext = new ProviderContext(configuration, mapperType, mapperMethod,
        configuration.getDatabaseId(), paramNameResolver, parameterObject, paramType);
    String sql = sqlProvider.provideSql(providerContext);
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, parameterType, paramNameResolver);
    return sqlSource.getBoundSql(parameterObject, paramType);
  }

}

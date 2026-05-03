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

import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;

/**
 * The context object for sql provider method.
 *
 * @author Kazuki Shimizu
 *
 * @since 3.4.5
 */
public final class ProviderContext {

  private final Class<?> mapperType;
  private final Method mapperMethod;
  private final String databaseId;
  private final Configuration configuration;
  private final ParamNameResolver paramNameResolver;
  private final Object parameterObject;

  /**
   * the param type expected by the provider
   */
  private final ParamType paramType;

  ProviderContext(Configuration configuration, Class<?> mapperType, Method mapperMethod, String databaseId,
      ParamNameResolver paramNameResolver, Object parameterObject) {
    this(configuration, mapperType, mapperMethod, databaseId, paramNameResolver, parameterObject, ParamType.INDEXED);
  }

  /**
   * Constructor.
   *
   * @param mapperType
   *          A mapper interface type that specified provider
   * @param mapperMethod
   *          A mapper method that specified provider
   * @param databaseId
   *          A database id
   */
  ProviderContext(Configuration configuration, Class<?> mapperType, Method mapperMethod, String databaseId,
      ParamNameResolver paramNameResolver, Object parameterObject, ParamType paramType) {
    this.mapperType = mapperType;
    this.mapperMethod = mapperMethod;
    this.databaseId = databaseId;
    this.configuration = configuration;
    this.paramNameResolver = paramNameResolver;
    this.parameterObject = parameterObject;
    this.paramType = paramType;
  }

  /**
   * Get a mapper interface type that specified provider.
   *
   * @return A mapper interface type that specified provider
   */
  public Class<?> getMapperType() {
    return mapperType;
  }

  /**
   * Get a mapper method that specified provider.
   *
   * @return A mapper method that specified provider
   */
  public Method getMapperMethod() {
    return mapperMethod;
  }

  /**
   * Get a database id that provided from {@link org.apache.ibatis.mapping.DatabaseIdProvider}.
   *
   * @return A database id
   *
   * @since 3.5.1
   */
  public String getDatabaseId() {
    return databaseId;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Object getParameterObject() {
    return parameterObject;
  }

  public ParamNameResolver getParamNameResolver() {
    return paramNameResolver;
  }

  public ParamType getParamType() {
    return paramType;
  }
}

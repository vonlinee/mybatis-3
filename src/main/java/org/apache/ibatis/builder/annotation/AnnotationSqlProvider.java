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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;

/**
 * @see MethodDelegationSqlProvider
 */
public class AnnotationSqlProvider implements SqlProvider {

  private final SqlProvider delegate;

  public AnnotationSqlProvider(Configuration configuration, Annotation provider, Class<?> mapperType,
      Method mapperMethod) {
    try {
      Class<?> providerType = getProviderType(configuration, provider, mapperMethod);
      if (SqlProviderFactory.class.isAssignableFrom(providerType)) {
        SqlProviderFactory sqlProviderFactory = configuration.getSqlProviderFactory(providerType);
        if (sqlProviderFactory == null) {
          throw new BuilderException(String.format(
              "a type of sql provider factory [%s] is used as the provider type in the mapper method %s#%s, but the SqlProviderFactory is not configured.",
              providerType.getName(), mapperType.getName(), mapperMethod.getName()));
        }
        this.delegate = Objects.requireNonNull(sqlProviderFactory.getSqlProvider(mapperType, mapperMethod),
            "the sql provider is null");
      } else {
        this.delegate = new MethodDelegationSqlProvider(configuration, provider, mapperType, mapperMethod,
            providerType);
      }
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new BuilderException("Error creating SqlSource for SqlProvider.  Cause: " + e);
    }
  }

  @Override
  public ParamNameResolver resolveParameterNames(Class<?> mapperClass, Method mapperMethod) {
    return delegate.resolveParameterNames(mapperClass, mapperMethod);
  }

  @Override
  public String provideSql(ProviderContext providerContext) {
    return delegate.provideSql(providerContext);
  }

  private Class<?> getProviderType(Configuration configuration, Annotation providerAnnotation, Method mapperMethod)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> type = (Class<?>) providerAnnotation.annotationType().getMethod("type").invoke(providerAnnotation);
    Class<?> value = (Class<?>) providerAnnotation.annotationType().getMethod("value").invoke(providerAnnotation);
    if (value == void.class && type == void.class) {
      if (configuration.getDefaultSqlProviderType() != null) {
        return configuration.getDefaultSqlProviderType();
      }
      throw new BuilderException("Please specify either 'value' or 'type' attribute of @"
          + providerAnnotation.annotationType().getSimpleName() + " at the '" + mapperMethod.toString() + "'.");
    }
    if (value != void.class && type != void.class && value != type) {
      throw new BuilderException("Cannot specify different class on 'value' and 'type' attribute of @"
          + providerAnnotation.annotationType().getSimpleName() + " at the '" + mapperMethod.toString() + "'.");
    }
    return value == void.class ? type : value;
  }
}

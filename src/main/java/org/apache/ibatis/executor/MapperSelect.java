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
package org.apache.ibatis.executor;

import java.util.List;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

public class MapperSelect extends MapperStatement {

  protected RowBounds rowBounds = RowBounds.DEFAULT;
  protected ResultHandler<?> resultHandler;
  protected CacheKey cacheKey;

  public MapperSelect(MappedStatement mappedStatement) {
    super(mappedStatement);
  }

  public static MapperSelect builder(MappedStatement mappedStatement) {
    return new MapperSelect(mappedStatement);
  }

  public MapperSelect rowBounds(RowBounds rowBounds) {
    this.rowBounds = rowBounds;
    return this;
  }

  public MapperSelect resultHandler(ResultHandler<?> handler) {
    this.resultHandler = handler;
    return this;
  }

  public MapperSelect parameter(Object parameter) {
    setParameterObject(parameter);
    return this;
  }

  public MapperSelect build() {
    return this;
  }

  protected CacheKey createCacheKey() {
    CacheKey cacheKey = new CacheKey();
    MappedStatement ms = this.mappedStatement;
    cacheKey.update(ms.getId());
    cacheKey.update(rowBounds.getOffset());
    cacheKey.update(rowBounds.getLimit());
    cacheKey.update(boundSql.getSql());
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
    // mimic DefaultParameterHandler logic
    MetaObject metaObject = null;
    for (ParameterMapping parameterMapping : parameterMappings) {
      if (parameterMapping.getMode() != ParameterMode.OUT) {
        Object value;
        String propertyName = parameterMapping.getProperty();
        if (parameterMapping.hasValue()) {
          value = parameterMapping.getValue();
        } else if (boundSql.hasAdditionalParameter(propertyName)) {
          value = boundSql.getAdditionalParameter(propertyName);
        } else if (parameterObject == null) {
          value = null;
        } else {
          ParamNameResolver paramNameResolver = ms.getParamNameResolver();
          if (paramNameResolver != null
              && typeHandlerRegistry.hasTypeHandler(paramNameResolver.getType(paramNameResolver.getNames()[0]))
              || typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            value = parameterObject;
          } else {
            if (metaObject == null) {
              metaObject = configuration.newMetaObject(parameterObject);
            }
            value = metaObject.getValue(propertyName);
          }
        }
        cacheKey.update(value);
      }
    }
    if (configuration.getEnvironment() != null) {
      // issue #176
      cacheKey.update(configuration.getEnvironment().getId());
    }
    return cacheKey;
  }
}

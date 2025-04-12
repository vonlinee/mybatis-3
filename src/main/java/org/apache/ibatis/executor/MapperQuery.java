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

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.BoundSql;
import org.apache.ibatis.scripting.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * a query with MappedStatement as sql
 *
 * @see MappedStatement
 */
public class MapperQuery extends MapperStatement {

  protected RowBounds rowBounds = RowBounds.DEFAULT;
  protected ResultHandler<?> resultHandler = Executor.NO_RESULT_HANDLER;
  protected Object cacheKey;

  public MapperQuery(MappedStatement ms, Object parameter) {
    super(ms);
    this.parameterObject = parameter;
  }

  protected MapperQuery(MappedStatement ms) {
    super(ms);
  }

  public Object getCacheKey() {
    return cacheKey;
  }

  public void setBoundSql() {
    if (this.boundSql == null) {
      this.boundSql = ms.getBoundSql(this.parameterObject);
    }
  }

  /**
   * override current sql has been set.
   *
   * @param boundSql
   *          boundSql
   */
  public void setBoundSql(BoundSql boundSql) {
    this.boundSql = boundSql;
  }

  public void beforeExecuted(Executor executor) {
    setBoundSql();
    setCacheKey(executor, false);
  }

  /**
   * override current cache key has been set
   *
   * @param cacheKey
   *          cacheKey
   */
  public void setCacheKey(Object cacheKey) {
    this.cacheKey = cacheKey;
  }

  public MapperQuery boundSql(BoundSql boundSql) {
    this.boundSql = boundSql;
    return this;
  }

  public MapperQuery rowBounds(RowBounds rowBounds) {
    this.rowBounds = rowBounds == null ? RowBounds.DEFAULT : rowBounds;
    return this;
  }

  public <T> MapperQuery resultHandler(ResultHandler<T> handler) {
    this.resultHandler = handler;
    return this;
  }

  public void setCacheKey(Executor executor, boolean force) {
    if (this.cacheKey == null) {
      this.cacheKey = executor.createCacheKey(ms, parameterObject, rowBounds, boundSql);
    } else if (force) {
      // force re-calculate cache key
      this.cacheKey = executor.createCacheKey(ms, parameterObject, rowBounds, boundSql);
    }
  }

  public StatementHandler newStatementHandler(Executor executor) {
    return configuration.newStatementHandler(executor, ms, parameterObject, rowBounds, resultHandler, boundSql);
  }

  public void logBeforeExecuted() {
    ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
  }

  public boolean hasResultHandler() {
    return resultHandler != null;
  }

  public boolean isFlushCacheRequired() {
    return ms.isFlushCacheRequired();
  }

  public void handleLocallyCachedOutputParameters(Object cachedParameter) {
    if (cachedParameter != null && parameterObject != null) {
      final MetaObject metaCachedParameter = configuration.newMetaObject(cachedParameter);
      final MetaObject metaParameter = configuration.newMetaObject(parameterObject);
      for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
        if (parameterMapping.getMode() != ParameterMode.IN) {
          final String parameterName = parameterMapping.getProperty();
          final Object cachedValue = metaCachedParameter.getValue(parameterName);
          metaParameter.setValue(parameterName, cachedValue);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public <E> ResultHandler<E> getResultHandler() {
    return (ResultHandler<E>) resultHandler;
  }

  public boolean isUseCache() {
    return ms.isUseCache();
  }
}

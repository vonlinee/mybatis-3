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

import static org.apache.ibatis.executor.ExecutionPlaceholder.EXECUTION_PLACEHOLDER;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.internal.util.JdbcUtils;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.BoundSql;
import org.apache.ibatis.scripting.MappedStatement;
import org.apache.ibatis.scripting.MethodParamMetadata;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
public abstract class BaseExecutor implements Executor {

  private static final Log log = LogFactory.getLog(BaseExecutor.class);

  protected Transaction transaction;
  protected Executor wrapper;

  protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;
  protected PerpetualCache localCache;
  protected PerpetualCache localOutputParameterCache;
  protected Configuration configuration;

  protected int queryStack;
  private boolean closed;

  protected BaseExecutor(Configuration configuration, Transaction transaction) {
    this.transaction = transaction;
    this.deferredLoads = new ConcurrentLinkedQueue<>();
    this.localCache = new PerpetualCache("LocalCache");
    this.localOutputParameterCache = new PerpetualCache("LocalOutputParameterCache");
    this.closed = false;
    this.configuration = configuration;
    this.wrapper = this;
  }

  @Override
  public Transaction getTransaction() {
    checkIfClosed();
    return transaction;
  }

  @Override
  public void close(boolean forceRollback) {
    try {
      try {
        rollback(forceRollback);
      } finally {
        if (transaction != null) {
          transaction.close();
        }
      }
    } catch (SQLException e) {
      // Ignore. There's nothing that can be done at this point.
      log.warn("Unexpected exception on closing transaction.  Cause: " + e);
    } finally {
      transaction = null;
      deferredLoads = null;
      localCache = null;
      localOutputParameterCache = null;
      closed = true;
    }
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public int update(MapperUpdate update) throws SQLException {
    checkIfClosed();
    clearLocalCache();
    update.logBeforeExecuted();
    return doUpdate(update);
  }

  @Override
  public List<BatchResult> flushStatements() throws SQLException {
    return flushStatements(false);
  }

  public List<BatchResult> flushStatements(boolean isRollBack) throws SQLException {
    checkIfClosed();
    return doFlushStatements(isRollBack);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <E> List<E> query(MapperQuery query) throws SQLException {
    query.beforeExecuted(this);
    query.logBeforeExecuted();
    checkIfClosed();
    if (queryStack == 0 && query.isFlushCacheRequired()) {
      clearLocalCache();
    }
    List<E> list;
    try {
      queryStack++;
      list = !query.hasResultHandler() ? (List<E>) localCache.getObject(query.getCacheKey()) : null;
      if (list != null) {
        handleLocallyCachedOutputParameters(query);
      } else {
        list = queryFromDatabase(query);
      }
    } finally {
      queryStack--;
    }
    if (queryStack == 0) {
      handleDeferred();
      if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
        // issue #482
        clearLocalCache();
      }
    }
    return list;
  }

  private void handleDeferred() {
    for (DeferredLoad deferredLoad : deferredLoads) {
      loadDeferred(deferredLoad);
    }
    // issue #601
    deferredLoads.clear();
  }

  @Override
  public <E> Cursor<E> queryCursor(MapperQuery query) throws SQLException {
    query.setBoundSql();
    return doQueryCursor(query);
  }

  @Override
  public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, Object cacheKey,
      Class<?> targetType) {
    checkIfClosed();

    DeferredLoad deferredLoad = new DeferredLoad(resultObject, property, cacheKey, targetType);
    if (hasLocalCache(cacheKey)) {
      loadDeferred(deferredLoad);
    } else {
      deferredLoads.add(deferredLoad);
    }
  }

  private boolean hasLocalCache(Object cacheKey) {
    return localCache.getObject(cacheKey) != null
        && localCache.getObject(cacheKey) != ExecutionPlaceholder.EXECUTION_PLACEHOLDER;
  }

  private void loadDeferred(DeferredLoad deferredLoad) {
    @SuppressWarnings("unchecked")
    // we suppose we get back a List
    List<Object> list = (List<Object>) localCache.getObject(deferredLoad.getCacheKey());
    ResultExtractor extractor = new ResultExtractor(configuration);
    Object value = extractor.extractObjectFromList(list, deferredLoad.getTargetType());
    deferredLoad.setValue(value);
  }

  private void checkIfClosed() {
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
  }

  @Override
  public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    checkIfClosed();
    CacheKey cacheKey = new CacheKey();
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
          MethodParamMetadata paramNameResolver = ms.getParamNameResolver();
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

  @Override
  public boolean isCached(MappedStatement ms, Object key) {
    return localCache.getObject(key) != null;
  }

  @Override
  public void commit(boolean required) throws SQLException {
    if (closed) {
      throw new ExecutorException("Cannot commit, transaction is already closed");
    }
    clearLocalCache();
    flushStatements();
    if (required) {
      transaction.commit();
    }
  }

  @Override
  public void rollback(boolean required) throws SQLException {
    if (!closed) {
      try {
        clearLocalCache();
        flushStatements(true);
      } finally {
        if (required) {
          transaction.rollback();
        }
      }
    }
  }

  @Override
  public void clearLocalCache() {
    if (!closed) {
      localCache.clear();
      localOutputParameterCache.clear();
    }
  }

  protected abstract int doUpdate(MapperUpdate update) throws SQLException;

  protected abstract List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException;

  protected abstract <E> List<E> doQuery(MapperQuery query) throws SQLException;

  protected abstract <E> Cursor<E> doQueryCursor(MapperQuery query) throws SQLException;

  protected void closeStatement(Statement statement) {
    JdbcUtils.closeSilently(statement);
  }

  /**
   * Apply a transaction timeout.
   *
   * @param statement
   *          a current statement
   *
   * @throws SQLException
   *           if a database access error occurs, this method is called on a closed <code>Statement</code>
   *
   * @since 3.4.0
   *
   * @see JdbcUtils#applyTransactionTimeout(Statement, Integer, Integer)
   */
  protected void applyTransactionTimeout(Statement statement) throws SQLException {
    JdbcUtils.applyTransactionTimeout(statement, statement.getQueryTimeout(), transaction.getTimeout());
  }

  private void handleLocallyCachedOutputParameters(MapperQuery query) {
    if (query.isCall()) {
      final Object cachedParameter = localOutputParameterCache.getObject(query.getCacheKey());
      query.handleLocallyCachedOutputParameters(cachedParameter);
    }
  }

  private <E> List<E> queryFromDatabase(MapperQuery query) throws SQLException {
    List<E> list;
    final Object cacheKey = query.getCacheKey();
    localCache.putObject(cacheKey, EXECUTION_PLACEHOLDER);
    try {
      list = doQuery(query);
    } finally {
      localCache.removeObject(cacheKey);
    }
    localCache.putObject(cacheKey, list);
    if (query.isCall()) {
      localOutputParameterCache.putObject(cacheKey, query.getParameterObject());
    }
    return list;
  }

  protected Connection getConnection(Log statementLog) throws SQLException {
    Connection connection = transaction.getConnection();
    if (statementLog.isDebugEnabled()) {
      return ConnectionLogger.newInstance(connection, statementLog, queryStack);
    }
    return connection;
  }

  @Override
  public void setExecutorWrapper(Executor wrapper) {
    this.wrapper = wrapper;
  }

}

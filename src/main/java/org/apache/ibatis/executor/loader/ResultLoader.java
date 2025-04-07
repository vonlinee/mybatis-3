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
package org.apache.ibatis.executor.loader;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ResultExtractor;
import org.apache.ibatis.scripting.BoundSql;
import org.apache.ibatis.scripting.MappedStatement;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 */
public class ResultLoader {

  protected final Configuration configuration;
  protected final Executor executor;
  protected final MappedStatement mappedStatement;
  protected final Object parameterObject;
  protected final Class<?> targetType;
  protected final Object cacheKey;
  protected final BoundSql boundSql;
  protected final ResultExtractor resultExtractor;
  protected final long creatorThreadId;

  protected boolean loaded;

  public ResultLoader(Configuration config, Executor executor, MappedStatement mappedStatement, Object parameterObject,
      Class<?> targetType, Object cacheKey, BoundSql boundSql) {
    this.configuration = config;
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.parameterObject = parameterObject;
    this.targetType = targetType;
    this.cacheKey = cacheKey;
    this.boundSql = boundSql;
    this.resultExtractor = new ResultExtractor(configuration);
    this.creatorThreadId = Thread.currentThread().getId();
  }

  public Object loadResult() throws SQLException {
    List<Object> list = selectList();
    return resultExtractor.extractObjectFromList(list, targetType);
  }

  private <E> List<E> selectList() throws SQLException {
    Executor localExecutor = executor;
    if (Thread.currentThread().getId() != this.creatorThreadId || localExecutor.isClosed()) {
      localExecutor = configuration.newExecutor(ExecutorType.SIMPLE);
    }
    try {
      return localExecutor.query(mappedStatement, parameterObject, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER,
          cacheKey, boundSql);
    } finally {
      if (localExecutor != executor) {
        localExecutor.close(false);
      }
    }
  }
}

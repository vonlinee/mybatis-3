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
package org.apache.ibatis.scripting.defaults;

import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.ReuseExecutor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.DefaultCallableStatementHandler;
import org.apache.ibatis.executor.statement.PreparedStatementHandler;
import org.apache.ibatis.executor.statement.SimpleStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.scripting.ExtensionFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

public class DefaultExtensionFactory implements ExtensionFactory {

  private final Configuration configuration;
  private final InterceptorChain interceptorChain;

  public DefaultExtensionFactory(Configuration configuration) {
    this.configuration = configuration;
    this.interceptorChain = configuration.getInterceptorChain();
  }

  @Override
  public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject,
      BoundSql boundSql) {
    return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
  }

  @Override
  public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds,
      ParameterHandler parameterHandler, ResultHandler<?> resultHandler, BoundSql boundSql) {
    ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, resultHandler,
        rowBounds);
    return (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
  }

  @Override
  public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement,
      Object parameterObject, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql) {

    if (boundSql == null) { // issue #435, get the key before calculating the statement
      KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
      ErrorContext.instance().store();
      keyGenerator.processBefore(executor, mappedStatement, null, parameterObject);
      ErrorContext.instance().recall();
      boundSql = mappedStatement.getBoundSql(parameterObject);
    }

    StatementHandler statementHandler;
    switch (mappedStatement.getStatementType()) {
      case STATEMENT:
        statementHandler = new SimpleStatementHandler(executor, mappedStatement, rowBounds, boundSql);
        break;
      case PREPARED:
        statementHandler = new PreparedStatementHandler(executor, mappedStatement, rowBounds, boundSql);
        break;
      case CALLABLE:
        statementHandler = new DefaultCallableStatementHandler(executor, mappedStatement, rowBounds, boundSql,
            resultHandler);
        break;
      default:
        throw new ExecutorException("Unknown statement type: " + mappedStatement.getStatementType());
    }

    // set ParameterHandler
    statementHandler.setParameterHandler(createParameterHandler(mappedStatement, parameterObject, boundSql));

    // set ResultSetHandler
    ResultSetHandler resultSetHandler = newResultSetHandler(executor, mappedStatement, rowBounds,
        statementHandler.getParameterHandler(), resultHandler, boundSql);
    statementHandler.setResultSetHandler(resultSetHandler);

    return (StatementHandler) interceptorChain.pluginAll(statementHandler);
  }

  @Override
  public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? configuration.getDefaultExecutorType() : executorType;
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
      executor = new BatchExecutor(configuration, transaction);
    } else if (ExecutorType.REUSE == executorType) {
      executor = new ReuseExecutor(configuration, transaction);
    } else {
      executor = new SimpleExecutor(configuration, transaction);
    }
    if (configuration.isCacheEnabled()) {
      executor = new CachingExecutor(executor);
    }
    return (Executor) interceptorChain.pluginAll(executor);
  }
}

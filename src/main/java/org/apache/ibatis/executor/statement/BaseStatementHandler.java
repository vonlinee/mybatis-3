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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 */
public abstract class BaseStatementHandler implements StatementHandler {

  protected final Configuration configuration;
  protected ResultSetHandler resultSetHandler;
  protected ParameterHandler parameterHandler;

  protected Executor executor;
  protected final MappedStatement mappedStatement;
  protected final RowBounds rowBounds;

  protected BoundSql boundSql;

  protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds,
      BoundSql boundSql) {
    this.configuration = mappedStatement.getConfiguration();
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;

    this.boundSql = boundSql;
  }

  @Override
  public void setExecutor(Executor executor) {
    this.executor = executor;
  }

  @Override
  public BoundSql getBoundSql() {
    return boundSql;
  }

  @Override
  public void setParameterHandler(ParameterHandler parameterHandler) {
    this.parameterHandler = parameterHandler;
  }

  @Override
  public void setResultSetHandler(ResultSetHandler resultSetHandler) {
    this.resultSetHandler = resultSetHandler;
  }

  @Override
  public ParameterHandler getParameterHandler() {
    return parameterHandler;
  }

  @Override
  public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
    ErrorContext.instance().sql(boundSql.getSql());
    Statement statement = null;
    try {
      statement = instantiateStatement(connection, mappedStatement);
      setStatementTimeout(statement, mappedStatement, transactionTimeout);
      setFetchSize(statement, mappedStatement);
      return statement;
    } catch (SQLException e) {
      closeStatement(statement);
      throw e;
    } catch (Exception e) {
      closeStatement(statement);
      throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
    }
  }

  protected abstract Statement instantiateStatement(Connection connection, MappedStatement mappedStatement)
      throws SQLException;

  protected void setStatementTimeout(Statement stmt, MappedStatement mappedStatement, Integer transactionTimeout)
      throws SQLException {
    Integer queryTimeout = null;
    Configuration config = mappedStatement.getConfiguration();
    if (mappedStatement.getTimeout() != null) {
      queryTimeout = mappedStatement.getTimeout();
    } else if (config.getDefaultStatementTimeout() != null) {
      queryTimeout = config.getDefaultStatementTimeout();
    }
    if (queryTimeout != null) {
      stmt.setQueryTimeout(queryTimeout);
    }
    JdbcUtils.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
  }

  protected void setFetchSize(Statement stmt, MappedStatement mappedStatement) throws SQLException {
    Integer fetchSize = mappedStatement.getFetchSize();
    if (fetchSize == null) {
      Configuration config = mappedStatement.getConfiguration();
      fetchSize = config.getDefaultFetchSize();
    }
    if (fetchSize != null) {
      stmt.setFetchSize(fetchSize);
    }
  }

  protected void closeStatement(Statement statement) {
    JdbcUtils.closeSilently(statement);
  }

}

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

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

public interface ExtensionFactory {

  /**
   * Creates a {@link ParameterHandler} that passes the actual parameters to the JDBC statement.
   *
   * @author Frank D. Martinez [mnesarco]
   *
   * @param mappedStatement
   *          The mapped statement that is being executed
   * @param parameterObject
   *          The input parameter object (can be null)
   * @param boundSql
   *          The resulting SQL once the dynamic language has been executed.
   *
   * @return the parameter handler
   *
   * @see DefaultParameterHandler
   */
  ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

  ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds,
      ParameterHandler parameterHandler, ResultHandler<?> resultHandler, BoundSql boundSql);

  StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject,
      RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql);

  Executor newExecutor(Transaction transaction, ExecutorType executorType);
}

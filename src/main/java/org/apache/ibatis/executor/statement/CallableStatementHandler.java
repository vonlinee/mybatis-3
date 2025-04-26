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

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.type.TypeHandler;
import org.jetbrains.annotations.Nullable;

public interface CallableStatementHandler extends StatementHandler {

  <E> void handleOutputParameters(CallableStatement cs, Object parameterObject,
      @Nullable ResultHandler<E> resultHandler) throws SQLException;

  <T> void handleRefCursorOutputParameter(ResultSet rs, ParameterMapping pm, MetaObject metaParam,
      ResultHandler<T> handler) throws SQLException;

  TypeHandler<?> getOutputParameterTypeHandler(MetaObject metaParam, ParameterMapping parameterMapping);
}

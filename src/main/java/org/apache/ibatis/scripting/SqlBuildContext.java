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
package org.apache.ibatis.scripting;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.dialect.Dialect;
import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.session.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SqlBuildContext {

  String PARAMETER_OBJECT_KEY = "_parameter";
  String DATABASE_ID_KEY = "_databaseId";

  @NotNull
  ContextMap createBindings(Object parameterObject);

  ParamType getParamType();

  Map<String, Object> getBindings();

  void bind(String name, Object value);

  void appendSql(String sql);

  String getSql();

  List<ParameterMapping> getParameterMappings();

  String parseParam(String sql);

  @Nullable
  Object getParameterObject();

  @Nullable
  Class<?> getParameterType();

  ParamNameResolver getParamNameResolver();

  boolean isParamExists();

  Configuration getConfiguration();

  Environment getEnvironment();

  Dialect getDialect();

  ExpressionEvaluator getExpressionEvaluator();
}

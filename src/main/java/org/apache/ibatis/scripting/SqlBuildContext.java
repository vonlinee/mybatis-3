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

import java.util.List;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SqlBuildContext {

  String PARAMETER_OBJECT_KEY = "_parameter";
  String DATABASE_ID_KEY = "_databaseId";

  @NotNull
  Configuration getConfiguration();

  @NotNull
  ContextMap createBindings(@Nullable Object parameterObject);

  @NotNull
  ContextMap getBindings();

  void bind(@NotNull String name, @Nullable Object value);

  void appendSql(String sql);

  String getSql();

  List<ParameterMapping> getParameterMappings();

  String parseParam(String sql);

  Object getParameterObject();

  Class<?> getParameterType();

  ParamNameResolver getParamNameResolver();

  boolean isParamExists();
}

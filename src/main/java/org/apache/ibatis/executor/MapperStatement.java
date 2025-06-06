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

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MapperStatement implements JdbcStatement {

  @NotNull
  protected final MappedStatement mappedStatement;
  @NotNull
  protected final Configuration configuration;

  protected BoundSql boundSql;
  @Nullable
  protected Object parameterObject;

  public MapperStatement(MappedStatement mappedStatement) {
    this.mappedStatement = mappedStatement;
    this.configuration = mappedStatement.getConfiguration();
  }

  public void setParameterObject(@Nullable Object parameterObject) {
    this.parameterObject = ParamNameResolver.wrapToMapIfCollection(parameterObject, null);
  }
}

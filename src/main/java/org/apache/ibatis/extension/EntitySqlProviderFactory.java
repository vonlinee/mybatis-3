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
package org.apache.ibatis.extension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.builder.annotation.SqlProvider;
import org.apache.ibatis.builder.annotation.SqlProviderFactory;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.jetbrains.annotations.Nullable;

public class EntitySqlProviderFactory implements SqlProviderFactory {

  private final Map<String, SqlProvider> sqlProviders = new HashMap<>();

  public EntitySqlProviderFactory() {
    sqlProviders.put(SqlMethod.INSERT_ONE, new FunctionSqlProvider(EntitySqlProvider::insert));
    sqlProviders.put(SqlMethod.INSERT_BATCH, new FunctionSqlProvider(EntitySqlProvider::insertBatch));
    sqlProviders.put(SqlMethod.UPDATE_BY_ID, new FunctionSqlProvider(EntitySqlProvider::updateById));
    sqlProviders.put(SqlMethod.DELETE_BY_ID, new FunctionSqlProvider(EntitySqlProvider::deleteById));
    sqlProviders.put(SqlMethod.DELETE_BY_IDS, new FunctionSqlProvider(EntitySqlProvider::deleteByIds));
    sqlProviders.put(SqlMethod.SELECT_BY_ID, new FunctionSqlProvider(EntitySqlProvider::selectById));
    sqlProviders.put(SqlMethod.SELECT_BY_IDS, new FunctionSqlProvider(EntitySqlProvider::selectByIds));
    sqlProviders.put(SqlMethod.SELECT_ALL, new FunctionSqlProvider(EntitySqlProvider::selectAll));
    sqlProviders.put(SqlMethod.COUNT_ALL, new FunctionSqlProvider(EntitySqlProvider::countAll));
    sqlProviders.put(SqlMethod.EXISTS_BY_ID, new FunctionSqlProvider(EntitySqlProvider::existsById));
    sqlProviders.put(SqlMethod.DELETE_ALL, new FunctionSqlProvider(EntitySqlProvider::deleteAll));
    sqlProviders.put(SqlMethod.UPDATE_SELECTIVE, new FunctionSqlProvider(EntitySqlProvider::updateSelective));
  }

  @Override
  @Nullable
  public SqlProvider getSqlProvider(Class<?> mapperInterface, Method method) {
    SqlProvider sqlProvider = sqlProviders.get(method.getName());
    if (sqlProvider == null) {
      throw new IllegalArgumentException("No sql provider found for method " + method.getName());
    }
    return sqlProvider;
  }

  private static class FunctionSqlProvider implements SqlProvider {

    private final Function<ProviderContext, String> function;

    FunctionSqlProvider(Function<ProviderContext, String> function) {
      this.function = function;
    }

    @Override
    public ParamNameResolver resolveParameterNames(Class<?> mapperClass, Method mapperMethod) {
      return new ParamNameResolver(mapperClass, mapperMethod, true);
    }

    @Override
    public String provideSql(ProviderContext providerContext) {
      return function.apply(providerContext);
    }
  }
}

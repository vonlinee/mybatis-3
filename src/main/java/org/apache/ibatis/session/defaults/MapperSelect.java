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
package org.apache.ibatis.session.defaults;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.result.DefaultMapResultHandler;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.Select;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class MapperSelect extends MapperStatement<Select> implements Select {

  @Nullable
  RowBounds rowBounds = RowBounds.DEFAULT;
  @Nullable
  ResultHandler<?> handler;

  MapperSelect(DefaultSqlSession sqlSession, Executor executor, MappedStatement statement) {
    super(sqlSession, executor, statement);
  }

  @Override
  public @NotNull Select bind(Object parameter) {
    this.parameter = parameter;
    return this;
  }

  @Override
  public Select rowBounds(RowBounds rowBounds) {
    this.rowBounds = rowBounds;
    return this;
  }

  @Override
  public <T> Select resultHandler(ResultHandler<T> handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public <T> T toOne(Class<T> type) {
    // Popular vote was to return null on 0 results and throw exception on too many.
    List<T> list = selectList(parameter, rowBounds, Executor.NO_RESULT_HANDLER);
    if (list.size() == 1) {
      return list.get(0);
    }
    if (list.size() > 1) {
      throw new TooManyResultsException(
          "Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
    } else {
      return null;
    }
  }

  @Override
  public <T> T toOne() {
    return toOne(null);
  }

  @Override
  public <T> List<T> toList(Class<T> type) {
    return selectList(parameter, rowBounds, handler);
  }

  @Override
  public <T> List<T> toList() {
    return toList(null);
  }

  @Override
  public <T> Optional<T> toOptional(Class<T> type) {
    return Optional.ofNullable(toOne(type));
  }

  @Override
  public <T> Cursor<T> toCursor(Class<T> type) {
    try {
      sqlSession.markDirty(sqlSession.isDirty() | ms.isDirtySelect());
      Cursor<T> cursor = executor.queryCursor(ms, wrapCollection(parameter), rowBounds);
      sqlSession.registerCursor(cursor);
      return cursor;
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  @Override
  public <K, V> Map<K, V> toMap(String mapKey, Class<K> keyType, Class<V> valueType) {
    final List<? extends V> list = selectList(parameter, rowBounds, null);
    Configuration configuration = sqlSession.getConfiguration();
    final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<>(mapKey,
        configuration.getObjectFactory(), configuration.getObjectWrapperFactory(), configuration.getReflectorFactory());
    final DefaultResultContext<V> context = new DefaultResultContext<>();
    for (V o : list) {
      context.nextResultObject(o);
      mapResultHandler.handleResult(context);
    }
    return mapResultHandler.getMappedResults();
  }

  private <E> List<E> selectList(Object parameter, RowBounds rowBounds, ResultHandler<?> handler) {
    try {
      sqlSession.markDirty(sqlSession.isDirty() | ms.isDirtySelect());
      return executor.query(ms, wrapCollection(parameter), rowBounds, handler);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  @Override
  public int execute() {
    // result handler != null
    return selectList(parameter, rowBounds, handler).size();
  }
}

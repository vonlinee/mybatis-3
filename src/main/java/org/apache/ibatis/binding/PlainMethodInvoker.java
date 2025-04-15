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
package org.apache.ibatis.binding;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

class PlainMethodInvoker implements MapperMethodInvoker {
  private final MapperMethod mapperMethod;

  public PlainMethodInvoker(Class<?> mapperInterface, Method method, Configuration configuration) {
    this.mapperMethod = new MapperMethod(mapperInterface, method, configuration);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
    return execute(sqlSession, args);
  }

  private Object execute(SqlSession sqlSession, Object[] args) {
    SqlCommand command = mapperMethod.getCommand();
    Object result;
    switch (command.getType()) {
      case INSERT: {
        Object param = mapperMethod.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.insert(command.getName(), param));
        break;
      }
      case UPDATE: {
        Object param = mapperMethod.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.update(command.getName(), param));
        break;
      }
      case DELETE: {
        Object param = mapperMethod.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.delete(command.getName(), param));
        break;
      }
      case SELECT:
        if (mapperMethod.returnsVoid() && mapperMethod.hasResultHandler()) {
          executeWithResultHandler(sqlSession, args);
          result = null;
        } else if (mapperMethod.returnsMany()) {
          result = executeForMany(sqlSession, args);
        } else if (mapperMethod.returnsMap()) {
          result = executeForMap(sqlSession, args);
        } else if (mapperMethod.returnsCursor()) {
          result = executeForCursor(sqlSession, args);
        } else {
          Object param = mapperMethod.convertArgsToSqlCommandParam(args);
          result = sqlSession.selectOne(command.getName(), param);
          if (mapperMethod.returnsOptional()
              && (result == null || !mapperMethod.getReturnType().equals(result.getClass()))) {
            result = Optional.ofNullable(result);
          }
        }
        break;
      case FLUSH:
        result = sqlSession.flushStatements();
        break;
      default:
        throw new BindingException("Unknown execution method for: " + command.getName());
    }
    if (result == null && mapperMethod.getReturnType().isPrimitive() && !mapperMethod.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName()
          + "' attempted to return null from a method with a primitive return type (" + mapperMethod.getReturnType()
          + ").");
    }
    return result;
  }

  public Object rowCountResult(int rowCount) {
    final Object result;
    SqlCommand command = mapperMethod.getCommand();
    if (mapperMethod.returnsVoid()) {
      result = null;
    } else if (Integer.class.equals(mapperMethod.getReturnType())
        || Integer.TYPE.equals(mapperMethod.getReturnType())) {
      result = rowCount;
    } else if (Long.class.equals(mapperMethod.getReturnType()) || Long.TYPE.equals(mapperMethod.getReturnType())) {
      result = (long) rowCount;
    } else if (Boolean.class.equals(mapperMethod.getReturnType())
        || Boolean.TYPE.equals(mapperMethod.getReturnType())) {
      result = rowCount > 0;
    } else {
      throw new BindingException(
          "Mapper method '" + command.getName() + "' has an unsupported return type: " + mapperMethod.getReturnType());
    }
    return result;
  }

  public void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    SqlCommand command = mapperMethod.getCommand();
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    if (!StatementType.CALLABLE.equals(ms.getStatementType())
        && void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException(
          "method " + command.getName() + " needs either a @ResultMap annotation, a @ResultType annotation,"
              + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    Object param = mapperMethod.convertArgsToSqlCommandParam(args);
    if (mapperMethod.hasRowBounds()) {
      RowBounds rowBounds = mapperMethod.extractRowBounds(args);
      sqlSession.select(command.getName(), param, rowBounds, mapperMethod.extractResultHandler(args));
    } else {
      sqlSession.select(command.getName(), param, mapperMethod.extractResultHandler(args));
    }
  }

  public <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    SqlCommand command = mapperMethod.getCommand();
    Object param = mapperMethod.convertArgsToSqlCommandParam(args);
    if (mapperMethod.hasRowBounds()) {
      RowBounds rowBounds = mapperMethod.extractRowBounds(args);
      result = sqlSession.selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectList(command.getName(), param);
    }
    // issue #510 Collections & arrays support
    if (!mapperMethod.getReturnType().isAssignableFrom(result.getClass())) {
      if (mapperMethod.getReturnType().isArray()) {
        return convertToArray(result);
      }
      return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
    }
    return result;
  }

  public <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
    Cursor<T> result;
    SqlCommand command = mapperMethod.getCommand();
    Object param = mapperMethod.convertArgsToSqlCommandParam(args);
    if (mapperMethod.hasRowBounds()) {
      RowBounds rowBounds = mapperMethod.extractRowBounds(args);
      result = sqlSession.selectCursor(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectCursor(command.getName(), param);
    }
    return result;
  }

  public <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    Object collection = config.getObjectFactory().create(mapperMethod.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    metaObject.addAll(list);
    return collection;
  }

  @SuppressWarnings("unchecked")
  public <E> Object convertToArray(List<E> list) {
    Class<?> arrayComponentType = mapperMethod.getReturnType().getComponentType();
    Object array = Array.newInstance(arrayComponentType, list.size());
    if (!arrayComponentType.isPrimitive()) {
      return list.toArray((E[]) array);
    }
    for (int i = 0; i < list.size(); i++) {
      Array.set(array, i, list.get(i));
    }
    return array;
  }

  public <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
    Map<K, V> result;
    SqlCommand command = mapperMethod.getCommand();
    Object param = mapperMethod.convertArgsToSqlCommandParam(args);
    if (mapperMethod.hasRowBounds()) {
      RowBounds rowBounds = mapperMethod.extractRowBounds(args);
      result = sqlSession.selectMap(command.getName(), param, mapperMethod.getMapKey(), rowBounds);
    } else {
      result = sqlSession.selectMap(command.getName(), param, mapperMethod.getMapKey());
    }
    return result;
  }
}

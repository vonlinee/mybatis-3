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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.executor.result.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

class PlainMapperMethod extends MapperMethod {

  protected MethodSignature signature;
  protected ParamNameResolver paramNameResolver;

  public PlainMapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.signature = parseSignature(config, mapperInterface, method);
    this.paramNameResolver = new ParamNameResolver(mapperInterface, method, config.isUseActualParamName());
  }

  private Object convertArgsToSqlCommandParam(Object[] args) {
    return paramNameResolver.getNamedParams(args);
  }

  private String getMapKey(Method method) {
    String mapKey = null;
    if (Map.class.isAssignableFrom(method.getReturnType())) {
      final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
      if (mapKeyAnnotation != null) {
        mapKey = mapKeyAnnotation.value();
      }
    }
    return mapKey;
  }

  private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
    Integer index = null;
    final Class<?>[] argTypes = method.getParameterTypes();
    for (int i = 0; i < argTypes.length; i++) {
      if (paramType.isAssignableFrom(argTypes[i])) {
        if (index != null) {
          throw new BindingException(
              method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
        }
        index = i;
      }
    }
    return index;
  }

  public Class<?> resolveReturnType(Class<?> mapperInterface, Method method) {
    Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
    Class<?> returnType;
    if (resolvedReturnType instanceof Class<?>) {
      returnType = (Class<?>) resolvedReturnType;
    } else if (resolvedReturnType instanceof ParameterizedType) {
      returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
    } else {
      returnType = method.getReturnType();
    }
    return returnType;
  }

  public MethodSignature parseSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
    MethodSignature signature = new MethodSignature();
    signature.setReturnType(resolveReturnType(mapperInterface, method));
    signature.setReturnsVoid(void.class.equals(signature.getReturnType()));
    signature.setReturnsMany(configuration.getObjectFactory().isCollection(signature.getReturnType())
        || signature.getReturnType().isArray());
    signature.setReturnsCursor(Cursor.class.equals(signature.getReturnType()));
    signature.setReturnsOptional(Optional.class.equals(signature.getReturnType()));
    signature.setMapKey(getMapKey(method));
    signature.setReturnsMap(signature.getMapKey() != null);
    signature.setRowBoundsIndex(getUniqueParamIndex(method, RowBounds.class));
    signature.setResultHandlerIndex(getUniqueParamIndex(method, ResultHandler.class));
    return signature;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
    return execute(sqlSession, args);
  }

  private Object execute(SqlSession sqlSession, Object[] args) {
    SqlCommand command = this.command;
    Object result;
    switch (command.getType()) {
      case INSERT: {
        Object param = convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.insert(command.getName(), param));
        break;
      }
      case UPDATE: {
        Object param = convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.update(command.getName(), param));
        break;
      }
      case DELETE: {
        Object param = convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.delete(command.getName(), param));
        break;
      }
      case SELECT:
        if (signature.returnsVoid() && signature.hasResultHandler()) {
          executeWithResultHandler(sqlSession, args);
          result = null;
        } else if (signature.returnsMany()) {
          result = executeForMany(sqlSession, args);
        } else if (signature.returnsMap()) {
          result = executeForMap(sqlSession, args);
        } else if (signature.returnsCursor()) {
          result = executeForCursor(sqlSession, args);
        } else {
          Object param = convertArgsToSqlCommandParam(args);
          result = sqlSession.selectOne(command.getName(), param);
          if (signature.returnsOptional() && (result == null || !signature.getReturnType().equals(result.getClass()))) {
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
    if (result == null && signature.getReturnType().isPrimitive() && !signature.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName()
          + "' attempted to return null from a method with a primitive return type (" + signature.getReturnType()
          + ").");
    }
    return result;
  }

  public Object rowCountResult(int rowCount) {
    final Object result;
    SqlCommand command = getCommand();
    if (signature.returnsVoid()) {
      result = null;
    } else if (Integer.class.equals(signature.getReturnType()) || Integer.TYPE.equals(signature.getReturnType())) {
      result = rowCount;
    } else if (Long.class.equals(signature.getReturnType()) || Long.TYPE.equals(signature.getReturnType())) {
      result = (long) rowCount;
    } else if (Boolean.class.equals(signature.getReturnType()) || Boolean.TYPE.equals(signature.getReturnType())) {
      result = rowCount > 0;
    } else {
      throw new BindingException(
          "Mapper method '" + command.getName() + "' has an unsupported return type: " + signature.getReturnType());
    }
    return result;
  }

  public void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    SqlCommand command = getCommand();
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    if (!StatementType.CALLABLE.equals(ms.getStatementType())
        && void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException(
          "method " + command.getName() + " needs either a @ResultMap annotation, a @ResultType annotation,"
              + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    Object param = convertArgsToSqlCommandParam(args);
    if (signature.hasRowBounds()) {
      RowBounds rowBounds = signature.extractRowBounds(args);
      sqlSession.select(command.getName(), param, rowBounds, signature.extractResultHandler(args));
    } else {
      sqlSession.select(command.getName(), param, signature.extractResultHandler(args));
    }
  }

  public <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    SqlCommand command = getCommand();
    Object param = convertArgsToSqlCommandParam(args);
    if (signature.hasRowBounds()) {
      RowBounds rowBounds = signature.extractRowBounds(args);
      result = sqlSession.selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectList(command.getName(), param);
    }
    // issue #510 Collections & arrays support
    if (!signature.getReturnType().isAssignableFrom(result.getClass())) {
      if (signature.getReturnType().isArray()) {
        return convertToArray(result);
      }
      return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
    }
    return result;
  }

  public <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
    Cursor<T> result;
    SqlCommand command = getCommand();
    Object param = convertArgsToSqlCommandParam(args);
    if (signature.hasRowBounds()) {
      RowBounds rowBounds = signature.extractRowBounds(args);
      result = sqlSession.selectCursor(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectCursor(command.getName(), param);
    }
    return result;
  }

  public <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    Object collection = config.getObjectFactory().create(signature.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    metaObject.addAll(list);
    return collection;
  }

  @SuppressWarnings("unchecked")
  public <E> Object convertToArray(List<E> list) {
    Class<?> arrayComponentType = signature.getReturnType().getComponentType();
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
    SqlCommand command = getCommand();
    Object param = convertArgsToSqlCommandParam(args);
    if (signature.hasRowBounds()) {
      RowBounds rowBounds = signature.extractRowBounds(args);
      result = sqlSession.selectMap(command.getName(), param, signature.getMapKey(), rowBounds);
    } else {
      result = sqlSession.selectMap(command.getName(), param, signature.getMapKey());
    }
    return result;
  }
}

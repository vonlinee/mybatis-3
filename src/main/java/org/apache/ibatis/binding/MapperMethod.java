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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 * @author Kazuki Shimizu
 */
public class MapperMethod {

  private final SqlCommand command;
  private final MethodSignature signature;
  private final ParamNameResolver paramNameResolver;

  public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.signature = parseSignature(config, mapperInterface, method);
    this.paramNameResolver = new ParamNameResolver(config, method, mapperInterface);
  }

  public Object convertArgsToSqlCommandParam(Object[] args) {
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

  public SqlCommand getCommand() {
    return command;
  }

  public boolean hasRowBounds() {
    return signature.hasRowBounds();
  }

  public RowBounds extractRowBounds(Object[] args) {
    return hasRowBounds() ? (RowBounds) args[signature.getRowBoundsIndex()] : null;
  }

  public boolean hasResultHandler() {
    return signature.hasResultHandler();
  }

  public ResultHandler<?> extractResultHandler(Object[] args) {
    return signature.extractResultHandler(args);
  }

  public Class<?> getReturnType() {
    return signature.getReturnType();
  }

  public boolean returnsMany() {
    return signature.returnsMany();
  }

  public boolean returnsMap() {
    return signature.returnsMap();
  }

  public boolean returnsVoid() {
    return signature.returnsVoid();
  }

  public boolean returnsCursor() {
    return signature.returnsCursor();
  }

  /**
   * return whether return type is {@code java.util.Optional}.
   *
   * @return return {@code true}, if return type is {@code java.util.Optional}
   *
   * @since 3.5.0
   */
  public boolean returnsOptional() {
    return signature.returnsOptional();
  }

  public String getMapKey() {
    return signature.getMapKey();
  }
}

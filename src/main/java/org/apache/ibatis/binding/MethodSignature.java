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
package org.apache.ibatis.binding;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.extension.pagination.Page;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public class MethodSignature {

  private final boolean returnsMany;
  private final boolean returnsMap;
  private final boolean returnsVoid;
  private final boolean returnsCursor;
  private final boolean returnsIterator;
  private final boolean returnsStream;
  private final boolean returnsOptional;
  private final boolean returnsPage;
  private final Class<?> returnType;
  private final String mapKey;
  private final Integer resultHandlerIndex;
  private final Integer rowBoundsIndex;
  private final ParamNameResolver paramNameResolver;

  public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
    Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
    if (resolvedReturnType instanceof Class<?>) {
      this.returnType = (Class<?>) resolvedReturnType;
    } else if (resolvedReturnType instanceof ParameterizedType) {
      this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
    } else {
      this.returnType = method.getReturnType();
    }
    this.returnsVoid = void.class.equals(this.returnType);
    this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
    this.returnsCursor = Cursor.class.equals(this.returnType);
    this.returnsIterator = Iterator.class.equals(this.returnType);
    this.returnsStream = Stream.class.equals(this.returnType);
    this.returnsOptional = Optional.class.equals(this.returnType);
    this.returnsPage = Page.class.isAssignableFrom(this.returnType);
    this.mapKey = getMapKey(method);
    this.returnsMap = this.mapKey != null;
    this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
    this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
    this.paramNameResolver = new ParamNameResolver(mapperInterface, method, configuration.isUseActualParamName());
  }

  public Object convertArgsToSqlCommandParam(Object[] args, boolean nullValueWhenKeyNotFoundInParamMap) {
    return paramNameResolver.getNamedParams(args, nullValueWhenKeyNotFoundInParamMap);
  }

  public boolean hasRowBounds() {
    return rowBoundsIndex != null;
  }

  public RowBounds extractRowBounds(Object[] args) {
    return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
  }

  public boolean hasResultHandler() {
    return resultHandlerIndex != null;
  }

  public ResultHandler<?> extractResultHandler(Object[] args) {
    return hasResultHandler() ? (ResultHandler<?>) args[resultHandlerIndex] : null;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  public boolean returnsMany() {
    return returnsMany;
  }

  public boolean returnsMap() {
    return returnsMap;
  }

  public boolean returnsVoid() {
    return returnsVoid;
  }

  public boolean returnsCursor() {
    return returnsCursor;
  }

  public boolean returnsIterator() {
    return returnsIterator;
  }

  public boolean returnsStream() {
    return returnsStream;
  }

  public boolean returnsPage() {
    return returnsPage;
  }

  /**
   * return whether return type is {@code java.util.Optional}.
   *
   * @return return {@code true}, if return type is {@code java.util.Optional}
   *
   * @since 3.5.0
   */
  public boolean returnsOptional() {
    return returnsOptional;
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

  public String getMapKey() {
    return mapKey;
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
}

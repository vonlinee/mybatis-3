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
package org.apache.ibatis.builder;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.executor.result.Cursor;
import org.apache.ibatis.internal.util.ClassUtils;
import org.apache.ibatis.internal.util.ReflectionUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 */
public abstract class BaseBuilder {
  protected final Configuration configuration;
  protected final TypeAliasRegistry typeAliasRegistry;
  protected final TypeHandlerRegistry typeHandlerRegistry;

  public BaseBuilder(@NotNull Configuration configuration) {
    this.configuration = configuration;
    this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Pattern parseExpression(String regex, String defaultValue) {
    return Pattern.compile(regex == null ? defaultValue : regex);
  }

  public Boolean booleanValueOf(String value, Boolean defaultValue) {
    return value == null ? defaultValue : Boolean.valueOf(value);
  }

  public Integer integerValueOf(String value, Integer defaultValue) {
    return StringUtils.isNumber(value) ? Integer.valueOf(value) : defaultValue;
  }

  public Set<String> stringSetValueOf(String value, String defaultValue) {
    value = value == null ? defaultValue : value;
    return new HashSet<>(Arrays.asList(value.split(",")));
  }

  @Nullable
  protected JdbcType resolveJdbcType(String alias) {
    try {
      return alias == null ? null : JdbcType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
    }
  }

  @Nullable
  protected ResultSetType resolveResultSetType(String alias) {
    try {
      return alias == null ? null : ResultSetType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
    }
  }

  protected StatementType resolveStatementType(String statementType) {
    if (StringUtils.isBlank(statementType)) {
      return StatementType.PREPARED;
    }
    try {
      return StatementType.valueOf(statementType);
    } catch (IllegalArgumentException e) {
      throw new BuilderException(
          "unknown statement type " + statementType + ", expected: [" + Arrays.toString(StatementType.values()) + "]");
    }
  }

  @Nullable
  protected ParameterMode resolveParameterMode(String alias) {
    try {
      return alias == null ? null : ParameterMode.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
    }
  }

  @Nullable
  @SuppressWarnings("unchecked")
  protected <T> T createInstance(String alias, Class<T> requiredType) {
    Class<?> clazz = resolveClass(alias);
    if (clazz == null) {
      return null;
    }
    if (!ClassUtils.isAssignable(requiredType, clazz)) {
      throw new BuilderException(
          "Error creating instance " + requiredType + ". Cause: not compatible type " + requiredType + " to " + clazz);
    }
    try {
      return (T) ReflectionUtils.instantiateClass(clazz);
    } catch (Exception e) {
      throw new BuilderException("Error creating instance. Cause: " + e, e);
    }
  }

  @Nullable
  @SuppressWarnings("unchecked")
  protected <T> Class<T> resolveClass(String alias) {
    try {
      return alias == null ? null : (Class<T>) resolveAlias(alias);
    } catch (Exception e) {
      throw new BuilderException("Error resolving class. Cause: " + e, e);
    }
  }

  // @Deprecated(since = "3.6.0", forRemoval = true)
  @Deprecated
  @Nullable
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
    return resolveTypeHandler(null, javaType, null, typeHandlerAlias);
  }

  // @Deprecated(since = "3.6.0", forRemoval = true)
  @Deprecated
  @Nullable
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
    return resolveTypeHandler(javaType, null, typeHandlerType);
  }

  @Nullable
  protected TypeHandler<?> resolveTypeHandler(Class<?> parameterType, Type propertyType, JdbcType jdbcType,
      String typeHandlerAlias) {
    Class<? extends TypeHandler<?>> typeHandlerType;
    typeHandlerType = resolveClass(typeHandlerAlias);
    if (typeHandlerType != null && !TypeHandler.class.isAssignableFrom(typeHandlerType)) {
      throw new BuilderException("Type " + typeHandlerType.getName()
          + " is not a valid TypeHandler because it does not implement TypeHandler interface");
    }
    return resolveTypeHandler(propertyType, jdbcType, typeHandlerType);
  }

  @Nullable
  protected TypeHandler<?> resolveTypeHandler(Type javaType, JdbcType jdbcType,
      Class<? extends TypeHandler<?>> typeHandlerType) {
    if (typeHandlerType == null && jdbcType == null) {
      return null;
    }
    return typeHandlerRegistry.getTypeHandler(javaType, jdbcType, typeHandlerType);
  }

  @Nullable
  protected <T> Class<? extends T> resolveAlias(String alias) {
    return typeAliasRegistry.resolveAlias(alias);
  }

  @NotNull
  protected ParameterExpression parseParameterMapping(String content, String openToken, String closeToken) {
    try {
      return new ParameterExpression(content);
    } catch (BuilderException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BuilderException("Parsing error was found in mapping " + openToken + content + closeToken
          + ".  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
    }
  }

  protected Throwable reportThrowable(String msg, Throwable throwable) {
    if (throwable instanceof BuilderException) {
      return throwable;
    } else {
      return new BuilderException(msg, throwable);
    }
  }

  public Class<?> getMethodReturnType(String mapperFqn, String localStatementId) {
    if (mapperFqn == null || localStatementId == null) {
      return null;
    }
    // No corresponding mapper interface which is OK
    Class<?> mapperClass = Resources.classForNameOrNull(mapperFqn);
    if (mapperClass == null) {
      return null;
    }
    for (Method method : mapperClass.getMethods()) {
      if (method.getName().equals(localStatementId) && canHaveStatement(method)) {
        return getReturnType(method, mapperClass);
      }
    }
    return null;
  }

  // issue #237
  protected boolean canHaveStatement(Method method) {
    return !method.isBridge() && !method.isDefault();
  }

  protected Class<?> getReturnType(Method method, Class<?> type) {
    Class<?> returnType = method.getReturnType();
    Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
    if (resolvedReturnType instanceof Class) {
      returnType = (Class<?>) resolvedReturnType;
      if (returnType.isArray()) {
        returnType = returnType.getComponentType();
      }
      // gcode issue #508
      if (void.class.equals(returnType)) {
        ResultType rt = method.getAnnotation(ResultType.class);
        if (rt != null) {
          returnType = rt.value();
        }
      }
    } else if (resolvedReturnType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          Type returnTypeParameter = actualTypeArguments[0];
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
          } else if (returnTypeParameter instanceof ParameterizedType) {
            // (gcode issue #443) actual type can be a also a parameterized type
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
          } else if (returnTypeParameter instanceof GenericArrayType) {
            Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
            // (gcode issue #525) support List<byte[]>
            returnType = Array.newInstance(componentType, 0).getClass();
          }
        }
      } else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
        // (gcode issue 504) Do not look into Maps if there is not MapKey annotation
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 2) {
          Type returnTypeParameter = actualTypeArguments[1];
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
          } else if (returnTypeParameter instanceof ParameterizedType) {
            // (gcode issue 443) actual type can be a also a parameterized type
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
          }
        }
      } else if (Optional.class.equals(rawType)) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        Type returnTypeParameter = actualTypeArguments[0];
        if (returnTypeParameter instanceof Class<?>) {
          returnType = (Class<?>) returnTypeParameter;
        }
      }
    }
    return returnType;
  }
}

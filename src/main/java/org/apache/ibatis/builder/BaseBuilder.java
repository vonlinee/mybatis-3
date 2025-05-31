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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.Configuration;
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
    return value == null ? defaultValue : Integer.valueOf(value);
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

  @Nullable
  protected ParameterMode resolveParameterMode(String alias) {
    try {
      return alias == null ? null : ParameterMode.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
    }
  }

  @Nullable
  protected Object createInstance(String alias) {
    Class<?> clazz = resolveClass(alias);
    try {
      return clazz == null ? null : clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new BuilderException("Error creating instance. Cause: " + e, e);
    }
  }

  @Nullable
  protected <T> Class<? extends T> resolveClass(String alias) {
    try {
      return alias == null ? null : resolveAlias(alias);
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
    return configuration.getTypeHandlerRegistry().getTypeHandler(javaType, jdbcType, typeHandlerType);
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
}

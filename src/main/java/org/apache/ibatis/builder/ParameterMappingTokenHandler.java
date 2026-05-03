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
package org.apache.ibatis.builder;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.binding.ParamMap;
import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.extension.SqlValueFormatter;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.PropertyTokenizer;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

public class ParameterMappingTokenHandler extends ParameterMappingParser implements TokenHandler {

  private final List<ParameterMapping> parameterMappings;
  private final Class<?> parameterType;
  private final MetaObject metaParameters;
  private final Object parameterObject;
  private final boolean paramExists;
  private final ParamNameResolver paramNameResolver;
  private final GenericTokenParser tokenParser;
  private final ParamType paramType;

  private Type genericType = null;
  private TypeHandler<?> typeHandler = null;

  public ParameterMappingTokenHandler(List<ParameterMapping> parameterMappings, Configuration configuration,
      Object parameterObject, Class<?> parameterType, Map<String, Object> additionalParameters,
      ParamNameResolver paramNameResolver, boolean paramExists, ParamType paramType) {
    super(configuration);
    this.parameterType = parameterObject == null ? (parameterType == null ? Object.class : parameterType)
        : parameterObject.getClass();
    this.metaParameters = configuration.newMetaObject(additionalParameters);
    this.parameterObject = parameterObject;
    this.paramExists = paramExists;
    this.parameterMappings = parameterMappings;
    this.paramNameResolver = paramNameResolver;
    this.paramType = paramType;
    this.tokenParser = new GenericTokenParser(getOpenToken(), getCloseToken(), this);
  }

  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

  @Override
  public String handleToken(String content) {
    genericType = null;
    typeHandler = null;

    ParameterMapping parameterMapping = buildParameterMapping(content);
    // for SQL source at rendering phrase
    if ((paramType == ParamType.INLINED) && parameterMapping.hasValue()) {
      SqlValueFormatter formatter = configuration.getSqlValueFormatter();
      if (formatter == null) {
        throw new BuilderException("No SqlValueFormatter found for type " + parameterMapping.getJavaType());
      }
      return formatter.format(parameterMapping.getValue());
    }
    parameterMappings.add(parameterMapping);
    if (paramType == ParamType.NAMED) {
      // for raw SQL source at scripting phrase
      return asParameterExpression(content);
    }
    return "?";
  }

  public String parse(String content) {
    return tokenParser.parse(content);
  }

  @Override
  protected ParameterMapping.Builder parseParameterMappingBuilder(String content) {
    ParameterMapping.Builder builder = super.parseParameterMappingBuilder(content);
    if (builder.getExpression() != null) {
      throw new BuilderException("Expression based parameters are not supported yet");
    }
    return builder;
  }

  private ParameterMapping buildParameterMapping(String content) {
    ParameterMapping.Builder builder = parseParameterMappingBuilder(content);
    final Class<?> javaType = figureOutJavaType(builder);
    builder.javaType(javaType);
    if (genericType == null) {
      genericType = javaType;
    }
    if (typeHandler == null || builder.getTypeHandlerAlias() != null) {
      typeHandler = resolveTypeHandler(genericType, builder.getJdbcType(), builder.getTypeHandlerAlias());
    }
    builder.typeHandler(typeHandler);

    if (paramExists && !ParameterMode.OUT.equals(builder.getParameterMode())) {
      builder.value(getParameterValue(builder.getProperty()));
    }
    return builder.build();
  }

  private Object getParameterValue(String property) {
    PropertyTokenizer propertyTokenizer = new PropertyTokenizer(property);
    Object value;
    if (metaParameters.hasGetter(propertyTokenizer.getName())) {
      value = metaParameters.getValue(property);
    } else if (parameterObject == null) {
      value = null;
    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
      value = parameterObject;
    } else {
      MetaObject metaObject = configuration.newMetaObject(parameterObject);
      value = metaObject.getValue(property);
    }
    return value;
  }

  private Class<?> figureOutJavaType(ParameterMapping.Builder builder) {
    if (builder.getJavaType() != null) {
      return builder.getJavaType();
    }
    String property = builder.getProperty();
    PropertyTokenizer propertyTokenizer = new PropertyTokenizer(property);
    if (metaParameters.hasGetter(propertyTokenizer.getName())) { // issue #448 get type from additional params
      return metaParameters.getGetterType(property);
    }
    JdbcType jdbcType = builder.getJdbcType();
    typeHandler = resolveTypeHandler(parameterType, jdbcType, (Class<? extends TypeHandler<?>>) null);
    if (typeHandler != null) {
      return parameterType;
    }
    if (JdbcType.CURSOR.equals(jdbcType)) {
      return ResultSet.class;
    }
    if (paramNameResolver != null && ParamMap.class.equals(parameterType)) {
      Type actualParamType = paramNameResolver.getType(property);
      if (actualParamType != null) {
        MetaClass metaClass = MetaClass.forClass(actualParamType, configuration.getReflectorFactory());
        String multiParamsPropertyName;
        if (propertyTokenizer.hasNext()) {
          multiParamsPropertyName = propertyTokenizer.getChildren();
          if (metaClass.hasGetter(multiParamsPropertyName)) {
            Entry<Type, Class<?>> getterType = metaClass.getGenericGetterType(multiParamsPropertyName);
            genericType = getterType.getKey();
            return getterType.getValue();
          }
        } else {
          genericType = actualParamType;
        }
      }
      return Object.class;
    }
    if (Map.class.isAssignableFrom(parameterType)) {
      return Object.class;
    }
    MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
    if (metaClass.hasGetter(property)) {
      Entry<Type, Class<?>> getterType = metaClass.getGenericGetterType(property);
      genericType = getterType.getKey();
      return getterType.getValue();
    }
    return Object.class;
  }

}

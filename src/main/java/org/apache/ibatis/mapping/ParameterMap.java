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
package org.apache.ibatis.mapping;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.internal.util.CollectionUtils;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
public class ParameterMap {

  private String id;
  private Class<?> type;
  private List<ParameterMapping> parameterMappings;

  private ParameterMap() {
  }

  public static ParameterMap create(String statementId, Class<?> parameterType) {
    return buildEmpty(statementId, parameterType);
  }

  public static ParameterMap create(String statementId, Class<?> parameterType, List<ParameterMapping> mappings) {
    ParameterMap parameterMap = new ParameterMap();
    parameterMap.id = statementId;
    parameterMap.type = parameterType;
    parameterMap.parameterMappings = CollectionUtils.unmodifiableList(mappings);
    return parameterMap;
  }

  public static ParameterMap buildEmpty(String statementId, Class<?> parameterType) {
    return create(statementId, parameterType, Collections.emptyList());
  }

  public static Builder builder(Configuration config, String id, Class<?> parameterType) {
    return new Builder(config, id, parameterType);
  }

  public static class Builder {
    private final ParameterMap parameterMap = new ParameterMap();
    private final TypeHandlerRegistry registry;

    public Builder(Configuration config, String id, Class<?> type, List<ParameterMapping> parameterMappings) {
      this.registry = config.getTypeHandlerRegistry();
      parameterMap.id = id;
      parameterMap.type = type;
      parameterMap.parameterMappings = parameterMappings;
    }

    public Builder(Configuration config, String id, Class<?> type) {
      this(config, id, type, new ArrayList<>());
    }

    public Builder addMapping(String property, Type type) {
      parameterMap.parameterMappings.add(new ParameterMapping.Builder(property, registry.getTypeHandler(type)).build());
      return this;
    }

    public Builder addMapping(String property, Type type, JdbcType jdbcType) {
      parameterMap.parameterMappings
          .add(new ParameterMapping.Builder(property, registry.getTypeHandler(type)).jdbcType(jdbcType).build());
      return this;
    }

    public Builder addMapping(String property, Type type, JdbcType jdbcType, ParameterMode mode) {
      parameterMap.parameterMappings.add(
          new ParameterMapping.Builder(property, registry.getTypeHandler(type)).jdbcType(jdbcType).mode(mode).build());
      return this;
    }

    public Builder addMapping(ParameterMapping parameterMapping) {
      parameterMap.parameterMappings.add(parameterMapping);
      return this;
    }

    public Class<?> type() {
      return parameterMap.type;
    }

    public ParameterMap build() {
      // lock down collections
      parameterMap.parameterMappings = Collections.unmodifiableList(parameterMap.parameterMappings);
      return parameterMap;
    }
  }

  public String getId() {
    return id;
  }

  public Class<?> getType() {
    return type;
  }

  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

}

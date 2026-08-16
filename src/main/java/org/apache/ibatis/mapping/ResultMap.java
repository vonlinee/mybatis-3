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
import java.util.*;

import org.apache.ibatis.internal.util.CollectionUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
public class ResultMap {

  private String id;
  private Class<?> type;
  private List<ResultMapping> resultMappings;
  private List<ResultMapping> idResultMappings;
  private List<ResultMapping> constructorResultMappings;
  private List<ResultMapping> propertyResultMappings;
  private Set<String> mappedColumns;
  private Set<String> mappedProperties;
  private Discriminator discriminator;
  private boolean hasResultMapsUsingConstructorCollection;
  private boolean hasNestedResultMaps;
  private boolean hasNestedQueries;
  private Boolean autoMapping;

  private ResultMap() {
  }

  /**
   * for inlined result map
   */
  public static ResultMap create(String id, Class<?> resultType) {
    return buildEmpty(id, resultType);
  }

  public static ResultMap buildEmpty(String statementId, Class<?> resultType) {
    ResultMap emptyResultMap = new ResultMap();
    emptyResultMap.id = statementId;
    emptyResultMap.type = resultType;
    emptyResultMap.resultMappings = Collections.emptyList();
    emptyResultMap.idResultMappings = Collections.emptyList();
    emptyResultMap.constructorResultMappings = Collections.emptyList();
    emptyResultMap.propertyResultMappings = Collections.emptyList();
    emptyResultMap.mappedColumns = Collections.emptySet();
    emptyResultMap.mappedProperties = Collections.emptySet();
    return emptyResultMap;
  }

  public static ResultMap.Builder builder(Configuration config, String id, Class<?> parameterType) {
    return new Builder(config, id, parameterType);
  }

  public static class Builder {
    private final ResultMap resultMap = new ResultMap();
    private final TypeHandlerRegistry registry;
    private final Configuration config;

    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings) {
      this(configuration, id, type, resultMappings, null);
    }

    public Builder(Configuration configuration, String id, Class<?> type) {
      this(configuration, id, type, new ArrayList<>(), null);
    }

    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings,
        Boolean autoMapping) {
      this.registry = configuration.getTypeHandlerRegistry();
      this.config = configuration;
      resultMap.id = id;
      resultMap.type = type;
      resultMap.resultMappings = resultMappings;
      resultMap.autoMapping = autoMapping;
    }

    /**
     * Appends a pre-built {@link ResultMapping} directly. Use this for complex cases such as nested queries, nested
     * result maps, constructor mappings, or composite columns that require the full {@link ResultMapping.Builder} API.
     *
     * @param resultMapping
     *          the mapping to add
     *
     * @return this builder
     */
    public Builder addMapping(ResultMapping resultMapping) {
      resultMap.resultMappings.add(resultMapping);
      return this;
    }

    public Builder addNestedMapping(String property, String nestedResultMapId) {
      resultMap.resultMappings.add(new ResultMapping.Builder(property, config.isLazyLoadingEnabled())
          .nestedResultMapId(nestedResultMapId).build());
      return this;
    }

    public Builder addNestedMapping(String property, String column, Type type, Class<?> javaType, String nestQueryId) {
      resultMap.resultMappings
          .add(new ResultMapping.Builder(property, config.isLazyLoadingEnabled(), column, registry.getTypeHandler(type))
              .javaType(javaType).nestedQueryId(nestQueryId).build());
      return this;
    }

    /**
     * Appends a property-to-column mapping using the given Java {@link Type} (for {@link TypeHandler} lookup) plus a
     * fixed {@code javaType} stored on the mapping. Equivalent to calling the {@code int flags} overload with
     * {@link ResultFlag#NONE}.
     *
     * @return this builder
     */
    public Builder addMapping(String property, String column, Type type, Class<?> javaType) {
      return addMapping(property, column, type, javaType, ResultFlag.NONE);
    }

    /**
     * Appends a property-to-column mapping using the given Java {@link Type} (for {@link TypeHandler} lookup) plus a
     * fixed {@code javaType} stored on the mapping.
     *
     * @param flags
     *          a bit-mask built from {@link ResultFlag#mask()} values (or {@link ResultFlag#NONE}); combine multiple
     *          flags via bitwise OR or {@link ResultFlag#of(ResultFlag...)}
     *
     * @return this builder
     */
    public Builder addMapping(String property, String column, Type type, Class<?> javaType, int flags) {
      ResultMapping.Builder rm = new ResultMapping.Builder(property, config.isLazyLoadingEnabled(), column,
          registry.getTypeHandler(type));
      rm.flags(flags);
      rm.javaType(javaType);
      resultMap.resultMappings.add(rm.build());
      return this;
    }

    /**
     * Appends a simple property-to-column mapping using the supplied {@link TypeHandler}, with no flags.
     *
     * @return this builder
     */
    public Builder addMapping(String property, String column, TypeHandler<?> typeHandler) {
      return addMapping(property, column, typeHandler, ResultFlag.NONE);
    }

    /**
     * Appends a simple property-to-column mapping using the supplied {@link TypeHandler}.
     *
     * @param property
     *          the Java property name; may be {@code null} for constructor-arg mappings
     * @param column
     *          the result-set column name
     * @param typeHandler
     *          the type handler for this mapping
     * @param flags
     *          a bit-mask built from {@link ResultFlag#mask()} values (or {@link ResultFlag#NONE})
     *
     * @return this builder
     */
    public Builder addMapping(String property, String column, TypeHandler<?> typeHandler, int flags) {
      ResultMapping.Builder rm = new ResultMapping.Builder(property, config.isLazyLoadingEnabled(), column,
          typeHandler);
      rm.flags(flags);
      resultMap.resultMappings.add(rm.build());
      return this;
    }

    public Builder addMapping(String property, String column, TypeHandler<?> typeHandler, ResultFlag... flags) {
      return addMapping(property, column, typeHandler, ResultFlag.of(flags));
    }

    /**
     * Appends a simple property-to-column mapping whose {@link TypeHandler} is resolved from the
     * {@link TypeHandlerRegistry} by the given Java {@link Type}, with no flags.
     *
     * @return this builder
     */
    public Builder addMapping(String property, String column, Type type) {
      return addMapping(property, column, type, ResultFlag.NONE);
    }

    /**
     * Appends a simple property-to-column mapping whose {@link TypeHandler} is resolved from the
     * {@link TypeHandlerRegistry} by the given Java {@link Type}. This mirrors the behaviour of
     * {@code registry.getTypeHandler(type)} and is the preferred overload when the exact type is known at compile-time
     * (e.g. {@code int.class}, {@code String.class}).
     *
     * @param property
     *          the Java property name; may be {@code null} for constructor-arg mappings
     * @param column
     *          the result-set column name
     * @param type
     *          the Java type used to look up the {@link TypeHandler} from the registry
     * @param flags
     *          a bit-mask built from {@link ResultFlag#mask()} values (or {@link ResultFlag#NONE})
     *
     * @return this builder
     */
    public Builder addMapping(String property, String column, Type type, int flags) {
      ResultMapping.Builder rm = new ResultMapping.Builder(property, config.isLazyLoadingEnabled(), column,
          registry.getTypeHandler(type));
      rm.flags(flags);
      resultMap.resultMappings.add(rm.build());
      return this;
    }

    public Builder addMapping(String property, String column, Type type, ResultFlag... flags) {
      return addMapping(property, column, type, ResultFlag.of(flags));
    }

    /**
     * Appends a simple property-to-column mapping resolved by Java type (stored as {@code javaType} on the mapping),
     * with no flags.
     *
     * @return this builder
     */
    public Builder addMapping(String property, String column, Class<?> javaType) {
      return addMapping(property, column, javaType, ResultFlag.NONE);
    }

    /**
     * Appends a simple property-to-column mapping resolved by Java type (stored as {@code javaType} on the mapping, not
     * resolved to a {@link TypeHandler} immediately). Use this overload when the column type is unknown at build time
     * or when auto-mapping resolution is preferred.
     *
     * @param property
     *          the Java property name
     * @param column
     *          the result-set column name
     * @param javaType
     *          the Java type stored directly as {@code javaType} on the mapping
     * @param flags
     *          a bit-mask built from {@link ResultFlag#mask()} values (or {@link ResultFlag#NONE})
     *
     * @return this builder
     */
    public Builder addMapping(String property, String column, Class<?> javaType, int flags) {
      ResultMapping.Builder rm = new ResultMapping.Builder(property, config.isLazyLoadingEnabled(), column, javaType);
      rm.flags(flags);
      resultMap.resultMappings.add(rm.build());
      return this;
    }

    public Builder addMapping(String property, String column, Class<?> javaType, ResultFlag... flags) {
      return addMapping(property, column, javaType, ResultFlag.of(flags));
    }

    public Builder discriminator(Discriminator discriminator) {
      resultMap.discriminator = discriminator;
      return this;
    }

    public Builder discriminator(String property, String column, Class<?> javaType,
        Map<String, String> discriminatorMap) {
      return discriminator(new Discriminator.Builder(
          new ResultMapping.Builder(property, config.isLazyLoadingEnabled(), column, javaType).build(),
          discriminatorMap).build());
    }

    public Class<?> type() {
      return resultMap.type;
    }

    public ResultMap build() {
      if (resultMap.id == null) {
        throw new IllegalArgumentException("ResultMaps must have an id");
      }

      resultMap.mappedColumns = new HashSet<>();
      resultMap.mappedProperties = new HashSet<>();
      resultMap.idResultMappings = new ArrayList<>();
      resultMap.constructorResultMappings = new ArrayList<>();
      resultMap.propertyResultMappings = new ArrayList<>();

      for (ResultMapping resultMapping : resultMap.resultMappings) {
        resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
        resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps || resultMapping.getNestedResultMapId() != null
            && resultMapping.getResultSet() == null && !JdbcType.CURSOR.equals(resultMapping.getJdbcType());
        final String column = resultMapping.getColumn();
        if (column != null) {
          resultMap.mappedColumns.add(column.toUpperCase(Locale.ENGLISH));
        } else if (resultMapping.isCompositeResult()) {
          for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
            final String compositeColumn = compositeResultMapping.getColumn();
            if (compositeColumn != null) {
              resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
            }
          }
        }

        final String property = resultMapping.getProperty();
        if (property != null) {
          resultMap.mappedProperties.add(property);
        }

        if (resultMapping.hasFlag(ResultFlag.CONSTRUCTOR)) {
          resultMap.constructorResultMappings.add(resultMapping);

          // #101
          Class<?> javaType = resultMapping.getJavaType();
          resultMap.hasResultMapsUsingConstructorCollection = resultMap.hasResultMapsUsingConstructorCollection
              || (resultMapping.getNestedQueryId() == null && resultMapping.getTypeHandler() == null && javaType != null
                  && config.getObjectFactory().isCollection(javaType));
        } else {
          resultMap.propertyResultMappings.add(resultMapping);
        }

        if (resultMapping.hasFlag(ResultFlag.ID)) {
          resultMap.idResultMappings.add(resultMapping);
        }
      }

      if (resultMap.idResultMappings.isEmpty()) {
        resultMap.idResultMappings.addAll(resultMap.resultMappings);
      }

      // lock down collections
      resultMap.resultMappings = CollectionUtils.immutableList(resultMap.resultMappings);
      resultMap.idResultMappings = CollectionUtils.immutableList(resultMap.idResultMappings);
      resultMap.constructorResultMappings = CollectionUtils.immutableList(resultMap.constructorResultMappings);
      resultMap.propertyResultMappings = CollectionUtils.immutableList(resultMap.propertyResultMappings);
      resultMap.mappedColumns = CollectionUtils.immutableSet(resultMap.mappedColumns);
      resultMap.mappedProperties = CollectionUtils.immutableSet(resultMap.mappedProperties);

      return resultMap;
    }
  }

  public String getId() {
    return id;
  }

  public boolean hasResultMapsUsingConstructorCollection() {
    return hasResultMapsUsingConstructorCollection;
  }

  public boolean hasNestedResultMaps() {
    return hasNestedResultMaps;
  }

  public boolean hasNestedQueries() {
    return hasNestedQueries;
  }

  public Class<?> getType() {
    return type;
  }

  public List<ResultMapping> getResultMappings() {
    return resultMappings;
  }

  public boolean hasResultMappings() {
    return !resultMappings.isEmpty();
  }

  public String getMappedColumn(int columnIndex) {
    final List<ResultMapping> resultMappingList = resultMappings;
    final ResultMapping mapping = resultMappingList.get(columnIndex);
    return mapping.getColumn();
  }

  public List<ResultMapping> getConstructorResultMappings() {
    return constructorResultMappings;
  }

  public boolean hasConstructorResultMappings() {
    return !constructorResultMappings.isEmpty();
  }

  public List<ResultMapping> getPropertyResultMappings() {
    return propertyResultMappings;
  }

  public List<ResultMapping> getIdResultMappings() {
    return idResultMappings;
  }

  public Set<String> getMappedColumns() {
    return mappedColumns;
  }

  public Set<String> getMappedColumns(String columnPrefix) {
    final String upperColumnPrefix = columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH);
    return StringUtils.prependPrefixes(mappedColumns, upperColumnPrefix);
  }

  public Set<String> getMappedProperties() {
    return mappedProperties;
  }

  public boolean containsMappedProperty(String property) {
    return mappedProperties.contains(property);
  }

  public Discriminator getDiscriminator() {
    return discriminator;
  }

  public void forceNestedResultMaps() {
    hasNestedResultMaps = true;
  }

  public Boolean getAutoMapping() {
    return autoMapping;
  }

  public boolean hasLazyNestedResultMappings() {
    for (ResultMapping propertyMapping : propertyResultMappings) {
      // issue gcode #109 && issue #149
      if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
        return true;
      }
    }
    return false;
  }
}

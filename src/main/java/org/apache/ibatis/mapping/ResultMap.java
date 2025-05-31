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
package org.apache.ibatis.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.internal.util.CollectionUtils;
import org.apache.ibatis.type.JdbcType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Clinton Begin
 */
public class ResultMap {

  protected String id;
  protected Class<?> type;
  protected List<ResultMapping> resultMappings;
  protected List<ResultMapping> idResultMappings;
  protected List<ResultMapping> constructorResultMappings;
  protected List<ResultMapping> propertyResultMappings;
  protected Set<String> mappedColumns;
  protected Set<String> mappedProperties;
  protected Discriminator discriminator;
  protected boolean hasResultMapsUsingConstructorCollection;
  protected boolean hasNestedResultMaps;
  protected boolean hasNestedQueries;
  protected Boolean autoMapping;

  ResultMap() {
  }

  public static class Builder {
    private final ResultMap resultMap = new ResultMap();

    public Builder(String id, Class<?> type, List<ResultMapping> resultMappings) {
      this(id, type, resultMappings, null);
    }

    public Builder(String id, Class<?> type) {
      this(id, type, Collections.emptyList(), null);
    }

    public Builder(String id, Class<?> type, List<ResultMapping> resultMappings, Boolean autoMapping) {
      resultMap.id = id;
      resultMap.type = type;
      resultMap.resultMappings = resultMappings;
      resultMap.autoMapping = autoMapping;
    }

    public Builder discriminator(Discriminator discriminator) {
      resultMap.discriminator = discriminator;
      return this;
    }

    public Class<?> type() {
      return resultMap.type;
    }

    public ResultMap build(@NotNull Configuration configuration) {
      for (ResultMapping resultMapping : resultMap.resultMappings) {
        // #101
        Class<?> javaType = resultMapping.getJavaType();
        resultMap.hasResultMapsUsingConstructorCollection = resultMap.hasResultMapsUsingConstructorCollection
            || (resultMapping.getNestedQueryId() == null && resultMapping.getTypeHandler() == null && javaType != null
                && configuration.getObjectFactory().isCollection(javaType));
      }
      return build();
    }

    public ResultMap build() {
      if (resultMap.id == null) {
        throw new IllegalArgumentException("ResultMaps must have an id");
      }

      if (CollectionUtils.isEmpty(resultMap.resultMappings)) {
        resultMap.resultMappings = Collections.emptyList();
        resultMap.idResultMappings = Collections.emptyList();
        resultMap.constructorResultMappings = Collections.emptyList();
        resultMap.propertyResultMappings = Collections.emptyList();
        resultMap.mappedColumns = Collections.emptySet();
      } else {

        final int columnCount = resultMap.resultMappings.size();

        resultMap.mappedColumns = new HashSet<>(columnCount);
        resultMap.mappedProperties = new HashSet<>(columnCount);
        resultMap.idResultMappings = new ArrayList<>(1);
        resultMap.constructorResultMappings = new ArrayList<>(columnCount);
        resultMap.propertyResultMappings = new ArrayList<>(columnCount);

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

          if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
            resultMap.constructorResultMappings.add(resultMapping);
          } else {
            resultMap.propertyResultMappings.add(resultMapping);
          }

          if (resultMapping.getFlags().contains(ResultFlag.ID)) {
            resultMap.idResultMappings.add(resultMapping);
          }
        }

        if (resultMap.idResultMappings.isEmpty()) {
          resultMap.idResultMappings.addAll(resultMap.resultMappings);
        }

        // lock down collections
        resultMap.resultMappings = CollectionUtils.unmodifiableList(resultMap.resultMappings);
        resultMap.idResultMappings = CollectionUtils.unmodifiableList(resultMap.idResultMappings);
        resultMap.constructorResultMappings = CollectionUtils.unmodifiableList(resultMap.constructorResultMappings);
        resultMap.propertyResultMappings = CollectionUtils.unmodifiableList(resultMap.propertyResultMappings);
        resultMap.mappedColumns = CollectionUtils.unmodifiableSet(resultMap.mappedColumns);
      }
      return resultMap;
    }
  }

  public String getId() {
    return id;
  }

  public boolean hasResultMapsUsingConstructorCollection() {
    return hasResultMapsUsingConstructorCollection;
  }

  public void setHasResultMapsUsingConstructorCollection(boolean hasResultMapsUsingConstructorCollection) {
    this.hasResultMapsUsingConstructorCollection = hasResultMapsUsingConstructorCollection;
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

  public List<ResultMapping> getConstructorResultMappings() {
    return constructorResultMappings;
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

  public Set<String> getMappedProperties() {
    return mappedProperties;
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

}

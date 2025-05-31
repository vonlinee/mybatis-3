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
import java.util.List;
import java.util.Set;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.internal.util.CollectionUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 */
public class ResultMapping {

  @Deprecated
  private Configuration configuration;
  private String property;
  private String column;
  private Class<?> javaType;
  private JdbcType jdbcType;
  private TypeHandler<?> typeHandler;
  private String nestedResultMapId;
  private String nestedQueryId;
  private Set<String> notNullColumns;
  private String columnPrefix;
  private List<ResultFlag> flags;
  private List<ResultMapping> composites;
  private String resultSet;
  private String foreignColumn;
  private boolean lazy;

  ResultMapping() {
  }

  public static class Builder {
    private final ResultMapping resultMapping = new ResultMapping();

    @Deprecated
    public Builder(Configuration configuration, String property, String column, TypeHandler<?> typeHandler) {
      this(configuration, property);
      resultMapping.column = column;
      resultMapping.typeHandler = typeHandler;
    }

    public Builder(String property, String column, TypeHandler<?> typeHandler) {
      this(property);
      resultMapping.column = column;
      resultMapping.typeHandler = typeHandler;
    }

    public Builder(String property, boolean lazyLoadingEnabled, String column, TypeHandler<?> typeHandler) {
      this(property, lazyLoadingEnabled);
      resultMapping.column = column;
      resultMapping.typeHandler = typeHandler;
    }

    @Deprecated
    public Builder(Configuration configuration, String property, String column, Class<?> javaType) {
      this(configuration, property);
      resultMapping.column = column;
      resultMapping.javaType = javaType;
    }

    public Builder(String property, String column, Class<?> javaType) {
      this(property);
      resultMapping.column = column;
      resultMapping.javaType = javaType;
    }

    public Builder(String property, boolean lazyLoadingEnabled, String column, Class<?> javaType) {
      this(property, lazyLoadingEnabled);
      resultMapping.column = column;
      resultMapping.javaType = javaType;
    }

    @Deprecated
    public Builder(Configuration configuration, String property) {
      resultMapping.configuration = configuration;
      resultMapping.property = property;
      resultMapping.flags = new ArrayList<>(2);
      resultMapping.composites = new ArrayList<>();

      if (configuration != null) {
        resultMapping.lazy = configuration.isLazyLoadingEnabled();
      }
    }

    public Builder(String property) {
      resultMapping.property = property;
      resultMapping.flags = new ArrayList<>(2);
      resultMapping.composites = new ArrayList<>();
    }

    public Builder(String property, boolean lazyLoadingEnabled) {
      resultMapping.property = property;
      resultMapping.flags = new ArrayList<>(2);
      resultMapping.composites = new ArrayList<>();
      resultMapping.lazy = lazyLoadingEnabled;
    }

    public Builder(ResultMapping otherMapping) {
      this(otherMapping.configuration, otherMapping.property);

      resultMapping.flags.addAll(otherMapping.flags);
      resultMapping.composites.addAll(otherMapping.composites);

      resultMapping.column = otherMapping.column;
      resultMapping.javaType = otherMapping.javaType;
      resultMapping.jdbcType = otherMapping.jdbcType;
      resultMapping.typeHandler = otherMapping.typeHandler;
      resultMapping.nestedResultMapId = otherMapping.nestedResultMapId;
      resultMapping.nestedQueryId = otherMapping.nestedQueryId;
      resultMapping.notNullColumns = otherMapping.notNullColumns;
      resultMapping.columnPrefix = otherMapping.columnPrefix;
      resultMapping.resultSet = otherMapping.resultSet;
      resultMapping.foreignColumn = otherMapping.foreignColumn;
      resultMapping.lazy = otherMapping.lazy;
    }

    public Builder javaType(Class<?> javaType) {
      resultMapping.javaType = javaType;
      return this;
    }

    public Builder jdbcType(JdbcType jdbcType) {
      resultMapping.jdbcType = jdbcType;
      return this;
    }

    public Builder nestedResultMapId(String nestedResultMapId) {
      resultMapping.nestedResultMapId = nestedResultMapId;
      return this;
    }

    public Builder nestedQueryId(String nestedQueryId) {
      resultMapping.nestedQueryId = nestedQueryId;
      return this;
    }

    public Builder resultSet(String resultSet) {
      resultMapping.resultSet = resultSet;
      return this;
    }

    public Builder foreignColumn(String foreignColumn) {
      resultMapping.foreignColumn = foreignColumn;
      return this;
    }

    public Builder notNullColumns(Set<String> notNullColumns) {
      resultMapping.notNullColumns = notNullColumns;
      return this;
    }

    public Builder columnPrefix(String columnPrefix) {
      resultMapping.columnPrefix = columnPrefix;
      return this;
    }

    public Builder flags(List<ResultFlag> flags) {
      resultMapping.flags = flags;
      return this;
    }

    public Builder typeHandler(TypeHandler<?> typeHandler) {
      resultMapping.typeHandler = typeHandler;
      return this;
    }

    public Builder composites(List<ResultMapping> composites) {
      resultMapping.composites = composites;
      return this;
    }

    public Builder lazy(boolean lazy) {
      resultMapping.lazy = lazy;
      return this;
    }

    public ResultMapping build(Configuration config) {
      lazy(config.isLazyLoadingEnabled());
      return build();
    }

    public ResultMapping build() {
      // lock down collections
      resultMapping.flags = CollectionUtils.unmodifiableList(resultMapping.flags);
      resultMapping.composites = CollectionUtils.unmodifiableList(resultMapping.composites);
      validate();
      return resultMapping;
    }

    private void validate() {
      // Issue #697: cannot define both nestedQueryId and nestedResultMapId
      if (resultMapping.nestedQueryId != null && resultMapping.nestedResultMapId != null) {
        throw new IllegalStateException(
            "Cannot define both nestedQueryId and nestedResultMapId in property " + resultMapping.property);
      }
      // Issue #4 and GH #39: column is optional only in nested result maps but not in the rest
      if (resultMapping.nestedResultMapId == null && resultMapping.column == null
          && resultMapping.composites.isEmpty()) {
        throw new IllegalStateException("Mapping is missing column attribute for property " + resultMapping.property);
      }
      if (resultMapping.getResultSet() != null) {
        int numColumns = 0;
        if (resultMapping.column != null) {
          numColumns = resultMapping.column.split(",").length;
        }
        int numForeignColumns = 0;
        if (resultMapping.foreignColumn != null) {
          numForeignColumns = resultMapping.foreignColumn.split(",").length;
        }
        if (numColumns != numForeignColumns) {
          throw new IllegalStateException(
              "There should be the same number of columns and foreignColumns in property " + resultMapping.property);
        }
      }
    }

    public Builder column(String column) {
      resultMapping.column = column;
      return this;
    }
  }

  public String getProperty() {
    return property;
  }

  public String getColumn() {
    return column;
  }

  public Class<?> getJavaType() {
    return javaType;
  }

  public JdbcType getJdbcType() {
    return jdbcType;
  }

  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  public String getNestedResultMapId() {
    return nestedResultMapId;
  }

  public String getNestedQueryId() {
    return nestedQueryId;
  }

  public Set<String> getNotNullColumns() {
    return notNullColumns;
  }

  public String getColumnPrefix() {
    return columnPrefix;
  }

  public List<ResultFlag> getFlags() {
    return flags;
  }

  public List<ResultMapping> getComposites() {
    return composites;
  }

  public boolean isCompositeResult() {
    return this.composites != null && !this.composites.isEmpty();
  }

  public String getResultSet() {
    return this.resultSet;
  }

  public String getForeignColumn() {
    return foreignColumn;
  }

  @Deprecated
  public void setForeignColumn(String foreignColumn) {
    this.foreignColumn = foreignColumn;
  }

  public boolean isLazy() {
    return lazy;
  }

  @Deprecated
  public void setLazy(boolean lazy) {
    this.lazy = lazy;
  }

  public boolean isSimple() {
    return this.nestedResultMapId == null && this.nestedQueryId == null && this.resultSet == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResultMapping that = (ResultMapping) o;

    return property != null && property.equals(that.property);
  }

  @Override
  public int hashCode() {
    if (property != null) {
      return property.hashCode();
    }
    if (column != null) {
      return column.hashCode();
    } else {
      return 0;
    }
  }

  @Override
  public String toString() {
    String sb = "ResultMapping{" + "property='" + property + '\'' + ", column='" + column + '\'' + ", javaType="
        + javaType + ", jdbcType=" + jdbcType + ", nestedResultMapId='" + nestedResultMapId + '\'' + ", nestedQueryId='"
        + nestedQueryId + '\'' + ", notNullColumns=" + notNullColumns + ", columnPrefix='" + columnPrefix + '\''
        + ", flags=" + flags + ", composites=" + composites + ", resultSet='" + resultSet + '\'' + ", foreignColumn='"
        + foreignColumn + '\'' + ", lazy=" + lazy + '}';
    return sb;
  }

}

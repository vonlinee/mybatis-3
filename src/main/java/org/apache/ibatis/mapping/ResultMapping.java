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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.internal.util.CollectionUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 */
public class ResultMapping {

  private String property;
  private String column;
  private Class<?> javaType;
  private JdbcType jdbcType;
  private TypeHandler<?> typeHandler;
  private String nestedResultMapId;
  private String nestedQueryId;
  private Set<String> notNullColumns;
  private String columnPrefix;
  /** Bit-mask of {@link ResultFlag} values applied to this mapping. */
  private int flags;
  private List<ResultMapping> composites;
  private String resultSet;
  private String foreignColumn;
  private boolean lazy;

  ResultMapping() {
  }

  public static class Builder {
    private final ResultMapping resultMapping = new ResultMapping();

    public Builder(String property, boolean lazy, String column, TypeHandler<?> typeHandler) {
      this(property, lazy);
      resultMapping.column = column;
      resultMapping.typeHandler = typeHandler;
    }

    public Builder(String property, boolean lazy, String column, Class<?> javaType) {
      this(property, lazy);
      resultMapping.column = column;
      resultMapping.javaType = javaType;
    }

    public Builder(String property, boolean lazy) {
      resultMapping.property = property;
      resultMapping.flags = ResultFlag.NONE;
      resultMapping.composites = new ArrayList<>();
      resultMapping.lazy = lazy;
    }

    public Builder(ResultMapping otherMapping) {
      this(otherMapping.property, otherMapping.lazy);

      resultMapping.flags = otherMapping.flags;
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

    /**
     * Sets the flag bit-mask for this mapping.
     *
     * @param flags
     *          a bit-mask built from {@link ResultFlag#mask()} values (or {@link ResultFlag#NONE})
     *
     * @return this builder
     */
    public Builder flags(int flags) {
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

    public Builder composite(String property, String column, Class<?> javaType) {
      resultMapping.composites.add(new ResultMapping.Builder(property, false, column, javaType).build());
      return this;
    }

    public Builder lazy(boolean lazy) {
      resultMapping.lazy = lazy;
      return this;
    }

    public ResultMapping build() {
      // lock down collections
      resultMapping.composites = CollectionUtils.immutableList(resultMapping.composites);
      validate();
      return resultMapping;
    }

    private void validate() {
      // Issue #697: cannot define both nestedQueryId and nestedResultMapId
      if (resultMapping.nestedQueryId != null && resultMapping.nestedResultMapId != null) {
        throw new IllegalStateException(
            "Cannot define both nestedQueryId and nestedResultMapId in property " + resultMapping.property);
      }
      // Issue #4 and GH #39: column is optional only in nested resultMaps but not in the rest
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

  /**
   * @return the bit-mask of {@link ResultFlag} values applied to this mapping; {@link ResultFlag#NONE} if none.
   */
  public int getFlags() {
    return flags;
  }

  /**
   * Convenience method equivalent to {@link ResultFlag#has(int, ResultFlag)}.
   *
   * @param flag
   *          the flag to test
   *
   * @return {@code true} if {@code flag} is set on this mapping
   */
  public boolean hasFlag(ResultFlag flag) {
    return ResultFlag.has(flags, flag);
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

  public boolean isNestedResultMapping() {
    return nestedResultMapId != null && resultSet == null;
  }

  public String getForeignColumn() {
    return foreignColumn;
  }

  public boolean isLazy() {
    return lazy;
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
    final StringBuilder sb = new StringBuilder("ResultMapping{");
    // sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
    sb.append("property='").append(property).append('\'');
    sb.append(", column='").append(column).append('\'');
    sb.append(", javaType=").append(javaType);
    sb.append(", jdbcType=").append(jdbcType);
    // sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
    sb.append(", nestedResultMapId='").append(nestedResultMapId).append('\'');
    sb.append(", nestedQueryId='").append(nestedQueryId).append('\'');
    sb.append(", notNullColumns=").append(notNullColumns);
    sb.append(", columnPrefix='").append(columnPrefix).append('\'');
    sb.append(", flags=").append(flags);
    sb.append(", composites=").append(composites);
    sb.append(", resultSet='").append(resultSet).append('\'');
    sb.append(", foreignColumn='").append(foreignColumn).append('\'');
    sb.append(", lazy=").append(lazy);
    sb.append('}');
    return sb.toString();
  }

}

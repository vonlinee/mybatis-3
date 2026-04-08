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
package org.apache.ibatis.extension;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.extension.metadata.TableInfo;

public class EntitySqlProvider {

  private static final Map<Class<?>, Class<?>> entityTypeCache = new ConcurrentHashMap<>();

  /**
   * Private constructor to prevent instantiation.
   */
  private EntitySqlProvider() {
  }

  /**
   * Extracts the generic entity type from the mapper interface extending {@link CrudMapper}.
   *
   * @param mapperType
   *          the mapper interface type
   *
   * @return the generic entity type class
   */
  private static Class<?> extractEntityType(Class<?> mapperType) {
    return entityTypeCache.computeIfAbsent(mapperType, type -> {
      Type[] interfaces = type.getGenericInterfaces();
      for (Type interfaceType : interfaces) {
        if (interfaceType instanceof ParameterizedType) {
          ParameterizedType pt = (ParameterizedType) interfaceType;
          if (pt.getRawType() == CrudMapper.class) {
            return (Class<?>) pt.getActualTypeArguments()[0];
          }
        }
      }
      throw new IllegalArgumentException("Cannot extract entity type from " + mapperType.getName()
          + ", because it does not directly extend CrudMapper<T> or generic definition is missing.");
    });
  }

  /**
   * Helper method to obtain {@link TableInfo} for a given mapper context.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the table metadata
   */
  private static TableInfo getTableInfo(ProviderContext context) {
    Class<?> entityType = extractEntityType(context.getMapperType());
    return TableInfo.of(entityType);
  }

  /**
   * Generates an INSERT SQL statement.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String insert(ProviderContext context) {
    return SqlUtils.getInsertIntoTableSql(getTableInfo(context));
  }

  /**
   * Generates a batch INSERT SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String insertBatch(ProviderContext context) {
    return SqlUtils.getBatchInsertScriptSql(getTableInfo(context), "item");
  }

  /**
   * Generates an UPDATE SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String updateById(ProviderContext context) {
    return SqlUtils.getUpdateByIdScriptSql(getTableInfo(context));
  }

  /**
   * Generates a DELETE by ID SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String deleteById(ProviderContext context) {
    return SqlUtils.getDeleteByIdScriptSql(getTableInfo(context));
  }

  /**
   * Generates a DELETE by multiple IDs SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String deleteByIds(ProviderContext context) {
    return SqlUtils.getDeleteByIdsScriptSql(getTableInfo(context));
  }

  /**
   * Generates a SELECT by ID SQL statement.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL string
   */
  public static String selectById(ProviderContext context) {
    return SqlUtils.getSelectByIdScriptSql(getTableInfo(context));
  }

  /**
   * Generates a SELECT by multiple IDs SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String selectByIds(ProviderContext context) {
    return SqlUtils.getSelectByIdsScriptSql(getTableInfo(context));
  }

  /**
   * Generates a SELECT ALL SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String selectAll(ProviderContext context) {
    return SqlUtils.getSelectAllScriptSql(getTableInfo(context));
  }

  /**
   * Generates a COUNT ALL SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String countAll(ProviderContext context) {
    return SqlUtils.getCountAllScriptSql(getTableInfo(context));
  }

  /**
   * Generates an EXISTS BY ID SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String existsById(ProviderContext context) {
    return SqlUtils.getExistsByIdScriptSql(getTableInfo(context));
  }

  /**
   * Generates a DELETE ALL SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String deleteAll(ProviderContext context) {
    return SqlUtils.getDeleteAllScriptSql(getTableInfo(context));
  }

  /**
   * Generates an UPDATE SPECIFIED FIELDS SQL script.
   *
   * @param context
   *          the MyBatis provider context
   *
   * @return the SQL script
   */
  public static String updateSelective(ProviderContext context) {
    return SqlUtils.getConditionalFieldUpdateSql(getTableInfo(context));
  }
}

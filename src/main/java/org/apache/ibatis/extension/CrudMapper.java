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

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.*;

/**
 * A generic mapper interface that provides common CRUD (Create, Read, Update, Delete) operations.
 * <p>
 * Mapper interfaces should extend this interface and specify the entity type as the generic parameter. Doing so
 * automatically inherits standard database operations generated dynamically by {@link EntitySqlProvider}.
 * </p>
 *
 * @param <T>
 *          the entity type representing a database table
 */
public interface CrudMapper<T> {

  /**
   * Inserts a single entity into the database. Null values will be explicitly inserted unless the database default
   * takes over. Auto-increment primary keys are ignored in the insert statement.
   *
   * @param entity
   *          the entity to insert
   *
   * @return the number of affected rows
   */
  @InsertProvider(type = EntitySqlProvider.class, method = SqlMethod.INSERT_ONE)
  int insert(T entity);

  /**
   * Inserts multiple entities into the database in a single batch. Auto-increment primary keys are ignored.
   *
   * @param entities
   *          the collection of entities to insert
   *
   * @return the number of affected rows
   */
  @InsertProvider(type = EntitySqlProvider.class, method = SqlMethod.INSERT_BATCH)
  int insertBatch(@Param("collection") Collection<T> entities);

  /**
   * Updates an existing entity by its primary key. Only non-null fields in the entity are updated; null fields are
   * ignored.
   *
   * @param entity
   *          the entity containing updated values and the primary key
   *
   * @return the number of affected rows
   */
  @UpdateProvider(type = EntitySqlProvider.class, method = SqlMethod.UPDATE_BY_ID)
  int updateById(T entity);

  /**
   * Deletes an entity by its primary key.
   *
   * @param id
   *          the primary key of the entity to delete
   *
   * @return the number of affected rows
   */
  @DeleteProvider(type = EntitySqlProvider.class, method = SqlMethod.DELETE_BY_ID)
  int deleteById(Object id);

  /**
   * Deletes multiple entities by their primary keys.
   *
   * @param ids
   *          the collection of primary keys
   *
   * @return the number of affected rows
   */
  @DeleteProvider(type = EntitySqlProvider.class, method = SqlMethod.DELETE_BY_IDS)
  int deleteByIds(@Param("collection") Collection<?> ids);

  /**
   * Selects a single entity by its primary key.
   *
   * @param id
   *          the primary key
   *
   * @return the entity, or null if not found
   */
  @SelectProvider(type = EntitySqlProvider.class, method = SqlMethod.SELECT_BY_ID)
  T selectById(Object id);

  /**
   * Selects multiple entities by their primary keys.
   *
   * @param ids
   *          the collection of primary keys
   *
   * @return a list of entities matching the primary keys
   */
  @SelectProvider(type = EntitySqlProvider.class, method = SqlMethod.SELECT_BY_IDS)
  List<T> selectByIds(@Param("collection") Collection<?> ids);

  /**
   * Selects all entities.
   *
   * @return a list of all entities
   */
  @SelectProvider(type = EntitySqlProvider.class, method = SqlMethod.SELECT_ALL)
  List<T> selectAll();

  /**
   * Counts all entities in the database.
   *
   * @return the total count of entities
   */
  @SelectProvider(type = EntitySqlProvider.class, method = SqlMethod.COUNT_ALL)
  long countAll();

  /**
   * Checks if an entity exists by its primary key.
   *
   * @param id
   *          the primary key
   *
   * @return true if the entity exists, false otherwise
   */
  @SelectProvider(type = EntitySqlProvider.class, method = SqlMethod.EXISTS_BY_ID)
  boolean existsById(Object id);

  /**
   * Deletes all entities.
   *
   * @return the number of affected rows
   */
  @DeleteProvider(type = EntitySqlProvider.class, method = SqlMethod.DELETE_ALL)
  int deleteAll();

  /**
   * Updates specified fields of an existing entity by its primary key. Only the fields listed in the `fields`
   * collection are updated.
   *
   * @param entity
   *          the entity containing updated values and the primary key
   * @param fields
   *          the explicitly specified fields to update (Java field names)
   *
   * @return the number of affected rows
   */
  @UpdateProvider(type = EntitySqlProvider.class, method = SqlMethod.UPDATE_SELECTIVE)
  int updateSelective(@Param("entity") T entity, @Param("fields") Collection<String> fields);
}

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
package org.apache.ibatis.session;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.cursor.Cursor;

public interface Select extends JdbcStatement<Select> {

  /**
   * @param rowBounds
   *          row bounds
   */
  Select rowBounds(RowBounds rowBounds);

  /**
   * @param handler
   *          ResultHandler that will handle each retrieved row
   */
  <T> Select resultHandler(ResultHandler<T> handler);

  <T> T toOne(Class<T> type);

  /**
   * @param <T>
   *          row type
   *
   * @return single row
   */
  <T> T toOne();

  <T> List<T> toList();

  default Integer toInteger() {
    return toOne(Integer.class);
  }

  default <K, V> List<Map<K, V>> toMapList() {
    return toList();
  }

  <T> List<T> toList(Class<T> type);

  <T> Optional<T> toOptional(Class<T> type);

  <T> Cursor<T> toCursor(Class<T> type);

  default <T> Cursor<T> toCursor() {
    return toCursor(null);
  }

  default <K, V> Map<K, V> toMap() {
    return toOne();
  }

  <K, V> Map<K, V> toMap(String mapKey, Class<K> keyType, Class<V> valueType);

  default <K, V> Map<K, V> toMap(String mapKey) {
    return toMap(mapKey, null, null);
  }

  /**
   * @see Select#resultHandler(ResultHandler)
   */
  @Override
  int execute();
}

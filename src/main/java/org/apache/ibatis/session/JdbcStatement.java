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

import org.apache.ibatis.mapping.SqlCommandType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JdbcStatement<S extends JdbcStatement<S>> {

  @NotNull
  SqlCommandType getSqlCommandType();

  int execute();

  /**
   * @param parameter
   *          A parameter object to pass to the statement.
   */
  @NotNull
  S bind(Object parameter);

  /**
   * @return current sql
   */
  @Nullable
  String getSql();
}

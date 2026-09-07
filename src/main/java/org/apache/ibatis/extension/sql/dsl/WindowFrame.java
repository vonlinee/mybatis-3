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
package org.apache.ibatis.extension.sql.dsl;

import java.util.Objects;

public final class WindowFrame {

  public static final WindowFrame UNBOUNDED_PRECEDING = new WindowFrame("UNBOUNDED PRECEDING");
  public static final WindowFrame CURRENT_ROW = new WindowFrame("CURRENT ROW");
  public static final WindowFrame UNBOUNDED_FOLLOWING = new WindowFrame("UNBOUNDED FOLLOWING");

  private final String sql;

  private WindowFrame(String sql) {
    this.sql = sql;
  }

  public static WindowFrame preceding(int rows) {
    return rows(rows, "PRECEDING");
  }

  public static WindowFrame following(int rows) {
    return rows(rows, "FOLLOWING");
  }

  private static WindowFrame rows(int rows, String direction) {
    if (rows < 0) {
      throw new IllegalArgumentException("window frame row count must not be negative");
    }
    return new WindowFrame(rows + " " + direction);
  }

  String sql() {
    return Objects.requireNonNull(sql);
  }
}

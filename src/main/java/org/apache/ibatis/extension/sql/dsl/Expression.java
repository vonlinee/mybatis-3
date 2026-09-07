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

import java.util.Collection;

public interface Expression<T> extends SelectItem {

  Condition eq(Object value);

  Condition ne(Object value);

  Condition gt(Object value);

  Condition ge(Object value);

  Condition lt(Object value);

  Condition le(Object value);

  Condition like(Object value);

  Condition in(Object... values);

  Condition in(Collection<?> values);

  Condition isNull();

  Condition isNotNull();

  default WindowBuilder over() {
    return new WindowBuilder(this);
  }
}

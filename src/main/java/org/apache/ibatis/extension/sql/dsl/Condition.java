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

public interface Condition {

  default Condition and(Condition other) {
    return new DslNodes.CompoundCondition("AND", (DslNodes.Node) this, (DslNodes.Node) other);
  }

  default Condition or(Condition other) {
    return new DslNodes.CompoundCondition("OR", (DslNodes.Node) this, (DslNodes.Node) other);
  }

  default Condition not() {
    return new DslNodes.UnaryCondition("NOT", (DslNodes.Node) this);
  }
}

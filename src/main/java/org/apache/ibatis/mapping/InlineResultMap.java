/*
 *    Copyright 2009-2022 the original author or authors.
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

import java.util.Collections;

/**
 * this is for the statement which the resultMap is not specified explicitly.
 */
public class InlineResultMap extends ResultMap {

  public InlineResultMap(String statementId, Class<?> resultType) {
    this.id = statementId + "-Inline";
    this.type = resultType;
    this.autoMapping = null;

    this.mappedProperties = Collections.emptySet();
    this.discriminator = null;
    // lock down collections
    this.resultMappings = Collections.emptyList();
    this.idResultMappings = Collections.emptyList();
    this.constructorResultMappings = Collections.emptyList();
    this.propertyResultMappings = Collections.emptyList();
    this.mappedColumns = Collections.emptySet();
  }
}

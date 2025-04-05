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

import java.util.Collections;
import java.util.List;

/**
 * @author Clinton Begin
 */
public class ParameterMap {

  private final String id;
  private final Class<?> type;
  private final List<ParameterMapping> parameterMappings;

  public ParameterMap(String id, Class<?> type, List<ParameterMapping> parameterMappings) {
    this.id = id;
    this.type = type;
    this.parameterMappings = parameterMappings == null ? Collections.emptyList() : parameterMappings;
  }

  public ParameterMap(String id, Class<?> type) {
    this.id = id;
    this.type = type;
    this.parameterMappings = Collections.emptyList();
  }

  public String getId() {
    return id;
  }

  public Class<?> getType() {
    return type;
  }

  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

}

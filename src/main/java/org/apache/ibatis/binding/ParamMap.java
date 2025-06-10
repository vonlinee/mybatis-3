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
package org.apache.ibatis.binding;

import java.util.HashMap;

public class ParamMap extends HashMap<String, Object> {

  private static final long serialVersionUID = -2212268410512043556L;

  @Override
  public Object get(Object key) {
    if (!super.containsKey(key)) {
      throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
    }
    return super.get(key);
  }

  protected void assertContainsParamName(Object key) {
    if (!super.containsKey(key)) {
      throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
    }
  }

  public int getInt(Object key) {
    assertContainsParamName(key);
    Object value = super.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    } else {
      String typeName = value == null ? "null" : value.getClass().getName();
      throw new BindingException(
          "Parameter '" + key + "' exists but type mismatch. expected number, actual " + typeName);
    }
  }
}

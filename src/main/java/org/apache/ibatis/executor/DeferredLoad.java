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
package org.apache.ibatis.executor;

import org.apache.ibatis.reflection.MetaObject;

public class DeferredLoad {

  private final MetaObject resultObject;
  private final String property;
  private final Class<?> targetType;
  private final Object cacheKey;

  // issue #781
  public DeferredLoad(MetaObject resultObject, String property, Object cacheKey, Class<?> targetType) {
    this.resultObject = resultObject;
    this.property = property;
    this.cacheKey = cacheKey;
    this.targetType = targetType;
  }

  public Object getCacheKey() {
    return cacheKey;
  }

  public Class<?> getTargetType() {
    return targetType;
  }

  public void setValue(Object value) {
    resultObject.setValue(property, value);
  }

}

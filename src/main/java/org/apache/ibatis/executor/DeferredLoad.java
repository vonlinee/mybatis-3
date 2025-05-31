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

import java.util.List;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.reflection.MetaObject;

class DeferredLoad {

  private final MetaObject resultObject;
  private final String property;
  private final Class<?> targetType;
  private final CacheKey key;
  private final Cache localCache;
  private final ResultExtractor resultExtractor;

  // issue #781
  public DeferredLoad(MetaObject resultObject, String property, CacheKey key, Cache localCache,
      Configuration configuration, Class<?> targetType) {
    this.resultObject = resultObject;
    this.property = property;
    this.key = key;
    this.localCache = localCache;
    this.resultExtractor = new ResultExtractor(configuration, configuration.getObjectFactory());
    this.targetType = targetType;
  }

  public boolean canLoad() {
    return localCache.getObject(key) != null && localCache.getObject(key) != ExecutionPlaceholder.EXECUTION_PLACEHOLDER;
  }

  public void load() {
    @SuppressWarnings("unchecked")
    // we suppose we get back a List
    List<Object> list = (List<Object>) localCache.getObject(key);
    Object value = resultExtractor.extractObjectFromList(list, targetType);
    resultObject.setValue(property, value);
  }

}

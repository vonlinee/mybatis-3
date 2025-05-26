package org.apache.ibatis.executor;

import java.util.List;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

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

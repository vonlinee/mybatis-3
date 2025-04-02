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

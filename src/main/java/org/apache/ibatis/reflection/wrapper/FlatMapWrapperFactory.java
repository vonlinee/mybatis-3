package org.apache.ibatis.reflection.wrapper;

import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;

public class FlatMapWrapperFactory implements ObjectWrapperFactory {
  @Override
  public boolean hasWrapperFor(Object object) {
    return object instanceof Map;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
    return new FlatMapWrapper(metaObject, (Map<String, Object>) object);
  }
}

package org.apache.ibatis.reflection.wrapper;

import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

public class FlatMapWrapper extends MapWrapper {
  public FlatMapWrapper(MetaObject metaObject, Map<String, Object> map) {
    super(metaObject, map);
  }

  @Override
  public Object get(PropertyTokenizer prop) {
    String key;
    if (prop.getChildren() == null) {
      key = prop.getIndexedName();
    } else {
      key = prop.getIndexedName() + "." + prop.getChildren();
    }
    return map.get(key);
  }

  @Override
  public void set(PropertyTokenizer prop, Object value) {
    String key;
    if (prop.getChildren() == null) {
      key = prop.getIndexedName();
    } else {
      key = prop.getIndexedName() + "." + prop.getChildren();
    }
    map.put(key, value);
  }
}

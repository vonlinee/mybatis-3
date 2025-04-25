package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.MetaObject;

class PendingRelation {
  public MetaObject metaObject;
  public ResultMapping propertyMapping;

  public PendingRelation(MetaObject metaObject, ResultMapping propertyMapping) {
    this.metaObject = metaObject;
    this.propertyMapping = propertyMapping;
  }
}

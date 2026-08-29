/*
 *    Copyright 2009-2026 the original author or authors.
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

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionUtils;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;

/**
 * @author Andrew Gustafson
 */
public final class ResultExtractor {

  private ResultExtractor() {
  }

  public static Object extractObjectFromList(Configuration configuration, List<Object> list, Class<?> targetType) {
    return extractObjectFromList(configuration, configuration.getObjectFactory(), list, targetType);
  }

  public static Object extractObjectFromList(Configuration configuration, ObjectFactory objectFactory,
      List<Object> list, Class<?> targetType) {
    Object value = null;
    if (targetType != null && targetType.isAssignableFrom(list.getClass())) {
      value = list;
    } else if (targetType != null && objectFactory.isCollection(targetType)) {
      value = objectFactory.create(targetType);
      MetaObject metaObject = configuration.newMetaObject(value);
      metaObject.addAll(list);
    } else if (targetType != null && targetType.isArray()) {
      value = ReflectionUtils.convertToArray(list, targetType.getComponentType());
    } else if (list != null && list.size() > 1) {
      throw new ExecutorException("Statement returned more than one row, where no more than one was expected.");
    } else if (list != null && list.size() == 1) {
      value = list.get(0);
    }
    return value;
  }
}

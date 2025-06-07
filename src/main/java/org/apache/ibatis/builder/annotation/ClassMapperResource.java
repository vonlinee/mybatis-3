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
package org.apache.ibatis.builder.annotation;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.MapperResource;
import org.apache.ibatis.io.Resources;
import org.jetbrains.annotations.NotNull;

public class ClassMapperResource implements MapperResource {

  private final Class<?> mapperClass;

  public ClassMapperResource(@NotNull Class<?> mapperClass) {
    this.mapperClass = mapperClass;
  }

  public ClassMapperResource(@NotNull String mapperClassName) {
    this.mapperClass = Resources.classForNameOrNull(mapperClassName);
  }

  @Override
  public boolean exists() {
    return mapperClass != null;
  }

  @Override
  public String getNamespace() {
    if (mapperClass == null) {
      return null;
    }
    return mapperClass.getName();
  }

  @Override
  public String getResourceName() {
    if (mapperClass == null) {
      return null;
    }
    return mapperClass.getName();
  }

  @Override
  public boolean build(@NotNull Configuration config) {
    if (mapperClass == null) {
      return false;
    }
    config.addMapper(mapperClass);
    return true;
  }
}

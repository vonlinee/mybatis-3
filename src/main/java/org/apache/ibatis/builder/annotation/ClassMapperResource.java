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

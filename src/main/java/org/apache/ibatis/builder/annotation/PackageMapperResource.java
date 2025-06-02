package org.apache.ibatis.builder.annotation;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.MapperResource;
import org.jetbrains.annotations.NotNull;

public class PackageMapperResource implements MapperResource {

  private final String packageName;

  public PackageMapperResource(String packageName) {
    this.packageName = packageName;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public String getNamespace() {
    return null;
  }

  @Override
  public String getResourceName() {
    return packageName;
  }

  @Override
  public boolean build(@NotNull Configuration config) {
    config.addMappers(packageName);
    return true;
  }
}

package org.apache.ibatis.builder;

import java.util.Properties;

import org.jetbrains.annotations.NotNull;

public interface MapperResource {

  default boolean exists() {
    return true;
  }

  String getNamespace();

  String getResourceName();

  default void init(Properties properties) {
  }

  default boolean build(@NotNull Configuration config) {
    return true;
  }

  default void cleanup(Configuration config) throws Exception {
  }
}

package org.apache.ibatis.internal;

import java.util.Properties;

import org.apache.ibatis.internal.util.ClassUtils;
import org.apache.ibatis.internal.util.ObjectUtils;
import org.jetbrains.annotations.Nullable;

public class PropertiesHolder {

  @Nullable
  private Properties properties;

  public void setProperties(@Nullable Properties properties) {
    this.properties = properties;
  }

  @Nullable
  public Properties getProperties() {
    return properties;
  }

  public String getProperty(String key) {
    if (properties == null) {
      return null;
    }
    return properties.getProperty(key);
  }

  public int getInt(String key, int def) {
    String value = getProperty(key);
    return ObjectUtils.parseInt(value, def);
  }

  public long getLong(String key, long def) {
    String value = getProperty(key);
    if (value == null) {
      return def;
    }
    try {
      return Long.parseLong(key);
    } catch (Throwable throwable) {
      return def;
    }
  }

  public boolean getBoolean(String key, boolean def) {
    String value = getProperty(key);
    if (value == null) {
      return def;
    }
    value = value.trim();
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    return def;
  }

  /**
   * @param key
   *          property key
   *
   * @return class or null
   *
   * @see ClassUtils#classForNameOrNull(String)
   */
  @Nullable
  public Class<?> getClass(String key) {
    return ClassUtils.classForNameOrNull(getProperty(key));
  }

  public boolean containsPropertyValue(String key) {
    String value = getProperty(key);
    if (value == null) {
      return false;
    }
    return !"null".equalsIgnoreCase(value);
  }
}

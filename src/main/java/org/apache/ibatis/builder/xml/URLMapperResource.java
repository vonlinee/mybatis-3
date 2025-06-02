package org.apache.ibatis.builder.xml;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.MapperResource;
import org.apache.ibatis.io.Resources;
import org.jetbrains.annotations.NotNull;

public class URLMapperResource implements MapperResource {

  private final String url;

  private String namespace;

  public URLMapperResource(String url) {
    this.url = url;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public String getResourceName() {
    return url;
  }

  @Override
  public boolean build(@NotNull Configuration config) {
    try (InputStream inputStream = Resources.getUrlAsStream(url)) {
      XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, config, url, config.getSqlFragments());
      mapperParser.parse();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return true;
  }
}

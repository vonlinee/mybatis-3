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

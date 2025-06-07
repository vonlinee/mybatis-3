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

import java.io.InputStream;
import java.util.Properties;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.MapperResource;
import org.apache.ibatis.internal.util.IOUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.jetbrains.annotations.NotNull;

public class XMLMapperResource implements MapperResource, AutoCloseable {

  private final String resource;

  private InputStream inputStream;
  private XPathParser parser;
  private String namespace;

  public XMLMapperResource(String resource) {
    this.resource = resource;
    this.inputStream = Resources.getResourceAsStreamOrNull(getClass().getClassLoader(), resource);
    this.parser = new XPathParser(inputStream, true, null, new XMLMapperEntityResolver());
  }

  @Override
  public boolean exists() {
    return inputStream != null;
  }

  @Override
  public String getNamespace() {
    if (this.namespace == null) {
      if (parser == null) {
        return null;
      }
      XNode mapperNode = parser.evalRootNode();
      if (mapperNode != null) {
        this.namespace = mapperNode.getStringAttribute("namespace");
      }
    }
    return namespace;
  }

  @Override
  public String getResourceName() {
    return resource;
  }

  @Override
  public void init(Properties properties) {
    MapperResource.super.init(properties);
  }

  @Override
  public boolean build(@NotNull Configuration config) {
    if (!exists()) {
      return true;
    }
    parser.setVariables(config.getVariables());
    XMLMapperBuilder mapperParser = new XMLMapperBuilder(parser, config, resource, config.getSqlFragments());
    mapperParser.parse();
    return true;
  }

  @Override
  public void cleanup(Configuration config) throws Exception {
    close();
  }

  @Override
  public void close() throws Exception {
    IOUtils.closeSilently(this.inputStream);
    this.inputStream = null;
    this.parser = null;
  }
}

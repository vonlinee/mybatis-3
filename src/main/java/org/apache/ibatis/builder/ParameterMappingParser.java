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
package org.apache.ibatis.builder;

import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

public class ParameterMappingParser extends BaseBuilder {

  private static final String PARAMETER_PROPERTIES = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

  public ParameterMappingParser(Configuration configuration) {
    super(configuration);
  }

  protected ParameterMapping.Builder parseParameterMappingBuilder(String content) {
    ParameterExpression propertiesMap = parseParameterMapping(content);

    final String property = propertiesMap.remove("property");
    final JdbcType jdbcType = resolveJdbcType(propertiesMap.remove("jdbcType"));
    final String typeHandlerAlias = propertiesMap.remove("typeHandler");

    ParameterMapping.Builder builder = ParameterMapping.builder(property, jdbcType);
    builder.typeHandler(typeHandlerAlias);

    for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
      String name = entry.getKey();
      String value = entry.getValue();
      if ("mode".equals(name)) {
        builder.mode(resolveParameterMode(value));
      } else if ("numericScale".equals(name)) {
        builder.numericScale(Integer.valueOf(value));
      } else if ("resultMap".equals(name)) {
        builder.resultMapId(value);
      } else if ("jdbcTypeName".equals(name)) {
        builder.jdbcTypeName(value);
      } else if ("expression".equals(name)) {
        builder.expression(value);
      } else if ("javaType".equals(name)) {
        builder.javaType(resolveClass(value));
      } else {
        throw new BuilderException("An invalid property '" + name + "' was found in mapping "
            + asParameterExpression(content) + ".  Valid properties are " + PARAMETER_PROPERTIES);
      }
    }
    return builder;
  }

  protected ParameterExpression parseParameterMapping(String content) {
    try {
      return new ParameterExpression(content);
    } catch (BuilderException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BuilderException("Parsing error was found in mapping " + asParameterExpression(content)
          + ".  Check syntax " + asParameterExpression("property|(expression), var1=value1, var2=value2, ...") + " ",
          ex);
    }
  }

  public String getOpenToken() {
    return "#{";
  }

  public String getCloseToken() {
    return "}";
  }

  public String asParameterExpression(String content) {
    return getOpenToken() + content + getCloseToken();
  }
}

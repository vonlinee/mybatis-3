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
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * @see ParameterMappingTokenHandler
 * @see ParameterMapping
 */
public class ParameterMappingCollector extends BaseBuilder implements TokenHandler {

  private final List<ParameterMapping> parameterMappings;

  public ParameterMappingCollector(Configuration configuration) {
    super(configuration);
    this.parameterMappings = new ArrayList<>();
  }

  @Override
  public String handleToken(String content) {
    ParameterMapping parameter = buildParameterMapping(content);
    parameterMappings.add(parameter);
    return content;
  }

  protected ParameterMapping buildParameterMapping(String content) {
    ParameterExpression expression = parseParameterMapping(content, "#{", "}");

    final String property = expression.remove("property");
    final JdbcType jdbcType = resolveJdbcType(expression.remove("jdbcType"));
    final String typeHandlerAlias = expression.remove("typeHandler");

    ParameterMapping.Builder builder = new ParameterMapping.Builder(property, (Class<?>) null);
    builder.jdbcType(jdbcType);

    builder.typeHandler(typeHandlerAlias);

    ParameterMode mode;
    for (Map.Entry<String, String> entry : expression.entrySet()) {
      String name = entry.getKey();
      String value = entry.getValue();
      if ("mode".equals(name)) {
        mode = resolveParameterMode(value);
        builder.mode(mode);
      } else if ("numericScale".equals(name)) {
        builder.numericScale(Integer.valueOf(value));
      } else if ("resultMap".equals(name)) {
        builder.resultMapId(value);
      } else if ("jdbcTypeName".equals(name)) {
        builder.jdbcTypeName(value);
      } else if ("expression".equals(name)) {
        throw new BuilderException("Expression based parameters are not supported yet");
      } else {
        throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content
            + "}.  Valid properties are " + ParameterMappingTokenHandler.PARAMETER_PROPERTIES);
      }
    }
    return builder.build();
  }

  List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

  public static List<ParameterMapping> collectParameterMappings(Configuration configuration, String text) {
    ParameterMappingCollector handler = new ParameterMappingCollector(configuration);
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    parser.parse(text);
    return handler.getParameterMappings();
  }
}

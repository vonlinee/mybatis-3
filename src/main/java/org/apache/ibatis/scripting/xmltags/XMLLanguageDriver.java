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
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.TokenParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.scripting.BoundSql;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.MappedStatement;
import org.apache.ibatis.scripting.MethodParamMetadata;
import org.apache.ibatis.scripting.SqlSource;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.scripting.defaults.RawSqlSource;

/**
 * @author Eduardo Macarron
 */
public class XMLLanguageDriver implements LanguageDriver {

  private XMLScriptBuilder builder;

  @Override
  public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject,
      BoundSql boundSql) {
    return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
    return createSqlSource(configuration, script, parameterType, null);
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType,
      MethodParamMetadata methodParamMetadata) {
    if (builder == null) {
      builder = new XMLScriptBuilder(configuration);
    }
    return builder.parseScriptNode(script, parameterType, methodParamMetadata);
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
    return createSqlSource(configuration, script, parameterType, null);
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
      MethodParamMetadata paramNameResolver) {
    // issue #3
    if (script.startsWith("<script>")) {
      XPathParser parser = new XPathParser(script, false, configuration.getVariables(), new XMLMapperEntityResolver());
      return createSqlSource(configuration, parser.evalNode("/script"), parameterType);
    }
    // issue #127
    script = PropertyParser.parse(script, configuration.getVariables());
    if (TokenParser.containsToken(script, "${", "}")) {
      TextSqlNode textSqlNode = new TextSqlNode(script);
      return new DynamicSqlSource(configuration, textSqlNode);
    } else {
      return new RawSqlSource(configuration, script, parameterType, paramNameResolver);
    }
  }

}

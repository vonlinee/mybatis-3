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
package org.apache.ibatis.scripting;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.type.SimpleTypeRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * @author Clinton Begin
 */
public class TextSqlNode implements SqlNode {
  private final String text;

  public TextSqlNode(String text) {
    this.text = text;
  }

  @Override
  public boolean isDynamic() {
    return GenericTokenParser.containsToken(text, "${", "}");
  }

  @Override
  public boolean apply(@NotNull SqlBuildContext context) {
    GenericTokenParser parser = createParser(new BindingTokenParser(context));
    context.appendSql(context.parseParam(parser.parse(text)));
    return true;
  }

  private GenericTokenParser createParser(TokenHandler handler) {
    return new GenericTokenParser("${", "}", handler);
  }

  private static class BindingTokenParser implements TokenHandler {

    private final SqlBuildContext context;

    public BindingTokenParser(SqlBuildContext context) {
      this.context = context;
    }

    @Override
    public String handleToken(String content) {
      Object parameter = context.getBindings().get(SqlBuildContext.PARAMETER_OBJECT_KEY);
      if (parameter == null) {
        context.getBindings().put("value", null);
      } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
        context.getBindings().put("value", parameter);
      }
      ExpressionEvaluator evaluator = context.getExpressionEvaluator();
      Object value = evaluator.getValue(content, context.getBindings());
      // issue #274 return "" instead of "null"
      return value == null ? "" : String.valueOf(value);
    }
  }
}

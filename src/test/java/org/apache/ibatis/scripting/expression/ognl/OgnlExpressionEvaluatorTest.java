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
package org.apache.ibatis.scripting.expression.ognl;

import static org.apache.ibatis.scripting.expression.ognl.OgnlExpressionEvaluator.qualifyFunctionCalls;
import static org.apache.ibatis.scripting.expression.ognl.OgnlExpressionEvaluator.toStaticCallExpression;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.ibatis.internal.util.ObjectUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.scripting.expression.ExpressionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OgnlExpressionEvaluatorTest {

  static final OgnlExpressionEvaluator evaluator = new OgnlExpressionEvaluator();

  @Test
  void shouldConvertToStaticCallExpression() {
    String expression = toStaticCallExpression(StringUtils.class, "isNotBlank");
    assertEquals("@" + StringUtils.class.getName() + "@isNotBlank", expression);

    assertEquals(expression, toStaticCallExpression(StringUtils::isNotBlank));
    assertEquals(expression + "(name)", toStaticCallExpression(StringUtils::isNotBlank, "name"));
  }

  static class User {

    int id;
    String name;
    String nickname;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getNickname() {
      return nickname;
    }

    public void setNickname(String nickname) {
      this.nickname = nickname;
    }
  }

  private Object getRootObject() {
    User user = new User();
    user.setId(1);
    user.setName("zs");
    user.setNickname("");
    Map<String, Object> root = new HashMap<>();
    root.put("user", user);
    return root;
  }

  @Test
  public void shouldEvaluateOgnlExpression() {
    final Object root = getRootObject();
    {
      String expression = toStaticCallExpression(ObjectUtils::isEmpty, "user.name");
      Assertions.assertFalse(evaluator.evaluateBoolean(expression, root));
    }

    {
      String expression = toStaticCallExpression(ObjectUtils::isEmpty, "user.nickname");
      Assertions.assertTrue(evaluator.evaluateBoolean(expression, root));
    }
  }

  @Test
  void shouldEvaluateOgnlExpressionWithWhitespace() {
    final Object root = getRootObject();
    {
      Object value = Assertions
          .assertDoesNotThrow(() -> evaluator.getValue("@java.util.Objects@toString (user.name)", root));
      Assertions.assertEquals("zs", value);
    }

    {
      Object value = Assertions
          .assertDoesNotThrow(() -> evaluator.getValue("@java.util . Objects @ toString ( user. name)", root));
      Assertions.assertEquals("zs", value);
    }
  }

  static Stream<Arguments> expressionsWithWhitespace() {
    String[] expressions = { "@java.util.Objects @toString(user.name)", "@java.util . Objects @toString(user.name)",
        "@java.util.Objects @ toString ( user.name )", "@java.util . Objects @ toString ( user. name )",
        "name == 'zs' and @java.util.Objects @toString(user.name) == 'zs'",
        "@java.util.Objects @toString(user.name) == 'zs' and @java.util . Objects @toString(user.nickname) == ''" };
    return Arrays.stream(expressions).map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("expressionsWithWhitespace")
  void shouldKeepExistingStaticCallsWithWhitespaceVariations(String expression) {
    assertEquals(expression, qualifyFunctionCalls(expression, ObjectUtils.class), expression);
  }

  @Test
  void shouldFailedEvaluateOgnlExpressionWhenIdentifierContainsWhitespace() {
    final Object root = getRootObject();
    {
      // `to String` method
      Assertions.assertThrowsExactly(ExpressionException.class,
          () -> evaluator.getValue("@java.util . Objects @ to String ( user. name)", root));
    }

    {
      // `user.n ame` property
      Assertions.assertThrowsExactly(ExpressionException.class,
          () -> evaluator.getValue("@java.util.Objects @ toString (user.n ame)", root));
    }
  }

  @Test
  void shouldFailedWhenNonStaticMethod() {
    assertThrows(ExpressionException.class, () -> toStaticCallExpression(User::getName));
    assertThrows(ExpressionException.class, () -> toStaticCallExpression(User::getNickname));
  }

  private static final String DEFAULT_CLASS = "com.example.OgnlFunctions";

  @Test
  void shouldQualifyStaticFunctionCallExpression() {
    {
      // single function call
      String input = "@isEmpty(user.name)";
      String expected = "@com.example.OgnlFunctions@isEmpty(user.name)";
      assertEquals(expected, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }

    {
      // MultipleFunctionCalls
      String input = "@isEmpty(user.name) and @isBlank(user.nickname)";
      String expected = "@com.example.OgnlFunctions@isEmpty(user.name) and "
          + "@com.example.OgnlFunctions@isBlank(user.nickname)";
      assertEquals(expected, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }

    {
      // FunctionCallsWithComplexParameters
      String input = "@isBlank(user.nickname) or @isEmpty(user.name) and @isNotEmpty(list)";
      String expected = "@com.example.OgnlFunctions@isBlank(user.nickname) or "
          + "@com.example.OgnlFunctions@isEmpty(user.name) and " + "@com.example.OgnlFunctions@isNotEmpty(list)";
      assertEquals(expected, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }

    {
      // FunctionNameWithDigitsAndUnderscore
      String input = "@is_blank_1(name)";
      String expected = "@com.example.OgnlFunctions@is_blank_1(name)";
      assertEquals(expected, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }
  }

  @Test
  void shouldKeepExistingStaticCall() {
    {
      String input = "@java.lang.Math@max(1,2)";
      assertEquals(input, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }
    {
      // shouldKeepExistingStaticCallWithPackage
      String input = "@com.example.Util@isEmpty(name)";
      assertEquals(input, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }

    {
      // shouldMixedSugarAndExistingStaticCall
      String input = "@isEmpty(name) and @java.lang.Math@max(a,b)";
      String expected = "@com.example.OgnlFunctions@isEmpty(name) and @java.lang.Math@max(a,b)";
      assertEquals(expected, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }

    {
      // shouldStringLiteralNotConverted
      String input = "'@isEmpty(test)'";
      assertEquals(input, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }

    {
      // shouldDoubleQuotedStringNotConverted
      String input = "\"@isBlank(name)\"";
      assertEquals(input, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }
    {
      // shouldStringWithEscapedQuote
      String input = "'it\\'s @test(x)'";
      assertEquals(input, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }

    {
      // shouldExpressionWithStringAndSugar
      String input = "name == '@isEmpty(x)' or @isBlank(name)";
      String expected = "name == '@isEmpty(x)' or @com.example.OgnlFunctions@isBlank(name)";
      assertEquals(expected, qualifyFunctionCalls(input, DEFAULT_CLASS));
    }
  }

  @Test
  void shouldNullInputReturnsNull() {
    assertNull(qualifyFunctionCalls(null, DEFAULT_CLASS));
  }

  @Test
  void shouldEmptyInputReturnsEmpty() {
    assertEquals("", qualifyFunctionCalls("", DEFAULT_CLASS));
  }

  @Test
  void shouldAtSignWithoutFunctionName() {
    String input = "@";
    assertEquals("@", qualifyFunctionCalls(input, DEFAULT_CLASS));
  }

  @Test
  void shouldAtSignWithIdentifierButNoParenthesis() {
    String input = "@isEmpty";
    assertEquals("@isEmpty", qualifyFunctionCalls(input, DEFAULT_CLASS));
  }

  @Test
  void shouldAtSignWithIdentifierAndSpaceBeforeParenthesis() {
    String input = "@isEmpty (name)";
    assertEquals("@isEmpty (name)", qualifyFunctionCalls(input, DEFAULT_CLASS));
  }

  // ---------- custom function map ----------

  @Test
  void shouldCustomFunctionMap() {
    Map<String, String> map = new HashMap<>();
    map.put("isEmpty", "com.example.CollectionUtils");
    map.put("isBlank", "com.example.StringUtils");

    String input = "@isEmpty(user.name) and @isBlank(user.nickname)";
    String expected = "@com.example.CollectionUtils@isEmpty(user.name) and "
        + "@com.example.StringUtils@isBlank(user.nickname)";

    assertEquals(expected, qualifyFunctionCalls(input, DEFAULT_CLASS, map));
  }

  @Test
  void shouldCustomFunctionMapWithDefaultFallback() {
    Map<String, String> map = new HashMap<>();
    map.put("isBlank", "com.example.StringUtils");

    String input = "@isEmpty(user.name) and @isBlank(user.nickname)";
    String expected = "@com.example.OgnlFunctions@isEmpty(user.name) and "
        + "@com.example.StringUtils@isBlank(user.nickname)";

    assertEquals(expected, qualifyFunctionCalls(input, DEFAULT_CLASS, map));
  }

  @Test
  void shouldMissingFunctionClassThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> qualifyFunctionCalls("@isEmpty(name)", (String) null));
  }

  @Test
  void shouldMissingFunctionClassWithMapThrowsException() {
    Map<String, String> map = new HashMap<>(); // empty mapping
    assertThrows(IllegalArgumentException.class, () -> qualifyFunctionCalls("@isEmpty(name)", null, map));
  }

  static class Param {

    private String field1;
    private String field2;

    public String getField1() {
      return field1;
    }

    public void setField1(String field1) {
      this.field1 = field1;
    }

    public String getField2() {
      return field2;
    }

    public void setField2(String field2) {
      this.field2 = field2;
    }
  }

  @Test
  void staticMethodCallInOgnlExpression() {
    Map<String, Object> context = new HashMap<>();
    Param param = new Param();
    param.setField1("xxx");
    param.setField2("  ");
    context.put("param", param);

    assertFalse(
        evaluator.evaluateBoolean(toStaticCallExpression(ObjectUtils.class, "isEmpty", "param.field1"), context));
    assertFalse(
        evaluator.evaluateBoolean(toStaticCallExpression(StringUtils.class, "isEmpty", "param.field2"), context));
    assertFalse(
        evaluator.evaluateBoolean(toStaticCallExpression(StringUtils.class, "isBlank", "param.field1"), context));
    assertTrue(
        evaluator.evaluateBoolean(toStaticCallExpression(StringUtils.class, "isNotBlank", "param.field1"), context));
    assertFalse(evaluator.evaluateBoolean(
        toStaticCallExpression(StringUtils.class, "isAllBlank", "param.field1", "param.field2"), context));
    assertTrue(evaluator.evaluateBoolean(
        toStaticCallExpression(StringUtils.class, "isAnyBlank", "param.field1", "param.field2"), context));
  }
}

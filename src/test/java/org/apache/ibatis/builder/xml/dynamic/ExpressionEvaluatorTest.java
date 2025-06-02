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
package org.apache.ibatis.builder.xml.dynamic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.junit.jupiter.api.Test;

class ExpressionEvaluatorTest {

  private final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;

  Author author = new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS);
  Author author_password_null = new Author(1, "cbegin", null, "cbegin@apache.org", "N/A", Section.NEWS);

  @Test
  void shouldCompareStringsReturnTrue() {
    boolean value = evaluator.evaluateBoolean("username == 'cbegin'", author);
    assertTrue(value);
  }

  @Test
  void shouldCompareStringsReturnFalse() {
    boolean value = evaluator.evaluateBoolean("username == 'norm'", author);
    assertFalse(value);
  }

  @Test
  void shouldReturnTrueIfNotNull() {
    boolean value = evaluator.evaluateBoolean("username", author);
    assertTrue(value);
  }

  @Test
  void shouldReturnFalseIfNull() {
    boolean value = evaluator.evaluateBoolean("password", author_password_null);
    assertFalse(value);
  }

  @Test
  void shouldReturnTrueIfNotZero() {
    boolean value = evaluator.evaluateBoolean("id", author_password_null);
    assertTrue(value);
  }

  @Test
  void shouldReturnFalseIfZero() {
    boolean value = evaluator.evaluateBoolean("id",
        new Author(0, "cbegin", null, "cbegin@apache.org", "N/A", Section.NEWS));
    assertFalse(value);
  }

  @Test
  void shouldReturnFalseIfZeroWithScale() {
    class Bean {
      @SuppressWarnings("unused")
      public final double d = 0.0D;
    }
    assertFalse(evaluator.evaluateBoolean("d", new Bean()));
  }

  @Test
  void shouldIterateOverIterable() {
    final HashMap<String, String[]> parameterObject = new HashMap<>(1);
    parameterObject.put("array", new String[] { "1", "2", "3" });
    final Iterable<?> iterable = evaluator.evaluateIterable("array", parameterObject);
    int i = 0;
    for (Object o : iterable) {
      i++;
      assertEquals(String.valueOf(i), o);
    }
  }

}

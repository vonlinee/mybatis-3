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
package org.apache.ibatis.extension;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SqlUtilsTest {

  // A simple mock formatter for testing purposes
  private final SqlValueFormatter mockFormatter = value -> {
    if (value == null)
      return "NULL";
    if (value instanceof Number)
      return value.toString();
    // Mimic string escaping for test visibility
    return "'" + value.toString().replace("'", "''") + "'";
  };

  @Test
  void testSimpleReplacement() {
    String sql = "SELECT * FROM user WHERE id = ? AND status = ?";
    Object[] args = { 10, "ACTIVE" };

    String result = SqlUtils.inlineParams(sql, args, mockFormatter);

    assertEquals("SELECT * FROM user WHERE id = 10 AND status = 'ACTIVE'", result);
  }

  @Test
  void testQuestionMarkInsideSingleQuotesIsIgnored() {
    String sql = "SELECT * FROM questions WHERE title = 'What is Java?' AND author_id = ?";
    Object[] args = { 42 };

    String result = SqlUtils.inlineParams(sql, args, mockFormatter);

    assertEquals("SELECT * FROM questions WHERE title = 'What is Java?' AND author_id = 42", result);
  }

  @Test
  void testQuestionMarkInsideDoubleQuotesIsIgnored() {
    String sql = "SELECT \"column?name\" FROM user WHERE id = ?";
    Object[] args = { 1 };

    String result = SqlUtils.inlineParams(sql, args, mockFormatter);

    assertEquals("SELECT \"column?name\" FROM user WHERE id = 1", result);
  }

  @Test
  void testQuestionMarkInsideBackticksIsIgnored() {
    String sql = "SELECT `is_active?` FROM user WHERE id = ?";
    Object[] args = { 5 };

    String result = SqlUtils.inlineParams(sql, args, mockFormatter);

    assertEquals("SELECT `is_active?` FROM user WHERE id = 5", result);
  }

  @Test
  void testConcatWithPlaceholders() {
    String sql = "SELECT * FROM user WHERE name LIKE CONCAT('%', ?, '%')";
    Object[] args = { "John" };

    String result = SqlUtils.inlineParams(sql, args, mockFormatter);

    assertEquals("SELECT * FROM user WHERE name LIKE CONCAT('%', 'John', '%')", result);
  }

  @Test
  void testNullValueHandling() {
    String sql = "UPDATE user SET deleted_at = ? WHERE id = ?";
    Object[] args = { null, 99 };

    String result = SqlUtils.inlineParams(sql, args, mockFormatter);

    assertEquals("UPDATE user SET deleted_at = NULL WHERE id = 99", result);
  }

  @Test
  void testNotEnoughParametersThrowsException() {
    String sql = "SELECT * FROM user WHERE id = ? AND name = ?";
    Object[] args = { 1 }; // Missing second parameter

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      SqlUtils.inlineParams(sql, args, mockFormatter);
    });

    assertTrue(exception.getMessage().contains("Not enough parameters"));
  }

  @Test
  void testTooManyParametersThrowsException() {
    String sql = "SELECT * FROM user WHERE id = ?";
    Object[] args = { 1, "Extra" }; // Too many parameters

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      SqlUtils.inlineParams(sql, args, mockFormatter);
    });

    assertTrue(exception.getMessage().contains("Too many parameters"));
  }

  @Test
  void testEscapedSingleQuotesInsideStringLiteral() {
    // SQL: SELECT * FROM post WHERE content = 'It''s a sunny day?' AND id = ?
    String sql = "SELECT * FROM post WHERE content = 'It''s a sunny day?' AND id = ?";
    Object[] args = { 100 };

    String result = SqlUtils.inlineParams(sql, args, mockFormatter);

    assertEquals("SELECT * FROM post WHERE content = 'It''s a sunny day?' AND id = 100", result);
  }
}

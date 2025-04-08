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
package org.apache.ibatis.parsing;

import org.jetbrains.annotations.NotNull;

/**
 * support ${xxx} or #{xxx}
 *
 * @author Clinton Begin
 */
public class TokenParser {

  /**
   * ${}: String Interpolation or Variable Substitution Used for directly inserting variable values. Suitable for simple
   * string replacements that do not involve complex expressions.
   */
  public static final String OPEN_TOKEN_$ = "${";

  /**
   * #{}: Parameterized Variable or Prepared Statement Placeholder
   * <p>
   * Used to pass variables as parameters, typically with preprocessing (such as preventing SQL injection). Suitable for
   * use in SQL queries, especially when type handling of variables is required.
   * </p>
   */
  public static final String OPEN_TOKEN_PARAMETER_PLACEHOLDER = "#{";

  public static final String CLOSE_TOKEN = "}";

  public static String parse(String text, @NotNull String openToken, @NotNull String closeToken,
      @NotNull TokenHandler handler) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    int start = text.indexOf(openToken);
    if (start == -1) {
      return text;
    }
    char[] src = text.toCharArray();
    int offset = 0;
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null;
    do {
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        builder.append(src, offset, start - offset - 1).append(openToken);
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        builder.append(src, offset, start - offset);
        offset = start + openToken.length();
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          if ((end <= offset) || (src[end - 1] != '\\')) {
            expression.append(src, offset, end - offset);
            break;
          }
          // this close token is escaped. remove the backslash and continue.
          expression.append(src, offset, end - offset - 1).append(closeToken);
          offset = end + closeToken.length();
          end = text.indexOf(closeToken, offset);
        }
        if (end == -1) {
          // close token was not found.
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      start = text.indexOf(openToken, offset);
    } while (start > -1);
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }

  public static boolean containsToken(String text, @NotNull String openToken, @NotNull String closeToken) {
    if (text == null || text.isEmpty()) {
      return false;
    }
    // search open token
    int start = text.indexOf(openToken);
    if (start == -1) {
      return false;
    }
    char[] src = text.toCharArray();
    int offset;
    StringBuilder expression = null;
    do {
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        offset = start + openToken.length();
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          if ((end <= offset) || (src[end - 1] != '\\')) {
            expression.append(src, offset, end - offset);
            break;
          }
          // this close token is escaped. remove the backslash and continue.
          expression.append(src, offset, end - offset - 1).append(closeToken);
          offset = end + closeToken.length();
          end = text.indexOf(closeToken, offset);
        }
        if (end == -1) {
          // close token was not found.
          offset = src.length;
        } else {
          return true;
        }
      }
      start = text.indexOf(openToken, offset);
    } while (start > -1);
    return false;
  }
}

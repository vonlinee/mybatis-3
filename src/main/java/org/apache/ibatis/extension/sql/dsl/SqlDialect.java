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
package org.apache.ibatis.extension.sql.dsl;

import java.util.Objects;

/**
 * Rendering policy for identifiers and pagination syntax.
 */
public interface SqlDialect {

  SqlDialect STANDARD = identity();
  SqlDialect POSTGRESQL = quoted('"', PaginationStyle.LIMIT_OFFSET);
  SqlDialect MYSQL = quoted('`', PaginationStyle.LIMIT_OFFSET);
  SqlDialect SQL_SERVER = quoted('[', PaginationStyle.TOP);
  SqlDialect ORACLE = quoted('"', PaginationStyle.OFFSET_FETCH);

  String quoteIdentifier(String identifier);

  default PaginationStyle paginationStyle() {
    return PaginationStyle.LIMIT_OFFSET;
  }

  static SqlDialect identity() {
    return new BasicDialect(null, PaginationStyle.LIMIT_OFFSET);
  }

  static SqlDialect quoted(char quote) {
    return quoted(quote, PaginationStyle.LIMIT_OFFSET);
  }

  static SqlDialect quoted(char quote, PaginationStyle paginationStyle) {
    Objects.requireNonNull(paginationStyle, "paginationStyle");
    if (quote == '[') {
      return new BasicDialect(identifier -> quoteSegments(identifier, '[', ']'), paginationStyle);
    }
    return new BasicDialect(identifier -> quoteSegments(identifier, quote, quote), paginationStyle);
  }

  static SqlDialect custom(IdentifierQuoter quoter, PaginationStyle paginationStyle) {
    return new BasicDialect(Objects.requireNonNull(quoter, "quoter"),
        Objects.requireNonNull(paginationStyle, "paginationStyle"));
  }

  enum PaginationStyle {
    LIMIT_OFFSET, OFFSET_FETCH, TOP
  }

  interface IdentifierQuoter {
    String quote(String identifier);
  }

  final class BasicDialect implements SqlDialect {
    private final IdentifierQuoter quoter;
    private final PaginationStyle paginationStyle;

    BasicDialect(IdentifierQuoter quoter, PaginationStyle paginationStyle) {
      this.quoter = quoter;
      this.paginationStyle = Objects.requireNonNull(paginationStyle, "paginationStyle");
    }

    @Override
    public String quoteIdentifier(String identifier) {
      Objects.requireNonNull(identifier, "identifier");
      return quoter == null ? identifier : quoter.quote(identifier);
    }

    @Override
    public PaginationStyle paginationStyle() {
      return paginationStyle;
    }
  }

  static String quoteSegments(String identifier, char left, char right) {
    Objects.requireNonNull(identifier, "identifier");
    String[] segments = identifier.split("\\.", -1);
    StringBuilder result = new StringBuilder(identifier.length() + segments.length * 2);
    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        result.append('.');
      }
      result.append(left);
      result.append(segments[i].replace(String.valueOf(right), String.valueOf(right) + right));
      result.append(right);
    }
    return result.toString();
  }
}

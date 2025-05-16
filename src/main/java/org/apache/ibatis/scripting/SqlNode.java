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
package org.apache.ibatis.scripting;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.scripting.xmltags.DynamicCheckerTokenParser;
import org.jetbrains.annotations.NotNull;

/**
 * @author Clinton Begin
 */
public interface SqlNode {

  default void setExpressionEvaluator(ExpressionEvaluator evaluator) {
  }

  /**
   * 'dynamic' means:
   * <li>1. a xml tag element, like {@code <if/>, <where/>, <choose/>}</li>
   * <li>2. a text node contains parameter like ${xxx}</li>
   *
   * @return whether this sql node is dynamic
   *
   * @see DynamicCheckerTokenParser#isDynamic(String)
   */
  boolean isDynamic();

  /**
   * apply the sql build process.
   *
   * @param context
   *          context
   */
  boolean apply(SqlBuildContext context);

  default List<SqlNode> getChildren() {
    return Collections.emptyList();
  }

  default boolean hasChildren() {
    return getChildCount() != 0;
  }

  default int getChildCount() {
    return 0;
  }

  default SqlNode getChild(int index) {
    return null;
  }

  @NotNull
  default SqlNode getRoot() {
    return this;
  }

  SqlNode EMPTY = new SqlNode() {
    @Override
    public boolean isDynamic() {
      return false;
    }

    @Override
    public boolean apply(SqlBuildContext context) {
      return false;
    }
  };
}

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
package org.apache.ibatis.scripting.xmltags;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;

/**
 * @author Clinton Begin
 */
public class ForEachSqlNode implements SqlNode {

  private final String collectionExpression;
  private final Boolean nullable;
  private final SqlNode contents;
  private final String open;
  private final String close;
  private final String separator;
  private final String item;
  private final String index;

  /**
   * @since 3.5.9
   */
  public ForEachSqlNode(SqlNode contents, String collectionExpression, Boolean nullable, String index, String item,
      String open, String close, String separator) {
    this.collectionExpression = collectionExpression;
    this.nullable = nullable;
    this.contents = contents;
    this.open = open;
    this.close = close;
    this.separator = separator;
    this.index = index;
    this.item = item;
  }

  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public boolean apply(DynamicContext context) {
    final Map<String, Object> bindings = context.getBindings();
    final boolean nullableOnForEach = this.nullable == null ? context.getConfiguration().isNullableOnForEach()
        : this.nullable;
    final ExpressionEvaluator evaluator = context.getExpressionEvaluator();
    final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings, nullableOnForEach);
    if (iterable == null || !iterable.iterator().hasNext()) {
      return true;
    }
    boolean first = true;
    applyOpen(context);
    int i = 0;
    for (Object o : iterable) {
      PrefixedContext scopedContext;
      if (first || separator == null) {
        scopedContext = new PrefixedContext(context, "");
      } else {
        scopedContext = new PrefixedContext(context, separator);
      }
      // Issue #709
      if (o instanceof Map.Entry) {
        @SuppressWarnings("unchecked")
        Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
        applyIndex(scopedContext, mapEntry.getKey());
        applyItem(scopedContext, mapEntry.getValue());
      } else {
        applyIndex(scopedContext, i);
        applyItem(scopedContext, o);
      }
      contents.apply(scopedContext);
      if (first) {
        first = !scopedContext.isPrefixApplied();
      }
      i++;
    }
    applyClose(context);
    return true;
  }

  private void applyIndex(DynamicContext context, Object o) {
    if (index != null) {
      context.bind(index, o);
    }
  }

  private void applyItem(DynamicContext context, Object o) {
    if (item != null) {
      context.bind(item, o);
    }
  }

  private void applyOpen(DynamicContext context) {
    if (open != null) {
      context.appendSql(open);
    }
  }

  private void applyClose(DynamicContext context) {
    if (close != null) {
      context.appendSql(close);
    }
  }

  private static class PrefixedContext extends DynamicContext {
    private final DynamicContext delegate;
    private final String prefix;
    private boolean prefixApplied;

    public PrefixedContext(DynamicContext delegate, String prefix) {
      super(delegate);
      this.delegate = delegate;
      this.prefix = prefix;
      this.prefixApplied = false;
      this.bindings.putAll(delegate.getBindings());
    }

    public boolean isPrefixApplied() {
      return prefixApplied;
    }

    @Override
    public void appendSql(String sql) {
      if (!prefixApplied && sql != null && !sql.trim().isEmpty()) {
        delegate.appendSql(prefix);
        prefixApplied = true;
      }
      delegate.appendSql(sql);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    @Override
    public List<ParameterMapping> getParameterMappings() {
      return delegate.getParameterMappings();
    }
  }

}

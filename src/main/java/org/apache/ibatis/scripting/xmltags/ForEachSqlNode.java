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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
public class ForEachSqlNode implements SqlNode {

  private final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;
  private final String collectionExpression;
  private final Boolean nullable;
  private final SqlNode contents;
  private final String open;
  private final String close;
  private final String separator;
  private final String item;
  private final String index;
  private final Configuration configuration;

  /**
   * @deprecated Since 3.5.9, use the
   *             {@link #ForEachSqlNode(Configuration, SqlNode, String, Boolean, String, String, String, String, String)}.
   */
  @Deprecated
  public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index,
      String item, String open, String close, String separator) {
    this(configuration, contents, collectionExpression, null, index, item, open, close, separator);
  }

  /**
   * @since 3.5.9
   */
  public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, Boolean nullable,
      String index, String item, String open, String close, String separator) {
    this.collectionExpression = collectionExpression;
    this.nullable = nullable;
    this.contents = contents;
    this.open = open;
    this.close = close;
    this.separator = separator;
    this.index = index;
    this.item = item;
    this.configuration = configuration;
  }

  public Iterable<?> getIterable(DynamicContext context) {
    boolean nullable = this.nullable == null ? configuration.isNullableOnForEach() : this.nullable;
    return evaluator.evaluateIterable(collectionExpression, context.getBindings(), nullable);
  }

  @Override
  public boolean apply(DynamicContext context) {
    final Iterable<?> iterable = getIterable(context);
    if (iterable == null) {
      return true;
    }
    final Iterator<?> iterator = iterable.iterator();
    boolean hasNext = iterator.hasNext();
    if (!hasNext) {
      return true;
    }
    boolean first = true;
    applyOpen(context);
    int i = 0;

    final PrefixedContext scopedContext = new PrefixedContext(context, "");
    while (hasNext) {
      Object o = iterator.next();
      scopedContext.resetPrefix(first || separator == null ? "" : separator);

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

      hasNext = iterator.hasNext();
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

  private class PrefixedContext extends DynamicContext {
    private final DynamicContext delegate;
    private String prefix;
    private boolean prefixApplied;

    public PrefixedContext(DynamicContext delegate, String prefix) {
      super(configuration, delegate.getParameterObject(), delegate.getParameterType(), delegate.getParamNameResolver(),
          delegate.isParamExists());
      this.delegate = delegate;
      this.bindings.putAll(delegate.getBindings());
      resetPrefix(prefix);
    }

    public boolean isPrefixApplied() {
      return prefixApplied;
    }

    public void resetPrefix(String prefix) {
      this.prefix = prefix;
      this.prefixApplied = false;
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

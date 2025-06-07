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

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.scripting.SqlNode;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 */
public class ForEachSqlNode extends XmlSqlNode {

  private final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;

  /**
   * property expression that evaluate to a value of collection type
   */
  @NotNull
  private final String collectionExpression;
  @Nullable
  private final Boolean nullable;
  @NotNull
  private final SqlNode contents;
  @Nullable
  private final String open;
  @Nullable
  private final String close;
  @Nullable
  private final String separator;
  @Nullable
  private final String item;
  @Nullable
  private final String index;

  public ForEachSqlNode(SqlNode contents, String collectionExpression, String index, String item, String open,
      String close, String separator) {
    this(contents, collectionExpression, null, index, item, open, close, separator);
  }

  /**
   * @since 3.5.9
   */
  public ForEachSqlNode(@NotNull SqlNode contents, @NotNull String collectionExpression, @Nullable Boolean nullable,
      @Nullable String index, @Nullable String item, @Nullable String open, @Nullable String close,
      @Nullable String separator) {
    this.collectionExpression = collectionExpression;
    this.nullable = nullable;
    this.contents = contents;
    this.open = open;
    this.close = close;
    this.separator = separator;
    this.index = index;
    this.item = item;
  }

  public Iterable<?> getIterable(SqlBuildContext context) {
    Configuration configuration = context.getConfiguration();
    boolean nullable = this.nullable == null ? configuration.isNullableOnForEach() : this.nullable;
    return evaluator.evaluateIterable(collectionExpression, context.getBindings(), nullable);
  }

  @Override
  public boolean apply(SqlBuildContext context) {
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

  protected void applyIndex(SqlBuildContext context, Object o) {
    if (index != null) {
      context.bind(index, o);
    }
  }

  protected void applyItem(SqlBuildContext context, Object o) {
    if (item != null) {
      context.bind(item, o);
    }
  }

  protected void applyOpen(SqlBuildContext context) {
    if (open != null) {
      context.appendSql(open);
    }
  }

  protected void applyClose(SqlBuildContext context) {
    if (close != null) {
      context.appendSql(close);
    }
  }

  private static class PrefixedContext extends DynamicContext {
    private final SqlBuildContext delegate;
    private String prefix;
    private boolean prefixApplied;

    public PrefixedContext(SqlBuildContext delegate, String prefix) {
      super(delegate);
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

  public @Nullable String getClose() {
    return close;
  }

  public @NotNull String getCollectionExpression() {
    return collectionExpression;
  }

  public @NotNull SqlNode getContents() {
    return contents;
  }

  public @Nullable String getIndex() {
    return index;
  }

  public @Nullable String getItem() {
    return item;
  }

  public @Nullable Boolean getNullable() {
    return nullable;
  }

  public @Nullable String getOpen() {
    return open;
  }

  public @Nullable String getSeparator() {
    return separator;
  }
}

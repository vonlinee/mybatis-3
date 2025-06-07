package org.apache.ibatis.scripting;

import java.util.List;

import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlBuildContextDelegation implements SqlBuildContext {

  @NotNull
  private final SqlBuildContext delegate;

  public SqlBuildContextDelegation(@NotNull SqlBuildContext delegate) {
    this.delegate = delegate;
  }

  @NotNull
  public SqlBuildContext getDelegate() {
    return delegate;
  }

  @Override
  public @NotNull Configuration getConfiguration() {
    return delegate.getConfiguration();
  }

  @Override
  public @NotNull ContextMap createBindings(@Nullable Object parameterObject) {
    return delegate.createBindings(parameterObject);
  }

  @Override
  public @NotNull ContextMap getBindings() {
    return delegate.getBindings();
  }

  @Override
  public void bind(@NotNull String name, Object value) {
    delegate.bind(name, value);
  }

  @Override
  public void appendSql(String sql) {
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

  @Override
  public String parseParam(String sql) {
    return delegate.parseParam(sql);
  }

  @Override
  public Object getParameterObject() {
    return delegate.getParameterObject();
  }

  @Override
  public Class<?> getParameterType() {
    return delegate.getParameterType();
  }

  @Override
  public ParamNameResolver getParamNameResolver() {
    return delegate.getParamNameResolver();
  }

  @Override
  public boolean isParamExists() {
    return delegate.isParamExists();
  }
}

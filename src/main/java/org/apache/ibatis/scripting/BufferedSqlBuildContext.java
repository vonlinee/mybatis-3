package org.apache.ibatis.scripting;

import org.jetbrains.annotations.NotNull;

public class BufferedSqlBuildContext extends SqlBuildContextDelegation {

  @NotNull
  private final StringBuilder sqlBuffer;

  public BufferedSqlBuildContext(@NotNull SqlBuildContext delegate) {
    super(delegate);
    sqlBuffer = new StringBuilder();
  }

  @NotNull
  public StringBuilder getSqlBuffer() {
    return sqlBuffer;
  }

  @Override
  public void appendSql(String sql) {
    sqlBuffer.append(sql);
  }

  @Override
  public String getSql() {
    return sqlBuffer.toString();
  }
}

package org.apache.ibatis.executor;

public final class RawSql implements Sql {

  private final String sql;

  public RawSql(String sql) {
    this.sql = sql;
  }

  @Override
  public String toString() {
    return sql;
  }
}

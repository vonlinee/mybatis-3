package org.apache.ibatis.session;

public class OffsetLimitRowBounds implements RowBounds {

  private final int offset;
  private final int limit;

  public OffsetLimitRowBounds() {
    this.offset = NO_ROW_OFFSET;
    this.limit = NO_ROW_LIMIT;
  }

  public OffsetLimitRowBounds(int offset, int limit) {
    this.offset = offset;
    this.limit = limit;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public int getLimit() {
    return limit;
  }
}

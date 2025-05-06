package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.scripting.SqlNode;

abstract class XmlSqlNode implements SqlNode {
  @Override
  public final boolean isDynamic() {
    return true;
  }
}

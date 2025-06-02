package org.apache.ibatis.builder.xml;

import org.apache.ibatis.parsing.XNode;

public class XMLStatementResolver {

  XMLStatementBuilder builder;
  XNode context;

  public XMLStatementResolver(XMLStatementBuilder builder, XNode context) {
    this.builder = builder;
    this.context = context;
  }

  public boolean resolve() {
    builder.parseStatementNode(context);
    return true;
  }
}

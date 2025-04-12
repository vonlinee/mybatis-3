package org.apache.ibatis.builder.xml;

import org.apache.ibatis.parsing.XNode;

public class PendingXMLStatementResolver {

  protected final XMLStatementBuilder builder;
  protected final XNode statementNode;

  public PendingXMLStatementResolver(XMLStatementBuilder builder, XNode context) {
    this.builder = builder;
    this.statementNode = context;
  }

  public boolean resolve() {
    builder.parseStatementNode(statementNode);
    return true;
  }
}

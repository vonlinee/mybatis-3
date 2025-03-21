package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;

public class DynamicCheckerTokenParser implements TokenHandler {

  private boolean isDynamic;

  public DynamicCheckerTokenParser() {
    // Prevent Synthetic Access
  }

  boolean isDynamic() {
    return isDynamic;
  }

  @Override
  public String handleToken(String content) {
    this.isDynamic = true;
    return null;
  }

  public static boolean isDynamic(String text) {
    DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
    GenericTokenParser parser = new GenericTokenParser("${", "}", checker);
    parser.parse(text);
    return checker.isDynamic();
  }
}

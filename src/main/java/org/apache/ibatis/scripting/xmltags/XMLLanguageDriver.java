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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.ParameterMappingTokenHandler;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.MixedSqlNode;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.scripting.SqlNode;
import org.apache.ibatis.scripting.SqlNodeWrapper;
import org.apache.ibatis.scripting.StaticTextSqlNode;
import org.apache.ibatis.scripting.TextSqlNode;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Eduardo Macarron
 */
public class XMLLanguageDriver implements LanguageDriver {

  private Configuration configuration;
  private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
  private static final Map<String, SqlNode> emptyNodeCache = new ConcurrentHashMap<>();
  private final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;

  public XMLLanguageDriver() {
    this.nodeHandlerMap.putAll(initNodeHandlerMap());
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
    return createSqlSource(configuration, script, parameterType, null);
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    SqlNode rootSqlNode = this.parseRootSqlNode(script);
    rootSqlNode.setExpressionEvaluator(this.evaluator);
    return createSqlSource(rootSqlNode, configuration, parameterType, paramNameResolver);
  }

  @Override
  public SqlSource createSqlSource(SqlNode rootSqlNode, Configuration configuration, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    rootSqlNode.setExpressionEvaluator(this.evaluator);
    SqlSource sqlSource;
    if (rootSqlNode.isDynamic()) {
      sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
    } else {
      sqlSource = createForRawSqlSource(configuration, rootSqlNode, parameterType, paramNameResolver);
    }
    return sqlSource;
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
    return createSqlSource(configuration, script, parameterType, null);
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    // issue #3
    if (script.startsWith("<script>")) {
      XPathParser parser = new XPathParser(script, false, configuration.getVariables(), new XMLMapperEntityResolver());
      return createSqlSource(configuration, parser.evalNode("/script"), parameterType);
    }
    // issue #127
    script = PropertyParser.parse(script, configuration.getVariables());
    if (DynamicCheckerTokenParser.isDynamic(script)) {
      TextSqlNode textSqlNode = new TextSqlNode(script);
      textSqlNode.setExpressionEvaluator(this.evaluator);
      return new DynamicSqlSource(configuration, textSqlNode);
    } else {
      return createForRawSqlSource(configuration, script, parameterType, paramNameResolver);
    }
  }

  public SqlSource createForRawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    DynamicContext context = new DynamicContext(configuration, parameterType, paramNameResolver);
    rootSqlNode.apply(context);
    String sql = context.getSql();
    return SqlSourceBuilder.buildSqlSource(configuration, sql, context.getParameterMappings());
  }

  public SqlSource createForRawSqlSource(Configuration configuration, String sql, Class<?> parameterType,
      ParamNameResolver paramNameResolver) {
    Class<?> clazz = parameterType == null ? Object.class : parameterType;
    List<ParameterMapping> parameterMappings = new ArrayList<>();
    ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler(parameterMappings, configuration,
        clazz, new HashMap<>(), paramNameResolver);
    GenericTokenParser parser = new GenericTokenParser("#{", "}", tokenHandler);
    return SqlSourceBuilder.buildSqlSource(configuration, parser.parse(sql), parameterMappings);
  }

  private Map<String, NodeHandler> initNodeHandlerMap() {
    HashMap<String, NodeHandler> nodeHandlerMap = new HashMap<>();
    nodeHandlerMap.put("trim", new TrimHandler());
    nodeHandlerMap.put("where", new WhereHandler());
    nodeHandlerMap.put("set", new SetHandler());
    nodeHandlerMap.put("foreach", new ForEachHandler());
    nodeHandlerMap.put("if", new IfHandler());
    nodeHandlerMap.put("choose", new ChooseHandler());
    ConditionSqlNodeHandler handler = new ConditionSqlNodeHandler();
    nodeHandlerMap.put("and", handler);
    nodeHandlerMap.put("or", handler);
    nodeHandlerMap.put("when", new IfHandler());
    nodeHandlerMap.put("otherwise", new OtherwiseHandler());
    nodeHandlerMap.put("bind", new BindHandler());
    nodeHandlerMap.put("in", new InHandler());
    return nodeHandlerMap;
  }

  public SqlNode minimum(SqlNode sqlNode) {
    if (sqlNode instanceof SqlNodeWrapper) {
      return sqlNode.getRoot();
    } else if (sqlNode.getChildCount() == 1) {
      return sqlNode.getChild(0);
    }
    return sqlNode;
  }

  public SqlNode parseRootSqlNode(XNode node) {
    return parseDynamicTags(node);
  }

  private SqlNode parseDynamicTags(XNode node) {
    List<SqlNode> contents = new ArrayList<>();
    NodeList children = node.getNode().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      XNode child = node.newXNode(children.item(i));
      if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
        String data = child.getStringBody("");
        if (data.trim().isEmpty()) {
          contents.add(emptyNodeCache.computeIfAbsent(data, EmptySqlNode::new));
          continue;
        }
        if (DynamicCheckerTokenParser.isDynamic(data)) {
          TextSqlNode textSqlNode = new TextSqlNode(data);
          contents.add(textSqlNode);
        } else {
          contents.add(new StaticTextSqlNode(data));
        }
      } else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) { // issue #628
        String nodeName = child.getNode().getNodeName();
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        if (handler == null) {
          throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
        }
        handler.handleNode(child, contents);
      }
    }
    return new MixedSqlNode(contents);
  }

  @Override
  public final void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public interface NodeHandler {

    void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
  }

  private static class BindHandler implements NodeHandler {
    public BindHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      final String name = nodeToHandle.getStringAttribute("name");
      final String expression = nodeToHandle.getStringAttribute("value");
      final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
      targetContents.add(node);
    }
  }

  private class TrimHandler implements NodeHandler {
    public TrimHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      String prefix = nodeToHandle.getStringAttribute("prefix");
      String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
      String suffix = nodeToHandle.getStringAttribute("suffix");
      String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");

      SqlNode sqlNode = parseDynamicTags(nodeToHandle);

      sqlNode = minimum(sqlNode);
      TrimSqlNode trim = new TrimSqlNode(sqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
      targetContents.add(trim);
    }
  }

  private class WhereHandler implements NodeHandler {
    public WhereHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      SqlNode sqlNode = parseDynamicTags(nodeToHandle);

      sqlNode = minimum(sqlNode);

      WhereSqlNode where = new WhereSqlNode(sqlNode);
      targetContents.add(where);
    }
  }

  private class SetHandler implements NodeHandler {
    public SetHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      SqlNode sqlNode = parseDynamicTags(nodeToHandle);
      sqlNode = minimum(sqlNode);
      SetSqlNode set = new SetSqlNode(sqlNode);
      targetContents.add(set);
    }
  }

  private class ForEachHandler implements NodeHandler {
    public ForEachHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      String collection = nodeToHandle.getStringAttribute("collection");
      Boolean nullable = nodeToHandle.getBooleanAttribute("nullable");
      String item = nodeToHandle.getStringAttribute("item");
      String index = nodeToHandle.getStringAttribute("index");
      String open = nodeToHandle.getStringAttribute("open");
      String close = nodeToHandle.getStringAttribute("close");
      String separator = nodeToHandle.getStringAttribute("separator");

      SqlNode sqlNode = parseDynamicTags(nodeToHandle);
      sqlNode = minimum(sqlNode);
      ForEachSqlNode forEachSqlNode = new ForEachSqlNode(sqlNode, collection, nullable, index, item, open, close,
          separator);
      targetContents.add(forEachSqlNode);
    }
  }

  private class IfHandler implements NodeHandler {
    public IfHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      String test = nodeToHandle.getStringAttribute("test");

      SqlNode sqlNode = parseDynamicTags(nodeToHandle);
      sqlNode = minimum(sqlNode);
      IfSqlNode ifSqlNode = new IfSqlNode(sqlNode, test);
      targetContents.add(ifSqlNode);
    }
  }

  private class OtherwiseHandler implements NodeHandler {
    public OtherwiseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      SqlNode sqlNode = parseDynamicTags(nodeToHandle);
      sqlNode = minimum(sqlNode);
      targetContents.add(sqlNode);
    }
  }

  private class ChooseHandler implements NodeHandler {
    public ChooseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      List<SqlNode> whenSqlNodes = new ArrayList<>();
      List<SqlNode> otherwiseSqlNodes = new ArrayList<>();
      handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
      SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
      ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
      targetContents.add(chooseSqlNode);
    }

    private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes,
        List<SqlNode> defaultSqlNodes) {
      List<XNode> children = chooseSqlNode.getChildren();
      for (XNode child : children) {
        String nodeName = child.getNode().getNodeName();
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        if (handler instanceof IfHandler) {
          handler.handleNode(child, ifSqlNodes);
        } else if (handler instanceof OtherwiseHandler) {
          handler.handleNode(child, defaultSqlNodes);
        }
      }
    }

    private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
      SqlNode defaultSqlNode = null;
      if (defaultSqlNodes.size() == 1) {
        defaultSqlNode = defaultSqlNodes.get(0);
      } else if (defaultSqlNodes.size() > 1) {
        throw new BuilderException("Too many default (otherwise) elements in choose statement.");
      }
      return defaultSqlNode;
    }
  }

  private static class EmptySqlNode implements SqlNode {
    private final String whitespaces;

    public EmptySqlNode(String whitespaces) {
      super();
      this.whitespaces = whitespaces;
    }

    @Override
    public boolean isDynamic() {
      return false;
    }

    @Override
    public boolean apply(SqlBuildContext context) {
      context.appendSql(whitespaces);
      return true;
    }
  }

  /**
   * handle <and></and>, <or></or> in xml mapping file
   *
   * @see AndSqlNode
   * @see OrSqlNode
   * @see ConditionSqlNode
   */
  private class ConditionSqlNodeHandler implements NodeHandler {

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      final String nodeName = nodeToHandle.getName();
      final SqlNode sqlNode = parseDynamicTags(nodeToHandle);

      handleNode(nodeName, sqlNode, nodeToHandle, targetContents);
    }

    private void handleNode(String nodeName, SqlNode sqlNode, XNode nodeToHandle, List<SqlNode> targetContents) {
      if (sqlNode instanceof MixedSqlNode) {
        final List<SqlNode> contents = sqlNode.getChildren();

        final String test = nodeToHandle.getStringAttribute("test");

        if (contents.size() == 1) {
          if (nodeName.equalsIgnoreCase("and")) {
            targetContents.add(new AndSqlNode(test, contents));
          } else if (nodeName.equalsIgnoreCase("or")) {
            targetContents.add(new OrSqlNode(test, contents));
          }
        } else if (contents.isEmpty()) {
          throw new BuilderException("syntax error about <" + nodeName + ">, empty <" + nodeName + "> is meaningless.");
        } else {
          if (nodeName.equalsIgnoreCase("and")) {
            targetContents.add(new AndSqlNode(test, contents));
          } else if (nodeName.equalsIgnoreCase("or")) {
            targetContents.add(new OrSqlNode(test, contents));
          }
        }
      } else if (sqlNode instanceof SqlNodeWrapper) {
        handleNode(nodeName, sqlNode.getRoot(), nodeToHandle, targetContents);
      } else {
        targetContents.add(sqlNode);
      }
    }
  }

  /**
   * @see ForEachHandler
   */
  private class InHandler implements NodeHandler {

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      String collection = nodeToHandle.getStringAttribute("collection");
      String item = nodeToHandle.getStringAttribute("item");
      String test = nodeToHandle.getStringAttribute("test");
      Boolean nullable = nodeToHandle.getBooleanAttribute("nullable");

      if (StringUtils.isEmpty(item)) {
        item = "item";
      }

      StaticTextSqlNode contents = new StaticTextSqlNode("#{" + item + "}");
      item = parseItemExpression(item);
      targetContents.add(new InSqlNode(contents, collection, test, item, nullable));
    }

    /**
     * item.id -> item
     *
     * @param itemExpression
     *          item expression
     *
     * @return item expression value
     */
    private String parseItemExpression(String itemExpression) {
      if (itemExpression == null) {
        return null;
      }
      PropertyTokenizer tokenizer = new PropertyTokenizer(itemExpression);
      if (tokenizer.hasNext()) {
        return tokenizer.getName();
      }
      return itemExpression;
    }
  }
}

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
import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.parsing.TokenParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.reflection.PropertyTokenizer;
import org.apache.ibatis.scripting.MethodParamMetadata;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.scripting.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Clinton Begin
 */
public class XMLScriptBuilder {

  private final XNode context;
  private final Class<?> parameterType;
  private final MethodParamMetadata paramNameResolver;
  private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
  private static final Map<String, SqlNode> emptyNodeCache = new ConcurrentHashMap<>();
  private final Configuration configuration;

  public XMLScriptBuilder(Configuration configuration, XNode context) {
    this(configuration, context, null);
  }

  public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
    this(configuration, context, parameterType, null);
  }

  public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType,
      MethodParamMetadata paramNameResolver) {
    this.configuration = configuration;
    this.context = context;
    this.parameterType = parameterType;
    this.paramNameResolver = paramNameResolver;
    this.nodeHandlerMap.putAll(initNodeHandlerMap());
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

  public SqlSource parseScriptNode() {
    MixedSqlNode rootSqlNode = parseDynamicTags(context);
    SqlSource sqlSource;
    if (rootSqlNode.isDynamic()) {
      sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
    } else {
      sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType, paramNameResolver);
    }
    return sqlSource;
  }

  protected MixedSqlNode parseDynamicTags(XNode node) {
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
        if (TokenParser.containsToken(data, "${", "}")) {
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
        SqlNode sqlNode = handler.handleNode(child, contents);
        handler.init(sqlNode);
      }
    }
    return new MixedSqlNode(contents);
  }

  public interface NodeHandler {

    default void init(SqlNode sqlNode) {
      if (sqlNode instanceof EvaluableSqlNode) {
        ((EvaluableSqlNode) sqlNode).setExpressionEvaluator(ExpressionEvaluator.INSTANCE);
      }
    }

    /**
     * @param nodeToHandle
     *          current xml node to handle
     * @param targetContents
     *          existed nodes collection
     *
     * @return added sql node
     */
    SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
  }

  private static class BindHandler implements NodeHandler {
    public BindHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      final String name = nodeToHandle.getStringAttribute("name");
      final String expression = nodeToHandle.getStringAttribute("value");
      final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
      targetContents.add(node);
      return node;
    }
  }

  private class TrimHandler implements NodeHandler {
    public TrimHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String prefix = nodeToHandle.getStringAttribute("prefix");
      String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
      String suffix = nodeToHandle.getStringAttribute("suffix");
      String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
      TrimSqlNode trimSqlNode = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix,
          suffixOverrides);
      targetContents.add(trimSqlNode);
      return trimSqlNode;
    }
  }

  private class WhereHandler implements NodeHandler {
    public WhereHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      WhereSqlNode whereSqlNode = new WhereSqlNode(configuration, mixedSqlNode);
      targetContents.add(whereSqlNode);
      return whereSqlNode;
    }
  }

  private class SetHandler implements NodeHandler {
    public SetHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      SetSqlNode setSqlNode = new SetSqlNode(configuration, mixedSqlNode);
      targetContents.add(setSqlNode);
      return setSqlNode;
    }
  }

  private class ForEachHandler implements NodeHandler {
    public ForEachHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String collection = nodeToHandle.getStringAttribute("collection");
      Boolean nullable = nodeToHandle.getBooleanAttribute("nullable");
      String item = nodeToHandle.getStringAttribute("item");
      String index = nodeToHandle.getStringAttribute("index");
      String open = nodeToHandle.getStringAttribute("open");
      String close = nodeToHandle.getStringAttribute("close");
      String separator = nodeToHandle.getStringAttribute("separator");
      ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, nullable, index, item,
          open, close, separator);
      targetContents.add(forEachSqlNode);
      return forEachSqlNode;
    }
  }

  private class IfHandler implements NodeHandler {
    public IfHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String test = nodeToHandle.getStringAttribute("test");
      IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
      targetContents.add(ifSqlNode);
      return ifSqlNode;
    }
  }

  private class OtherwiseHandler implements NodeHandler {
    public OtherwiseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      targetContents.add(mixedSqlNode);
      return mixedSqlNode;
    }
  }

  private class ChooseHandler implements NodeHandler {
    public ChooseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      List<SqlNode> whenSqlNodes = new ArrayList<>();
      List<SqlNode> otherwiseSqlNodes = new ArrayList<>();
      handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
      SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
      ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
      targetContents.add(chooseSqlNode);
      return chooseSqlNode;
    }

    private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes,
        List<SqlNode> defaultSqlNodes) {
      List<XNode> children = chooseSqlNode.getChildren();
      for (XNode child : children) {
        String nodeName = child.getNode().getNodeName();
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        SqlNode nodeAdded = null;
        if (handler instanceof IfHandler) {
          nodeAdded = handler.handleNode(child, ifSqlNodes);
        } else if (handler instanceof OtherwiseHandler) {
          nodeAdded = handler.handleNode(child, defaultSqlNodes);
        }
        init(nodeAdded);
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
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      final String nodeName = nodeToHandle.getName();
      final MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      final List<SqlNode> contents = mixedSqlNode.getContents();
      final String test = nodeToHandle.getStringAttribute("test");
      SqlNode sqlNode = null;
      if (contents.size() == 1) {
        if (nodeName.equalsIgnoreCase("and")) {
          sqlNode = new AndSqlNode(configuration, test, contents.get(0));
        } else if (nodeName.equalsIgnoreCase("or")) {
          sqlNode = new OrSqlNode(configuration, test, contents.get(0));
        }
      } else if (contents.isEmpty()) {
        throw new BuilderException("syntax error about <" + nodeName + ">, empty <" + nodeName + "> is meaningless.");
      } else {
        MixedSqlNode childNode = parseDynamicTags(nodeToHandle);
        sqlNode = new ConditionConnectorSqlNode(configuration, nodeName, childNode.getContents());
      }
      targetContents.add(sqlNode);
      return sqlNode;
    }
  }

  /**
   * @see ForEachHandler
   */
  private class InHandler implements NodeHandler {

    @Override
    public SqlNode handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      String collection = nodeToHandle.getStringAttribute("collection");
      String item = nodeToHandle.getStringAttribute("item");
      String test = nodeToHandle.getStringAttribute("test");
      Boolean nullable = nodeToHandle.getBooleanAttribute("nullable");
      if (nullable == null) {
        nullable = configuration.isNullableOnForEach();
      }

      if (StringUtils.isEmpty(item)) {
        item = "item";
      }

      StaticTextSqlNode contents = new StaticTextSqlNode("#{" + item + "}");
      item = parseItemExpression(item);
      InSqlNode sqlNode = new InSqlNode(configuration, contents, collection, test, item, nullable);
      targetContents.add(sqlNode);
      return sqlNode;
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

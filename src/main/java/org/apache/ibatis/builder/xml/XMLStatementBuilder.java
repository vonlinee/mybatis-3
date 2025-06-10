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
package org.apache.ibatis.builder.xml;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.ibatis.binding.ParamMap;
import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 */
public class XMLStatementBuilder extends BaseBuilder {

  private final MapperBuilderAssistant assistant;
  private final String requiredDatabaseId;
  private final Class<?> mapperClass;

  public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant assistant, String databaseId,
      Class<?> mapperClass) {
    super(configuration);
    this.assistant = assistant;
    this.requiredDatabaseId = databaseId;
    this.mapperClass = mapperClass;
  }

  @Nullable
  public MappedStatement parseStatementNode(XNode context) {
    String id = context.getStringAttribute("id");
    String databaseId = context.getStringAttribute("databaseId");

    if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
      return null;
    }

    String nodeName = context.getNode().getNodeName();
    SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
    boolean useCache = context.getBooleanAttribute("useCache", isSelect);
    boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

    // Include Fragments before parsing
    processInclude(context);

    String parameterType = context.getStringAttribute("parameterType");
    Class<?> parameterTypeClass = resolveClass(parameterType);
    ParamNameResolver paramNameResolver = null;
    if (parameterTypeClass == null && mapperClass != null) {
      List<Method> mapperMethods = Arrays.stream(mapperClass.getMethods())
          .filter(m -> m.getName().equals(id) && !m.isDefault() && !m.isBridge()).collect(Collectors.toList());
      if (mapperMethods.size() == 1) {
        paramNameResolver = new ParamNameResolver(mapperClass, mapperMethods.get(0),
            configuration.isUseActualParamName());
        if (paramNameResolver.isUseParamMap()) {
          parameterTypeClass = ParamMap.class;
        } else {
          String[] paramNames = paramNameResolver.getNames();
          if (paramNames.length == 1) {
            Type paramType = paramNameResolver.getType(paramNames[0]);
            if (paramType instanceof Class) {
              parameterTypeClass = (Class<?>) paramType;
            }
          }
        }
      }
    }

    String lang = context.getStringAttribute("lang");
    LanguageDriver langDriver = this.getLanguageDriver(lang);

    // Parse selectKey after includes and remove them.
    processSelectKeyNodes(context, id, parameterTypeClass, langDriver);

    // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
    KeyGenerator keyGenerator = processKeyGenerator(context, id, sqlCommandType);

    SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass, paramNameResolver);

    StatementType statementType = this.resolveStatementType(context.getStringAttribute("statementType"));
    Integer fetchSize = context.getIntAttribute("fetchSize");
    Integer timeout = context.getIntAttribute("timeout");
    String parameterMap = context.getStringAttribute("parameterMap");
    Class<?> resultTypeClass = resolveClass(context.getStringAttribute("resultType"));
    String resultMap = context.getStringAttribute("resultMap");
    if (resultTypeClass == null && resultMap == null) {
      resultTypeClass = this.getMethodReturnType(assistant.getCurrentNamespace(), id);
    }
    String resultSetType = context.getStringAttribute("resultSetType");
    ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
    String keyProperty = context.getStringAttribute("keyProperty");
    String keyColumn = context.getStringAttribute("keyColumn");
    String resultSets = context.getStringAttribute("resultSets");
    boolean dirtySelect = context.getBooleanAttribute("affectData", Boolean.FALSE);

    MappedStatement mappedStatement = assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache,
        useCache, resultOrdered, keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets, dirtySelect,
        paramNameResolver);

    String countSql = context.getStringAttribute("countSql");
    if (countSql != null) {
      if (!countSql.contains(".")) {
        countSql = assistant.applyCurrentNamespace(countSql, false);
      }
    }
    mappedStatement.setCountStatement(countSql);
    return mappedStatement;
  }

  protected KeyGenerator processKeyGenerator(XNode context, String statementId, SqlCommandType sqlCommandType) {
    String keyStatementId = statementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    keyStatementId = assistant.applyCurrentNamespace(keyStatementId, true);

    final KeyGenerator keyGenerator;
    if (configuration.hasKeyGenerator(keyStatementId)) {
      keyGenerator = configuration.getKeyGenerator(keyStatementId);
    } else {
      keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
          configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
              ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
    }
    return keyGenerator;
  }

  @Override
  protected ResultSetType resolveResultSetType(String alias) {
    ResultSetType resultSetTypeEnum = super.resolveResultSetType(alias);
    if (resultSetTypeEnum == null) {
      resultSetTypeEnum = configuration.getDefaultResultSetType();
    }
    return resultSetTypeEnum;
  }

  public void processInclude(XNode context) {
    XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, assistant);
    includeParser.applyIncludes(context.getNode());
  }

  protected void processSelectKeyNodes(XNode context, String id, Class<?> parameterTypeClass,
      LanguageDriver langDriver) {
    List<XNode> selectKeyNodes = context.evalNodes("selectKey");
    if (configuration.getDatabaseId() != null) {
      parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
    }
    parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
    removeSelectKeyNodes(selectKeyNodes);
  }

  protected void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass,
      LanguageDriver langDriver, String skRequiredDatabaseId) {
    for (XNode nodeToHandle : list) {
      String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
      String databaseId = nodeToHandle.getStringAttribute("databaseId");
      if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
        parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
      }
    }
  }

  protected void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass,
      LanguageDriver langDriver, String databaseId) {
    String resultType = nodeToHandle.getStringAttribute("resultType");
    Class<?> resultTypeClass = resolveClass(resultType);
    StatementType statementType = resolveStatementType(nodeToHandle.getStringAttribute("statementType"));
    String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
    String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
    boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));

    // defaults
    boolean useCache = false;
    boolean resultOrdered = false;
    KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
    boolean flushCache = false;

    SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;

    assistant.addSelectKeyStatement(id, sqlSource, statementType, sqlCommandType, parameterTypeClass, resultTypeClass,
        flushCache, useCache, resultOrdered, keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null, false,
        null);

    id = assistant.applyCurrentNamespace(id, false);

    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
  }

  protected void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
    for (XNode nodeToHandle : selectKeyNodes) {
      nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
    }
  }

  protected boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      return requiredDatabaseId.equals(databaseId);
    }
    if (databaseId != null) {
      return false;
    }
    id = assistant.applyCurrentNamespace(id, false);
    if (!this.configuration.hasStatement(id, false)) {
      return true;
    }
    // skip this statement if there is a previous one with a not null databaseId
    MappedStatement previous = this.configuration.getMappedStatement(id, false); // issue #2
    return previous.getDatabaseId() == null;
  }

  protected LanguageDriver getLanguageDriver(String lang) {
    return assistant.resolveLanguageDriver(lang);
  }
}

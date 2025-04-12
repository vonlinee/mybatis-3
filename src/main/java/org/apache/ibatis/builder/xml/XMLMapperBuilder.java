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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BuilderAssistant;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.ResultMappingConstructorResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.scripting.FetchType;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLMapperBuilder {

  private final XPathParser parser;
  private final BuilderAssistant assistant;
  private final Map<String, XNode> sqlFragments;
  private final String resource;
  private Class<?> mapperClass;
  private final Configuration configuration;

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
      Map<String, XNode> sqlFragments, Class<?> mapperClass) {
    this(inputStream, configuration, resource, sqlFragments, mapperClass.getName());
    this.mapperClass = mapperClass;
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
      Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.assistant.setCurrentNamespace(namespace);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
      Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()), configuration,
        resource, sqlFragments);
  }

  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource,
      Map<String, XNode> sqlFragments) {
    this.configuration = configuration;
    this.assistant = new BuilderAssistant(configuration);
    this.assistant.setCurrentLoadedResource(resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  public void parse() {
    this.assistant.loadResource(resource, (configuration, resource) -> {
      configurationElement(parser.evalNode("/mapper"));
      // bind Mapper For Namespace
      String namespace = assistant.getCurrentNamespace();
      if (namespace != null) {
        // ignore, bound type is not required
        Class<?> boundType = Resources.classForNameOrNull(namespace);
        if (boundType != null && !configuration.hasMapper(boundType)) {
          // Spring may not know the real resource name, so we set a flag
          // to prevent loading again this resource from the mapper interface
          // look at MapperAnnotationBuilder#loadXmlResource
          configuration.addLoadedResource("namespace:" + namespace);
          configuration.addMapper(boundType);
        }
      }
    });
    this.assistant.parsePending(false);
  }

  public XNode getSqlFragment(String refId) {
    return sqlFragments.get(refId);
  }

  private void configurationElement(XNode context) {
    try {
      String namespace = context.getStringAttribute("namespace");
      if (StringUtils.isEmpty(namespace)) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      assistant.setCurrentNamespace(namespace);
      cacheRefElement(context.evalNode("cache-ref"));
      cacheElement(context.evalNode("cache"));
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      sqlElement(context.evalNodes("/mapper/sql"));
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  private void buildStatementFromContext(List<XNode> list) {
    final String targetDatabaseId = assistant.getDatabaseId();
    if (targetDatabaseId != null) {
      buildStatementFromContext(list, targetDatabaseId);
    }
    buildStatementFromContext(list, null);
  }

  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, assistant, context,
          requiredDatabaseId, mapperClass);
      try {
        statementParser.parseStatementNode(context);
      } catch (IncompleteElementException e) {
        assistant.addIncompleteStatement(statementParser, context);
      }
    }
  }

  private void cacheRefElement(XNode context) {
    if (context != null) {
      assistant.addCacheRef(context.getStringAttribute("namespace"));
      CacheRefResolver cacheRefResolver = new CacheRefResolver(assistant, context.getStringAttribute("namespace"));
      try {
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        assistant.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  private void cacheElement(XNode context) {
    if (context != null) {
      String type = context.getStringAttribute("type", "PERPETUAL");
      Class<? extends Cache> typeClass = assistant.resolveAlias(type);
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = assistant.resolveAlias(eviction);
      Long flushInterval = context.getLongAttribute("flushInterval");
      Integer size = context.getIntAttribute("size");
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      boolean blocking = context.getBooleanAttribute("blocking", false);
      Properties props = context.getChildrenAsProperties();

      assistant.addCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  private void parameterMapElement(List<XNode> list) {
    for (XNode parameterMapNode : list) {
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = assistant.resolveClass(type);
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        ParameterMode modeEnum = assistant.resolveParameterMode(mode);
        Class<?> javaTypeClass = assistant.resolveClass(javaType);
        JdbcType jdbcTypeEnum = assistant.resolveJdbcType(jdbcType);
        Class<? extends TypeHandler<?>> typeHandlerClass = assistant.resolveClass(typeHandler);
        ParameterMapping parameterMapping = assistant.buildParameterMapping(parameterClass, property, javaTypeClass,
            jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      assistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  private void resultMapElements(List<XNode> list) {
    for (XNode resultMapNode : list) {
      try {
        ResultMap resultMap = parseResultMap(resultMapNode, Collections.emptyList(), null);
        assistant.addResultMap(resultMap);
      } catch (IncompleteElementException e) {
        // ignore, it will be retried
      }
    }
  }

  public Class<?> determineTypeOfResultMap(XNode resultMapNode) {
    String type = resultMapNode.getStringAttribute("type");
    if (StringUtils.isBlank(type)) {
      type = resultMapNode.getStringAttribute("ofType");
    }
    if (StringUtils.isBlank(type)) {
      type = resultMapNode.getStringAttribute("resultType");
    }
    if (StringUtils.isBlank(type)) {
      type = resultMapNode.getStringAttribute("javaType");
    }
    if (StringUtils.isBlank(type)) {
      return null;
    }
    return assistant.resolveClass(type);
  }

  private ResultMap parseResultMap(XNode resultMapNode, List<ResultMapping> additionalResultMappings,
      Class<?> enclosingType) {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());

    Class<?> typeClass = determineTypeOfResultMap(resultMapNode);
    if (typeClass == null) {
      typeClass = inheritEnclosingType(resultMapNode, enclosingType);
    }

    String id = resultMapNode.getStringAttribute("id", resultMapNode::getValueBasedIdentifier);
    String extend = resultMapNode.getStringAttribute("extends");
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");

    Discriminator discriminator = null;
    List<ResultMapping> resultMappings = new ArrayList<>(additionalResultMappings);
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      if ("constructor".equals(resultChild.getName())) {
        processConstructorElement(resultChild, typeClass, resultMappings, id);
      } else if ("discriminator".equals(resultChild.getName())) {
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        List<ResultFlag> flags = new ArrayList<>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }

    ResultMapResolver resultMapResolver = new ResultMapResolver(assistant, id, typeClass, extend, discriminator,
        resultMappings, autoMapping);
    try {
      return resultMapResolver.resolve();
    } catch (IncompleteElementException e) {
      assistant.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  protected Class<?> inheritEnclosingType(XNode resultMapNode, Class<?> enclosingType) {
    if ("association".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      String property = resultMapNode.getStringAttribute("property");
      if (property != null && enclosingType != null) {
        return assistant.getSetterType(enclosingType, property);
      }
    } else if ("case".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      return enclosingType;
    }
    return null;
  }

  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings,
      String id) {

    final List<ResultMapping> mappings = new ArrayList<>();
    for (XNode argChild : resultChild.getChildren()) {
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }

      mappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }

    final ResultMappingConstructorResolver resolver = new ResultMappingConstructorResolver(configuration, mappings,
        resultType, id);
    resultMappings.addAll(resolver.resolveWithConstructor());
  }

  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType,
      List<ResultMapping> resultMappings) {
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    Class<?> javaTypeClass = assistant.resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = assistant.resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = assistant.resolveJdbcType(jdbcType);
    Map<String, String> discriminatorMap = new HashMap<>();
    for (XNode caseChild : context.getChildren()) {
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap",
          () -> processNestedResultMappings(caseChild, resultMappings, resultType));
      discriminatorMap.put(value, resultMap);
    }
    return assistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass,
        discriminatorMap);
  }

  private void sqlElement(List<XNode> list) {
    final String targetDatabaseId = assistant.getDatabaseId();
    if (targetDatabaseId != null) {
      sqlElement(list, targetDatabaseId);
    }
    sqlElement(list, null);
  }

  private void sqlElement(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      id = assistant.applyCurrentNamespace(id, false);
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        sqlFragments.put(id, context);
      }
    }
  }

  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      return requiredDatabaseId.equals(databaseId);
    }
    if (databaseId != null) {
      return false;
    }
    if (!this.sqlFragments.containsKey(id)) {
      return true;
    }
    // skip this fragment if there is a previous one with a not null databaseId
    XNode context = this.sqlFragments.get(id);
    return context.getStringAttribute("databaseId") == null;
  }

  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) {
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      property = context.getStringAttribute("name");
    } else {
      property = context.getStringAttribute("property");
    }
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String nestedSelect = context.getStringAttribute("select");
    String nestedResultMap = context.getStringAttribute("resultMap");
    if (StringUtils.isBlank(nestedResultMap)) {
      nestedResultMap = processNestedResultMappings(context, Collections.emptyList(), resultType);
    }
    String notNullColumn = context.getStringAttribute("notNullColumn");
    String columnPrefix = context.getStringAttribute("columnPrefix");
    String typeHandler = context.getStringAttribute("typeHandler");
    String resultSet = context.getStringAttribute("resultSet");
    String foreignColumn = context.getStringAttribute("foreignColumn");

    boolean lazy = assistant.resolveFetchType(context.getStringAttribute("fetchType")) == FetchType.LAZY;
    Class<?> javaTypeClass = assistant.resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = assistant.resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = assistant.resolveJdbcType(jdbcType);
    return assistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect,
        nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }

  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings,
      Class<?> enclosingType) {
    if (StringUtils.equalsAny(context.getName(), "association", "collection", "case")
        && context.getStringAttribute("select") == null) {
      validateCollection(context, enclosingType);
      ResultMap resultMap = parseResultMap(context, resultMappings, enclosingType);
      assistant.addResultMap(resultMap);
      return resultMap.getId();
    }
    return null;
  }

  protected void validateCollection(XNode context, Class<?> enclosingType) {
    if ("collection".equals(context.getName()) && context.getStringAttribute("resultMap") == null
        && context.getStringAttribute("javaType") == null) {
      String property = context.getStringAttribute("property");
      if (!assistant.hasSetter(enclosingType, property)) {
        throw new BuilderException(
            "Ambiguous collection type for property '" + property + "'. You must specify 'javaType' or 'resultMap'.");
      }
    }
  }
}

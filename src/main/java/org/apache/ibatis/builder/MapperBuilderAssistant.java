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
package org.apache.ibatis.builder;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.MappedStatement;
import org.apache.ibatis.scripting.ResultSetType;
import org.apache.ibatis.scripting.SqlCommandType;
import org.apache.ibatis.scripting.SqlSource;
import org.apache.ibatis.scripting.StatementType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Clinton Begin
 */
public class MapperBuilderAssistant {

  protected final Configuration configuration;
  private String currentNamespace;
  private String currentResource;
  private Cache currentCache;
  private boolean unresolvedCacheRef; // issue #676

  public MapperBuilderAssistant(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setCurrentLoadedResource(String resource) {
    this.currentResource = resource;
    ErrorContext.instance().resource(resource);
  }

  public String getCurrentNamespace() {
    return currentNamespace;
  }

  public void setCurrentNamespace(String currentNamespace) {
    if (currentNamespace == null) {
      throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
    }

    if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
      throw new BuilderException(
          "Wrong namespace. Expected '" + this.currentNamespace + "' but found '" + currentNamespace + "'.");
    }

    this.currentNamespace = currentNamespace;
  }

  public String applyCurrentNamespace(String base, boolean isReference) {
    if (base == null) {
      return null;
    }
    if (isReference) {
      // is it qualified with any namespace yet?
      if (base.contains(".")) {
        return base;
      }
    } else {
      // is it qualified with this namespace yet?
      if (base.startsWith(currentNamespace + ".")) {
        return base;
      }
      if (base.contains(".")) {
        throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
      }
    }
    return currentNamespace + "." + base;
  }

  public final void setCurrentCache(Cache cache) {
    this.currentCache = cache;
  }

  public boolean hasUnsolvedCacheRef() {
    return unresolvedCacheRef;
  }

  public void markCachedRefUnsolved() {
    unresolvedCacheRef = true;
  }

  public void markCacheRefResolved() {
    unresolvedCacheRef = false;
  }

  public Cache useCacheRef(String namespace) {
    if (namespace == null) {
      throw new BuilderException("cache-ref element requires a namespace attribute.");
    }
    try {
      markCachedRefUnsolved();
      Cache cache = configuration.getCache(namespace);
      if (cache == null) {
        throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
      }
      setCurrentCache(cache);
      markCacheRefResolved();
      return cache;
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
    }
  }

  public Cache buildCache(Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass, Long flushInterval,
      Integer size, boolean readWrite, boolean blocking, Properties props) {
    // @formatter:off
    return new CacheBuilder(currentNamespace)
      .implementation(typeClass == null ? PerpetualCache.class : typeClass)
      .addDecorator(evictionClass == null ? LruCache.class : evictionClass)
      .clearInterval(flushInterval)
      .size(size)
      .readWrite(readWrite)
      .blocking(blocking)
      .properties(props)
      .build();
    // @formatter:on
  }

  public ParameterMap buildParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
    id = applyCurrentNamespace(id, false);
    return new ParameterMap(id, parameterClass, parameterMappings);
  }

  public ParameterMapping buildParameterMapping(Class<?> parameterType, String property, Class<?> javaType,
      JdbcType jdbcType, String resultMap, ParameterMode parameterMode, Class<? extends TypeHandler<?>> typeHandler,
      Integer numericScale) {
    resultMap = applyCurrentNamespace(resultMap, true);

    // Class parameterType = parameterMapBuilder.type();
    Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
    TypeHandler<?> typeHandlerInstance = configuration.resolveTypeHandler(javaTypeClass, jdbcType, typeHandler);
    // @formatter:off
    return new ParameterMapping.Builder(property, javaTypeClass)
      .jdbcType(jdbcType)
      .resultMapId(resultMap)
      .mode(parameterMode)
      .numericScale(numericScale)
      .typeHandler(typeHandlerInstance)
      .build();
    // @formatter:on
  }

  public ResultMap buildResultMap(String id, Class<?> type, String extend, Discriminator discriminator,
      List<ResultMapping> resultMappings, Boolean autoMapping) {
    id = applyCurrentNamespace(id, false);
    extend = applyCurrentNamespace(extend, true);

    if (extend != null) {
      if (!configuration.hasResultMap(extend)) {
        throw new IncompleteElementException("Could not find a parent resultMap with id '" + extend + "'");
      }
      ResultMap resultMap = configuration.getResultMap(extend);
      List<ResultMapping> extendedResultMappings = new ArrayList<>(resultMap.getResultMappings());
      extendedResultMappings.removeAll(resultMappings);
      // Remove parent constructor if this resultMap declares a constructor.
      boolean declaresConstructor = false;
      for (ResultMapping resultMapping : resultMappings) {
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          declaresConstructor = true;
          break;
        }
      }
      if (declaresConstructor) {
        extendedResultMappings.removeIf(resultMapping -> resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR));
      }
      resultMappings.addAll(extendedResultMappings);
    }
    // @formatter:off
    return new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
      .discriminator(discriminator)
      .build();
    // @formatter:on
  }

  public Discriminator buildDiscriminator(Class<?> resultType, String column, Class<?> javaType, JdbcType jdbcType,
      Class<? extends TypeHandler<?>> typeHandler, Map<String, String> discriminatorMap) {
    ResultMapping resultMapping = buildResultMapping(resultType, null, column, javaType, jdbcType, null, null, null,
        null, typeHandler, new ArrayList<>(), null, null, false);
    Map<String, String> namespaceDiscriminatorMap = new HashMap<>();
    for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
      String resultMap = e.getValue();
      resultMap = applyCurrentNamespace(resultMap, true);
      namespaceDiscriminatorMap.put(e.getKey(), resultMap);
    }
    return new Discriminator.Builder(resultMapping, namespaceDiscriminatorMap).build();
  }

  public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
      SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
      String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
      boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
      LanguageDriver lang, String resultSets, boolean dirtySelect, ParamNameResolver paramNameResolver) {

    if (unresolvedCacheRef) {
      throw new IncompleteElementException("Cache-ref not yet resolved");
    }

    id = applyCurrentNamespace(id, false);

    // @formatter:off
    MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
      .resource(currentResource)
      .fetchSize(fetchSize)
      .timeout(timeout)
      .statementType(statementType)
      .keyGenerator(keyGenerator)
      .keyProperty(keyProperty)
      .keyColumn(keyColumn)
      .databaseId(databaseId)
      .lang(lang)
      .resultOrdered(resultOrdered)
      .resultSets(resultSets)
      .resultMaps(getStatementResultMaps(resultMap, resultType, id))
      .resultSetType(resultSetType)
      .flushCacheRequired(flushCache)
      .useCache(useCache)
      .cache(currentCache)
      .dirtySelect(dirtySelect)
      .paramNameResolver(paramNameResolver);
    // @formatter:on
    ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
    if (statementParameterMap != null) {
      statementBuilder.parameterMap(statementParameterMap);
    }

    MappedStatement statement = statementBuilder.build();
    configuration.addMappedStatement(statement);
    return statement;
  }

  private ParameterMap getStatementParameterMap(String parameterMapName, Class<?> parameterTypeClass,
      String statementId) {
    parameterMapName = applyCurrentNamespace(parameterMapName, true);
    ParameterMap parameterMap = null;
    if (parameterMapName != null) {
      try {
        parameterMap = configuration.getParameterMap(parameterMapName);
      } catch (IllegalArgumentException e) {
        throw new IncompleteElementException("Could not find parameter map " + parameterMapName, e);
      }
    } else if (parameterTypeClass != null) {
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      parameterMap = new ParameterMap(statementId + "-Inline", parameterTypeClass, parameterMappings);
    }
    return parameterMap;
  }

  private List<ResultMap> getStatementResultMaps(String resultMap, Class<?> resultType, String statementId) {
    resultMap = applyCurrentNamespace(resultMap, true);

    List<ResultMap> resultMaps = new ArrayList<>();
    if (resultMap != null) {
      String[] resultMapNames = resultMap.split(",");
      for (String resultMapName : resultMapNames) {
        try {
          resultMaps.add(configuration.getResultMap(resultMapName.trim()));
        } catch (IllegalArgumentException e) {
          throw new IncompleteElementException(
              "Could not find result map '" + resultMapName + "' referenced from '" + statementId + "'", e);
        }
      }
    } else if (resultType != null) {
      ResultMap inlineResultMap = new ResultMap.Builder(configuration, statementId + "-Inline", resultType,
          new ArrayList<>(), null).build();
      resultMaps.add(inlineResultMap);
    }
    return resultMaps;
  }

  public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
      JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
      Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags, String resultSet, String foreignColumn,
      boolean lazy) {
    Entry<Type, Class<?>> setterType = resolveSetterType(resultType, property, javaType);
    TypeHandler<?> typeHandlerInstance = configuration.resolveTypeHandler(setterType.getKey(), jdbcType, typeHandler);
    List<ResultMapping> composites;
    if (StringUtils.isAllEmpty(nestedSelect, foreignColumn)) {
      composites = Collections.emptyList();
    } else {
      composites = parseCompositeColumnName(column);
    }
    // @formatter:off
    return new ResultMapping.Builder(property, column, setterType.getValue(), configuration.isLazyLoadingEnabled())
      .jdbcType(jdbcType)
      .nestedQueryId(applyCurrentNamespace(nestedSelect, true))
      .nestedResultMapId(applyCurrentNamespace(nestedResultMap, true))
      .resultSet(resultSet)
      .typeHandler(typeHandlerInstance)
      .flags(flags == null ? new ArrayList<>() : flags)
      .composites(composites)
      .notNullColumns(parseMultipleColumnNames(notNullColumn))
      .columnPrefix(columnPrefix)
      .foreignColumn(foreignColumn)
      .lazy(lazy).build();
    // @formatter:on
  }

  /**
   * multiple column names
   *
   * @param columnName
   *          multiple columns like (column="{prop1=col1,prop2=col2}")
   *
   * @return column names
   */
  @NotNull
  public static Set<String> parseMultipleColumnNames(String columnName) {
    if (StringUtils.isBlank(columnName)) {
      return Collections.emptySet();
    }
    Set<String> columns = new HashSet<>();
    if (columnName.indexOf(',') > -1) {
      StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
      while (parser.hasMoreTokens()) {
        String column = parser.nextToken();
        columns.add(column.trim());
      }
    } else {
      columns.add(columnName.trim());
    }
    return columns;
  }

  public List<ResultMapping> parseCompositeColumnName(String columnName) {
    List<ResultMapping> compositeMappings = Collections.emptyList();
    if (StringUtils.containsAny(columnName, '=', ',')) {
      compositeMappings = new ArrayList<>();
      StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
      while (parser.hasMoreTokens()) {
        String property = parser.nextToken();
        String column = parser.nextToken();
        ResultMapping complexResultMapping = new ResultMapping.Builder(property, column,
            configuration.isLazyLoadingEnabled()).build();
        compositeMappings.add(complexResultMapping);
      }
    }
    return compositeMappings;
  }

  private Entry<Type, Class<?>> resolveSetterType(Class<?> resultType, String property, Class<?> javaType) {
    if (javaType != null) {
      return Map.entry(javaType, javaType);
    }
    if (property != null) {
      MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
      try {
        return metaResultType.getGenericSetterType(property);
      } catch (Exception e) {
        // Not all property types are resolvable.
      }
    }
    return Map.entry(Object.class, Object.class);
  }

  private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType,
      JdbcType jdbcType) {
    if (javaType == null) {
      if (JdbcType.CURSOR.equals(jdbcType)) {
        javaType = ResultSet.class;
      } else if (Map.class.isAssignableFrom(resultType)) {
        javaType = Object.class;
      } else {
        MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        javaType = metaResultType.getGetterType(property);
      }
    }
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  public void loadResource(String resource, BiConsumer<Configuration, String> callback) {
    if (!configuration.isResourceLoaded(resource)) {
      callback.accept(this.configuration, resource);
      configuration.addLoadedResource(resource);
    }
  }

  public void parsePending(boolean reportUnresolved) {
    configuration.parsePendingResultMaps(reportUnresolved);
    configuration.parsePendingCacheRefs(reportUnresolved);
    configuration.parsePendingStatements(reportUnresolved);
  }

  public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
    configuration.addIncompleteResultMap(resultMapResolver);
  }

  public Class<?> getSetterType(Class<?> type, String property) {
    MetaClass metaResultType = MetaClass.forClass(type, configuration.getReflectorFactory());
    return metaResultType.getSetterType(property);
  }

  public boolean hasSetter(Class<?> enclosingType, String property) {
    MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
    return metaResultType.hasSetter(property);
  }

  public <T> Class<? extends T> resolveClass(String alias) {
    try {
      return alias == null ? null : configuration.resolveAlias(alias);
    } catch (Exception e) {
      throw new BuilderException("Error resolving class. Cause: " + e, e);
    }
  }
}

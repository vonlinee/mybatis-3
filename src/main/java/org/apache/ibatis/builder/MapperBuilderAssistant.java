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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.jndi.JndiDataSourceFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.internal.util.CollectionUtils;
import org.apache.ibatis.internal.util.ObjectUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.InlineParameterMap;
import org.apache.ibatis.mapping.InlineResultMap;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.jetbrains.annotations.NotNull;

/**
 * helper class when build configuration
 *
 * @author Clinton Begin
 */
public class MapperBuilderAssistant extends BaseBuilder {

  private String currentNamespace;
  private final String resource;
  private Cache currentCache;
  private boolean unresolvedCacheRef; // issue #676

  public MapperBuilderAssistant(Configuration configuration, String resource) {
    super(configuration);
    ErrorContext.instance().resource(resource);
    this.resource = resource;
  }

  public static String getStatementId(Class<?> mapperClass, String methodName) {
    return mapperClass.getName() + "." + methodName;
  }

  public String getCurrentNamespace() {
    return currentNamespace;
  }

  public boolean hasUnresolvedCacheRef() {
    return unresolvedCacheRef;
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

  public String applyNamespace(String namespace, String base, boolean isReference) {
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
      if (base.startsWith(namespace + ".")) {
        return base;
      }
      if (base.contains(".")) {
        throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
      }
    }
    return namespace + "." + base;
  }

  public Cache useCacheRef(String namespace) {
    if (namespace == null) {
      throw new BuilderException("cache-ref element requires a namespace attribute.");
    }
    try {
      unresolvedCacheRef = true;
      Cache cache = configuration.getCache(namespace);
      if (cache == null) {
        throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
      }
      currentCache = cache;
      unresolvedCacheRef = false;
      return cache;
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
    }
  }

  public Cache useNewCache(Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass, Long flushInterval,
      Integer size, boolean readWrite, boolean blocking, Properties props) {
    // @formatter:off
    Cache cache = new CacheBuilder(currentNamespace)
      .implementation(ObjectUtils.nonNullOrElse(typeClass, PerpetualCache.class))
      .addDecorator(ObjectUtils.nonNullOrElse(evictionClass, LruCache.class))
      .clearInterval(flushInterval).size(size)
      .readWrite(readWrite)
      .blocking(blocking)
      .properties(props)
      .build();
    // @formatter:on
    configuration.addCache(cache);
    currentCache = cache;
    return cache;
  }

  // @formatter:off
  public ParameterMap buildParameterMap(String namespace,
                                        String id,
                                        Class<?> parameterClass,
                                        List<ParameterMapping> parameterMappings) {
    id = applyNamespace(namespace, id, false);
    if (parameterMappings == null) {
      parameterMappings = Collections.emptyList();
    }
    return new ParameterMap.Builder(id, parameterClass, parameterMappings).build();
  }
  // @formatter:on

  // @formatter:off
  public ParameterMapping buildParameterMapping(
    String namespace,
    Class<?> parameterType,
    String property,
    Class<?> javaType,
    JdbcType jdbcType,
    String resultMap,
    ParameterMode parameterMode,
    Class<? extends TypeHandler<?>> typeHandler,
    Integer numericScale) {
    resultMap = applyNamespace(namespace, resultMap, true);

    // Class parameterType = parameterMapBuilder.type();
    Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, jdbcType, typeHandler);

    return new ParameterMapping.Builder(property, javaTypeClass)
      .jdbcType(jdbcType)
      .resultMapId(resultMap)
      .mode(parameterMode)
      .numericScale(numericScale)
      .typeHandler(typeHandlerInstance)
      .build();
    // @formatter:on
  }

  public ResultMap addResultMap(String id, Class<?> type, String extend, Discriminator discriminator,
      List<ResultMapping> resultMappings, Boolean autoMapping) {
    id = applyCurrentNamespace(id, false);
    extend = applyCurrentNamespace(extend, true);

    if (extend != null) {
      if (!configuration.hasResultMap(extend)) {
        throw new IncompleteElementException("Could not find a parent result map with id '" + extend + "'");
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
    ResultMap resultMap = new ResultMap.Builder(id, type, resultMappings, autoMapping).discriminator(discriminator)
        .build(configuration);
    configuration.addResultMap(resultMap);
    return resultMap;
  }

  // @formatter:off
  public Discriminator buildDiscriminator(Class<?> resultType,
                                          String column,
                                          Class<?> javaType,
                                          JdbcType jdbcType,
                                          Class<? extends TypeHandler<?>> typeHandler,
                                          Map<String, String> discriminatorMap) {
    ResultMapping resultMapping = buildResultMapping(resultType, null, column, javaType, jdbcType, null, null, null,
        null, typeHandler, new ArrayList<>(0), null, null, false);
    Map<String, String> namespaceDiscriminatorMap = new HashMap<>(discriminatorMap.size());
    for (Entry<String, String> e : discriminatorMap.entrySet()) {
      String resultMap = e.getValue();
      resultMap = applyCurrentNamespace(resultMap, true);
      namespaceDiscriminatorMap.put(e.getKey(), resultMap);
    }
    return new Discriminator.Builder(resultMapping, namespaceDiscriminatorMap).build();
  }
  // @formatter:on

  public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
      SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
      String resultMapId, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
      boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
      LanguageDriver lang, String resultSets, boolean dirtySelect, ParamNameResolver paramNameResolver) {

    if (hasUnresolvedCacheRef()) {
      throw new IncompleteElementException("Cache-ref not yet resolved");
    }

    id = applyCurrentNamespace(id, false);

    // @formatter:off
    MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
      .resource(resource)
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
      .resultMaps(getStatementResultMaps(resultMapId, resultType, id))
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

  /**
   * {@code <selectKey></selectKey>}
   *
   * @return {@code <selectKey/>} statement
   */
  public MappedStatement addSelectKeyStatement(String id, SqlSource sqlSource, StatementType statementType,
      SqlCommandType sqlCommandType, Class<?> parameterType, Class<?> resultType, boolean flushCache, boolean useCache,
      boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
      LanguageDriver lang, String resultSets, boolean dirtySelect, ParamNameResolver paramNameResolver) {
    return addMappedStatement(id, sqlSource, statementType, sqlCommandType, null, null, null, parameterType, null,
        resultType, null, flushCache, useCache, resultOrdered, keyGenerator, keyProperty, keyColumn, databaseId, lang,
        resultSets, dirtySelect, paramNameResolver);
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
      parameterMap = new InlineParameterMap(statementId, parameterTypeClass);
    }
    return parameterMap;
  }

  private List<ResultMap> getStatementResultMaps(String resultMapId, Class<?> resultType, String statementId) {
    resultMapId = applyCurrentNamespace(resultMapId, true);
    List<ResultMap> resultMaps = Collections.emptyList();
    if (resultMapId != null) {
      String[] resultMapNames = resultMapId.split(",");
      resultMaps = new ArrayList<>(resultMapNames.length);
      for (String resultMapName : resultMapNames) {
        try {
          resultMaps.add(configuration.getResultMap(resultMapName.trim()));
        } catch (IllegalArgumentException e) {
          throw new IncompleteElementException(
              "Could not find result map '" + resultMapName + "' referenced from '" + statementId + "'", e);
        }
      }
    } else if (resultType != null) {
      resultMaps = new ArrayList<>(1);
      resultMaps.add(new InlineResultMap(statementId, resultType));
    }
    return resultMaps;
  }

  public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
      JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
      Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags, String resultSet, String foreignColumn,
      boolean lazy) {
    Entry<Type, Class<?>> setterType = this.resolveSetterType(resultType, property, javaType);
    TypeHandler<?> typeHandlerInstance = this.resolveTypeHandler(setterType.getKey(), jdbcType, typeHandler);
    List<ResultMapping> composites;
    if (StringUtils.isEmpty(nestedSelect) && StringUtils.isEmpty(foreignColumn)) {
      composites = Collections.emptyList();
    } else {
      composites = parseCompositeColumnName(column, lazy);
    }
    // @formatter:off
    return new ResultMapping.Builder(property, column, setterType.getValue())
      .jdbcType(jdbcType)
      .nestedQueryId(applyCurrentNamespace(nestedSelect, true))
      .nestedResultMapId(applyCurrentNamespace(nestedResultMap, true))
      .resultSet(resultSet)
      .typeHandler(typeHandlerInstance)
      .flags(flags)
      .composites(composites)
      .notNullColumns(parseMultipleColumnNames(notNullColumn))
      .columnPrefix(columnPrefix)
      .foreignColumn(foreignColumn)
      .lazy(lazy)
      .build();
    // @formatter:on
  }

  /**
   * Backward compatibility signature 'buildResultMapping'.
   *
   * @param resultType
   *          the result type
   * @param property
   *          the property
   * @param column
   *          the column
   * @param javaType
   *          the java type
   * @param jdbcType
   *          the jdbc type
   * @param nestedSelect
   *          the nested select
   * @param nestedResultMap
   *          the nested result map
   * @param notNullColumn
   *          the not null column
   * @param columnPrefix
   *          the column prefix
   * @param typeHandler
   *          the type handler
   * @param flags
   *          the flags
   *
   * @return the result mapping
   */
  public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
      JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
      Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags) {
    return buildResultMapping(resultType, property, column, javaType, jdbcType, nestedSelect, nestedResultMap,
        notNullColumn, columnPrefix, typeHandler, flags, null, null, configuration.isLazyLoadingEnabled());
  }

  /**
   * Gets the language driver.
   *
   * @param langClass
   *          the lang class
   *
   * @return the language driver
   *
   * @deprecated Use {@link Configuration#getLanguageDriver(Class)}
   */
  @Deprecated
  public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass) {
    return configuration.getLanguageDriver(langClass);
  }

  public Set<String> parseMultipleColumnNames(String columnName) {
    if (columnName == null) {
      return Collections.emptySet();
    }
    Set<String> columns = new HashSet<>();
    if (columnName.indexOf(',') > -1) {
      StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
      while (parser.hasMoreTokens()) {
        String column = parser.nextToken();
        columns.add(column);
      }
    } else {
      columns.add(columnName);
    }
    return columns;
  }

  public List<ResultMapping> parseCompositeColumnName(String columnName, boolean lazyLoadingEnabled) {
    List<ResultMapping> composites = new ArrayList<>();
    if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
      StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
      while (parser.hasMoreTokens()) {
        String property = parser.nextToken();
        String column = parser.nextToken();
        ResultMapping complexResultMapping = new ResultMapping.Builder(property, column, (TypeHandler<?>) null)
            .lazy(lazyLoadingEnabled).build();
        composites.add(complexResultMapping);
      }
    }
    return composites;
  }

  private Entry<Type, Class<?>> resolveSetterType(Class<?> resultType, String property, Class<?> javaType) {
    if (javaType != null) {
      return CollectionUtils.entry(javaType, javaType);
    }
    if (property != null) {
      MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
      try {
        return metaResultType.getGenericSetterType(property);
      } catch (Exception e) {
        // Not all property types are resolvable.
      }
    }
    return CollectionUtils.entry(Object.class, Object.class);
  }

  protected Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType,
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

  public static MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
      Class<?> declaringClass, Configuration configuration) {
    String statementId = mapperInterface.getName() + "." + methodName;
    if (configuration.hasStatement(statementId)) {
      return configuration.getMappedStatement(statementId);
    }
    if (mapperInterface.equals(declaringClass)) {
      return null;
    }
    for (Class<?> superInterface : mapperInterface.getInterfaces()) {
      if (declaringClass.isAssignableFrom(superInterface)) {
        MappedStatement ms = resolveMappedStatement(superInterface, methodName, declaringClass, configuration);
        if (ms != null) {
          return ms;
        }
      }
    }
    return null;
  }

  public LanguageDriver resolveLanguageDriver(String lang) {
    Class<? extends LanguageDriver> langClass = null;
    if (lang != null) {
      langClass = resolveClass(lang);
    }
    return configuration.getLanguageDriver(langClass);
  }

  public static void registerDefaultTypeAlias(@NotNull TypeAliasRegistry typeAliasRegistry) {
    typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
    typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);

    typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
    typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
    typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);

    typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
    typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
    typeAliasRegistry.registerAlias("LRU", LruCache.class);
    typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
    typeAliasRegistry.registerAlias("WEAK", WeakCache.class);

    typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);

    typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);

    typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
    typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
    typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
    typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
    typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
    typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
    typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);

    typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
    typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);
  }
}

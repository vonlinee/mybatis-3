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
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
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
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Clinton Begin
 *
 * @see BuilderException
 */
public class MapperBuilderAssistant {

  private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

  protected final Configuration configuration;
  protected final TypeAliasRegistry typeAliasRegistry;
  protected final TypeHandlerRegistry typeHandlerRegistry;

  private String currentNamespace;
  private final String resource;
  private Cache currentCache;
  private boolean unresolvedCacheRef; // issue #676

  public MapperBuilderAssistant(Configuration configuration, String resource) {
    this.configuration = configuration;
    this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();

    ErrorContext.instance().resource(resource);
    this.resource = resource;
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
      .implementation(valueOrDefault(typeClass, PerpetualCache.class))
      .addDecorator(valueOrDefault(evictionClass, LruCache.class))
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

  public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
    id = applyCurrentNamespace(id, false);
    ParameterMap parameterMap = new ParameterMap.Builder(id, parameterClass, parameterMappings).build();
    configuration.addParameterMap(parameterMap);
    return parameterMap;
  }

  public ParameterMapping buildParameterMapping(Class<?> parameterType, String property, Class<?> javaType,
      JdbcType jdbcType, String resultMap, ParameterMode parameterMode, Class<? extends TypeHandler<?>> typeHandler,
      Integer numericScale) {
    resultMap = applyCurrentNamespace(resultMap, true);

    // Class parameterType = parameterMapBuilder.type();
    Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, jdbcType, typeHandler);
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

  public ResultMap addResultMap(String id, Class<?> type, String extend, Discriminator discriminator,
      List<ResultMapping> resultMappings, Boolean autoMapping) {
    id = applyCurrentNamespace(id, false);
    extend = applyCurrentNamespace(extend, true);

    if (extend != null) {
      if (!configuration.hasResultMap(extend)) {
        throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
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
    ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
        .discriminator(discriminator).build();
    configuration.addResultMap(resultMap);
    return resultMap;
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

  /**
   * Backward compatibility signature 'addMappedStatement'.
   *
   * @param id
   *          the id
   * @param sqlSource
   *          the sql source
   * @param statementType
   *          the statement type
   * @param sqlCommandType
   *          the sql command type
   * @param fetchSize
   *          the fetch size
   * @param timeout
   *          the timeout
   * @param parameterMap
   *          the parameter map
   * @param parameterType
   *          the parameter type
   * @param resultMap
   *          the result map
   * @param resultType
   *          the result type
   * @param resultSetType
   *          the result set type
   * @param flushCache
   *          the flush cache
   * @param useCache
   *          the use cache
   * @param resultOrdered
   *          the result ordered
   * @param keyGenerator
   *          the key generator
   * @param keyProperty
   *          the key property
   * @param keyColumn
   *          the key column
   * @param databaseId
   *          the database id
   * @param lang
   *          the lang
   *
   * @return the mapped statement
   */
  public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
      SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
      String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
      boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
      LanguageDriver lang, String resultSets) {
    return addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
        parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator,
        keyProperty, keyColumn, databaseId, lang, null, false, null);
  }

  public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
      SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
      String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
      boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
      LanguageDriver lang) {
    return addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
        parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator,
        keyProperty, keyColumn, databaseId, lang, null);
  }

  private <T> T valueOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
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
      parameterMap = new ParameterMap.Builder(statementId + "-Inline", parameterTypeClass, parameterMappings).build();
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
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(setterType.getKey(), jdbcType, typeHandler);
    List<ResultMapping> composites;
    if ((nestedSelect == null || nestedSelect.isEmpty()) && (foreignColumn == null || foreignColumn.isEmpty())) {
      composites = Collections.emptyList();
    } else {
      composites = parseCompositeColumnName(configuration, column);
    }
    // @formatter:off
    return new ResultMapping.Builder(configuration, property, column, setterType.getValue())
      .jdbcType(jdbcType)
      .nestedQueryId(applyCurrentNamespace(nestedSelect, true))
      .nestedResultMapId(applyCurrentNamespace(nestedResultMap, true))
      .resultSet(resultSet)
      .typeHandler(typeHandlerInstance).flags(flags == null ? new ArrayList<>() : flags)
      .composites(composites)
      .notNullColumns(parseMultipleColumnNames(notNullColumn))
      .columnPrefix(columnPrefix)
      .foreignColumn(foreignColumn)
      .lazy(lazy).build();
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

  // non-static utility method start

  @Nullable
  public Object resolveInstance(@Nullable String alias) {
    Class<?> clazz = resolveClass(alias);
    try {
      return clazz == null ? null : clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new BuilderException("Error creating instance. Cause: " + e, e);
    }
  }

  /**
   * @param config
   *          configuration
   * @param alias
   *          type alias
   * @param <T>
   *          the type of instance
   *
   * @return instance
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> T resolveInstance(@NotNull Configuration config, @Nullable String alias) {
    TypeAliasRegistry typeAliasRegistry = config.getTypeAliasRegistry();
    Class<?> clazz = typeAliasRegistry.resolveAlias(alias);
    try {
      return clazz == null ? null : (T) clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new BuilderException("Error creating instance. Cause: " + e, e);
    }
  }

  @Nullable
  public <T> Class<? extends T> resolveClass(@Nullable String alias) {
    try {
      return alias == null ? null : resolveAlias(alias);
    } catch (Exception e) {
      throw new BuilderException("Error resolving class. Cause: " + e, e);
    }
  }

  @Nullable
  public TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
    return resolveTypeHandler(null, javaType, null, typeHandlerAlias);
  }

  @Nullable
  public TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
    return resolveTypeHandler(javaType, null, typeHandlerType);
  }

  @Nullable
  public TypeHandler<?> resolveTypeHandler(Class<?> parameterType, Type propertyType, JdbcType jdbcType,
      String typeHandlerAlias) {
    Class<? extends TypeHandler<?>> typeHandlerType = null;
    typeHandlerType = resolveClass(typeHandlerAlias);
    if (typeHandlerType != null && !TypeHandler.class.isAssignableFrom(typeHandlerType)) {
      throw new BuilderException("Type " + typeHandlerType.getName()
          + " is not a valid TypeHandler because it does not implement TypeHandler interface");
    }
    return resolveTypeHandler(propertyType, jdbcType, typeHandlerType);
  }

  @Nullable
  public TypeHandler<?> resolveTypeHandler(Type javaType, JdbcType jdbcType,
      Class<? extends TypeHandler<?>> typeHandlerType) {
    if (typeHandlerType == null && jdbcType == null) {
      return null;
    }
    return configuration.getTypeHandlerRegistry().getTypeHandler(javaType, jdbcType, typeHandlerType);
  }

  @Nullable
  public <T> Class<? extends T> resolveAlias(String alias) {
    return typeAliasRegistry.resolveAlias(alias);
  }

  // non-static utility method end

  // Static Utility Method START

  public static Set<String> parseMultipleColumnNames(String columnName) {
    Set<String> columns = new HashSet<>();
    if (columnName != null) {
      if (columnName.indexOf(',') > -1) {
        StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
        while (parser.hasMoreTokens()) {
          String column = parser.nextToken();
          columns.add(column);
        }
      } else {
        columns.add(columnName);
      }
    }
    return columns;
  }

  public static List<ResultMapping> parseCompositeColumnName(Configuration configuration, String columnName) {
    List<ResultMapping> composites = new ArrayList<>();
    if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
      StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
      while (parser.hasMoreTokens()) {
        String property = parser.nextToken();
        String column = parser.nextToken();
        ResultMapping complexResultMapping = new ResultMapping.Builder(configuration, property, column,
            (TypeHandler<?>) null).build();
        composites.add(complexResultMapping);
      }
    }
    return composites;
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

  public static ParameterExpression parseParameterMapping(String content) {
    try {
      return new ParameterExpression(content);
    } catch (BuilderException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BuilderException("Parsing error was found in mapping @{" + content
          + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
    }
  }

  public static JdbcType resolveJdbcType(String alias) {
    try {
      return alias == null ? null : JdbcType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
    }
  }

  @Nullable
  public static <T> Class<? extends T> resolveClass(Configuration config, @Nullable String alias) {
    try {
      TypeAliasRegistry typeAliasRegistry = config.getTypeAliasRegistry();
      return alias == null ? null : typeAliasRegistry.resolveAlias(alias);
    } catch (Exception e) {
      throw new BuilderException("Error resolving class. Cause: " + e, e);
    }
  }

  @Nullable
  public static TypeHandler<?> resolveTypeHandler(@NotNull Configuration config, Class<?> parameterType,
      Type propertyType, JdbcType jdbcType, String typeHandlerAlias) {
    Class<? extends TypeHandler<?>> typeHandlerType;
    TypeAliasRegistry typeAliasRegistry = config.getTypeAliasRegistry();
    typeHandlerType = typeAliasRegistry.resolveAlias(typeHandlerAlias);
    if (typeHandlerType != null && !TypeHandler.class.isAssignableFrom(typeHandlerType)) {
      throw new BuilderException("Type " + typeHandlerType.getName()
          + " is not a valid TypeHandler because it does not implement TypeHandler interface");
    }
    if (typeHandlerType == null && jdbcType == null) {
      return null;
    }
    return config.getTypeHandlerRegistry().getTypeHandler(propertyType, jdbcType, typeHandlerType);
  }

  public static ResultSetType resolveResultSetType(String alias) {
    try {
      return alias == null ? null : ResultSetType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
    }
  }

  public static ParameterMode resolveParameterMode(String alias) {
    try {
      return alias == null ? null : ParameterMode.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
    }
  }

  @NotNull
  public static ParameterMapping buildParameterMapping(@NotNull Configuration config, String content,
      Class<?> parameterType) {
    ParameterExpression propertiesMap = parseParameterMapping(content);
    String property = propertiesMap.get("property");
    JdbcType jdbcType = resolveJdbcType(propertiesMap.get("jdbcType"));
    Class<?> propertyType;

    final TypeHandlerRegistry typeHandlerRegistry = config.getTypeHandlerRegistry();

    if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
      propertyType = parameterType;
    } else if (JdbcType.CURSOR.equals(jdbcType)) {
      propertyType = ResultSet.class;
    } else if (property != null) {
      MetaClass metaClass = MetaClass.forClass(parameterType, config.getReflectorFactory());
      if (metaClass.hasGetter(property)) {
        propertyType = metaClass.getGetterType(property);
      } else {
        propertyType = Object.class;
      }
    } else {
      propertyType = Object.class;
    }
    ParameterMapping.Builder builder = new ParameterMapping.Builder(property, propertyType);
    if (jdbcType != null) {
      builder.jdbcType(jdbcType);
    }
    Class<?> javaType = null;
    String typeHandlerAlias = null;
    for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
      String name = entry.getKey();
      String value = entry.getValue();
      if (name != null) {
        switch (name) {
          case "javaType":
            javaType = resolveClass(config, value);
            builder.javaType(javaType);
            break;
          case "mode":
            builder.mode(resolveParameterMode(value));
            break;
          case "numericScale":
            builder.numericScale(Integer.valueOf(value));
            break;
          case "resultMap":
            builder.resultMapId(value);
            break;
          case "typeHandler":
            typeHandlerAlias = value;
            break;
          case "jdbcTypeName":
            builder.jdbcTypeName(value);
            break;
          case "property":
            break;
          case "expression":
            builder.expression(value);
            break;
          default:
            throw new BuilderException("An invalid property '" + name + "' was found in mapping @{" + content
                + "}.  Valid properties are " + parameterProperties);
        }
      } else {
        throw new BuilderException("An invalid property '" + name + "' was found in mapping @{" + content
            + "}.  Valid properties are " + parameterProperties);
      }
    }
    if (typeHandlerAlias != null) {
      builder.typeHandler(resolveTypeHandler(config, javaType, propertyType, jdbcType, typeHandlerAlias));
    }
    return builder.build();
  }

  // Static Utility Method END
}

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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.builder.annotation.ClassMapperResource;
import org.apache.ibatis.builder.annotation.PackageMapperResource;
import org.apache.ibatis.builder.xml.URLMapperResource;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.builder.xml.XMLMapperResource;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.statement.JdbcUtils;
import org.apache.ibatis.internal.util.ClassUtils;
import org.apache.ibatis.internal.util.CollectionUtils;
import org.apache.ibatis.internal.util.ReflectionUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.jetbrains.annotations.Nullable;

/**
 * the entry point of mybatis config to build a {@link Configuration}
 * <a href="https://mybatis.org/mybatis-3/configuration.html">MyBatis Configuration</a>
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 *
 * @see Configuration
 */
public class XMLConfigBuilder extends BaseBuilder {

  private boolean parsed;
  private final XPathParser parser;
  private String environment;
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(Configuration.class, reader, environment, props);
  }

  public XMLConfigBuilder(Class<? extends Configuration> configClass, Reader reader, String environment,
      Properties props) {
    this(configClass, new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(Configuration.class, inputStream, environment, props);
  }

  public XMLConfigBuilder(Class<? extends Configuration> configClass, InputStream inputStream, String environment,
      Properties props) {
    this(configClass, new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  private XMLConfigBuilder(Class<? extends Configuration> configClass, XPathParser parser, String environment,
      Properties props) {
    super(newConfig(configClass));
    ErrorContext.instance().resource("SQL Mapper Configuration");
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }

  public Configuration parse() {
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  protected void parseConfiguration(XNode root) {
    try {
      // issue #117 read properties first
      Properties defaults = propertiesElement(root.evalNode("properties"));
      if (defaults != null) {
        Properties vars = configuration.getVariables();
        if (vars != null) {
          defaults.putAll(vars);
        }
        parser.setVariables(defaults);
        configuration.setVariables(defaults);
      }

      Properties settings = settingsAsProperties(root.evalNode("settings"));
      if (settings != null) {
        loadCustomVfsImpl(settings);
        loadCustomLogImpl(settings);
      }

      typeAliasesElement(root.evalNode("typeAliases"));
      pluginsElement(root.evalNode("plugins"));
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));

      // should be called after typeAliases has been processed
      if (settings != null) {
        settingsElement(configuration, settings);
      }

      // read it after objectFactory and objectWrapperFactory issue #631
      environmentsElement(root.evalNode("environments"));
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      typeHandlersElement(root.evalNode("typeHandlers"));

      // mappers
      List<MapperResource> resources = mappersElement(root.evalNode("mappers"));
      if (!CollectionUtils.isEmpty(resources)) {
        handleMapperResources(configuration, resources);
      }
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  protected void handleMapperResources(Configuration config, List<MapperResource> resources) throws Exception {
    List<MapperResource> unfinishedResources = new ArrayList<>();
    for (MapperResource resource : resources) {
      handleMapperResource(resource, config, unfinishedResources);
    }
    if (!unfinishedResources.isEmpty()) {
      for (MapperResource unfinishedResource : unfinishedResources) {
        handleMapperResource(unfinishedResource, config, unfinishedResources);
      }
    }
  }

  protected void handleMapperResource(MapperResource resource, Configuration config,
      List<MapperResource> unfinishedResources) throws Exception {
    resource.init(config.getVariables());
    if (resource.exists()) {
      ErrorContext.instance().resource(resource.getResourceName());
      boolean finished = resource.build(config);
      if (finished) {
        resource.cleanup(config);
      } else {
        unfinishedResources.add(resource);
      }
    }
  }

  protected Properties settingsAsProperties(XNode context) {
    if (context == null) {
      return new Properties();
    }
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException(
            "The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  protected void loadCustomVfsImpl(Properties props) throws ClassNotFoundException {
    for (String clazz : StringUtils.splitToArray(props.getProperty("vfsImpl"))) {
      if (!clazz.isEmpty()) {
        @SuppressWarnings("unchecked")
        Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
        configuration.setVfsImpl(vfsImpl);
      }
    }
  }

  private void loadCustomLogImpl(Properties props) {
    Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
    if (logImpl != null) {
      configuration.setLogImpl(logImpl);
    }
  }

  protected void typeAliasesElement(XNode context) {
    if (context == null) {
      return;
    }
    for (XNode child : context.getChildren()) {
      if ("package".equals(child.getName())) {
        String typeAliasPackage = child.getStringAttribute("name");
        configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
      } else {
        String alias = child.getStringAttribute("alias");
        String type = child.getStringAttribute("type");
        Class<?> clazz;
        try {
          clazz = resolveClass(type);
        } catch (Throwable e) {
          throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
        }
        if (clazz == null) {
          throw new BuilderException(
              "Error registering typeAlias for '" + alias + "'. Cause: resolved type of give alias is null");
        }
        if (alias == null) {
          typeAliasRegistry.registerAlias(clazz);
        } else {
          typeAliasRegistry.registerAlias(alias, clazz);
        }
      }
    }
  }

  protected void pluginsElement(XNode context) throws Exception {
    if (context != null) {
      for (XNode child : context.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();
        Interceptor interceptorInstance = createInstance(interceptor, Interceptor.class);
        if (interceptorInstance == null) {
          throw new BuilderException("failed to create Interceptor instance of type " + interceptor);
        }
        interceptorInstance.setProperties(properties);
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  protected void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectFactory factory = createInstance(type, ObjectFactory.class);
      if (factory != null) {
        Properties properties = context.getChildrenAsProperties();
        factory.setProperties(properties);
        configuration.setObjectFactory(factory);
      }
    }
  }

  protected void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectWrapperFactory factory = createInstance(type, ObjectWrapperFactory.class);
      if (factory == null) {
        throw new IllegalArgumentException("cannot create ObjectWrapperFactory");
      }
      configuration.setObjectWrapperFactory(factory);
    }
  }

  protected void reflectorFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ReflectorFactory factory = createInstance(type, ReflectorFactory.class);
      if (factory == null) {
        throw new IllegalArgumentException("cannot create ReflectorFactory");
      }
      configuration.setReflectorFactory(factory);
    }
  }

  @Nullable
  protected Properties propertiesElement(XNode context) throws Exception {
    if (context == null) {
      return null;
    }
    Properties defaults = context.getChildrenAsProperties();
    String resource = context.getStringAttribute("resource");
    String url = context.getStringAttribute("url");
    if (resource != null && url != null) {
      throw new BuilderException(
          "The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
    }
    if (resource != null) {
      defaults.putAll(Resources.getResourceAsProperties(resource));
    } else if (url != null) {
      defaults.putAll(Resources.getUrlAsProperties(url));
    }
    return defaults;
  }

  protected void settingsElement(Configuration configuration, Properties props) {
    configuration
        .setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    configuration.setAutoMappingUnknownColumnBehavior(
        AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
    configuration.setProxyFactory(createInstance(props.getProperty("proxyFactory"), ProxyFactory.class));
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
    configuration.setDefaultResultSetType(resolveResultSetType(props.getProperty("defaultResultSetType")));
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    configuration.setLazyLoadTriggerMethods(
        stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
    configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
    configuration.setShrinkWhitespacesInSql(booleanValueOf(props.getProperty("shrinkWhitespacesInSql"), false));
    configuration.setArgNameBasedConstructorAutoMapping(
        booleanValueOf(props.getProperty("argNameBasedConstructorAutoMapping"), false));
    configuration.setDefaultSqlProviderType(resolveClass(props.getProperty("defaultSqlProviderType")));
    configuration.setNullableOnForEach(booleanValueOf(props.getProperty("nullableOnForEach"), false));
    configuration.setDefaultPageSize(integerValueOf(props.getProperty("defaultPageSize"), 10));
  }

  protected void environmentsElement(XNode context) throws Exception {
    if (context == null) {
      return;
    }
    if (environment == null) {
      environment = context.getStringAttribute("default");
    }
    for (XNode child : context.getChildren()) {
      String id = child.getStringAttribute("id");
      if (isSpecifiedEnvironment(id)) {
        TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
        DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
        DataSource dataSource = dsFactory.getDataSource();

        String dialect = child.getStringAttribute("dialect");

        Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory)
            .dialect(JdbcUtils.dialect(dialect)).dataSource(dataSource);
        configuration.setEnvironment(environmentBuilder.build());
        break;
      }
    }
  }

  protected void databaseIdProviderElement(XNode context) throws Exception {
    if (context == null) {
      return;
    }
    String type = context.getStringAttribute("type");
    // awful patch to keep backward compatibility
    if ("VENDOR".equals(type)) {
      type = "DB_VENDOR";
    }
    Properties properties = context.getChildrenAsProperties();
    DatabaseIdProvider databaseIdProvider = createInstance(type, DatabaseIdProvider.class);
    if (databaseIdProvider != null) {
      databaseIdProvider.setProperties(properties);
      Environment environment = configuration.getEnvironment();
      if (environment != null) {
        String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
        configuration.setDatabaseId(databaseId);
      }
    }
  }

  protected TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      TransactionFactory factory = createInstance(type, TransactionFactory.class);
      if (factory == null) {
        throw new BuilderException("cannot create TransactionFactory instance");
      }
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  protected DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      if (StringUtils.isBlank(type)) {
        throw new BuilderException("cannot create DataSourceFactory instance, type is not set");
      }
      DataSourceFactory factory = createInstance(type, DataSourceFactory.class);
      if (factory == null) {
        throw new BuilderException("cannot create DataSourceFactory instance");
      }
      Properties props = context.getChildrenAsProperties();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  protected void typeHandlersElement(XNode context) {
    if (context == null) {
      return;
    }
    for (XNode child : context.getChildren()) {
      if ("package".equals(child.getName())) {
        String typeHandlerPackage = child.getStringAttribute("name");
        typeHandlerRegistry.register(typeHandlerPackage);
      } else {
        String javaTypeName = child.getStringAttribute("javaType");
        String jdbcTypeName = child.getStringAttribute("jdbcType");
        String handlerTypeName = child.getStringAttribute("handler");
        Class<?> javaTypeClass = resolveClass(javaTypeName);
        JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
        Class<?> typeHandlerClass = resolveClass(handlerTypeName);
        if (javaTypeClass != null) {
          if (jdbcType == null) {
            typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
          } else {
            typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
          }
        } else {
          typeHandlerRegistry.register(typeHandlerClass);
        }
      }
    }
  }

  protected List<MapperResource> mappersElement(XNode context) throws Exception {
    if (context == null) {
      return Collections.emptyList();
    }
    List<XNode> children = context.getChildren();
    if (CollectionUtils.isEmpty(children)) {
      return Collections.emptyList();
    }
    List<MapperResource> resources = new ArrayList<>(children.size());
    for (XNode child : children) {
      if ("package".equals(child.getName())) {
        String mapperPackage = child.getStringAttribute("name");
        resources.add(new PackageMapperResource(mapperPackage));
      } else {
        String resource = child.getStringAttribute("resource");
        String url = child.getStringAttribute("url");
        String mapperClass = child.getStringAttribute("class");
        if (resource != null && url == null && mapperClass == null) {
          resources.add(new XMLMapperResource(resource));
        } else if (resource == null && url != null && mapperClass == null) {
          resources.add(new URLMapperResource(url));
        } else if (resource == null && url == null && mapperClass != null) {
          resources.add(new ClassMapperResource(mapperClass));
        } else {
          throw new BuilderException(
              "A mapper element may only specify a url, resource or class, but not more than one.");
        }
      }
    }
    return resources;
  }

  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {
      throw new BuilderException("No environment specified.");
    }
    if (id == null) {
      throw new BuilderException("Environment requires an id attribute.");
    }
    return environment.equals(id);
  }

  private static Configuration newConfig(Class<? extends Configuration> configClass) {
    try {
      return ReflectionUtils.instantiateClass(configClass);
    } catch (Exception ex) {
      throw new BuilderException("Failed to create a new Configuration instance.", ex);
    }
  }

  @Override
  protected @Nullable JdbcType resolveJdbcType(String alias) {
    if (StringUtils.isNullOrBlank(alias)) {
      return null;
    }
    return super.resolveJdbcType(alias);
  }

  /**
   * if an alias has been set (not null), it should be created or throw exception
   *
   * @param alias
   *          alias
   * @param requiredType
   *          expected type
   * @param <T>
   *          type of object to create
   *
   * @return instance of specified type
   */
  @Override
  protected <T> @Nullable T createInstance(String alias, Class<T> requiredType) {
    T instance = super.createInstance(alias, requiredType);
    if (StringUtils.hasText(alias) && instance == null) {
      throw new BuilderException("cannot instance " + requiredType + " with alias " + alias);
    }
    return instance;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected @Nullable <T> Class<T> resolveAlias(@Nullable String alias) {
    if (StringUtils.isNullOrBlank(alias)) {
      return null;
    }
    Class<?> clazz = null;
    assert alias != null;
    if (alias.startsWith("[")) {
      clazz = ClassUtils.classForNameOrNull(alias);
    } else if (StringUtils.isLowerCase(alias) && StringUtils.isAlphabetic(alias)) {
      clazz = ClassUtils.classForNameOrNull(alias);
    }
    if (clazz == null) {
      clazz = super.resolveAlias(alias);
    }
    return (Class<T>) clazz;
  }

  public static Configuration withDefault() {
    String resource = "org/apache/ibatis/builder/xml/mybatis-config-defaults.xml";
    try (Reader reader = Resources.getResourceAsReader(resource)) {
      XMLConfigBuilder builder = new XMLConfigBuilder(reader, resource);
      return builder.parse();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

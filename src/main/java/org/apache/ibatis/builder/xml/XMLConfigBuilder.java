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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.builder.AutoMappingBehavior;
import org.apache.ibatis.builder.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.Configuration;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.internal.util.ObjectUtils;
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
import org.apache.ibatis.scripting.ResultSetType;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.jetbrains.annotations.Nullable;

/**
 * load core config file
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder {

  /**
   * the environment to load, nullable
   */
  @Nullable
  private String environment;

  @Nullable
  private ReflectorFactory localReflectorFactory;

  @Nullable
  private Properties properties;

  @Nullable
  private Class<? extends Configuration> configurationClass;

  public XMLConfigBuilder() {
  }

  public XMLConfigBuilder(@Nullable String environment, @Nullable Properties properties) {
    this.environment = environment;
    this.properties = properties;
  }

  public void setProperties(@Nullable Properties props) {
    this.properties = props;
  }

  public void setEnvironment(@Nullable String environment) {
    this.environment = environment;
  }

  public void setLocalReflectorFactory(@Nullable ReflectorFactory localReflectorFactory) {
    this.localReflectorFactory = localReflectorFactory;
  }

  public void setConfigurationClass(@Nullable Class<? extends Configuration> configurationClass) {
    this.configurationClass = configurationClass;
  }

  protected Configuration createConfiguration() {
    Class<? extends Configuration> configClass = this.configurationClass;
    if (configClass == null) {
      configClass = Configuration.class;
    }
    Configuration configuration = newConfig(configClass);
    if (this.properties != null) {
      configuration.setVariables(this.properties);
    }
    return configuration;
  }

  /**
   * parse specified xml config file with default settings
   *
   * @param file
   *          xml config file
   *
   * @return Configuration
   */
  public static Configuration load(File file) {
    XMLConfigBuilder builder = new XMLConfigBuilder();
    try (Reader reader = Files.newBufferedReader(file.toPath())) {
      return builder.parse(reader);
    } catch (IOException e) {
      throw new BuilderException("failed to parse mybatis config file, cause ", e);
    }
  }

  public Configuration parse(Reader reader) {
    ErrorContext.instance().resource("SQL Mapper Configuration");
    final Configuration config = createConfiguration();
    final XPathParser parser = new XPathParser(reader, true, this.properties, new XMLMapperEntityResolver());
    parseConfiguration(config, parser, parser.evalNode("/configuration"));
    return config;
  }

  public Configuration parse(InputStream inputStream) {
    try (Reader reader = new InputStreamReader(inputStream)) {
      return parse(reader);
    } catch (IOException e) {
      throw new BuilderException(e);
    }
  }

  private void parseConfiguration(Configuration config, XPathParser parser, XNode root) throws BuilderException {
    try {
      // issue #117 read properties first
      propertiesElement(config, parser, root.evalNode("properties"));
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfsImpl(config, settings);
      loadCustomLogImpl(config, settings);
      typeAliasesElement(config, root.evalNode("typeAliases"));
      pluginsElement(config, root.evalNode("plugins"));
      objectFactoryElement(config, root.evalNode("objectFactory"));
      objectWrapperFactoryElement(config, root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(config, root.evalNode("reflectorFactory"));
      settingsElement(config, settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      environmentsElement(config, root.evalNode("environments"));
      databaseIdProviderElement(config, root.evalNode("databaseIdProvider"));
      typeHandlersElement(config, root.evalNode("typeHandlers"));
      mappersElement(config, root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  private Properties settingsAsProperties(XNode context) {
    if (context == null) {
      return new Properties();
    }
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    ReflectorFactory reflectorFactory = this.localReflectorFactory;
    if (reflectorFactory == null) {
      reflectorFactory = new DefaultReflectorFactory();
    }
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, reflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException(
            "The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  private void loadCustomVfsImpl(Configuration configuration, Properties props) throws ClassNotFoundException {
    for (String clazz : StringUtils.splitToSet(props.getProperty("vfsImpl"), null)) {
      if (!clazz.isEmpty()) {
        @SuppressWarnings("unchecked")
        Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
        configuration.setVfsImpl(vfsImpl);
      }
    }
  }

  private void loadCustomLogImpl(Configuration configuration, Properties props) {
    Class<? extends Log> logImpl = configuration.resolveClass(props.getProperty("logImpl"));
    configuration.setLogImpl(logImpl);
  }

  private void typeAliasesElement(Configuration configuration, XNode context) {
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
        try {
          Class<?> clazz = Resources.classForName(type);
          if (alias == null) {
            configuration.getTypeAliasRegistry().registerAlias(clazz);
          } else {
            configuration.getTypeAliasRegistry().registerAlias(alias, clazz);
          }
        } catch (ClassNotFoundException e) {
          throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
        }
      }
    }
  }

  private void pluginsElement(Configuration configuration, XNode context) throws Exception {
    if (context != null) {
      for (XNode child : context.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();
        Interceptor interceptorInstance = (Interceptor) configuration.resolveClass(interceptor).getDeclaredConstructor()
            .newInstance();
        interceptorInstance.setProperties(properties);
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  private void objectFactoryElement(Configuration configuration, XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties properties = context.getChildrenAsProperties();
      ObjectFactory factory = (ObjectFactory) configuration.resolveClass(type).getDeclaredConstructor().newInstance();
      factory.setProperties(properties);
      configuration.setObjectFactory(factory);
    }
  }

  private void objectWrapperFactoryElement(Configuration configuration, XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectWrapperFactory factory = (ObjectWrapperFactory) configuration.resolveClass(type).getDeclaredConstructor()
          .newInstance();
      configuration.setObjectWrapperFactory(factory);
    }
  }

  private void reflectorFactoryElement(Configuration configuration, XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ReflectorFactory factory = (ReflectorFactory) configuration.resolveClass(type).getDeclaredConstructor()
          .newInstance();
      configuration.setReflectorFactory(factory);
    }
  }

  private void propertiesElement(Configuration configuration, XPathParser parser, XNode context) throws Exception {
    if (context == null) {
      return;
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
    Properties vars = configuration.getVariables();
    if (vars != null) {
      defaults.putAll(vars);
    }
    parser.setVariables(defaults);
    configuration.setVariables(defaults);
  }

  private void settingsElement(Configuration configuration, Properties props) {
    configuration
        .setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    configuration.setAutoMappingUnknownColumnBehavior(
        AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    configuration.setCacheEnabled(ObjectUtils.parseBoolean(props.getProperty("cacheEnabled"), true));
    configuration.setProxyFactory((ProxyFactory) configuration.createInstance(props.getProperty("proxyFactory")));
    configuration.setLazyLoadingEnabled(ObjectUtils.parseBoolean(props.getProperty("lazyLoadingEnabled"), false));
    configuration.setAggressiveLazyLoading(ObjectUtils.parseBoolean(props.getProperty("aggressiveLazyLoading"), false));
    configuration.setUseColumnLabel(ObjectUtils.parseBoolean(props.getProperty("useColumnLabel"), true));
    configuration.setUseGeneratedKeys(ObjectUtils.parseBoolean(props.getProperty("useGeneratedKeys"), false));
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    configuration
        .setDefaultStatementTimeout(ObjectUtils.parseInteger(props.getProperty("defaultStatementTimeout"), null));
    configuration.setDefaultFetchSize(ObjectUtils.parseInteger(props.getProperty("defaultFetchSize"), null));

    try {
      String defaultResultSetType = props.getProperty("defaultResultSetType");
      if (defaultResultSetType != null) {
        ResultSetType resultSetType = ResultSetType.valueOf(defaultResultSetType);
        configuration.setDefaultResultSetType(resultSetType);
      }
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
    }

    configuration
        .setMapUnderscoreToCamelCase(ObjectUtils.parseBoolean(props.getProperty("mapUnderscoreToCamelCase"), false));
    configuration.setSafeRowBoundsEnabled(ObjectUtils.parseBoolean(props.getProperty("safeRowBoundsEnabled"), false));
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    configuration.setLazyLoadTriggerMethods(
        StringUtils.splitToSet(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    configuration
        .setSafeResultHandlerEnabled(ObjectUtils.parseBoolean(props.getProperty("safeResultHandlerEnabled"), true));
    configuration
        .setDefaultScriptingLanguage(configuration.resolveClass(props.getProperty("defaultScriptingLanguage")));
    configuration.setDefaultEnumTypeHandler(configuration.resolveClass(props.getProperty("defaultEnumTypeHandler")));
    configuration.setCallSettersOnNulls(ObjectUtils.parseBoolean(props.getProperty("callSettersOnNulls"), false));
    configuration.setUseActualParamName(ObjectUtils.parseBoolean(props.getProperty("useActualParamName"), true));
    configuration
        .setReturnInstanceForEmptyRow(ObjectUtils.parseBoolean(props.getProperty("returnInstanceForEmptyRow"), false));
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    configuration.setConfigurationFactory(configuration.resolveClass(props.getProperty("configurationFactory")));
    configuration
        .setShrinkWhitespacesInSql(ObjectUtils.parseBoolean(props.getProperty("shrinkWhitespacesInSql"), false));
    configuration.setArgNameBasedConstructorAutoMapping(
        ObjectUtils.parseBoolean(props.getProperty("argNameBasedConstructorAutoMapping"), false));
    configuration.setDefaultSqlProviderType(configuration.resolveClass(props.getProperty("defaultSqlProviderType")));
    configuration.setNullableOnForEach(ObjectUtils.parseBoolean(props.getProperty("nullableOnForEach"), false));
  }

  private void environmentsElement(Configuration configuration, XNode context) throws Exception {
    if (context == null) {
      return;
    }
    if (environment == null) {
      environment = context.getStringAttribute("default");
    }
    for (XNode child : context.getChildren()) {
      String id = child.getStringAttribute("id");
      if (isSpecifiedEnvironment(id)) {
        TransactionFactory txFactory = transactionManagerElement(configuration, child.evalNode("transactionManager"));
        DataSourceFactory dsFactory = dataSourceElement(configuration, child.evalNode("dataSource"));
        DataSource dataSource = dsFactory.getDataSource();
        configuration.setEnvironment(new Environment(id, txFactory, dataSource));
        break;
      }
    }
  }

  private void databaseIdProviderElement(Configuration configuration, XNode context) throws Exception {
    if (context == null) {
      return;
    }
    String type = context.getStringAttribute("type");
    // awful patch to keep backward compatibility
    if ("VENDOR".equals(type)) {
      type = "DB_VENDOR";
    }
    Properties properties = context.getChildrenAsProperties();
    DatabaseIdProvider databaseIdProvider = (DatabaseIdProvider) configuration.resolveClass(type)
        .getDeclaredConstructor().newInstance();
    databaseIdProvider.setProperties(properties);
    Environment environment = configuration.getEnvironment();
    if (environment != null) {
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      configuration.setDatabaseId(databaseId);
    }
  }

  private TransactionFactory transactionManagerElement(Configuration configuration, XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      TransactionFactory factory = (TransactionFactory) configuration.resolveClass(type).getDeclaredConstructor()
          .newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  private DataSourceFactory dataSourceElement(Configuration configuration, XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      DataSourceFactory factory = (DataSourceFactory) configuration.resolveClass(type).getDeclaredConstructor()
          .newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  private void typeHandlersElement(Configuration configuration, XNode context) {
    if (context == null) {
      return;
    }
    for (XNode child : context.getChildren()) {
      if ("package".equals(child.getName())) {
        String typeHandlerPackage = child.getStringAttribute("name");
        configuration.getTypeHandlerRegistry().register(typeHandlerPackage);
      } else {
        String javaTypeName = child.getStringAttribute("javaType");
        String jdbcTypeName = child.getStringAttribute("jdbcType");
        String handlerTypeName = child.getStringAttribute("handler");
        Class<?> javaTypeClass = configuration.resolveClass(javaTypeName);
        JdbcType jdbcType = JdbcType.forName(jdbcTypeName, true);
        Class<?> typeHandlerClass = configuration.resolveClass(handlerTypeName);
        if (javaTypeClass != null) {
          if (jdbcType == null) {
            configuration.getTypeHandlerRegistry().register(javaTypeClass, typeHandlerClass);
          } else {
            configuration.getTypeHandlerRegistry().register(javaTypeClass, jdbcType, typeHandlerClass);
          }
        } else {
          configuration.getTypeHandlerRegistry().register(typeHandlerClass);
        }
      }
    }
  }

  private void mappersElement(Configuration configuration, XNode context) throws Exception {
    if (context == null) {
      return;
    }
    for (XNode child : context.getChildren()) {
      if ("package".equals(child.getName())) {
        String mapperPackage = child.getStringAttribute("name");
        configuration.addMappers(mapperPackage);
      } else {
        String resource = child.getStringAttribute("resource");
        String url = child.getStringAttribute("url");
        String mapperClass = child.getStringAttribute("class");
        if (resource != null && url == null && mapperClass == null) {
          ErrorContext.instance().resource(resource);
          try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource,
                configuration.getSqlFragments());
            mapperParser.parse();
          }
        } else if (resource == null && url != null && mapperClass == null) {
          ErrorContext.instance().resource(url);
          try (InputStream inputStream = Resources.getUrlAsStream(url)) {
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url,
                configuration.getSqlFragments());
            mapperParser.parse();
          }
        } else if (resource == null && url == null && mapperClass != null) {
          Class<?> mapperInterface = Resources.classForName(mapperClass);
          configuration.addMapper(mapperInterface);
        } else {
          throw new BuilderException(
              "A mapper element may only specify a url, resource or class, but not more than one.");
        }
      }
    }
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
      return configClass.getDeclaredConstructor().newInstance();
    } catch (Exception ex) {
      throw new BuilderException("Failed to create a new Configuration instance.", ex);
    }
  }

}

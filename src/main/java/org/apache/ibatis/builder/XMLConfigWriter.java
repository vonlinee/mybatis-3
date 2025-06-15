package org.apache.ibatis.builder;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ibatis.mapping.MappedStatement;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLConfigWriter {

  private final Configuration configuration;

  public XMLConfigWriter(Configuration configuration) {
    this.configuration = configuration;
  }

  private Document convertToXmlDocument(Configuration configuration) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.newDocument();

    Comment comment = document.createComment("\n" + "       Copyright 2009-2025 the original author or authors.\n"
        + "\n" + "       Licensed under the Apache License, Version 2.0 (the \"License\");\n"
        + "       you may not use this file except in compliance with the License.\n"
        + "       You may obtain a copy of the License at\n" + "\n"
        + "          https://www.apache.org/licenses/LICENSE-2.0\n" + "\n"
        + "       Unless required by applicable law or agreed to in writing, software\n"
        + "       distributed under the License is distributed on an \"AS IS\" BASIS,\n"
        + "       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
        + "       See the License for the specific language governing permissions and\n"
        + "       limitations under the License.\n");
    document.appendChild(comment);

    // <configuration/>
    Element rootElement = document.createElement("configuration");

    document.appendChild(rootElement);

    // <properties/>
    addPropertiesIfExists(document, rootElement, configuration.getVariables());

    // settings
    addSettings(document, rootElement, configuration);

    // typeAliases
    addTypeAliases(document, rootElement, configuration);

    // plugins
    addPlugins(document, rootElement, configuration);

    // objectFactory
    addObjectFactory(document, rootElement, configuration);

    // objectWrapperFactory
    addObjectWrapperFactory(document, rootElement, configuration);

    // reflectorFactory
    addReflectorFactory(document, rootElement, configuration);

    // environments
    addEnvironments(document, rootElement, configuration);

    // databaseIdProvider
    addDatabaseIdProvider(document, rootElement, configuration);

    // typeHandlers
    addTypeHandlersElement(document, rootElement, configuration);

    // mappers
    addMappers(document, rootElement, configuration);
    return document;
  }

  protected void addPropertiesIfExists(Document document, Element parent, Properties properties) {
    if (properties != null && !properties.isEmpty()) {
      Element propertiesElement = document.createElement("properties");

      properties.forEach((key, value) -> {
        Element propertyElement = document.createElement("property");
        propertyElement.setAttribute("name", key.toString());
        propertyElement.setAttribute("value", value.toString());
        propertiesElement.appendChild(propertyElement);
      });

      parent.appendChild(propertiesElement);
    }
  }

  protected void addSettings(Document document, Element parent, Configuration configuration) {
    Element settingsElement = document.createElement("settings");

    // add settings
    addSettingIfNotNull(document, settingsElement, "cacheEnabled", configuration.isCacheEnabled());
    addSettingIfNotNull(document, settingsElement, "lazyLoadingEnabled", configuration.isLazyLoadingEnabled());
    addSettingIfNotNull(document, settingsElement, "aggressiveLazyLoading", configuration.isAggressiveLazyLoading());
    addSettingIfNotNull(document, settingsElement, "useColumnLabel", configuration.isUseColumnLabel());
    addSettingIfNotNull(document, settingsElement, "useGeneratedKeys", configuration.isUseGeneratedKeys());
    addSettingIfNotNull(document, settingsElement, "autoMappingBehavior",
        configuration.getAutoMappingBehavior().name());
    addSettingIfNotNull(document, settingsElement, "autoMappingUnknownColumnBehavior",
        configuration.getAutoMappingUnknownColumnBehavior().name());
    addSettingIfNotNull(document, settingsElement, "defaultExecutorType",
        configuration.getDefaultExecutorType().name());
    addSettingIfNotNull(document, settingsElement, "defaultStatementTimeout",
        configuration.getDefaultStatementTimeout());
    addSettingIfNotNull(document, settingsElement, "defaultFetchSize", configuration.getDefaultFetchSize());
    addSettingIfNotNull(document, settingsElement, "defaultResultSetType",
        configuration.getDefaultResultSetType() != null ? configuration.getDefaultResultSetType().name() : null);
    addSettingIfNotNull(document, settingsElement, "mapUnderscoreToCamelCase",
        configuration.isMapUnderscoreToCamelCase());
    addSettingIfNotNull(document, settingsElement, "safeRowBoundsEnabled", configuration.isSafeRowBoundsEnabled());
    addSettingIfNotNull(document, settingsElement, "safeResultHandlerEnabled",
        configuration.isSafeResultHandlerEnabled());
    addSettingIfNotNull(document, settingsElement, "localCacheScope", configuration.getLocalCacheScope().name());
    addSettingIfNotNull(document, settingsElement, "jdbcTypeForNull", configuration.getJdbcTypeForNull().name());
    addSettingIfNotNull(document, settingsElement, "lazyLoadTriggerMethods",
        String.join(",", configuration.getLazyLoadTriggerMethods()));
    addSettingIfNotNull(document, settingsElement, "defaultScriptingLanguage",
        configuration.getDefaultScriptingLanguageInstance().getClass().getName());
    addSettingIfNotNull(document, settingsElement, "callSettersOnNulls", configuration.isCallSettersOnNulls());
    addSettingIfNotNull(document, settingsElement, "logPrefix", configuration.getLogPrefix());
    addSettingIfNotNull(document, settingsElement, "logImpl",
        configuration.getLogImpl() != null ? configuration.getLogImpl().getName() : null);
    addSettingIfNotNull(document, settingsElement, "proxyFactory",
        configuration.getProxyFactory().getClass().getName());
    addSettingIfNotNull(document, settingsElement, "vfsImpl",
        configuration.getVfsImpl() != null ? configuration.getVfsImpl().getName() : null);

    if (settingsElement.hasChildNodes()) {
      parent.appendChild(settingsElement);
    }
  }

  protected void addSettingIfNotNull(Document document, Element settingsElement, String name, Object value) {
    if (value != null) {
      Element settingElement = document.createElement("setting");
      settingElement.setAttribute("name", name);
      settingElement.setAttribute("value", value.toString());
      settingsElement.appendChild(settingElement);
    }
  }

  protected void addTypeAliases(Document document, Element parent, Configuration configuration) {
    if (!configuration.getTypeAliasRegistry().getTypeAliases().isEmpty()) {
      Element typeAliasesElement = document.createElement("typeAliases");

      configuration.getTypeAliasRegistry().getTypeAliases().forEach((alias, type) -> {
        Element typeAliasElement = document.createElement("typeAlias");
        typeAliasElement.setAttribute("type", type.getName());
        typeAliasElement.setAttribute("alias", alias);
        typeAliasesElement.appendChild(typeAliasElement);
      });

      parent.appendChild(typeAliasesElement);
    }
  }

  private void addPlugins(Document document, Element parent, Configuration configuration) {
    if (!configuration.getInterceptors().isEmpty()) {
      Element pluginsElement = document.createElement("plugins");

      configuration.getInterceptors().forEach(interceptor -> {
        Element pluginElement = document.createElement("plugin");
        pluginElement.setAttribute("interceptor", interceptor.getClass().getName());
        pluginsElement.appendChild(pluginElement);
      });

      parent.appendChild(pluginsElement);
    }
  }

  private void addObjectFactory(Document document, Element parent, Configuration configuration) {
    if (configuration.getObjectFactory() != null && !configuration.getObjectFactory().getClass().getName()
        .equals("org.apache.ibatis.reflection.factory.DefaultObjectFactory")) {
      Element objectFactoryElement = document.createElement("objectFactory");
      objectFactoryElement.setAttribute("type", configuration.getObjectFactory().getClass().getName());
      parent.appendChild(objectFactoryElement);
    }
  }

  private void addObjectWrapperFactory(Document document, Element parent, Configuration configuration) {
    if (configuration.getObjectWrapperFactory() != null && !configuration.getObjectWrapperFactory().getClass().getName()
        .equals("org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory")) {
      Element objectWrapperFactoryElement = document.createElement("objectWrapperFactory");
      objectWrapperFactoryElement.setAttribute("type", configuration.getObjectWrapperFactory().getClass().getName());
      parent.appendChild(objectWrapperFactoryElement);
    }
  }

  private void addReflectorFactory(Document document, Element parent, Configuration configuration) {
    if (configuration.getReflectorFactory() != null && !configuration.getReflectorFactory().getClass().getName()
        .equals("org.apache.ibatis.reflection.DefaultReflectorFactory")) {
      Element reflectorFactoryElement = document.createElement("reflectorFactory");
      reflectorFactoryElement.setAttribute("type", configuration.getReflectorFactory().getClass().getName());
      parent.appendChild(reflectorFactoryElement);
    }
  }

  private void addEnvironments(Document document, Element parent, Configuration configuration) {
    if (configuration.getEnvironment() != null) {
      Element environmentsElement = document.createElement("environments");
      environmentsElement.setAttribute("default", configuration.getEnvironment().getId());

      Element environmentElement = document.createElement("environment");
      environmentElement.setAttribute("id", configuration.getEnvironment().getId());

      // transactionManager
      Element transactionManagerElement = document.createElement("transactionManager");
      transactionManagerElement.setAttribute("type",
          configuration.getEnvironment().getTransactionFactory().getClass().getName());
      environmentElement.appendChild(transactionManagerElement);

      // dataSource
      Element dataSourceElement = document.createElement("dataSource");
      dataSourceElement.setAttribute("type", "");

      // dataSource
      Properties dataSourceProperties = configuration.getVariables();
      if (dataSourceProperties != null) {
        dataSourceProperties.forEach((key, value) -> {
          Element propertyElement = document.createElement("property");
          propertyElement.setAttribute("name", key.toString());
          propertyElement.setAttribute("value", value.toString());
          dataSourceElement.appendChild(propertyElement);
        });
      }

      environmentElement.appendChild(dataSourceElement);
      environmentsElement.appendChild(environmentElement);
      parent.appendChild(environmentsElement);
    }
  }

  private void addDatabaseIdProvider(Document document, Element parent, Configuration configuration) {
    if (configuration.getDatabaseId() != null) {
      Element databaseIdProviderElement = document.createElement("databaseIdProvider");
      databaseIdProviderElement.setAttribute("type", "");
      parent.appendChild(databaseIdProviderElement);
    }
  }

  private void addTypeHandlersElement(Document document, Element parent, Configuration configuration) {
    if (!configuration.getTypeHandlerRegistry().getTypeHandlers().isEmpty()) {
      Element typeHandlersElement = document.createElement("typeHandlers");

      // if (!configuration.getTypeHandlerRegistry().getTypeHandlerMap().isEmpty()) {
      // configuration.getTypeHandlerRegistry().getTypeHandlerMap().keySet().stream().map(Class::getName).distinct().forEach(pkg
      // -> {
      // Element packageElement = document.createElement("package");
      // packageElement.setAttribute("name", pkg);
      // typeHandlersElement.appendChild(packageElement);
      // });
      // }

      configuration.getTypeHandlerRegistry().getTypeHandlers().forEach(handler -> {
        Element typeHandlerElement = document.createElement("typeHandler");
        typeHandlerElement.setAttribute("handler", handler.getClass().getName());

        typeHandlersElement.appendChild(typeHandlerElement);
      });

      parent.appendChild(typeHandlersElement);
    }
  }

  private void addMappers(Document document, Element parent, Configuration configuration) {
    if (!configuration.getMappedStatements().isEmpty()) {
      Element mappersElement = document.createElement("mappers");
      if (!configuration.getMapperRegistry().getMappers().isEmpty()) {
        configuration.getMapperRegistry().getMappers().stream().map(mapper -> mapper.getPackage().getName()).distinct()
            .forEach(pkg -> {
              Element packageElement = document.createElement("package");
              packageElement.setAttribute("name", pkg);
              mappersElement.appendChild(packageElement);
            });
      }

      configuration.getMapperRegistry().getMappers().forEach(mapper -> {
        Element mapperElement = document.createElement("mapper");
        mapperElement.setAttribute("class", mapper.getName());
        mappersElement.appendChild(mapperElement);
      });

      configuration.getMappedStatements().stream().map(MappedStatement::getResource).filter(Objects::nonNull).distinct()
          .forEach(resource -> {
            Element mapperElement = document.createElement("mapper");
            mapperElement.setAttribute("resource", resource);
            mappersElement.appendChild(mapperElement);
          });

      parent.appendChild(mappersElement);
    }
  }

  protected Transformer createTransformer() throws TransformerConfigurationException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    return transformer;
  }

  public void writeToFile(String path) throws Exception {
    writeTo(Files.newBufferedWriter(Paths.get(path)));
  }

  public void writeTo(Writer writer) throws Exception {
    Document document = convertToXmlDocument(this.configuration);
    Transformer transformer = createTransformer();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
  }
}

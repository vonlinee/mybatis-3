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

import static com.googlecode.catchexception.apis.BDDCatchException.caughtException;
import static com.googlecode.catchexception.apis.BDDCatchException.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.InputStream;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.internal.util.ObjectUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.scripting.MappedStatement;
import org.apache.ibatis.scripting.ResultSetType;
import org.apache.ibatis.scripting.StatementType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class XmlMapperBuilderTest {

  @Test
  void shouldSuccessfullyLoadXMLMapperFile() {
    assertDoesNotThrow(() -> {
      Configuration configuration = new Configuration();
      String resource = "org/apache/ibatis/builder/AuthorMapper.xml";
      try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
        XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, resource,
            configuration.getSqlFragments());
        builder.parse();
      }
    });
  }

  @Test
  void mappedStatementWithOptions() throws Exception {
    Configuration configuration = new Configuration();
    String resource = "org/apache/ibatis/builder/AuthorMapper.xml";
    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
      XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, resource,
          configuration.getSqlFragments());
      builder.parse();

      MappedStatement mappedStatement = configuration.getMappedStatement("selectWithOptions");
      assertThat(mappedStatement.getFetchSize()).isEqualTo(200);
      assertThat(mappedStatement.getTimeout()).isEqualTo(10);
      assertThat(mappedStatement.getStatementType()).isEqualTo(StatementType.PREPARED);
      assertThat(mappedStatement.getResultSetType()).isEqualTo(ResultSetType.SCROLL_SENSITIVE);
      assertThat(mappedStatement.isFlushCacheRequired()).isFalse();
      assertThat(mappedStatement.isUseCache()).isFalse();
    }
  }

  @Test
  void mappedStatementWithoutOptionsWhenSpecifyDefaultValue() throws Exception {
    Configuration configuration = new Configuration();
    configuration.setDefaultResultSetType(ResultSetType.SCROLL_INSENSITIVE);
    String resource = "org/apache/ibatis/builder/AuthorMapper.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, resource,
        configuration.getSqlFragments());
    builder.parse();
    inputStream.close();

    MappedStatement mappedStatement = configuration.getMappedStatement("selectAuthor");
    assertThat(mappedStatement.getResultSetType()).isEqualTo(ResultSetType.SCROLL_INSENSITIVE);
  }

  @Test
  void parseExpression() {
    {
      Pattern pattern = ObjectUtils.parseExpression("[0-9]", "[a-z]");
      assertThat(pattern.matcher("0").find()).isTrue();
      assertThat(pattern.matcher("a").find()).isFalse();
    }
    {
      Pattern pattern = ObjectUtils.parseExpression(null, "[a-z]");
      assertThat(pattern.matcher("0").find()).isFalse();
      assertThat(pattern.matcher("a").find()).isTrue();
    }
  }

  @Test
  void resolveJdbcTypeWithUndefinedValue() {
    Configuration configuration = new Configuration();
    when(() -> configuration.resolveJdbcType("aaa"));
    then(caughtException()).isInstanceOf(BuilderException.class)
        .hasMessageStartingWith("Error resolving JdbcType. Cause: java.lang.IllegalArgumentException: No enum")
        .hasMessageEndingWith("org.apache.ibatis.type.JdbcType.aaa");
  }

  @Test
  void resolveResultSetTypeWithUndefinedValue() {
    Configuration configuration = new Configuration();
    when(() -> configuration.resolveResultSetType("bbb"));
    then(caughtException()).isInstanceOf(BuilderException.class)
        .hasMessageStartingWith("Error resolving ResultSetType. Cause: java.lang.IllegalArgumentException: No enum")
        .hasMessageEndingWith("org.apache.ibatis.scripting.ResultSetType.bbb");
  }

  @Test
  void resolveParameterModeWithUndefinedValue() {
    Configuration configuration = new Configuration();
    when(() -> configuration.resolveParameterMode("ccc"));
    then(caughtException()).isInstanceOf(BuilderException.class)
        .hasMessageStartingWith("Error resolving ParameterMode. Cause: java.lang.IllegalArgumentException: No enum")
        .hasMessageEndingWith("org.apache.ibatis.mapping.ParameterMode.ccc");
  }

  @Test
  void resolveClassWithNotFound() {
    Configuration configuration = new Configuration();
    when(() -> configuration.resolveClass("ddd"));
    then(caughtException()).isInstanceOf(BuilderException.class).hasMessage(
        "Error resolving class. Cause: org.apache.ibatis.type.TypeException: Could not resolve type alias 'ddd'.  Cause: java.lang.ClassNotFoundException: Cannot find class: ddd");
  }

  @Test
  void resolveTypeHandlerTypeHandlerAliasIsNull() {
    Configuration configuration = new Configuration();
    TypeHandler<?> typeHandler = configuration.resolveTypeHandler(String.class, null, null, null);
    assertThat(typeHandler).isNull();
  }

  @Test
  void resolveTypeHandlerNoAssignable() {
    Configuration configuration = new Configuration();
    when(() -> configuration.resolveTypeHandler(String.class, null, null, "integer"));
    then(caughtException()).isInstanceOf(BuilderException.class).hasMessage(
        "Type java.lang.Integer is not a valid TypeHandler because it does not implement TypeHandler interface");
  }

  @Test
  void setCurrentNamespaceValueIsNull() {
    BuilderAssistant builder = new BuilderAssistant(new Configuration());
    builder.setCurrentLoadedResource("resource");
    when(() -> builder.setCurrentNamespace(null));
    then(caughtException()).isInstanceOf(BuilderException.class)
        .hasMessage("The mapper element requires a namespace attribute to be specified.");
  }

  @Test
  void useCacheRefNamespaceIsNull() {
    BuilderAssistant builder = new BuilderAssistant(new Configuration());
    builder.setCurrentLoadedResource("resource");
    when(() -> builder.useCacheRef(null));
    then(caughtException()).isInstanceOf(BuilderException.class)
        .hasMessage("cache-ref element requires a namespace attribute.");
  }

  @Test
  void useCacheRefNamespaceIsUndefined() {
    BuilderAssistant builder = new BuilderAssistant(new Configuration());
    builder.setCurrentLoadedResource("resource");
    when(() -> builder.useCacheRef("eee"));
    then(caughtException()).hasMessage("No cache for namespace 'eee' could be found.");
  }

  @Test
  void shouldFailedLoadXMLMapperFile() throws Exception {
    Configuration configuration = new Configuration();
    String resource = "org/apache/ibatis/builder/ProblemMapper.xml";
    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
      XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, resource,
          configuration.getSqlFragments());
      Exception exception = Assertions.assertThrows(BuilderException.class, builder::parse);
      Assertions.assertTrue(exception.getMessage()
          .contains("Error parsing Mapper XML. The XML location is 'org/apache/ibatis/builder/ProblemMapper.xml'"));
    }
  }

  @Test
  void parseMultipleColumnNames() {
    Set<String> columns = BuilderAssistant.parseMultipleColumnNames("{prop1=col1,prop2=col2}");
    Assertions.assertEquals(2, columns.size());
  }

  @Test
  public void shouldNotLoadTheSameNamespaceFromTwoResourcesWithDifferentNames() throws Exception {
    Configuration configuration = new Configuration();
    String resource = "org/apache/ibatis/builder/AuthorMapper.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, "name1",
        configuration.getSqlFragments());
    builder.parse();
    InputStream inputStream2 = Resources.getResourceAsStream(resource);
    XMLMapperBuilder builder2 = new XMLMapperBuilder(inputStream2, configuration, "name2",
        configuration.getSqlFragments());

    when(builder2::parse);
    then(caughtException()).hasMessageContaining(
        "Error parsing Mapper XML. The XML location is 'name2'. Cause: java.lang.IllegalArgumentException: Parameter Maps collection already contains key ");
  }

  @Test
  void errorResultMapLocation() throws Exception {
    Configuration configuration = new Configuration();
    String resource = "org/apache/ibatis/builder/ProblemResultMapper.xml";
    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
      XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, resource,
          configuration.getSqlFragments());
      builder.parse();
      String resultMapName = "java.lang.String";
      // namespace + "." + id
      String statementId = "org.mybatis.spring.ErrorProblemMapper.findProblemResultMapTest";
      // same as MapperBuilderAssistant.getStatementResultMaps Exception message
      String message = "Could not find result map '" + resultMapName + "' referenced from '" + statementId + "'";
      IncompleteElementException exception = Assertions.assertThrows(IncompleteElementException.class,
          () -> configuration.getMappedStatement("findProblemTypeTest"));
      assertThat(exception.getMessage()).isEqualTo(message);
    }
  }
}

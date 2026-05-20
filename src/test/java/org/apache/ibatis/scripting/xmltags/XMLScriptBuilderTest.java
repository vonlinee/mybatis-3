/*
 *    Copyright 2009-2026 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class XMLScriptBuilderTest {

  @Test
  void shouldWhereInsertWhitespace() throws Exception {
    String xml = """
        <script>
        select * from user
        <where><if test="1==1">and id = 1</if><if test="1==1">and id > 0</if></where>
        <if test="1==1">and id = 1</if><if test="1==1">and id > 0</if>
        </script>
        """;

    Configuration configuration = new Configuration();
    SqlSource sqlSource = new XMLScriptBuilder(configuration, new XPathParser(xml).evalNode("/script"))
        .parseScriptNode();
    assertThat(sqlSource.getBoundSql(configuration, 1).getSql()).containsPattern(
        "(?m)^\\s*select \\* from user\\s+WHERE\\s+id = 1\\s+and id > 0\\s+and id = 1\\s+and id > 0\\s*$");
  }

  @Test
  void shouldThrowIfUnknownElementFound() {
    String xml = """
        <script>
        select * from user
        <choose>
        <when test="1==1">and id = 1</when>
        <otherwize>and id > 0</otherwize>
        </choose>
        </script>
        """;
    XMLScriptBuilder parser = new XMLScriptBuilder(new Configuration(), new XPathParser(xml).evalNode("/script"));
    assertThatThrownBy(parser::parseScriptNode).isInstanceOf(BuilderException.class)
        .hasMessage("Unknown element <otherwize> in SQL statement.");
  }

  @Test
  void shouldFormatInTagWithDefaultItem() throws Exception {
    String xml = """
        <script>
        SELECT * FROM users
        WHERE id <in collection="ids"/>
        </script>
        """;

    Configuration configuration = new Configuration();
    SqlSource sqlSource = new XMLScriptBuilder(configuration, new XPathParser(xml).evalNode("/script"))
        .parseScriptNode();
    java.util.Map<String, Object> params = new java.util.HashMap<>();
    params.put("ids", java.util.Arrays.asList(1, 2, 3));
    assertThat(sqlSource.getBoundSql(configuration, params).getSql()).containsPattern(
        "(?m)^\\s*SELECT \\* FROM users\\s+WHERE id\\s+IN\\s*\\(\\s*\\?\\s*,\\s*\\?\\s*,\\s*\\?\\s*\\)\\s*$");
  }

  @Test
  void shouldFormatInTagWithBody() throws Exception {
    String xml = """
        <script>
        SELECT * FROM users
        WHERE id <in collection="ids" item="id">#{id}</in>
        </script>
        """;

    Configuration configuration = new Configuration();
    SqlSource sqlSource = new XMLScriptBuilder(configuration, new XPathParser(xml).evalNode("/script"))
        .parseScriptNode();
    java.util.Map<String, Object> params = new java.util.HashMap<>();
    params.put("ids", java.util.Arrays.asList(1, 2, 3));
    assertThat(sqlSource.getBoundSql(configuration, params).getSql()).containsPattern(
        "(?m)^\\s*SELECT \\* FROM users\\s+WHERE id\\s+IN\\s*\\(\\s*\\?\\s*,\\s*\\?\\s*,\\s*\\?\\s*\\)\\s*$");
  }
}

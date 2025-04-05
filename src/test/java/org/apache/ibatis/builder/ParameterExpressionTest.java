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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParameterExpressionTest {

  @Test
  void simpleProperty() {
    ParameterExpression result = new ParameterExpression("id");
    Assertions.assertEquals("id", result.getProperty());
  }

  @Test
  void propertyWithSpacesInside() {
    ParameterExpression result = new ParameterExpression(" with spaces ");
    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals("with spaces", result.getProperty());
  }

  @Test
  void simplePropertyWithOldStyleJdbcType() {
    ParameterExpression result = new ParameterExpression("id:VARCHAR");
    Assertions.assertEquals("id", result.getProperty());
    Assertions.assertEquals("VARCHAR", result.getJdbcType());
  }

  @Test
  void oldStyleJdbcTypeWithExtraWhitespaces() {
    ParameterExpression result = new ParameterExpression(" id :  VARCHAR ");
    Assertions.assertEquals("id", result.getProperty());
    Assertions.assertEquals("VARCHAR", result.getJdbcType());
  }

  @Test
  void expressionWithOldStyleJdbcType() {
    ParameterExpression result = new ParameterExpression("(id.toString()):VARCHAR");
    Assertions.assertEquals("id.toString()", result.getExpression());
    Assertions.assertEquals("VARCHAR", result.getJdbcType());
  }

  @Test
  void simplePropertyWithOneAttribute() {
    ParameterExpression result = new ParameterExpression("id,name=value");
    Assertions.assertEquals("id", result.getProperty());
    Assertions.assertEquals("value", result.getAttribute("name"));
  }

  @Test
  void expressionWithOneAttribute() {
    ParameterExpression result = new ParameterExpression("(id.toString()),name=value");
    Assertions.assertEquals("id.toString()", result.getExpression());
    Assertions.assertEquals("value", result.getAttribute("name"));
  }

  @Test
  void simplePropertyWithManyAttributes() {
    ParameterExpression result = new ParameterExpression("id, attr1=val1, attr2=val2, attr3=val3");
    Assertions.assertEquals("id", result.getProperty());
    Assertions.assertEquals("val1", result.getAttribute("attr1"));
    Assertions.assertEquals("val2", result.getAttribute("attr2"));
    Assertions.assertEquals("val3", result.getAttribute("attr3"));
  }

  @Test
  void expressionWithManyAttributes() {
    ParameterExpression result = new ParameterExpression("(id.toString()), attr1=val1, attr2=val2, attr3=val3");
    Assertions.assertEquals("id.toString()", result.getExpression());
    Assertions.assertEquals("val1", result.getAttribute("attr1"));
    Assertions.assertEquals("val2", result.getAttribute("attr2"));
    Assertions.assertEquals("val3", result.getAttribute("attr3"));
  }

  @Test
  void simplePropertyWithOldStyleJdbcTypeAndAttributes() {
    ParameterExpression result = new ParameterExpression("id:VARCHAR, attr1=val1, attr2=val2");
    Assertions.assertEquals("id", result.getProperty());
    Assertions.assertEquals("VARCHAR", result.getJdbcType());
    Assertions.assertEquals("val1", result.getAttribute("attr1"));
    Assertions.assertEquals("val2", result.getAttribute("attr2"));
  }

  @Test
  void simplePropertyWithSpaceAndManyAttributes() {
    ParameterExpression result = new ParameterExpression("user name, attr1=val1, attr2=val2, attr3=val3");
    Assertions.assertEquals("user name", result.getProperty());
    Assertions.assertEquals("val1", result.getAttribute("attr1"));
    Assertions.assertEquals("val2", result.getAttribute("attr2"));
    Assertions.assertEquals("val3", result.getAttribute("attr3"));
  }

  @Test
  void shouldIgnoreLeadingAndTrailingSpaces() {
    ParameterExpression result = new ParameterExpression(" id , jdbcType =  VARCHAR,  attr1 = val1 ,  attr2 = val2 ");
    Assertions.assertEquals("id", result.getProperty());
    Assertions.assertEquals("VARCHAR", result.getJdbcType());
    Assertions.assertEquals("val1", result.getAttribute("attr1"));
    Assertions.assertEquals("val2", result.getAttribute("attr2"));
  }

  @Test
  void invalidOldJdbcTypeFormat() {
    try {
      new ParameterExpression("id:");
      Assertions.fail();
    } catch (BuilderException e) {
      Assertions.assertTrue(e.getMessage().contains("Parsing error in {id:} in position 3"));
    }
  }

  @Test
  void invalidJdbcTypeOptUsingExpression() {
    try {
      new ParameterExpression("(expression)+");
      Assertions.fail();
    } catch (BuilderException e) {
      Assertions.assertTrue(e.getMessage().contains("Parsing error in {(expression)+} in position 12"));
    }
  }

}

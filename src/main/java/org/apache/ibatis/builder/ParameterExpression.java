/*
 *    Copyright 2009-2023 the original author or authors.
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

import java.util.HashMap;

/**
 * Inline parameter expression parser. Supported grammar (simplified):
 *
 * <pre>
 * inline-parameter = (propertyName | expression) oldJdbcType attributes
 * propertyName = /expression language's property navigation path/
 * expression = '(' /expression language's expression/ ')'
 * oldJdbcType = ':' /any valid jdbc type/
 * attributes = (',' attribute)*
 * attribute = name '=' value
 * </pre>
 * <p>
 * syntax: (propertyName | expression) jdbcType option
 * </p>
 * <p>
 * For Example:
 * </p>
 * <blockquote>
 *
 * <pre>
 * (id.toString()):VARCHAR, attr1=val1, attr2=val2, attr3=val3
 * </pre>
 *
 * </blockquote>
 * <li>expression: id.toString()</li>
 * <li>jdbcType: VARCHAR</li>
 * <li>attr2:val2</li>
 * <li>attr1:val1</li>
 * <li>attr3:val3</li> <br/>
 * <blockquote>
 *
 * <pre>
 * id:VARCHAR, attr1=val1, attr2=val2, attr3=val3
 * </pre>
 *
 * </blockquote>
 * <li>property: id</li>
 * <li>jdbcType: VARCHAR</li>
 * <li>attr2:val2</li>
 * <li>attr1:val1</li>
 * <li>attr3:val3</li>
 * <p>
 * First, like other parts of MyBatis, parameters can specify a more specific data type. <blockquote>
 *
 * <pre>
 * #{property,javaType=int,jdbcType=NUMERIC}
 * </pre>
 *
 * </blockquote> Like the rest of MyBatis, the javaType can almost always be determined from the parameter object,
 * unless that object is a HashMap. Then the javaType should be specified to ensure the correct TypeHandler is used.
 * <p>
 * </p>
 * To further customize type handling, you can also specify a specific TypeHandler class (or alias), for example:
 * <blockquote>
 *
 * <pre>
 * #{age,javaType=int,jdbcType=NUMERIC,typeHandler=MyTypeHandler}
 * </pre>
 *
 * </blockquote>
 * <p>
 * For numeric types there's also a numericScale for determining how many decimal places are relevant. <blockquote>
 *
 * <pre>
 * #{height,javaType=double,jdbcType=NUMERIC,numericScale=2}
 * </pre>
 *
 * </blockquote>
 * <p>
 * Finally, the mode attribute allows you to specify IN, OUT or INOUT parameters. If a parameter is OUT or INOUT, the
 * actual value of the parameter object property will be changed, just as you would expect if you were calling for an
 * output parameter. If the mode=OUT (or INOUT) and the jdbcType=CURSOR (i.e. Oracle REFCURSOR), you must specify a
 * resultMap to map the ResultSet to the type of the parameter. Note that the javaType attribute is optional here, it
 * will be automatically set to ResultSet if left blank with a CURSOR as the jdbcType. <blockquote>
 *
 * <pre>
 * #{department, mode=OUT, jdbcType=CURSOR, javaType=ResultSet, resultMap=departmentResultMap}
 * </pre>
 *
 * </blockquote> MyBatis also supports more advanced data types such as structs, but you must tell the statement the
 * type name when registering the out parameter. For example (again, don't break lines like this in practice):
 * <blockquote>
 *
 * <pre>
 * #{middleInitial, mode=OUT, jdbcType=STRUCT, jdbcTypeName=MY_TYPE, resultMap=departmentResultMap}
 * </pre>
 *
 * </blockquote>
 * <p>
 * See more details, refer to <a href="https://mybatis.org/mybatis-3/sqlmap-xml.html#Parameters">Parameters</a>
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class ParameterExpression extends HashMap<String, String> {

  private static final long serialVersionUID = -2417552199605158680L;

  public ParameterExpression(String expression) {
    parse(expression);
  }

  private void parse(String expression) {
    int p = skipWS(expression, 0);
    if (expression.charAt(p) == '(') {
      expression(expression, p + 1);
    } else {
      property(expression, p);
    }
  }

  private void expression(String expression, int left) {
    int match = 1;
    int right = left + 1;
    while (match > 0) {
      if (expression.charAt(right) == ')') {
        match--;
      } else if (expression.charAt(right) == '(') {
        match++;
      }
      right++;
    }
    put("expression", expression.substring(left, right - 1));
    jdbcTypeOpt(expression, right);
  }

  private void property(String expression, int left) {
    if (left < expression.length()) {
      int right = skipUntil(expression, left, ",:");
      put("property", trimmedStr(expression, left, right));
      jdbcTypeOpt(expression, right);
    }
  }

  private int skipWS(String expression, int p) {
    for (int i = p; i < expression.length(); i++) {
      if (expression.charAt(i) > 0x20) {
        return i;
      }
    }
    return expression.length();
  }

  private int skipUntil(String expression, int p, final String endChars) {
    for (int i = p; i < expression.length(); i++) {
      char c = expression.charAt(i);
      if (endChars.indexOf(c) > -1) {
        return i;
      }
    }
    return expression.length();
  }

  private void jdbcTypeOpt(String expression, int p) {
    p = skipWS(expression, p);
    if (p < expression.length()) {
      if (expression.charAt(p) == ':') {
        jdbcType(expression, p + 1);
      } else if (expression.charAt(p) == ',') {
        option(expression, p + 1);
      } else {
        throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
      }
    }
  }

  private void jdbcType(String expression, int p) {
    int left = skipWS(expression, p);
    int right = skipUntil(expression, left, ",");
    if (right <= left) {
      throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
    }
    put("jdbcType", trimmedStr(expression, left, right));
    option(expression, right + 1);
  }

  private void option(String expression, int p) {
    int left = skipWS(expression, p);
    if (left < expression.length()) {
      int right = skipUntil(expression, left, "=");
      String name = trimmedStr(expression, left, right);
      left = right + 1;
      right = skipUntil(expression, left, ",");
      String value = trimmedStr(expression, left, right);
      put(name, value);
      option(expression, right + 1);
    }
  }

  private String trimmedStr(String str, int start, int end) {
    while (str.charAt(start) <= 0x20) {
      start++;
    }
    while (str.charAt(end - 1) <= 0x20) {
      end--;
    }
    return start >= end ? "" : str.substring(start, end);
  }

}

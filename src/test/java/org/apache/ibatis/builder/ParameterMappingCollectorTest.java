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

import java.util.List;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @see ParameterExpression
 */
public class ParameterMappingCollectorTest {

  @Test
  void shouldCollectParameters() {

    String expression = "select * from blog where name = "
        + "#{name:VARCHAR, mode=IN, jdbcType=VARCHAR, jdbcTypeName=MY_TYPE, resultMap=departmentResultMap}";

    List<ParameterMapping> mappings = ParameterMappingCollector.collectParameterMappings(new Configuration(),
        expression);

    Assertions.assertEquals(1, mappings.size());

    ParameterMapping parameterMapping = mappings.get(0);

    Assertions.assertEquals("name", parameterMapping.getProperty());
    Assertions.assertEquals(ParameterMode.IN, parameterMapping.getMode());
    Assertions.assertEquals(JdbcType.VARCHAR, parameterMapping.getJdbcType());
    Assertions.assertEquals("MY_TYPE", parameterMapping.getJdbcTypeName());
    Assertions.assertEquals("departmentResultMap", parameterMapping.getResultMapId());
  }
}

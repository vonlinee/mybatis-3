package org.apache.ibatis.builder;

import java.util.List;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
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

    List<ParameterMapping> mappings = ParameterMappingCollector.collectParameterMappings(expression);

    Assertions.assertEquals(1, mappings.size());

    ParameterMapping parameterMapping = mappings.get(0);

    Assertions.assertEquals("name", parameterMapping.getProperty());
    Assertions.assertEquals(ParameterMode.IN, parameterMapping.getMode());
    Assertions.assertEquals(JdbcType.VARCHAR, parameterMapping.getJdbcType());
    Assertions.assertEquals("MY_TYPE", parameterMapping.getJdbcTypeName());
    Assertions.assertEquals("departmentResultMap", parameterMapping.getResultMapId());
  }
}

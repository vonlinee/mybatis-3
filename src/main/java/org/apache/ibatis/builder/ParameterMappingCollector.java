package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * @see ParameterMappingTokenHandler
 * @see ParameterMapping
 */
public class ParameterMappingCollector implements TokenHandler {

  List<ParameterMapping> parameterMappings;

  public ParameterMappingCollector() {
    this.parameterMappings = new ArrayList<>();
  }

  @Override
  public String handleToken(String content) {
    ParameterMapping parameter = buildParameterMapping(content);
    parameterMappings.add(parameter);
    return content;
  }

  protected ParameterMapping buildParameterMapping(String content) {
    ParameterExpression propertiesMap = parseParameterMapping(content);

    final String property = propertiesMap.remove("property");
    final JdbcType jdbcType = resolveJdbcType(propertiesMap.remove("jdbcType"));
    final String typeHandlerAlias = propertiesMap.remove("typeHandler");

    ParameterMapping.Builder builder = new ParameterMapping.Builder(property, (Class<?>) null);
    builder.jdbcType(jdbcType);

    builder.typeHandler(typeHandlerAlias);

    ParameterMode mode;
    for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
      String name = entry.getKey();
      String value = entry.getValue();
      if ("mode".equals(name)) {
        mode = resolveParameterMode(value);
        builder.mode(mode);
      } else if ("numericScale".equals(name)) {
        builder.numericScale(Integer.valueOf(value));
      } else if ("resultMap".equals(name)) {
        builder.resultMapId(value);
      } else if ("jdbcTypeName".equals(name)) {
        builder.jdbcTypeName(value);
      } else if ("expression".equals(name)) {
        throw new BuilderException("Expression based parameters are not supported yet");
      } else {
        throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content
            + "}.  Valid properties are " + ParameterMappingTokenHandler.PARAMETER_PROPERTIES);
      }
    }
    return builder.build();
  }

  List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

  public static List<ParameterMapping> collectParameterMappings(String text) {
    ParameterMappingCollector handler = new ParameterMappingCollector();
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    String sql = parser.parse(text);
    return handler.getParameterMappings();
  }

  private ParameterExpression parseParameterMapping(String content) {
    try {
      return new ParameterExpression(content);
    } catch (BuilderException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BuilderException("Parsing error was found in mapping #{" + content
          + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
    }
  }

  protected JdbcType resolveJdbcType(String alias) {
    try {
      return alias == null ? null : JdbcType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
    }
  }

  protected ParameterMode resolveParameterMode(String alias) {
    try {
      return alias == null ? null : ParameterMode.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
    }
  }
}

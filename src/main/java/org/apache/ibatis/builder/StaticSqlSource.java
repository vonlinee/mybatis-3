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
package org.apache.ibatis.builder;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.extension.ParamType;
import org.apache.ibatis.extension.SqlUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.SimpleTypeRegistry;

/**
 * @author Clinton Begin
 */
public class StaticSqlSource implements SqlSource {

  private final String sql;
  private final List<ParameterMapping> parameterMappings;

  public StaticSqlSource(String sql) {
    this(sql, null);
  }

  public StaticSqlSource(String sql, List<ParameterMapping> parameterMappings) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
  }

  @Override
  public BoundSql getBoundSql(Configuration configuration, Object parameterObject, ParamType paramType) {
    if (paramType == ParamType.INLINED) {
      if (!parameterMappings.isEmpty()) {
        MetaObject metaObject = configuration.newMetaObject(parameterObject);
        Object[] argValues = new Object[parameterMappings.size()];
        for (int i = 0; i < parameterMappings.size(); i++) {
          ParameterMapping pm = parameterMappings.get(i);
          Object argValue = null;
          if (pm.hasValue()) {
            argValue = pm.getValue();
          } else if (metaObject.hasProperty(pm.getProperty())) {
            argValue = metaObject.getValue(pm.getProperty());
          } else if (parameterObject != null && SimpleTypeRegistry.isSimpleType(parameterObject.getClass())) {
            argValue = parameterObject;
          }
          argValues[i] = argValue;
        }
        String inlinedSql = SqlUtils.inlineParams(this.sql, argValues, configuration.getSqlValueFormatter());
        return new BoundSql(configuration, inlinedSql, Collections.emptyList(), parameterObject);
      }
    }
    return new BoundSql(configuration, sql, parameterMappings, parameterObject);
  }

}

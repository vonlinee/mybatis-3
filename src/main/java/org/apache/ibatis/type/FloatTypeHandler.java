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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class FloatTypeHandler extends BaseTypeHandler<Float> {
  public static final FloatTypeHandler INSTANCE = new FloatTypeHandler();

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Float parameter, JdbcType jdbcType) throws SQLException {
    ps.setFloat(i, parameter);
  }

  @Override
  public Float getNullableResult(ResultSet rs, String columnName) throws SQLException {
    float result = rs.getFloat(columnName);
    return result == 0 && rs.wasNull() ? null : result;
  }

  @Override
  public Float getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    float result = rs.getFloat(columnIndex);
    return result == 0 && rs.wasNull() ? null : result;
  }

  @Override
  public Float getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    float result = cs.getFloat(columnIndex);
    return result == 0 && cs.wasNull() ? null : result;
  }
}

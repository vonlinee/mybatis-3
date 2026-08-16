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
package org.apache.ibatis.executor.result;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Used to map raw JDBC ResultSet to objects.
 * <p>
 * This provides a low level mapping option with direct use of JDBC ResultSet with the option of having logic in the
 * mapping. For example, only map some columns depending on the values read from other columns.
 * </p>
 *
 * <pre>{@code
 *
 * // Map from ResultSet to Customer bean
 * class CustomerMapper implements RowMapper<Customer> {
 *
 *   @Override
 *   public Customer map(ResultSet rs, int rowNum) throws SQLException {
 *     long id = rs.getLong(1);
 *     String name = rs.getString(2);
 *     String status = rs.getString(3);
 *     return new Customer(id, name, status);
 *   }
 * }
 *
 * // Then use the mapper
 * String sql = "select id, name, status from t_customer where name = ?";
 * PrepareStatement stmt = connection.prepareStatement(sql);
 * ResultSet rs = stmt.executeQuery();
 * CustomerMapper mapper = new CustomerMapper();
 * int rowNum = 0;
 * while (rs.next()) {
 *   Customer customer = mapper.mapRow(rs, rowNum++);
 * }
 * }</pre>
 *
 * @param <T>
 *          The type the row data is mapped into.
 */
@FunctionalInterface
public interface RowMapper<T> {

  /**
   * Implementations must implement this method to map each row of data in the {@code ResultSet}. This method should not
   * call {@code next()} on the {@code ResultSet}; it is only supposed to map values of the current row.
   *
   * @param rs
   *          the {@code ResultSet} to map (pre-initialized for the current row)
   * @param rowNum
   *          the number of the current row
   *
   * @return the result object for the current row (maybe {@code null})
   *
   * @throws SQLException
   *           if an SQLException is encountered while getting column values (that is, there's no need to catch
   *           SQLException)
   */
  T mapRow(ResultSet rs, int rowNum) throws SQLException;
}

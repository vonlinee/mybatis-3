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
package org.apache.ibatis.mapping;

import org.jetbrains.annotations.NotNull;

public enum NameMapping implements NamingStrategy {

  /**
   * lowerCamelCase in java and lower_underscore in database
   */
  LOWER_UNDERSCORE_TO_LOWER_CAMEL(NamingCase.LOWER_CAMEL, NamingCase.LOWER_UNDERSCORE),

  /**
   * upperCamelCase in java and upper_underscore in database
   */
  UPPER_UNDERSCORE_TO_LOWER_CAMEL(NamingCase.LOWER_CAMEL, NamingCase.UPPER_UNDERSCORE),

  /**
   * lowerCamelCase in java and lower_underscore in database
   */
  LOWER_CAMEL_TO_LOWER_CAMEL(NamingCase.LOWER_CAMEL, NamingCase.LOWER_CAMEL);

  /**
   * Java naming style
   */
  private final NamingStyle javaNamingStyle;

  /**
   * Database naming style
   */
  private final NamingStyle dbNamingStyle;

  NameMapping(NamingStyle javaNamingStyle, NamingStyle dbNamingStyle) {
    this.javaNamingStyle = javaNamingStyle;
    this.dbNamingStyle = dbNamingStyle;
  }

  @Override
  @NotNull
  public String getName() {
    return name();
  }

  @Override
  public String columnToProperty(String column) {
    return dbNamingStyle.to(javaNamingStyle, column);
  }

  @Override
  public String propertyToColumn(String property) {
    return javaNamingStyle.to(dbNamingStyle, property);
  }
}

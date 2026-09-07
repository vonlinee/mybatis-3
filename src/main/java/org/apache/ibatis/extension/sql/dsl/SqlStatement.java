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
package org.apache.ibatis.extension.sql.dsl;

import org.apache.ibatis.extension.ParamType;

public interface SqlStatement {

  RenderedSql getRenderedSql(ParamType paramType, SqlFormat format);

  default RenderedSql getRenderedSql(ParamType paramType) {
    return getRenderedSql(paramType, SqlFormat.COMPACT);
  }

  default RenderedSql getRenderedSql(ParamType paramType, SqlDialect dialect) {
    return getRenderedSql(paramType, SqlFormat.COMPACT, dialect);
  }

  default RenderedSql getRenderedSql(ParamType paramType, SqlFormat format, SqlDialect dialect) {
    return getRenderedSql(paramType, format);
  }

  default String getSql(ParamType paramType, SqlFormat format, SqlDialect dialect) {
    return getRenderedSql(paramType, format, dialect).sql();
  }

  default String getSql(ParamType paramType, SqlDialect dialect) {
    return getSql(paramType, SqlFormat.COMPACT, dialect);
  }

  default String getSql(ParamType paramType, SqlFormat format) {
    return getRenderedSql(paramType, format).sql();
  }

  default String getSql(ParamType paramType) {
    return getSql(paramType, SqlFormat.COMPACT);
  }
}

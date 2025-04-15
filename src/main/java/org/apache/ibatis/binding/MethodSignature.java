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
package org.apache.ibatis.binding;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * method metadata of mapper method
 */
public class MethodSignature {

  private boolean returnsMany;
  private boolean returnsMap;
  private boolean returnsVoid;
  private boolean returnsCursor;
  private boolean returnsOptional;
  private Class<?> returnType;
  private String mapKey;
  private Integer resultHandlerIndex;
  private Integer rowBoundsIndex;

  public boolean hasRowBounds() {
    return rowBoundsIndex != null;
  }

  public RowBounds extractRowBounds(Object[] args) {
    return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
  }

  public boolean hasResultHandler() {
    return resultHandlerIndex != null;
  }

  public Integer getResultHandlerIndex() {
    return resultHandlerIndex;
  }

  public Integer getRowBoundsIndex() {
    return rowBoundsIndex;
  }

  public ResultHandler<?> extractResultHandler(Object[] args) {
    return hasResultHandler() ? (ResultHandler<?>) args[resultHandlerIndex] : null;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  public boolean returnsMany() {
    return returnsMany;
  }

  public boolean returnsMap() {
    return returnsMap;
  }

  public boolean returnsVoid() {
    return returnsVoid;
  }

  public boolean returnsCursor() {
    return returnsCursor;
  }

  /**
   * return whether return type is {@code java.util.Optional}.
   *
   * @return return {@code true}, if return type is {@code java.util.Optional}
   *
   * @since 3.5.0
   */
  public boolean returnsOptional() {
    return returnsOptional;
  }

  public String getMapKey() {
    return mapKey;
  }

  public void setMapKey(String mapKey) {
    this.mapKey = mapKey;
  }

  public void setResultHandlerIndex(Integer resultHandlerIndex) {
    this.resultHandlerIndex = resultHandlerIndex;
  }

  public void setReturnsCursor(boolean returnsCursor) {
    this.returnsCursor = returnsCursor;
  }

  public void setReturnsMany(boolean returnsMany) {
    this.returnsMany = returnsMany;
  }

  public void setReturnsMap(boolean returnsMap) {
    this.returnsMap = returnsMap;
  }

  public void setReturnsOptional(boolean returnsOptional) {
    this.returnsOptional = returnsOptional;
  }

  public void setReturnsVoid(boolean returnsVoid) {
    this.returnsVoid = returnsVoid;
  }

  public void setReturnType(Class<?> returnType) {
    this.returnType = returnType;
  }

  public void setRowBoundsIndex(Integer rowBoundsIndex) {
    this.rowBoundsIndex = rowBoundsIndex;
  }
}

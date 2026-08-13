/*
 *    Copyright 2009-2022 the original author or authors.
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

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 */
public class DefaultResultContext<T> implements ResultContext<T> {

  private T resultObject;
  private int resultCount;
  private boolean stopped;

  private final ResultHandler<?> resultHandler;
  private final RowBounds rowBounds;

  public DefaultResultContext() {
    this(null, null);
  }

  public DefaultResultContext(ResultHandler<?> resultHandler, RowBounds rowBounds) {
    resultObject = null;
    resultCount = 0;
    stopped = false;
    this.resultHandler = resultHandler;
    this.rowBounds = rowBounds;
  }

  @Override
  public T getResultObject() {
    return resultObject;
  }

  @Override
  public int getResultCount() {
    return resultCount;
  }

  @Override
  public boolean isStopped() {
    return stopped;
  }

  @SuppressWarnings("unchecked")
  public void nextResultObject(T resultObject) {
    resultCount++;
    this.resultObject = resultObject;

    if (resultHandler != null) {
      ((ResultHandler<Object>) resultHandler).handleResult(this);
    }
  }

  @Override
  public void stop() {
    this.stopped = true;
  }

  public boolean shouldProcessMoreRows() {
    return !stopped && (rowBounds == null || resultCount < rowBounds.getLimit());
  }
}

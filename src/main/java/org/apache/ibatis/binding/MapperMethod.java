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

import java.lang.reflect.Method;

import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 * @author Kazuki Shimizu
 */
public abstract class MapperMethod {

  protected SqlCommand command;

  public SqlCommand getCommand() {
    return command;
  }

  /**
   * @param proxy
   *          mapper proxy
   * @param method
   *          mapper method
   * @param args
   *          args
   * @param sqlSession
   *          sql session
   *
   * @return result
   */
  public abstract Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
}

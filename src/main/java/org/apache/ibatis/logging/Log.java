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
package org.apache.ibatis.logging;

/**
 * @author Clinton Begin
 */
public interface Log {

  boolean isInfoEnabled();

  boolean isErrorEnabled();

  boolean isWarnEnabled();

  boolean isDebugEnabled();

  boolean isTraceEnabled();

  void info(String s);

  void info(String s, Throwable e);

  void error(String s, Throwable e);

  void error(String s);

  void debug(String s);

  void debug(String s, Throwable e);

  void trace(String s);

  void trace(String s, Throwable e);

  void warn(String s);

  void warn(String s, Throwable e);

  default void debugIfEnabled(String s) {
    if (isDebugEnabled()) {
      debug(s);
    }
  }

  default void debugIfEnabled(String s, Throwable e) {
    if (isDebugEnabled()) {
      debug(s, e);
    }
  }

  default void infoIfEnabled(String s) {
    if (isInfoEnabled()) {
      info(s);
    }
  }

  default void infoIfEnabled(String s, Throwable e) {
    if (isInfoEnabled()) {
      info(s, e);
    }
  }

  default void errorIfEnabled(String s) {
    if (isErrorEnabled()) {
      error(s);
    }
  }

  default void errorIfEnabled(String s, Throwable e) {
    if (isErrorEnabled()) {
      error(s, e);
    }
  }

  default void traceIfEnabled(String s) {
    if (isTraceEnabled()) {
      trace(s);
    }
  }

  default void traceIfEnabled(String s, Throwable e) {
    if (isTraceEnabled()) {
      trace(s, e);
    }
  }

  default void warnIfEnabled(String s) {
    if (isWarnEnabled()) {
      warn(s);
    }
  }

  default void warnIfEnabled(String s, Throwable e) {
    if (isWarnEnabled()) {
      warn(s, e);
    }
  }
}

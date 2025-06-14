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
package org.apache.ibatis.logging.stdout;

import org.apache.ibatis.internal.util.ObjectUtils;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * @author Clinton Begin
 */
public class StdOutImpl implements Log {

  public StdOutImpl(String clazz) {
    // Do Nothing
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public boolean isTraceEnabled() {
    return true;
  }

  @Override
  public void info(String s) {
    System.out.println(s);
  }

  @Override
  public void info(String s, Throwable e) {
    System.out.println(s);
    e.printStackTrace(System.out);
  }

  @Override
  public void info(String format, Object... arguments) {
    Throwable throwable = ExceptionUtil.extractThrowable(arguments);
    if (throwable == null) {
      info(String.format(format, arguments));
    } else {
      arguments = ObjectUtils.trimmedLast(arguments);
      info(String.format(format, arguments), throwable);
    }
  }

  @Override
  public void error(String s, Throwable e) {
    System.err.println(s);
    e.printStackTrace(System.err);
  }

  @Override
  public void error(String s) {
    System.err.println(s);
  }

  @Override
  public void error(String format, Object... arguments) {
    Throwable throwable = ExceptionUtil.extractThrowable(arguments);
    if (throwable == null) {
      error(String.format(format, arguments));
    } else {
      arguments = ObjectUtils.trimmedLast(arguments);
      error(String.format(format, arguments), throwable);
    }
  }

  @Override
  public void debug(String s) {
    System.out.println(s);
  }

  @Override
  public void debug(String format, Object... arguments) {
    Throwable throwable = ExceptionUtil.extractThrowable(arguments);
    if (throwable == null) {
      debug(String.format(format, arguments));
    } else {
      arguments = ObjectUtils.trimmedLast(arguments);
      debug(String.format(format, arguments), throwable);
    }
  }

  @Override
  public void debug(String s, Throwable e) {
    System.out.println(s);
    e.printStackTrace(System.out);
  }

  @Override
  public void trace(String s) {
    System.out.println(s);
  }

  @Override
  public void trace(String s, Throwable e) {
    System.out.println(s);
    e.printStackTrace(System.out);
  }

  @Override
  public void trace(String format, Object... arguments) {
    Throwable throwable = ExceptionUtil.extractThrowable(arguments);
    if (throwable == null) {
      trace(String.format(format, arguments));
    } else {
      arguments = ObjectUtils.trimmedLast(arguments);
      trace(String.format(format, arguments), throwable);
    }
  }

  @Override
  public void warn(String s) {
    System.out.println(s);
  }

  @Override
  public void warn(String s, Throwable e) {
    System.out.println(s);
    e.printStackTrace(System.out);
  }

  @Override
  public void warn(String format, Object... arguments) {
    Throwable throwable = ExceptionUtil.extractThrowable(arguments);
    if (throwable == null) {
      warn(String.format(format, arguments));
    } else {
      arguments = ObjectUtils.trimmedLast(arguments);
      warn(String.format(format, arguments), throwable);
    }
  }
}

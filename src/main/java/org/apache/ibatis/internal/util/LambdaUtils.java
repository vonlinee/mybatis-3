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
package org.apache.ibatis.internal.util;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.ibatis.internal.util.function.ThrowableFunction;

public final class LambdaUtils {

  private LambdaUtils() {
  }

  public static <T, R> SerializedLambda getSerializedLambda(ThrowableFunction<T, R> methodRef)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    if (methodRef == null) {
      throw new IllegalArgumentException("method reference is null");
    }
    Method writeReplace = methodRef.getClass().getDeclaredMethod("writeReplace");
    writeReplace.setAccessible(true);
    return (SerializedLambda) writeReplace.invoke(methodRef);
  }
}

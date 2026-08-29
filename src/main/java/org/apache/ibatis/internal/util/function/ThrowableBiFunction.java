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
package org.apache.ibatis.internal.util.function;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * Represents a function that accepts two arguments and produces a result and may throw a throwable. This is the
 * two-arity specialization of {@link java.util.function.Function}.
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose functional method is
 * {@link #apply(Object, Object)}.
 *
 * @param <T>
 *          the type of the first argument to the function
 * @param <U>
 *          the type of the second argument to the function
 * @param <R>
 *          the type of the result of the function
 *
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface ThrowableBiFunction<T, U, R> extends Serializable {

  /**
   * Applies this function to the given arguments.
   *
   * @param t
   *          the first function argument
   * @param u
   *          the second function argument
   *
   * @return the function result
   *
   * @throws Throwable
   *           the function throws a throwable
   */
  R apply(T t, U u) throws Throwable;

  static <T, U, R> ThrowableBiFunction<T, U, R> of(BiFunction<T, U, R> biFunction) {
    return biFunction::apply;
  }
}

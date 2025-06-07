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
package org.apache.ibatis.internal;

/**
 * Consumer for 'forEach' operation.
 *
 * @param <T>
 *          context object, can be an {@link Iterable}
 * @param <E>
 *          Element type of iterable
 */
public interface ForEachConsumer<T, E> {

  /**
   * Accept an iterable element with index.
   *
   * @param ctx
   *          context object, can be an {@link Iterable}
   * @param element
   *          an iterable element
   * @param elementIndex
   *          an element index
   */
  void accept(T ctx, E element, int elementIndex);
}

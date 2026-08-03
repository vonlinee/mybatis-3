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
package org.apache.ibatis.binding;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

import org.apache.ibatis.session.SqlSession;
import org.jetbrains.annotations.Nullable;

public class CompositeMethodLookup implements MapperMethod.Lookup {

  private final Deque<MapperMethod.Lookup> lookups = new ConcurrentLinkedDeque<>();

  /**
   * Adds a lookup to the tail of the queue.
   *
   * @param lookup
   *          the lookup to add
   */
  public void addMethodLookup(MapperMethod.Lookup lookup) {
    addLast(lookup);
  }

  /**
   * Adds a lookup to the head of the queue.
   *
   * @param lookup
   *          the lookup to add
   */
  public void addFirst(MapperMethod.Lookup lookup) {
    lookups.addFirst(Objects.requireNonNull(lookup, "lookup"));
  }

  /**
   * Adds a lookup to the tail of the queue.
   *
   * @param lookup
   *          the lookup to add
   */
  public void addLast(MapperMethod.Lookup lookup) {
    lookups.addLast(Objects.requireNonNull(lookup, "lookup"));
  }

  /**
   * Removes and returns the lookup at the head of the queue.
   *
   * @return the removed lookup, or {@code null} if the queue is empty
   */
  @Nullable
  public MapperMethod.Lookup removeFirst() {
    return lookups.pollFirst();
  }

  /**
   * Removes and returns the lookup at the tail of the queue.
   *
   * @return the removed lookup, or {@code null} if the queue is empty
   */
  @Nullable
  public MapperMethod.Lookup removeLast() {
    return lookups.pollLast();
  }

  public List<MapperMethod.Lookup> getLookups() {
    return new ArrayList<>(lookups);
  }

  public void removeLookup(MapperMethod.Lookup lookup) {
    lookups.remove(lookup);
  }

  public void removeIf(Predicate<MapperMethod.Lookup> predicate) {
    lookups.removeIf(predicate);
  }

  @Override
  @Nullable
  public MapperMethod findMethod(Object proxy, Class<?> mapperInterface, Method method, Object[] args,
      SqlSession sqlSession) throws Throwable {
    for (MapperMethod.Lookup lookup : lookups) {
      MapperMethod mapperMethod = lookup.findMethod(proxy, mapperInterface, method, args, sqlSession);
      if (mapperMethod != null) {
        return mapperMethod;
      }
    }
    return null;
  }
}

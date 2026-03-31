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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

public final class ObjectUtils {

  private ObjectUtils() {
    // Prevent Instantiation of Static Class
  }

  public static boolean isEmpty(@Nullable Object obj) {
    if (obj == null) {
      return true;
    }
    if (obj instanceof Optional) {
      return ((Optional<?>) obj).isEmpty();
    }
    if (obj instanceof CharSequence) {
      return ((CharSequence) obj).length() == 0;
    }
    if (obj.getClass().isArray()) {
      return Array.getLength(obj) == 0;
    }
    if (obj instanceof Collection) {
      return ((Collection<?>) obj).isEmpty();
    }
    if (obj instanceof Map) {
      return ((Map<?, ?>) obj).isEmpty();
    }
    // else
    return false;
  }

  /**
   * Determine whether the given array is empty: i.e. {@code null} or of zero length.
   *
   * @param array
   *          the array to check
   *
   * @see #isEmpty(Object)
   */
  public static <T> boolean isEmpty(@Nullable T[] array) {
    return (array == null || array.length == 0);
  }
}

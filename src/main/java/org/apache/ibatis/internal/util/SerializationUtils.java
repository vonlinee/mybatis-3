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

import java.io.*;

public final class SerializationUtils {

  private SerializationUtils() {
    // Prevent Instantiation of Static Class
  }

  public static byte[] writeObject(Object original) throws IOException {
    ByteArrayOutputStream bass = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(bass)) {
      oos.writeObject(original);
    }
    return bass.toByteArray();
  }

  @SuppressWarnings("unchecked")
  public static <T> T readObject(byte[] bytes) throws IOException, ClassNotFoundException {
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return (T) ois.readObject();
    }
  }
}

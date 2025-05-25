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
package org.apache.ibatis.internal.util;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.jetbrains.annotations.Nullable;

public final class ObjectUtils {

  private static final String EMPTY_STRING = "";
  private static final String NULL_STRING = "null";
  private static final String ARRAY_START = "{";
  private static final String ARRAY_END = "}";
  private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
  private static final String ARRAY_ELEMENT_SEPARATOR = ", ";
  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  private static final String NON_EMPTY_ARRAY = ARRAY_START + "..." + ARRAY_END;
  private static final String COLLECTION = "[...]";
  private static final String MAP = NON_EMPTY_ARRAY;

  private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

  private ObjectUtils() {
  }

  public static int parseInt(@Nullable Object obj, int defaultValue) {
    if (obj == null) {
      return defaultValue;
    }
    return parseInt(obj.toString(), defaultValue);
  }

  public static int parseInt(@Nullable String str, int defaultValue) {
    if (StringUtils.isBlank(str)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException ignored) {
    }
    return defaultValue;
  }

  /**
   * Determine whether the given object is an array: either an Object array or a primitive array.
   *
   * @param obj
   *          the object to check
   */
  public static boolean isArray(@Nullable Object obj) {
    return (obj != null && obj.getClass().isArray());
  }

  /**
   * Determine whether the given array is empty: i.e. {@code null} or of zero length.
   *
   * @param array
   *          the array to check
   *
   * @see #isEmpty(Object)
   */
  public static boolean isEmpty(@Nullable Object[] array) {
    return (array == null || array.length == 0);
  }

  /**
   * Determine whether the given object is empty.
   * <p>
   * This method supports the following object types.
   * <ul>
   * <li>{@code Optional}: considered empty if not {@link Optional#isPresent()}</li>
   * <li>{@code Array}: considered empty if its length is zero</li>
   * <li>{@link CharSequence}: considered empty if its length is zero</li>
   * <li>{@link Collection}: delegates to {@link Collection#isEmpty()}</li>
   * <li>{@link Map}: delegates to {@link Map#isEmpty()}</li>
   * </ul>
   * <p>
   * If the given object is non-null and not one of the aforementioned supported types, this method returns
   * {@code false}.
   *
   * @param obj
   *          the object to check
   *
   * @return {@code true} if the object is {@code null} or <em>empty</em>
   *
   * @see Optional#isPresent()
   * @see ObjectUtils#isEmpty(Object[])
   * @see StringUtils#hasLength(CharSequence)
   * @see CollectionUtils#isEmpty(java.util.Collection)
   * @see CollectionUtils#isEmpty(java.util.Map)
   *
   * @since 4.2
   */
  public static boolean isEmpty(@Nullable Object obj) {
    if (obj == null) {
      return true;
    }

    if (obj instanceof Optional<?>) {
      Optional<?> optional = (Optional<?>) obj;
      return !optional.isPresent();
    }
    if (obj instanceof CharSequence) {
      CharSequence charSequence = (CharSequence) obj;
      return charSequence.length() == 0;
    }
    if (obj.getClass().isArray()) {
      return Array.getLength(obj) == 0;
    }
    if (obj instanceof Collection<?>) {
      Collection<?> collection = (Collection<?>) obj;
      return collection.isEmpty();
    }
    if (obj instanceof Map<?, ?>) {
      Map<?, ?> map = (Map<?, ?>) obj;
      return map.isEmpty();
    }
    // else
    return false;
  }

  /**
   * Convert the given array (which may be a primitive array) to an object array (if necessary of primitive wrapper
   * objects).
   * <p>
   * A {@code null} source value will be converted to an empty Object array.
   *
   * @param source
   *          the (potentially primitive) array
   *
   * @return the corresponding object array (never {@code null})
   *
   * @throws IllegalArgumentException
   *           if the parameter is not an array
   */
  public static Object[] toObjectArray(@Nullable Object source) {
    if (source instanceof Object[]) {
      Object[] objects = (Object[]) source;
      return objects;
    }
    if (source == null) {
      return EMPTY_OBJECT_ARRAY;
    }
    if (!source.getClass().isArray()) {
      throw new IllegalArgumentException("Source is not an array: " + source);
    }
    int length = Array.getLength(source);
    if (length == 0) {
      return EMPTY_OBJECT_ARRAY;
    }
    Class<?> wrapperType = Array.get(source, 0).getClass();
    Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
    for (int i = 0; i < length; i++) {
      newArray[i] = Array.get(source, i);
    }
    return newArray;
  }

  /**
   * Determine if the given objects are equal, returning {@code true} if both are {@code null} or {@code false} if only
   * one is {@code null}.
   * <p>
   * Compares arrays with {@code Arrays.equals}, performing an equality check based on the array elements rather than
   * the array reference.
   *
   * @param o1
   *          first Object to compare
   * @param o2
   *          second Object to compare
   *
   * @return whether the given objects are equal
   *
   * @see Object#equals(Object)
   * @see java.util.Arrays#equals
   */
  public static boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
    if (o1 == o2) {
      return true;
    }
    if (o1 == null || o2 == null) {
      return false;
    }
    if (o1.equals(o2)) {
      return true;
    }
    if (o1.getClass().isArray() && o2.getClass().isArray()) {
      return arrayEquals(o1, o2);
    }
    return false;
  }

  /**
   * Compare the given arrays with {@code Arrays.equals}, performing an equality check based on the array elements
   * rather than the array reference.
   *
   * @param o1
   *          first array to compare
   * @param o2
   *          second array to compare
   *
   * @return whether the given objects are equal
   *
   * @see #nullSafeEquals(Object, Object)
   * @see java.util.Arrays#equals
   */
  private static boolean arrayEquals(Object o1, Object o2) {
    if (o1 instanceof Object[] && o2 instanceof Object[]) {
      Object[] objects1 = (Object[]) o1;
      Object[] objects2 = (Object[]) o2;
      return Arrays.equals(objects1, objects2);
    }
    if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
      boolean[] booleans1 = (boolean[]) o1;
      boolean[] booleans2 = (boolean[]) o2;
      return Arrays.equals(booleans1, booleans2);
    }
    if (o1 instanceof byte[] && o2 instanceof byte[]) {
      byte[] bytes1 = (byte[]) o1;
      byte[] bytes2 = (byte[]) o2;
      return Arrays.equals(bytes1, bytes2);
    }
    if (o1 instanceof char[] && o2 instanceof char[]) {
      char[] chars1 = (char[]) o1;
      char[] chars2 = (char[]) o2;
      return Arrays.equals(chars1, chars2);
    }
    if (o1 instanceof double[] && o2 instanceof double[]) {
      double[] doubles1 = (double[]) o1;
      double[] doubles2 = (double[]) o2;
      return Arrays.equals(doubles1, doubles2);
    }
    if (o1 instanceof float[] && o2 instanceof float[]) {
      float[] floats1 = (float[]) o1;
      float[] floats2 = (float[]) o2;
      return Arrays.equals(floats1, floats2);
    }
    if (o1 instanceof int[] && o2 instanceof int[]) {
      int[] ints1 = (int[]) o1;
      int[] ints2 = (int[]) o2;
      return Arrays.equals(ints1, ints2);
    }
    if (o1 instanceof long[] && o2 instanceof long[]) {
      long[] longs1 = (long[]) o1;
      long[] longs2 = (long[]) o2;
      return Arrays.equals(longs1, longs2);
    }
    if (o1 instanceof short[] && o2 instanceof short[]) {
      short[] shorts1 = (short[]) o1;
      short[] shorts2 = (short[]) o2;
      return Arrays.equals(shorts1, shorts2);
    }
    return false;
  }

  /**
   * Check for a {@code BigInteger}/{@code BigDecimal} long overflow before returning the given number as a long value.
   *
   * @param number
   *          the number to convert
   * @param targetClass
   *          the target class to convert to
   *
   * @return the long value, if convertible without overflow
   *
   * @throws IllegalArgumentException
   *           if there is an overflow
   *
   * @see #raiseOverflowException
   */
  private static long checkedLongValue(Number number, Class<? extends Number> targetClass) {
    BigInteger bigInt = null;
    if (number instanceof BigInteger) {
      BigInteger bigInteger = (BigInteger) number;
      bigInt = bigInteger;
    } else if (number instanceof BigDecimal) {
      BigDecimal bigDecimal = (BigDecimal) number;
      bigInt = bigDecimal.toBigInteger();
    }
    // Effectively analogous to JDK 8's BigInteger.longValueExact()
    if (bigInt != null && (bigInt.compareTo(LONG_MIN) < 0 || bigInt.compareTo(LONG_MAX) > 0)) {
      raiseOverflowException(number, targetClass);
    }
    return number.longValue();
  }

  /**
   * Raise an <em>overflow</em> exception for the given number and target class.
   *
   * @param number
   *          the number we tried to convert
   * @param targetClass
   *          the target class we tried to convert to
   *
   * @throws IllegalArgumentException
   *           if there is an overflow
   */
  private static void raiseOverflowException(Number number, Class<?> targetClass) {
    throw new IllegalArgumentException("Could not convert number [" + number + "] of type ["
        + number.getClass().getName() + "] to target class [" + targetClass.getName() + "]: overflow");
  }

  /**
   * Generate a null-safe, concise string representation of the supplied object as described below.
   * <p>
   * Favor this method over {@link #nullSafeToString(Object)} when you need the length of the generated string to be
   * limited.
   * <p>
   * Returns:
   * <ul>
   * <li>{@code "null"} if {@code obj} is {@code null}</li>
   * <li>{@code "Optional.empty"} if {@code obj} is an empty {@link Optional}</li>
   * <li>{@code "Optional[<concise-string>]"} if {@code obj} is a non-empty {@code Optional}, where
   * {@code <concise-string>} is the result of invoking this method on the object contained in the {@code Optional}</li>
   * <li>{@code "{}"} if {@code obj} is an empty array</li>
   * <li>{@code "{...}"} if {@code obj} is a {@link Map} or a non-empty array</li>
   * <li>{@code "[...]"} if {@code obj} is a {@link Collection}</li>
   * <li>{@linkplain Class#getName() Class name} if {@code obj} is a {@link Class}</li>
   * <li>{@linkplain Charset#name() Charset name} if {@code obj} is a {@link Charset}</li>
   * <li>{@linkplain TimeZone#getID() TimeZone ID} if {@code obj} is a {@link TimeZone}</li>
   * <li>{@linkplain ZoneId#getId() Zone ID} if {@code obj} is a {@link ZoneId}</li>
   * <li>Potentially {@linkplain StringUtils#truncate(CharSequence) truncated string} if {@code obj} is a {@link String}
   * or {@link CharSequence}</li>
   * <li>Potentially {@linkplain StringUtils#truncate(CharSequence) truncated string} if {@code obj} is a <em>simple
   * value type</em> whose {@code toString()} method returns a non-null value</li>
   * <li>Otherwise, a string representation of the object's type name concatenated with {@code "@"} and a hex string
   * form of the object's identity hash code</li>
   * </ul>
   * <p>
   * In the context of this method, a <em>simple value type</em> is any of the following: primitive wrapper (excluding
   * {@link Void}), {@link Enum}, {@link Number}, {@link java.util.Date Date}, {@link java.time.temporal.Temporal
   * Temporal}, {@link java.io.File File}, {@link java.nio.file.Path Path}, {@link java.net.URI URI},
   * {@link java.net.URL URL}, {@link java.net.InetAddress InetAddress}, {@link java.util.Currency Currency},
   * {@link java.util.Locale Locale}, {@link java.util.UUID UUID}, {@link java.util.regex.Pattern Pattern}.
   *
   * @param obj
   *          the object to build a string representation for
   *
   * @return a concise string representation of the supplied object
   *
   * @see #nullSafeToString(Object)
   * @see StringUtils#truncate(CharSequence)
   * @see ClassUtils#isSimpleValueType(Class)
   *
   * @since 5.3.27
   */
  public static String nullSafeConciseToString(@Nullable Object obj) {
    if (obj == null) {
      return "null";
    }
    if (obj instanceof Optional<?>) {
      Optional<?> optional = (Optional<?>) obj;
      return (optional.map(object -> String.format("Optional[%s]", (nullSafeConciseToString(object))))
          .orElse("Optional.empty"));
    }
    if (obj.getClass().isArray()) {
      return (Array.getLength(obj) == 0 ? EMPTY_ARRAY : NON_EMPTY_ARRAY);
    }
    if (obj instanceof Collection) {
      return COLLECTION;
    }
    if (obj instanceof Map) {
      return MAP;
    }
    if (obj instanceof Class<?>) {
      Class<?> clazz = (Class<?>) obj;
      return clazz.getName();
    }
    if (obj instanceof Charset) {
      Charset charset = (Charset) obj;
      return charset.name();
    }
    if (obj instanceof TimeZone) {
      TimeZone timeZone = (TimeZone) obj;
      return timeZone.getID();
    }
    if (obj instanceof ZoneId) {
      ZoneId zoneId = (ZoneId) obj;
      return zoneId.getId();
    }
    if (obj instanceof CharSequence) {
      CharSequence charSequence = (CharSequence) obj;
      return StringUtils.truncate(charSequence);
    }
    Class<?> type = obj.getClass();
    if (ClassUtils.isSimpleValueType(type)) {
      String str = obj.toString();
      if (str != null) {
        return StringUtils.truncate(str);
      }
    }
    return type.getTypeName() + "@" + getIdentityHexString(obj);
  }

  /**
   * Return a hex String form of an object's identity hash code.
   *
   * @param obj
   *          the object
   *
   * @return the object's identity code in hex notation
   */
  public static String getIdentityHexString(Object obj) {
    return Integer.toHexString(System.identityHashCode(obj));
  }

  /**
   * Return a String representation of the specified Object.
   * <p>
   * Builds a String representation of the contents in case of an array. Returns a {@code "null"} String if {@code obj}
   * is {@code null}.
   *
   * @param obj
   *          the object to build a String representation for
   *
   * @return a String representation of {@code obj}
   *
   * @see #nullSafeConciseToString(Object)
   */
  public static String nullSafeToString(@Nullable Object obj) {
    if (obj == null) {
      return NULL_STRING;
    }
    if (obj instanceof String) {
      String string = (String) obj;
      return string;
    }
    if (obj instanceof Object[]) {
      Object[] objects = (Object[]) obj;
      return nullSafeToString(objects);
    }
    if (obj instanceof boolean[]) {
      boolean[] booleans = (boolean[]) obj;
      return nullSafeToString(booleans);
    }
    if (obj instanceof byte[]) {
      byte[] bytes = (byte[]) obj;
      return nullSafeToString(bytes);
    }
    if (obj instanceof char[]) {
      char[] chars = (char[]) obj;
      return nullSafeToString(chars);
    }
    if (obj instanceof double[]) {
      double[] doubles = (double[]) obj;
      return nullSafeToString(doubles);
    }
    if (obj instanceof float[]) {
      float[] floats = (float[]) obj;
      return nullSafeToString(floats);
    }
    if (obj instanceof int[]) {
      int[] ints = (int[]) obj;
      return nullSafeToString(ints);
    }
    if (obj instanceof long[]) {
      long[] longs = (long[]) obj;
      return nullSafeToString(longs);
    }
    if (obj instanceof short[]) {
      short[] shorts = (short[]) obj;
      return nullSafeToString(shorts);
    }
    String str = obj.toString();
    return (str != null ? str : EMPTY_STRING);
  }
}

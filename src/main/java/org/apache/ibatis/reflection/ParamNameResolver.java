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
package org.apache.ibatis.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.binding.ParamMap;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParamNameResolver {

  public static final String GENERIC_NAME_PREFIX = "param";

  public static final String[] GENERIC_NAME_CACHE = new String[10];

  static {
    for (int i = 0; i < 10; i++) {
      GENERIC_NAME_CACHE[i] = GENERIC_NAME_PREFIX + (i + 1);
    }
  }

  private final boolean useActualParamName;

  /**
   * The key is the index and the value is the name of the parameter.<br />
   * The name is obtained from {@link Param} if specified. When {@link Param} is not specified, the parameter index is
   * used. Note that this index could be different from the actual index when the method has special parameters (i.e.
   * {@link RowBounds} or {@link ResultHandler}).
   * <ul>
   * <li>aMethod(@Param("M") int a, @Param("N") int b) -&gt; {{0, "M"}, {1, "N"}}</li>
   * <li>aMethod(int a, int b) -&gt; {{0, "0"}, {1, "1"}}</li>
   * <li>aMethod(int a, RowBounds rb, int b) -&gt; {{0, "0"}, {2, "1"}}</li>
   * </ul>
   */
  private final SortedMap<Integer, String> names;
  private final Map<String, Type> typeMap = new HashMap<>();

  private boolean hasParamAnnotation;
  private boolean useParamMap;

  public ParamNameResolver(Class<?> mapperClass, Method method, boolean useActualParamName) {
    this.useActualParamName = useActualParamName;
    final Class<?>[] paramTypes = method.getParameterTypes();
    final Annotation[][] paramAnnotations = method.getParameterAnnotations();
    final SortedMap<Integer, String> map = new TreeMap<>();
    Type[] actualParamTypes = TypeParameterResolver.resolveParamTypes(method, mapperClass);
    int paramCount = paramAnnotations.length;
    // get names from @Param annotations
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      if (isSpecialParameter(paramTypes[paramIndex])) {
        // skip special parameters
        continue;
      }
      String name = null;
      for (Annotation annotation : paramAnnotations[paramIndex]) {
        if (annotation instanceof Param) {
          hasParamAnnotation = true;
          useParamMap = true;
          name = ((Param) annotation).value();
          break;
        }
      }
      if (name == null) {
        // @Param was not specified.
        if (useActualParamName) {
          name = getActualParamName(method, paramIndex);
        }
        if (name == null) {
          // use the parameter index as the name ("0", "1", ...)
          // gcode issue #71
          name = String.valueOf(map.size());
        }
      }
      map.put(paramIndex, name);
      typeMap.put(name, actualParamTypes[paramIndex]);
    }
    names = Collections.unmodifiableSortedMap(map);
    if (names.size() > 1) {
      useParamMap = true;
    }
    if (names.size() == 1) {
      Type soleParamType = actualParamTypes[0];
      if (soleParamType instanceof GenericArrayType) {
        typeMap.put("array", soleParamType);
      } else {
        Class<?> soleParamClass = resolveSingleParameterClass(method, soleParamType);
        if (Collection.class.isAssignableFrom(soleParamClass)) {
          typeMap.put("collection", soleParamType);
          if (List.class.isAssignableFrom(soleParamClass)) {
            typeMap.put("list", soleParamType);
          }
        }
      }
    }
  }

  public static @NotNull Class<?> resolveSingleParameterClass(Method method, Type soleParamType) {
    Class<?> soleParamClass = null;
    if (soleParamType instanceof ParameterizedType) {
      soleParamClass = (Class<?>) ((ParameterizedType) soleParamType).getRawType();
    } else if (soleParamType instanceof Class) {
      soleParamClass = (Class<?>) soleParamType;
    }
    if (soleParamClass == null) {
      throw new BindingException("Unable to resolve the class of the sole parameter for method " + method
          + ". This usually happens when the parameter type cannot be "
          + "determined (e.g., due to missing generic type information or the "
          + "`-parameters` compiler flag). Verify the method signature and that the " + "parameter type is concrete.");
    }
    return soleParamClass;
  }

  private String getActualParamName(Method method, int paramIndex) {
    return ReflectionUtils.getParamNames(method).get(paramIndex);
  }

  private static boolean isSpecialParameter(Class<?> clazz) {
    return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
  }

  /**
   * Returns parameter names referenced by SQL providers.
   *
   * @return the names
   */
  public String[] getNames() {
    return names.values().toArray(new String[0]);
  }

  /**
   * A single non-special parameter is returned without a name. Multiple parameters are named using the naming rule. In
   * addition to the default names, this method also adds the generic names (param1, param2, ...).
   *
   * @param args
   *          the args
   *
   * @return the named params
   */
  public Object getNamedParams(Object[] args, boolean nullValueWhenKeyNotFoundInParamMap) {
    final int paramCount = names.size();
    if (args == null || paramCount == 0) {
      return null;
    }
    if (!hasParamAnnotation && paramCount == 1) {
      Object value = args[names.firstKey()];
      return wrapToMapIfCollection(value, useActualParamName ? names.get(names.firstKey()) : null,
          nullValueWhenKeyNotFoundInParamMap);
    } else {
      final Map<String, Object> param = new ParamMap(nullValueWhenKeyNotFoundInParamMap);
      int i = 0;
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        param.put(entry.getValue(), args[entry.getKey()]);
        // add generic param names (param1, param2, ...)
        final String genericParamName = i < 10 ? GENERIC_NAME_CACHE[i] : GENERIC_NAME_PREFIX + (i + 1);
        // ensure not to overwrite parameter named with @Param
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      return param;
    }
  }

  public Type getType(String name) {
    PropertyTokenizer propertyTokenizer = new PropertyTokenizer(name);
    String unindexed = propertyTokenizer.getName();
    Type type = typeMap.get(unindexed);

    if (type == null && unindexed.startsWith(GENERIC_NAME_PREFIX)) {
      try {
        int paramIndex = Integer.parseInt(unindexed.substring(GENERIC_NAME_PREFIX.length())) - 1;
        unindexed = names.get(paramIndex);
        if (unindexed != null) {
          type = typeMap.get(unindexed);
        }
      } catch (NumberFormatException e) {
        // user mistake
      }
    }

    if (propertyTokenizer.getIndex() != null) {
      if (type instanceof ParameterizedType) {
        Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
        return typeArgs[0];
      } else if (type instanceof Class && ((Class<?>) type).isArray()) {
        return ((Class<?>) type).getComponentType();
      }
    }
    return type;
  }

  public static ParamNameResolver resolve(Method method, boolean useActualParamName) {
    Objects.requireNonNull(method, "method is null");
    return new ParamNameResolver(method.getDeclaringClass(), method, useActualParamName);
  }

  public static ParamNameResolver resolve(Class<?> mapperClass, Method method, boolean useActualParamName) {
    Objects.requireNonNull(mapperClass, "mapperClass is null");
    Objects.requireNonNull(method, "method is null");
    return new ParamNameResolver(mapperClass, method, useActualParamName);
  }

  public static Object wrapToMapIfCollection(@Nullable Object object) {
    return wrapToMapIfCollection(object, null, false);
  }

  public static Object wrapToMapIfCollection(@Nullable Object object, boolean nullValueWhenKeyNotFoundInParamMap) {
    return wrapToMapIfCollection(object, null, nullValueWhenKeyNotFoundInParamMap);
  }

  /**
   * Wrap to a {@link ParamMap} if object is {@link Collection} or array.
   *
   * @param object
   *          a parameter object
   * @param actualParamName
   *          an actual parameter name (If specify a name, set an object to {@link ParamMap} with specified name)
   *
   * @return a {@link ParamMap}
   *
   * @since 3.5.5
   */
  public static Object wrapToMapIfCollection(@Nullable Object object, String actualParamName,
      boolean nullValueWhenKeyNotFoundInParamMap) {
    if (object instanceof Collection) {
      ParamMap map = new ParamMap(nullValueWhenKeyNotFoundInParamMap);
      map.put("collection", object);
      if (object instanceof List) {
        map.put("list", object);
      }
      if (actualParamName != null) {
        map.put(actualParamName, object);
      }
      return map;
    }
    if (object != null && object.getClass().isArray()) {
      ParamMap map = new ParamMap(nullValueWhenKeyNotFoundInParamMap);
      map.put("array", object);
      if (actualParamName != null) {
        map.put(actualParamName, object);
      }
      return map;
    }
    return object;
  }

  public boolean isUseParamMap() {
    return useParamMap;
  }
}

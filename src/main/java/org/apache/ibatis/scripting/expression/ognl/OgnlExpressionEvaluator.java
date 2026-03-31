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
package org.apache.ibatis.scripting.expression.ognl;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ognl.*;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.internal.util.CollectionUtils;
import org.apache.ibatis.scripting.ContextMap;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.scripting.expression.ExtensionFunction;
import org.apache.ibatis.scripting.xmltags.DynamicContext;

/**
 * @author Clinton Begin
 */
public class OgnlExpressionEvaluator implements ExpressionEvaluator {

  public static final OgnlExpressionEvaluator INSTANCE = new OgnlExpressionEvaluator();

  private final Map<String, ExtensionFunction> functionMap = new HashMap<>();

  public OgnlExpressionEvaluator() {
    OgnlRuntime.setMethodAccessor(Object.class, new ObjectMethodAccessor(functionMap));
  }

  @Override
  public Object getValue(String expression, Object root) {
    return OgnlCache.getValue(expression, root);
  }

  @Override
  public boolean evaluateBoolean(String expression, Object parameterObject) {
    Object value = OgnlCache.getValue(expression, parameterObject);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof Number) {
      return new BigDecimal(String.valueOf(value)).compareTo(BigDecimal.ZERO) != 0;
    }
    return value != null;
  }

  /**
   * @since 3.5.9
   */
  @Override
  public Iterable<?> evaluateIterable(String expression, Object parameterObject, boolean nullable) {
    Object value = OgnlCache.getValue(expression, parameterObject);
    if (value == null) {
      if (nullable) {
        return null;
      }
      throw new BuilderException("The expression '" + expression + "' evaluated to a null value.");
    }
    if (value instanceof Iterable) {
      return (Iterable<?>) value;
    }
    if (value.getClass().isArray()) {
      // the array may be primitive, so Arrays.asList() may throw
      // a ClassCastException (issue 209). Do the work manually
      // Curse primitives! :) (JGB)
      int size = Array.getLength(value);
      List<Object> answer = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        Object o = Array.get(value, i);
        answer.add(o);
      }
      return answer;
    }
    if (value instanceof Map) {
      return ((Map<?, ?>) value).entrySet();
    }
    throw new BuilderException(
        "Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
  }

  @Override
  public void registerFunction(ExtensionFunction function) {
    functionMap.put(function.getName(), function);
  }

  static {
    OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
  }

  static class ContextAccessor implements PropertyAccessor {

    @Override
    public Object getProperty(OgnlContext context, Object target, Object name) {
      Map<?, ?> map = (Map<?, ?>) target;

      Object result = map.get(name);
      if (map.containsKey(name) || result != null) {
        return result;
      }

      Object parameterObject = map.get(DynamicContext.PARAMETER_OBJECT_KEY);
      if (parameterObject instanceof Map) {
        return ((Map<?, ?>) parameterObject).get(name);
      }

      return null;
    }

    @Override
    public void setProperty(OgnlContext context, Object target, Object name, Object value) {
      @SuppressWarnings("unchecked")
      Map<Object, Object> map = (Map<Object, Object>) target;
      map.put(name, value);
    }

    @Override
    public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }

    @Override
    public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }
  }

  /**
   * @see ognl.ObjectMethodAccessor
   */
  static class ObjectMethodAccessor implements MethodAccessor {

    private final MethodAccessor objectMethodAccessor;

    private final Map<String, ExtensionFunction> functionMap;

    ObjectMethodAccessor(Map<String, ExtensionFunction> functionMap) {
      this.functionMap = functionMap;
      try {
        this.objectMethodAccessor = OgnlRuntime.getMethodAccessor(Object.class);
      } catch (OgnlException e) {
        throw new BuilderException("Error setting internal method accessor for type: " + Object.class.getName(), e);
      }
    }

    @Override
    public Object callStaticMethod(OgnlContext context, Class<?> targetClass, String methodName, Object[] args)
        throws MethodFailedException {
      return objectMethodAccessor.callStaticMethod(context, targetClass, methodName, args);
    }

    @Override
    public Object callMethod(OgnlContext context, Object target, String methodName, Object[] args)
        throws MethodFailedException {
      Class<?> targetClass = (target == null) ? null : target.getClass();
      List<Method> methods = OgnlRuntime.getMethods(targetClass, methodName, false);
      if (CollectionUtils.isEmpty(methods)) {
        // static methods
        methods = OgnlRuntime.getMethods(targetClass, methodName, true);
      }
      final ExtensionFunction function = functionMap.get(methodName);
      if (function != null && function.supports(target)) {
        boolean callMethodOnTarget = false;
        if (CollectionUtils.isNotEmpty(methods)) {
          final Class<?>[] functionParameterTypes = function.getParameterTypes();
          for (Method method : methods) {
            if (method.getParameterCount() == function.getParameterCount()) {
              if (isCompatible(functionParameterTypes, method.getParameterTypes())) {
                callMethodOnTarget = true;
                break;
              }
            }
          }
        }
        if (!callMethodOnTarget) {
          return function.execute(args);
        }
      }
      // fallback to method already defined in the class
      return OgnlRuntime.callAppropriateMethod(context, target, target, methodName, null, methods, args);
    }

    private static boolean isCompatible(Class<?>[] parameterTypes, Class<?>[] targetParameterTypes) {
      if (parameterTypes.length != targetParameterTypes.length) {
        return false;
      }
      for (int i = 0; i < parameterTypes.length; i++) {
        if (!isAssignableFrom(parameterTypes[i], targetParameterTypes[i])) {
          return false;
        }
      }
      return true;
    }

    private static boolean isAssignableFrom(Class<?> type1, Class<?> type2) {
      if (type1 == type2) {
        return true;
      }
      // TODO consider primitive types ?
      return type1.isAssignableFrom(type2);
    }
  }
}

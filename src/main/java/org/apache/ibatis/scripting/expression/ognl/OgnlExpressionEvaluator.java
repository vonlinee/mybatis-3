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

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import ognl.OgnlContext;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.internal.util.LambdaUtils;
import org.apache.ibatis.internal.util.ObjectUtils;
import org.apache.ibatis.internal.util.function.ThrowableFunction;
import org.apache.ibatis.scripting.ContextMap;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.scripting.expression.ExpressionException;

/**
 * @author Clinton Begin
 */
public class OgnlExpressionEvaluator implements ExpressionEvaluator {

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
  public String postProcessExpression(String expression) {
    return qualifyFunctionCalls(expression, ObjectUtils.class);
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

      Object parameterObject = map.get(SqlBuildContext.PARAMETER_OBJECT_KEY);
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

  public static String toStaticCallExpression(Class<?> type, String method) {
    return toStaticCallExpression(type.getName(), method);
  }

  public static String toStaticCallExpression(String typeName, String method) {
    return "@" + typeName + "@" + method;
  }

  public static <T, R> String toStaticCallExpression(ThrowableFunction<T, R> methodRef) {
    try {
      SerializedLambda lambda = LambdaUtils.getSerializedLambda(methodRef);
      String className = lambda.getImplClass().replace('/', '.');
      String methodName = lambda.getImplMethodName();
      int methodKind = lambda.getImplMethodKind();
      // methodKind: 6 = static method (REF_invokeStatic)
      if (methodKind != 6) {
        throw new IllegalArgumentException("not static method: " + methodKind);
      }
      return toStaticCallExpression(className, methodName);
    } catch (Exception e) {
      throw new ExpressionException("cannot parse Lambda method reference", e);
    }
  }

  public static String toStaticCallExpression(Class<?> type, String method, String... argExpressions) {
    return "@" + type.getName() + "@" + method + "(" + String.join(",", argExpressions) + ")";
  }

  public static <T, R> String toStaticCallExpression(ThrowableFunction<T, R> function, String... argNames) {
    StringJoiner args = new StringJoiner(",", "(", ")");
    for (String argName : argNames) {
      args.add(argName);
    }
    return toStaticCallExpression(function) + args;
  }

  public static String qualifyFunctionCalls(String expression, Class<?> type) {
    return qualifyFunctionCalls(expression, type.getName(), null);
  }

  /**
   * Qualifies function calls in an OGNL expression with the default function class.
   *
   * @param expression
   *          the original OGNL expression
   * @param defaultFunctionClass
   *          the fully qualified name of the class containing the default functions
   *
   * @return the OGNL expression with unqualified function calls converted to static calls
   */
  public static String qualifyFunctionCalls(String expression, String defaultFunctionClass) {
    return qualifyFunctionCalls(expression, defaultFunctionClass, null);
  }

  /**
   * Qualifies function calls in an OGNL expression with their target function classes.
   * <p>
   * An unqualified call such as {@code @isEmpty(value)} is converted to {@code @com.example.Functions@isEmpty(value)}.
   * Existing OGNL static calls and function-like text inside string literals are left unchanged.
   *
   * @param expression
   *          the original OGNL expression
   * @param defaultFunctionClass
   *          the fully qualified name of the fallback function class
   * @param functionClassMap
   *          a map from function names to fully qualified class names, or {@code null}
   *
   * @return the OGNL expression with unqualified function calls converted to static calls
   *
   * @throws IllegalArgumentException
   *           if a function call has neither a mapped class nor a valid default class
   */
  public static String qualifyFunctionCalls(String expression, String defaultFunctionClass,
      Map<String, String> functionClassMap) {
    if (expression == null || expression.isEmpty()) {
      return expression;
    }
    final int length = expression.length();

    StringBuilder out = new StringBuilder(length + 32);
    int i = 0;
    while (i < length) {
      char c = expression.charAt(i);
      // Copy string literals as-is so that text such as "@isEmpty(value)" is not treated as a function call.
      if (c == '\'' || c == '"') {
        out.append(c);
        i++;
        while (i < length) {
          char ch = expression.charAt(i);
          out.append(ch);

          // Preserve escaped characters so an escaped quote cannot terminate the literal early.
          if (ch == '\\' && i + 1 < length) {
            out.append(expression.charAt(i + 1));
            i += 2;
            continue;
          }
          i++;
          if (ch == c) {
            break;
          }
        }
        continue;
      }

      // Look for the shorthand OGNL function-call form: @functionName(...).
      if (c == '@' && i + 1 < length) {
        int start = i + 1;
        if (Character.isJavaIdentifierStart(expression.charAt(start))) {
          int j = start + 1;
          // Consume the complete Java identifier so names containing digits or underscores are supported.
          while (j < length && Character.isJavaIdentifierPart(expression.charAt(j))) {
            j++;
          }
          // A function call is recognized only when the identifier is immediately followed by '('.
          if (j < length && expression.charAt(j) == '(') {
            // Skip the method part of an existing @class@method(...) static call, including calls with whitespace.
            if (!isExistingStaticCall(expression, i)) {
              String functionName = expression.substring(start, j);

              // A function-specific mapping takes precedence over the default class.
              String targetClass = functionClassMap != null
                  ? functionClassMap.getOrDefault(functionName, defaultFunctionClass) : defaultFunctionClass;

              if (targetClass == null || targetClass.trim().isEmpty()) {
                throw new IllegalArgumentException("cannot find class of function " + functionName);
              }

              out.append('@').append(targetClass.trim()).append('@').append(functionName);
              // Leave '(' for the next iteration so the original argument list is copied unchanged.
              i = j;
              continue;
            }
          }
        }
      }
      out.append(c);
      i++;
    }
    return out.toString();
  }

  private static boolean isExistingStaticCall(String expression, int methodAt) {
    int index = methodAt - 1;
    while (index >= 0 && Character.isWhitespace(expression.charAt(index))) {
      index--;
    }
    if (index < 0 || !Character.isJavaIdentifierPart(expression.charAt(index))) {
      return false;
    }
    // Walk backward through the class name, allowing whitespace around package separators.
    while (index >= 0) {
      while (index >= 0 && Character.isJavaIdentifierPart(expression.charAt(index))) {
        index--;
      }
      while (index >= 0 && Character.isWhitespace(expression.charAt(index))) {
        index--;
      }
      if (index < 0 || expression.charAt(index) != '.') {
        break;
      }
      do {
        index--;
      } while (index >= 0 && Character.isWhitespace(expression.charAt(index)));
    }
    while (index >= 0 && Character.isWhitespace(expression.charAt(index))) {
      index--;
    }
    return index >= 0 && expression.charAt(index) == '@';
  }
}

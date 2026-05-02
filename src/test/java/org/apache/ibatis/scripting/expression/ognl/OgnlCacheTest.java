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

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.apache.ibatis.internal.util.ObjectUtils;
import org.apache.ibatis.internal.util.StringUtils;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.apache.ibatis.scripting.expression.ExtensionMethod;
import org.junit.jupiter.api.Test;

class OgnlCacheTest {
  @Test
  void concurrentAccess() throws Exception {
    class DataClass {
      @SuppressWarnings("unused")
      private int id;
    }
    int run = 1000;
    Map<String, Object> context = new HashMap<>();
    List<Future<Object>> futures = new ArrayList<>();
    context.put("data", new DataClass());
    ExecutorService executor = Executors.newCachedThreadPool();
    IntStream.range(0, run).forEach(i -> futures.add(executor.submit(() -> OgnlCache.getValue("data.id", context))));
    for (int i = 0; i < run; i++) {
      assertNotNull(futures.get(i).get());
    }
    executor.shutdown();
  }

  @Test
  void issue2609() throws Exception {
    Map<String, Object> context = new HashMap<>();
    context.put("d1", Date.valueOf("2022-01-01"));
    context.put("d2", Date.valueOf("2022-01-02"));
    assertEquals(-1, OgnlCache.getValue("d1.compareTo(d2)", context));
  }

  static class Param {

    private String field1;
    private String field2;

    public String getField1() {
      return field1;
    }

    public void setField1(String field1) {
      this.field1 = field1;
    }

    public String getField2() {
      return field2;
    }

    public void setField2(String field2) {
      this.field2 = field2;
    }
  }

  @Test
  void staticMethodCallInOgnlExpression() {
    final ExpressionEvaluator evaluator = new OgnlExpressionEvaluator();

    Map<String, Object> context = new HashMap<>();
    Param param = new Param();
    param.setField1("xxx");
    param.setField2("  ");
    context.put("param", param);

    assertFalse(evaluator.evaluateBoolean(getStaticMethodCallExpression(ObjectUtils.class, "isEmpty", "param.field1"),
        context));
    assertFalse(evaluator.evaluateBoolean(getStaticMethodCallExpression(StringUtils.class, "isEmpty", "param.field2"),
        context));
    assertFalse(evaluator.evaluateBoolean(getStaticMethodCallExpression(StringUtils.class, "isBlank", "param.field1"),
        context));
    assertTrue(evaluator.evaluateBoolean(getStaticMethodCallExpression(StringUtils.class, "isNotBlank", "param.field1"),
        context));
    assertFalse(evaluator.evaluateBoolean(
        getStaticMethodCallExpression(StringUtils.class, "isAllBlank", "param.field1", "param.field2"), context));
    assertTrue(evaluator.evaluateBoolean(
        getStaticMethodCallExpression(StringUtils.class, "isAnyBlank", "param.field1", "param.field2"), context));
  }

  static String getStaticMethodCallExpression(Class<?> type, String method, String... argExpressions) {
    return "@" + type.getName() + "@" + method + "(" + String.join(",", argExpressions) + ")";
  }

  enum BultinExtensionMethod implements ExtensionMethod {

    isEmpty {
      @Override
      public String getName() {
        return "isEmpty";
      }

      @Override
      public int getParameterCount() {
        return 1;
      }

      @Override
      public Class<?>[] getParameterTypes() {
        return new Class[] { Object.class };
      }

      @Override
      public Object invoke(Object target, Object[] args) {
        return ObjectUtils.isEmpty(args[0]);
      }
    },

    isNotEmpty {
      @Override
      public String getName() {
        return "isNotEmpty";
      }

      @Override
      public int getParameterCount() {
        return 1;
      }

      @Override
      public Class<?>[] getParameterTypes() {
        return new Class[] { Object.class };
      }

      @Override
      public Object invoke(Object target, Object[] args) {
        return !ObjectUtils.isEmpty(args[0]);
      }
    },

    isBlank {
      @Override
      public String getName() {
        return "isBlank";
      }

      @Override
      public int getParameterCount() {
        return 1;
      }

      @Override
      public Class<?>[] getParameterTypes() {
        return new Class[] { String.class };
      }

      @Override
      public Object invoke(Object target, Object[] args) {
        if (!(args[0] instanceof String)) {
          throw new IllegalArgumentException("args[0] must be String");
        }
        return StringUtils.isBlank((String) args[0]);
      }
    },

    isNotBlank {
      @Override
      public String getName() {
        return "isNotBlank";
      }

      @Override
      public int getParameterCount() {
        return 1;
      }

      @Override
      public Class<?>[] getParameterTypes() {
        return new Class[] { String.class };
      }

      @Override
      public Object invoke(Object target, Object[] args) {
        if (!(args[0] instanceof String)) {
          throw new IllegalArgumentException("args[0] must be String");
        }
        return StringUtils.isNotBlank((String) args[0]);
      }
    },
  }

  @Test
  void customExtensionMethodInExpression() {
    final ExpressionEvaluator evaluator = new OgnlExpressionEvaluator();

    evaluator.setSupportExtensionMethods(true);

    evaluator.registerMethod(BultinExtensionMethod.isEmpty);
    evaluator.registerMethod(BultinExtensionMethod.isNotEmpty);
    evaluator.registerMethod(BultinExtensionMethod.isBlank);
    evaluator.registerMethod(BultinExtensionMethod.isNotBlank);

    Map<String, Object> context = new HashMap<>();
    Param param = new Param();
    param.setField1("xxx");
    param.setField2("  ");
    context.put("param", param);

    assertFalse(evaluator.evaluateBoolean("isEmpty(param.field1)", context));
    assertFalse(evaluator.evaluateBoolean("isEmpty(param.field1)", context));
    assertTrue(evaluator.evaluateBoolean("isNotEmpty(param.field1)", context));
    assertTrue(evaluator.evaluateBoolean("isNotBlank(param.field1)", context));
  }
}

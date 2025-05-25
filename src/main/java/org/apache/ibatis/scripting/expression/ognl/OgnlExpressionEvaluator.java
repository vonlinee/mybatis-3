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
package org.apache.ibatis.scripting.expression.ognl;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import ognl.ASTAnd;
import ognl.ASTAssign;
import ognl.ASTChain;
import ognl.ASTConst;
import ognl.ASTEval;
import ognl.ASTGreater;
import ognl.ASTGreaterEq;
import ognl.ASTIn;
import ognl.ASTInstanceof;
import ognl.ASTKeyValue;
import ognl.ASTLess;
import ognl.ASTLessEq;
import ognl.ASTList;
import ognl.ASTMap;
import ognl.ASTNot;
import ognl.ASTNotEq;
import ognl.ASTNotIn;
import ognl.ASTOr;
import ognl.ASTProperty;
import ognl.ASTSelect;
import ognl.ASTSelectFirst;
import ognl.ASTSelectLast;
import ognl.ASTSequence;
import ognl.ASTStaticField;
import ognl.ASTTest;
import ognl.ASTVarRef;
import ognl.BooleanExpression;
import ognl.ComparisonExpression;
import ognl.ExpressionNode;
import ognl.Node;
import ognl.NumericExpression;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;
import ognl.SimpleNode;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.scripting.ContextMap;
import org.apache.ibatis.scripting.SqlBuildContext;
import org.apache.ibatis.scripting.expression.ExpressionEvaluator;
import org.jetbrains.annotations.NotNull;

/**
 * @author Clinton Begin
 */
public class OgnlExpressionEvaluator implements ExpressionEvaluator {

  public static final OgnlExpressionEvaluator INSTANCE = new OgnlExpressionEvaluator();

  static {
    OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
  }

  @Override
  public Object getValue(String expression, Object parameterObject) {
    return OgnlCache.getValue(expression, parameterObject);
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

  @Override
  public Iterable<?> evaluateIterable(String expression, Object parameterObject) {
    return evaluateIterable(expression, parameterObject, false);
  }

  /**
   * @since 3.5.9
   */
  @Override
  public Iterable<?> evaluateIterable(String expression, Object parameterObject, boolean nullable) {
    Object value = getValue(expression, parameterObject);
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
  public List<ParameterMapping> collectParameters(@NotNull String expression) {
    try {
      Node node = (Node) Ognl.parseExpression(expression);
      if (!(node instanceof SimpleNode)) {
        return Collections.emptyList();
      }
      SimpleNode rootNode = (SimpleNode) node;
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      collectParameterMappings(rootNode, parameterMappings);
      return parameterMappings;
    } catch (OgnlException e) {
      throw new ExecutorException(e);
    }
  }

  private void collectParameterMappings(Node node, List<ParameterMapping> parameterMappings) {
    if (!(node instanceof SimpleNode)) {
      return;
    }
    SimpleNode simpleNode = (SimpleNode) node;
    if (simpleNode instanceof ExpressionNode) {
      if (simpleNode instanceof BooleanExpression) {
        collectParametersFromBooleanExpression((BooleanExpression) simpleNode, parameterMappings);
      } else if (simpleNode instanceof ASTTest) {
        // TODO
      } else if (simpleNode instanceof NumericExpression) {
        collectParametersFromNumericExpression((NumericExpression) simpleNode, parameterMappings);
      }
    } else if (simpleNode instanceof ASTVarRef) {
      // TODO ASTVarRef

    } else {
      // simple node
      collectParametersFromSimpleNode(simpleNode, parameterMappings);
    }
  }

  private void collectParametersFromBooleanExpression(BooleanExpression booleanExpression,
      List<ParameterMapping> parameterMappings) {
    if (booleanExpression instanceof ComparisonExpression) {
      collectParametersFromComparisonExpression((ComparisonExpression) booleanExpression, parameterMappings);
    } else if (booleanExpression instanceof ASTOr) {
      collectParametersFromChildren(booleanExpression, parameterMappings);
    } else if (booleanExpression instanceof ASTAnd) {
      collectParametersFromChildren(booleanExpression, parameterMappings);
    } else if (booleanExpression instanceof ASTNot) {
      collectParametersFromChildren(booleanExpression, parameterMappings);
    }
  }

  private void collectParametersFromComparisonExpression(ComparisonExpression expression,
      List<ParameterMapping> parameterMappings) {
    if (expression instanceof ASTNotEq) {
      collectParametersFromChildren(expression, parameterMappings);
    } else if (expression instanceof ASTLessEq) {
      collectParametersFromChildren(expression, parameterMappings);
    } else if (expression instanceof ASTLess) {
      collectParametersFromChildren(expression, parameterMappings);
    } else if (expression instanceof ASTGreater) {
      collectParametersFromChildren(expression, parameterMappings);
    } else if (expression instanceof ASTGreaterEq) {
      collectParametersFromChildren(expression, parameterMappings);
    }
  }

  private void collectParametersFromSimpleNode(SimpleNode simpleNode, List<ParameterMapping> parameterMappings) {
    if (simpleNode instanceof ASTChain) {
      ASTChain astChain = (ASTChain) simpleNode;
      // does not cover all case
      StringJoiner sb = new StringJoiner(".");
      for (int i = 0; i < astChain.jjtGetNumChildren(); i++) {
        if (astChain.jjtGetChild(i) instanceof ASTProperty) {
          ASTProperty node = (ASTProperty) astChain.jjtGetChild(i);
          String property = getProperty(node);
          sb.add(property);
        } else {
          break;
        }
      }
      if (!(sb.length() == 0)) {
        ParameterMapping pm = new ParameterMapping();
        pm.setProperty(sb.toString());
        parameterMappings.add(pm);
      }
    } else if (simpleNode instanceof ASTMap) {
      // TODO
    } else if (simpleNode instanceof ASTIn) {
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTNotIn) {
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTProperty) {
      ASTProperty astProperty = (ASTProperty) simpleNode;
      if (astProperty.jjtGetNumChildren() > 0) {
        Node node = astProperty.jjtGetChild(0);
        if (node instanceof ASTConst) {
          ASTConst astConst = (ASTConst) node;
          ParameterMapping pm = new ParameterMapping();
          pm.setProperty(String.valueOf(astConst.getValue()));
          parameterMappings.add(pm);
        }
      }
    } else if (simpleNode instanceof ASTConst) {
      // TODO
    } else if (simpleNode instanceof ASTAssign) {
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTSelect) {
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTSelectFirst) {
      // TODO
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTList) {
      // TODO
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTEval) {
      // TODO
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTStaticField) {
      // TODO
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTSequence) {
      // TODO
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTSelectLast) {
      // TODO
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTKeyValue) {
      // TODO
      collectParametersFromChildren(simpleNode, parameterMappings);
    } else if (simpleNode instanceof ASTInstanceof) {
      // TODO
      collectParametersFromChildren(simpleNode, parameterMappings);
    }
  }

  private void collectParametersFromNumericExpression(NumericExpression numericExpression,
      List<ParameterMapping> parameterMappings) {
    // TODO
  }

  private String getProperty(ASTProperty astProperty) {
    int i = astProperty.jjtGetNumChildren();
    if (i > 0) {
      Node node = astProperty.jjtGetChild(0);
      if (node instanceof ASTConst) {
        return String.valueOf(((ASTConst) node).getValue());
      }
      return "";
    }
    return "";
  }

  private void collectParametersFromChildren(Node node, List<ParameterMapping> parameterMappings) {
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      collectParameterMappings(node.jjtGetChild(i), parameterMappings);
    }
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
}

package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.TokenType;

/**
 * A binary arithmetic expression like "a + b" or "c ^ d".
 */
public class OperatorExpression implements Expression {
    private final Expression mLeft;
    private final TokenType  mOperator;
    private final Expression mRight;

    public OperatorExpression(Expression left, TokenType operator, Expression right) {
        mLeft = left;
        mOperator = operator;
        mRight = right;
    }
  
    @Override
    public void print(StringBuilder builder) {
        builder.append("(");
        mLeft.print(builder);
        builder.append(" ").append(mOperator.punctuator()).append(" ");
        mRight.print(builder);
        builder.append(")");
    }
}

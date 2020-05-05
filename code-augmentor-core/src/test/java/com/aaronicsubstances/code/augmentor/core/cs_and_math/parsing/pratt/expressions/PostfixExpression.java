package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.TokenType;

/**
 * A postfix unary arithmetic expression like "a!".
 */
public class PostfixExpression implements Expression {
    private final Expression mLeft;
    private final TokenType  mOperator;

    public PostfixExpression(Expression left, TokenType operator) {
        mLeft = left;
        mOperator = operator;
    }
  
    @Override
    public void print(StringBuilder builder) {
        builder.append("(");
        mLeft.print(builder);
        builder.append(mOperator.punctuator()).append(")");
    }
}

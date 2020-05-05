package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.TokenType;

/**
 * A prefix unary arithmetic expression like "!a" or "-b".
 */
public class PrefixExpression implements Expression {
    private final TokenType  mOperator;
    private final Expression mRight;
    
    public PrefixExpression(TokenType operator, Expression right) {
        mOperator = operator;
        mRight = right;
    }
  
    @Override
    public void print(StringBuilder builder) {
        builder.append("(").append(mOperator.punctuator());
        mRight.print(builder);
        builder.append(")");
    }
}

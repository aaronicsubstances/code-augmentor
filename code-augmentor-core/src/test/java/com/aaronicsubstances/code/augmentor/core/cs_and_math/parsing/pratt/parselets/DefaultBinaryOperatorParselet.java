package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.BinaryOperatorParselet;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.DefaultToken;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.OperatorExpression;

/**
 * Generic infix parselet for a binary arithmetic operator. The only
 * difference when parsing, "+", "-", "*", "/", and "^" is precedence and
 * associativity, so we can use a single parselet class for all of those.
 */
public class DefaultBinaryOperatorParselet extends BinaryOperatorParselet<DefaultToken, Expression> {
    public DefaultBinaryOperatorParselet(int precedence, boolean isRight) {
        super(precedence, isRight);
    }

    @Override
    protected Expression createParseExpression(Expression left, DefaultToken token, Expression right) {
        return new OperatorExpression(left, token.getEnumType(), right);
    }
}
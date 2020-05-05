package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.DefaultToken;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.PostfixOperatorParselet;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.PostfixExpression;

/**
 * Generic infix parselet for an unary arithmetic operator. Parses postfix
 * unary "?" expressions.
 */
public class DefaultPostfixOperatorParselet extends PostfixOperatorParselet<DefaultToken, Expression> {
    public DefaultPostfixOperatorParselet(int precedence) {
        super(precedence);
    }

    @Override
    protected Expression createParseExpression(Expression left, DefaultToken token) {
        return new PostfixExpression(left, token.getEnumType());
    }
}
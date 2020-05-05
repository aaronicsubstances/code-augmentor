package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.DefaultToken;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.PrefixOperatorParselet;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.PrefixExpression;

/**
 * Generic prefix parselet for an unary arithmetic operator. Parses prefix unary
 * "-", "+", "~", and "!" expressions.
 */
public class DefaultPrefixOperatorParselet extends PrefixOperatorParselet<DefaultToken, Expression> {
    
    public DefaultPrefixOperatorParselet(int precedence) {
        super(precedence);
    }

    @Override
    protected Expression createParseExpression(DefaultToken token, Expression right) {
        return new PrefixExpression(token.getEnumType(), right);
    }
}
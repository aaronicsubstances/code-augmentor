package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.DefaultToken;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.GroupParselet;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.TokenType;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;

/**
 * Parses parentheses used to group an expression, like "a * (b + c)".
 */
public class DefaultGroupParselet extends GroupParselet<DefaultToken, Expression> {

    public DefaultGroupParselet() {
        super(TokenType.RIGHT_PAREN.ordinal());
    }

    @Override
    protected Expression createParseExpression(DefaultToken left, Expression expression, 
            DefaultToken right) {
        return expression;
    }
}

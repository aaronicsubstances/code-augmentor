package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.DefaultToken;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.InfixParselet;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.PrattParser;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.Precedence;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.TokenType;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.ConditionalExpression;

/**
 * Parselet for the condition or "ternary" operator, like "a ? b : c".
 */
public class ConditionalParselet implements InfixParselet<DefaultToken, Expression> {
    @Override
    public Expression parse(PrattParser<DefaultToken, Expression> parser, Expression left, 
            DefaultToken token) {
        Expression thenArm = parser.parseExpression();
        parser.consume(TokenType.COLON.ordinal());
        Expression elseArm = parser.parseExpression(Precedence.CONDITIONAL - 1);
      
        return new ConditionalExpression(left, thenArm, elseArm);
    }

    @Override
    public int getPrecedence() {
        return Precedence.CONDITIONAL;
    }
}
package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.DefaultToken;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.InfixParselet;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.PrattParser;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.Precedence;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.TokenType;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.CallExpression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;

/**
 * Parselet to parse a function call like "a(b, c, d)".
 */
public class CallParselet implements InfixParselet<DefaultToken, Expression> {
    
    @Override
    public Expression parse(PrattParser<DefaultToken, Expression> parser, Expression left, 
            DefaultToken token) {
        // Parse the comma-separated arguments until we hit, ")".
        List<Expression> args = new ArrayList<Expression>();
    
        // There may be no arguments at all.
        if (parser.match(TokenType.RIGHT_PAREN.ordinal()) == null) {
            do {
                args.add(parser.parseExpression());
            } while (parser.match(TokenType.COMMA.ordinal()) != null);
            parser.consume(TokenType.RIGHT_PAREN.ordinal());
        }
    
        return new CallExpression(left, args);
    }

    @Override
    public int getPrecedence() {
        return Precedence.CALL;
    }
}
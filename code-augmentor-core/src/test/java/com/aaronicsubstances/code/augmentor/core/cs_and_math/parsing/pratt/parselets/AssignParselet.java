package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.DefaultToken;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.InfixParselet;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.ParseException;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.PrattParser;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.Precedence;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.AssignExpression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.NameExpression;

/**
 * Parses assignment expressions like "a = b". The left side of an assignment
 * expression must be a simple name like "a", and expressions are
 * right-associative. (In other words, "a = b = c" is parsed as "a = (b = c)").
 */
public class AssignParselet implements InfixParselet<DefaultToken, Expression> {
  
    @Override
    public Expression parse(PrattParser<DefaultToken, Expression> parser, Expression left, 
            DefaultToken token) {
        Expression right = parser.parseExpression(Precedence.ASSIGNMENT - 1);
    
        if (!(left instanceof NameExpression)) throw new ParseException(
            "The left-hand side of an assignment must be a name.");
    
        String name = ((NameExpression) left).getName();
        return new AssignExpression(name, right);
    }

    @Override
    public int getPrecedence() { return Precedence.ASSIGNMENT; }
}
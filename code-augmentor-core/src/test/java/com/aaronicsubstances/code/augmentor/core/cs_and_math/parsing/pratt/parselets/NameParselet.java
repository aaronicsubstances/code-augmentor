package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.DefaultToken;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.PrattParser;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.PrefixParselet;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.NameExpression;

/**
 * Simple parselet for a named variable like "abc".
 */
public class NameParselet implements PrefixParselet<DefaultToken, Expression> {
    @Override
    public Expression parse(PrattParser<DefaultToken, Expression> parser, DefaultToken token) {
      return new NameExpression(token.text);
    }
}

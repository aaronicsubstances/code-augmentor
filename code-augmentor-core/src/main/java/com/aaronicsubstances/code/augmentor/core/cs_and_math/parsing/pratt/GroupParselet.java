package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.GenericToken;

/**
 * Parses parentheses used to group an expression, like "a * (b + c)".
 */
public abstract class GroupParselet<T extends GenericToken, E> implements PrefixParselet<T, E> {
    private final int endTokenType;

    public GroupParselet(int endTokenType) {
        this.endTokenType = endTokenType;
    }

    @Override
    public E parse(PrattParser<T, E> parser, T token) {
        E expression = parser.parseExpression();
        T rightToken = parser.consume(endTokenType);
        return createParseExpression(token, expression, rightToken);
    }
  
    protected abstract E createParseExpression(T left, E expression, T right);
}

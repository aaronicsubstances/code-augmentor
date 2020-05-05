package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.GenericToken;

/**
 * Generic prefix parselet for an unary arithmetic operator. Parses prefix
 * unary "-", "+", "~", and "!" expressions.
 */
public abstract class PrefixOperatorParselet<T extends GenericToken, E> implements PrefixParselet<T, E> {
    private final int mPrecedence;

    public PrefixOperatorParselet(int precedence) {
        mPrecedence = precedence;
    }
  
    @Override
    public E parse(PrattParser<T, E> parser, T token) {
        // To handle right-associative operators like "^", we allow a slightly
        // lower precedence when parsing the right-hand side. This will let a
        // parselet with the same precedence appear on the right, which will then
        // take *this* parselet's result as its left-hand argument.
        E right = parser.parseExpression(mPrecedence);
    
        return createParseExpression(token, right);
    }

    public int getPrecedence() {
        return mPrecedence;
    }
  
    protected abstract E createParseExpression(T token, E right);  
}
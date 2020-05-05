package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.GenericToken;

/**
 * Generic infix parselet for an unary arithmetic operator. Parses postfix
 * unary "?" expressions.
 */
public abstract class PostfixOperatorParselet<T extends GenericToken, E> implements InfixParselet<T, E> {
    private final int mPrecedence;

    public PostfixOperatorParselet(int precedence) {
        mPrecedence = precedence;
    }
  
    @Override
    public E parse(PrattParser<T, E> parser, E left, T token) {
        return createParseExpression(left, token);
    }

    public int getPrecedence() {
        return mPrecedence;
    }
  
    protected abstract E createParseExpression(E left, T token);
}
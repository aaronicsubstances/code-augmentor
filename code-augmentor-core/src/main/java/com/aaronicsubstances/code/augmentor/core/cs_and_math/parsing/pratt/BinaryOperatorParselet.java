package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.GenericToken;

/**
 * Generic infix parselet for a binary arithmetic operator. The only
 * difference when parsing, "+", "-", "*", "/", and "^" is precedence and
 * associativity, so we can use a single parselet class for all of those.
 */
public abstract class BinaryOperatorParselet<T extends GenericToken, E> implements InfixParselet<T, E> {
    private final int mPrecedence;
    private final boolean mIsRight;

    public BinaryOperatorParselet(int precedence, boolean isRight) {
        mPrecedence = precedence;
        mIsRight = isRight;
    }
  
    @Override
    public E parse(PrattParser<T, E> parser, E left, T token) {
        // To handle right-associative operators like "^", we allow a slightly
        // lower precedence when parsing the right-hand side. This will let a
        // parselet with the same precedence appear on the right, which will then
        // take *this* parselet's result as its left-hand argument.
        E right = parser.parseExpression(mPrecedence - (mIsRight ? 1 : 0));
      
        return createParseExpression(left, token, right);
    }

    public int getPrecedence() {
        return mPrecedence;
    }

    public boolean isRight() {
        return mIsRight;
    }
  
    protected abstract E createParseExpression(E left, T token, E right);
}
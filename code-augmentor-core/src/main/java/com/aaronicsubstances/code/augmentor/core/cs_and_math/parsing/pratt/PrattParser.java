package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.GenericToken;

/**
 * Implements generic pratt parser.
 * @param <T> type of token
 * @param <E> type of expression
 */
public abstract class PrattParser<T extends GenericToken, E> {
    private final Iterator<T> mTokens;
    private final List<T> mRead = new ArrayList<>();
    private final Map<Integer, PrefixParselet<T, E>> mPrefixParselets = new HashMap<>();
    private final Map<Integer, InfixParselet<T, E>> mInfixParselets = new HashMap<>();

    /**
     * Creates new instance.
     * @param tokens source of tokens.
     */
    public PrattParser(Iterator<T> tokens) {
        mTokens = tokens;
    }
  
    /**
     * Registers a prefix parselet for a given token type. Any existing registration for
     * the token type is removed.
     * @param tokenType token type
     * @param parselet prefix parselet
     */
    public void register(int tokenType, PrefixParselet<T, E> parselet) {
        mPrefixParselets.put(tokenType, parselet);
    }
  
    /**
     * Registers a infix parselet for a given token type. Any existing registration for
     * the token type is removed.
     * @param tokenType token type
     * @param parselet infix parselet
     */
    public void register(int tokenType, InfixParselet<T, E> parselet) {
        mInfixParselets.put(tokenType, parselet);
    }

    /**
     * Called to create parse error when a prefix parselet is not found.
     * @param offendingToken
     * @return parse exception
     */
    protected abstract RuntimeException createPrefixParseletNotFoundException(T offendingToken);
    
    /**
     * Called to create parse error when a infix parselet is not found.
     * @param offendingToken
     * @return parse exception
     */
    protected abstract RuntimeException createInfixParseletNotFoundException(T offendingToken);
    
    /**
     * Called to create parse error when a {@link #consume(int)} call fails.
     * @param expectedTokenType
     * @param offendingToken
     * @return parse exception
     */
    protected abstract RuntimeException createTokenMismatchException(int expectedTokenType, 
        T offendingToken);        
    
    /**
     * Called when source tokens runs out of tokens.
     * @return EOF exception
     */
    protected abstract RuntimeException createEndOfTokensException();

    /**
     * Parses expression starting from current lookahead token.
     * @param precedence starting precedence level.
     * @return parsed expression.
     */
    public E parseExpression(int precedence) {
        T token = consume();
        PrefixParselet<T, E> prefix = mPrefixParselets.get(token.type);      
        if (prefix == null) {
            throw createPrefixParseletNotFoundException(token);
        }
      
        E left = prefix.parse(this, token);
      
        while (precedence < getPrecedence()) {
            token = consume();
        
            InfixParselet<T, E> infix = mInfixParselets.get(token.type);
            if (infix == null) {
                throw createInfixParseletNotFoundException(token);
            }

            left = infix.parse(this, left, token);
        }
      
        return left;
    }
  
    /**
     * Parses expression starting from current lookahead token and with
     * precedence level of 0.
     * @return parsed expression.
     */
    public E parseExpression() {
        return parseExpression(0);
    }
  
    /**
     * Consumes the current lookahead token only if its type is asserted successfully.
     * Else null is returned and current lookahead token is not consumed.
     * @param expectedTokenType used to assert type of current lookahead token.
     * @return consumed token or null if matching failed.
     */
    public T match(int expectedTokenType) {
        T token = lookAhead(0);
        if (token.type != expectedTokenType) {
            return null;
        }

        consume();
        return token;
    }
  
    /**
     * Consumes the current lookahead token, after asserting its type.
     * @param expectedTokenType used to assert type of current lookahead token.
     * If current lookahead token doesn't have this type, an exception is thrown.
     * @return consumed token.
     */
    public T consume(int expectedTokenType) {
        T token = lookAhead(0);
        if (token.type != expectedTokenType) {
            throw createTokenMismatchException(expectedTokenType, token);
        }
    
        return consume();
    }
  
    /**
     * Consumes the current lookahead token.
     * @return consumed token.
     */
    public T consume() {
        // Make sure we've read the token.
        lookAhead(0);
    
        return mRead.remove(0);
    }
  
    private T lookAhead(int distance) {
        // Read in as many as needed.
        while (distance >= mRead.size()) {
            if (!mTokens.hasNext()) {
                throw createEndOfTokensException();
            }
            mRead.add(mTokens.next());
        }

        // Get the queued token.
        return mRead.get(distance);
    }

    private int getPrecedence() {
        T token = lookAhead(0);
        InfixParselet<T, E> parser = mInfixParselets.get(token.type);
        if (parser != null) {
            return parser.getPrecedence();
        }
    
        return 0;
    }
}

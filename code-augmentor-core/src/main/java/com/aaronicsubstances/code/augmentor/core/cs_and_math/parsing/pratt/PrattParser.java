package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.GenericToken;

public abstract class PrattParser<T extends GenericToken, E> {
    private final Iterator<T> mTokens;
    private final List<T> mRead = new ArrayList<>();
    private final Map<Integer, PrefixParselet<T, E>> mPrefixParselets = new HashMap<>();
    private final Map<Integer, InfixParselet<T, E>> mInfixParselets = new HashMap<>();

    public PrattParser(Iterator<T> tokens) {
        mTokens = tokens;
    }
  
    public void register(int tokenType, PrefixParselet<T, E> parselet) {
        mPrefixParselets.put(tokenType, parselet);
    }
  
    public void register(int tokenType, InfixParselet<T, E> parselet) {
        mInfixParselets.put(tokenType, parselet);
    }

    protected abstract RuntimeException createPrefixParseletNotFoundException(T offendingToken);
    protected abstract RuntimeException createInfixParseletNotFoundException(T offendingToken);
    protected abstract RuntimeException createTokenMismatchException(int expectedTokenType, 
        T offendingToken);

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
  
    public E parseExpression() {
        return parseExpression(0);
    }
  
    public T match(int expectedTokenType) {
        T token = lookAhead(0);
        if (token.type != expectedTokenType) {
            return null;
        }

        consume();
        return token;
    }
  
    public T consume(int expectedTokenType) {
        T token = lookAhead(0);
        if (token.type != expectedTokenType) {
            throw createTokenMismatchException(expectedTokenType, token);
        }
    
        return consume();
    }
  
    public T consume() {
        // Make sure we've read the token.
        lookAhead(0);
    
        return mRead.remove(0);
    }
  
    private T lookAhead(int distance) {
        // Read in as many as needed.
        while (distance >= mRead.size()) {
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

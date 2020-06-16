package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.parselets.*;

/**
 * Extends the generic Parser class with support for parsing the actual Bantam
 * grammar.
 */
public class BantamParser extends PrattParser<DefaultToken, Expression> {
    
    public BantamParser(Lexer lexer) {
        super(lexer);
    
        // Register all of the parselets for the grammar.
    
        // Register the ones that need special parselets.
        register(TokenType.NAME.ordinal(),       new NameParselet());
        register(TokenType.ASSIGN.ordinal(),     new AssignParselet());
        register(TokenType.QUESTION.ordinal(),   new ConditionalParselet());
        register(TokenType.LEFT_PAREN.ordinal(), new DefaultGroupParselet());
        register(TokenType.LEFT_PAREN.ordinal(), new CallParselet());

        // Register the simple operator parselets.
        prefix(TokenType.PLUS,      Precedence.PREFIX);
        prefix(TokenType.MINUS,     Precedence.PREFIX);
        prefix(TokenType.TILDE,     Precedence.PREFIX);
        prefix(TokenType.BANG,      Precedence.PREFIX);
    
        // For kicks, we'll make "!" both prefix and postfix, kind of like ++.
        postfix(TokenType.BANG,     Precedence.POSTFIX);

        infixLeft(TokenType.PLUS,     Precedence.SUM);
        infixLeft(TokenType.MINUS,    Precedence.SUM);
        infixLeft(TokenType.ASTERISK, Precedence.PRODUCT);
        infixLeft(TokenType.SLASH,    Precedence.PRODUCT);
        infixRight(TokenType.CARET,   Precedence.EXPONENT);
    }
  
    /**
     * Registers a postfix unary operator parselet for the given token and
     * precedence.
     */
    public void postfix(TokenType token, int precedence) {
        register(token.ordinal(), new DefaultPostfixOperatorParselet(precedence));
    }
  
    /**
     * Registers a prefix unary operator parselet for the given token and
     * precedence.
     */
    public void prefix(TokenType token, int precedence) {
        register(token.ordinal(), new DefaultPrefixOperatorParselet(precedence));
    }
  
    /**
     * Registers a left-associative binary operator parselet for the given token
     * and precedence.
     */
    public void infixLeft(TokenType token, int precedence) {
        register(token.ordinal(), new DefaultBinaryOperatorParselet(precedence, false));
    }
  
    /**
     * Registers a right-associative binary operator parselet for the given token
     * and precedence.
     */
    public void infixRight(TokenType token, int precedence) {
        register(token.ordinal(), new DefaultBinaryOperatorParselet(precedence, true));
    }

    @Override
    protected RuntimeException createPrefixParseletNotFoundException(DefaultToken token) {
        return new ParseException("Could not parse \"" + token.text + "\".");
    }

    @Override
    protected RuntimeException createInfixParseletNotFoundException(DefaultToken token) {
        return new ParseException("Could not parse \"" + token.text + "\".");
    }

    @Override
    protected RuntimeException createTokenMismatchException(int expected, DefaultToken token) {
        throw new RuntimeException("Expected token " + expected +
            " and found " + token.type);
    }

    @Override
    protected RuntimeException createEndOfTokensException() {
        // not needed since our lexer never runs out of tokens.
        return new UnsupportedOperationException();
    }
}
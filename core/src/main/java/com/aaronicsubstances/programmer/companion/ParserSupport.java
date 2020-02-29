package com.aaronicsubstances.programmer.companion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Class for supporting parsers by providing common services required
 * during implementation.
 */
public class ParserSupport {
    private final ParserInputSource inputSource;
    private Lexer lexer;
    private final List<Token> readTokens;
    
    public ParserSupport(ParserInputSource inputSource, Lexer lexer) {
        this.inputSource = inputSource;
        this.lexer = lexer;
        this.readTokens = new ArrayList<>();
    }

    public ParserInputSource getParserInputSource() {
        return inputSource;
    }

    public Lexer getLexer() {
        return lexer;
    }

    public final void setLexer(Lexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Backtracks input source to specified position. In other words, it makes
     * it appear as if token consumption beyond the specified position never
     * happened.
     * 
     * @param position position to backtrack to.
     */
    public void rewindTo(int position) {
        readTokens.clear();
        int[] lineAndColumnNumbers = LexerSupport.calculateLineAndColumnNumbers(
            inputSource.getInput(), position);
        inputSource.setPosition(position);
        inputSource.setLineNumber(lineAndColumnNumbers[0]);
        inputSource.setColumnNumber(lineAndColumnNumbers[1]);
    }

    /**
     * Checks that current lookahead token matches given token type,
     * and only when a match is found does it return that token,
     * and consumes it. Input source is not advanced if token type doesn't match.
     * @param expectedTokenType token type to be compared with current lookahead
     * @return consumed token if match is successful; else null.
     */
    public Token match(int expectedTokenType) {
        Token token = lookAhead(0);
        if (token.type != expectedTokenType) {
            return null;
        }

        return consume();
    }

    /**
     * Asserts that current lookahead token matches given token type,
     * and then consumes. If match is unsuccessful, an exception is thrown.
     * 
     * @param expectedTokenType token type to be compared with current lookahead.
     * @return consumed token.
     */
    public Token consume(int expectedTokenType) {
        Token token = lookAhead(0);
        if (token.type != expectedTokenType) {
            String expectedTokenName = String.valueOf(expectedTokenType);
            if (lexer != null) {
                expectedTokenName = lexer.getTokenName(expectedTokenType);
            }
            throw inputSource.createAbortException("Expected token type " + expectedTokenName
                    + " and found " + lexer.describeToken(token), token);
        }

        return consume();
    }

    /**
     * Consumes current lookahead and returns it.
     */
    public Token consume() {
        // Make sure we've read the token.
        Token token = lookAhead(0);

        if (!readTokens.isEmpty()) {
            readTokens.remove(0);
        }
        return token;
    }

    /**
     * Get token at given offset from current lookahead.
     * @param distance offset
     * @return token or EOF token if offset goes beyond bounds of input source.
     */
    public Token lookAhead(int distance) {
        return lookAhead(distance, null);
    }

    /**
     * Get token at given offset from current lookahead.
     * @param distance offset
     * @param lexerFunction used to override lexer property as an alternative
     * source of tokens. If lexer property is null, then specifying this argument
     * is mandatory.
     * @return token or EOF token if offset goes beyond bounds of input source.
     */
    public Token lookAhead(int distance, Function<ParserInputSource, List<Token>> lexerFunction) {
        // Read in as many as needed.
        while (distance >= readTokens.size()) {
            List<Token> tokens;
            if (lexerFunction != null) {
                tokens = lexerFunction.apply(inputSource);
            }
            else {
                if (lexer == null) {
                    throw new IllegalArgumentException("lexerFunction argument cannot be null " +
                        "if lexer property is null");
                }
                tokens = lexer.next(inputSource);
            }
            if (tokens == null || tokens.isEmpty()) {
                break;
            }
            readTokens.addAll(tokens);
        }

        // Get the queued token.
        if (distance < readTokens.size()) {
            return readTokens.get(distance);
        }
        // create EOF token.
        return new Token();
    }
}
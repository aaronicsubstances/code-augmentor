package com.aaronicsubstances.programmer.companion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 
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

    public void rewindTo(int position) {
        readTokens.clear();
        int[] lineAndColumnNumbers = LexerSupport.calculateLineAndColumnNumbers(
            inputSource.getInput(), position);
        inputSource.setPosition(position);
        inputSource.setLineNumber(lineAndColumnNumbers[0]);
        inputSource.setColumnNumber(lineAndColumnNumbers[1]);
    }

    public Token match(int expected) {
        Token token = lookAhead(0);
        if (token.type != expected) {
            return null;
        }

        return consume();
    }

    public Token consume(int expected) {
        Token token = lookAhead(0);
        if (token.type != expected) {
            String expectedTokenName = lexer.getTokenName(
                    expected);
            throw inputSource.createAbortException("Expected token type " + expectedTokenName
                    + " and found " + lexer.describeToken(token), token);
        }

        return consume();
    }

    public Token consume() {
        // Make sure we've read the token.
        Token token = lookAhead(0);

        if (!readTokens.isEmpty()) {
            readTokens.remove(0);
        }
        return token;
    }

    public Token lookAhead(int distance) {
        return lookAhead(distance, null);
    }
    
    public Token lookAhead(int distance, Function<ParserInputSource, List<Token>> lexerFunction) {
        // Read in as many as needed.
        while (distance >= readTokens.size()) {
            List<Token> tokens;
            if (lexerFunction != null) {
                tokens = lexerFunction.apply(inputSource);
            }
            else {
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
package com.aaronicsubstances.programmer.companion.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.aaronicsubstances.programmer.companion.LexerSupport;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.ParserSupport;
import com.aaronicsubstances.programmer.companion.Token;

/**
 * Parses Java source code into a limited set of tokens from which new lines and comments
 * can be extracted.
 */
public class JavaParser {
    private JavaLexer lexer;
    private final ParserSupport parserSupport;

    public JavaParser(JavaSourceCodeWrapper inputSource) {
        this.lexer = new JavaLexer();
        this.parserSupport = new ParserSupport(inputSource, lexer);
    }

    public List<Token> start() {
        return parse();
    }

    private List<Token> parse() {
        List<Token> parseResults = new ArrayList<>();
        while (true) {
            Token token = parserSupport.lookAhead(0);
            if (token.type == LexerSupport.EOF) {
                break;
            }
            switch (token.type) {
                case JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_START:
                    parseSingleLineComment(parseResults);
                    break;
                case JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_START:
                    parseMultiLineComment(parseResults);
                    break;
                case JavaLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER:
                    parseSingleLineString(parseResults);
                    break;
                default:
                    parseResults.add(parserSupport.consume());
                    break;
            }
        }
        return parseResults;
    }

    private void parseSingleLineComment(List<Token> parseResults) {
        Function<ParserInputSource, List<Token>> lexerFunction = lexer::consumeSingleLineComment;
        Token commentStart = parserSupport.match(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_START,
            lexerFunction);
        parseResults.add(commentStart);
        while (true) {
            Token token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_END) {
                break;
            }
            Token contentToken = parserSupport.match(JavaLexer.TOKEN_TYPE_COMMENT_CONTENT, 
                lexerFunction);
            parseResults.add(contentToken);
        }
        Token commentEnd = parserSupport.match(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_END,
            lexerFunction);
        parseResults.add(commentEnd);
    }

    private void parseMultiLineComment(List<Token> parseResults) {
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeMultiLineComment(inputSource));
        Token commentStart = parserSupport.match(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_START,
            lexerFunction);
        parseResults.add(commentStart);
        while (true) {
            Token token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_END) {
                break;
            }
            parseResults.add(parserSupport.consume(lexerFunction));
        }
        Token commentEnd = parserSupport.match(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_END,
            lexerFunction);
        parseResults.add(commentEnd);
    }

    private void parseSingleLineString(List<Token> parseResults) {
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeSingleLineString(inputSource));
        Token delimiterToken = parserSupport.match(JavaLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER,
            lexerFunction);
        parseResults.add(delimiterToken);
        while (true) {
            Token token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER) {
                break;
            }
            Token contentToken = parserSupport.match(JavaLexer.TOKEN_TYPE_STRING_CONTENT,
                lexerFunction);
            parseResults.add(contentToken);
        }
        delimiterToken = parserSupport.match(JavaLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER,
            lexerFunction);
        parseResults.add(delimiterToken);
    }
}
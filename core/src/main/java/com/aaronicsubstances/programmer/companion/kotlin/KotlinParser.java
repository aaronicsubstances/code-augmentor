package com.aaronicsubstances.programmer.companion.kotlin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.aaronicsubstances.programmer.companion.LexerSupport;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.ParserSupport;
import com.aaronicsubstances.programmer.companion.Token;

/**
 * Parses Kotlin source code into a limited set of tokens from which new lines and comments
 * can be extracted.
 */
public class KotlinParser {
    private KotlinLexer lexer;
    private final ParserSupport parserSupport;

    public KotlinParser(ParserInputSource inputSource) {
        this.lexer = new KotlinLexer();
        this.parserSupport = new ParserSupport(inputSource, lexer);
    }

    public List<Token> parse() {
        List<Token> parseResults = new ArrayList<>();
        while (true) {
            Token token = parserSupport.lookAhead(0);
            if (token.type == LexerSupport.EOF) {
                break;
            }
            parse(parseResults, null);
        }
        return parseResults;
    }

    private void parse(List<Token> parseResults, 
            Function<ParserInputSource, List<Token>> startLexerFunction) {
        Token token = parserSupport.lookAhead(0, startLexerFunction);
        switch (token.type) {
            case KotlinLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_START:
                parseSingleLineComment(parseResults);
                break;
            case KotlinLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_START:
                parseMultiLineComment(parseResults);
                break;
            case KotlinLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER:
                parseSingleLineString(parseResults);
                break;
            case KotlinLexer.TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER:
                parseMultiLineString(parseResults);
                break;
            default:
                parseResults.add(parserSupport.consume());
                break;
        }
    }

    private void parseSingleLineComment(List<Token> parseResults) {
        Token commentStart = parserSupport.consume(KotlinLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_START);
        parseResults.add(commentStart);
        Function<ParserInputSource, List<Token>> lexerFunction = lexer::consumeSingleLineComment;
        while (true) {
            Token token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == KotlinLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_END) {
                break;
            }
            Token contentToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_COMMENT_CONTENT);
            parseResults.add(contentToken);
        }
        Token commentEnd = parserSupport.consume(KotlinLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_END);
        parseResults.add(commentEnd);
    }

    private void parseMultiLineComment(List<Token> parseResults) {
        Token commentStart = parserSupport.consume(KotlinLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_START);
        parseResults.add(commentStart);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeMultiLineComment(inputSource));
        while (true) {
            Token token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == KotlinLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_END) {
                break;
            }
            parseResults.add(parserSupport.consume());
        }
        Token commentEnd = parserSupport.consume(KotlinLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_END);
        parseResults.add(commentEnd);
    }

    private void parseSingleLineString(List<Token> parseResults) {
        Token delimiterToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER);
        parseResults.add(delimiterToken);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeSingleLineString(inputSource));
        while (true) {
            Token token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == KotlinLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER) {
                break;
            }
            if (token.type == KotlinLexer.TOKEN_TYPE_STRING_TEMPLATE_START) {
                parseStringTemplate(parseResults);
                continue;
            }
            Token contentToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_STRING_CONTENT);
            parseResults.add(contentToken);
        }
        delimiterToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER);
        parseResults.add(delimiterToken);
    }

	private void parseMultiLineString(List<Token> parseResults) {
        Token delimiterToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER);
        parseResults.add(delimiterToken);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeSingleLineString(inputSource));
        while (true) {
            Token token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == KotlinLexer.TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER) {
                break;
            }
            if (token.type == KotlinLexer.TOKEN_TYPE_STRING_TEMPLATE_START) {
                parseStringTemplate(parseResults);
                continue;
            }
            Token contentToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_STRING_CONTENT);
            parseResults.add(contentToken);
        }
        delimiterToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER);
        parseResults.add(delimiterToken);
    }

    private void parseStringTemplate(List<Token> parseResults) {
        Token startToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_STRING_TEMPLATE_START);
        parseResults.add(startToken);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeTemplateContent(inputSource));
        while (true) {
            Token token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == KotlinLexer.TOKEN_TYPE_STRING_TEMPLATE_END) {
                break;
            }
            parse(parseResults, lexerFunction);
        }
        Token endToken = parserSupport.consume(KotlinLexer.TOKEN_TYPE_STRING_TEMPLATE_END);
        parseResults.add(endToken);
	}
}
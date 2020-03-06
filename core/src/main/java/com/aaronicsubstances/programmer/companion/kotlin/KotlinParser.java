package com.aaronicsubstances.programmer.companion.kotlin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.aaronicsubstances.programmer.companion.LexerSupport;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.ParserSupport;
import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.java.JavaLexer;

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
            case KotlinLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER:
                parseSingleLineString(parseResults);
                break;
            case KotlinLexer.TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER:
                parseMultiLineString(parseResults);
                break;
            case KotlinLexer.TOKEN_TYPE_IMPORT_KEYWORD:
                parseImport(parseResults);
                break;
            default:
                parseResults.add(parserSupport.consume());
                break;
        }
    }

    private void parseSingleLineString(List<Token> parseResults) {
        Token token = parserSupport.consume(KotlinLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER);
        parseResults.add(token);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeSingleLineString(inputSource));
        while (true) {
            token = parserSupport.lookAhead(0, lexerFunction);
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
            parseResults.add(token);
            parserSupport.consume(KotlinLexer.TOKEN_TYPE_LITERAL_STRING_CONTENT);            
        }
        parseResults.add(token);
        parserSupport.consume(KotlinLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER);        
    }

	private void parseMultiLineString(List<Token> parseResults) {
        Token token = parserSupport.consume(KotlinLexer.TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER);
        parseResults.add(token);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeMultiLineString(inputSource));
        while (true) {
            token = parserSupport.lookAhead(0, lexerFunction);
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
            parseResults.add(token);
            parserSupport.consume(KotlinLexer.TOKEN_TYPE_LITERAL_STRING_CONTENT);            
        }
        parseResults.add(token);
        parserSupport.consume(KotlinLexer.TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER);
    }

    private void parseStringTemplate(List<Token> parseResults) {
        Token token = parserSupport.consume(KotlinLexer.TOKEN_TYPE_STRING_TEMPLATE_START);
        parseResults.add(token);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeTemplateContent(inputSource));
        while (true) {
            token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == KotlinLexer.TOKEN_TYPE_STRING_TEMPLATE_END) {
                break;
            }
            parse(parseResults, lexerFunction);
        }
        parserSupport.consume(KotlinLexer.TOKEN_TYPE_STRING_TEMPLATE_END);        
        parseResults.add(token);
	}

    private void parseImport(List<Token> parseResults) {
        Token token = parserSupport.consume(JavaLexer.TOKEN_TYPE_IMPORT_KEYWORD);
        parseResults.add(token);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeImport(inputSource));
        boolean importContentSeen = false;
        while (true) {
            token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                break;
            }
            if (token.type == JavaLexer.TOKEN_TYPE_SEMI_COLON ||
                    token.type == JavaLexer.TOKEN_TYPE_NEWLINE) {
                break;
            }
            switch (token.type) {
                case JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT:
                case JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT:
                case JavaLexer.TOKEN_TYPE_NON_NEWLINE_WHITESPACE:
                    break;
                case JavaLexer.TOKEN_TYPE_IMPORT_CONTENT:
                    importContentSeen = true;
                    break;
                default:
                    throw parserSupport.getParserInputSource().
                        createAbortException("Unexpected token in import statement", 
                        token);
            }
            parseResults.add(token);
            parserSupport.consume();
        }
        
        if (!importContentSeen) {
            throw parserSupport.getParserInputSource().
                createAbortException("Expecting imported item(s)", token);
        }

        if (token.type != LexerSupport.EOF) {
            parseResults.add(token);
            token = parserSupport.match(JavaLexer.TOKEN_TYPE_NEWLINE);
            if (token == null) {
                parserSupport.consume(JavaLexer.TOKEN_TYPE_SEMI_COLON);
            }
        }
	}
}
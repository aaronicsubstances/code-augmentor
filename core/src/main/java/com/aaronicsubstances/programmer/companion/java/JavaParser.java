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

    public List<Token> parse() {
        List<Token> parseResults = new ArrayList<>();
        while (true) {
            Token token = parserSupport.lookAhead(0);
            if (token.type == LexerSupport.EOF) {
                break;
            }
            switch (token.type) {
                case JavaLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER:
                    parseSingleLineString(parseResults);
                    break;
                case JavaLexer.TOKEN_TYPE_IMPORT_KEYWORD:
                    parseImportStatement(parseResults);
                    break;
                default:
                    parseResults.add(parserSupport.consume());
                    break;
            }
        }
        return parseResults;
    }

    private void parseImportStatement(List<Token> parseResults) {
        Token token = parserSupport.consume(JavaLexer.TOKEN_TYPE_IMPORT_KEYWORD);
        parseResults.add(token);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeImport(inputSource));
        boolean staticKeywordSeen = false;
        boolean importContentSeen = false;
        while (true) {
            token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw parserSupport.getParserInputSource().createAbortException(
                    "Unterminated import statement", token);
            }
            if (token.type == JavaLexer.TOKEN_TYPE_SEMI_COLON) {
                break;
            }
            switch (token.type) {
                case JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT:
                case JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT:
                case JavaLexer.TOKEN_TYPE_NON_NEWLINE_WHITESPACE:
                    break;
                case JavaLexer.TOKEN_TYPE_IMPORT_CONTENT:
                case JavaLexer.TOKEN_TYPE_STATIC_KEYWORD:
                    if (importContentSeen) {
                        throw parserSupport.getParserInputSource().
                            createAbortException("Expecting end of import", token);
                    }
                    if (token.type == JavaLexer.TOKEN_TYPE_IMPORT_CONTENT) {
                        importContentSeen = true;
                    }
                    else {
                        if (staticKeywordSeen) {
                            throw parserSupport.getParserInputSource().
                                createAbortException("Expecting imported item(s)", token);
                        }
                        staticKeywordSeen = true;
                    }
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

        parseResults.add(token);
        parserSupport.consume(JavaLexer.TOKEN_TYPE_SEMI_COLON);
	}

	private void parseSingleLineString(List<Token> parseResults) {
        Token token = parserSupport.consume(JavaLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER);
        parseResults.add(token);
        Function<ParserInputSource, List<Token>> lexerFunction = 
            inputSource -> Arrays.asList(lexer.consumeSingleLineString(inputSource));
        while (true) {
            token = parserSupport.lookAhead(0, lexerFunction);
            if (token.type == LexerSupport.EOF) {
                throw new RuntimeException("Unexpected EOF");
            }
            if (token.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER) {
                break;
            }
            parseResults.add(token);
            parserSupport.consume(JavaLexer.TOKEN_TYPE_LITERAL_STRING_CONTENT);
        }
        parseResults.add(token);
        parserSupport.consume(JavaLexer.TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER);
    }
}
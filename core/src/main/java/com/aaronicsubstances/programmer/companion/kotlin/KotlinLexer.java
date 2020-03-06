package com.aaronicsubstances.programmer.companion.kotlin;

import com.aaronicsubstances.programmer.companion.LexerSupport;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.java.JavaLexer;

/**
 * Lexer for Kotlin programmingn language.
 */
public class KotlinLexer extends JavaLexer {
    public static final int TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER = 57;
    public static final int TOKEN_TYPE_STRING_TEMPLATE_START = 60;
    public static final int TOKEN_TYPE_STRING_TEMPLATE_END = 61;

    @Override
    protected Token fetchNextToken(ParserInputSource inputSource) {
        // deal with shebang
        if (inputSource.getPosition() == 0 && inputSource.lookahead(0) == '#') {
            StringBuilder shebang = new StringBuilder();
            int ch;
            while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
                if (LexerSupport.isNewLine(ch)) {
                    break;
                }
                shebang.appendCodePoint(ch);
                inputSource.consume(1);
            }            
            Token shebangToken = new Token(TOKEN_TYPE_OTHER, 
                shebang.toString(), 0, inputSource.getPosition(), 1, 1);
            return shebangToken;
        }
        if (inputSource.lookahead(0) == '"' &&
                inputSource.lookahead(1) == '"' &&
                inputSource.lookahead(2) == '"') {
            return consumeToken(inputSource, TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER, 
                "\"\"\"", 3);
        }
        return super.fetchNextToken(inputSource);
    }

    @Override
    public Token consumeSingleLineString(ParserInputSource inputSource) {        
		int ch = inputSource.lookahead(0);

        if (ch == '"') {
            Token endOfString = consumeToken(inputSource, TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER, 
                "\"", 1);
            return endOfString;
        }

        if (ch == '$' && inputSource.lookahead(1) == '{') {
            Token templateBeginToken = consumeToken(inputSource, TOKEN_TYPE_STRING_TEMPLATE_START,
                "${", 2);
            return templateBeginToken;
        }

        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		boolean escaped = false;
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
            // check for new line.
			if (LexerSupport.isNewLine(ch)) {
				throw inputSource.createAbortException("Unexpected newline in string", null);
            }
            
            if (!escaped) {
                // check for end of string.
                if (ch == '"') {
                    break;
                }
                else if (ch == '$' && inputSource.lookahead(1) == '{') {
                    break;
                }
            }
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
            
			if (escaped) {
				escaped = false;
			}
			else if (ch == '\\') {
				escaped = true;
			}
        }
        if (ch == LexerSupport.EOF) {
            throw inputSource.createAbortException("Unterminated string", null);
        }
		assert tokenText.length() > 0;
        int endPos = inputSource.getPosition();
        Token contentToken = new Token(TOKEN_TYPE_LITERAL_STRING_CONTENT, 
            tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
        return contentToken;
    }

    public Token consumeMultiLineString(ParserInputSource inputSource) {
		int ch = inputSource.lookahead(0);

        int ch2 = inputSource.lookahead(1);
        if (ch == '"' && ch2 == '"' && inputSource.lookahead(2) == '"') {
            Token endOfString = consumeToken(inputSource, TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER, 
                "\"\"\"", 3);
            return endOfString;
        }

        if (ch == '$' && inputSource.lookahead(1) == '{') {
            Token templateBeginToken = consumeToken(inputSource, TOKEN_TYPE_STRING_TEMPLATE_START,
                "${", 2);
            return templateBeginToken;
        }

        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
            ch2 = inputSource.lookahead(1);
            if (ch == '"' && ch2 == '"' && inputSource.lookahead(2) == '"') {
                break;
            }
            else if (ch == '$' && ch2 == '{') {
                break;
            }
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
        }
        if (ch == LexerSupport.EOF) {
            throw inputSource.createAbortException("Unterminated string", null);
        }
		assert tokenText.length() > 0;
        int endPos = inputSource.getPosition();
        Token contentToken = new Token(TOKEN_TYPE_LITERAL_STRING_CONTENT, 
            tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
        return contentToken;
    }

    public Token consumeTemplateContent(ParserInputSource inputSource) {
        int lookahead = inputSource.lookahead(0);
        if (lookahead == LexerSupport.EOF) {
            throw inputSource.createAbortException("Unterminated string template", null);
        }
        else if (lookahead == '}') {
            Token endTemplateToken = consumeToken(inputSource, TOKEN_TYPE_STRING_TEMPLATE_END, 
                "}", 1);
            return endTemplateToken;
        }
        else {
            // recursively start from very beginning.
            return fetchNextToken(inputSource);
        }
    }
}
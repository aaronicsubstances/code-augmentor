package com.aaronicsubstances.programmer.companion.java;

import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.programmer.companion.Lexer;
import com.aaronicsubstances.programmer.companion.LexerSupport;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.Token;

/**
 * Lexer for Java programmingn language.
 */
public class JavaLexer implements Lexer {
	public static final int TOKEN_TYPE_EOF = -1;
	public static final int TOKEN_TYPE_SINGLE_LINE_COMMENT = 1;
	public static final int TOKEN_TYPE_MULTI_LINE_COMMENT = 3;
    public static final int TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER = 7;
    public static final int TOKEN_TYPE_NEWLINE = 10;
    public static final int TOKEN_TYPE_NON_NEWLINE_WHITESPACE = 12;
    public static final int TOKEN_TYPE_LITERAL_STRING_CONTENT = 20;
    public static final int TOKEN_TYPE_IMPORT_KEYWORD = 30;
    public static final int TOKEN_TYPE_STATIC_KEYWORD = 31;
    public static final int TOKEN_TYPE_IMPORT_CONTENT = 32;
    public static final int TOKEN_TYPE_SEMI_COLON = 36;
    public static final int TOKEN_TYPE_OTHER = 50;

    @Override
    public String getTokenName(int tokenType) {
        return LexerSupport.getTokenName(tokenType, getClass(), "TOKEN_TYPE_", "UNKNOWN");
    }

    @Override
    public String describeToken(Token token) {
        return getTokenName(token.type);
    }

    @Override
    public List<Token> next(ParserInputSource inputSource) {
        int lookup = inputSource.lookahead(0);
        if (lookup == LexerSupport.EOF) {
            return null;
        }
        Token nextToken = fetchNextToken(inputSource);
        return Arrays.asList(nextToken);
    }

    protected Token fetchNextToken(ParserInputSource inputSource) {
        int lookup = inputSource.lookahead(0);
        int nextChar = inputSource.lookahead(1);
		switch (lookup) {
			case '/':
				if (nextChar == '/') {
					return consumeSingleLineComment(inputSource);
				}
				else if (nextChar == '*') {
					return consumeMultiLineComment(inputSource);
                }
                // due to use of '/' to signal non "other" tokens, rather than
                // let "other" code handle this, just return here.
                return consumeToken(inputSource, TOKEN_TYPE_OTHER, "/", 1);
			case '"':
				return consumeToken(inputSource, TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER, "\"", 1);
			case '\r':
			case '\n':
				return consumeNewLineToken(inputSource);
            case ';':
                return consumeToken(inputSource, TOKEN_TYPE_SEMI_COLON, ";", 1);
        }
        
        // check for whitespace other than newlines.
        if (Character.isWhitespace(lookup)) {
            return consumeNonNewLineWhitespace(inputSource);
        }

        // check for import statements by reading identifiers.
        if (Character.isJavaIdentifierPart(lookup)) {
            return consumeJavaIdentifierOrNumber(inputSource);
        }
		
		// getting here means skip tokens involved.
		StringBuilder otherTokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
        int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		while ((lookup = inputSource.lookahead(0)) != LexerSupport.EOF) {
            boolean possibleRelevantTokenFound = false;
            // consider any character which can begin above conditional tests
            // (switch statement, whitespace and import if branches)
            // as a possible relevant token.
			switch (lookup) {
				case '/':
				case '"':
				case '\r':
                case '\n':
                case ';':
					possibleRelevantTokenFound = true;
					break;
            }
            if (!possibleRelevantTokenFound) {
                possibleRelevantTokenFound = Character.isWhitespace(lookup) ||
                    Character.isJavaIdentifierPart(lookup);
            }
			if (possibleRelevantTokenFound) {
				break;
			}
            otherTokenText.appendCodePoint(lookup);
            inputSource.consume(1);
		}
		assert otherTokenText.length() > 0;
        Token token = new Token(TOKEN_TYPE_OTHER, otherTokenText.toString(), startPos,
            inputSource.getPosition(), lineNumber, columnNumber);
        return token;
    }

    private Token consumeNonNewLineWhitespace(ParserInputSource inputSource) {
        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		int ch;
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
            if (!Character.isWhitespace(ch)) {
                break;
            }
			if (LexerSupport.isNewLine(ch)) {
				break;
            }
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
		}
		assert tokenText.length() > 0;
        int endPos = inputSource.getPosition();
        Token token = new Token(TOKEN_TYPE_NON_NEWLINE_WHITESPACE, 
            tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
        return token;
    }

    private Token consumeJavaIdentifierOrNumber(ParserInputSource inputSource) {
        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		int ch;
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
            if (!Character.isJavaIdentifierPart(ch)) {
                break;
            }
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
		}
        assert tokenText.length() > 0;
        int endPos = inputSource.getPosition();
        // at this stage check if we have an import statement or not.
        String tokenTextAsStr = tokenText.toString();
        int tokenType = TOKEN_TYPE_OTHER;
        if (tokenTextAsStr.equals("import")) {
            tokenType = TOKEN_TYPE_IMPORT_KEYWORD;
        }
        else if (tokenTextAsStr.equals("static")) {
            tokenType = TOKEN_TYPE_STATIC_KEYWORD;
        }
        Token token = new Token(tokenType, 
            tokenTextAsStr, startPos, endPos, lineNumber, columnNumber);
        return token;
    }

    protected Token consumeToken(ParserInputSource inputSource, int type, String text, 
            int consumptionCount) {
        int startPos = inputSource.getPosition();
        int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
        inputSource.consume(consumptionCount);
        int endPos = inputSource.getPosition();
		Token token = new Token(type, text, startPos, endPos, lineNumber,
			columnNumber);
		return token;
	}
	
	protected Token consumeNewLineToken(ParserInputSource inputSource) {
		int lookup = inputSource.lookahead(0);
		int nextChar = inputSource.lookahead(1);
		assert LexerSupport.isNewLine(lookup);
		Token token;
		if (lookup == '\r' && nextChar == '\n') {
			token = consumeToken(inputSource, TOKEN_TYPE_NEWLINE, "\r\n", 2);
		}
		else {
			token = consumeToken(inputSource, TOKEN_TYPE_NEWLINE, "" + (char)lookup, 1);
		}
		return token;
	}
    
    private Token consumeSingleLineComment(ParserInputSource inputSource) {
        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
        
        inputSource.consume(2);
        tokenText.append("//");

		int ch;
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
			if (LexerSupport.isNewLine(ch)) {
				break;
			}
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
		}
        int endPos = inputSource.getPosition();
        Token token = new Token(TOKEN_TYPE_SINGLE_LINE_COMMENT, 
            tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
        return token;
    }
    
    private Token consumeMultiLineComment(ParserInputSource inputSource) {
        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
        
        inputSource.consume(2);
        tokenText.append("/*");

        int ch;
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
            // check for end of comment.
            if (ch == '*' && inputSource.lookahead(1) == '/') {
                tokenText.append("*/");
                inputSource.consume(2);
                break;
            }
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
        }
        if (ch == LexerSupport.EOF) {
            throw inputSource.createAbortException("Unterminated multiline comment", null);
        }
        int endPos = inputSource.getPosition();
        Token contentToken = new Token(TOKEN_TYPE_MULTI_LINE_COMMENT, 
            tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
        return contentToken;
    }
	
	public Token consumeSingleLineString(ParserInputSource inputSource) {
		int ch = inputSource.lookahead(0);

        if (ch == '"') {
            Token endOfString = consumeToken(inputSource, TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER, 
                "\"", 1);
            return endOfString;
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
            // check for end of string.
            if (ch == '"' && !escaped) {
                break;
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
        Token token = new Token(TOKEN_TYPE_LITERAL_STRING_CONTENT, 
            tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
        return token;
	}
	
	public Token consumeImport(ParserInputSource inputSource) {
        int ch = inputSource.lookahead(0);
        if (Character.isWhitespace(ch)) {
            // read away whitespace, newlines included.
            return fetchNextToken(inputSource);
        }
        if (ch == '/') {
            int nextCh = inputSource.lookahead(1);
            if (nextCh == '/' || nextCh == '*') {
                // read away comments.
                return fetchNextToken(inputSource);
            }
        }
        if (ch == ';') {
            return fetchNextToken(inputSource);
        }

        // at this stage, ch must be a java identifier start
        if (!Character.isJavaIdentifierStart(ch)) {
            throw inputSource.createAbortException("Expected valid identifier start character", null);
        }

		StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
            if (!Character.isJavaIdentifierPart(ch)) {
                // since we are checking for java imports, include '.' and '*'
                // in search.
                if (ch != '.' && ch != '*') {
                    break;
                }
                // make exception for static keyword.
                if (tokenText.length() == 6 && tokenText.charAt(0) == 's' &&
                    tokenText.charAt(1) == 't' && tokenText.charAt(2) == 'a' &&
                    tokenText.charAt(3) == 't' && tokenText.charAt(4) == 'i' &&
                    tokenText.charAt(5) == 'c') {
                    break;
                }
            }
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
        }
        if (ch == LexerSupport.EOF) {
            // instead of raising error, return null to give
            // Kotlin subclass different means of handling EOF here.
            return null;
        }
		assert tokenText.length() > 0;
        int endPos = inputSource.getPosition();
        String tokenTextAsString = tokenText.toString();
        int tokenType = TOKEN_TYPE_IMPORT_CONTENT;
        if (tokenTextAsString.equals("static")) {
            tokenType = TOKEN_TYPE_STATIC_KEYWORD;
        }
        Token token = new Token(tokenType,
            tokenTextAsString, startPos, endPos, lineNumber, columnNumber);
        return token;
	}
}
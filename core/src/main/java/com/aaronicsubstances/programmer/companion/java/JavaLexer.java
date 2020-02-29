package com.aaronicsubstances.programmer.companion.java;

import java.util.ArrayList;
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
	public static final int TOKEN_TYPE_SINGLE_LINE_COMMENT_START = 1;
	public static final int TOKEN_TYPE_SINGLE_LINE_COMMENT_END = 2;
	public static final int TOKEN_TYPE_MULTI_LINE_COMMENT_START = 3;
	public static final int TOKEN_TYPE_MULTI_LINE_COMMENT_END = 5;
    public static final int TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER = 7;
	public static final int TOKEN_TYPE_NEWLINE = 10;
	public static final int TOKEN_TYPE_COMMENT_CONTENT = 16;
    public static final int TOKEN_TYPE_STRING_CONTENT = 20;
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
					return consumeToken(inputSource, TOKEN_TYPE_SINGLE_LINE_COMMENT_START, "//", 2);
				}
				else if (nextChar == '*') {
					return consumeToken(inputSource, TOKEN_TYPE_MULTI_LINE_COMMENT_START, "/*", 2);
                }
                return consumeToken(inputSource, TOKEN_TYPE_OTHER, "/", 1);
			case '"':
				return consumeToken(inputSource, TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER, "\"", 1);
			case '\r':
			case '\n':
				return consumeNewLineToken(inputSource);
		}
		
		// getting here means skip tokens involved.
		StringBuilder otherTokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
        int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		while ((lookup = inputSource.lookahead(0)) != LexerSupport.EOF) {
			boolean relevantTokenFound = false;
			switch (lookup) {
				case '/':
				case '"':
				case '\r':
				case '\n':
					relevantTokenFound = true;
					break;
			}
			if (relevantTokenFound) {
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
    
    public List<Token> consumeSingleLineComment(ParserInputSource inputSource) {
        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		int ch;
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
			if (LexerSupport.isNewLine(ch)) {
				break;
			}
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
		}
        List<Token> tokens = new ArrayList<>();
        int endPos = inputSource.getPosition();
		if (tokenText.length() > 0) {
            Token contentToken = new Token(TOKEN_TYPE_COMMENT_CONTENT, 
                tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
            tokens.add(contentToken);
        }
        // create "virtual" token for end of single line comment.
        Token endOfCommentToken = new Token(TOKEN_TYPE_SINGLE_LINE_COMMENT_END, 
            null, endPos, endPos, lineNumber, inputSource.getColumnNumber());
        tokens.add(endOfCommentToken);
		return tokens;
    }
    
    public Token consumeMultiLineComment(ParserInputSource inputSource) {
        int ch = inputSource.lookahead(0);

        if (ch == '*' && inputSource.lookahead(1) == '/') {
            Token endOfComment = consumeToken(inputSource, TOKEN_TYPE_MULTI_LINE_COMMENT_END, 
                "*/", 2);
            return endOfComment;
        }

        // return newlines as separate tokens.
        if (LexerSupport.isNewLine(ch)) {
            Token eolToken = consumeNewLineToken(inputSource);
            return eolToken;
        }

        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
            // check for end of comment.
            if (ch == '*' && inputSource.lookahead(1) == '/') {
                break;
            }
            // check for new line.
			if (LexerSupport.isNewLine(ch)) {
				break;
			}
            tokenText.appendCodePoint(ch);
            inputSource.consume(1);
        }
        if (ch == LexerSupport.EOF) {
            throw inputSource.createAbortException("Unterminated multiline comment", null);
        }
		if (tokenText.length() > 0) {
            int endPos = inputSource.getPosition();
            Token contentToken = new Token(TOKEN_TYPE_COMMENT_CONTENT, 
                tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
                return contentToken;
        }
        else {
            assert ch == '*' && inputSource.lookahead(1) == '/';
            return consumeMultiLineComment(inputSource);
        }
    }
	
	public Token consumeSingleLineString(ParserInputSource inputSource) {
		int ch = inputSource.lookahead(0);

        if (ch == '"') {
            Token endOfString = consumeToken(inputSource, TOKEN_TYPE_SINGLE_LINE_STRING_DELIMITER, 
                "\"", 1);
            return endOfString;
        }

        // return newlines as separate tokens.
        if (LexerSupport.isNewLine(ch)) {
            Token eolToken = consumeNewLineToken(inputSource);
            return eolToken;
        }

        StringBuilder tokenText = new StringBuilder();
        int startPos = inputSource.getPosition();
		int lineNumber = inputSource.getLineNumber();
        int columnNumber = inputSource.getColumnNumber();
		boolean escaped = false;
		while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
            // check for new line.
			if (LexerSupport.isNewLine(ch)) {
				break;
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
		if (tokenText.length() > 0) {
            int endPos = inputSource.getPosition();
            Token contentToken = new Token(TOKEN_TYPE_STRING_CONTENT, 
                tokenText.toString(), startPos, endPos, lineNumber, columnNumber);
                return contentToken;
        }
        else {
            assert ch == '"';
            return consumeSingleLineString(inputSource);
        }
	}
}
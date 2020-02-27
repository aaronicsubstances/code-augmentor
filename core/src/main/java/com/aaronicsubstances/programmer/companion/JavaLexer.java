package com.aaronicsubstances.programmer.companion;

import java.util.Iterator;

public class JavaLexer implements Iterator<Token> {	
	protected static final int MODE_NONE = 0;
	protected static final int MODE_SINGLE_LINE_COMMENT = 1;
	protected static final int MODE_MULTI_LINE_COMMENT = 3;
	protected static final int MODE_SINGLE_LINE_STRING = 5;
	
	protected final String sourceCode;

	protected int position = 0;
	protected int lineNumber = 1;
	protected int columnNumber = 1;
	protected int mode = MODE_NONE;
	protected Token savedToken;
	
	public JavaLexer(String sourceCode) {
		this.sourceCode = sourceCode;
	}
	
	@Override
	public boolean hasNext() {
		return true;
	}
	
	@Override
	public Token next() {
		Token nextToken = savedToken;
		savedToken = null;
		if (nextToken != null) {
			return nextToken;
		}
		if (position >= sourceCode.length()) {
			return null;
		}
		switch (mode) {
			case MODE_SINGLE_LINE_COMMENT:
				nextToken = processSingleLineComment();
				break;
			case MODE_MULTI_LINE_COMMENT:
				nextToken = processMultiLineComment();
				break;
			case MODE_SINGLE_LINE_STRING:
				nextToken = processSingleLineString();
				break;
		}
		if (nextToken == null) {
			nextToken = determineNextToken();
		}
		return nextToken;
	}
	
	private Token determineNextToken() {		
		char lookup = sourceCode.charAt(position);
		int nextChar = -1;
		if (position + 1 < sourceCode.length()) {
			nextChar = sourceCode.charAt(position + 1);
		}
		switch (lookup) {
			case '/':
				if (nextChar == '/') {
					mode = MODE_SINGLE_LINE_COMMENT;
					return consumeToken(Token.TOKEN_TYPE_SINGLE_LINE_COMMENT_START, "//", 2);
				}
				else if (nextChar == '*') {
					mode = MODE_MULTI_LINE_COMMENT;
					return consumeToken(Token.TOKEN_TYPE_MULTI_LINE_COMMENT_START, "/*", 2);
				}
				break;
			case '"':
				mode = MODE_SINGLE_LINE_STRING;
				return consumeToken(Token.TOKEN_TYPE_SINGLE_LINE_STRING_START, "\"", 1);
			case '\r':
			case '\n':
				return consumeNewLineToken();
			case ' ':
			case '\f':
			case '\t':
				return consumeNonNewLineWhiteSpaceToken();
		}
		
		// getting here means skip tokens involved.
		StringBuilder otherTokenText = new StringBuilder();
		int i;
		for (i = position; i < sourceCode.length(); i++) {
			boolean relevantTokenFound = false;
			char ch = sourceCode.charAt(i);
			switch (ch) {
				case '/':
				case '"':
				case '\r':
				case '\n':
				case ' ':
				case '\f':
				case '\t':
					relevantTokenFound = true;
					break;
			}
			if (relevantTokenFound) {
				break;
			}
			otherTokenText.append(ch);
		}
		assert otherTokenText.length() > 0;
		return consumeToken(Token.TOKEN_TYPE_OTHER, otherTokenText.toString(), i - position);
	}
	
	protected Token consumeNewLineToken() {
		char lookup = sourceCode.charAt(position);
		int nextChar = -1;
		if (position + 1 < sourceCode.length()) {
			nextChar = sourceCode.charAt(position + 1);
		}
		assert lookup == '\r' || lookup == '\n';
		Token token;
		if (lookup == '\r' && nextChar == '\n') {
			token = consumeToken(Token.TOKEN_TYPE_NEWLINE, "\r\n", 2);
		}
		else {
			token = consumeToken(Token.TOKEN_TYPE_NEWLINE, "" + lookup, 1);
		}
		lineNumber++;
		columnNumber = 1;
		return token;
	}
	
	protected Token consumeNonNewLineWhiteSpaceToken() {
		StringBuilder tokenText = new StringBuilder();
		int i;
		for (i = position; i < sourceCode.length(); i++) {
			boolean isWhitespace = false;
			char ch = sourceCode.charAt(i);
			switch (ch) {				
				case ' ':
				case '\f':
				case '\t':
					isWhitespace = true;
			}
			if (!isWhitespace) {
				break;
			}
			tokenText.append(ch);
		}
		assert tokenText.length() > 0;
		return consumeToken(Token.TOKEN_TYPE_NON_NEWLINE_WHITE_SPACE, tokenText.toString(), i - position);
	}

	protected Token consumeToken(int type, String text, int charConsumptionCount) {
		Token token = new Token(type, text, position, position + charConsumptionCount, lineNumber,
			columnNumber);			
		position += charConsumptionCount;		
		columnNumber += charConsumptionCount;
		return token;
	}
	
	private Token processSingleLineComment() {
		StringBuilder tokenText = new StringBuilder();
		int i;
		for (i = position; i < sourceCode.length(); i++) {
			char ch = sourceCode.charAt(i);
			if (ch == '\r' || ch == '\n') {
				break;
			}
			tokenText.append(ch);
		}
		mode = MODE_NONE;
		Token nextToken;
		if (tokenText.length() > 0) {
			nextToken = consumeToken(Token.TOKEN_TYPE_OTHER, tokenText.toString(), i - position);
			savedToken = consumeToken(Token.TOKEN_TYPE_SINGLE_LINE_COMMENT_END, null, 0);
		}
		else {
			nextToken = consumeToken(Token.TOKEN_TYPE_SINGLE_LINE_COMMENT_END, null, 0);
		}
		return nextToken;
	}
	
	private Token processMultiLineComment() {
		char lookup = sourceCode.charAt(position);
		// return new line separately.
		if (lookup == '\r' || lookup == '\n') {
			return consumeNewLineToken();
		}
		int nextChar = -1;
		if (position + 1 < sourceCode.length()) {
			nextChar = sourceCode.charAt(position + 1);
		}
		if (lookup == '*' && nextChar == '/') {
			mode = MODE_NONE;
			return consumeToken(Token.TOKEN_TYPE_MULTI_LINE_COMMENT_END, "*/", 2);
		}
		StringBuilder content = new StringBuilder();		
		int i;
		for (i = position; i < sourceCode.length(); i++) {
			lookup = sourceCode.charAt(position);
			if (lookup == '\r' || lookup == '\n') {
				break;
			}
			nextChar = -1;
			if (position + 1 < sourceCode.length()) {
				nextChar = sourceCode.charAt(position + 1);
			}
			if (lookup == '*' && nextChar == '/') {
				break;
			}
			content.append(lookup);
		}
		assert content.length() > 0;
		return consumeToken(Token.TOKEN_TYPE_OTHER, content.toString(), i - position);
	}
	
	private Token processSingleLineString() {
		char lookup = sourceCode.charAt(position);
		if (lookup == '\"') {
			mode = MODE_NONE;
			return consumeToken(Token.TOKEN_TYPE_SINGLE_LINE_STRING_END, "\"", 1);
		}
		boolean escaped = false;
		StringBuilder content = new StringBuilder();
		int i;
		for (i = position; i < sourceCode.length(); i++) {
			lookup = sourceCode.charAt(i);
			if (lookup == '\r' || lookup == '\n') {
				mode = MODE_NONE;
				break;
			}
			if (lookup == '"' && !escaped) {
				break;
			}
			content.append(lookup);
			if (escaped) {
				escaped = false;
			}
			else if (lookup == '\\') {
				escaped = true;
			}
		}
		// abnormal, but check for newline in string to avoid
		// lexer misinterpreting subsequent characters.
		if (content.length() == 0) {
			assert mode == MODE_NONE;
			return null;
		}
		return consumeToken(Token.TOKEN_TYPE_OTHER, content.toString(), i - position);
	}
}
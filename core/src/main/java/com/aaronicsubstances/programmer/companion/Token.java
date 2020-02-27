package com.aaronicsubstances.programmer.companion;

import java.util.Objects;

public class Token {
	public static final int TOKEN_TYPE_EOF = -1;
	public static final int TOKEN_TYPE_SINGLE_LINE_COMMENT_START = 1;
	public static final int TOKEN_TYPE_SINGLE_LINE_COMMENT_END = 2;
	public static final int TOKEN_TYPE_MULTI_LINE_COMMENT_START = 3;
	public static final int TOKEN_TYPE_MULTI_LINE_COMMENT_END = 5;
	public static final int TOKEN_TYPE_SINGLE_LINE_STRING_START = 7;
	public static final int TOKEN_TYPE_SINGLE_LINE_STRING_END = 9;
	public static final int TOKEN_TYPE_MULTI_LINE_STRING_START = 11;
	public static final int TOKEN_TYPE_MULTI_LINE_STRING_END = 15;
	public static final int TOKEN_TYPE_NEWLINE = 20;
	public static final int TOKEN_TYPE_NON_NEWLINE_WHITE_SPACE = 25;
	public static final int TOKEN_TYPE_OTHER = 50;
	
	public final int type;
	public final String text;
	public final int startPos;
	public final int endPos;
	public final int lineNumber;
	public final int columnNumber;
	
	/**
	 * Creates an end-of-line token.
	 */
	public Token() {
		this(TOKEN_TYPE_EOF, null, 0, 0, 0, 0);
	}
	
	/**
	 * Creates new token.
	 *
	 * @param type
	 * @param text
	 * @param startPos
	 * @param endPos
	 * @param lineNumber
	 * @param columnNumber
	 */
	public Token(int type, String text, int startPos, int endPos, int lineNumber, int columnNumber) {
		this.type = type;
		this.text = text;
		this.startPos = startPos;
		this.endPos = endPos;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Token other = (Token) obj;
		return this.type == other.type &&
			Objects.equals(this.text, other.text) &&
			this.startPos == other.startPos &&
			this.endPos == other.endPos &&
			this.lineNumber == other.lineNumber &&
			this.columnNumber == other.columnNumber;
	}
	
	@Override 
	public int hashCode() {
		int hash = 57;
		hash = hash * 423 + type;
		hash = hash * 423 + Objects.hashCode(text);
		hash = hash * 423 + startPos;
		hash = hash * 423 + endPos;
		hash = hash * 423 + lineNumber;
		hash = hash * 423 + columnNumber;
		return hash;
	}
	
	@Override
	public String toString() {
		return String.format("Token{type=%s, text=%s, startPos=%s, endPos=%s, " +
			"lineNumber=%s, columnNumber=%s}",
			type, text, startPos, endPos, lineNumber, columnNumber);
	}
}
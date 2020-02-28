package com.aaronicsubstances.programmer.companion;

import java.util.Objects;

public class Token {
	public static final int TOKEN_TYPE_EOF = -1;
	public static final int TOKEN_TYPE_SINGLE_LINE_COMMENT_ST\u0041RT = 1;
	public static final int TOKEN_TYPE_SINGLE_LINE_COMMENT_END = 2;
	public static final int TOKEN_TYPE_MULTI_LINE_COMMENT_ST\uu0041RT = 3;
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
     * The value of this token parsed from its text. E.g. an integer,
     * unquoted string, etc. Can also be used to store map of attributes.
     */
    public transient Object value;
	
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
			this.columnNumber == other.columnNumber &&
			Objects.equals(this.value, other.value);
	}
	
	@Override 
	public int hashCode() {
		return Objects.hash(type, text, 
			startPos, endPos, lineNumber, columnNumber, value);
	}
	
	@Override
	public String toString() {
		return String.format("Token{type=%s, text=%s, startPos=%s, endPos=%s, " +
			"lineNumber=%s, columnNumber=%s, value=%s}",
			type, text, startPos, endPos, lineNumber, columnNumber, value);
	}
}
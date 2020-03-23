package com.aaronicsubstances.code.augmentor.parsing;

import java.util.Map;
import java.util.Objects;

/**
 * Represents result of lexing/scanning some source code prior to parsing.
 */
public class Token {
	public static final int TYPE_SINGLE_LINE_COMMENT = 1;
	public static final int TYPE_MULTI_LINE_COMMENT = 3;
    public static final int TYPE_NEWLINE = 10;
    public static final int TYPE_NON_NEWLINE_WHITESPACE = 12;
    public static final int TYPE_LITERAL_STRING_CONTENT = 20;
    public static final int TYPE_SHEBANG = 40;
    public static final int TYPE_PACKAGE_STATEMENT = 42;
    public static final int TYPE_IMPORT_STATEMENT = 43;
    public static final int TYPE_OTHER = 50;

	public static final String VALUE_KEY_IMPORT_STATEMENT = "import";
	
	public final int type;
	public final String text;
	public final int startPos;
	public final int endPos;
	public final int lineNumber;
    
    /**
     * The value of this token parsed from its text. E.g. an integer,
     * unquoted string, etc. Can also be used to store map of attributes.
     */
	public transient Map<String, Object> value;
	
	/**
	 * Creates an end-of-line token.
	 */
	public Token() {
		this(-1, null, 0, 0, 0);
	}
	
	/**
	 * Creates new token.
	 *
	 * @param type
	 * @param text
	 * @param startPos
	 * @param endPos
	 * @param lineNumber
	 */
	public Token(int type, String text, int startPos, int endPos, int lineNumber) {
		this.type = type;
		this.text = text;
		this.startPos = startPos;
		this.endPos = endPos;
		this.lineNumber = lineNumber;
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
		boolean v = this.type == other.type &&
			Objects.equals(this.text, other.text) &&
			this.startPos == other.startPos &&
			this.endPos == other.endPos &&
			this.lineNumber == other.lineNumber;
		if (!v) {
			return false;
		}
		v =	Objects.equals(this.value, other.value);
		return v;
	}
	
	@Override 
	public int hashCode() {
		return Objects.hash(type, text, 
			startPos, endPos, lineNumber, value);
	}
	
	@Override
	public String toString() {
		return String.format("Token{type=%s, text=%s, startPos=%s, endPos=%s, " +
			"lineNumber=%s, value=%s}",
			type, text, startPos, endPos, lineNumber, value);
	}
}
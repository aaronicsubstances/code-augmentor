package com.aaronicsubstances.code.augmentor.core.parsing;

/**
 * Exception thrown during course of parsing operations.
 */
public class ParserException extends RuntimeException {
    private static final long serialVersionUID = 3489428574490L;

    private final int lineNumber;

    public ParserException(int lineNumber, String message) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public ParserException(int lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
    }

	public int getLineNumber() {
		return lineNumber;
	}
}
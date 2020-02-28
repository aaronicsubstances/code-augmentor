package com.aaronicsubstances.programmer.companion;

/**
 * Exception thrown during course of parsing operations.
 */
public class ParserException extends RuntimeException {
    private static final long serialVersionUID = 3489428574490L;

    private final int lineNumber;
    private final int columnNumber;
    private final String snippet;

    public ParserException(String message) {
        super(message);
        this.lineNumber = 0;
        this.columnNumber = 0;
        this.snippet = null;
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = 0;
        this.columnNumber = 0;
        this.snippet = null;
    }

    public ParserException(String message, int lineNumber, int columnNumber, String snippet) {
        this(message, null, lineNumber, columnNumber, snippet);
    }

    public ParserException(String message, Throwable cause, int lineNumber, int columnNumber, 
            String snippet) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.snippet = snippet;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String getSnippet() {
        return snippet;
    }
}
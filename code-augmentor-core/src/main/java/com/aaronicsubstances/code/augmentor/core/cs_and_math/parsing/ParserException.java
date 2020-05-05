package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing;

import java.io.File;

/**
 * Exception thrown during course of parsing operations.
 */
public class ParserException extends RuntimeException {
    private static final long serialVersionUID = 3489428574490L;

    private final int lineNumber;
    private final int columnNumber;
    private final String snippet;
    private final File dir;
    private final String filePath;

    public ParserException(String message) {
        this(message, null, 0, 0, null, null, null);
    }

    public ParserException(String message, Throwable cause) {
        this(message, cause, 0, 0, null, null, null);
    }

    public ParserException(String message, int lineNumber, int columnNumber, String snippet) {
        this(message, null, lineNumber, columnNumber, snippet, null, null);
    }

    public ParserException(String message, Throwable cause, int lineNumber, int columnNumber, 
            String snippet, File dir, String filePath) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.snippet = snippet;
        this.dir = dir;
        this.filePath = filePath;
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

    public File getDir() {
        return dir;
    }

    public String getFilePath() {
        return filePath;
    }
}
package com.aaronicsubstances.programmer.companion;

/**
 * ParserInputSource
 */
public class ParserInputSource {
    public static final int EOF = -1;

    private final String input;

    private int position;
    private int lineNumber;
    private int columnNumber;
     
    public ParserInputSource(String input) {
        this.input = input;
        position = 0;
        lineNumber = 1;
        columnNumber = 1;
    }

    public String getInput() {
        return input;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    protected void fixInputCoordinates(int[] inputCoordinates) {
    }

    public ParserException createAbortException(String message, Token token) {
        int errorPosition = position, errorLineNumber = lineNumber, 
            errorColumnNumber = columnNumber;
        if (token != null && token.type != EOF) {
            errorPosition = token.startPos;
            errorLineNumber = token.lineNumber;
            errorColumnNumber = token.columnNumber;
        }
        int[] dest = new int[]{ errorPosition, errorLineNumber, errorColumnNumber };
        fixInputCoordinates(dest);
        errorLineNumber = dest[1];
        errorColumnNumber = dest[2];

        // visually identify error location in input.
        String[] inputLines = input.split("\r\n|\r|\n", -1);
        StringBuilder snippet = new StringBuilder(inputLines[errorLineNumber - 1]);
        snippet.append("\n");
        for (int i = 0; i < errorColumnNumber; i++) {
            snippet.append('^');
        }
        
        String errorMessage = String.format("%s:%s %s\n\n%s", errorLineNumber,
            errorColumnNumber, message, snippet);
        return new ParserException(errorMessage, errorLineNumber, errorColumnNumber, snippet.toString());
    }

    public int lookahead(int offset) {
        int effectivePosition = position + offset;
        if (effectivePosition < input.length()) {
            return input.charAt(effectivePosition);
        }
        return EOF;
    }

    public void consume(int count) {
        for (int i = 0; i < count; i++) {
            if (position >= input.length()) {
                break;
            }
            char ch = input.charAt(position);
            position++;
            columnNumber++;
            // update line number if necessary.
            if (ch == '\n') {
                lineNumber++;
                columnNumber = 1;
            }
            else if (ch == '\r') {
                // old macintosh
                // ignore if followed by '\n'
                if (position < input.length() && input.charAt(position) != '\n') {
                    lineNumber++;
                    columnNumber = 1;
                }
            }
        }
    }
}
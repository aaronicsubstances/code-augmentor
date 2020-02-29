package com.aaronicsubstances.programmer.companion;

/**
 * Used by lexers and parsers to manage position information when traversing characters; 
 * and to also raise errors embedded with such position information.
 */
public class ParserInputSource {
    private String input;

    private int position;
    private int lineNumber;
    private int columnNumber;

    /**
     * Creates a new ParserInputSource instance with initial position set to 0, and
     * line and column numbers set to 1.
     * @param input input/source code string
     */
    public ParserInputSource(String input) {
        this.input = input;
        position = 0;
        lineNumber = 1;
        columnNumber = 1;
    }

    /**
     * Default constructor meant for use by subclasses.
     */
    protected ParserInputSource() {
        this(null);
    }

    public String getInput() {
        return input;
    }

    protected final void setInput(String input) {
        this.input = input;
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

    /**
     * May be used by subclasses to automatically fix/correct coordinates
     * calculated in a transformed input string.
     * <p>
     * Intended to be used for parsing Java code, and to cater for similar situations, in which
     * source code as written by programmer is different from that understood by compiler.
     * E.g. the string "I \u0041M a programmer." counts as 25 characters to a human
     * programmer, but is seen as "I AM a programmer." in Java, and therefore
     * counts as 20 characters. Thus without some kind of source code mapping,
     * line numbers and snippets around error locations will be wrongly determined.
     * <p>
     * Default implementation returns unmodified input property. Overriding implementations
     * should interpret null array argument as intention to get only the original input
     * string.
     * 
     * @param inputCoordinates array containing 3 input coordinates: absolute position, line number
     * and column number in transformed/target input; also acts as receiver of fixed/corrected
     * coordinates. May be null, if all caller wants is the original input string.
     * @return original input string. 
     */
    protected String fixInputCoordinates(int[] inputCoordinates) {
        return input;
    }

    /**
     * Generates an appropriate parser exception message with embedded error 
     * location information.
     * @param message error message
     * @param token if not null, its startPos, lineNumber and columnNumber properties 
     * will be used to override similarly named properties of this class.
     * @return an parser exception instance with message decorated with error location.
     */
    public ParserException createAbortException(String message, Token token) {
        int errorPosition = position, errorLineNumber = lineNumber, 
            errorColumnNumber = columnNumber;
        if (token != null && token.type != LexerSupport.EOF) {
            errorPosition = token.startPos;
            errorLineNumber = token.lineNumber;
            errorColumnNumber = token.columnNumber;
        }

        // At this stage, the input used by this instance may well be the result
        // of transforming some original source code.
        // Thus we need to get the real input and the corresponding coordinates which
        // map to errorLineNumber and errorColumnNumber.
        int[] dest = new int[]{ errorPosition, errorLineNumber, errorColumnNumber };
        String originalInput = fixInputCoordinates(dest);
        errorLineNumber = dest[1];
        errorColumnNumber = dest[2];

        // visually identify error location in input.
        String[] inputLines = LexerSupport.NEW_LINE_REGEX.split(originalInput, -1);
        StringBuilder snippet = new StringBuilder(inputLines[errorLineNumber - 1]);
        snippet.append("\n");
        for (int i = 0; i < errorColumnNumber; i++) {
            snippet.append('^');
        }
        
        String errorMessage = String.format("%s:%s %s\n\n%s", errorLineNumber,
            errorColumnNumber, message, snippet);
        return new ParserException(errorMessage, errorLineNumber, errorColumnNumber, snippet.toString());
    }

    /**
     * Fetches the input character at an offset from current input pointer location.
     * @param offset offset from current input pointer location.
     * @return character at offset or -1 if character to be read is beyond input length.
     */
    public int lookahead(int offset) {
        int effectivePosition = position + offset;
        if (effectivePosition < input.length()) {
            return input.charAt(effectivePosition);
        }
        return -1;
    }

    /**
     * Advances the position of the input pointer
     * @param count determines how far input pointer should advance.
     */
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
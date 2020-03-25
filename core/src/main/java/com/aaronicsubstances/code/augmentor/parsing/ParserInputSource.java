package com.aaronicsubstances.code.augmentor.parsing;

/**
 * Used to manage chain of transformed source codes in order to trace 
 * positions back to original source code.
 */
public class ParserInputSource {
    private String input;
    private int position;
	
	private ParserInputSource embeddedInputSource;
    private SourceMap sourceMap;

    /**
     * Creates a new ParserInputSource instance.
     * 
     * @param input input/source code string
     */
    public ParserInputSource(String input) {
        this(input, null);
    }

    public ParserInputSource(String transformedInput, SourceMap sourceMap) {
        this.input = transformedInput;
        this.sourceMap = sourceMap;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
	
	public ParserInputSource getEmbeddedInputSource() {
		return embeddedInputSource;
	}
	
	public void setEmbeddedInputSource(ParserInputSource embeddedInputSource) {
		this.embeddedInputSource = embeddedInputSource;
	}
	
    public SourceMap getSourceMap() {
		return sourceMap;
	}
	
    public void setSourceMap(SourceMap sourceMap) {
		this.sourceMap = sourceMap;
	}

    /**
     * Intended to be used for parsing Java code, and to cater for similar situations, in which
     * source code as written by programmer is different from that understood by compiler.
     * E.g. the string "I \u0041M a programmer." counts as 25 characters to a human
     * programmer, but is seen as "I AM a programmer." in Java, and therefore
     * counts as 20 characters. Thus without some kind of source code mapping,
     * line numbers and snippets around error locations will be wrongly determined.
     * 
     * @param inputCoordinates array containing absolute position in transformed/target input; 
     * also acts as receiver of fixed/corrected absolute position. 
     * May be null, if all caller wants is the original input string.
     * @return original input string. 
     */
    public String fixInputCoordinates(int[] inputCoordinates) {
		if (inputCoordinates != null && sourceMap != null) {
            int transformedPosition = inputCoordinates[0];
            int originalPosition = sourceMap.getSrcIndex(transformedPosition);
            inputCoordinates[0] = originalPosition;
        }
		if (embeddedInputSource != null) {
			return embeddedInputSource.fixInputCoordinates(inputCoordinates);
		}
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
        int errorPosition = position;
        if (token != null && token.type != LexerSupport.EOF) {
            errorPosition = token.startPos;
        }

        // At this stage, the input used by this instance may well be the result
        // of transforming some original source code.
        // Thus we need to get the real input and the corresponding coordinates which
        // map to errorLineNumber and errorColumnNumber.
        int[] dest = new int[]{ errorPosition };
        String originalInput = input;
        if (embeddedInputSource != null) {
			originalInput = embeddedInputSource.fixInputCoordinates(dest);
        }
        int originalPosition = dest[0];
        int[] originalLineAndColumnNumbers = LexerSupport.calculateLineAndColumnNumbers(
            input, originalPosition);
        int errorLineNumber = originalLineAndColumnNumbers[0];
        int errorColumnNumber = originalLineAndColumnNumbers[1];

        // visually identify error location in input.
        String[] inputLines = LexerSupport.NEW_LINE_REGEX.split(originalInput, -1);
        StringBuilder snippet = new StringBuilder(inputLines[errorLineNumber - 1]);
        // for purposes of testing, accept invalid column numbers without complaining
        if (errorColumnNumber > 0) {
            snippet.append("\n");
            for (int i = 0; i < errorColumnNumber - 1; i++) {
                snippet.append(' ');
            }
            snippet.append('^');
        }
        
        String errorMessage = String.format("%s:%s %s\n\n%s", errorLineNumber,
            errorColumnNumber, message, snippet);
        return new ParserException(errorMessage, errorLineNumber, errorColumnNumber, snippet.toString());
    }
}
package com.aaronicsubstances.code.augmentor.core.parsing;

import com.aaronicsubstances.code.augmentor.core.parsing.peg.PositionInfo;

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
        this.input = input;
    }

    public ParserInputSource(String originalInput, SourceMap sourceMap) {
        this.input = originalInput;
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
        return fixInputCoordinates(inputCoordinates, null);
    }

    public String fixInputCoordinates(int[] inputCoordinates, String targetInput) {
        // Yes, use identity comparison rather than equivalence.
        if (input == targetInput) {
            return input;
        }
        if (embeddedInputSource != null) {
            if (inputCoordinates != null) {
                SourceMap sourceMap = embeddedInputSource.getSourceMap();
                if (sourceMap != null) {
                    int transformedPosition = inputCoordinates[0];
                    int originalPosition = sourceMap.getSrcIndex(transformedPosition);
                    inputCoordinates[0] = originalPosition;
                }
            }
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
        PositionInfo pInfo = createErrorLineInfo(errorPosition);
        return createAbortException(pInfo, message);
    }

    public ParserException createAbortException(PositionInfo pInfo, String message) {
        int errorLineNumber = pInfo.getLineNr();
        int errorColumnNumber = pInfo.getIndexInLine() + 1;

        StringBuilder snippet = new StringBuilder(pInfo.getLine());
        snippet.append('\n').append(pInfo.getUnderline(' ', '^'));
        
        String errorMessage = String.format("%s:%s %s\n\n%s", errorLineNumber,
            errorColumnNumber, message, snippet);
        return new ParserException(errorMessage, errorLineNumber, errorColumnNumber, snippet.toString());
    }

    public PositionInfo createErrorLineInfo(int errorPosition) {
        int[] dest = new int[]{ errorPosition };
        String originalInput = fixInputCoordinates(dest);
        int originalPosition = dest[0];
        PositionInfo pInfo = new PositionInfo(originalInput, originalPosition);
        return pInfo;
    }
}
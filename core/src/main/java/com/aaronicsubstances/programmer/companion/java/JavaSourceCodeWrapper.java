package com.aaronicsubstances.programmer.companion.java;

import com.aaronicsubstances.programmer.companion.JavaCodeLexerSupport;
import com.aaronicsubstances.programmer.companion.LexerSupport;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.SourceMap;

/**
 * ParserInputSource subclass for Java source code.
 */
public class JavaSourceCodeWrapper extends ParserInputSource {
    private final String originalInput;
    private final SourceMap sourceMap;

    public JavaSourceCodeWrapper(String originalInput) {
        this.originalInput = originalInput;
        sourceMap = new SourceMap();
        String transformedInput = JavaCodeLexerSupport.transformUnicodeEscapes(originalInput, sourceMap);
        setInput(transformedInput);
    }

    public String getOriginalInput() {
        return originalInput;
    }

    /**
     * Maps positions in transformed Java source code (ie with unicode escapes replaced)
     * back to original source code.
     * 
     * @param inputCoordinates contains 3 elements: position, line number and column number
     * 
     * @return original source code with unicode escapes
     */
    @Override
    protected String fixInputCoordinates(int[] inputCoordinates) {
        if (inputCoordinates != null) {
            int transformedPosition = inputCoordinates[0];
            int originalPosition = sourceMap.getSrcIndex(transformedPosition);
            int[] originalLineAndColumnNumbers = LexerSupport.calculateLineAndColumnNumbers(
                originalInput, originalPosition);
            inputCoordinates[0] = originalPosition;
            inputCoordinates[1] = originalLineAndColumnNumbers[0];
            inputCoordinates[2] = originalLineAndColumnNumbers[1];
        }
        return originalInput;
    }
}
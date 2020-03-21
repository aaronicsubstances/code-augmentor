package com.aaronicsubstances.code.augmentor.tasks;

/**
 * 
 */
public class SourceCodeTransformer {
    private final StringBuilder transformedText;
    private int positionAdjustment;
    
    public SourceCodeTransformer(String originalText) {
        this.transformedText = new StringBuilder(originalText);
    }

    public void addTransform(String replacement, int startPos) {
        transformedText.insert(positionAdjustment + startPos, replacement);
        positionAdjustment += replacement.length();
        // Alternatively.
        //addTransform(replacement, startPos, startPos);
    }
    
    public void addTransform(String replacement, int startPos, int endPos) {
        transformedText.replace(positionAdjustment + startPos, positionAdjustment + endPos, 
            replacement);
        positionAdjustment += replacement.length() - (endPos - startPos);
    }

    public String getTransformedText() {
        return transformedText.toString();
    }
}
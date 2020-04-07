package com.aaronicsubstances.code.augmentor.core.tasks;

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
        // alternatively,
        //addTransform(replacement, startPos, startPos);
    }
    
    public void addTransform(String replacement, int startPos, int endPos) {
        transformedText.replace(positionAdjustment + startPos, positionAdjustment + endPos, 
            replacement);
        int diff = replacement.length() - (endPos - startPos);
        positionAdjustment += diff;
    }

    public String getTransformedText() {
        return transformedText.toString();
    }
}
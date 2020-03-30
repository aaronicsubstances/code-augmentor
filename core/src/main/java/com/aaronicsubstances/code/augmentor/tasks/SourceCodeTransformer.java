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

    public int addTransform(String replacement, int startPos) {
        transformedText.insert(positionAdjustment + startPos, replacement);
        int diff = replacement.length();
        positionAdjustment += diff;
        return diff;
        // Alternatively.
        //return addTransform(replacement, startPos, startPos);
    }
    
    public int addTransform(String replacement, int startPos, int endPos) {
        transformedText.replace(positionAdjustment + startPos, positionAdjustment + endPos, 
            replacement);
        int diff = replacement.length() - (endPos - startPos);
        positionAdjustment += diff;
        return diff;
    }

    public String getTransformedText() {
        return transformedText.toString();
    }
}
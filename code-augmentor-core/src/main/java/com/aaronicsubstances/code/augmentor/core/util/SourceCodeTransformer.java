package com.aaronicsubstances.code.augmentor.core.util;

/**
 * Used by
 * {@link com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask} 
 * to make a series of replacements in a source code file at prespecified offsets in the file.
 * <p>
 * It enables one to start with a string (referred to as original string), and
 * change that string using indices into original string. The class takes care of
 * mapping the original string indices to positions in the modified version of
 * the string.
 */
public class SourceCodeTransformer {
    private final StringBuilder transformedText;
    private int positionAdjustment;
    
    /**
     * Creates a new instance.
     * @param originalText the original string
     */
    public SourceCodeTransformer(String originalText) {
        this.transformedText = new StringBuilder(originalText);
    }

    /**
     * Inserts a string into original string and updates adjusting offset information.
     * @param replacement string to insert.
     * @param startPos position in original string to insert at.
     */
    public void addTransform(String replacement, int startPos) {
        transformedText.insert(positionAdjustment + startPos, replacement);
        positionAdjustment += replacement.length();
        // alternatively,
        //addTransform(replacement, startPos, startPos);
    }
    
    /**
     * Changes a section in original string and updates adjusting offset information.
     * @param replacement replacement for string section.
     * @param startPos starting position (inclusive) of original string section
     * @param endPos ending position (exclusive) of original string section 
     */
    public void addTransform(String replacement, int startPos, int endPos) {
        transformedText.replace(positionAdjustment + startPos, positionAdjustment + endPos, 
            replacement);
        int diff = replacement.length() - (endPos - startPos);
        positionAdjustment += diff;
    }

    /**
     * Gets string resulting from modification of original string by replacements.
     * @return modified string.
     */
    public String getTransformedText() {
        return transformedText.toString();
    }

    /**
     * Gets offset that can be added to an index into original string to 
     * get corresponding position in modified string. As long as index does not point
     * within any section which has been changed, that index can be used as
     * a position into modified string using this property.
     * @return offset for adjusting original positions.
     */
    public int getPositionAdjustment() {
        return positionAdjustment;
    }
}
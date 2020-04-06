package com.aaronicsubstances.code.augmentor.core.parsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Used to save position-adjusting information need to reconcile positions between a 
 * source text and a destination text after replacments are made in the former to
 * obtain the latter.
 */
public class SourceMap {
    private final List<SubstringRange> srcRanges;
    private final List<SubstringRange> destRanges;

    // Used to track shifting indices of dest substrings from
    // corresponding source substrings.
    private int totalDiffLength;
    
    public SourceMap() {
        srcRanges = new ArrayList<>();
        destRanges = new ArrayList<>();
    }

    /**
     * For testing.
     * @param srcRanges
     * @param destRanges
     */
    public SourceMap(List<SubstringRange> srcRanges, List<SubstringRange> destRanges) {
        this.srcRanges = srcRanges;
        this.destRanges = destRanges;
    }

    public List<SubstringRange> getSrcRanges() {
        return srcRanges;
    }

    public List<SubstringRange> getDestRanges() {
        return destRanges;
    }
    
    /**
     * Appends a pairing of source substring to
     * dest/replacement substring to this source map. Source map is unchanged if
     * substrings have same length.
     * 
     * @param srcIndex start index of substring in source text
     * @param srcTokenLength length of source substring
     * @param destTokenLength length of dest/replacement substring
     */
    public void append(int srcIndex, int srcTokenLength, int destTokenLength) {
        int diffLen = destTokenLength - srcTokenLength;
        if (diffLen == 0) {
            return;
        }
        SubstringRange newSrcRange = new SubstringRange(
                srcIndex, srcIndex + srcTokenLength);
        srcRanges.add(newSrcRange);
        int destIndex = srcIndex + totalDiffLength;
        SubstringRange newDestRange = new SubstringRange(
                destIndex, destIndex + destTokenLength);
        destRanges.add(newDestRange);
        totalDiffLength += diffLen;
    }
    
    /**
     * Finds the index in a source text which corresponds to given index
     * in transformed text.
     * 
     * @param destIndex index in transformed text.
     * @return corresponding index in source text.
     */
    public int getSrcIndex(int destIndex) {
        // Since destRanges are sorted, look for a range which contains
        // destIndex using binary search.
        SubstringRange destIndexKey = new SubstringRange(destIndex, destIndex);
        int rIdx = Collections.binarySearch(destRanges, destIndexKey, 
                (p1, p2) -> {
                    if (p1.start < p2.start &&
                            p1.end < p2.end) {
                        return -1;
                    }
                    if (p1.start > p2.start &&
                            p1.end > p2.end) {
                        return 1;
                    }
                    // we leverage fact that items of destRanges don't overlap
                    // to conclude upon getting here that p1 and p2 compare as equal.
                    // This will enable finding substring range which covers
                    // destIndex. 
                    return 0;
                });
        if (rIdx >= 0) { // containing range found.
            // Getting here means that character at destIndex is inside a
            // replacement text for a source substring.
            SubstringRange destRange = destRanges.get(rIdx);
            SubstringRange srcRange = srcRanges.get(rIdx);

            // In this case, characters of source substring and
            // replacement text don't correspond to each other.
            // So we map all but the last of the replacement text
            // characters to the first character of the source substring.
            if (destIndex < destRange.end) {
                return srcRange.start;
            }
            else {
                // else the last replacement text character is mapped to the 
                // the last source substring character.
                return srcRange.end;
            }
        }
        else {
            // Getting here means that the character at destIndex is the same
            // character at the index we are now going to find. 

            // Try and get the largest index of the ranges which
            // are smaller (ie appear earlier) than destIndex. 
            rIdx = (-rIdx - 1) - 1;
            if (rIdx >= 0) {
                // During appending, 
                // destIndex = srcIndex + totalDiffLength
                // totalDiffLength = (prev) totalDiffLength + (prev) diffLen
                //                 = (prev) totalDiffLength + (prev) destTokenLength - (prev) srcTokenLength
                // (prev) destRange.end = (prev) srcIndex + (prev) totalDiffLength + (prev) destTokenLength
                // (prev) srcRange.end = (prev) srcIndex + (prev) srcTokenLength
        
                //
                // So by reversing and manipulating we arrive at, 
                //    srcIndex = destIndex - totalDiffLength
                //             = destIndex - [(prev) totalDiffLength + (prev) destTokenLength - (prev) srcTokenLength]
                //             = destIndex - [(prev) destRange.end - (prev) srcRange.end]
                //             = destIndex - (prev) destRange.end + (prev) srcRange.end
                SubstringRange destRange = destRanges.get(rIdx);
                SubstringRange srcRange = srcRanges.get(rIdx);
                return destIndex - destRange.end + srcRange.end;
            }
            else {
                // getting here means that destRanges is empty, and hence
                // there was no transformation of source text. 
                return destIndex;
            }
        }
    }

    /**
     * Used by SourceMap to record substring ranges.
     */
    static class SubstringRange {
        public final int start;
        public final int end;

        public SubstringRange(int left, int right) {
            this.start = left;
            this.end = right;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SubstringRange other = (SubstringRange) obj;
            if (start != other.start)
                return false;
            if (end != other.end)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "SubstringRange{start=" + start + ", end=" + end + "}";
        }
    }
}
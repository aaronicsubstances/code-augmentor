package com.aaronicsubstances.code.augmentor.core.parsing.peg.extras;

import java.util.function.Function;

import com.aaronicsubstances.code.augmentor.core.parsing.SourceMap;

/**
 * Normalizes a string to consist only of BMP unicode characters.
 */
public class UnicodeBmpNormalizer {
    
    public static String replaceSupplementaryCharacters(String sourceCode, int startIndex, int endIndex,
            Function<Integer, Character> smpReplacementFunction, SourceMap sourceMap) {
        StringBuilder s = new StringBuilder(sourceCode.substring(0, startIndex));
        for (int i = startIndex; i < endIndex; i++) {
            char ch = sourceCode.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                if (i + 1 >= endIndex) {
                    // error?
                    s.append(ch);
                    continue;
                }
                char lowSurrogate = sourceCode.charAt(i + 1);
                if (!Character.isSurrogatePair(ch, lowSurrogate)) {
                    // error?
                    s.append(ch);
                    continue;
                }

                char replacement = smpReplacementFunction.apply(
                    Character.toCodePoint(ch, lowSurrogate));
                s.append(replacement);
                
                if (sourceMap != null) {
                    sourceMap.append(i, 2, 1);
                }
                i++;
            }
            else {
                s.append(ch);
            }
        }
        s.append(s.substring(endIndex));
        return s.toString();
    }
}
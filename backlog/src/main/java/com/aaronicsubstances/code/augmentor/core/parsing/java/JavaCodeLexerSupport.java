package com.aaronicsubstances.code.augmentor.core.parsing.java;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aaronicsubstances.code.augmentor.core.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.core.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.core.parsing.SourceMap;

/**
 * Provides utility methods (esp unicode escape handling) for Java code parsing tasks.
 */
public class JavaCodeLexerSupport {
    private static final Pattern pattern;
    private static final int backslashesIndex, surrogatePairIndex,
            highSurrogateIndex, lowSurrogateEscapeIndex, lowSurrogateIndex,
            unicodeCharIndex;
    
    static {
        String escapedBackSlash = "\\\\";
        String opt1 = "(([Dd][89AaBb][0-9a-fA-F]{2})(" + escapedBackSlash +
                "u+([Dd][c-fC-F][0-9a-fA-F]{2})))";
        String opt2 = "([0-9a-fA-F]{0,4})";
        String str = String.format("(%s+)u+(?:%s|%s)", 
                escapedBackSlash, opt1, opt2);
        pattern = Pattern.compile(str);
        backslashesIndex = 1;
        surrogatePairIndex = 2;
        highSurrogateIndex = 3;
        lowSurrogateEscapeIndex = 4;
        lowSurrogateIndex = 5;
        unicodeCharIndex = 6;
    }
    
    /**
     * Transforms a java source code's unicode escapes, replacing them with their 
     * actual characters. E.g. \u0041M becomes AM
     * 
     * @param javaSourceCode text which is code in java or similar language.
     * @param sourceMap optional sourceMap instance which if not null, receives
     * position changes information useful for mapping back from transformed code
     * to original code.
     * 
     * @return transformed java source code containing no unicode escapes.
     */
    public static String transformUnicodeEscapes(String javaSourceCode, 
            SourceMap sourceMap) {        
        StringBuffer transformed = new StringBuffer();
        Matcher matcher = pattern.matcher(javaSourceCode);
        while (matcher.find()) {
            String srcToken = matcher.group();
            String backSlashes = matcher.group(backslashesIndex);
            String surrogatePair = matcher.group(surrogatePairIndex);
            
            // only bother about processing if there are an odd
            // number of backslashes.
            final boolean backSlashesEscaped = backSlashes.length() % 2 == 0;
            
            String replacement;
            if (surrogatePair != null && !surrogatePair.isEmpty()) {
                String lowSurrogate = matcher.group(lowSurrogateIndex);
                char lowSurrogateInt = (char)LexerSupport.parseHexadecimalString(lowSurrogate, 0, 
                                lowSurrogate.length());
                if (backSlashesEscaped) {
                    // error? low without high surrogate.
                    String lowSurrogateEscape = matcher.group(
                            lowSurrogateEscapeIndex);
                    // replace lowSurrogateEscape suffix with lowSurrogateInt
                    replacement = srcToken.substring(0, srcToken.length() -
                            lowSurrogateEscape.length()) + 
                            String.valueOf(lowSurrogateInt);
                }
                else {
                    String highSurrogate = matcher.group(highSurrogateIndex);
                    char highSurrogateInt = (char)LexerSupport.parseHexadecimalString(highSurrogate, 0,
                                    highSurrogate.length());
                    replacement = backSlashes.substring(0,
                        backSlashes.length() - 1);
                    replacement += new String(new char[]{ highSurrogateInt, 
                        lowSurrogateInt });
                }
            }
            else {
                if (backSlashesEscaped) {
                    continue;
                }
                String unicodeChar = matcher.group(unicodeCharIndex);
                if (unicodeChar.length() < 4) {
                    // error.
                    throw new ParserException("incomplete unicode escape at index " +
                            matcher.start());
                }
                char ch = (char)LexerSupport.parseHexadecimalString(unicodeChar, 0,
                                unicodeChar.length());                
                replacement = backSlashes.substring(0,
                    backSlashes.length() - 1);
                // ch may be a high without low surrogate. error?
                replacement += String.valueOf(ch);
            }
            if (sourceMap != null) {
                sourceMap.append(matcher.start(), srcToken.length(), 
                        replacement.length());
            }
            matcher.appendReplacement(transformed, 
                    Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(transformed);
        return transformed.toString();
    }
}
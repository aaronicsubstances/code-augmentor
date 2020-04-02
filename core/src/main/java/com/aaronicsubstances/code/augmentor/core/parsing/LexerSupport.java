package com.aaronicsubstances.code.augmentor.core.parsing;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains various utility methods that a hand-crafted lexer might need.
 */
public class LexerSupport {
    public static final int EOF = -1;
    public static final Pattern NEW_LINE_REGEX = Pattern.compile("\r\n|\r|\n");
    
    /**
     * Calculates line and column numbers given a position in a string.
     * 
     * @param s source code text.
     * @param position position in s.
     * 
     * @return array of two elements: line number and column number.
     */
    public static int[] calculateLineAndColumnNumbers(String s, int position) {
        int lineNumber = 1; // NB: line number starts from 1.
        Matcher newLineMatcher = NEW_LINE_REGEX.matcher(s);
        int lastNewLineEnd = 0;
        while (newLineMatcher.find(lastNewLineEnd)) {
            if (newLineMatcher.end() > position) {
                break;
            }
            lastNewLineEnd = newLineMatcher.end();
            lineNumber++;
        }

        // use last match to calculate column number position.
        // NB: column number starts from 1.
        int columnNumber = position - lastNewLineEnd + 1;
        return new int[]{ lineNumber, columnNumber };
    }

    /**
     * Helper method for getting the names of token types declared as static integer constants in a class.
     * 
     * @param type token type.
     * @param cls declaring class
     * @param typePrefix common prefix of all constants for token types. NB: this value is 
     * stripped of the start of the field name to obtain the final token name.
     * @param defName default name to use if a token type is not found.
     * @return name of token or defName is no corresponding field is found.
     */
    public static String getTokenName(int type, Class<?> cls, String typePrefix, String defName) {
        Field[] fields = cls.getFields();
        if (fields != null) {
            for (Field f : fields) {
                if (!f.getName().startsWith(typePrefix)) {
                    continue;
                }
                int fType; 
                try {
                    fType = f.getInt(null);
                }
                catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
                if (fType == type) {
                    String tokenName = f.getName().substring(typePrefix.length());
                    return tokenName;
                }
            }
        }
        return defName;
    }
    
    /**
     * Determines whether ch is a whitespace as defined in C89.
     */
    public static boolean isC89Whitespace(int ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' ||
                ch == '\f' || ch == '\u000b';
    }

	public static boolean isNewLine(int ch) {
		return ch == '\r' || ch == '\n';
	}
    
    /**
     * Determines whether ch is a digit (0-9).
     */
    public static boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }
    
    /**
     * Determines whether ch is an upper case English alphabet. 
     */
    public static boolean isUpperAlpha(int ch) {
        return ch >= 'A' && ch <= 'Z';
    }
    
    /**
     * Determines whether ch is a lower case English alphabet.
     */
    public static boolean isLowerAlpha(int ch) {
        return ch >= 'a' && ch <= 'z';
    }
    
    /**
     * Determines whether ch is an English alphabet.
     */
    public static boolean isAlpha(int ch) {
        return isUpperAlpha(ch) || isLowerAlpha(ch);
    }
    
    /**
     * Determines whether ch is a digit or English alphabet. 
     */
    public static boolean isAlnum(int ch) {
        return isDigit(ch) || isAlpha(ch);
    }
    
    /**
     * Determines whether ch is a hexadecimal digit (including digits and A-F, ignoring case)
     */
    public static boolean isHexDigit(int ch) {
        return isDigit(ch) || (ch >= 'A' && ch <= 'F') || 
                (ch >= 'a' && ch <= 'f');
    }
    
    /**
     * Determines whether ch is valid identifier.
     * 
     * @param ch
     * @param starter true if it is to be also determined whether ch can
     * be a valid start identifier.
     */
    public static boolean isValidC89IdentifierChar(int ch, boolean starter) {
        if (ch == '_' || isAlpha(ch)) {
            return true;
        }
        return !starter && isDigit(ch);
    }
    
    /**
     * Parser decimal string as an 32-bit non-negative integer. No leading sign is accepted.
     * 
     * @param s string of decimal digits.
     * @param start start of substring
     * @param end end of substring
     * 
     * @return positive number (or 0).
     */
    public static int parseDecimalString(CharSequence s, int start, int end) {
        if (s.length() == 0 || start >= end) {
            throw new RuntimeException("received empty substring");
        }
        int num = 0;
        for (int i = start; i < end; i++) {
            char ch = s.charAt(i);
            int digit = ch - '0';
            if (digit < 0 || digit > 9) {
                throw new IllegalArgumentException("non hex digit found at " +
                        "index "  + i + ": " + ch);
            }
            num = Math.multiplyExact(num, 10);
            num = Math.addExact(num, digit);
        }
        return num;
    }    
    
    /**
     * Parser hexadecimal string as an 32-bit non-negative integer. No leading sign is accepted.
     * 
     * @param s hexadecimal string
     * @param start start of substring
     * @param end end of substring
     * 
     * @return positive number (or 0).
     */
    public static int parseHexadecimalString(CharSequence s, int start, int end) {
        if (s.length() == 0 || start >= end) {
            throw new RuntimeException("received empty substring");
        }
        int num = 0;
        for (int i = start; i < end; i++) {
            char ch = s.charAt(i);
            int digit;
            if (ch >= 'A' && ch <= 'F') {
                digit = ch - 'A' + 10;
            }
            else if (ch >= 'a' && ch <= 'f') {
                digit = ch - 'a' + 10;
            }
            else {
                digit = ch - '0';
            }
            if (digit < 0 || digit > 15) {
                throw new IllegalArgumentException("non hex digit found at " +
                        "index "  + i + ": " + ch);
            }
            num = Math.multiplyExact(num, 16);
            num = Math.addExact(num, digit);
        }
        return num;
    }
}

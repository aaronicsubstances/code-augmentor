package com.aaronicsubstances.programmer.companion;

/**
 * Contains various utility methods that a hand-crafted lexer might need.
 */
public class LexerSupport {
    
    /**
     * Determines whether ch is a whitespace as defined in C89.
     */
    public static boolean isWhitespace(int ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' ||
                ch == '\f' || ch == '\u000B'; // last ch is C's '\v'.
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
    public static boolean isValidIdentifierChar(int ch, boolean starter) {
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
     * @returns positive number (or 0).
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
     * @returns positive number (or 0).
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

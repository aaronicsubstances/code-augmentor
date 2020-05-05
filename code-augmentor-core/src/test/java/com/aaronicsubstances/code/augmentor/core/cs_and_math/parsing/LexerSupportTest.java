package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;

public class LexerSupportTest {
    public static final int TOKEN_TEST_TYPE_1 = 2;
    public static final String TOKEN_ERROR_1_TEST_TYPE_1 = "2cz0";

    @Test(dataProvider = "createTestGetTokenNameData")
    public void testGetTokenName(int type, Class<?> cls, String typePrefix, String defName,
            String expectedName) {
        String actual = LexerSupport.getTokenName(type, cls, typePrefix, defName);
        assertEquals(actual, expectedName);
    }

    @DataProvider
    public Object[][] createTestGetTokenNameData() {
        return new Object[][]{
            new Object[]{ 2, LexerSupportTest.class, "TOKEN_TEST_TYPE_", null, "1" },
            new Object[]{ 2, LexerSupportTest.class, "TOKEN_T", null, "EST_TYPE_1" },
        };
    }

    @Test(dataProvider = "createTestGetTokenNameErrorData", 
        expectedExceptions = RuntimeException.class)
    public void testGetTokenNameWithError(Class<?> cls, String typePrefix) {
        LexerSupport.getTokenName(1, cls, typePrefix, null);
    }

    @DataProvider
    public Object[][] createTestGetTokenNameErrorData() {
        return new Object[][]{
            new Object[]{ LexerSupportTest.class, "TOKEN_ERROR_1_" },
        };
    }
    
    @Test(dataProvider = "createTestCalculateLineAndColumnNumbersData")
    public void testCalculateLineAndColumnNumbers(String s, int pos,
            int expLineNumber, int expColumnNumber) {
        int[] expected = new int[]{ expLineNumber, expColumnNumber };
        int[] actual = LexerSupport.calculateLineAndColumnNumbers(s, pos);
        assertEquals(actual, expected);
    }
    
    @DataProvider
    public Object[][] createTestCalculateLineAndColumnNumbersData() {
        String inputWin32 = TestResourceLoader.loadResourceNewlinesNormalized("testConsume-win32.txt", 
            getClass(), "\r\n");
        String inputUnix = TestResourceLoader.loadResourceNewlinesNormalized("testConsume-unix.txt", 
            getClass(), "\n");
        String inputMac = TestResourceLoader.loadResourceNewlinesNormalized("testConsume-mac.txt", 
            getClass(), "\r");

        return new Object[][]{
            { "", 0, 1, 1 },
            { "\n", 1, 2, 1 },
            { "abc", 3, 1, 4 },
            { "ab\nc", 4, 2, 2 },
            { "ab\nc\r\n", 6, 3, 1 },

            new Object[]{ inputWin32, 1, 1, 2 },
            new Object[]{ inputWin32, 4, 1, 5 },
            new Object[]{ inputWin32, 5, 1, 6 },
            new Object[]{ inputWin32, 9, 1, 10 },
            new Object[]{ inputWin32, 10, 1, 11 },
            new Object[]{ inputWin32, 12, 1, 13 },
            new Object[]{ inputWin32, 13, 1, 14 },
            new Object[]{ inputWin32, 14, 1, 15 }, // test that encountering \r does not lead to newline
            new Object[]{ inputWin32, 15, 2, 1 },  // until \n here on this line.
            new Object[]{ inputWin32, 18, 2, 4 },

            new Object[]{ inputUnix, 1, 1, 2 },
            new Object[]{ inputUnix, 4, 1, 5 },
            new Object[]{ inputUnix, 5, 1, 6 },
            new Object[]{ inputUnix, 9, 1, 10 },
            new Object[]{ inputUnix, 10, 1, 11 },
            new Object[]{ inputUnix, 12, 1, 13 },
            new Object[]{ inputUnix, 13, 1, 14 },
            new Object[]{ inputUnix, 14, 2, 1 },
            new Object[]{ inputUnix, 17, 2, 4 },

            new Object[]{ inputMac, 1, 1, 2 },
            new Object[]{ inputMac, 4, 1, 5 },
            new Object[]{ inputMac, 5, 1, 6 },
            new Object[]{ inputMac, 9, 1, 10 },
            new Object[]{ inputMac, 10, 1, 11 },
            new Object[]{ inputMac, 12, 1, 13 },
            new Object[]{ inputMac, 13, 1, 14 },
            new Object[]{ inputMac, 14, 2, 1 },
            new Object[]{ inputMac, 17, 2, 4 },
        };
    }

    @Test(dataProvider = "createTestIsWhitespaceData")
    public void testIsWhitespace(int ch, boolean expResult) {
        boolean result = LexerSupport.isWhitespace(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsWhitespaceData() {
        return new Object[][]{
            new Object[]{ (int)'a', false },
            new Object[]{ (int)' ', true },
            new Object[]{ (int)'\t', true },
            new Object[]{ (int)'\n', true },
            new Object[]{ (int)'\r', true },
            new Object[]{ (int)'\f', true },
            new Object[]{ (int)'\u000b', false },
            new Object[]{ (int)'0', false },
            new Object[]{ (int)'_', false }
        };
    }

    @Test(dataProvider = "createTestIsNewLineData")
    public void testIsNewLine(int ch, boolean expResult) {
        boolean result = LexerSupport.isNewLine(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsNewLineData() {
        return new Object[][]{
            new Object[]{ (int)'a', false },
            new Object[]{ (int)' ', false },
            new Object[]{ (int)'\t', false },
            new Object[]{ (int)'\n', true },
            new Object[]{ (int)'\r', true },
            new Object[]{ (int)'\f', false },
            new Object[]{ (int)'\u000b', false },
            new Object[]{ (int)'0', false },
            new Object[]{ (int)'_', false }
        };
    }

    @Test(dataProvider = "createTestIsDigitData")
    public void testIsDigit(int ch, boolean expResult) {
        boolean result = LexerSupport.isDigit(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsDigitData() {
        return new Object[][]{
            new Object[]{ (int)'a', false },
            new Object[]{ (int)'9', true },
            new Object[]{ (int)'\t', false },
            new Object[]{ (int)'\n', false },
            new Object[]{ (int)'\r', false },
            new Object[]{ (int)'6', true },
            new Object[]{ (int)'2', true },
            new Object[]{ (int)'0', true },
            new Object[]{ (int)'_', false }
        };
    }

    @Test(dataProvider = "createTestIsUpperAlphaData")
    public void testIsUpperAlpha(int ch, boolean expResult) {
        boolean result = LexerSupport.isUpperAlpha(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsUpperAlphaData() {
        return new Object[][]{
            new Object[]{ (int)'a', false },
            new Object[]{ (int)'Z', true },
            new Object[]{ (int)'8', false },
            new Object[]{ (int)'\n', false },
            new Object[]{ (int)'\r', false },
            new Object[]{ (int)'Q', true },
            new Object[]{ (int)'C', true },
            new Object[]{ (int)'A', true },
            new Object[]{ (int)'_', false }
        };
    }

    @Test(dataProvider = "createTestIsLowerAlphaData")
    public void testIsLowerAlpha(int ch, boolean expResult) {
        boolean result = LexerSupport.isLowerAlpha(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsLowerAlphaData() {
        return new Object[][]{
            new Object[]{ (int)'A', false },
            new Object[]{ (int)'z', true },
            new Object[]{ (int)'6', false },
            new Object[]{ (int)'\n', false },
            new Object[]{ (int)'\r', false },
            new Object[]{ (int)'m', true },
            new Object[]{ (int)'c', true },
            new Object[]{ (int)'a', true },
            new Object[]{ (int)'_', false }
        };
    }

    @Test(dataProvider = "createTestIsAlphaData")
    public void testIsAlpha(int ch, boolean expResult) {
        boolean result = LexerSupport.isAlpha(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsAlphaData() {
        return new Object[][]{
            new Object[]{ (int)'A', true },
            new Object[]{ (int)'z', true },
            new Object[]{ (int)'C', true },
            new Object[]{ (int)'9', false },
            new Object[]{ (int)'0', false },
            new Object[]{ (int)'Z', true },
            new Object[]{ (int)'\r', false },
            new Object[]{ (int)'m', true },
            new Object[]{ (int)'c', true },
            new Object[]{ (int)'a', true },
            new Object[]{ (int)'_', false }
        };
    }

    @Test(dataProvider = "createTestIsAlnumData")
    public void testIsAlnum(int ch, boolean expResult) {
        boolean result = LexerSupport.isAlnum(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsAlnumData() {
        return new Object[][]{
            new Object[]{ (int)'A', true },
            new Object[]{ (int)'z', true },
            new Object[]{ (int)'C', true },
            new Object[]{ (int)'5', true },
            new Object[]{ (int)'9', true },
            new Object[]{ (int)'0', true },
            new Object[]{ (int)'8', true },
            new Object[]{ (int)'2', true },
            new Object[]{ (int)'3', true },
            new Object[]{ (int)'Z', true },
            new Object[]{ (int)'\r', false },
            new Object[]{ (int)'m', true },
            new Object[]{ (int)'c', true },
            new Object[]{ (int)'a', true },
            new Object[]{ (int)'_', false }
        };
    }

    @Test(dataProvider = "createTestIsHexDigitData")
    public void testIsHexDigit(int ch, boolean expResult) {
        boolean result = LexerSupport.isHexDigit(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsHexDigitData() {
        return new Object[][]{
            new Object[]{ (int)'A', true },
            new Object[]{ (int)'z', false },
            new Object[]{ (int)'C', true },
            new Object[]{ (int)'E', true },
            new Object[]{ (int)'F', true },
            new Object[]{ (int)'G', false },
            new Object[]{ (int)'H', false },
            new Object[]{ (int)'5', true },
            new Object[]{ (int)'9', true },
            new Object[]{ (int)'0', true },
            new Object[]{ (int)'8', true },
            new Object[]{ (int)'2', true },
            new Object[]{ (int)'3', true },
            new Object[]{ (int)'Z', false },
            new Object[]{ (int)'\r', false },
            new Object[]{ (int)'m', false },
            new Object[]{ (int)'c', true },
            new Object[]{ (int)'a', true },
            new Object[]{ (int)'e', true },
            new Object[]{ (int)'f', true },
            new Object[]{ (int)'g', false },
            new Object[]{ (int)'h', false },
            new Object[]{ (int)'_', false }
        };
    }

    @Test(dataProvider = "createTestIsValidIdentifierCharData")
    public void testIsValidIdentifierChar(int ch, boolean starter, boolean expResult) {
        boolean result = LexerSupport.isValidIdentifierChar(ch, starter);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsValidIdentifierCharData() {
        return new Object[][]{
            new Object[]{ (int)'A', true, true },
            new Object[]{ (int)'z', false, true },
            new Object[]{ (int)'C', true, true },
            new Object[]{ (int)'E', true, true },
            new Object[]{ (int)'F', true, true },
            new Object[]{ (int)'G', false, true },
            new Object[]{ (int)'H', false, true },
            new Object[]{ (int)'5', true, false },
            new Object[]{ (int)'9', true, false },
            new Object[]{ (int)'0', false, true },
            new Object[]{ (int)'8', false, true },
            new Object[]{ (int)'2', true, false },
            new Object[]{ (int)'3', false, true },
            new Object[]{ (int)'Z', false, true },
            new Object[]{ (int)'\r', false, false },
            new Object[]{ (int)'m', false, true },
            new Object[]{ (int)'c', true, true },
            new Object[]{ (int)'a', true, true },
            new Object[]{ (int)'e', false, true },
            new Object[]{ (int)'f', false, true },
            new Object[]{ (int)'g', false, true },
            new Object[]{ (int)'"', true, false },
            new Object[]{ (int)'_', false, true },
            new Object[]{ (int)'_', true, true },
            new Object[]{ (int)'/', false, false }
        };
    }

    @Test(dataProvider = "createTestParseDecimalStringData")
    public void testParseDecimalString(CharSequence s, int start, int end,
            int expResult) {
        int result = LexerSupport.parseDecimalString(s, start, end);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestParseDecimalStringData() {
        return new Object[][]{
            new Object[]{ "0", 0, 1, 0 },
            new Object[]{ "10", 0, 2, 10 },
            new Object[]{ "1234567890", 0, 10, 1234567890 },
            new Object[]{ "9876543210", 0, 9, 987654321 },
            new Object[]{ "9876543210", 1, 10, 876543210 }
        };
    }
    
    @Test(dataProvider = "createTestParseDecimalStringForErrorData")
    public void testParseDecimalStringForError(CharSequence s, int start, 
            int end) {
        try {
            LexerSupport.parseDecimalString(s, start, end);
            fail("Expected RuntimeException");
        }
        catch (RuntimeException ex) {
            System.out.println(ex.getClass().getName() + ": " + 
                    ex.getMessage());
        }
    }
    
    @DataProvider
    public Object[][] createTestParseDecimalStringForErrorData() {
        return new Object[][]{
            new Object[]{ "", 0, 0 },
            new Object[]{ "3010", 3, 2 },
            new Object[]{ "3010", 2, 2 },
            new Object[]{ "ab", 3, 2 },
            new Object[]{ "x", 0, 1 },
            new Object[]{ "f2", 0, 1 },
            new Object[]{ "-2", 0, 2 },
            new Object[]{ "+3", 0, 2 },
            new Object[]{ "129876543210", 0, 12 }, // overflow
            new Object[]{ "98765g3210", 3, 10 }
        };
    }

    @Test(dataProvider = "createTestParseHexadecimalStringData")
    public void testParseHexadecimalString(CharSequence s, int start, int end,
            int expResult) {
        int result = LexerSupport.parseHexadecimalString(s, start, end);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestParseHexadecimalStringData() {
        return new Object[][]{
            new Object[]{ "0", 0, 1, 0 },
            new Object[]{ "F", 0, 1, 0xf },
            new Object[]{ "AFd3e", 0, 5, 0xafd3E },
            new Object[]{ "10", 0, 2, 0x10 },
            new Object[]{ "1234567890", 1, 9, 0x23456789 },
            new Object[]{ "-9876543210", 2, 9, 0x8765432 },
            new Object[]{ "9876543210", 3, 10, 0x6543210 }
        };
    }
    
    @Test(dataProvider = "createTestParseHexadecimalStringForErrorData")
    public void testParseHexadecimalStringForError(CharSequence s, int start, 
            int end) {
        try {
            LexerSupport.parseHexadecimalString(s, start, end);
            fail("Expected RuntimeException");
        }
        catch (RuntimeException ex) {
            System.out.println(ex.getClass().getName() + ": " + 
                    ex.getMessage());
        }
    }
    
    @DataProvider
    public Object[][] createTestParseHexadecimalStringForErrorData() {
        return new Object[][]{
            new Object[]{ "", 0, 0 },
            new Object[]{ "3010", 3, 2 },
            new Object[]{ "3010", 2, 2 },
            new Object[]{ "ab", 3, 2 },
            new Object[]{ "x", 0, 1 },
            new Object[]{ "-2", 0, 2 },
            new Object[]{ "+3", 0, 2 },
            new Object[]{ "9876543210", 0, 8 }, // overflow
            new Object[]{ "98765g3210", 3, 10 }
        };
    }
}
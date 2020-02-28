package com.aaronicsubstances.programmer.companion;

import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class LexerSupportTest {

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
            new Object[]{ (int)'\u000b', true },
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
            Reporter.log(ex.getClass().getName() + ": " + 
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
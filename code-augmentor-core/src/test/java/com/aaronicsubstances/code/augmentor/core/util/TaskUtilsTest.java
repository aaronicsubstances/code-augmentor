package com.aaronicsubstances.code.augmentor.core.util;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TaskUtilsTest {
    
    @Test(dataProvider = "createTestCalculateLineNumber")
    public void testCalculateLineNumber(String s, int pos, int expLineNumber) {
        int actual = TaskUtils.calculateLineNumber(s, pos);
        assertEquals(actual, expLineNumber);
    }
    
    @DataProvider
    public Object[][] createTestCalculateLineNumber() {
        String inputWin32 = TestResourceLoader.loadResourceNewlinesNormalized("testConsume-win32.txt", 
            getClass(), "\r\n");
        String inputUnix = TestResourceLoader.loadResourceNewlinesNormalized("testConsume-unix.txt", 
            getClass(), "\n");
        String inputMac = TestResourceLoader.loadResourceNewlinesNormalized("testConsume-mac.txt", 
            getClass(), "\r");

        return new Object[][]{
            { "", 0, 1, },
            { "\n", 1, 2, },
            { "abc", 3, 1, },
            { "ab\nc", 4, 2} ,
            { "ab\nc\r\n", 6, 3 },

            new Object[]{ inputWin32, 1, 1 },
            new Object[]{ inputWin32, 4, 1 },
            new Object[]{ inputWin32, 5, 1 },
            new Object[]{ inputWin32, 9, 1 },
            new Object[]{ inputWin32, 10, 1 },
            new Object[]{ inputWin32, 12, 1 },
            new Object[]{ inputWin32, 13, 1 },
            new Object[]{ inputWin32, 14, 1 }, // test that encountering \r does not lead to newline
            new Object[]{ inputWin32, 15, 2 },  // until \n here on this line.
            new Object[]{ inputWin32, 18, 2 },

            new Object[]{ inputUnix, 1, 1 },
            new Object[]{ inputUnix, 4, 1 },
            new Object[]{ inputUnix, 5, 1 },
            new Object[]{ inputUnix, 9, 1 },
            new Object[]{ inputUnix, 10, 1 },
            new Object[]{ inputUnix, 12, 1 },
            new Object[]{ inputUnix, 13, 1 },
            new Object[]{ inputUnix, 14, 2 },
            new Object[]{ inputUnix, 17, 2 },

            new Object[]{ inputMac, 1, 1 },
            new Object[]{ inputMac, 4, 1 },
            new Object[]{ inputMac, 5, 1 },
            new Object[]{ inputMac, 9, 1 },
            new Object[]{ inputMac, 10, 1 },
            new Object[]{ inputMac, 12, 1 },
            new Object[]{ inputMac, 13, 1 },
            new Object[]{ inputMac, 14, 2 },
            new Object[]{ inputMac, 17, 2 },
        };
    }

    @Test(dataProvider = "createTestSplitIntoLinesData")
    public void testSplitIntoLines(String text, List<String> expected) {
        List<String> actual = TaskUtils.splitIntoLines(text, true);
        assertEquals(actual, expected);
    }

    @DataProvider 
    public Object[][] createTestSplitIntoLinesData() {
        return new Object[][]{
            { "", Arrays.asList() },
            { "\n", Arrays.asList("", "\n") },
            { "abc", Arrays.asList("abc", null) },
            { "ab\nc", Arrays.asList("ab", "\n", "c", null) } ,
            { "ab\nc\r\n", Arrays.asList("ab", "\n", "c", "\r\n") },
        };
    }

    @Test(dataProvider = "createTestSplitIntoLinesCompactData")
    public void testSplitIntoLinesCompact(String text, List<String> expected) {
        List<String> actual = TaskUtils.splitIntoLines(text, false);
        assertEquals(actual, expected);
    }

    @DataProvider 
    public Object[][] createTestSplitIntoLinesCompactData() {
        return new Object[][]{
            { "", Arrays.asList() },
            { "\n", Arrays.asList("\n") },
            { "abc", Arrays.asList("abc") },
            { "ab\nc", Arrays.asList("ab\n", "c") } ,
            { "ab\nc\r\n", Arrays.asList("ab\n", "c\r\n") },
        };
    }

    @Test(dataProvider = "createTestIsNewLineData")
    public void testIsNewLine(char ch, boolean expResult) {
        boolean result = TaskUtils.isNewLine(ch);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsNewLineData() {
        return new Object[][]{
            new Object[]{ 'a', false },
            new Object[]{ ' ', false },
            new Object[]{ '\t', false },
            new Object[]{ '\n', true },
            new Object[]{ '\r', true },
            new Object[]{ '\f', false },
            new Object[]{ '\u000b', false },
            new Object[]{ '0', false },
            new Object[]{ '_', false }
        };
    }

    @Test(dataProvider = "createTestIsBlankData")
    public void testIsBlank(String s, boolean expResult) {
        boolean result = TaskUtils.isBlank(s);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsBlankData() {
        return new Object[][]{
            new Object[]{ " |", false },
            new Object[]{ "", true },
            new Object[]{ null, true },
            new Object[]{ " ", true },
            new Object[]{ "\t", true },
            new Object[]{ "  \f\r\n", true },
            new Object[]{ "\r", true },
            new Object[]{ "\b ", true },
            new Object[]{ "(d2", false },
            new Object[]{ "0", false },
            new Object[]{ "_", false }
        };
    }

    @Test(dataProvider = "createTestIsEmptyData")
    public void testIsEmpty(String s, boolean expResult) {
        boolean result = TaskUtils.isEmpty(s);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestIsEmptyData() {
        return new Object[][]{
            new Object[]{ " |", false },
            new Object[]{ "", true },
            new Object[]{ null, true },
            new Object[]{ " ", false },
            new Object[]{ "\t", false },
            new Object[]{ "  \f\r\n", false },
            new Object[]{ "\r", false },
            new Object[]{ "\b ", false },
            new Object[]{ "(d2", false },
            new Object[]{ "0", false },
            new Object[]{ "_", false }
        };
    }

    @Test(dataProvider = "createTestStrMultiplyData")
    public void testStrMultiply(String s, int nTimes, String expResult) {
        String result = TaskUtils.strMultiply(s, nTimes);
        assertEquals(result, expResult);
    }

    @DataProvider
    public Object[][] createTestStrMultiplyData() {
        return new Object[][] {
            {null, 0, null},
            {null, 2, null},
            {"", 0, ""},
            {"", 5, ""},
            {"yz", 0, ""},
            {"yz", 1, "yz"},
            {"yz", 2, "yzyz"},
            {"x", 1, "x"},
            {"x", 5, "xxxxx"},
        };
    }

    @Test(dataProvider = "createTestIsValidJsonData")
    public void testIsValidJson(String s, boolean expected) {
        String errorMessage = TaskUtils.validateJson(s);
        if (expected) {
            assertNull(errorMessage);
        }
        else {
            assertNotNull(errorMessage);
        }
    }

    @DataProvider
    public Object[][] createTestIsValidJsonData() {
        return new Object[][] {
            { "", false },
            { "     ", false },
            { null, false },
            { "\"\"", true },
            { "[", false },
            { "]", false },
            { "{}", true },
            { "[]{}", false },
            { "{}", true },
            { "\"yes\"", true },
            { "null", true },
            { "nul", false },
            { "2A0", false },
            { "0 e-2", false },
            { "0x2A0", false },
            { "0x2A 0", false },
            { "0.20", true },
            { ".020", false },
            { "0e-2", true },
            { "20 ", true },
            { "-920 ", true },
            { "true", true },
            { "tru", false },
            { " false", true },
            { "\"k\"", true },
            { "\"k", false },
            { "k\"", false },
            { " { \"k\": true, \"c\": \"no\" } ", true },
            { " []", true },
            { " [1, 3, null, \"3\"]", true },
            { " [1, 3, null, '3']", false },
            { "[]", true },
            { "[{}]", true },
            { "[\n]", true },
            { "[]\n", true }
        };
    }

    @Test(dataProvider = "createTestModifyNameToBeAbsentData")
    public void testModifyNameToBeAbsent(List<String> names, String originalName,
            String expected) {
        String actual = TaskUtils.modifyNameToBeAbsent(names, originalName);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestModifyNameToBeAbsentData() {
        return new Object[][]{
            { Arrays.asList(), "nay", "nay" },
            { Arrays.asList(), "", "" },
            { Arrays.asList("nay"), "nay", "nay-1" },
            { Arrays.asList("so", "hi", "hi-1"), "hi", "hi-2" },
            { Arrays.asList(""), "", "-1" },
            { Arrays.asList("sand", "sand-1", "sand-2"), "sand-1", "sand-1-1" }
        };
    }

    @Test(dataProvider = "createTestCalcHashData")
    public void testCalcHash(String str, String expected) throws Exception {
        String actual = TaskUtils.calcHash(str, StandardCharsets.UTF_8);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestCalcHashData() {
        return new Object[][]{
            { "", "d41d8cd98f00b204e9800998ecf8427e" },
            { "The quick brown fox jumps over the lazy dog", "9e107d9d372bb6826bd81d3542a419d6" },
            { "The quick brown fox jumps over the lazy dog.", "e4d909c290d0fb1ca068ffaddf22cbd0" }
        };
    }
}
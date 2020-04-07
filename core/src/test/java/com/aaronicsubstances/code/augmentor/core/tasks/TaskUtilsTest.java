package com.aaronicsubstances.code.augmentor.core.tasks;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

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
        List<String> actual = TaskUtils.splitIntoLines(text);
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
}
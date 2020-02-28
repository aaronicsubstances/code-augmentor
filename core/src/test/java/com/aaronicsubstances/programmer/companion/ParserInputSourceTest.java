package com.aaronicsubstances.programmer.companion;

import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ParserInputSourceTest {

    @Test(dataProvider = "createTestCreateAbortExceptionData")
    public void testCreateAbortException(
            String inputPath, int position, int lineNumber, int columnNumber, Token token, 
            int expectedLineNumber, int expectedColumnNumber, String expectedSnippetPath) {
        String input = TestResourceLoader.loadResource(inputPath, getClass());
        String expectedSnippet = TestResourceLoader.loadResource(expectedSnippetPath, getClass());
        ParserInputSource instance = new ParserInputSource(input);
        instance.setPosition(position);
        instance.setLineNumber(lineNumber);
        instance.setColumnNumber(columnNumber);
        ParserException parseException = instance.createAbortException("Test parse error", token);
        assertEquals(parseException.getLineNumber(), expectedLineNumber);
        assertEquals(parseException.getColumnNumber(), expectedColumnNumber);
        assertEquals(parseException.getSnippet(), expectedSnippet);
        System.out.format("Exception message for snippet %s: %s\n", expectedSnippetPath,
            parseException.getMessage());
    }

    @DataProvider
    public Object[][] createTestCreateAbortExceptionData() {
        Token eofToken = new Token();
        Token macToken = new Token(1, null, 26, 31, 3, 1);
        return new Object[][]{
            new Object[]{ "testConsume-win32.txt", 10, 1, 11, null, 1, 11, "testAbortSnippet-01.txt" },
            new Object[]{ "testConsume-unix.txt", 14, 2, 4, eofToken, 2, 4, "testAbortSnippet-02.txt" },
            new Object[]{ "testConsume-mac.txt", 0, 1, 1, macToken, 3, 1, "testAbortSnippet-03.txt" },
        };
    }

    @Test(dataProvider = "createTestLookaheadData")
    public void testLookahead(String input, int initialPosition, int offset, int expected) {
        ParserInputSource instance = new ParserInputSource(input);
        instance.setPosition(initialPosition);
        int actual = instance.lookahead(offset);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestLookaheadData() {
        return new Object[][] {
            new Object[]{ "", 0, 3, -1 },
            new Object[]{ "abc", 0, 0, (int)'a' },
            new Object[]{ "a\r\n", 1, 0, (int)'\r' },
            new Object[]{ "a\r\n1", 1, 1, (int)'\n' },
            new Object[]{ "a\r\n1", 1, 2, (int)'1' },
            new Object[]{ "a\r\n1", 1, 3, -1 },
        };
    }

    @Test(dataProvider = "createTestConsumeDataWin32")
    public void testConsumeWithWindowsLineEnding(ParserInputSource instance, int count, int expectedPosition, 
            int expectedLineNumber, int expectedColumnNumber) {
		instance.consume(count);
		assertEquals(instance.getPosition(), expectedPosition, "Position mismatch!");
		assertEquals(instance.getLineNumber(), expectedLineNumber, "Line number mismatch!");
		assertEquals(instance.getColumnNumber(), expectedColumnNumber, "Column number mismatch!");
	}
    
    @DataProvider
	public Object[][] createTestConsumeDataWin32() {
        String input = TestResourceLoader.loadResource("testConsume-win32.txt", 
            ParserInputSourceTest.class);
        ParserInputSource instance = new ParserInputSource(input);
        return new Object[][] {
            // line 1.
            new Object[]{ instance, 1, 1, 1, 2 },
            new Object[]{ instance, 3, 4, 1, 5 },
            new Object[]{ instance, 1, 5, 1, 6 },
            new Object[]{ instance, 4, 9, 1, 10 },
            new Object[]{ instance, 1, 10, 1, 11 },
            new Object[]{ instance, 2, 12, 1, 13 },
            new Object[]{ instance, 1, 13, 1, 14 },
            new Object[]{ instance, 1, 14, 1, 15 },
            new Object[]{ instance, 1, 15, 2, 1 },

            // no-op
            new Object[]{ instance, 0, 15, 2, 1 },

            // line 2.
            new Object[]{ instance, 3, 18, 2, 4 },
            new Object[]{ instance, 1, 19, 2, 5 },
            new Object[]{ instance, 3, 22, 2, 8 },
            new Object[]{ instance, 1, 23, 2, 9 },
            new Object[]{ instance, 2, 25, 2, 11 },
            new Object[]{ instance, 1, 26, 2, 12 },
            new Object[]{ instance, 1, 27, 2, 13 },
            new Object[]{ instance, 1, 28, 3, 1 },

            // line 3.
            new Object[]{ instance, 5, 33, 3, 6 },
            new Object[]{ instance, 2, 35, 4, 1 },

            // line 4
            new Object[]{ instance, 1, 36, 4, 2 },

            // EOF
            new Object[]{ instance, 1, 36, 4, 2 },
            new Object[]{ instance, 2, 36, 4, 2 },
            new Object[]{ instance, 0, 36, 4, 2 },
        };
    }

    @Test(dataProvider = "createTestConsumeDataUnix")
    public void testConsumeWithUnixLineEnding(ParserInputSource instance, int count, int expectedPosition, 
            int expectedLineNumber, int expectedColumnNumber) {
		instance.consume(count);
		assertEquals(instance.getPosition(), expectedPosition, "Position mismatch!");
		assertEquals(instance.getLineNumber(), expectedLineNumber, "Line number mismatch!");
		assertEquals(instance.getColumnNumber(), expectedColumnNumber, "Column number mismatch!");
	}
    
    @DataProvider
	public Object[][] createTestConsumeDataUnix() {
        String input = TestResourceLoader.loadResource("testConsume-unix.txt", 
            ParserInputSourceTest.class);
        ParserInputSource instance = new ParserInputSource(input);
        return new Object[][] {
            // line 1.
            new Object[]{ instance, 1, 1, 1, 2 },
            new Object[]{ instance, 3, 4, 1, 5 },
            new Object[]{ instance, 1, 5, 1, 6 },
            new Object[]{ instance, 4, 9, 1, 10 },
            new Object[]{ instance, 1, 10, 1, 11 },
            new Object[]{ instance, 2, 12, 1, 13 },
            new Object[]{ instance, 1, 13, 1, 14 },
            new Object[]{ instance, 1, 14, 2, 1 },

            // no-op
            new Object[]{ instance, 0, 14, 2, 1 },

            // line 2.
            new Object[]{ instance, 3, 17, 2, 4 },
            new Object[]{ instance, 1, 18, 2, 5 },
            new Object[]{ instance, 3, 21, 2, 8 },
            new Object[]{ instance, 1, 22, 2, 9 },
            new Object[]{ instance, 2, 24, 2, 11 },
            new Object[]{ instance, 1, 25, 2, 12 },
            new Object[]{ instance, 1, 26, 3, 1 },

            // line 3.
            new Object[]{ instance, 5, 31, 3, 6 },
            new Object[]{ instance, 1, 32, 4, 1 },

            // line 4
            new Object[]{ instance, 1, 33, 4, 2 },

            // EOF
            new Object[]{ instance, 1, 33, 4, 2 },
            new Object[]{ instance, 2, 33, 4, 2 },
            new Object[]{ instance, 0, 33, 4, 2 },
        };
    }

    @Test(dataProvider = "createTestConsumeDataMac")
    public void testConsumeWithMacLineEnding(ParserInputSource instance, int count, int expectedPosition, 
            int expectedLineNumber, int expectedColumnNumber) {
		instance.consume(count);
		assertEquals(instance.getPosition(), expectedPosition, "Position mismatch!");
		assertEquals(instance.getLineNumber(), expectedLineNumber, "Line number mismatch!");
		assertEquals(instance.getColumnNumber(), expectedColumnNumber, "Column number mismatch!");
	}
    
    @DataProvider
	public Object[][] createTestConsumeDataMac() {
        String input = TestResourceLoader.loadResource("testConsume-mac.txt", 
            ParserInputSourceTest.class);
        ParserInputSource instance = new ParserInputSource(input);
        return new Object[][] {
            // line 1.
            new Object[]{ instance, 1, 1, 1, 2 },
            new Object[]{ instance, 3, 4, 1, 5 },
            new Object[]{ instance, 1, 5, 1, 6 },
            new Object[]{ instance, 4, 9, 1, 10 },
            new Object[]{ instance, 1, 10, 1, 11 },
            new Object[]{ instance, 2, 12, 1, 13 },
            new Object[]{ instance, 1, 13, 1, 14 },
            new Object[]{ instance, 1, 14, 2, 1 },

            // no-op
            new Object[]{ instance, 0, 14, 2, 1 },

            // line 2.
            new Object[]{ instance, 3, 17, 2, 4 },
            new Object[]{ instance, 1, 18, 2, 5 },
            new Object[]{ instance, 3, 21, 2, 8 },
            new Object[]{ instance, 1, 22, 2, 9 },
            new Object[]{ instance, 2, 24, 2, 11 },
            new Object[]{ instance, 1, 25, 2, 12 },
            new Object[]{ instance, 1, 26, 3, 1 },

            // line 3.
            new Object[]{ instance, 5, 31, 3, 6 },
            new Object[]{ instance, 1, 32, 4, 1 },

            // line 4
            new Object[]{ instance, 1, 33, 4, 2 },

            // EOF
            new Object[]{ instance, 1, 33, 4, 2 },
            new Object[]{ instance, 2, 33, 4, 2 },
            new Object[]{ instance, 0, 33, 4, 2 },
        };
    }
}
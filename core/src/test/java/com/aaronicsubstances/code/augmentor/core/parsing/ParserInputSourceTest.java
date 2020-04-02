package com.aaronicsubstances.code.augmentor.core.parsing;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;

public class ParserInputSourceTest {

    @Test(dataProvider = "createTestCreateAbortExceptionData")
    public void testCreateAbortException(
            String inputPath, int position, Token token, 
            int expectedLineNumber, int expectedColumnNumber, String expectedSnippetPath) {
        String input = loadInput(inputPath, getClass());
        String expectedSnippet = TestResourceLoader.loadResourceNewlinesNormalized(
            expectedSnippetPath, getClass(), "\n");

        ParserInputSource instance = new ParserInputSource(input);
        instance.setPosition(position);
        ParserException parseException = instance.createAbortException("Test parse error", token);
        assertEquals(parseException.getLineNumber(), expectedLineNumber);
        assertEquals(parseException.getColumnNumber(), expectedColumnNumber);
        assertEquals(parseException.getSnippet(), expectedSnippet);
        System.out.format("Exception message for snippet %s: %s\n", expectedSnippetPath,
            parseException.getMessage());
    }

    private static String loadInput(String path, Class<?> class1) {		
        if (path.contains("mac")) {
            return TestResourceLoader.loadResourceNewlinesNormalized(path, class1, "\r");
        }
        else if (path.contains("win32")) {
            return TestResourceLoader.loadResourceNewlinesNormalized(path, class1, "\r\n");
        }
        else {
            // assume unix.
            return TestResourceLoader.loadResourceNewlinesNormalized(path, class1, "\n");
        }
	}

	@DataProvider
    public Object[][] createTestCreateAbortExceptionData() {
        Token eofToken = new Token();
        Token macToken = new Token(1, null, 26, 31, 3);
        return new Object[][]{
            new Object[]{ "testConsume-win32.txt", 10, null, 1, 11, "testAbortSnippet-01.txt" },
            new Object[]{ "testConsume-unix.txt", 17, eofToken, 2, 4, "testAbortSnippet-02.txt" },
            new Object[]{ "testConsume-mac.txt", 0, macToken, 3, 1, "testAbortSnippet-03.txt" },
        };
    }
}
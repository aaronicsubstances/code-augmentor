package com.aaronicsubstances.code.augmentor.core.parsing.java;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.parsing.Token;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JavaParserTest {

    @Test(dataProvider = "createTestData")
    public void test(int inputIndex, int expectedTokenListIndex) {
        String input = loadInput(inputIndex);
        JavaParser instance = new JavaParser(input);
        if (inputIndex == 1) {
            System.out.format("\nParsing JavaParserTest#%s\n" +
                "-------------------------------\n", inputIndex);
            instance.setTraceLog(System.err::println);
            instance.setVerbose(true);
        }

        List<Token> expected = new ArrayList<>(TestResourceLoader.deserializeTokens(
            expectedTokenListIndex, getClass()));
        List<Token> actual = new ArrayList<>(instance.parse());

        // make list same in length to make error messages clearer.
        int maxLen = Math.max(expected.size(), actual.size());
        List<Token> shorter = expected.size() == maxLen ? actual : expected;
        for (int i = shorter.size(); i < maxLen; i++) {
            shorter.add(null);
        }
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestData() {
        return new Object[][]{
            new Object[]{ 0, 0 },
            new Object[]{ 1, 1 },
        };
    }
    
    private static String loadInput(int i) {
        String path = String.format("input-%02d.txt", i);
        String text = TestResourceLoader.loadResourceNewlinesNormalized(path, JavaParserTest.class,
            "\r\n");
        return text;
    }
}
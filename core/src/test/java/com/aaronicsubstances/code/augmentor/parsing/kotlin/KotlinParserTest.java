package com.aaronicsubstances.code.augmentor.parsing.kotlin;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.parsing.Token;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class KotlinParserTest {

    @Test(dataProvider = "createTestData")
    public void test(int inputIndex, int expectedTokenListIndex) {
        String input = loadInput(inputIndex);
        KotlinParser instance = new KotlinParser(input);

        if (inputIndex == 2) {
            instance.setTraceLog(System.err::println);
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
            new Object[]{ 2, 2 },
            new Object[]{ 3, 3 },
            new Object[]{ 4, 4 },
        };
    }
    
    private static String loadInput(int i) {
        String path = String.format("input-%02d.txt", i);
        String text = TestResourceLoader.loadResourceNewlinesNormalized(path, KotlinParserTest.class,
            "\r\n");
        return text;
    }
}
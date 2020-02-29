package com.aaronicsubstances.programmer.companion.java;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.programmer.companion.TestResourceLoader;
import com.aaronicsubstances.programmer.companion.Token;
import com.google.gson.Gson;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JavaParserTest {

    @Test(dataProvider = "createTestData")
    public void test(int inputIndex, int expectedTokenListIndex) {
        String input = loadInput(inputIndex);
        JavaSourceCodeWrapper inputSource = new JavaSourceCodeWrapper(input);
        JavaParser instance = new JavaParser(inputSource);

        List<Token> expected = new ArrayList<>(deserializeTokens(expectedTokenListIndex));
        List<Token> actual = new ArrayList<>(instance.start());
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
    
    private static List<Token> deserializeTokens(int i) {
        String path = String.format("tokens-%02d.json", i);
        String text = TestResourceLoader.loadResource(path, JavaParserTest.class);
        Gson gson = new Gson();
        Token[] tokens = gson.fromJson(text, Token[].class);
        return Arrays.asList(tokens);
    }
    
    private static String loadInput(int i) {
        String path = String.format("input-%02d.txt", i);
        String text = TestResourceLoader.loadResource(path, JavaParserTest.class);
        return text;
    }
}
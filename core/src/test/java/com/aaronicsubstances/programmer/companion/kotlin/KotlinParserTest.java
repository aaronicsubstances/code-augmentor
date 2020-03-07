package com.aaronicsubstances.programmer.companion.kotlin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.TestResourceLoader;
import com.aaronicsubstances.programmer.companion.Token;
import com.google.gson.Gson;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class KotlinParserTest {

    @Test(dataProvider = "createTestData")
    public void test(int inputIndex, int expectedTokenListIndex) {
        String input = loadInput(inputIndex);
        ParserInputSource inputSource = new ParserInputSource(input);
        KotlinParser instance = new KotlinParser(inputSource);

        List<Token> expected = new ArrayList<>(deserializeTokens(expectedTokenListIndex));
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
    
    private static List<Token> deserializeTokens(int i) {
        String path = String.format("tokens-%02d.json", i);
        String text = TestResourceLoader.loadResource(path, KotlinParserTest.class);
        Gson gson = new Gson();
        Token[] tokens = gson.fromJson(text, Token[].class);
        return Arrays.asList(tokens);
    }
    
    private static String loadInput(int i) {
        String path = String.format("input-%02d.txt", i);
        String text = TestResourceLoader.loadResourceNewlinesNormalized(path, KotlinParserTest.class,
            "\r\n");
        return text;
    }
}
package com.aaronicsubstances.code.augmentor.core.util;

import static org.testng.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.TestArg;
import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.FiniteStateAutomaton;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetChangeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetChangeDescriptor.ExactValue;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.google.common.collect.Sets;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMap;
import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMapEntry;
import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.craftErrorMessageInvolvingRandomContentParts;

public class GeneratedCodeSimilarityCheckerTest {
    
    @Test(dataProvider = "createTestBuildSimilarityRegexData")
    public void testBuildSimilarityRegex(TestArg<List<ContentPart>> contentPartsWrapper, 
            TestArg<List<Object>> expectedWrapper) {
        List<ContentPart> contentParts = contentPartsWrapper.value;
        List<Object> expected = expectedWrapper.value;
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(
            contentParts, false);
        List<Object> actual = instance.getSimilarityRegex();
        assertThat(actual, is(expected));
    }

    @DataProvider
    public Object[][] createTestBuildSimilarityRegexData() {
        Object anySpace = GeneratedCodeSimilarityChecker.MATCH_TYPE_ANY_SPACES;
        Object reqdSpace = GeneratedCodeSimilarityChecker.MATCH_TYPE_REQUIRE_SPACE;
        List<ContentPart> contentParts = Arrays.asList(
            new ContentPart(" a bicycle", false),
            new ContentPart("  \n,is it expensive?", false),
            new ContentPart("\na. maybe", false),
            new ContentPart("\nb. maybe  not ", false),
            new ContentPart("\n", false)
        );
        List<Object> expected = Arrays.asList(anySpace, "a", reqdSpace, "bicycle", anySpace, "\n",
            anySpace, ",is", reqdSpace, "it", reqdSpace, "expensive?", anySpace, "\n",
            anySpace, "a.", reqdSpace, "maybe", anySpace, "\n",
            anySpace, "b.", reqdSpace, "maybe", reqdSpace, "not", anySpace, "\n", anySpace);
        TestArg<List<ContentPart>> contentParts1Wrapper = new TestArg<>(contentParts);
        TestArg<List<Object>> expected1Wrapper = new TestArg<>(expected);

        contentParts = Arrays.asList(new ContentPart("", false));
        expected = Arrays.asList(anySpace);
        TestArg<List<ContentPart>> contentParts2Wrapper = new TestArg<>(contentParts);
        TestArg<List<Object>> expected2Wrapper = new TestArg<>(expected);

        contentParts = Arrays.asList(new ContentPart("", true));
        expected = Arrays.asList();
        TestArg<List<ContentPart>> contentParts3Wrapper = new TestArg<>(contentParts);
        TestArg<List<Object>> expected3Wrapper = new TestArg<>(expected);
        
        return new Object[][]{
            { contentParts1Wrapper, expected1Wrapper },
            { contentParts2Wrapper, expected2Wrapper },
            { contentParts3Wrapper, expected3Wrapper }
        };
    }

    @Test(dataProvider = "createTestBuildSimilarityRegex2")
    public void testBuildSimilarityRegex2(String path, TestArg<List<Object>> expectedWrapper) {
        List<ContentPart> inputContentParts = TestResourceLoader.loadContentParts(path, getClass());
        List<Object> expected = expectedWrapper.value;
        // TestResourceLoader.printTestHeader("testBuildSimilarityRegex", path,
        // expectedWrapper);
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(inputContentParts, false);
        List<Object> actual = instance.getSimilarityRegex();
        assertEquals(actual, expected,
                craftErrorMessageInvolvingRandomContentParts(inputContentParts, expected, actual));
    }

    @DataProvider
    public Object[][] createTestBuildSimilarityRegex2() {
        Object anySpace = GeneratedCodeSimilarityChecker.MATCH_TYPE_ANY_SPACES;
        Object reqdSpace = GeneratedCodeSimilarityChecker.MATCH_TYPE_REQUIRE_SPACE;
        List<Object> expected0 = Arrays.asList(anySpace, "ab", anySpace, "\n", anySpace, "c", reqdSpace, "d", anySpace,
                "\r", anySpace, "\r\n", anySpace, "\n", anySpace, "\n", anySpace, "\n", anySpace, "\r", anySpace, "e",
                anySpace, "\r", anySpace, "f", anySpace, "\r", anySpace, "gh", reqdSpace, "i", anySpace, "\r\n",
                anySpace, "j", anySpace, "\r", anySpace, "\r", anySpace, "\r", anySpace, "kL", "x\r\fx\n \r x\n",
                reqdSpace);
        List<Object> expected1 = Arrays.asList(anySpace, "\r", anySpace, "\n", anySpace, "\r", anySpace, "\r\n",
                anySpace, "\n", anySpace, "ab", anySpace, "\r\n", anySpace, "\r", anySpace, "c", anySpace, "\n",
                anySpace, "\n", anySpace, "\n", anySpace, "d", reqdSpace,
                "\r\f\tx\r\t \r\n\r \r\f\r\r\t\n \r  \f\t\nx");
        List<Object> expected2 = Arrays.asList(anySpace, "\r", anySpace, "\r", anySpace, "x", anySpace, "\r", anySpace,
                "y", anySpace, "\r", anySpace, "\r", anySpace, "\r", anySpace, "z", reqdSpace, "\f",
                "  \r\rx\nx \n\n  \r\n\r\nx x\f\f\f \nxx x\rx\nx\t");
        List<Object> expected3 = Arrays.asList(anySpace, "\n", anySpace, "\n", anySpace, "x", reqdSpace, "x", anySpace,
                "\n", anySpace, "\r", anySpace, "\r", anySpace, "\n", anySpace, "\n", anySpace, "\n", anySpace, "\n",
                anySpace, "q", anySpace, "\r\n", " \t\r\t \f\f\r\r\t x\n\r", "\r\r\n", anySpace, "x", anySpace, "\n",
                reqdSpace, "\r");
        List<Object> expected4 = Arrays.asList(anySpace, "\r", reqdSpace,
                " x\r\r\f\f\t\t\nxx\r\r\r\r\r\t \fx\n\t\r\f \fx\t\fx\f   \t\r \r", "x", "\t\r", anySpace, "\r",
                anySpace, "\r", anySpace);
        List<Object> expected5 = Arrays.asList(anySpace, "\r", reqdSpace,
                " x\r\r\f\f\t\t\nxx\r\r\r\r\r\t \fx\n\t\r\f \fx\t\fx\f   \t\r ", "x", "\t", "\r", anySpace, "\r",
                anySpace);

        return new Object[][] { { "similarity-test-data-00.json", new TestArg<>(expected0) },
                { "similarity-test-data-01.json", new TestArg<>(expected1) },
                { "similarity-test-data-02.json", new TestArg<>(expected2) },
                { "similarity-test-data-03.json", new TestArg<>(expected3) },
                { "similarity-test-data-04.json", new TestArg<>(expected4) },
                { "similarity-test-data-05.json", new TestArg<>(expected5) } };
    }

    @Test
    public void testAreExactTextsSimilarNonRandomly() {
        String input = "packing it ...";
        List<ContentPart> inputContentParts = Arrays.asList(new ContentPart(input, false));

        // TestResourceLoader.printTestHeader("testAreExactTextsSimilarNonRandomly");
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(inputContentParts, false);
        CodeSnippetChangeDescriptor actual = instance.match(input);
        Object actualDescription = createMismatchDescription(input, actual);
        String assertionMsg = craftErrorMessageInvolvingRandomContentParts(
            inputContentParts, null, actualDescription);
        assertEquals(actual, null, assertionMsg);
    }

    @Test(dataProvider = "createTestAreExactTextsSimilarData")
    public void testAreExactTextsSimilar(int inputPtr, TestArg<String> inputWrapper, TestArg<List<ContentPart>> c) {
        String input = inputWrapper.value;
        List<ContentPart> inputContentParts = c.value;

        // TestResourceLoader.printTestHeader("testAreExactTextsSimilar", inputPtr,
        // inputWrapper, c);
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(inputContentParts, false);
        CodeSnippetChangeDescriptor actual = instance.match(input);
        Object actualDescription = createMismatchDescription(input, actual);
        String assertionMsg = craftErrorMessageInvolvingRandomContentParts(
            inputContentParts, null, actualDescription);
        assertEquals(actual, null, assertionMsg);
    }

    @DataProvider
    public Iterator<Object[]> createTestAreExactTextsSimilarData() {
        List<String> inputs = new ArrayList<>();

        // case 1
        String input = TestResourceLoader.loadResourceNewlinesNormalized("input-unix.txt", getClass(), "\n");
        inputs.add(input);

        // case 2
        input = TestResourceLoader.loadResourceNewlinesNormalized("input-win.txt", getClass(), "\r\n");
        inputs.add(input);

        // add other cases "inline"

        // case 5
        inputs.add("");

        // case 6
        inputs.add("\n56");

        // case 7
        StringBuilder randInput = new StringBuilder();
        List<String> randChars = new ArrayList<>(Arrays.asList("\r", "\n"));
        for (int i = 0; i < 50; i++) {
            int j = TestResourceLoader.RAND_GEN.nextInt(randChars.size());
            randInput.append(randChars.get(j));
        }
        inputs.add(randInput.toString());

        // case 8.
        randInput = new StringBuilder();
        randChars.addAll(Arrays.asList("\t", " ", "\f", "x"));
        for (int i = 0; i < 50; i++) {
            int j = TestResourceLoader.RAND_GEN.nextInt(randChars.size());
            randInput.append(randChars.get(j));
        }
        inputs.add(randInput.toString());

        return new CodeSimilarityTestDataProvider(10, inputs);
    }

    @Test(dataProvider = "createTestMatchData")
    public void testMatch(TestArg<String> textArg, TestArg<List<ContentPart>> c,
            CodeSnippetChangeDescriptor expected) {
        String text = textArg.value;
        List<ContentPart> contentParts = c.value;
        // TestResourceLoader.printTestHeader("testMatch", textArg, c, expected);
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(contentParts, false);
        CodeSnippetChangeDescriptor actual = instance.match(text);
        Object actualDescription = createMismatchDescription(text, actual);
        String assertionMsg = craftErrorMessageInvolvingRandomContentParts(
            contentParts, expected, actualDescription);
        assertEquals(actual, expected, assertionMsg);
    }

    @DataProvider
    public Object[][] createTestMatchData() {
        String inputWin = TestResourceLoader.loadResourceNewlinesNormalized(
            "input-win.txt", getClass(), "\r\n");
        TestArg<String> testWinInput = new TestArg<>(inputWin);

        // applies all rules except rule 1-4, 6
        String text0 = TestResourceLoader.loadResourceNewlinesNormalized(
            "similarity-test-input-00.txt", getClass(), "\r\n");
        List<ContentPart> contentParts0 = buildContentParts(text0, Arrays.asList(
            new int[]{ 0, 2204 },
            new int[]{ 2204, 6060, 1 },
            new int[]{ 6060, 6973 },
            new int[]{ 6973, 7115, 1 },
            new int[]{ 7115 }
        ));

        // applies rules 1a, 2a, 5
        String text1 = TestResourceLoader.loadResourceNewlinesNormalized(
            "similarity-test-input-01.txt", getClass(), "\r\n");
        List<ContentPart> contentParts1 = buildContentParts(text1, Arrays.asList(
            new int[]{ 0, 6973 },
            new int[]{ 6973, 7110, 1 },
            new int[]{ 7110 }
        ));

        // tests required space of trailing space after exact content
        List<ContentPart> contentParts2 = buildContentParts(text1, Arrays.asList(
            new int[]{ 0, 6973 },
            new int[]{ 6973, 7115, 1 },
            new int[]{ 7115 }
        ));
        CodeSnippetChangeDescriptor err2 = new CodeSnippetChangeDescriptor();
        err2.setSrcCharIndex(7125);
        err2.setDestCharIndex(7125);
        err2.setType(GeneratedCodeSimilarityChecker.MISMATCH_TYPE_REQUIRED_SPACE);
        err2.setCurrentSection("");
        
        String input3 = " a";
        List<ContentPart> contentParts3 = Arrays.asList(new ContentPart("", false));
        CodeSnippetChangeDescriptor err3 = new CodeSnippetChangeDescriptor();
        err3.setSrcCharIndex(1);
        err3.setDestCharIndex(1);
        err3.setType(GeneratedCodeSimilarityChecker.MISMATCH_TYPE_END_OF_SECTION);
        err3.setCurrentSection("a");
        
        String input4 = " ";
        List<ContentPart> contentParts4 = Arrays.asList(new ContentPart("", true));
        CodeSnippetChangeDescriptor err4 = new CodeSnippetChangeDescriptor();
        err4.setSrcCharIndex(0);
        err4.setType(GeneratedCodeSimilarityChecker.MISMATCH_TYPE_END_OF_SECTION);
        err4.setCurrentSection(" ");
        
        String input5 = " ";
        List<ContentPart> contentParts5 = Arrays.asList(new ContentPart("", false));
        
        String input6 = "c 'x '";
        List<ContentPart> contentParts6 = Arrays.asList(new ContentPart("c ", false),
            new ContentPart("' ", true));
        CodeSnippetChangeDescriptor err6 = new CodeSnippetChangeDescriptor();
        err6.setSrcCharIndex(3);
        err6.setDestCharIndex(3);
        err6.setType(GeneratedCodeSimilarityChecker.MISMATCH_TYPE_EXACT);
        err6.setCurrentSection("x '");
        ExactValue expectedExactValue = new ExactValue();
        err6.setExpectedExactValue(expectedExactValue);
        expectedExactValue.setLength(2);
        expectedExactValue.setUpdatedSectionOffset(1);
        expectedExactValue.setUpdatedSection(" ");
        expectedExactValue.setPrefix("'");
        
        String input7 = "c'x '";
        List<ContentPart> contentParts7 = Arrays.asList(new ContentPart("c ", false),
            new ContentPart("' ", false));
        CodeSnippetChangeDescriptor err7 = new CodeSnippetChangeDescriptor();
        err7.setSrcCharIndex(1);
        err7.setDestCharIndex(1);
        err7.setType(GeneratedCodeSimilarityChecker.MISMATCH_TYPE_REQUIRED_SPACE);
        err7.setCurrentSection("'x '");
        
        String input8 = "\tc    \t'    '     ";
        List<ContentPart> contentParts8 = Arrays.asList(new ContentPart("c ", false),
            new ContentPart("' '", false));
        
        String input9 = "abcdefghijklmnopqrstuvwxyz01234\n" + "y" +
            "56789ABCDEFGHIJKLMNOPQRSTUVWXYZ98777777\n" +
            "1234567890123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        List<ContentPart> contentParts9 = Arrays.asList(
            new ContentPart("abcdefghijklmnopqrstuvwxyz01234\n" + "q" +
            "56789ABCDEFGHIJKLMNOPQRSTUVWXYZ98777777\n" +
            "1234567890123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", true));
        CodeSnippetChangeDescriptor err9 = new CodeSnippetChangeDescriptor();
        err9.setSrcCharIndex(32);
        err9.setDestCharIndex(32);
        err9.setType(GeneratedCodeSimilarityChecker.MISMATCH_TYPE_EXACT);
        err9.setCurrentSection("y" +
            "56789ABCDEFGHIJKLMNOPQRSTUVWX");
        expectedExactValue = new ExactValue();
        err9.setExpectedExactValue(expectedExactValue);
        expectedExactValue.setLength(118);
        expectedExactValue.setUpdatedSectionOffset(32);
        expectedExactValue.setUpdatedSection("q" + 
            "56789ABCDEFGHIJKLMNOPQRSTUVWX");
        expectedExactValue.setPrefix("abcdefghijklmnopqrstuvwxyz0123");
        expectedExactValue.setSuffix("6789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        
        return new Object[][]{
            { testWinInput, new TestArg<>(contentParts0), null },
            { testWinInput, new TestArg<>(contentParts1), null },
            { testWinInput, new TestArg<>(contentParts2), err2 },
            { new TestArg<>(input3), new TestArg<>(contentParts3), err3 },
            { new TestArg<>(input4), new TestArg<>(contentParts4), err4 },
            { new TestArg<>(input5), new TestArg<>(contentParts5), null },
            { new TestArg<>(input6), new TestArg<>(contentParts6), err6 },
            { new TestArg<>(input7), new TestArg<>(contentParts7), err7 },
            { new TestArg<>(input8), new TestArg<>(contentParts8), null },
            { new TestArg<>(input9), new TestArg<>(contentParts9), err9 },
        };
    }

    @Test(dataProvider = "createTestFetchPrefixData")
    public void testFetchPrefix(String s, int start, int maxEnd, String expected) {
        String actual = GeneratedCodeSimilarityChecker.fetchPrefix(s, start, maxEnd);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestFetchPrefixData() {
        return new Object[][]{
            { "", 0, 0, "" },
            { "ab", 0, 5, "ab" },
            { "ab", 2, 5, "" },
            { "ab", 1, 0, "" },
            { "abcdefghij", 3, 3, "" },
            { "abcdefghij", 3, 6, "def" },
            { "abcdefghijklm nopqrstuvwxyz 0123456789", 0, 10, "abcdefghij" },
            { "abcdefghijklm nopqrstuvwxyz 0123456789", 0, 40, "abcdefghijklm nopqrstuvwxyz 01" },
            { "abcdefghijklm nopqrstuvwxyz 0123456789", 10, 20, "klm nopqrs" },
            { "abcdefghijklm nopqrstuvwxyz 0123456789", 10, 50, "klm nopqrstuvwxyz 0123456789" }
        };
    }

    @Test(dataProvider = "createTestFetchSuffixData")
    public void testFetchSuffix(String s, int minStart, String expected) {
        String actual = GeneratedCodeSimilarityChecker.fetchSuffix(s, minStart);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestFetchSuffixData() {
        return new Object[][]{
            { "", 0, "" },
            { "", 1, "" },
            { "abcd", 0, "abcd" },
            { "abcd", 1, "bcd" },
            { "abcd", 2, "cd" },
            { "abcd", 3, "d" },
            { "abcd", 4, "" },
            { "abcd", 5, "" },
            { "abcdefghijklm nopqrstuvwxyz 0123456789", 0, "ijklm nopqrstuvwxyz 0123456789" },
            { "abcdefghijklm nopqrstuvwxyz 0123456789", 10, "klm nopqrstuvwxyz 0123456789" }
        };
    }

    @Test(dataProvider = "createTestGetStatesReachableFromStartStateViaOneNonNullSymbolData")
    public void testGetStatesReachableFromStartStateViaOneNonNullSymbol(
            TestArg<FiniteStateAutomaton> nfaWrapper, Set<Integer> expected) {
        Set<Integer> actual = GeneratedCodeSimilarityChecker
            .getStatesReachableFromStartStateViaOneNonNullSymbol(nfaWrapper.value);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestGetStatesReachableFromStartStateViaOneNonNullSymbolData() {
        // nfa1
        Set<Integer> states = Sets.newHashSet(1, 2);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(1, newMap(Arrays.asList(
                newMapEntry(FiniteStateAutomaton.NULL_SYMBOL, Sets.newHashSet(2)))))
        ));
        FiniteStateAutomaton nfa1 = new FiniteStateAutomaton(null, states, 
            1, Sets.newHashSet(2), nfaTransitionTable, null);
        
        // nfa2
        states = Sets.newHashSet(0, 1, 2, 3);
        nfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0, 1)), newMapEntry(1, Sets.newHashSet(3))))),
            newMapEntry(1, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0)), newMapEntry(1, Sets.newHashSet(1, 3))))),
            newMapEntry(2, newMap(Arrays.asList(
                /*NULL,*/ newMapEntry(1, Sets.newHashSet(0, 2))))),
            newMapEntry(3, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0, 1, 2)), newMapEntry(1, Sets.newHashSet(1)))))
        ));
        FiniteStateAutomaton nfa2 = new FiniteStateAutomaton(null, states, 
            0, Sets.newHashSet(3), nfaTransitionTable, null);

        // nfa3
        states = Sets.newHashSet(0, 1, 2, 3);
        nfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(
                newMapEntry(FiniteStateAutomaton.NULL_SYMBOL, Sets.newHashSet(1))))),
            newMapEntry(1, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(2))))),
            newMapEntry(2, newMap(Arrays.asList(
                newMapEntry(FiniteStateAutomaton.NULL_SYMBOL, Sets.newHashSet(3)))))
        ));
        FiniteStateAutomaton nfa3 = new FiniteStateAutomaton(null, states, 
            0, Sets.newHashSet(), nfaTransitionTable, null);

        return new Object[][]{
            { new TestArg<>(nfa1), Sets.newHashSet() },
            { new TestArg<>(nfa2), Sets.newHashSet(1, 3) },
            { new TestArg<>(nfa3), Sets.newHashSet(2, 3) }
        };
    }

    private static List<ContentPart> buildContentParts(String input, List<int[]> ranges) {
        List<ContentPart> contentParts = new ArrayList<>();
        for (int[] range : ranges) {
            int startIdx = 0;
            if (range.length > 0) {
                startIdx = range[0];
            }
            int endIdx = input.length();
            if (range.length > 1) {
                endIdx = range[1];
            }
            boolean exactMatch = false;
            if (range.length > 2) {
                exactMatch = range[2] != 0;
            }
            String s = input.substring(startIdx, endIdx);
            contentParts.add(new ContentPart(s, exactMatch));
        }
        assertEquals(new GeneratedCode(contentParts).getWholeContent(), input);
        return contentParts;
    }

    private static Object createMismatchDescription(String input,
            CodeSnippetChangeDescriptor instance) {        
        Map<String, Object> descriptionObj = new HashMap<>();
        descriptionObj.put("input", input);
        descriptionObj.put("mismatch", instance);
        return descriptionObj;
    }
}
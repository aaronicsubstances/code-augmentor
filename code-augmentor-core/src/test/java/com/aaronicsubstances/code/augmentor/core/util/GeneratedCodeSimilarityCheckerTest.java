package com.aaronicsubstances.code.augmentor.core.util;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.TestArg;
import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.craftErrorMessageInvolvingRandomContentParts;

public class GeneratedCodeSimilarityCheckerTest {

    @Test(dataProvider = "createTestBuildSimilarityRegex")
    public void testBuildSimilarityRegex(String path, TestArg<List<Object>> expectedWrapper) {
        List<ContentPart> inputContentParts = TestResourceLoader.loadContentParts(path,
            getClass());
        List<Object> expected = expectedWrapper.value;
        //TestResourceLoader.printTestHeader("testBuildSimilarityRegex", path, expectedWrapper);
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(
            inputContentParts, false);
        List<Object> actual = instance.getSimilarityRegex();
        // because gson will output underlying regex node too deeply, map to string.
        assertEquals(actual, expected,
            craftErrorMessageInvolvingRandomContentParts(inputContentParts,
                expected.stream().map(x -> x.toString()).collect(Collectors.toList()),
                actual.stream().map(x -> x.toString()).collect(Collectors.toList())));
    }

    @DataProvider
    public Object[][] createTestBuildSimilarityRegex() {
        Object anySpace = GeneratedCodeSimilarityChecker.MATCH_TYPE_ANY_SPACES;
        Object reqdSpace = GeneratedCodeSimilarityChecker.MATCH_TYPE_REQUIRE_SPACE;
        List<Object> expected0 = Arrays.asList(
            anySpace, "ab", anySpace, "\n",
            anySpace, "c", reqdSpace, "d", anySpace, "\r",
            anySpace, "\r\n",
            anySpace, "\n",
            anySpace, "\n",
            anySpace, "\n",
            anySpace, "\r",
            anySpace, "e", anySpace, "\r",
            anySpace, "f", anySpace, "\r",
            anySpace, "gh", reqdSpace, "i", anySpace, "\r\n",
            anySpace, "j", anySpace, "\r",
            anySpace, "\r",
            anySpace, "\r",
            anySpace, "kL", "x\r\fx\n \r x\n",
            reqdSpace
        );
        List<Object> expected1 = Arrays.asList(
            anySpace, "\r",
            anySpace, "\n",
            anySpace, "\r",
            anySpace, "\r\n",
            anySpace, "\n",
            anySpace, "ab", anySpace, "\r\n",
            anySpace, "\r",
            anySpace, "c", anySpace, "\n",
            anySpace, "\n",
            anySpace, "\n",
            anySpace, "d", reqdSpace,
            "\r\f\tx\r\t \r\n\r \r\f\r\r\t\n \r  \f\t\nx"
        );
        List<Object> expected2 = Arrays.asList(
            anySpace, "\r",
            anySpace, "\r",
            anySpace, "x", anySpace, "\r",
            anySpace, "y", anySpace, "\r",
            anySpace, "\r",
            anySpace, "\r",
            anySpace, "z", reqdSpace,
            "\f",
            "  \r\rx\nx \n\n  \r\n\r\nx x\f\f\f \nxx x\rx\nx\t"
        );
        List<Object> expected3 = Arrays.asList(
            anySpace, "\n",
            anySpace, "\n",
            anySpace, "x", reqdSpace, "x", anySpace, "\n",
            anySpace, "\r",
            anySpace, "\r",
            anySpace, "\n",
            anySpace, "\n",
            anySpace, "\n",
            anySpace, "\n",
            anySpace, "q", anySpace, "\r\n",
            " \t\r\t \f\f\r\r\t x\n\r",
            "\r\r\n",
            anySpace, "x", anySpace, "\n",
            reqdSpace, "\r"
        );
        List<Object> expected4 = Arrays.asList(
            anySpace, "\r",
            reqdSpace, " x\r\r\f\f\t\t\nxx\r\r\r\r\r\t \fx\n\t\r\f \fx\t\fx\f   \t\r \r",
            "x", "\t\r",
            anySpace, "\r",
            anySpace, "\r",
            anySpace
        );
        List<Object> expected5 = Arrays.asList(
            anySpace, "\r",
            reqdSpace, " x\r\r\f\f\t\t\nxx\r\r\r\r\r\t \fx\n\t\r\f \fx\t\fx\f   \t\r ",
            "x", "\t",
            "\r",
            anySpace, "\r",
            anySpace
        );

        return new Object[][]{
            { "similarity-test-data-00.json", new TestArg<>(expected0) },
            { "similarity-test-data-01.json", new TestArg<>(expected1) },
            { "similarity-test-data-02.json", new TestArg<>(expected2) },
            { "similarity-test-data-03.json", new TestArg<>(expected3) },
            { "similarity-test-data-04.json", new TestArg<>(expected4) },
            { "similarity-test-data-05.json", new TestArg<>(expected5) }
        };
    }

    @Test
    public void testAreExactTextsSimilarNonRandomly() {
        String input = "packing it ...";
        List<ContentPart> inputContentParts = Arrays.asList(new ContentPart(input, false));

        //TestResourceLoader.printTestHeader("testAreExactTextsSimilarNonRandomly");
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(
            inputContentParts, false);
        boolean actual = instance.match(input);
        Object actualDescription = createMismatchDescription(input, instance);
        assertEquals(actual, true, craftErrorMessageInvolvingRandomContentParts(
            inputContentParts, true, actualDescription));
    }

    @Test(dataProvider = "createTestAreExactTextsSimilarData")
    public void testAreExactTextsSimilar(int inputPtr, TestArg<String> inputWrapper, 
            TestArg<List<ContentPart>> c) {
        String input = inputWrapper.value;
        List<ContentPart> inputContentParts = c.value;

        //TestResourceLoader.printTestHeader("testAreExactTextsSimilar", inputPtr, inputWrapper, c);
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(
            inputContentParts, false);
        boolean actual = instance.match(input);
        Object actualDescription = createMismatchDescription(input, instance);
        assertEquals(actual, true, craftErrorMessageInvolvingRandomContentParts(
            inputContentParts, true, actualDescription));
    }

    @DataProvider
    public Iterator<Object[]> createTestAreExactTextsSimilarData() {
        List<String> inputs = new ArrayList<>();

        // case 1
        String input = TestResourceLoader.loadResourceNewlinesNormalized("input-unix.txt", 
            getClass(), "\n");
        inputs.add(input);

        // case 2
        input = TestResourceLoader.loadResourceNewlinesNormalized("input-win.txt", 
            getClass(), "\r\n");
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
            boolean expected) {
        String text = textArg.value;
        List<ContentPart> contentParts = c.value;
        //TestResourceLoader.printTestHeader("testMatch", textArg, c, expected);
        GeneratedCodeSimilarityChecker instance = new GeneratedCodeSimilarityChecker(
            contentParts, false);
        boolean actual = instance.match(text);
        Object actualDescription = createMismatchDescription(text, instance);
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
            new int[]{ 6973, 7115, 1 },
            new int[]{ 7115 }
        ));
        return new Object[][]{
            { testWinInput, new TestArg<>(contentParts0), true },
            { testWinInput, new TestArg<>(contentParts1), false }
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
            GeneratedCodeSimilarityChecker instance) {        
        Map<String, Object> descriptionObj = new HashMap<>();
        descriptionObj.put("input", input);
        descriptionObj.put("errorIndex", instance.getErrorIndex());
        descriptionObj.put("expectedEOF", instance.isExpectedEOF());
        descriptionObj.put("actualEOF", instance.isActualEOF());
        descriptionObj.put("expected", instance.getExpected());
        descriptionObj.put("actual", instance.getActual());
        return descriptionObj;
    }
}
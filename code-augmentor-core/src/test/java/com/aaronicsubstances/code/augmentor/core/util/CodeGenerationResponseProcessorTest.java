package com.aaronicsubstances.code.augmentor.core.util;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CodeGenerationResponseProcessorTest {
    
    @Test(dataProvider = "createTestDetermineReplacementRangeData")
    public void testDetermineReplacementRange(GeneratedCode genCode, 
            CodeSnippetDescriptor snippetDescriptor, int[] expected) {
        int[] actual = CodeGenerationResponseProcessor.determineReplacementRange(
            snippetDescriptor, genCode);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestDetermineReplacementRangeData() {
        // use possible ways of combining four booleans:
        // - replaceAugCodeDirectives?
        // - replaceGenCodeDirectives?
        // - genCodeDescriptor exists?
        // - genCodeDescriptor is inline?
        // That gives 16 - 4 duplicates resulting from observation that if
        // genCodeDescriptor doesn't exist, inline status is irrelevant
        return new Object[][] {
            //0000
            { new GeneratedCode(1, false, null, false, 
                false, false, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 0, 12, null, 1, "\n"),
                null),
            new int[]{ 12, 12 }  },

            //0010
            { new GeneratedCode(1, false, null, false, 
                false, false, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 0, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, false)),
            new int[]{ 38, 40 }  },

            //0011
            { new GeneratedCode(1, false, null, false, 
                false, false, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 0, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, true)),
            new int[]{ 32, 50 }  },

            //0100
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 0, 12, null, 1, "\n"),
                null),
            new int[]{ 12, 12 }  },

            //0110
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 10, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, false)),
            new int[]{ 12, 50 }  },

            //0111
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 10, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, true)),
            new int[]{ 12, 50 }  },
            
            //1000
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 0, 12, null, 1, "\n"),
                null),
            new int[]{ 0, 12 }  },

            //1010
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 0, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, false)),
            new int[]{ 0, 12 }  },

            //1011
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 10, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, true)),
            new int[]{ 10, 12 }  },

            //1100
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 0, 12, null, 1, "\n"),
                null),
            new int[]{ 0, 12 }  },

            //1110
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 10, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, false)),
            new int[]{ 10, 50 }  },

            //1111
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 10, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, true)),
            new int[]{ 10, 50 }  }
        };
    }

    @Test(dataProvider = "createShouldWrapInGenCodeDirectivesData")
    public void testShouldWrapInGenCodeDirectives(GeneratedCode genCode,
            GeneratedCodeDescriptor generatedCodeDescriptor, boolean expected) {
        boolean actual = CodeGenerationResponseProcessor.shouldWrapInGenCodeDirectives(genCode, 
            generatedCodeDescriptor);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createShouldWrapInGenCodeDirectivesData() {
        // use possible ways of combining four booleans:
        // - replaceAugCodeDirectives?
        // - replaceGenCodeDirectives?
        // - genCodeDescriptor exists?
        // - genCodeDescriptor is inline?
        // That gives 16 - 4 duplicates resulting from observation that if
        // genCodeDescriptor doesn't exist, inline status is irrelevant
        return new Object[][] {
            //0000
            { new GeneratedCode(1, false, null, false, 
                false, false, null), 
            null,
            true  },

            //0010
            { new GeneratedCode(1, false, null, false, 
                false, false, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, false),
            false  },

            //0011
            { new GeneratedCode(1, false, null, false, 
                false, false, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, true),
            true  },

            //0100
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            null,
            false  },

            //0110
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, false),
            false  },

            //0111
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, true),
            false  },
            
            //1000
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            null,
            false  },

            //1010
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, false),
            false },

            //1011
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, true),
            false  },

            //1100
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            null,
            false  },

            //1110
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, false),
            false  },

            //1111
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, true),
            false  }
        };
    }

    @Test(dataProvider = "createEnsureEndingNewlineData")
    public void testEnsureEndingNewline(String str, String newline, String expected) {
        String actual = CodeGenerationResponseProcessor.ensureEndingNewline(str, newline);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createEnsureEndingNewlineData() {
        return new Object[][]{
            { "", "\n", "\n" },
            { "a", "\n", "a\n" },
            { "a", "\r\n", "a\r\n" },
            { "ab\n", "\n", "ab\n" },
            { "ab\n", "\r\n", "ab\n" },
            { "ab\r\n", "\n", "ab\r\n" },
            { "\r", "\r\n", "\r" },
        };
    }

    @Test(dataProvider = "createTestEffectiveIndentData")
    public void testGetEffectiveIndent(GeneratedCode genCode, 
            AugmentingCodeDescriptor augCodeDescriptor, String expected) {
        String actual = CodeGenerationResponseProcessor.getEffectiveIndent(augCodeDescriptor, genCode);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestEffectiveIndentData() {
        return new Object[][]{
            { 
                new GeneratedCode(0, true, " ", false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "", 0, null),
                "" 
            },
            { 
                new GeneratedCode(0, true, null, false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                "" 
            },
            { 
                new GeneratedCode(0, false, " ", false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "", 0, null),
                " " 
            },
            { 
                new GeneratedCode(0, false, "\t\t", false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                "\t\t" 
            },
            { 
                new GeneratedCode(0, false, "", false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, " ", 0, null),
                " " 
            },
            { 
                new GeneratedCode(0, false, null, false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "", 0, null),
                ""
            },
            { 
                new GeneratedCode(0, false, null, false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                "\t"
            }
        };
    }

    @Test(dataProvider = "createTestIndentCodeData")
    @SuppressWarnings("unchecked")
    public void testIndentCode(int inputPtr, TestArg c, String indent, TestArg expectedWrapper) {
        List<ContentPart> inputContentParts = (List<ContentPart>) c.value;
        String expected = (String) expectedWrapper.value;

        CodeGenerationResponseProcessor.indentCode(inputContentParts, indent);

        String actual = new GeneratedCode(inputContentParts).getWholeContent();
        
        assertEquals(actual, expected, craftErrorMessageInvolvingRandomContentParts(
            inputContentParts, expected, actual));
    }

    @DataProvider
    public Iterator<Object[]> createTestIndentCodeData() {
        List<String> inputs = new ArrayList<>();
        List<String> indents = new ArrayList<>();
        List<String> outputs = new ArrayList<>();

        // case 1
        String input = TestResourceLoader.loadResourceNewlinesNormalized("input-unix.txt", 
            getClass(), "\n");
        String output = TestResourceLoader.loadResourceNewlinesNormalized("output-unix.txt", 
            getClass(), "\n");
        inputs.add(input);
        indents.add("    ");
        outputs.add(output);

        // case 2
        input = TestResourceLoader.loadResourceNewlinesNormalized("input-win.txt", 
            getClass(), "\r\n");
        output = TestResourceLoader.loadResourceNewlinesNormalized("output-win.txt", 
            getClass(), "\r\n");
        inputs.add(input);
        indents.add("        ");
        outputs.add(output);

        // add other cases "inline"

        // case 3
        inputs.add("");
        indents.add("");
        outputs.add("");

        // case 4
        inputs.add("");
        indents.add("  ");
        outputs.add("");

        // case 5
        inputs.add("\n56");
        indents.add("  ");
        outputs.add("\n  56");

        // case 6.
        indents.add("\t");
        StringBuilder randInput = new StringBuilder();
        List<String> randChars = new ArrayList<>(Arrays.asList("\r", "\n"));
        for (int i = 0; i < 50; i++) {
            int j = TestResourceLoader.RAND_GEN.nextInt(randChars.size());
            randInput.append(randChars.get(j));
        }
        inputs.add(randInput.toString());
        outputs.add(inputs.get(inputs.size() - 1));

        // case 7.
        indents.add("");
        randInput = new StringBuilder();
        randChars.addAll(Arrays.asList("\t", " ", "\f", "x"));
        for (int i = 0; i < 50; i++) {
            int j = TestResourceLoader.RAND_GEN.nextInt(randChars.size());
            randInput.append(randChars.get(j));
        }
        inputs.add(randInput.toString());
        outputs.add(inputs.get(inputs.size() - 1));

        return new IndentCodeTestDataProvider(10, inputs, indents, outputs);
    }

    @Test(dataProvider = "createTestRepairSplitCrLfsData")
    public void testRepairSplitCrLfs(List<ContentPart> inputContentParts,
            List<ContentPart> expected) {
        CodeGenerationResponseProcessor.repairSplitCrLfs(inputContentParts);
        assertEquals(inputContentParts, expected);
    }

    @DataProvider
    public Object[][] createTestRepairSplitCrLfsData() {
        return new Object[][]{
            { Arrays.asList(), Arrays.asList() },
            { Arrays.asList(new ContentPart("", false)), Arrays.asList(new ContentPart("", false)) },
            { Arrays.asList(new ContentPart("aab\r", true)), 
              Arrays.asList(new ContentPart("aab\r", true)) },
            { Arrays.asList(new ContentPart("aab\r", true), new ContentPart("\ncd", false)), 
              Arrays.asList(new ContentPart("aab\r\n", true), new ContentPart("cd", false)) },
            { Arrays.asList(new ContentPart("aab\r", true), new ContentPart("\ncd\r", false),
                new ContentPart("\nefgh", true), new ContentPart("end", false)), 
              Arrays.asList(new ContentPart("aab\r\n", true), new ContentPart("cd\r\n", false),
                new ContentPart("efgh", true), new ContentPart("end", false)) },
            { Arrays.asList(new ContentPart("aab\r", true), new ContentPart("cd\n", false),
                new ContentPart("\nefgh\r\n", true), new ContentPart("\rend", false)), 
              Arrays.asList(new ContentPart("aab\r", true), new ContentPart("cd\n", false),
                new ContentPart("\nefgh\r\n", true), new ContentPart("\rend", false)) }
        };
    }

    @Test(dataProvider = "createTestDoesPartBeginOnNewlineData")
    public void testDoesPartBeginOnNewline(List<ContentPart> contentParts, int i, boolean expected) {
        boolean actual = CodeGenerationResponseProcessor.doesPartBeginOnNewline(contentParts, i);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestDoesPartBeginOnNewlineData() {
        return new Object[][]{
            { Arrays.asList(new ContentPart("", false)), 0, false },
            { Arrays.asList(new ContentPart("abcd", true)), 0, true },
            { Arrays.asList(new ContentPart("", false),
                new ContentPart("", true)), 1, false },
            { Arrays.asList(new ContentPart("abcd", true),
                new ContentPart("abcd", false)), 1, false },
            { Arrays.asList(new ContentPart("abcd", true), 
                new ContentPart("defd", false)), 1, false },
            { Arrays.asList(new ContentPart("abcd\n", true), 
                new ContentPart("defd", false)), 0, true },
            { Arrays.asList(new ContentPart("abcd\r", true), 
                new ContentPart("defd", false)), 0, true },
            { Arrays.asList(new ContentPart("", true), new ContentPart("abcd", true), 
                new ContentPart("defd", false)), 1, true },
            { Arrays.asList(new ContentPart("abcd\r", true), 
                new ContentPart("defd", false)), 1, true },
            { Arrays.asList(new ContentPart("abcd\n", true), 
                new ContentPart("defd", false)), 1, true },
            { Arrays.asList(new ContentPart("abcd\r\n", true), new ContentPart("", false),
                new ContentPart("defd", false)), 1, false },
            { Arrays.asList(new ContentPart("abcd\r\n", true), new ContentPart("", false),
                new ContentPart("defd", false)), 2, true },
            { Arrays.asList(new ContentPart("abcd", true), 
                new ContentPart("defd\r\n", false)), 1, false },
            { Arrays.asList(new ContentPart("abcd", true), new ContentPart("", true),
                new ContentPart("defd\r\n", false)), 2, false }
        };
    }

    @Test(dataProvider = "createTestBuildSimilarityRegex")
    @SuppressWarnings("unchecked")
    public void testBuildSimilarityRegex(String path, TestArg expectedWrapper) {
        List<ContentPart> inputContentParts = TestResourceLoader.loadContentParts(path,
            getClass());
        List<Object> expected = (List<Object>) expectedWrapper.value;
        TestResourceLoader.printTestHeader("testBuildSimilarityRegex", path, expectedWrapper);
        List<Object> actual = CodeGenerationResponseProcessor.buildSimilarityRegex(
            inputContentParts, true);
        assertEquals(actual, expected, 
            craftErrorMessageInvolvingRandomContentParts(inputContentParts, expected, actual));
    }

    @DataProvider
    public Object[][] createTestBuildSimilarityRegex() {
        int anySpace = CodeGenerationResponseProcessor.MATCH_TYPE_ANY_SPACES;
        int reqdSpace = CodeGenerationResponseProcessor.MATCH_TYPE_REQUIRE_SPACE;
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
            { "similarity-test-data-00.json", new TestArg(expected0) },
            { "similarity-test-data-01.json", new TestArg(expected1) },
            { "similarity-test-data-02.json", new TestArg(expected2) },
            { "similarity-test-data-03.json", new TestArg(expected3) },
            { "similarity-test-data-04.json", new TestArg(expected4) },
            { "similarity-test-data-05.json", new TestArg(expected5) }
        };
    }

    @Test(dataProvider = "createTestAreExactTextsSimilarData")
    @SuppressWarnings("unchecked")
    public void testAreExactTextsSimilar(int inputPtr, TestArg inputWrapper, TestArg c) {
        String input = (String) inputWrapper.value;
        List<ContentPart> inputContentParts = (List<ContentPart>) c.value;

        //TestResourceLoader.printTestHeader("testAreExactTextsSimilar", inputPtr, inputWrapper, c);
        
        Map<String, Object> actual = CodeGenerationResponseProcessor.runSimilarityTest(input,
            inputContentParts, false);
        Map<String, Object> actualDescription = new HashMap<>();
        if (actual != null) {
            actualDescription.putAll(actual);
        }
        actualDescription.put("errorInput", input);
        assertNull(actual, craftErrorMessageInvolvingRandomContentParts(
            inputContentParts, null, actualDescription));
    }

    @DataProvider
    public Iterator<Object[]> createTestAreExactTextsSimilarData() {
        List<String> inputs = new ArrayList<>();

        // case 1-2
        /*String input = TestResourceLoader.loadResourceNewlinesNormalized("input-unix.txt", 
            getClass(), "\n");
        String output = TestResourceLoader.loadResourceNewlinesNormalized("output-unix.txt", 
            getClass(), "\n");
        inputs.add(input);
        inputs.add(output);

        // case 2-4
        input = TestResourceLoader.loadResourceNewlinesNormalized("input-win.txt", 
            getClass(), "\r\n");
        output = TestResourceLoader.loadResourceNewlinesNormalized("output-win.txt", 
            getClass(), "\r\n");
        inputs.add(input);
        inputs.add(output);*/

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

        return new CodeSimilarityTestDataProvider(0, inputs);
    }

    @Test(dataProvider = "createTestAreTextsSimilarData")
    @SuppressWarnings("unchecked")
    public void testAreTextsSimilar(TestArg textArg, TestArg c, boolean expected) {
        String text = (String) textArg.value;
        List<ContentPart> contentParts = (List<ContentPart>) c.value;
        Map<String, Object> actual = CodeGenerationResponseProcessor.runSimilarityTest(
            text, contentParts, false);
        if (expected == true) {
            assertNull(actual, craftErrorMessageInvolvingRandomContentParts(
                contentParts, null, actual));
        }
        else {
            assertNotNull(actual);
        }
    }

    @DataProvider
    public Object[][] createTestAreTextsSimilarData() {
        String inputWin = TestResourceLoader.loadResourceNewlinesNormalized(
            "input-win.txt", getClass(), "\r\n");
        TestArg testWinInput = new TestArg(inputWin);

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
            { testWinInput, new TestArg(contentParts0), true },
            //{ testWinInput, new TestArg(contentParts1), true }
        };
    }

    private static String craftErrorMessageInvolvingRandomContentParts(List<ContentPart> inputContentParts,
            Object expected, Object actual) {        
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("After split yielded:\n");
        errorMessage.append(TestResourceLoader.GSON_INST.toJson(inputContentParts));
        errorMessage.append("\n\nExpectation:\n");
        errorMessage.append(TestResourceLoader.GSON_INST.toJson(expected));
        errorMessage.append("\n\nActual:\n");
        errorMessage.append(TestResourceLoader.GSON_INST.toJson(actual));
        errorMessage.append("\n\n");
        return errorMessage.toString();
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

    public static class TestArg {
        final Object value;

        public TestArg(Object value) {
            this.value = value;
        }

        @Override
        public String toString() {
            // generate a shorter name than Object.toString()
            return String.format("%s@%x",
                getClass().getSimpleName(), hashCode());
        }
    }
}
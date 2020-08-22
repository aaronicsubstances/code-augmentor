package com.aaronicsubstances.code.augmentor.core.util;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestArg;
import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.craftErrorMessageInvolvingRandomContentParts;

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
                new GeneratedCodeDescriptor(32, 38, 40, 50, "", false)),
            new int[]{ 38, 40 }  },

            //0011
            { new GeneratedCode(1, false, null, false, 
                false, false, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 0, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, "", true)),
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
                new GeneratedCodeDescriptor(32, 38, 40, 50, "", false)),
            new int[]{ 12, 50 }  },

            //0111
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 10, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, "", true)),
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
                new GeneratedCodeDescriptor(32, 38, 40, 50, "", false)),
            new int[]{ 0, 12 }  },

            //1011
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 10, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, "", true)),
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
                new GeneratedCodeDescriptor(32, 38, 40, 50, "", false)),
            new int[]{ 10, 50 }  },

            //1111
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            new CodeSnippetDescriptor(
                new AugmentingCodeDescriptor(1, 10, 12, null, 1, "\n"),
                new GeneratedCodeDescriptor(32, 38, 40, 50, "", true)),
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
            new GeneratedCodeDescriptor(32, 38, 40, 50, "", false),
            false  },

            //0011
            { new GeneratedCode(1, false, null, false, 
                false, false, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, "", true),
            true  },

            //0100
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            null,
            false  },

            //0110
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, "", false),
            false  },

            //0111
            { new GeneratedCode(1, false, null, false, 
                false, true, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, "", true),
            false  },
            
            //1000
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            null,
            false  },

            //1010
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, "", false),
            false },

            //1011
            { new GeneratedCode(1, false, null, false, 
                true, false, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, "", true),
            false  },

            //1100
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            null,
            false  },

            //1110
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, "", false),
            false  },

            //1111
            { new GeneratedCode(1, false, null, false, 
                true, true, null), 
            new GeneratedCodeDescriptor(32, 38, 40, 50, "", true),
            false  }
        };
    }

    @Test(dataProvider = "createTestGetShouldEnsureEndingNewlineData")
    public void testGetShouldEnsureEndingNewline(GeneratedCode genCode, boolean expected) {
        boolean actual = CodeGenerationResponseProcessor.getShouldEnsureEndingNewline(genCode);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestGetShouldEnsureEndingNewlineData() {
        return new Object[][]{
            { new GeneratedCode(0, true, null, true, false, false, null), true },
            { new GeneratedCode(0, false, null, true, false, false, null), true },
            { new GeneratedCode(0, true, null, true, true, false, null), false },
            { new GeneratedCode(0, false, null, true, false, true, null), true },
            { new GeneratedCode(0, true, null, true, true, true, null), false },
            { new GeneratedCode(0, false, null, true, true, true, null), true }
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
            AugmentingCodeDescriptor augCodeDescriptor,
            GeneratedCodeDescriptor genCodeDescriptor,
            String expected) {
        String actual = CodeGenerationResponseProcessor.getEffectiveIndent(
            new CodeSnippetDescriptor(augCodeDescriptor, genCodeDescriptor), genCode);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestEffectiveIndentData() {
        return new Object[][]{
            { 
                new GeneratedCode(0, true, " ", false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "", 0, null),
                null,
                " " 
            },
            { 
                new GeneratedCode(0, true, null, false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                null,
                "\t" 
            },
            { 
                new GeneratedCode(0, true, null, true, false, true, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                null,
                "" 
            },
            { 
                new GeneratedCode(0, false, " ", false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "", 0, null),
                null,
                " " 
            },
            { 
                new GeneratedCode(0, false, "\t\t", false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                null,
                "\t\t" 
            },
            { 
                new GeneratedCode(0, false, "", false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, " ", 0, null),
                null,
                "" 
            },
            { 
                new GeneratedCode(0, false, null, false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "", 0, null),
                null,
                ""
            },
            { 
                new GeneratedCode(0, false, null, false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                null,
                "\t"
            },
            { 
                new GeneratedCode(0, false, null, false, true, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                new GeneratedCodeDescriptor(),
                ""
            },
            { 
                new GeneratedCode(0, false, null, false, true, true, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                new GeneratedCodeDescriptor(),
                ""
            },
            { 
                new GeneratedCode(0, false, " ", false, true, true, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                null,
                " "
            },
            { 
                new GeneratedCode(0, false, null, false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, "\t", 0, null),
                new GeneratedCodeDescriptor(0, 0, 0, 0, "  ", false),
                "  "
            },
            { 
                new GeneratedCode(0, false, null, false, false, false, null),
                new AugmentingCodeDescriptor(0, 0, 0, " ", 0, null),
                new GeneratedCodeDescriptor(0, 0, 0, 0, "  ", true),
                "  "
            }
        };
    }

    @Test(dataProvider = "createTestIndentCodeData")
    public void testIndentCode(int inputPtr, TestArg<List<ContentPart>> c, 
            String indent, TestArg<String> expectedWrapper) {
        List<ContentPart> inputContentParts = c.value;
        String expected = expectedWrapper.value;

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
}
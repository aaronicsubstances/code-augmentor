package com.aaronicsubstances.code.augmentor.core.util;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
    public void testIndentCode(int inputPtr, TestArg c, String indent, 
            TestArg e) {
        List<ContentPart> inputContentParts = (List<ContentPart>) c.value;
        String expected = (String) e.value;

        CodeGenerationResponseProcessor.indentCode(inputContentParts, indent);

        String actual = new GeneratedCode(inputContentParts).getWholeContent();
        
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("After split yielded:\n");
        errorMessage.append(PersistenceUtil.serializeFormattedToJson(inputContentParts));
        errorMessage.append("\n\nExpectation:\n");
        errorMessage.append(PersistenceUtil.serializeCompactlyToJson(expected));
        errorMessage.append("\n\nActual:\n");
        errorMessage.append(PersistenceUtil.serializeCompactlyToJson(actual));
        errorMessage.append("\n\n");

        assertEquals(actual, expected, errorMessage.toString());
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
        Random randGen = new Random();
        List<String> randChars = new ArrayList<>(Arrays.asList("\r", "\n"));
        for (int i = 0; i < 50; i++) {
            int j = randGen.nextInt(randChars.size());
            randInput.append(randChars.get(j));
        }
        inputs.add(randInput.toString());
        outputs.add(inputs.get(inputs.size() - 1));

        // case 7.
        indents.add("");
        randInput = new StringBuilder();
        randChars.addAll(Arrays.asList("\t", " ", "\f", "x"));
        for (int i = 0; i < 50; i++) {
            int j = randGen.nextInt(randChars.size());
            randInput.append(randChars.get(j));
        }
        inputs.add(randInput.toString());
        outputs.add(inputs.get(inputs.size() - 1));

        return new IndentCodeDataProvider(10, inputs, indents, outputs);
    }

    @Test
    public void testIndentCodeWithSplitCrLf() {
        String indent = "\t";
        List<ContentPart> inputContentParts = Arrays.asList(
            new ContentPart("amos\r", false), new ContentPart("\nis real.", true)
        );
        String expected = "\tamos\r\n\tis real.";
        
        CodeGenerationResponseProcessor.indentCode(inputContentParts, indent);

        String actual = new GeneratedCode(inputContentParts).getWholeContent();
        
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("After split yielded:\n");
        errorMessage.append(PersistenceUtil.serializeFormattedToJson(inputContentParts));
        errorMessage.append("\n\nExpectation:\n");
        errorMessage.append(PersistenceUtil.serializeCompactlyToJson(expected));
        errorMessage.append("\n\nActual:\n");
        errorMessage.append(PersistenceUtil.serializeCompactlyToJson(actual));
        errorMessage.append("\n\n");

        assertEquals(actual, expected, errorMessage.toString());
    }

    private static List<ContentPart> buildContentParts(String input, List<int[]> ranges) {
        List<ContentPart> contentParts = new ArrayList<>();
        for (int[] range : ranges) {
            int startIdx = range[0], endIdx = range[1];
            boolean exactMatch = false;
            if (range.length > 2) {
                exactMatch = range[2] != 0;
            }
            String s = input.substring(startIdx, endIdx);
            contentParts.add(new ContentPart(s, exactMatch));
        }
        return contentParts;
    }

    public static class TestArg {
        final Object value;
        public TestArg(Object value) {
            this.value = value;
        }
    }
}
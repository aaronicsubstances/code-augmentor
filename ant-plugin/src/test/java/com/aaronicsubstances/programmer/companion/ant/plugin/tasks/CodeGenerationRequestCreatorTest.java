package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aaronicsubstances.programmer.companion.ParserException;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode.Block;
import com.aaronicsubstances.programmer.companion.ant.plugin.tasks.CodeGenerationRequestCreator.SuffixDescriptor;
import com.aaronicsubstances.programmer.companion.java.JavaLexer;

public class CodeGenerationRequestCreatorTest {

    @Test(dataProvider = "createTestGetCommentContentWithoutSuffixData")
    public void testGetCommentContentWithoutSuffix(Token t, String suffix, String expected) {
        String actual = CodeGenerationRequestCreator.getCommentContentWithoutSuffix(t, suffix);
        assertEquals(actual, expected);
    }
    
    @DataProvider
    public Object[][] createTestGetCommentContentWithoutSuffixData() {
        return new Object[][]{
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//a", 0, 0, 0, 0), 
                "", "a" },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*b*/", 0, 0, 0, 0), 
                "", "b" },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//a", 0, 0, 0, 0), 
                "a", "" },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*a*/", 0, 0, 0, 0), 
                "a", "" },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//a: print('yes')", 0, 0, 0, 0), 
                "a:", " print('yes')" },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//a: print('yes')", 0, 0, 0, 0), 
                "a: ", "print('yes')" },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*/\ncreateClassSnippet('TEST')\n*/", 0, 0, 0, 0), 
                "/", "\ncreateClassSnippet('TEST')\n" },
        };
    }
    
    @Test(dataProvider = "createTestGetCommentContentWithoutSuffixErrorData", 
        expectedExceptions = Throwable.class)
    public void testGetCommentContentWithoutSuffixForErrors(Token t, String suffix) {
        CodeGenerationRequestCreator.getCommentContentWithoutSuffix(t, suffix);
    }
    
    @DataProvider
    public Object[][] createTestGetCommentContentWithoutSuffixErrorData() {
        return new Object[][]{
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_NON_NEWLINE_WHITESPACE, "//a", 0, 0, 0, 0), 
                "a" },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_NEWLINE, "/*a*/", 0, 0, 0, 0), 
                "a" }
        };
    }

    @Test(dataProvider = "createTestGroupDoubleSlashReleventTokensData")
    public void testGroupDoubleSlashReleventTokens(List<Token> tokens, List<List<Token>> expected) {
        List<List<Token>> actual = CodeGenerationRequestCreator.groupDoubleSlashReleventTokens(tokens);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestGroupDoubleSlashReleventTokensData() {
        List<Token> secondTokenList = Arrays.asList(newTokenWithLnNum(1), newTokenWithLnNum(3),
            newTokenWithLnNum(5), newTokenWithLnNum(7));
        List<Token> thirdTokenList = Arrays.asList(newTokenWithLnNum(10), newTokenWithLnNum(11),
            newTokenWithLnNum(12), newTokenWithLnNum(13));

        List<Token> fourthTokenList = Arrays.asList(newTokenWithLnNum(10), newTokenWithLnNum(11),
            newTokenWithLnNum(13), newTokenWithLnNum(15), newTokenWithLnNum(16), 
            newTokenWithLnNum(17));
        List<List<Token>> fourthGroup = Arrays.asList(
            Arrays.asList(newTokenWithLnNum(10), newTokenWithLnNum(11)), 
            Arrays.asList(newTokenWithLnNum(13)), 
            Arrays.asList(newTokenWithLnNum(15), newTokenWithLnNum(16), 
                newTokenWithLnNum(17)));

        List<Token> fifthTokenList = Arrays.asList(newTokenWithLnNum(10), newTokenWithLnNum(12),
            newTokenWithLnNum(13), newTokenWithLnNum(15), newTokenWithLnNum(16), 
            newTokenWithLnNum(20));
        List<List<Token>> fifthGroup = Arrays.asList(
            Arrays.asList(newTokenWithLnNum(10)),
            Arrays.asList(newTokenWithLnNum(12), newTokenWithLnNum(13)), 
            Arrays.asList(newTokenWithLnNum(15), newTokenWithLnNum(16)),            
            Arrays.asList(newTokenWithLnNum(20)));

        List<Token> sixthList = Arrays.asList(newTokenWithLnNum(10));

        return new Object[][] {
            new Object[]{ Arrays.asList(), Arrays.asList() },
            new Object[]{ secondTokenList, 
                secondTokenList.stream()
                    .map(t -> Arrays.asList(t))
                    .collect(Collectors.toList()), },
            new Object[]{ thirdTokenList, Arrays.asList(thirdTokenList) },
            new Object[]{ fourthTokenList, fourthGroup },
            new Object[]{ fifthTokenList, fifthGroup },
            new Object[]{ sixthList, Arrays.asList(sixthList) }
        };
    }

    private static Token newTokenWithLnNum(int lineNumber) {
        return new Token(0, null, 0, 0, lineNumber, 0);
    }

    private static Token newTokenWithStartPos(int startPos) {
        return new Token(0, null, startPos, 0, 0, 0);
    }

    @Test(dataProvider = "createTestCombineAndSortRelevantTokensData")
    public void testCombineAndSortRelevantTokens(List<Token> tokens, List<List<Token>> groups,
            List<Object> expected) {
        List<Object> actual = CodeGenerationRequestCreator.combineAndSortRelevantTokens(tokens, groups);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestCombineAndSortRelevantTokensData() {
        List<Token> tokens = Arrays.asList(newTokenWithStartPos(7), newTokenWithStartPos(20),
            newTokenWithStartPos(99));
        List<List<Token>> groups = Arrays.asList(
            Arrays.asList(newTokenWithStartPos(3), newTokenWithStartPos(4),
                newTokenWithStartPos(6)),                
            Arrays.asList(newTokenWithStartPos(30), newTokenWithStartPos(40),
                newTokenWithStartPos(52)));
        List<Object> fourthExpected = Arrays.asList(
            Arrays.asList(newTokenWithStartPos(3), newTokenWithStartPos(4),
                newTokenWithStartPos(6)),
            newTokenWithStartPos(7),
            newTokenWithStartPos(20),
            Arrays.asList(newTokenWithStartPos(30), newTokenWithStartPos(40),
                newTokenWithStartPos(52)),
            newTokenWithStartPos(99));
        return new Object[][]{
            new Object[]{ Arrays.asList(), Arrays.asList(), Arrays.asList() },
            new Object[]{ tokens, Arrays.asList(), tokens },
            new Object[]{ Arrays.asList(), groups, groups },
            new Object[]{ tokens, groups, fourthExpected }
        };
    }

    @Test(dataProvider = "createTestGetSuffixDescriptorData")
    public void testGetSuffixDescriptor(Token t, SuffixDescriptor expected) {
        List<String> headerDoubleSlashSuffixes = Arrays.asList("H", "I");
        List<String> genCodeStartSuffixes = Arrays.asList("GS");
        List<String> genCodeEndSuffixes = Arrays.asList("GE");
        List<String> embeddedStringDoubleSlashSuffixes = Arrays.asList("ES");
        List<CodeGenerationRequestSpecification> requestSpecList = Arrays.asList(
            new CodeGenerationRequestSpecification(Arrays.asList("PY", "PY30")),
            new CodeGenerationRequestSpecification(Arrays.asList("JS"))
        );
        CodeGenerationRequestCreator instance = new CodeGenerationRequestCreator(
            headerDoubleSlashSuffixes, genCodeStartSuffixes, genCodeEndSuffixes,
            embeddedStringDoubleSlashSuffixes, requestSpecList);
        SuffixDescriptor actual = instance.getSuffixDescriptor(t);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestGetSuffixDescriptorData() {
        return new Object[][]{
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//ES3", 0, 0, 0, 0), 
                new SuffixDescriptor("ES", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_EMB_STRING, -1) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*GS5+*/", 0, 0, 0, 0),
                new SuffixDescriptor("GS", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_GEN_CODE_START, -1) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//GE", 0, 0, 0, 0),
                new SuffixDescriptor("GE", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_GEN_CODE_END, -1) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*H***/", 0, 0, 0, 0), null },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//H/", 0, 0, 0, 0),
                new SuffixDescriptor("H", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_HEADER, -1) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//I/", 0, 0, 0, 0),
                new SuffixDescriptor("I", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_HEADER, -1) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//PY30#", 0, 0, 0, 0),
                new SuffixDescriptor("PY30", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 0) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*PY3#*/", 0, 0, 0, 0),
                new SuffixDescriptor("PY", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 0) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//PY30#", 0, 0, 0, 0),
                new SuffixDescriptor("PY30", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 0) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*JS6*/", 0, 0, 0, 0),
                new SuffixDescriptor("JS", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 1) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_NEWLINE, "\n", 0, 0, 0, 0), null }
        };
    }

    @Test(dataProvider = "createTestValidateDoubleSlashRelevantTokenGroupData")
    public void testValidateDoubleSlashRelevantTokenGroup(int index, 
            List<Token> tokenGroup, Integer expected) {
        // create input which spans max possible line and column numbers.
        final int maxLineNumber = 40, maxColumnNumber = 10;
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < maxLineNumber; i++) {
            for (int j = 0; j < maxColumnNumber; j++) {
                if (j % 10 == 9) {
                    input.append(' ');
                }
                input.append('x');
            }
            input.append('\n');
        }
        ParserInputSource inputSource = new ParserInputSource(input.toString());
        ParserException actual = CodeGenerationRequestCreator
            .validateDoubleSlashRelevantTokenGroup(tokenGroup, inputSource);
        if (expected == null) {
            assertNull(actual);
        }
        else {
            assertNotNull(actual);
            assertEquals(actual.getLineNumber(), (int)expected, "Line numbers differ");
            System.out.println("testValidateDoubleSlashRelevantTokenGroup[" + index +
                "].exceptionMessage = " + actual.getMessage());
        }
    }

    @DataProvider
    public Object[][] createTestValidateDoubleSlashRelevantTokenGroupData() {
        List<Token> firstGroup = Arrays.asList(createTokenWithValue(2, "ES"));       
        List<Token> secondGroup = Arrays.asList(createTokenWithValue(2, "1"));       
        List<Token> thirdGroup = Arrays.asList(createTokenWithValue(2, "1"),
            createTokenWithValue(3, "ES"));
        List<Token> fourthGroup = Arrays.asList(createTokenWithValue(2, "1"),
            createTokenWithValue(3, "ES"), createTokenWithValue(4, "10"));
        List<Token> fifthGroup = Arrays.asList(createTokenWithValue(2, "H"),
            createTokenWithValue(3, "H"), createTokenWithValue(4, "H"));
        List<Token> sixthGroup = Arrays.asList(createTokenWithValue(2, "H"));
        List<Token> seventhGroup = Arrays.asList(createTokenWithValue(12, "H"),
            createTokenWithValue(13, "H"), createTokenWithValue(14, "0"));
        List<Token> eighthGroup = Arrays.asList(createTokenWithValue(12, "H"),
            createTokenWithValue(13, "ES"));
        return new Object[][]{
            new Object[]{ 0, firstGroup, 2 },
            new Object[]{ 1, secondGroup, null },
            new Object[]{ 2, thirdGroup, null },
            new Object[]{ 3, fourthGroup, 4},
            new Object[]{ 4, fifthGroup, null },
            new Object[]{ 5, sixthGroup, null },
            new Object[]{ 6, seventhGroup, 14 },
            new Object[]{ 7, eighthGroup, 13 }
        };
    }

    private static SuffixDescriptor createSuffixDescriptor(String suffixDescStr) {
        int suffixType, augCodeSpecIndex = -1;
        if (suffixDescStr.equals("GE")) {
            suffixType = CodeGenerationRequestCreator.SUFFIX_TYPE_GEN_CODE_END;
        }
        else if (suffixDescStr.equals("GS")) {
            suffixType = CodeGenerationRequestCreator.SUFFIX_TYPE_GEN_CODE_START;
        }
        else if (suffixDescStr.equals("H")) {
            suffixType = CodeGenerationRequestCreator.SUFFIX_TYPE_HEADER;
        }
        else if (suffixDescStr.equals("ES")) {
            suffixType = CodeGenerationRequestCreator.SUFFIX_TYPE_EMB_STRING;
        }
        else if (suffixDescStr.equals("JS")) {
            suffixType = CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE;
            augCodeSpecIndex = 0;
        }
        else {
            suffixType = CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE;
            augCodeSpecIndex = Integer.parseInt(suffixDescStr);
        }
        SuffixDescriptor suffixDescriptor = new SuffixDescriptor(
            augCodeSpecIndex != -1 ? "JS" : suffixDescStr, suffixType, augCodeSpecIndex);
        return suffixDescriptor;
    }

    private static Token createTokenWithValue(int lineNumber, String suffixDescStr) {        
        SuffixDescriptor suffixDescriptor = createSuffixDescriptor(suffixDescStr);
        Map<String, Object> tokenAttributes = new HashMap<>();
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR,
            suffixDescriptor);
        int colNumber = (int)((Math.random() * 5)) + 1;
        Token t = new Token(0, null, 0, 0, lineNumber, colNumber);
        t.value = tokenAttributes;
        return t;
    }

    @Test(dataProvider = "createTestCreateDoubleSlashAugCodeData")
    public void testCreateDoubleSlashAugCode(List<Token> tokenGroup, String expectedIndent,
            AugmentingCode expected) {
        StringBuilder actualIndent = new StringBuilder();
        AugmentingCode actual = CodeGenerationRequestCreator.createDoubleSlashAugCode(tokenGroup, actualIndent);
        assertEquals(actual, expected);
        assertEquals(actualIndent.toString(), expectedIndent, "indents differ");
    }

    @DataProvider
    public Object[][] createTestCreateDoubleSlashAugCodeData() {
        List<Token> firstGroup = Arrays.asList(
            createTokenWithValue(5, "JS", "  ", "//JS println(")
        );
        List<Token> secondGroup = Arrays.asList(
            createTokenWithValue(5, "JS", "  ", "//JS println"),
            createTokenWithValue(6, "JS", "  ", "//JS("),
            createTokenWithValue(7, "ES", "", "//ES{'value': true}"),
            createTokenWithValue(8, "JS", "  ", "//JS)")
        );
        List<Token> thirdGroup = Arrays.asList(
            createTokenWithValue(5, "JS", "  ", "//JS println("),
            createTokenWithValue(6, "ES", "  ", "//ES{"),
            createTokenWithValue(7, "ES", "  ", "//ES'value': true}"),
            createTokenWithValue(8, "JS", "  ", "//JS)")
        );
        List<Token> fourthGroup = Arrays.asList(
            createTokenWithValue(5, "JS", "  ", "//JS println"),
            createTokenWithValue(6, "JS", "  ", "//JS("),
            createTokenWithValue(7, "ES", "  ", "//ES{'value': true"),
            createTokenWithValue(8, "ES", " ", "//ES}")
        );
        List<Token> fifthGroup = Arrays.asList(
            createTokenWithValue(5, "JS", "  ", "//JS println"),
            createTokenWithValue(6, "JS", "  ", "//JS("),
            createTokenWithValue(7, "ES", "  ", "//ES{"),
            createTokenWithValue(8, "ES", "  ", "//ES'value': true"),
            createTokenWithValue(9, "ES", " ", "//ES}"),
            createTokenWithValue(10, "JS", " ", "//JS);"),
            createTokenWithValue(11, "JS", "", "//JS println()")
        );
        return new Object[][]{
            new Object[]{ firstGroup, "  ", createAugCode("JS", new Block(" println(", false)) },
            new Object[]{ secondGroup, "", createAugCode("JS", new Block(" println\n(\n", false),
                new Block("{'value': true}", true), new Block("\n)", false)), },
            new Object[]{ thirdGroup, "  ", createAugCode("JS", new Block(" println(\n", false),
                new Block("{\n'value': true}", true), new Block("\n)", false))
            },
            new Object[]{ fourthGroup, " ", createAugCode("JS", new Block(" println\n(\n", false),
                new Block("{'value': true\n}", true)) },
            
            new Object[]{ fifthGroup, "", createAugCode("JS", new Block(" println\n(\n", false),
                new Block("{\n'value': true\n}", true), new Block("\n);\n println()", false)), },
        };
    }

    private static AugmentingCode createAugCode(String suffix, Block... blocks) {        
        AugmentingCode augCode = new AugmentingCode(Arrays.asList(blocks));
        augCode.setCommentSuffix(suffix);
        return augCode;
    }

    private static Token createTokenWithValue(int lineNumber, String suffixDescStr, 
            String indent, String comment) {
        Map<String, Object> tokenAttributes = new HashMap<>();
        SuffixDescriptor suffixDescriptor = createSuffixDescriptor(suffixDescStr);
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR,
            suffixDescriptor);
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_INDENT,
            indent);
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_FF_NEWLINE,
            new Token(JavaLexer.TOKEN_TYPE_NEWLINE, "\n", 0, 0, lineNumber, 1));
        Token t = new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, comment, 0, 0, 
            lineNumber, 1);
        t.value = tokenAttributes;
        return t;
    }
}
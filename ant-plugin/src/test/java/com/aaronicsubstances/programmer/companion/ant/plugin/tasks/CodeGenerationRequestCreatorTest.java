package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aaronicsubstances.programmer.companion.ParserException;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.ant.plugin.TestResourceLoader;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode.Block;
import com.aaronicsubstances.programmer.companion.ant.plugin.tasks.CodeGenerationRequestCreator.SuffixDescriptor;
import com.aaronicsubstances.programmer.companion.java.JavaLexer;
import com.google.gson.Gson;

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
        CodeGenerationRequestCreator instance = createInstance();
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
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*JS6*/", 0, 0, 0, 0),
                new SuffixDescriptor("JS", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 0) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//PY30#", 0, 0, 0, 0),
                new SuffixDescriptor("PY30", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 1) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT, "/*PY3#*/", 0, 0, 0, 0),
                new SuffixDescriptor("PY", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 1) },
            new Object[]{ new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, "//PY30#", 0, 0, 0, 0),
                new SuffixDescriptor("PY30", 
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
        List<Token> firstGroup = Arrays.asList(newToken(2, "ES"));       
        List<Token> secondGroup = Arrays.asList(newToken(2, "1"));       
        List<Token> thirdGroup = Arrays.asList(newToken(2, "1"),
            newToken(3, "ES"));
        List<Token> fourthGroup = Arrays.asList(newToken(2, "1"),
            newToken(3, "ES"), newToken(4, "10"));
        List<Token> fifthGroup = Arrays.asList(newToken(2, "H"),
            newToken(3, "H"), newToken(4, "H"));
        List<Token> sixthGroup = Arrays.asList(newToken(2, "H"));
        List<Token> seventhGroup = Arrays.asList(newToken(12, "H"),
            newToken(13, "H"), newToken(14, "0"));
        List<Token> eighthGroup = Arrays.asList(newToken(12, "H"),
            newToken(13, "ES"));
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
            newToken(5, "JS", "  ", "//JS println(")
        );
        List<Token> secondGroup = Arrays.asList(
            newToken(5, "JS", "  ", "//JS println"),
            newToken(6, "JS", "  ", "//JS("),
            newToken(7, "ES", "", "//ES{'value': true}"),
            newToken(8, "JS", "  ", "//JS)")
        );
        List<Token> thirdGroup = Arrays.asList(
            newToken(5, "JS", "  ", "//JS println("),
            newToken(6, "ES", "  ", "//ES{"),
            newToken(7, "ES", "  ", "//ES'value': true}"),
            newToken(8, "JS", "  ", "//JS)")
        );
        List<Token> fourthGroup = Arrays.asList(
            newToken(5, "JS", "  ", "//JS println"),
            newToken(6, "JS", "  ", "//JS("),
            newToken(7, "ES", "  ", "//ES{'value': true"),
            newToken(8, "ES", " ", "//ES}")
        );
        List<Token> fifthGroup = Arrays.asList(
            newToken(5, "JS", "  ", "//JS println"),
            newToken(6, "JS", "  ", "//JS("),
            newToken(7, "ES", "  ", "//ES{"),
            newToken(8, "ES", "  ", "//ES'value': true"),
            newToken(9, "ES", " ", "//ES}"),
            newToken(10, "JS", " ", "//JS);"),
            newToken(11, "JS", "", "//JS println()")
        );
        List<Token> sixthGroup = Arrays.asList(
            newToken(5, "H", "    ", "//H"),
            newToken(6, "H", "    ", "//Here!"),
            newToken(7, "H", "    ", "//Here!!")
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
            new Object[]{ sixthGroup, "    ", createAugCode("H", new Block("\nere!\nere!!", false)) }
        };
    }

    @Test
    public void testGetNormalizedImportStatements() {
        List<String> expected = Arrays.asList("import org.springframework.boot.SpringApplication",
            "import org.springframework.boot.autoconfigure.SpringBootApplication");
        String s = TestResourceLoader.loadResource("tokens-for-import.json", getClass());
        List<Token> sourceTokens = fetchTokens(s);
        List<String> actual = CodeGenerationRequestCreator.getNormalizedImportStatements(sourceTokens);
        assertEquals(actual, expected);
    }

    @Test
    public void testGetSlashStarRelevantTokens() {
        List<Token> expected = Arrays.asList(
            newToken(5, "/*JS println(\"Hello World from JS-star\")\r\n" +
                "var i = 3 + new Date(); \r\n" +
                "...etc*/", 59, "0", 10, null, null)
        );
        String s = TestResourceLoader.loadResource("tokens-for-relevance.json", getClass());
        List<Token> sourceTokens = fetchTokens(s);
        CodeGenerationRequestCreator instance = createInstance();
        List<Token> actual = instance.getSlashStarRelevantTokens(sourceTokens);
        assertEquals(actual, expected);
    }

    @Test
    public void testGetDoubleSlashReleventTokens() {
        List<Token> expected = Arrays.asList(
            newToken(2, "//H", 12, "H", 3, "", newToken(2, "\r\n", 15)),
            newToken(24, "//JS println(\"World\")", 493, "0", 61, "", newToken(24, "\r\n", 514)),
            newToken(26, "//JS println(\"Hello\")", 560, "0", 73, "        ", newToken(26, "\r\n", 581))
        );
        String s = TestResourceLoader.loadResource("tokens-for-relevance.json", getClass());
        List<Token> sourceTokens = fetchTokens(s);
        CodeGenerationRequestCreator instance = createInstance();
        List<Token> actual = instance.getDoubleSlashReleventTokens(sourceTokens);
        assertEquals(actual, expected);
    }

    private static CodeGenerationRequestCreator createInstance() {        
        List<String> headerDoubleSlashSuffixes = Arrays.asList("H", "I");
        List<String> genCodeStartSuffixes = Arrays.asList("GS");
        List<String> genCodeEndSuffixes = Arrays.asList("GE");
        List<String> embeddedStringDoubleSlashSuffixes = Arrays.asList("ES");
        List<CodeGenerationRequestSpecification> requestSpecList = Arrays.asList(
            new CodeGenerationRequestSpecification(Arrays.asList("JS")),
            new CodeGenerationRequestSpecification(Arrays.asList("PY", "PY30"))
        );
        CodeGenerationRequestCreator instance = new CodeGenerationRequestCreator(
            headerDoubleSlashSuffixes, genCodeStartSuffixes, genCodeEndSuffixes,
            embeddedStringDoubleSlashSuffixes, requestSpecList);
        return instance;
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

    private static AugmentingCode createAugCode(String suffix, Block... blocks) {        
        AugmentingCode augCode = new AugmentingCode(Arrays.asList(blocks));
        augCode.setCommentSuffix(suffix);
        return augCode;
    }

    private static List<Token> fetchTokens(String s) {
        TokenLite[] ts = new Gson().fromJson(s, TokenLite[].class);
        List<Token> tokens = new ArrayList<>();
        int startPos = 0;
        for (TokenLite t : ts) {
            Token token = newToken(t.lineNumber, t.text, startPos);
            tokens.add(token);
            startPos = token.endPos;
        }
        return tokens;
    }

    private static Token newToken(int lineNumber, String suffixDescStr) {        
        SuffixDescriptor suffixDescriptor = createSuffixDescriptor(suffixDescStr);
        Map<String, Object> tokenAttributes = new HashMap<>();
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR,
            suffixDescriptor);
        Token t = new Token(0, null, 0, 0, lineNumber, 0);
        t.value = tokenAttributes;
        return t;
    }

    private static Token newToken(int lineNumber, String suffixDescStr, 
            String indent, String comment) {
        Map<String, Object> tokenAttributes = new HashMap<>();
        SuffixDescriptor suffixDescriptor = createSuffixDescriptor(suffixDescStr);
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR,
            suffixDescriptor);
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_INDENT,
            indent);
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_FF_NEWLINE,
            new Token(JavaLexer.TOKEN_TYPE_NEWLINE, "\n", 0, 0, lineNumber, 0));
        Token t = new Token(JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT, comment, 0, 0, 
            lineNumber, 0);
        t.value = tokenAttributes;
        return t;
    }

    private static Token newToken(int lineNumber, String text, int startPos,
            String suffixDescStr, int tokenIndex,
            String indent, Token ffNewline) {
        Map<String, Object> tokenAttributes = new HashMap<>();
        SuffixDescriptor suffixDescriptor = createSuffixDescriptor(suffixDescStr);
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR,
            suffixDescriptor);
        tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_INDEX_IN_SOURCE,
            tokenIndex);
        if (indent != null) {
            tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_INDENT,
                indent);
        }
        if (ffNewline != null) {
            tokenAttributes.put(CodeGenerationRequestCreator.TOKEN_ATTRIBUTE_FF_NEWLINE,
                ffNewline);
        }
        Token t = newToken(lineNumber, text, startPos);
        t.value = tokenAttributes;
        return t;
    }

    private static Token newTokenWithLnNum(int lineNumber) {
        return new Token(0, null, 0, 0, lineNumber, 0);
    }

    private static Token newTokenWithStartPos(int startPos) {
        return new Token(0, null, startPos, 0, 0, 0);
    }

    static class TokenLite {
        int lineNumber;
        String text;
    }

    private static Token newToken(int lineNumber, String text, int startPos) {
        int type;
        switch (text) {
            case "\r\n":
            case "\n":
                type = JavaLexer.TOKEN_TYPE_NEWLINE;
                break;
            case ";":
                type = JavaLexer.TOKEN_TYPE_SEMI_COLON;
                break;
            case "import":
                type = JavaLexer.TOKEN_TYPE_IMPORT_KEYWORD;
                break;
            default:
                if (text.startsWith("//")) {
                    type = JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT;
                }
                else if (text.startsWith("/*")) {
                    type = JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT;
                }
                else if (text.startsWith(" ")) {
                    type = JavaLexer.TOKEN_TYPE_NON_NEWLINE_WHITESPACE;
                }
                else {
                    type = JavaLexer.TOKEN_TYPE_OTHER;
                }
                break;
        }
        int endPos = startPos + text.length();
        Token token = new Token(type, text, startPos, endPos, lineNumber, 0);
        return token;
    }
}
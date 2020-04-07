package com.aaronicsubstances.code.augmentor.core.tasks;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.core.parsing.ParserInputSource;
import com.aaronicsubstances.code.augmentor.core.parsing.Token;
import com.aaronicsubstances.code.augmentor.core.tasks.CodeGenerationRequestCreator.SuffixDescriptor;
import com.google.gson.Gson;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CodeGenerationRequestCreatorTest {

    @Test(dataProvider = "createTestGetCommentContentWithoutSuffixData")
    public void testGetCommentContentWithoutSuffix(Token t, String suffix, String expected) {
        String actual = CodeGenerationRequestCreator.getCommentContentWithoutSuffix(t, suffix);
        assertEquals(actual, expected);
    }
    
    @DataProvider
    public Object[][] createTestGetCommentContentWithoutSuffixData() {
        return new Object[][]{
            new Object[]{ new Token(Token.TYPE_SINGLE_LINE_COMMENT, "//a", 0, 0, 0), 
                "", "a" },
            new Object[]{ new Token(Token.TYPE_MULTI_LINE_COMMENT, "/*b*/", 0, 0, 0), 
                "", "b" },
            new Object[]{ new Token(Token.TYPE_SINGLE_LINE_COMMENT, "//a", 0, 0, 0), 
                "a", "" },
            new Object[]{ new Token(Token.TYPE_MULTI_LINE_COMMENT, "/*a*/", 0, 0, 0), 
                "a", "" },
            new Object[]{ new Token(Token.TYPE_SINGLE_LINE_COMMENT, "//a: print('yes')", 0, 0, 0), 
                "a:", " print('yes')" },
            new Object[]{ new Token(Token.TYPE_SINGLE_LINE_COMMENT, "//a: print('yes')", 0, 0, 0), 
                "a: ", "print('yes')" },
            new Object[]{ new Token(Token.TYPE_MULTI_LINE_COMMENT, "/*/\ncreateClassSnippet('TEST')\n*/", 0, 0, 0), 
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
            new Object[]{ new Token(Token.TYPE_NON_NEWLINE_WHITESPACE, "//a", 0, 0, 0), 
                "a" },
            new Object[]{ new Token(Token.TYPE_NEWLINE, "/*a*/", 0, 0, 0), 
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
            new Object[]{ new Token(Token.TYPE_SINGLE_LINE_COMMENT, "//ES3", 0, 0, 0), 
                new SuffixDescriptor("ES", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_EMB_STRING, -1) },
            new Object[]{ new Token(Token.TYPE_MULTI_LINE_COMMENT, "/*GS5+*/", 0, 0, 0),
                new SuffixDescriptor("GS", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_GEN_CODE_START, -1) },
            new Object[]{ new Token(Token.TYPE_SINGLE_LINE_COMMENT, "//GE", 0, 0, 0),
                new SuffixDescriptor("GE", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_GEN_CODE_END, -1) },
            new Object[]{ new Token(Token.TYPE_MULTI_LINE_COMMENT, "/*H***/", 0, 0, 0), null },
            new Object[]{ new Token(Token.TYPE_MULTI_LINE_COMMENT, "/*JS6*/", 0, 0, 0),
                new SuffixDescriptor("JS", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 0) },
            new Object[]{ new Token(Token.TYPE_SINGLE_LINE_COMMENT, "//PY30#", 0, 0, 0),
                new SuffixDescriptor("PY30", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 1) },
            new Object[]{ new Token(Token.TYPE_MULTI_LINE_COMMENT, "/*PY3#*/", 0, 0, 0),
                new SuffixDescriptor("PY", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 1) },
            new Object[]{ new Token(Token.TYPE_SINGLE_LINE_COMMENT, "//PY30#", 0, 0, 0),
                new SuffixDescriptor("PY30", 
                CodeGenerationRequestCreator.SUFFIX_TYPE_AUG_CODE, 1) },
            new Object[]{ new Token(Token.TYPE_NEWLINE, "\n", 0, 0, 0), null }
        };
    }

    @Test(dataProvider = "createTestValidateDoubleSlashRelevantTokenGroupData")
    public void testValidateDoubleSlashRelevantTokenGroup(int index, 
            List<Token> tokenGroup, Integer expected) {
        // create input which spans max possible line and column numbers.
        final int maxLineNumber = 40;
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < maxLineNumber; i++) {
            int ch = 'A' + (i % 26);
            input.append((char)ch);
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
        List<Token> fifthGroup = Arrays.asList(newToken(14, "0"));
        List<Token> sixthGroup = Arrays.asList(newToken(13, "ES"));
        return new Object[][]{
            new Object[]{ 0, firstGroup, 2 },
            new Object[]{ 1, secondGroup, null },
            new Object[]{ 2, thirdGroup, null },
            new Object[]{ 3, fourthGroup, 4},
            new Object[]{ 4, fifthGroup, null },
            new Object[]{ 5, sixthGroup, 13 }
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
                new Block("{\n'value': true\n}", true), new Block("\n);\n println()", false)), }
        };
    }

    @Test
    public void testGetSlashStarRelevantTokens() {
        List<Token> expected = Arrays.asList(
            newToken(5, "/*JS println(\"Hello World from JS-star\")\r\n" +
                "var i = 3 + new Date(); \r\n" +
                "...etc*/", 59, "0", 3, null, null)
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
            newToken(24, "//JS println(\"World\")", 493, "0", 39, "", newToken(24, "\r\n", 514)),
            newToken(26, "//JS println(\"Hello\")", 560, "0", 51, "        ", newToken(26, "\r\n", 581))
        );
        String s = TestResourceLoader.loadResource("tokens-for-relevance.json", getClass());
        List<Token> sourceTokens = fetchTokens(s);
        CodeGenerationRequestCreator instance = createInstance();
        List<Token> actual = instance.getDoubleSlashReleventTokens(sourceTokens);
        assertEquals(actual, expected);
    }

    @Test(dataProvider = "createTestCreateGeneratedCodeDescriptorData")
    public void testCreateGeneratedCodeDescriptor(String sourceName, int startIndex, 
            boolean isSlashStar, GeneratedCodeDescriptor expected) {
        String s = TestResourceLoader.loadResource(sourceName, getClass());
        List<Token> sourceTokens = fetchTokens(s);
        CodeGenerationRequestCreator instance = createInstance();
        GeneratedCodeDescriptor actual = instance.createGeneratedCodeDescriptor(sourceTokens,
            startIndex, isSlashStar);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestCreateGeneratedCodeDescriptorData() {
        final String sourceName1 = "tokens-for-relevance.json";
        final String sourceName2 = "tokens-for-generated-code-descriptor.json";
        return new Object[][]{
            new Object[]{ sourceName1, 3, true, null },
            new Object[]{ sourceName1, 40, false, new GeneratedCodeDescriptor(514, 516) },
            new Object[]{ sourceName1, 52, false, new GeneratedCodeDescriptor(581, 583) },
            new Object[]{ sourceName1, 144, false, new GeneratedCodeDescriptor(1068, 1070) },
            new Object[]{ sourceName1, 145, false, null },
            new Object[]{ sourceName2, 0, true, null },
            new Object[]{ sourceName2, 1, true, null },
            new Object[]{ sourceName2, 7, true, null },
            new Object[]{ sourceName2, 16, true, null },
            new Object[]{ sourceName2, 34, true, null },
            new Object[]{ sourceName2, 38, true, null },
            new Object[]{ sourceName2, 80, true, null },
            new Object[]{ sourceName2, 66, false, new GeneratedCodeDescriptor(549, 577) },
            new Object[]{ sourceName2, 90, false, new GeneratedCodeDescriptor(672, 674) },
            new Object[]{ sourceName2, 105, true, null },
            new Object[]{ sourceName2, 136, true, new GeneratedCodeDescriptor(984, 997) },
            new Object[]{ sourceName2, 144, true, null },
            new Object[]{ sourceName2, 142, false, new GeneratedCodeDescriptor(1017, 1019) },
            new Object[]{ sourceName2, 149, false, new GeneratedCodeDescriptor(1036, 1038) },
            new Object[]{ sourceName2, 176, true, null },
            new Object[]{ sourceName2, 200, false, new GeneratedCodeDescriptor(1305, 1307) }
        };
    }

    @Test(dataProvider = "createTestProcessSourceFileData")
    public void testProcessSourceFile(String sourceName, SourceFileDescriptor expected,
            List<AugmentingCode> expectedAug1,
            List<AugmentingCode> expectedAug2) {
        String s = TestResourceLoader.loadResource(sourceName, getClass());
        List<Token> sourceTokens = fetchTokens(s);
        StringBuilder input = new StringBuilder(); 
        for (Token t : sourceTokens) {
            input.append(t.text);
        }
        ParserInputSource inputSource = new ParserInputSource(input.toString());
        List<List<AugmentingCode>> specAugCodesList = Arrays.asList(new ArrayList<>(),
            new ArrayList<>());
        CodeGenerationRequestCreator instance = createInstance();
        SourceFileDescriptor actual = instance.processSourceFile(inputSource, sourceTokens, 
            specAugCodesList, null);
        assertEquals(actual, expected);
        assertEquals(specAugCodesList.get(0), expectedAug1);
        assertEquals(specAugCodesList.get(1), expectedAug2);
    }

    @DataProvider
    public Object[][] createTestProcessSourceFileData() {
        SourceFileDescriptor first = new SourceFileDescriptor(new ArrayList<>());
        List<CodeSnippetDescriptor> bodySnippets2 = Arrays.asList(
            new CodeSnippetDescriptor(createAugCodeDescriptor(true, null, 728, 804, 0), null),
            new CodeSnippetDescriptor(createAugCodeDescriptor(false, "    ", 1042, 1063, 1),
                new GeneratedCodeDescriptor(1063, 1065))
        );
        SourceFileDescriptor second = new SourceFileDescriptor(bodySnippets2);
        List<AugmentingCode> secondJs = Arrays.asList(
            createAugCode(0, "JS", new Block(
                " println(\"Hello World from JS-star\")\r\n" +
            "var i = 3 + new Date(); \r\n" +
            "...etc", false)),
            createAugCode(1, "JS", new Block(" println(\"Hello\")", false))
        );

        // Data for 3rd test
        List<CodeSnippetDescriptor> bodySnippets3 = Arrays.asList(
            new CodeSnippetDescriptor(createAugCodeDescriptor(true, null, 59, 135, 0), null),
            new CodeSnippetDescriptor(createAugCodeDescriptor(false, "", 493, 514, 1),
                new GeneratedCodeDescriptor(514, 516)),
            new CodeSnippetDescriptor(createAugCodeDescriptor(false, "        ", 560, 581, 2),
                new GeneratedCodeDescriptor(581, 583))
        );
        SourceFileDescriptor third = new SourceFileDescriptor(bodySnippets3);
        List<AugmentingCode> thirdJs = Arrays.asList(
            createAugCode(0, "JS", new Block(
                " println(\"Hello World from JS-star\")\r\n" +
            "var i = 3 + new Date(); \r\n" +
            "...etc", false)),
            createAugCode(1, "JS", new Block(" println(\"World\")", false)),
            createAugCode(2, "JS", new Block(" println(\"Hello\")", false))
        );
        return new Object[][]{
            new Object[]{ "tokens-for-import.json", 
                first, Arrays.asList(), Arrays.asList() },
            new Object[]{ "tokens-for-generated-code-descriptor.json", 
                second, secondJs, Arrays.asList() },
            new Object[]{ "tokens-for-relevance.json", 
                third, thirdJs, Arrays.asList() }
        };
    }

    private static CodeGenerationRequestCreator createInstance() {
        List<String> genCodeStartSuffixes = Arrays.asList("GS");
        List<String> genCodeEndSuffixes = Arrays.asList("GE");
        List<String> embeddedStringDoubleSlashSuffixes = Arrays.asList("ES");
        List<List<String>> requestSpecList = Arrays.asList(
            Arrays.asList("JS"), Arrays.asList("PY", "PY30")
        );
        CodeGenerationRequestCreator instance = new CodeGenerationRequestCreator(
            genCodeStartSuffixes, genCodeEndSuffixes,
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
        return createAugCode(0, suffix, blocks);
    }

    private static AugmentingCode createAugCode(int index, String suffix, Block... blocks) {        
        AugmentingCode augCode = new AugmentingCode(Arrays.asList(blocks));
        augCode.setCommentSuffix(suffix);
        augCode.setIndex(index);
        return augCode;
    }

    private static AugmentingCodeDescriptor createAugCodeDescriptor(
            boolean isSlashStar, String indent, int startPos, int endPos, int index) {        
        AugmentingCodeDescriptor augCodeDesc = new AugmentingCodeDescriptor();
        augCodeDesc.setAnnotatedWithSlashStar(isSlashStar);
        augCodeDesc.setIndent(indent);
        augCodeDesc.setStartPos(startPos);
        augCodeDesc.setEndPos(endPos);
        augCodeDesc.setIndex(index);
        return augCodeDesc;
    }

    private static List<Token> fetchTokens(String s) {
        TokenLite[] ts = new Gson().fromJson(s, TokenLite[].class);
        List<Token> tokens = new ArrayList<>();
        int startPos = 0;
        for (TokenLite t : ts) {
            Token token = newToken(t.lineNumber, t.text, startPos);
            if (t.value != null) {
                token.value = new HashMap<>();
                token.value.put(Token.VALUE_KEY_IMPORT_STATEMENT, t.value);
            }
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
        // assume single char per line.
        int startPos = (lineNumber - 1) * 2;
        int endPos = startPos + 2;
        Token t = new Token(0, null, startPos, endPos, lineNumber);
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
            new Token(Token.TYPE_NEWLINE, "\n", 0, 0, lineNumber));
        Token t = new Token(Token.TYPE_SINGLE_LINE_COMMENT, comment, 0, 0, 
            lineNumber);
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
        return new Token(0, null, 0, 0, lineNumber);
    }

    private static Token newTokenWithStartPos(int startPos) {
        return new Token(0, null, startPos, 0, 0);
    }

    static class TokenLite {
        int lineNumber;
        String text;
        String value;
    }

    private static Token newToken(int lineNumber, String text, int startPos) {
        int type;
        switch (text) {
            case "\r\n":
            case "\n":
                type = Token.TYPE_NEWLINE;
                break;
            default:
                if (text.startsWith("#!")) {
                    type = Token.TYPE_SHEBANG;
                }
                else if (text.startsWith("package")) {
                    type = Token.TYPE_PACKAGE_STATEMENT;
                }
                else if (text.startsWith("import")) {
                    type = Token.TYPE_IMPORT_STATEMENT;
                }
                else if (text.startsWith("//")) {
                    type = Token.TYPE_SINGLE_LINE_COMMENT;
                }
                else if (text.startsWith("/*")) {
                    type = Token.TYPE_MULTI_LINE_COMMENT;
                }
                else if (text.startsWith(" ")) {
                    type = Token.TYPE_NON_NEWLINE_WHITESPACE;
                }
                else if (text.startsWith("\"") && text.length() > 1) {
                    type = Token.TYPE_LITERAL_STRING_CONTENT;
                    text = text.substring(1, text.length() - 1);
                }
                else {
                    type = Token.TYPE_OTHER;
                }
                break;
        }
        int endPos = startPos + text.length();
        Token token = new Token(type, text, startPos, endPos, lineNumber);
        return token;
    }
}
package com.aaronicsubstances.code.augmentor.core.tasks;

import static org.testng.Assert.*;

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
import com.aaronicsubstances.code.augmentor.core.parsing.Token;
import com.google.gson.Gson;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CodeGenerationRequestCreatorTest {

    /*@Test(dataProvider = "createTestProcessSourceFileData")
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
        augCode.setDirectiveMarker(suffix);
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
    }*/

    @Test(dataProvider = "createTestIdentifyAugCodeSectionsData")
    public void testIdentifyAugCodeSections(List<Token> tokens, List<List<Token>> expected) {
        List<List<Token>> actual = CodeGenerationRequestCreator.identifyAugCodeSections(tokens,
            null, null);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestIdentifyAugCodeSectionsData() {
        List<Token> secondTokenList = Arrays.asList(
            newTokenWithLnNum(1, "//00"), newTokenWithLnNum(3, "//01"),
            newTokenWithLnNum(5, "//01"), newTokenWithLnNum(7, "//01"));
        List<Token> thirdTokenList = Arrays.asList(
            newTokenWithLnNum(10, "//01"), newTokenWithLnNum(11, "//01"),
            newTokenWithLnNum(12, "//02"), newTokenWithLnNum(13, "//01"));

        List<Token> fourthTokenList = Arrays.asList(
            newTokenWithLnNum(10, "//01"), newTokenWithLnNum(11, "//01"),
            newTokenWithLnNum(13, "//ES"), newTokenWithLnNum(15, "//01"), 
            newTokenWithLnNum(16, "//01"), newTokenWithLnNum(17, "//01"));
        List<List<Token>> fourthGroup = Arrays.asList(
            Arrays.asList(
                newTokenWithLnNum(10, "//01"), newTokenWithLnNum(11, "//01")), 
            Arrays.asList(
                newTokenWithLnNum(13, "//ES")), 
            Arrays.asList(
                newTokenWithLnNum(15, "//01"), newTokenWithLnNum(16, "//01"), 
                newTokenWithLnNum(17, "//01")));

        List<Token> fifthTokenList = Arrays.asList(
            newTokenWithLnNum(10, "//00"), newTokenWithLnNum(12, "//01"),
            newTokenWithLnNum(13, "//ES"), newTokenWithLnNum(15, "//01"), 
            newTokenWithLnNum(16, "//01"), newTokenWithLnNum(20, "//00"));
        List<List<Token>> fifthGroup = Arrays.asList(
            Arrays.asList(
                newTokenWithLnNum(10, "//00")),
            Arrays.asList(
                newTokenWithLnNum(12, "//01"), newTokenWithLnNum(13, "//ES")), 
            Arrays.asList(
                newTokenWithLnNum(15, "//01"), newTokenWithLnNum(16, "//01")),            
            Arrays.asList(
                newTokenWithLnNum(20, "//00")));

        List<Token> sixthList = Arrays.asList(
            newTokenWithLnNum(10, "//01"));

        // test enable/disable scans
        List<Token> seventhList = new ArrayList<>(fifthTokenList);
        seventhList.add(0, newTokenWithLnNum(1, "//--"));

        List<Token> eighthList = new ArrayList<>(fifthTokenList);
        eighthList.add(newTokenWithLnNum(21, "//--"));
        eighthList.add(newTokenWithLnNum(22, "//ES"));

        List<Token> ninthTokenList = Arrays.asList(
            newTokenWithLnNum(10, "//01"), newTokenWithLnNum(11, "//--"),
            newTokenWithLnNum(12, "//01"), newTokenWithLnNum(13, "//++"),
            newTokenWithLnNum(14, "//GE"), newTokenWithLnNum(15, "//03"));
        List<List<Token>> ninthGroup = Arrays.asList(
            Arrays.asList(
                newTokenWithLnNum(10, "//01")),
            Arrays.asList(
                newTokenWithLnNum(15, "//03")));

        return new Object[][] {
            { Arrays.asList(), Arrays.asList() },
            { secondTokenList, 
                secondTokenList.stream()
                    .map(t -> Arrays.asList(t))
                    .collect(Collectors.toList()), },
            { thirdTokenList, Arrays.asList(thirdTokenList) },
            { fourthTokenList, fourthGroup },
            { fifthTokenList, fifthGroup },
            { sixthList, Arrays.asList(sixthList) },
            { seventhList, Arrays.asList() },
            { eighthList, fifthGroup },
            { ninthTokenList, ninthGroup }
        };
    }
    
    @Test(dataProvider = "createTestValidateAugCodeSectionData")
    public void testValidateAugCodeSection(int index, 
            List<Token> tokenGroup, Integer expected) {
        ParserException actual = CodeGenerationRequestCreator
            .validateAugCodeSection(tokenGroup, null);
        if (expected == null) {
            assertNull(actual);
        }
        else {
            assertNotNull(actual);
            assertEquals(actual.getLineNumber(), (int)expected, "Line numbers differ");
            System.out.println("testValidateAugCodeSection[" + index +
                "].exceptionMessage = " + actual.getMessage());
        }
    }

    @DataProvider
    public Object[][] createTestValidateAugCodeSectionData() {
        List<Token> firstGroup = Arrays.asList(newTokenWithLnNum(2, "//ES"));       
        List<Token> secondGroup = Arrays.asList(newTokenWithLnNum(2, "//01"));       
        List<Token> thirdGroup = Arrays.asList(newTokenWithLnNum(2, "//01"),
            newTokenWithLnNum(3, "//ES"));
        List<Token> fourthGroup = Arrays.asList(newTokenWithLnNum(2, "//-1"),
            newTokenWithLnNum(3, "//ES"), newTokenWithLnNum(4, "//-2"));
        List<Token> fifthGroup = Arrays.asList(newTokenWithLnNum(14, "//-3"));
        List<Token> sixthGroup = Arrays.asList(newTokenWithLnNum(13, "//ES"));

        // test mixed scenarios of aug code directives
        List<Token> seventhGroup = Arrays.asList(newTokenWithLnNum(2, "//01"),
            newTokenWithLnNum(3, "//ES"), newTokenWithLnNum(4, "//-3"));

        // more tests on data driven directive
        List<Token> eighthGroup = Arrays.asList(newTokenWithLnNum(12, "//01"),
            newTokenWithLnNum(13, "//ES"), newTokenWithLnNum(13, "//ES"),
            newTokenWithLnNum(14, "//01"));

        List<Token> ninthGroup = Arrays.asList(newTokenWithLnNum(12, "//-1"),
            newTokenWithLnNum(13, "//ES"), newTokenWithLnNum(13, "//ES"),
            newTokenWithLnNum(14, "//-1"));

        return new Object[][]{
            { 0, firstGroup, 2 },
            { 1, secondGroup, 2 },
            { 2, thirdGroup, null },
            { 3, fourthGroup, 4},
            { 4, fifthGroup, null },
            { 5, sixthGroup, 13 },
            { 6, seventhGroup, 4,},
            { 7, eighthGroup, 14,},
            { 8, ninthGroup, null,}
        };
    }
    
    @Test(dataProvider = "createTestCreateAugmentingCodeBlocksData")
    public void testCreateAugmentingCodeBlocks(List<Token> tokenGroup,
            List<Block> expected) {
        List<Block> actual = CodeGenerationRequestCreator.createAugmentingCodeBlocks(tokenGroup);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestCreateAugmentingCodeBlocksData() {
        List<Token> firstGroup = Arrays.asList(
            newTokenWithLnNum(5, "//-1 println"));
        List<Token> secondGroup = Arrays.asList(
            newTokenWithLnNum(5, "//-2 println"),
            newTokenWithLnNum(6, "//-2("),
            newTokenWithLnNum(7, "//ES{'value': true}"),
            newTokenWithLnNum(8, "//-2)")
        );
        List<Token> thirdGroup = Arrays.asList(
            newTokenWithLnNum(5, "//-1 println("),
            newTokenWithLnNum(6, "//ES{"),
            newTokenWithLnNum(7, "//ES'value': true}"),
            newTokenWithLnNum(8, "//-1)")
        );
        List<Token> fourthGroup = Arrays.asList(
            newTokenWithLnNum(5, "//-1 println"),
            newTokenWithLnNum(6, "//-1("),
            newTokenWithLnNum(7, "//ES{'value': true"),
            newTokenWithLnNum(8, "//ES}")
        );
        List<Token> fifthGroup = Arrays.asList(
            newTokenWithLnNum(5, "//-1 println"),
            newTokenWithLnNum(6, "//-1("),
            newTokenWithLnNum(7, "//ES{"),
            newTokenWithLnNum(8, "//ES'value': true"),
            newTokenWithLnNum(9, "//ES}"),
            newTokenWithLnNum(10, "//-1);"),
            newTokenWithLnNum(11, "//-1 println()")
        );
        List<Token> sixthGroup = Arrays.asList(
            newTokenWithLnNum(15, "//00 println"),
            newTokenWithLnNum(16, "//ES[]")
        );
        return new Object[][]{
            { firstGroup, Arrays.asList(new Block(" println", false)) },
            { secondGroup, Arrays.asList(new Block(" println\r\n(", false),
                new Block("{'value': true}", true), new Block(")", false)), },
            { thirdGroup, Arrays.asList(new Block(" println(", false),
                new Block("{\r\n'value': true}", true), new Block(")", false))
            },
            { fourthGroup, Arrays.asList(new Block(" println\r\n(", false),
                new Block("{'value': true\r\n}", true)) },            
            { fifthGroup, Arrays.asList(new Block(" println\r\n(", false),
                new Block("{\r\n'value': true\r\n}", true), new Block(");\r\n println()", false)) },
            { sixthGroup, Arrays.asList(new Block(" println", false),
                new Block("[]", true)) },
        };
    }

    @Test(dataProvider = "createTestCreateGeneratedCodeDescriptorData")
    public void testCreateGeneratedCodeDescriptor(String sourceName, int startIndex, 
            GeneratedCodeDescriptor expected) {
        List<Token> sourceTokens = fetchTokens(sourceName);
        GeneratedCodeDescriptor actual = CodeGenerationRequestCreator.createGeneratedCodeDescriptor(
            sourceTokens, startIndex);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestCreateGeneratedCodeDescriptorData() {
        final String sourceName1 = "php-tokens.json";
        final String sourceName2 = "java-tokens.json";
        return new Object[][]{
            { sourceName1, 4, null },
            { sourceName1, 5, new GeneratedCodeDescriptor(43, 49, 63, 69) },
            { sourceName1, 10, null },
            { sourceName2, 2, null },
            { sourceName2, 13, null },
            { sourceName2, 20, null },
            { sourceName2, 24, new GeneratedCodeDescriptor(654, 660, 736, 742) },
            { sourceName2, 30, null },
            { sourceName2, 33, new GeneratedCodeDescriptor(859, 865, 900, 906) },
            { sourceName2, 40, null }
        };
    }

    static class TokenLite {
        public String text;
        public boolean noNewline;
        public int startPos;
        public int endPos;

        public TokenLite() {
        }

        public TokenLite(String text, int startPos, int endPos) {
            this.text = text;
            this.startPos = startPos;
            this.endPos = endPos;
        }

        public TokenLite(String text, boolean noNewline, int startPos, int endPos) {
            this.text = text;
            this.noNewline = noNewline;
            this.startPos = startPos;
            this.endPos = endPos;
        }
        
        public Token toToken() {
            int type, directiveType, augCodeSpecIndex = 0;
            String directiveContent;
            boolean uncheckedAugCode = false;
            final int commonMarkerLen = 4;
            if (text.equals("")) {
                type = CodeGenerationRequestCreator.TOKEN_TYPE_BLANK;
                directiveType = 0;
                directiveContent = null;
            }
            else if (text.startsWith("//GE")) {
                type = CodeGenerationRequestCreator.TOKEN_TYPE_DIRECTIVE;
                directiveType = CodeGenerationRequestCreator.DIRECTIVE_TYPE_GEN_CODE_END;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//GS")) {
                type = CodeGenerationRequestCreator.TOKEN_TYPE_DIRECTIVE;
                directiveType = CodeGenerationRequestCreator.DIRECTIVE_TYPE_GEN_CODE_START;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//ES")) {
                type = CodeGenerationRequestCreator.TOKEN_TYPE_DIRECTIVE;
                directiveType = CodeGenerationRequestCreator.DIRECTIVE_TYPE_EMB_STRING;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//++")) {
                type = CodeGenerationRequestCreator.TOKEN_TYPE_DIRECTIVE;
                directiveType = CodeGenerationRequestCreator.DIRECTIVE_TYPE_ENABLE_SCAN;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//--")) {
                type = CodeGenerationRequestCreator.TOKEN_TYPE_DIRECTIVE;
                directiveType = CodeGenerationRequestCreator.DIRECTIVE_TYPE_DISABLE_SCAN;
                directiveContent = text.substring(commonMarkerLen);
            }
            else {
                try {
                    assert text.substring(0, 2).equals("//");
                    augCodeSpecIndex = Integer.parseInt(text.substring(2, 4));
                    type = CodeGenerationRequestCreator.TOKEN_TYPE_DIRECTIVE;
                    directiveType = CodeGenerationRequestCreator.DIRECTIVE_TYPE_AUG_CODE;
                    directiveContent = text.substring(commonMarkerLen);
                    if (augCodeSpecIndex < 0) {
                        // change 1-based of negative ints to 0-based.
                        augCodeSpecIndex = -augCodeSpecIndex - 1;
                        uncheckedAugCode = true;
                    }
                }
                catch (NumberFormatException | AssertionError | IndexOutOfBoundsException ex) {
                    type = CodeGenerationRequestCreator.TOKEN_TYPE_OTHER;
                    directiveType = 0;
                    directiveContent = null;
                }
            }
            Token t = new Token(type);
            t.text = text;
            t.startPos = startPos;
            t.endPos = endPos;
            t.directiveType = directiveType;
            t.directiveContent = directiveContent;
            t.augCodeSpecIndex = augCodeSpecIndex;
            t.uncheckedAugCodeDirective = uncheckedAugCode;
            if (!noNewline) {
                t.newline = "\r\n";
                t.text += t.newline;
            }
            return t;
        }
    }

    private static List<Token> fetchTokens(String path) {
        String s = TestResourceLoader.loadResource(path, CodeGenerationRequestCreatorTest.class);
        TokenLite[] ts = new Gson().fromJson(s, TokenLite[].class);
        List<Token> tokens = new ArrayList<>();
        int startPos = 0;
        for (int i = 0; i < ts.length; i++) {
            Token token = ts[i].toToken();
            token.startPos = startPos;
            token.endPos = token.startPos + token.text.length();
            token.index = i;
            token.lineNumber = token.index + 1;
            tokens.add(token);
            startPos = token.endPos;
        }
        return tokens;
    }

    private static Token newTokenWithLnNum(int lineNumber, String text) {
        Token t = new TokenLite(text, 0, 0).toToken();
        t.lineNumber = lineNumber;
        return t;
    }
}
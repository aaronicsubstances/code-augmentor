package com.aaronicsubstances.code.augmentor.core.tasks;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.util.ParserException;
import com.aaronicsubstances.code.augmentor.core.util.Token;
import com.google.gson.Gson;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CodeGenerationRequestCreatorTest {

    @Test(dataProvider = "createTestProcessSourceFileData")
    public void testProcessSourceFile(String sourceName, List<CodeSnippetDescriptor> expected,
            List<AugmentingCode> expectedAug1,
            List<AugmentingCode> expectedAug2) {
        List<Token> sourceTokens = TestResourceLoader.fetchTokens(sourceName,
            getClass());
        List<List<AugmentingCode>> specAugCodesList = Arrays.asList(new ArrayList<>(),
            new ArrayList<>());
        List<CodeSnippetDescriptor> actual = CodeGenerationRequestCreator.processSourceFile( 
            sourceTokens, null, specAugCodesList, null);
        assertEquals(actual, expected);
        assertEquals(specAugCodesList.get(0), expectedAug1);
        assertEquals(specAugCodesList.get(1), expectedAug2);
    }

    @DataProvider
    public Object[][] createTestProcessSourceFileData() {
        List<CodeSnippetDescriptor> bodySnippets2 = Arrays.asList(
            new CodeSnippetDescriptor(createAugCodeDescriptor("  ", 0, 261, 290), null),
            new CodeSnippetDescriptor(createAugCodeDescriptor("", 1, 294, 301), null)
        );
        List<AugmentingCode> augCodes20 = Arrays.asList(
            createAugCode("  ", 0, "#PHP", 
                new Block("", false, false),
                new Block("", true, false),
                new Block("[]", false, true))            
        );
        List<AugmentingCode> augCodes21 = Arrays.asList(
            createAugCode("", 1, "#PHP7", new Block("", false, false))
        );
        
        List<CodeSnippetDescriptor> bodySnippets4 = Arrays.asList(
            new CodeSnippetDescriptor(createAugCodeDescriptor("    ", 0, 27, 58), null),
            new CodeSnippetDescriptor(createAugCodeDescriptor("", 1, 61, 68),
                new GeneratedCodeDescriptor(68, 73, 78, 83))
        );
        List<AugmentingCode> augCodes40 = Arrays.asList(
            createAugCode("    ", 0, "#PHP", 
                new Block("", false, false),
                new Block("", true, false),
                new Block("12", false, true))            
        );
        List<AugmentingCode> augCodes41 = Arrays.asList(
            createAugCode("", 1, "#PHP7", new Block("", false, false))
        );
        return new Object[][] {
            new Object[]{ "tokens-for-process-source-00.json", 
                Arrays.asList(), Arrays.asList(), Arrays.asList() },
            new Object[]{ "tokens-for-process-source-01.json", 
                bodySnippets2, augCodes20, augCodes21 },
            new Object[]{ "tokens-for-process-source-02.json", 
                Arrays.asList(), Arrays.asList(), Arrays.asList() },
            new Object[]{ "tokens-for-process-source-03.json",
                bodySnippets4, augCodes40, augCodes41 }
        };
    }

    private static AugmentingCode createAugCode(String indent, int index, String directiveMarker,
            Block... blocks) {
        AugmentingCode augCode = new AugmentingCode(Arrays.asList(blocks));
        augCode.setDirectiveMarker(directiveMarker);
        augCode.setIndex(index);
        augCode.setIndent(indent != null ? indent : "");
        return augCode;
    }

    private static AugmentingCodeDescriptor createAugCodeDescriptor(
            String indent, int index, int startPos, int endPos) {        
        AugmentingCodeDescriptor augCodeDesc = new AugmentingCodeDescriptor();
        augCodeDesc.setIndent(indent != null ? indent : "");
        augCodeDesc.setStartPos(startPos);
        augCodeDesc.setEndPos(endPos);
        augCodeDesc.setIndex(index);
        return augCodeDesc;
    }

    @Test(dataProvider = "createTestIdentifyAugCodeSectionsData")
    public void testIdentifyAugCodeSections(TestArgWrapper tokens, TestArgWrapper expected) {
        List<List<Token>> actual = CodeGenerationRequestCreator.identifyAugCodeSections(
            tokens.tokens, null, null);
        assertEquals(actual, expected.tokenGroups);
    }

    @DataProvider
    public Object[][] createTestIdentifyAugCodeSectionsData() {
        // test with initial data excluding enable/disable scans, or
        // generated code sections.
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

        // test disable scan.
        List<Token> seventhList = new ArrayList<>(fifthTokenList);
        seventhList.add(0, newTokenWithLnNum(1, "//--"));

        // test that generated code section doesn't recognize
        // disable/enable scans.
        List<Token> eighthList = new ArrayList<>(fifthTokenList);
        eighthList.add(newTokenWithLnNum(21, "//GS"));
        eighthList.add(newTokenWithLnNum(22, "//--"));
        eighthList.add(newTokenWithLnNum(23, "//GE"));
        eighthList.add(newTokenWithLnNum(24, "//ES"));

        List<List<Token>> eighthGroup = Arrays.asList(
            Arrays.asList(
                newTokenWithLnNum(10, "//00")),
            Arrays.asList(
                newTokenWithLnNum(12, "//01"), newTokenWithLnNum(13, "//ES")), 
            Arrays.asList(
                newTokenWithLnNum(15, "//01"), newTokenWithLnNum(16, "//01")),            
            Arrays.asList(
                newTokenWithLnNum(20, "//00")),
            Arrays.asList(
                newTokenWithLnNum(24, "//ES")));
        
        // test that disable/enable scans don't recognize generated code section.
        List<Token> ninthTokenList = Arrays.asList(
            newTokenWithLnNum(10, "//--"), newTokenWithLnNum(11, "//01"),
            newTokenWithLnNum(12, "//--"), newTokenWithLnNum(13, "//01"),
            newTokenWithLnNum(14, "//GS"), newTokenWithLnNum(15, "//GE"),
            newTokenWithLnNum(22, "//02"), newTokenWithLnNum(33, "//01"),
            newTokenWithLnNum(36, "//++"), newTokenWithLnNum(37, "//01"),
            newTokenWithLnNum(38, "//++"), newTokenWithLnNum(39, "//03"));
        List<List<Token>> ninthGroup = Arrays.asList(
            Arrays.asList(
                newTokenWithLnNum(37, "//01")),
            Arrays.asList(
                newTokenWithLnNum(39, "//03")));
 
        List<Token> tenthTokenList = Arrays.asList(
            newTokenWithLnNum(10, "//01"), newTokenWithLnNum(11, "//--"),
            newTokenWithLnNum(12, "//GE"), newTokenWithLnNum(13, "//GS"),
            newTokenWithLnNum(14, "//++"), newTokenWithLnNum(15, "//03"));
        List<List<Token>> tenthGroup = Arrays.asList(
            Arrays.asList(
                newTokenWithLnNum(10, "//01")),
            Arrays.asList(
                newTokenWithLnNum(15, "//03")));

        TestArgWrapper tArg = new TestArgWrapper();
        return new Object[][] {
            { tArg.wrapTokens(Arrays.asList()), tArg.wrapTokenGroups(Arrays.asList()) },
            { tArg.wrapTokens(secondTokenList), tArg.wrapTokenGroups(secondTokenList.stream()
                    .map(t -> Arrays.asList(t))
                    .collect(Collectors.toList())), },
            { tArg.wrapTokens(thirdTokenList), tArg.wrapTokenGroups(Arrays.asList(thirdTokenList)) },
            { tArg.wrapTokens(fourthTokenList), tArg.wrapTokenGroups(fourthGroup) },
            { tArg.wrapTokens(fifthTokenList), tArg.wrapTokenGroups(fifthGroup) },
            { tArg.wrapTokens(sixthList), tArg.wrapTokenGroups(Arrays.asList(sixthList)) },
            { tArg.wrapTokens(seventhList), tArg.wrapTokenGroups(Arrays.asList()) },
            { tArg.wrapTokens(eighthList), tArg.wrapTokenGroups(eighthGroup) },
            { tArg.wrapTokens(ninthTokenList), tArg.wrapTokenGroups(ninthGroup) },
            { tArg.wrapTokens(tenthTokenList), tArg.wrapTokenGroups(tenthGroup) }
        };
    }
    
    @Test(dataProvider = "createTestValidateAugCodeSectionData")
    public void testValidateAugCodeSection(int index, 
            TestArgWrapper tokenGroup, Integer expected) {
        ParserException actual = CodeGenerationRequestCreator
            .validateAugCodeSection(tokenGroup.tokens, null);
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
        List<Token> fourthGroup = Arrays.asList(newTokenWithLnNum(2, "//01"),
            newTokenWithLnNum(3, "//ES"), newTokenWithLnNum(4, "//02"));
        List<Token> fifthGroup = Arrays.asList(newTokenWithLnNum(14, "//03"));
        List<Token> sixthGroup = Arrays.asList(newTokenWithLnNum(13, "//ES"));

        // test mixed scenarios of aug code directives
        List<Token> seventhGroup = Arrays.asList(newTokenWithLnNum(2, "//01"),
            newTokenWithLnNum(3, "//ES"), newTokenWithLnNum(4, "//03"));

        // more tests involving embedded json directives
        List<Token> eighthGroup = Arrays.asList(newTokenWithLnNum(12, "//01"),
            newTokenWithLnNum(13, "//{>"), newTokenWithLnNum(13, "//{>"),
            newTokenWithLnNum(14, "//01"));

        List<Token> ninthGroup = Arrays.asList(newTokenWithLnNum(12, "//01"),
            newTokenWithLnNum(13, "//ES"), newTokenWithLnNum(13, "//{>"),
            newTokenWithLnNum(14, "//01"));

        TestArgWrapper tArg = new TestArgWrapper();
        return new Object[][]{
            { 0, tArg.wrapTokens(firstGroup), 2 },
            { 1, tArg.wrapTokens(secondGroup), null },
            { 2, tArg.wrapTokens(thirdGroup), null },
            { 3, tArg.wrapTokens(fourthGroup), 4},
            { 4, tArg.wrapTokens(fifthGroup), null },
            { 5, tArg.wrapTokens(sixthGroup), 13 },
            { 6, tArg.wrapTokens(seventhGroup), 4,},
            { 7, tArg.wrapTokens(eighthGroup), null,},
            { 8, tArg.wrapTokens(ninthGroup), null,}
        };
    }
    
    @Test(dataProvider = "createTestCreateAugmentingCodeBlocksData")
    public void testCreateAugmentingCodeBlocks(TestArgWrapper tokenGroup,
            TestArgWrapper expected, List<Integer> expectedReceiver) {
        List<Integer> receiver = new ArrayList<>();
        List<Block> actual = CodeGenerationRequestCreator.createAugmentingCodeBlocks(
            tokenGroup.tokens, receiver);
        assertEquals(actual, expected.blocks);
        assertEquals(receiver, expectedReceiver);
    }

    @DataProvider
    public Object[][] createTestCreateAugmentingCodeBlocksData() {
        Token[] firstGroup = new Token[]{
            newTokenWithLnNum(5, "//01 println") };
        List<Integer> firstReceiver = Arrays.asList(0);

        Token[] secondGroup = new Token[]{
            newTokenWithLnNum(5, "//02 println"),
            newTokenWithLnNum(6, "//02("),
            newTokenWithLnNum(7, "//ES{'value': true}"),
            newTokenWithLnNum(8, "//02)")
        };
        List<Integer> secondReceiver = Arrays.asList(0, 2, 3);

        Token[] thirdGroup = new Token[]{
            newTokenWithLnNum(5, "//01 println("),
            newTokenWithLnNum(6, "//ES{"),
            newTokenWithLnNum(7, "//ES'value': true}"),
            newTokenWithLnNum(8, "//01)")
        };
        List<Integer> thirdReceiver = Arrays.asList(0, 1, 3);

        Token[] fourthGroup = new Token[]{
            newTokenWithLnNum(5, "//01 println"),
            newTokenWithLnNum(6, "//01("),
            newTokenWithLnNum(7, "//ES{'value': true"),
            newTokenWithLnNum(8, "//ES}")
        };
        List<Integer> fourthReceiver = Arrays.asList(0, 2);

        Token[] fifthGroup = new Token[]{
            newTokenWithLnNum(5, "//01 println"),
            newTokenWithLnNum(6, "//01("),
            newTokenWithLnNum(7, "//ES{"),
            newTokenWithLnNum(8, "//{>{'value': true"),
            newTokenWithLnNum(9, "//{>}"),
            newTokenWithLnNum(10, "//01);"),
            newTokenWithLnNum(11, "//01 println()")
        };
        List<Integer> fifthReceiver = Arrays.asList(0, 2, 3, 5);

        Token[] sixthGroup = new Token[]{
            newTokenWithLnNum(15, "//00 println"),
            newTokenWithLnNum(16, "//{>[]")
        };
        List<Integer> sixthReceiver = Arrays.asList(0, 1);

        TestArgWrapper tArg = new TestArgWrapper();
        return new Object[][]{
            { tArg.wrapTokens(firstGroup), tArg.wrapBlocks(new Block(" println", false, false)),
              firstReceiver },
            { tArg.wrapTokens(secondGroup), tArg.wrapBlocks(new Block(" println\n(", false, false),
                new Block("{'value': true}", true, false), new Block(")", false, false)),
              secondReceiver },
            { tArg.wrapTokens(thirdGroup), tArg.wrapBlocks(new Block(" println(", false, false),
                new Block("{\n'value': true}", true, false), new Block(")", false, false)),
              thirdReceiver
            },
            { tArg.wrapTokens(fourthGroup), tArg.wrapBlocks(new Block(" println\n(", false, false),
                new Block("{'value': true\n}", true, false)),
              fourthReceiver },            
            { tArg.wrapTokens(fifthGroup), tArg.wrapBlocks(new Block(" println\n(", false, false),
                new Block("{", true, false),
                new Block("{'value': true\n}", false, true), 
                new Block(");\n println()", false, false)),
              fifthReceiver },
            { tArg.wrapTokens(sixthGroup), tArg.wrapBlocks(new Block(" println", false, false),
                new Block("[]", false, true)),
              sixthReceiver },
        };
    }

    @Test(dataProvider = "createTestCreateGeneratedCodeDescriptorData")
    public void testCreateGeneratedCodeDescriptor(TestArgWrapper sourceTokens, int startIndex, 
            GeneratedCodeDescriptor expected) {
        GeneratedCodeDescriptor actual = CodeGenerationRequestCreator.createGeneratedCodeDescriptor(
            sourceTokens.tokens, startIndex);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestCreateGeneratedCodeDescriptorData() {
        String sourceName = "tokens-for-generated-code-descriptor.json";
        List<Token> tokens = fetchTokens(sourceName);
        TestArgWrapper tokenSource = new TestArgWrapper(sourceName).wrapTokens(tokens);
        return new Object[][]{
            { tokenSource, 0, null },
            { tokenSource, 5, null },
            { tokenSource, 9, new GeneratedCodeDescriptor(227, 234, 329, 335) },
            { tokenSource, 10, null },
            { tokenSource, 13, null },
            { tokenSource, 19, new GeneratedCodeDescriptor(337, 343, 508, 514) },
            { tokenSource, 20, null },
            { tokenSource, 24, null },
            { tokenSource, 30, null },
            { tokenSource, 32, new GeneratedCodeDescriptor(574, 580, 663, 669) },
            { tokenSource, 33, null },
            { tokenSource, 40, null },
            { tokenSource, 41, new GeneratedCodeDescriptor(723, 731, 1012, 1018) },
            { tokenSource, 60, null },
            { tokenSource, 62, new GeneratedCodeDescriptor(1095, 1101, 1172, 1178) },
            { tokenSource, 67, null },
            { tokenSource, 69, new GeneratedCodeDescriptor(1178, 1184, 1232, 1241) },
            { tokenSource, 74, new GeneratedCodeDescriptor(1241, 1247, 1258, 1264) },
            { tokenSource, 81, null} 
        };
    }

    @Test(dataProvider = "createTestCreateGeneratedCodeDescriptorForErrorData",
        expectedExceptions = RuntimeException.class)
    public void testCreateGeneratedCodeDescriptorForError(TestArgWrapper sourceTokens, int startIndex) {
        CodeGenerationRequestCreator.createGeneratedCodeDescriptor(
            sourceTokens.tokens, startIndex);
    }

    @DataProvider
    public Object[][] createTestCreateGeneratedCodeDescriptorForErrorData() {
        String sourceName = "tokens-for-generated-code-descriptor.json";
        List<Token> tokens = fetchTokens(sourceName);
        TestArgWrapper tokenSource = new TestArgWrapper(sourceName).wrapTokens(tokens);
        return new Object[][]{
            { tokenSource, 57 }, // test for gen code section not ending before another section starts.
            { tokenSource, 80 }  // test for gen code ending not found.
        };
    }

    /**
     * Used to wrap arguments to test methods, in order to avoid the time wasting and 
     * verbosity of stringifying test method arguments, for inclusion in test method instances
     * generated per each argument list.
     */
    static class TestArgWrapper {
        public String name;
        public Object arg;
        public List<Token> tokens;
        public List<List<Token>> tokenGroups;
        public List<Block> blocks;

        public TestArgWrapper() {
        }

        public TestArgWrapper(String name) {
            this.name = name;
        }

        public TestArgWrapper wrapTokens(Token... tokens) {
            return wrapTokens(Arrays.asList(tokens));
        }

        public TestArgWrapper wrapTokens(List<Token> tokens) {
            TestArgWrapper newInstance = new TestArgWrapper(name);
            newInstance.arg = newInstance.tokens = tokens;
            return newInstance;
        }

        public TestArgWrapper wrapTokenGroups(List<List<Token>> tokenGroups) {
            TestArgWrapper newInstance = new TestArgWrapper(name);
            newInstance.arg = newInstance.tokenGroups = tokenGroups;
            return newInstance;
        }

        public TestArgWrapper wrapBlocks(Block... blocks) {
            TestArgWrapper newInstance = new TestArgWrapper(name);
            newInstance.arg = newInstance.blocks = Arrays.asList(blocks);
            return newInstance;
        }

        @Override
        public String toString() {
            if (name != null) {
                return name;
            }
            if (arg == null) {
                return "null";
            }
            // generate a shorter name than Object.toString()
            return String.format("%s@%x",
                arg.getClass().getSimpleName(), arg.hashCode());
        }
    }

    static class TokenLite {
        public String text;
        public boolean noNewline;

        public TokenLite() {
        }

        public TokenLite(String text) {
            this.text = text;
        }
        
        public Token toToken(String newline) {
            int type, augCodeSpecIndex = 0;
            String directiveContent;
            final int commonMarkerLen = 4;
            if (text.equals("")) {
                type = Token.TYPE_BLANK;
                directiveContent = null;
            }
            else if (text.startsWith("//GE")) {
                type = Token.DIRECTIVE_TYPE_GEN_CODE_END;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//GS")) {
                type = Token.DIRECTIVE_TYPE_GEN_CODE_START;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//ES")) {
                type = Token.DIRECTIVE_TYPE_EMB_STRING;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//{>")) {
                type = Token.DIRECTIVE_TYPE_EMB_JSON;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//++")) {
                type = Token.DIRECTIVE_TYPE_ENABLE_SCAN;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//--")) {
                type = Token.DIRECTIVE_TYPE_DISABLE_SCAN;
                directiveContent = text.substring(commonMarkerLen);
            }
            else {
                try {
                    assert text.substring(0, 2).equals("//");
                    augCodeSpecIndex = Integer.parseInt(text.substring(2, 4));
                    assert augCodeSpecIndex >= 0;
                    type = Token.DIRECTIVE_TYPE_AUG_CODE;
                    directiveContent = text.substring(commonMarkerLen);
                }
                catch (NumberFormatException | AssertionError | IndexOutOfBoundsException ex) {
                    type = Token.TYPE_OTHER;
                    directiveContent = null;
                }
            }
            Token t = new Token(type);
            t.text = text;
            t.directiveContent = directiveContent;
            t.augCodeSpecIndex = augCodeSpecIndex;
            if (!noNewline) {
                t.newline = newline;
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
            Token token = ts[i].toToken("\r\n");
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
        Token t = new TokenLite(text).toToken("\n");
        t.lineNumber = lineNumber;
        return t;
    }
}
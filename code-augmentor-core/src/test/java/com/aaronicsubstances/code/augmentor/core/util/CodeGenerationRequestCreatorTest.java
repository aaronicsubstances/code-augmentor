package com.aaronicsubstances.code.augmentor.core.util;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
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
            new CodeSnippetDescriptor(createAugCodeDescriptor("\r\n", "  ", 1, 261, 290, 13), null),
            new CodeSnippetDescriptor(createAugCodeDescriptor("\r\n", "", 2, 294, 301, 17), null)
        );
        List<AugmentingCode> augCodes20 = Arrays.asList(
            createAugCode("\r\n", "  ", 1, "#PHP", new int[]{13, 15}, null,
                new Block("", false, false),
                new Block("", true, false),
                new Block("[]", false, true))            
        );
        List<AugmentingCode> augCodes21 = Arrays.asList(
            createAugCode("\r\n", "", 2, "#PHP7", new int[]{17, 17}, null, 
                new Block("", false, false))
        );
        
        List<CodeSnippetDescriptor> bodySnippets4 = Arrays.asList(
            new CodeSnippetDescriptor(createAugCodeDescriptor("\r\n", "    ", 1, 27, 58, 2), null),
            new CodeSnippetDescriptor(createAugCodeDescriptor("\r\n", "", 2, 61, 68, 6),
                new GeneratedCodeDescriptor(68, 73, 78, 83, "", false))
        );
        List<AugmentingCode> augCodes40 = Arrays.asList(
            createAugCode("\r\n", "    ", 1, "#PHP", new int[]{2, 4}, null,
                new Block("", false, false),
                new Block("", true, false),
                new Block("12", false, true))            
        );
        List<AugmentingCode> augCodes41 = Arrays.asList(
            createAugCode("\r\n", "", 2, "#PHP7", new int[]{6, 6}, new int[]{7, 9},
                new Block("", false, false))
        );
        augCodes41.get(0).setGenCodeIndent("");
        
        List<CodeSnippetDescriptor> bodySnippets5 = Arrays.asList(
            new CodeSnippetDescriptor(createAugCodeDescriptor("\n", " ", 1, 0, 41, 1), null),
            new CodeSnippetDescriptor(createAugCodeDescriptor("\n", "    ", 2, 43, 63, 6),
                new GeneratedCodeDescriptor(64, 0, 0, 80, "", true)),
            new CodeSnippetDescriptor(createAugCodeDescriptor("\n", "", 3, 81, 108, 10), null),
            new CodeSnippetDescriptor(createAugCodeDescriptor("\n", "", 4, 109, 145, 13), null),                
            new CodeSnippetDescriptor(createAugCodeDescriptor("\n", "    ", 5, 146, 171, 16), null)
        );
        List<AugmentingCode> augCodes50 = Arrays.asList(
            createAugCode("\n", "    ", 2, "#PHP", new int[]{6, 6}, new int[]{8, 8}, 
                new Block(" separate", false, false)),
            createAugCode("\n", "    ", 5, "#PHP", new int[]{16, 16}, null,
                new Block("  ------------", false, false))
        );
        List<AugmentingCode> augCodes51 = Arrays.asList(
            createAugCode("\n", " ", 1, "#PHP7", new int[]{1, 4}, null,
                new Block(" generate\n", false, false),
                new Block(" [\n ]", false, true)),
            
            createAugCode("\n", "", 3, "#PHP5", new int[]{10, 11}, null,
                new Block(" complete", false, false),
                new Block(": tree", true, false)),

            createAugCode("\n", "", 4, "#PHP7", new int[]{13, 14}, null, 
                new Block("-----------", false, false),
                new Block(" ending again", true, false))                
        );

        augCodes50.get(0).setGenCodeIndent("");
        augCodes50.get(0).setHasNestedLevelStartMarker(true);
        augCodes50.get(0).setMatchingNestedLevelEndMarkerIndex(1);
        augCodes50.get(0).setExternalNestedContent("\n#GG# print(145)\n\n#PHP5(( complete\n" +
            "#ES: tree\n\n#PHP7))-----------\n#ES ending again\n\n");
        augCodes50.get(1).setHasNestedLevelEndMarker(true);
        augCodes50.get(1).setMatchingNestedLevelStartMarkerIndex(0);

        augCodes51.get(1).setHasNestedLevelStartMarker(true);
        augCodes51.get(1).setMatchingNestedLevelEndMarkerIndex(2);
        augCodes51.get(1).setExternalNestedContent("\n");
        augCodes51.get(2).setHasNestedLevelEndMarker(true);
        augCodes51.get(2).setMatchingNestedLevelStartMarkerIndex(1);
        augCodes51.get(1).setNestedLevelNumber(1);
        augCodes51.get(2).setNestedLevelNumber(1);

        return new Object[][] {
            new Object[]{ "tokens-for-process-source-00.json", 
                Arrays.asList(), Arrays.asList(), Arrays.asList() },
            new Object[]{ "tokens-for-process-source-01.json", 
                bodySnippets2, augCodes20, augCodes21 },
            new Object[]{ "tokens-for-process-source-02.json", 
                Arrays.asList(), Arrays.asList(), Arrays.asList() },
            new Object[]{ "tokens-for-process-source-03.json",
                bodySnippets4, augCodes40, augCodes41 },
            new Object[]{ "tokens-for-process-source-04.json",
                bodySnippets5, augCodes50, augCodes51 }
        };
    }

    private static AugmentingCode createAugCode(String newline, String indent,
            int id, String directiveMarker,
            int[] lineNumbers, int[] genCodeLineNumbers, Block... blocks) {
        AugmentingCode augCode = new AugmentingCode(Arrays.asList(blocks));
        augCode.setDirectiveMarker(directiveMarker);
        augCode.setId(id);
        augCode.setLineNumber(lineNumbers[0]);
        augCode.setEndLineNumber(lineNumbers[1]);
        augCode.setIndent(indent != null ? indent : "");
        augCode.setLineSeparator(newline);
        if (genCodeLineNumbers != null) {
            augCode.setGenCodeLineNumber(genCodeLineNumbers[0]);
            augCode.setGenCodeEndLineNumber(genCodeLineNumbers[1]);
        }
        return augCode;
    }

    private static AugmentingCodeDescriptor createAugCodeDescriptor(String newline,
            String indent, int id, int startPos, int endPos, int lineNumber) {        
        AugmentingCodeDescriptor augCodeDesc = new AugmentingCodeDescriptor();
        augCodeDesc.setIndent(indent != null ? indent : "");
        augCodeDesc.setStartPos(startPos);
        augCodeDesc.setEndPos(endPos);
        augCodeDesc.setId(id);
        augCodeDesc.setLineNumber(lineNumber);
        augCodeDesc.setLineSeparator(newline);
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
        // test with initial data excluding skip code sections.
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

        // test skipping code section.
        List<Token> seventhList = new ArrayList<>(fifthTokenList);
        seventhList.add(0, newTokenWithLnNum(1, "//++"));
        seventhList.add(newTokenWithLnNum(1, "//--"));

        // test that generated code section doesn't overlap skipped code sections.
        List<Token> eighthList = new ArrayList<>(fifthTokenList);
        eighthList.add(newTokenWithLnNum(24, "//ES"));
        eighthList.add(newTokenWithLnNum(25, "//GS"));
        eighthList.add(newTokenWithLnNum(26, "//GE"));
        eighthList.add(newTokenWithLnNum(27, "//++"));
        eighthList.add(newTokenWithLnNum(28, "//--"));

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
        
        // test that skipped code section don't overlap generated code section.
        List<Token> ninthTokenList = Arrays.asList(
            newTokenWithLnNum(10, "//++"), newTokenWithLnNum(11, "//01"),
            newTokenWithLnNum(12, "//--"), newTokenWithLnNum(13, "//GS"),
            newTokenWithLnNum(14, "//01"), newTokenWithLnNum(15, "//GE"),
            newTokenWithLnNum(22, "//02"), newTokenWithLnNum(33, "//01"),
            newTokenWithLnNum(34, "//++"), newTokenWithLnNum(35, "//01"),
            newTokenWithLnNum(36, "//--"), newTokenWithLnNum(38, "//01"),
            newTokenWithLnNum(39, "//03"));
        List<List<Token>> ninthGroup = Arrays.asList(
            Arrays.asList(
                newTokenWithLnNum(22, "//02")),
            Arrays.asList(
                newTokenWithLnNum(33, "//01")),
            Arrays.asList(
                newTokenWithLnNum(38, "//01"), newTokenWithLnNum(39, "//03")));
 
        List<Token> tenthTokenList = Arrays.asList(
            newTokenWithLnNum(10, "//01"), newTokenWithLnNum(11, "//++"),
            newTokenWithLnNum(12, "//02"), newTokenWithLnNum(13, "//03"),
            newTokenWithLnNum(14, "//--"), newTokenWithLnNum(15, "//03"));
        List<List<Token>> tenthGroup = Arrays.asList(
            Arrays.asList(
                newTokenWithLnNum(10, "//01")),
            Arrays.asList(
                newTokenWithLnNum(15, "//03")));

        // test nested levels.
        List<Token> eleventhTokenList = new ArrayList<>();
        Token nested = newTokenWithLnNum(8, "//00");
        nested.nestedLevelStartMarker = "s";
        eleventhTokenList.add(nested);
        eleventhTokenList.add(newTokenWithLnNum(9, "//{>[]"));
        nested = newTokenWithLnNum(10, "");
        eleventhTokenList.add(nested);
        nested = newTokenWithLnNum(12, "//00");
        nested.nestedLevelEndMarker = "e";
        eleventhTokenList.add(nested);
        List<List<Token>> eleventhGroup = Arrays.asList(new ArrayList<>(), new ArrayList<>());
        nested = newTokenWithLnNum(8, "//00");
        nested.nestedLevelStartMarker = "s";
        eleventhGroup.get(0).add(nested);
        nested = newTokenWithLnNum(9, "//{>[]");
        nested.nestedLevelNumber = 1;
        eleventhGroup.get(0).add(nested);
        nested = newTokenWithLnNum(12, "//00");
        nested.nestedLevelEndMarker = "e";
        eleventhGroup.get(1).add(nested);
        
        List<Token> tokenList12 = new ArrayList<>();
        nested = newTokenWithLnNum(8, "//00");
        nested.nestedLevelStartMarker = "s";
        tokenList12.add(nested);
        tokenList12.add(newTokenWithLnNum(9, "//{>[]"));
        nested = newTokenWithLnNum(10, "");
        tokenList12.add(nested);
        nested = newTokenWithLnNum(12, "//00");
        nested.nestedLevelEndMarker = "e";
        tokenList12.add(nested);
        nested = newTokenWithLnNum(18, "//01");
        nested.nestedLevelStartMarker = "s";
        tokenList12.add(nested);
        tokenList12.add(newTokenWithLnNum(19, "//{>[]"));
        nested = newTokenWithLnNum(20, "");
        tokenList12.add(nested);
        nested = newTokenWithLnNum(22, "//01");
        nested.nestedLevelEndMarker = "e";
        tokenList12.add(nested);
        List<List<Token>> group12 = Arrays.asList(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>());
        nested = newTokenWithLnNum(8, "//00");
        nested.nestedLevelStartMarker = "s";
        group12.get(0).add(nested);
        nested = newTokenWithLnNum(9, "//{>[]");
        nested.nestedLevelNumber = 1;
        group12.get(0).add(nested);
        nested = newTokenWithLnNum(12, "//00");
        nested.nestedLevelEndMarker = "e";
        group12.get(1).add(nested);
        nested = newTokenWithLnNum(18, "//01");
        nested.nestedLevelStartMarker = "s";
        group12.get(2).add(nested);
        nested = newTokenWithLnNum(19, "//{>[]");
        nested.nestedLevelNumber = 1;
        group12.get(2).add(nested);
        nested = newTokenWithLnNum(22, "//01");
        nested.nestedLevelEndMarker = "e";
        group12.get(3).add(nested);
        
        List<Token> tokenList13 = new ArrayList<>();
        nested = newTokenWithLnNum(8, "//00");
        nested.nestedLevelStartMarker = "s";
        tokenList13.add(nested);
        tokenList13.add(newTokenWithLnNum(9, "//{>[]"));
        nested = newTokenWithLnNum(10, "");
        tokenList13.add(nested);
        nested = newTokenWithLnNum(12, "//01");
        nested.nestedLevelStartMarker = "s";
        tokenList13.add(nested);
        nested = newTokenWithLnNum(18, "//03");
        tokenList13.add(nested);
        nested = newTokenWithLnNum(20, "//01");
        nested.nestedLevelEndMarker = "e";
        tokenList13.add(nested);
        nested = newTokenWithLnNum(22, "//00");
        nested.nestedLevelEndMarker = "e";
        tokenList13.add(nested);
        List<List<Token>> group13 = Arrays.asList(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        nested = newTokenWithLnNum(8, "//00");
        nested.nestedLevelStartMarker = "s";
        group13.get(0).add(nested);
        nested = newTokenWithLnNum(9, "//{>[]");
        nested.nestedLevelNumber = 1;
        group13.get(0).add(nested);
        nested = newTokenWithLnNum(12, "//01");
        nested.nestedLevelNumber = 1;
        nested.nestedLevelStartMarker = "s";
        group13.get(1).add(nested);
        nested = newTokenWithLnNum(18, "//03");
        nested.nestedLevelNumber = 2;
        group13.get(2).add(nested);
        nested = newTokenWithLnNum(20, "//01");
        nested.nestedLevelEndMarker = "e";
        nested.nestedLevelNumber = 1;
        group13.get(3).add(nested);
        nested = newTokenWithLnNum(22, "//00");
        nested.nestedLevelEndMarker = "e";
        group13.get(4).add(nested);

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
            { tArg.wrapTokens(tenthTokenList), tArg.wrapTokenGroups(tenthGroup) },
            { tArg.wrapTokens(eleventhTokenList), tArg.wrapTokenGroups(eleventhGroup) },
            { tArg.wrapTokens(tokenList12), tArg.wrapTokenGroups(group12) },
            { tArg.wrapTokens(tokenList13), tArg.wrapTokenGroups(group13) }
        };
    }
    
    @Test(dataProvider = "createTestIdentifyAugCodeSectionsForErrorsData")
    public void testIdentifyAugCodeSectionsForErrors(int index, 
            TestArgWrapper tokenGroup, Integer expected) {
        try {
            CodeGenerationRequestCreator.identifyAugCodeSections(tokenGroup.tokens, null, null);
            if (expected != null) {
                fail("Expected exception at line number " + expected);
            }
        }
        catch (GenericTaskException ex) {
            if (expected == null) {
                fail("Didn't expect exception but got one", ex);
            }
            else {
                assertEquals(ex.getLineNumber(), (int)expected, "Line numbers differ");
                System.err.println("testIdentifyAugCodeSectionsForErrors[" + index +
                    "].exceptionMessage = " + ex.getMessage());
            }
        }
    }

    @DataProvider
    public Object[][] createTestIdentifyAugCodeSectionsForErrorsData() {
        List<Token> firstGroup = Arrays.asList(newTokenWithLnNum(2, "//++"));
        List<Token> secondGroup = Arrays.asList(newTokenWithLnNum(2, "//--"));
        List<Token> thirdGroup = Arrays.asList(newTokenWithLnNum(2, "//GS"),
            newTokenWithLnNum(3, "//GS"));
        List<Token> fourthGroup = Arrays.asList(newTokenWithLnNum(2, "//GS"),
            newTokenWithLnNum(3, "//--"), newTokenWithLnNum(4, "//02"));
        List<Token> fifthGroup = Arrays.asList(newTokenWithLnNum(14, "//GE"));
        List<Token> sixthGroup = Arrays.asList(newTokenWithLnNum(13, "//GE"),
            newTokenWithLnNum(13, "//ES"));
        List<Token> seventhGroup = Arrays.asList(newTokenWithLnNum(2, "//01"),
            newTokenWithLnNum(3, "//GS"), newTokenWithLnNum(4, "tow+03"));
        List<Token> eighthGroup = Arrays.asList(newTokenWithLnNum(12, "//GS"),
            newTokenWithLnNum(13, "//GG"));
        List<Token> ninthGroup = Arrays.asList(newTokenWithLnNum(12, "//GS"),
            newTokenWithLnNum(13, "//++"));
        List<Token> tenthGroup = Arrays.asList(newTokenWithLnNum(12, "//++"),
            newTokenWithLnNum(13, "//GE"));

        // add tests for nested level errors.
        Token nested = newTokenWithLnNum(18, "//00");
        nested.nestedLevelStartMarker = "s";
        List<Token> eleventhGroup = Arrays.asList(nested);

        nested = newTokenWithLnNum(19, "//01");
        nested.nestedLevelEndMarker = "e";
        List<Token> twelfthGroup = Arrays.asList(nested);

        List<Token> group13 = new ArrayList<>();
        nested = newTokenWithLnNum(18, "//00");
        nested.nestedLevelStartMarker = "s";
        group13.add(nested);
        nested = newTokenWithLnNum(20, "//01");
        nested.nestedLevelEndMarker = "e";
        group13.add(nested);
        
        int counter = 0;
        TestArgWrapper tArg = new TestArgWrapper();
        return new Object[][]{
            { counter++, tArg.wrapTokens(firstGroup), 2 },
            { counter++, tArg.wrapTokens(secondGroup), 2 },
            { counter++, tArg.wrapTokens(thirdGroup), 2 },
            { counter++, tArg.wrapTokens(fourthGroup), 3 },
            { counter++, tArg.wrapTokens(fifthGroup), 14 },
            { counter++, tArg.wrapTokens(sixthGroup), 13 },
            { counter++, tArg.wrapTokens(seventhGroup), 3 },
            { counter++, tArg.wrapTokens(eighthGroup), 12 },
            { counter++, tArg.wrapTokens(ninthGroup), 12 },
            { counter++, tArg.wrapTokens(tenthGroup), 13 },
            { counter++, tArg.wrapTokens(eleventhGroup), 18 },
            { counter++, tArg.wrapTokens(twelfthGroup), 19 },
            { counter++, tArg.wrapTokens(group13), 20 },

            // test required newline ending.
            { counter++, tArg.wrapTokens(Arrays.asList(newTokenWithLnNum3(10, "//GG"))), 10 },
            { counter++, tArg.wrapTokens(Arrays.asList(newTokenWithLnNum3(10, "//ES"))), 10 },
            { counter++, tArg.wrapTokens(Arrays.asList(newTokenWithLnNum3(10, "//{>"))), 10 },
            { counter++, tArg.wrapTokens(Arrays.asList(newTokenWithLnNum3(10, "//00"))), 10 },
            { counter++, tArg.wrapTokens(Arrays.asList(newTokenWithLnNum3(10, "for"))), null },
            { counter++, tArg.wrapTokens(Arrays.asList(newTokenWithLnNum(10, "//GS"),
                newTokenWithLnNum3(11, "//GE"))), 11 },
            { counter++, tArg.wrapTokens(Arrays.asList(newTokenWithLnNum(10, "//++"),
                newTokenWithLnNum3(11, "//--"))), 11 },
        };
    }
    
    @Test(dataProvider = "createTestValidateAugCodeSectionData")
    public void testValidateAugCodeSection(int index, 
            TestArgWrapper tokenGroup, Integer expected) {
        try {
            CodeGenerationRequestCreator.validateAugCodeSection(tokenGroup.tokens, null, null);
            if (expected != null) {
                fail("Expected exception at line number " + expected);
            }
        }
        catch (GenericTaskException ex) {
            if (expected == null) {
                fail("Didn't expect exception but got one", ex);
            }
            else {
                assertEquals(ex.getLineNumber(), (int)expected, "Line numbers differ");
                System.err.println("testValidateAugCodeSection[" + index +
                    "].exceptionMessage = " + ex.getMessage());
            }
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

        Token wrongNested = newTokenWithLnNum(15, "//01");
        wrongNested.nestedLevelStartMarker = "ss"; 
        List<Token> tenthGroup = Arrays.asList(newTokenWithLnNum(12, "//01"),
            newTokenWithLnNum(13, "//ES"), newTokenWithLnNum(13, "//{>"),
            newTokenWithLnNum(14, "//01"), wrongNested);

        wrongNested = newTokenWithLnNum(14, "//01");
        wrongNested.nestedLevelEndMarker = "ee";
        List<Token> eleventhGroup = Arrays.asList(newTokenWithLnNum(12, "//01"),
            newTokenWithLnNum(13, "//ES"),  wrongNested);

        Token correctNested = newTokenWithLnNum(7, "//00");
        correctNested.nestedLevelStartMarker = "cs";
        List<Token> twelfthGroup = Arrays.asList(correctNested);
            
        correctNested = newTokenWithLnNum(7, "//00");
        correctNested.nestedLevelEndMarker = "ce";
        List<Token> group13 = Arrays.asList(correctNested);

        TestArgWrapper tArg = new TestArgWrapper();
        int counter = 0;
        return new Object[][]{
            { counter++, tArg.wrapTokens(firstGroup), 2 },
            { counter++, tArg.wrapTokens(secondGroup), null },
            { counter++, tArg.wrapTokens(thirdGroup), null },
            { counter++, tArg.wrapTokens(fourthGroup), 4 },
            { counter++, tArg.wrapTokens(fifthGroup), null },
            { counter++, tArg.wrapTokens(sixthGroup), 13 },
            { counter++, tArg.wrapTokens(seventhGroup), 4 },
            { counter++, tArg.wrapTokens(eighthGroup), null },
            { counter++, tArg.wrapTokens(ninthGroup), null },
            { counter++, tArg.wrapTokens(tenthGroup), 15 },
            { counter++, tArg.wrapTokens(eleventhGroup), 14 },
            { counter++, tArg.wrapTokens(twelfthGroup), null },
            { counter++, tArg.wrapTokens(group13), null }
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
            GeneratedCodeDescriptor expected, int[] expectedRange) {
        int[] actualRange = new int[2];
        GeneratedCodeDescriptor actual = CodeGenerationRequestCreator.createGeneratedCodeDescriptor(
            sourceTokens.tokens, startIndex, actualRange);
        assertEquals(actual, expected);
        if (expectedRange != null) {
            assertEquals(actualRange, expectedRange);
        }
    }

    @DataProvider
    public Object[][] createTestCreateGeneratedCodeDescriptorData() {
        String sourceName = "tokens-for-generated-code-descriptor.json";
        List<Token> tokens = fetchTokens(sourceName);
        TestArgWrapper tokenSource = new TestArgWrapper(sourceName).wrapTokens(tokens);
        return new Object[][]{
            { tokenSource, 0, null, null },
            { tokenSource, 5, null, null },
            { tokenSource, 9, new GeneratedCodeDescriptor(227, 234, 329, 335, "", false),
                new int[]{11, 19} },
            { tokenSource, 10, null, null },
            { tokenSource, 13, null, null },
            { tokenSource, 19, new GeneratedCodeDescriptor(337, 343, 508, 514, "", false),
            new int[]{21, 30} },
            { tokenSource, 20, null, null },
            { tokenSource, 24, null, null },
            { tokenSource, 30, null, null },
            { tokenSource, 32, new GeneratedCodeDescriptor(574, 580, 663, 669, "", false),
                new int[]{34, 38} },
            { tokenSource, 33, null, null },
            { tokenSource, 40, null, null },
            { tokenSource, 41, new GeneratedCodeDescriptor(723, 731, 1012, 1018, "", false),
                new int[]{43, 57} },
            { tokenSource, 60, null, null },
            { tokenSource, 62, new GeneratedCodeDescriptor(1095, 1101, 1172, 1178, "", false),
                new int[]{64, 70} },
            { tokenSource, 67, null, null },
            { tokenSource, 69, new GeneratedCodeDescriptor(1178, 1184, 1232, 1241, "   ", false),
                new int[]{71, 75} },
            { tokenSource, 74, new GeneratedCodeDescriptor(1241, 1247, 1258, 1264, "", false),
                new int[]{76, 79} },
            { tokenSource, 81, null, null },
            { tokenSource, 82, new GeneratedCodeDescriptor(1384, 0, 0, 1412, "", true),
                new int[]{84, 86} },
        };
    }

    @Test(dataProvider = "createTestCreateGeneratedCodeDescriptorForErrorData",
        expectedExceptions = RuntimeException.class)
    public void testCreateGeneratedCodeDescriptorForError(TestArgWrapper sourceTokens, int startIndex) {
        CodeGenerationRequestCreator.createGeneratedCodeDescriptor(
            sourceTokens.tokens, startIndex, new int[2]);
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

    @Test(dataProvider = "createTestCreatePaddedJsonEquivalentData")
    public void testCreatePaddedJsonEquivalent(int blockIndex,
            List<Token> augCodeSection, List<Integer> blockDelimiters, String expected) {
        String actual = CodeGenerationRequestCreator.createPaddedJsonEquivalent(
            blockIndex, augCodeSection, blockDelimiters);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestCreatePaddedJsonEquivalentData() {
        return new Object[][]{
            { 1, Arrays.asList(newTokenWithLnNum2(1, "//00", " "),
                    newTokenWithLnNum2(2, "//{> ['']", " ")), Arrays.asList(0, 1), 
                "\n      ['']"},
            { 1, Arrays.asList(newTokenWithLnNum(1, "//00"),
                    newTokenWithLnNum(2, "//{>[")), Arrays.asList(0, 1), 
                "\n    ["},
            { 1, Arrays.asList(newTokenWithLnNum(1, "//00"),
                    newTokenWithLnNum(2, "//{>[@")), Arrays.asList(0, 1), 
                "\n    [@"},
            { 1, Arrays.asList(newTokenWithLnNum(1, "//00"),
                    newTokenWithLnNum(2, "//{>[{]")), Arrays.asList(0, 1), 
                "\n    [{]"}
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
            boolean isGeneratedCodeMarker = false;
            boolean isInlineGeneratedCodeMarker = false;
            final int commonMarkerLen = 4;
            if (text.equals("")) {
                type = Token.TYPE_BLANK;
                directiveContent = null;
            }
            else if (text.startsWith("//GE")) {
                type = Token.DIRECTIVE_TYPE_SKIP_CODE_END;
                directiveContent = text.substring(commonMarkerLen);
                isGeneratedCodeMarker = true;
            }
            else if (text.startsWith("//GS")) {
                type = Token.DIRECTIVE_TYPE_SKIP_CODE_START;
                directiveContent = text.substring(commonMarkerLen);
                isGeneratedCodeMarker = true;
            }
            else if (text.startsWith("//GG")) {
                type = Token.DIRECTIVE_TYPE_SKIP_CODE_START;
                directiveContent = text.substring(commonMarkerLen);
                isGeneratedCodeMarker = true;
                isInlineGeneratedCodeMarker = true;
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
                type = Token.DIRECTIVE_TYPE_SKIP_CODE_START;
                directiveContent = text.substring(commonMarkerLen);
            }
            else if (text.startsWith("//--")) {
                type = Token.DIRECTIVE_TYPE_SKIP_CODE_END;
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
            if (t.type == Token.TYPE_OTHER) {                
                Matcher m = Pattern.compile("^\\s*").matcher(text);
                m.find();
                t.indent = m.group();
            }
            else if (t.type != Token.TYPE_BLANK) {
                t.indent = "";
            }
            if (directiveContent != null) {
                t.directiveContent = directiveContent;
                t.directiveMarker = text.substring(0, commonMarkerLen);
                t.isGeneratedCodeMarker = isGeneratedCodeMarker;
                t.isInlineGeneratedCodeMarker = isInlineGeneratedCodeMarker;
            }
            t.augCodeSpecIndex = augCodeSpecIndex;
            if (!noNewline && newline != null) {
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

    private static Token newTokenWithLnNum2(int lineNumber, String text, String indent) {
        Token t = new TokenLite(text).toToken("\n");
        t.lineNumber = lineNumber;
        t.indent = indent;
        t.text = indent + t.text;
        return t;
    }

    private static Token newTokenWithLnNum3(int lineNumber, String text) {
        Token t = new TokenLite(text).toToken(null);
        t.lineNumber = lineNumber;
        return t;
    }
}
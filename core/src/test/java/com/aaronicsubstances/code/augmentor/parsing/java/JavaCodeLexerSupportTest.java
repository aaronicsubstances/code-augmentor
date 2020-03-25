package com.aaronicsubstances.code.augmentor.parsing.java;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import com.aaronicsubstances.code.augmentor.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.parsing.SourceMap;

public class JavaCodeLexerSupportTest {

    @Test(dataProvider = "createTestTransformUnicodeEscapesData")
    public void testTransformUnicodeEscapes(String javaSourceCode, 
            String expResult) {
        SourceMap sourceMap = null;
        String result = JavaCodeLexerSupport.transformUnicodeEscapes(javaSourceCode, sourceMap);
        assertEquals(result, expResult);
    }
    
    @DataProvider
    public Object[][] createTestTransformUnicodeEscapesData() {
        return new Object[][]{
            { "", "" },
            { "\\", "\\" },
            { "\\U", "\\U" },
            { "\\\\u", "\\\\u" },
            { "abcdefg", "abcdefg" },            
            { "\\uuuuuuuuuu0061bc\\de\\\\u0065g", "abc\\de\\\\u0065g" },
            { "123\\\\\\u0041\\uuuDA00\\uuDE00", "123\\\\A\uDA00\uDE00" }
        };
    }

    @Test(dataProvider = "createTestWithIncompleteUnicodeEscapesData", 
            expectedExceptions = ParserException.class)
    public void testWithIncompleteUnicodeEscapes(String javaSourceCode) {
        SourceMap sourceMap = null;
        JavaCodeLexerSupport.transformUnicodeEscapes(javaSourceCode, sourceMap);
    }
    
    @DataProvider
    public Object[][] createTestWithIncompleteUnicodeEscapesData() {
        return new Object[][]{
            { "\\u" },
            { "abc\\udefg" },        
            { "\\uuuuuuuuuu006Lbc\\de\\\\u0065g" },
            {"\\uabc\\de\\\\u0065g" },
            { "123\\\\\\u0041\\uuuDA0\\uuDE0fg"}, 
            { "123\\\\A\uDA00\\uDE0P" }
        };
    }
}
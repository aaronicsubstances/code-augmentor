package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import com.aaronicsubstances.code.augmentor.core.TestArg;

public class RegexNodeTest {

    @Test(dataProvider = "createTestToStringData")
    public void testToString(TestArg<RegexNode> instanceWrapper, String expected) {
        RegexNode instance = instanceWrapper.value;
        String actual = instance.toString();
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestToStringData() {
        return new Object[][]{
            { new TestArg<>(new LiteralStringRegexNode(0)), "0" },
            { new TestArg<>(new LiteralStringRegexNode(new int[0])), "e" },
            { new TestArg<>(new LiteralStringRegexNode(new int[]{ 0, 1 })), "0:1" },
            { new TestArg<>(new LiteralStringRegexNode(new int[]{ 20, 111, 2 })), "20:111:2" },
            { new TestArg<>(new ConcatRegexNode(new LiteralStringRegexNode(0), 
                new LiteralStringRegexNode(1))), "0 - 1" },
            { new TestArg<>(new UnionRegexNode(new LiteralStringRegexNode(0), 
                new LiteralStringRegexNode(1))), "(0 | 1)" },
            { new TestArg<>(new UnionRegexNode(new ConcatRegexNode(
                new LiteralStringRegexNode(0), new LiteralStringRegexNode(1)), 
                new LiteralStringRegexNode(0))), "(0 - 1 | 0)" },
            { new TestArg<>(new UnionRegexNode(new LiteralStringRegexNode(0), 
                new LiteralStringRegexNode(1), new LiteralStringRegexNode(2))), "(0 | 1 | 2)" },
            { new TestArg<>(new KleeneClosureRegexNode(new ConcatRegexNode(
                new LiteralStringRegexNode(0), new LiteralStringRegexNode(1)))), "(0 - 1)*" },
            { new TestArg<>(new KleeneClosureRegexNode(new LiteralStringRegexNode(1))), "(1)*" },
            { new TestArg<>(new KleeneClosureRegexNode(new UnionRegexNode( 
                new LiteralStringRegexNode(0), new LiteralStringRegexNode(1)))), "((0 | 1))*" },
        };
    }

}
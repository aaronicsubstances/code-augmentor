package com.aaronicsubstances.programmer.companion;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class ParserSupportTest {

    /**
     * Simple lexer that creates a token for each character.
     */
    private static class TestLexer implements Lexer {

        @Override
        public List<Token> next(ParserInputSource inputSource) {
            int lookahead = inputSource.lookahead(0);
            if (lookahead == -1) {
                return null;
            }
            int position = inputSource.getPosition();
            int lineNumber = inputSource.getLineNumber();
            int columnNumber = inputSource.getColumnNumber();
            inputSource.consume(1);
            Token token = new Token(1, "" + (char)lookahead, position, 
                inputSource.getPosition(), lineNumber, columnNumber);
            return Arrays.asList(token);
        }

        @Override
        public String getTokenName(int tokenType) {
            return String.valueOf(tokenType);
        }

        @Override
        public String describeToken(Token token) {
            return getTokenName(token.type);
        }
    }

    @BeforeClass
    public static void setUpClass() {
    }
    
    @Test(dataProvider = "createTestLookAheadData")
    public void testLookAhead(ParserSupport instance, int distance, Token expected) {
        Token actual = instance.lookAhead(distance);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestLookAheadData() {
        ParserInputSource inputSource = new ParserInputSource("abcdefg");
        ParserSupport instance = new ParserSupport(inputSource, new TestLexer());
        return new Object[][]{
            new Object[]{ instance, 5, new Token(1, "f", 5, 6, 1, 6)  },
            new Object[]{ instance, 0, new Token(1, "a", 0, 1, 1, 1)  },
            new Object[]{ instance, 6, new Token(1, "g", 6, 7, 1, 7)  },
            new Object[]{ instance, 7, new Token() },
            new Object[]{ instance, 20, new Token() },
            new Object[]{ instance, 1, new Token(1, "b", 1, 2, 1, 2)  },
            new Object[]{ instance, 0, new Token(1, "a", 0, 1, 1, 1)  },
        };
    }
    
    @Test(dataProvider = "createTestLookAheadWithoutLexerData")
    public void testLookAheadWithoutLexer(ParserSupport instance, int distance, Token expected,
            Function<ParserInputSource, List<Token>> lexerFunction) {
        Token actual = instance.lookAhead(distance, lexerFunction);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestLookAheadWithoutLexerData() {
        ParserInputSource inputSource = new ParserInputSource("abcdefg");
        ParserSupport instance = new ParserSupport(inputSource, null);
        Lexer lexer = new TestLexer();
        Function<ParserInputSource, List<Token>> lexerFunction = lexer::next;
        return new Object[][]{
            new Object[]{ instance, 5, new Token(1, "f", 5, 6, 1, 6), lexerFunction  },
            new Object[]{ instance, 0, new Token(1, "a", 0, 1, 1, 1), lexerFunction  },
            new Object[]{ instance, 6, new Token(1, "g", 6, 7, 1, 7), lexerFunction  },
            new Object[]{ instance, 7, new Token(), lexerFunction },
            new Object[]{ instance, 20, new Token(), lexerFunction },
            new Object[]{ instance, 1, new Token(1, "b", 1, 2, 1, 2), lexerFunction  },
            new Object[]{ instance, 0, new Token(1, "a", 0, 1, 1, 1), lexerFunction  },
        };
    }

    @Test(dataProvider = "createTestConsumeData", 
        dependsOnMethods = {"testLookAhead", "testLookAheadWithoutLexer"})
    public void testConsume(ParserSupport instance, Integer optionalParam, Token expected) {
        // Mandatory read before consume.
        instance.lookAhead(0);
        if (optionalParam == null) {
            instance.consume();
        }
        else {
            instance.consume(optionalParam);
        }
        Token actual = instance.lookAhead(0);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestConsumeData() {
        ParserInputSource inputSource = new ParserInputSource("abcdefg");
        ParserSupport instance = new ParserSupport(inputSource, new TestLexer());
        return new Object[][]{
            new Object[]{ instance, null, new Token(1, "b", 1, 2, 1, 2) },
            new Object[]{ instance, 1, new Token(1, "c", 2, 3, 1, 3) },
            new Object[]{ instance, null, new Token(1, "d", 3, 4, 1, 4) },
            new Object[]{ instance, 1, new Token(1, "e", 4, 5, 1, 5) },
            new Object[]{ instance, null, new Token(1, "f", 5, 6, 1, 6) },
            new Object[]{ instance, 1, new Token(1, "g", 6, 7, 1, 7) },
            new Object[]{ instance, null, new Token() },
            new Object[]{ instance, -1, new Token() },
        };
    }

    @Test(dataProvider = "createTestConsumeForMismatchData", 
        dependsOnMethods = {"testLookAhead", "testLookAheadWithoutLexer"},
        expectedExceptions = ParserException.class)
    public void testConsumeForMismatch(ParserSupport instance, int tokenType) {
        // Mandatory read before consume.
        instance.lookAhead(0);
        instance.consume(tokenType);
    }

    @DataProvider
    public Object[][] createTestConsumeForMismatchData() {
        ParserInputSource inputSource = new ParserInputSource("abcdefg");
        ParserSupport instance = new ParserSupport(inputSource, new TestLexer());
        return new Object[][]{
            new Object[]{ instance, -1 },
        };
    }

    @Test(dataProvider = "createTestMatchData",
        dependsOnMethods = { "testConsume", "testConsumeForMismatch" })
    public void testMatch(ParserSupport instance, int tokenType, Token expected) {
        // Mandatory lookahead before match.
        instance.lookAhead(0);
        Token actual = instance.match(tokenType);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestMatchData() {
        ParserInputSource inputSource = new ParserInputSource("abcdefg");
        ParserSupport instance = new ParserSupport(inputSource, new TestLexer());
        return new Object[][]{
            new Object[]{ instance, 1, new Token(1, "a", 0, 1, 1, 1) },
            new Object[]{ instance, 10, null },
            new Object[]{ instance, 1, new Token(1, "b", 1, 2, 1, 2) },
            new Object[]{ instance, 9, null },
            new Object[]{ instance, 1, new Token(1, "c", 2, 3, 1, 3) },
            new Object[]{ instance, 1, new Token(1, "d", 3, 4, 1, 4) },
            new Object[]{ instance, 1, new Token(1, "e", 4, 5, 1, 5) },            
            new Object[]{ instance, -1, null },
            new Object[]{ instance, 1, new Token(1, "f", 5, 6, 1, 6) },
            new Object[]{ instance, 1, new Token(1, "g", 6, 7, 1, 7) },
            new Object[]{ instance, 1, null },
            new Object[]{ instance, -1, new Token() },
        };
    }

    @Test(dataProvider = "createTestRewindToData",
        dependsOnMethods = { "testConsume", "testConsumeForMismatch" })
    public void testRewindTo(int consumptionCount, int position, Token expected) {
        ParserInputSource inputSource = new ParserInputSource("abcdefg");
        ParserSupport instance = new ParserSupport(inputSource, new TestLexer());
        for (int i = 0; i < consumptionCount; i++) {
            // Mandatory lookahead before consume.
            instance.lookAhead(0);
            instance.consume();
        }
        instance.rewindTo(position);
        Token actual = instance.lookAhead(0);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestRewindToData() {
        return new Object[][]{
            new Object[]{ 1, 0, new Token(1, "a", 0, 1, 1, 1) },
            new Object[]{ 4, 1, new Token(1, "b", 1, 2, 1, 2) },
            new Object[]{ 6, 2, new Token(1, "c", 2, 3, 1, 3) },
            new Object[]{ 10, 3, new Token(1, "d", 3, 4, 1, 4) },
        };
    }
}
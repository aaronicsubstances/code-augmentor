package com.aaronicsubstances.code.augmentor.core.parsing.peg;

import static org.testng.Assert.*;

import org.testng.annotations.*;

import java.util.Arrays;
import java.util.HashSet;

public class ParsingFailureTest {

    public static class ParsingFailureParser extends DefaultParser {

        public ParsingFailureParser(DefaultParsingContext ctx) {
            super(ctx);
        }

        String anyChar() {
            return Str("a") + AnyChar();
        }

        String string() {
            return Str("foo");
        }

        /*String matchChar() {*/
        char matchChar() {
            return Char(Character::isLetter, "letter");
        }

        //String charRange() {
        char charRange() {
            return CharRange('a', 'b');
        }

        String oneOrMoreChars() {
            return OneOrMoreChars(Character::isLetter, "identifier");
        }

        String atomic() {
            return AtomicRet("atomic", () -> Str("a") + Str("b"));
        }

        String expect() {
            return Expect("expect", () -> Str("a") + Str("b"));
        }
    }

    ParsingFailureParser parser;
    DefaultParsingContext ctx;

    @BeforeMethod
    public void setup() {
        ctx = new DefaultParsingContext("");
        parser = new ParsingFailureParser(ctx);
    }

    @Test
    public void anyChar() {
        expectFailure("a", parser::anyChar, 1, "any character");
    }

    @Test
    public void string() {
        expectFailure("fo", parser::string, 0, "\"foo\"");
        expectFailure("foa ", parser::string, 0, "\"foo\"");

        ctx.setContent("foo");
        assertEquals(parser.string(), "foo");

        ctx.setContent("foo bar");
        assertEquals(parser.string(), "foo");
    }

    @Test
    public void matchChar() {
        expectFailure("1", parser::matchChar, 0, "letter");
        expectFailure("", parser::matchChar, 0, "letter");
    }

    @Test
    public void charRange() {
        expectFailure("c", parser::charRange, 0, "character between 'a' and 'b'");
        expectFailure("", parser::charRange, 0, "character between 'a' and 'b'");
    }

    @Test
    public void oneOrMoreChars() {
        expectFailure("1", parser::oneOrMoreChars, 0, "identifier");
        expectFailure("", parser::oneOrMoreChars, 0, "identifier");
    }

    @Test
    public void atomic() {
        expectFailure("a", parser::atomic, 0, "atomic");
        expectFailure("", parser::atomic, 0, "atomic");
    }

    @Test
    public void expect() {
        expectFailure("a", parser::expect, 1, "expect");
        expectFailure("", parser::expect, 0, "expect");
    }

    private void expectFailure(String content, Runnable runnable, int failureIndex, String... expectations) {
        ctx.setContent(content);
        try {
            runnable.run();
            fail("Expected failure");
        } catch (NoMatchException e) {
            assertEquals(ctx.getExpectationFrame().index, failureIndex, "failure index");
            assertEquals(ctx.getExpectationFrame().expectations, new HashSet<String>(Arrays.asList(expectations)));
        }
    }
}

package com.aaronicsubstances.code.augmentor.core.parsing.peg;

import static org.testng.Assert.*;

import org.testng.annotations.*;

import java.util.function.Supplier;

public class PlusMinusTest {

    /**
     * A simple parser recognizing:
     * 
     * <pre>
     * input = sum EOI
     * sum = number ('+' number | '-' number)*
     * number = digit +
     * </pre>
     */
    public static class PlusMinusParser extends DefaultParser {

        public PlusMinusParser(DefaultParsingContext ctx) {
            super(ctx);
        }

        int input() {
            int result = sum();
            EOI();
            return result;
        }

        int sum() {
            int result = number();
            return ZeroOrMoreRet((Supplier<Integer>) () -> FirstOfRet(() -> {
                Str("+");
                return number();
            } , () -> {
                Str("-");
                return -number();
            })).stream().reduce(result, (a, b) -> a + b);
        }

        int number() {
            return Integer.parseInt(OneOrMoreChars(Character::isDigit, "number"));
        }
    }

    @Test
    public void singleNumber() {
        PlusMinusParser instance = new PlusMinusParser(new DefaultParsingContext("123"));
        assertEquals(instance.input(), 123);
    }

    @Test
    public void addition() {
        DefaultParsingContext ctx = new DefaultParsingContext("1+2");
        PlusMinusParser parser = new PlusMinusParser(ctx);
        int result = parser.input();

        assertEquals(result, 3);
    }

    @Test
    public void subtraction() {
        PlusMinusParser instance = new PlusMinusParser(new DefaultParsingContext("12-5"));
        assertEquals(instance.input(), 7);
    }

    @Test
    public void complex() {
        PlusMinusParser instance = new PlusMinusParser(new DefaultParsingContext("12-5+2"));
        assertEquals(instance.input(), 9);
    }
}

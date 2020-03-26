package com.aaronicsubstances.code.augmentor.parsing.peg;

import static org.testng.Assert.*;

import org.testng.annotations.*;

import static java.util.stream.Collectors.joining;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParserFactoryTest {

    /**
     * Grammar:
     * 
     * <pre>
     * Expression ← Term ((‘+’ / ‘-’) Term)*
     * Term       ← Factor ((‘*’ / ‘/’) Factor)*
     * Factor     ← Number / Parens
     * Number     ← Digit+
     * Parens     ← ‘(’ expression ‘)’
     * Digit      ← [0-9]
     * </pre>
     */
    private static class SimpleParser extends DefaultParser {

        public SimpleParser(DefaultParsingContext ctx) {
            super(ctx);
        }

        public String InputLine() {
            String result = Expression();
            EOI();
            return result;
        }

        String Expression() {
            return "(" + Term()
                    + ZeroOrMoreRet(() -> FirstOfRet(() -> Str("+", " plus "), () -> Str("-", " minus ")) + Term() + ")")
                            .stream().collect(Collectors.joining());
        }

        String Term() {
            return "(" + Factor()
                    + ZeroOrMoreRet(() -> FirstOfRet(() -> Str("*", " mul "), () -> Str("/", " div")) + Factor()).stream()
                            .collect(Collectors.joining())
                    + ")";
        }

        String Factor() {
            return FirstOfRet(this::Number, () -> Parens());
        }

        String Parens() {
            return Str("(", "(") + Expression() + Str(")", ")");
        }

        String Number() {
            //return OneOrMore(this::Digit).stream().collect(joining());
            return OneOrMoreRet(this::Digit).stream().map(String::valueOf).collect(joining());
        }

        //String Digit() {
        char Digit() {
            return Char(Character::isDigit, "digit");
        }
    }

    /**
     * Grammar:
     * 
     * <pre>
     * Expression ← Term ((‘+’ / ‘-’) Term)*
     * Term       ← Factor ((‘*’ / ‘/’) Factor)*
     * Factor     ← Number / Parens
     * Number     ← Digit+
     * Parens     ← ‘(’ expression ‘)’
     * Digit      ← [0-9]
     * </pre>
     */
    private static class EvaluatingParser extends DefaultParser {

        public EvaluatingParser(DefaultParsingContext ctx) {
            super(ctx);
        }

        public int InputLine() {
            int result = Expression();
            EOI();
            return result;
        }

        int Expression() {
            int left = Term();
            return FirstOfRet(() -> Str("+", () -> left + Term()), () -> Str("-", () -> left - Term()));
        }

        int Term() {
            int left = Factor();

            Collection<Function<Integer, Integer>> funcs = ZeroOrMoreRet(
                    () -> this.<Function<Integer, Integer>> FirstOfRet(() -> {
                        Str("*");
                        int right = Factor();
                        return x -> x * right;
                    } , () -> {
                        Str("/");
                        int right = Factor();
                        return x -> x / right;
                    }));

            int result = left;
            for (Function<Integer, Integer> f : funcs) {
                result = f.apply(result);
            }
            return result;
        }

        int Factor() {
            return FirstOfRet(this::Number, () -> Parens());
        }

        int Parens() {
            Str("(");
            int result = Expression();
            Str(")");
            return result;
        }

        int Number() {
            //return Integer.valueOf(OneOrMore(this::Digit).stream().collect(joining()));
            return Integer.valueOf(OneOrMoreRet(this::Digit).stream().map(String::valueOf).collect(joining()));
        }

        //String Digit() {
        char Digit() {
            return Char(Character::isDigit, "digit");
        }
    }

    static class RulesWithArgumentsParser extends DefaultParser {
        public RulesWithArgumentsParser(DefaultParsingContext ctx) {
            super(ctx);
        }

        void ruleWithArguments(int a, int b) {
            System.out.println("a: " + a + " b:" + b);
        }
    }
 
    static class InnerClassParser extends DefaultParser {
        public InnerClassParser(DefaultParsingContext ctx) {
            super(ctx);
        }

        private class InnerClass {
        }

        void rule() {
            new InnerClass();
            new Function<String, String>() {

                @Override
                public java.lang.String apply(java.lang.String t) {
                    return null;
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends ParsingContext<?>, T extends Parser<C>> T create(Class<T> cls, String input) {
        C ctx = (C)new DefaultParsingContext(input);
        T parser;
        try {
            Constructor<?> constructor = cls.getConstructor(ctx.getClass());
            try {
                parser = (T) constructor.newInstance(ctx);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Error while creating instance of parser " + cls, e);
            }
        } catch (IllegalArgumentException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("Error while instantiating parser " + cls, e);
        }
        new Tracer(ctx, System.out);
        return parser;
    }

    @Test
    public void simpleTest() {
        SimpleParser parser = create(SimpleParser.class, "2*3+1");
        assertEquals(parser.InputLine(), "((2 mul 3) plus (1))");
    }

    @Test
    public void simpleTestEvaluation() {
        EvaluatingParser parser = create(EvaluatingParser.class, "2*3+1");
        assertEquals(parser.InputLine(), 7);
    }

    @Test
    public void simpleTestEvaluationDiv() {
        EvaluatingParser parser = create(EvaluatingParser.class, "5*1/2+1");
        assertEquals(parser.InputLine(), 3);
    }

    @Test
    public void ruleWithArguments() {
        RulesWithArgumentsParser parser = create(RulesWithArgumentsParser.class, "1+2*3");
        parser.ruleWithArguments(1, 2);
    }

    @Test
    public void innerClass() {
        InnerClassParser parser = new InnerClassParser(new DefaultParsingContext(" b bb"));
        parser.rule();
    }

}

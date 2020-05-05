package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions.Expression;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PrattParserTest {

    @DataProvider
    public Object[][] createTestData() {
        return new Object[][]{
            // Function call.
            { "a()", "a()" },
            { "a(b)", "a(b)" },
            { "a(b, c)", "a(b, c)" },
            { "a(b)(c)", "a(b)(c)" },
            { "a(b) + c(d)", "(a(b) + c(d))" },
            { "a(b ? c : d, e + f)", "a((b ? c : d), (e + f))" },
            
            // Unary precedence.
            { "~!-+a", "(~(!(-(+a))))" },
            { "a!!!", "(((a!)!)!)" },
            
            // Unary and binary predecence.
            { "-a * b", "((-a) * b)" },
            { "!a + b", "((!a) + b)" },
            { "~a ^ b", "((~a) ^ b)" },
            { "-a!",    "(-(a!))" },
            { "!a!",    "(!(a!))" },
            
            // Binary precedence.
            { "a = b + c * d ^ e - f / g", "(a = ((b + (c * (d ^ e))) - (f / g)))" },
            
            // Binary associativity.
            { "a = b = c", "(a = (b = c))" },
            { "a + b - c", "((a + b) - c)" },
            { "a * b / c", "((a * b) / c)" },
            { "a ^ b ^ c", "(a ^ (b ^ c))" },
            
            // Conditional operator.
            { "a ? b : c ? d : e", "(a ? b : (c ? d : e))" },
            { "a ? b ? c : d : e", "(a ? (b ? c : d) : e)" },
            { "a + b ? c * d : e / f", "((a + b) ? (c * d) : (e / f))" },
            
            // Grouping.
            { "a + (b + c) + d", "((a + (b + c)) + d)" },
            { "a ^ (b + c)", "(a ^ (b + c))" },
            { "(!a)!",    "((!a)!)" }
        };
    }
  
    /**
     * Parses the given chunk of code and verifies that it matches the expected
     * pretty-printed result.
     */
    @Test(dataProvider = "createTestData")
    public void test(String source, String expected) {
        Lexer lexer = new Lexer(source);
        BantamParser parser = new BantamParser(lexer);
        Expression result = parser.parseExpression();
        StringBuilder builder = new StringBuilder();
        result.print(builder);
        String actual = builder.toString();
        assertEquals(actual, expected);
    }
}

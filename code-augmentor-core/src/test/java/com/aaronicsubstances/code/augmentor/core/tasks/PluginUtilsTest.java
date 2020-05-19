package com.aaronicsubstances.code.augmentor.core.tasks;

import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.LexerSupport;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PluginUtilsTest {

    @Test(dataProvider = "createTestStringifyPossibleScriptErrorsData")
    public void testStringifyPossibleScriptErrors(List<Throwable> exceptions,
            boolean stackTraceEnabled, List<String> limits, List<String> filters,
            int minCount, int maxCount) {
        String msg = PluginUtils.stringifyPossibleScriptErrors(
            exceptions, stackTraceEnabled, limits, filters);
        String[] lines = getLines(msg);
        System.out.println(msg);
        assertThat(lines.length, greaterThanOrEqualTo(minCount));
        assertThat(lines.length, lessThanOrEqualTo(maxCount));
    }

    @DataProvider
    public Object[][] createTestStringifyPossibleScriptErrorsData() {
        Throwable exception = new RuntimeException(
            "test with stack trace using integer limit");
        exception.fillInStackTrace();
        Throwable exception2 = new RuntimeException(
            "test with stack trace using integer limit and empty filter");
        exception2.fillInStackTrace();
        Throwable exception3 = new RuntimeException(
            "test with stack trace using integer limit and inner exception cause", exception2);
        exception3.fillInStackTrace();
        return new Object[][]{
            { Arrays.asList(), false, null, null , 2, 2 },
            { Arrays.asList(), true, null, null , 2, 2 },
            { Arrays.asList(new RuntimeException("test excluding stack trace")), 
              false, null, null , 3, 3 },
            { Arrays.asList(new RuntimeException("test including stack trace " +
                "using default filters")), 
              true, null, null , 3, 3 },
            { Arrays.asList(new RuntimeException("test with stack trace using " +
                "integer limit, when stack trace is absent")), 
              true, null, null , 3, 3 },
            { Arrays.asList(exception), true, Arrays.asList("x", "5"), Arrays.asList(), 3, 8 },
            { Arrays.asList(exception), true, Arrays.asList(getClass().getName()), Arrays.asList(), 3, 4 },
            { Arrays.asList(exception2), true, Arrays.asList("5"), Arrays.asList(""), 3, 4 },
            { Arrays.asList(exception3), true, Arrays.asList("5"), Arrays.asList(), 4, 14 },
            { Arrays.asList(exception, exception2), true, Arrays.asList("5"), null, 4, 14 },
            { Arrays.asList(exception, exception3), true, Arrays.asList("5"), null, 5, 20 }
        };
    }

    private static String[] getLines(String msg) {
        return LexerSupport.NEW_LINE_REGEX.split(msg.trim(), -1);
    }
}
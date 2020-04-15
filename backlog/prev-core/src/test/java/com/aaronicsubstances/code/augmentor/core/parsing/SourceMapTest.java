package com.aaronicsubstances.code.augmentor.core.parsing;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SourceMapTest {
    private static List<SourceMap.SubstringRange> testSrcRanges, testDestRanges;

    @BeforeClass
    public static void setUpClass() throws Exception {
        testSrcRanges = Arrays.asList(new SourceMap.SubstringRange(10, 13),
                new SourceMap.SubstringRange(17, 22));
        testDestRanges = Arrays.asList(new SourceMap.SubstringRange(10, 11),
                new SourceMap.SubstringRange(15, 16));
    }
    
    @Test(dataProvider = "createTestAppendData")
    public void testAppend(List<List<Object>> params,
            List<SourceMap.SubstringRange> expSrcRanges,
            List<SourceMap.SubstringRange> expDestRanges) {
        SourceMap instance = new SourceMap();
        for (List<Object> param : params) {
            int srcIndex = (int)param.get(0);
            int srcTokenLength;
            if (param.get(1) instanceof String) {
                srcTokenLength = ((String)param.get(1)).length();
            }
            else {
                srcTokenLength = (int)param.get(1);
            }
            int destTokenLength;
            if (param.get(2) instanceof String) {
                destTokenLength = ((String)param.get(2)).length();
            }
            else {
                destTokenLength = (int)param.get(2);
            }
            instance.append(srcIndex, srcTokenLength, destTokenLength);
        }
        assertEquals(instance.getSrcRanges(), expSrcRanges);
        assertEquals(instance.getDestRanges(), expDestRanges);
    }

    @DataProvider
    public Object[][] createTestAppendData() {
        return new Object[][]{
            { 
                Arrays.asList(
                        Arrays.asList(0, "God's good", "God's good" ),
                        Arrays.asList(10, "goo", "G" ),
                        Arrays.asList(13, "good", "good"),
                        Arrays.asList(17, "goods", "G"),
                        Arrays.asList(22, "yes", "yes"),
                        Arrays.asList(25, "true", "true") ),
                testSrcRanges,
                testDestRanges 
            }
        };
    }

    /**
     * Test of getSrcIndex method, of class SourceMap.
     */
    @Test(dataProvider = "createTestGetSrcIndexData")
    public void testGetSrcIndex(int destIndex, int expSrcIndex) {
        SourceMap instance = new SourceMap(testSrcRanges, testDestRanges);
        int result = instance.getSrcIndex(destIndex);
        assertEquals(result, expSrcIndex);
    }
    
    @DataProvider
    public Object[][] createTestGetSrcIndexData() {
        return new Object[][] {
            {0, 0}, {1, 1}, {2, 2}, {8, 8}, {9, 9}, {10, 10},
            {11, 13}, {12, 14}, {13, 15}, {14, 16}, {15, 17},
            {16, 22}, {17, 23}, {18, 24}, {19, 25}, {20, 26}
        };
    }
}
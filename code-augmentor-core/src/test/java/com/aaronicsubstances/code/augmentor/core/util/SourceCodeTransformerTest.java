package com.aaronicsubstances.code.augmentor.core.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.List;

public class SourceCodeTransformerTest {

    @Test(dataProvider = "createTestTransformerData")
    public void testTransformer(String originalText, String expected,
            List<TransformRequest> transforms) {
        SourceCodeTransformer instance = new SourceCodeTransformer(originalText);
        assertEquals(instance.getPositionAdjustment(), 0);
        for (TransformRequest transform : transforms) {
            String replacement = transform.replacement;
            int startPos = transform.startPos;
            if (transform.endPos != -1) {
                int endPos = transform.endPos;
                instance.addTransform(replacement, startPos, endPos);
            }
            else {
                instance.addTransform(replacement, startPos);
            }
            assertEquals(instance.getPositionAdjustment(), transform.totalDiff);
        }
        String actual = instance.getTransformedText();
        assertEquals(actual, expected);
    }
    
    @DataProvider
    public Object[][] createTestTransformerData() {
        return new Object[][]{
            new Object[]{ "", "", Arrays.asList() },
            new Object[]{ "pie", "pie", Arrays.asList(
                new TransformRequest(0, "", 0)) },
            new Object[]{ "pie", "", Arrays.asList(
                new TransformRequest(-3, "", 0, 3)) },
            new Object[]{ "", "pie", Arrays.asList(
                new TransformRequest(3, "pie", 0)) },
            new Object[]{ "I am going to school.", "She's going to school?!", Arrays.asList(
                new TransformRequest(2, "She", 0, 1), new TransformRequest(1, "'s", 1, 4),
                new TransformRequest(1, "?", 20, 21), new TransformRequest(2, "!", 21)
            ) }
        };
    }

    static class TransformRequest {
        final String replacement;
        final int startPos;
        final int endPos;
        final int totalDiff;

        TransformRequest(int totalDiff, String replacement, int startPos) {
            this(totalDiff, replacement, startPos, -1);
        }

        TransformRequest(int totalDiff, String replacement, int startPos, int endPos) {
            this.replacement = replacement;
            this.startPos = startPos;
            this.endPos = endPos;
            this.totalDiff = totalDiff;
        }
    }
}
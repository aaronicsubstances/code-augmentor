package com.aaronicsubstances.code.augmentor.core.util;

import java.util.Iterator;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.aaronicsubstances.code.augmentor.core.util.CodeGenerationResponseProcessorTest.TestArg;

public class CodeSimilarityTestDataProvider implements Iterator<Object[]> {
    private final int maxCount;
    private final List<String> inputs;
    
    private int inputPtr;
    private int count;

    public CodeSimilarityTestDataProvider(int maxCount, List<String> inputs) {
        this.maxCount = maxCount;
        this.inputs = inputs;
    }
    
    @Override
    public boolean hasNext() {
        return maxCount > 0 && inputPtr < inputs.size();
    }

    @Override
    public Object[] next() {
        String input = inputs.get(inputPtr);
        List<ContentPart> contentParts = TestResourceLoader.generateRandomContentPartList(input);
        // set test data before updating counters.
        Object[] testData = { inputPtr, new TestArg(input), new TestArg(contentParts) };
        count++;
        if (count >= maxCount) {
            inputPtr++;
            count = 0;
        }
        return testData;
    }
}
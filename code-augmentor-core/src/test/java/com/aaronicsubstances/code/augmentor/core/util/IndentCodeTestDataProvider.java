package com.aaronicsubstances.code.augmentor.core.util;

import java.util.Iterator;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestArg;
import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

public class IndentCodeTestDataProvider implements Iterator<Object[]> {
    private final int maxCount;
    private final List<String> inputs;
    private final List<String> indents;
    private final List<String> outputs;
    
    private int inputPtr;
    private int count;

    public IndentCodeTestDataProvider(int maxCount, List<String> inputs, 
            List<String> indents, List<String> outputs) {
        this.maxCount = maxCount;
        this.inputs = inputs;
        this.indents = indents;
        this.outputs = outputs;
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
        Object[] testData = { inputPtr, new TestArg<>(contentParts), indents.get(inputPtr), 
            new TestArg<>(outputs.get(inputPtr)) };
        count++;
        if (count >= maxCount) {
            inputPtr++;
            count = 0;
        }
        return testData;
    }
}
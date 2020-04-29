package com.aaronicsubstances.code.augmentor.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.aaronicsubstances.code.augmentor.core.util.CodeGenerationResponseProcessorTest.TestArg;

public class IndentCodeDataProvider implements Iterator<Object[]> {
    private static final Random randGen = new Random();

    private final int maxCount;
    private final List<String> inputs;
    private final List<String> indents;
    private final List<String> outputs;
    
    private int inputPtr;
    private int count;

    public IndentCodeDataProvider(int maxCount, List<String> inputs, 
            List<String> indents, List<String> outputs) {
        this.maxCount = maxCount;
        this.inputs = inputs;
        this.indents = indents;
        this.outputs = outputs;
    }
    
    @Override
    public boolean hasNext() {
        return inputPtr < inputs.size();
    }

    @Override
    public Object[] next() {
        String input = inputs.get(inputPtr);
        int maxContentPartSize = randGen.nextInt(30) + 1;
        List<ContentPart> contentParts = new ArrayList<>(maxContentPartSize);
        int start = 0;
        for (int i = 0; i < maxContentPartSize - 1; i++) {
            int randLen = 0;
            int remainderLen = input.length() - start;
            if (remainderLen > 0) {
                // generate from 0 up to and including input length.
                randLen = randGen.nextInt(remainderLen + 1);
            }
            int endIdx = start + randLen;
            if (randLen > 0) {
                // ensure we don't accidentally split a \r\n newline.
                if (input.charAt(endIdx - 1) == '\r') {
                    if (endIdx < input.length() && input.charAt(endIdx) == '\n') {
                        endIdx++;
                    }
                }
            }
            contentParts.add(new ContentPart(input.substring(start, endIdx), 
                randGen.nextBoolean()));
            start = endIdx;
        }
        if (contentParts.isEmpty() || start < input.length() || randGen.nextBoolean()) {
            contentParts.add(new ContentPart(input.substring(start), 
                randGen.nextBoolean()));
        }
        assert new GeneratedCode(contentParts).getWholeContent().equals(input);
        // set test data before updating counters.
        Object[] testData = { inputPtr, new TestArg(contentParts), indents.get(inputPtr), 
            new TestArg(outputs.get(inputPtr)) };
        count++;
        if (count >= maxCount) {
            inputPtr++;
            count = 0;
        }
        return testData;
    }
}
package com.aaronicsubstances.code.augmentor.core.tasks;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileGeneratedCode;

public class GeneratedCodeFetcherTest {

    @Test(dataProvider = "createTestGetGeneratedCodeData")
    public void testGetGeneratedCode(GeneratedCodeFetcher instance, 
            int fileIndex, int augCodeIndex,
            GeneratedCode expected) throws Exception {
        StringBuilder newlineReceiver = new StringBuilder();
        GeneratedCode actual = instance.getGeneratedCode(fileIndex, augCodeIndex, 
            newlineReceiver);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestGetGeneratedCodeData() {
        return new Iterator<Object[]>() {
            // per test invocation control fields.
            boolean isDone;
            int fileIndex;
            int perFileTestCount;

            // set up fields.
            List<List<Integer>> allFileAugCodeIndices = new ArrayList<>();
            Map<String, GeneratedCode> expectedResults = new HashMap<>();            
            GeneratedCodeFetcher instance = null;

            Random randGen = new Random();

            {
                // ensure at least 1 gen code file.
                int genFileCount = randGen.nextInt(5) + 1;
                List<CodeGenerationResponse> buckets = new ArrayList<>();
                for (int i = 0; i < genFileCount; i++) {
                    CodeGenerationResponse bucket = new CodeGenerationResponse(new ArrayList<>());
                    buckets.add(bucket);
                }

                int srcFileDescriptorCount = randGen.nextInt(20);
                int maxAugCodeCount = 10;

                // use this counter to check that implementation doesn't assume
                // start from 0 or increment by 1.
                int runningAugCodeIndex = 0;

                // randomly dump generated codes into buckets.

                for (int i = 0; i < srcFileDescriptorCount; i++) {
                    int fileIndex = i;
                    List<Integer> fileAugCodeIndices = new ArrayList<>();
                    allFileAugCodeIndices.add(fileAugCodeIndices);

                    int augCodeCount = randGen.nextInt(maxAugCodeCount);
                    for (int j = 0; j < augCodeCount; j++) {
                        int augCodeIndex = runningAugCodeIndex++ + (randGen.nextInt(2));
                        fileAugCodeIndices.add(augCodeIndex);

                        GeneratedCode dummyGenCode = new GeneratedCode();
                        dummyGenCode.setIndex(augCodeIndex);
                        dummyGenCode.setSkipped(true);

                        expectedResults.put(fileIndex + "-" + augCodeIndex, dummyGenCode);

                        // get a random CodeGenerationResponse bucket
                        // to dump gen code corresponding to aug code index.
                        int bucketIndex = randGen.nextInt(genFileCount);
                        CodeGenerationResponse codeGenRes = buckets.get(bucketIndex);
                        SourceFileGeneratedCode lastGenCodeWrapper = null;
                        if (!codeGenRes.getSourceFileGeneratedCodeList().isEmpty()) {
                            lastGenCodeWrapper = codeGenRes.getSourceFileGeneratedCodeList().get(
                                codeGenRes.getSourceFileGeneratedCodeList().size() - 1);
                            if (lastGenCodeWrapper.getFileIndex() != fileIndex) {
                                lastGenCodeWrapper = null; 
                            }
                        }
                        if (lastGenCodeWrapper == null) {
                            lastGenCodeWrapper = new SourceFileGeneratedCode(new ArrayList<>());
                            lastGenCodeWrapper.setFileIndex(fileIndex);
                            codeGenRes.getSourceFileGeneratedCodeList().add(lastGenCodeWrapper);
                        }
                        lastGenCodeWrapper.getGeneratedCodeList().add(dummyGenCode);
                    }
                }

                // serializing code generation responses...
                List<String> serializedBuckets = new ArrayList<>();
                for (CodeGenerationResponse bucket : buckets) {
                    StringWriter sw = new StringWriter();
                    try {
                        bucket.serialize(sw, true);
                        serializedBuckets.add(sw.toString());
                    }
                    catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }

                // initialize control fields.
                isDone = false;
                fileIndex = 0;
                perFileTestCount = 0;
                
                // finally create generated code fetcher instance
                try {
                    instance = new GeneratedCodeFetcher(serializedBuckets.toArray());
                    instance.prepareForFile(fileIndex);
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public boolean hasNext() {
                return !isDone;
            }

            @Override
            public Object[] next() {
                // these are the return results, together with fileIndex.                
                int augCodeIndex;
                GeneratedCode expected;

                // start by getting aug code indices for current file index.
                List<Integer> fileAugCodeIndices = allFileAugCodeIndices.get(fileIndex);

                // calculate how many times we may have generated miss result,
                // and hence determine whether we are in miss mode ... 
                int noResultCount = perFileTestCount - fileAugCodeIndices.size();
                if (noResultCount >= 0) {
                    expected = null;
                    // ensure augCodeIndex is invalid by adding
                    // arbitrary positive amount to maximum of
                    // valid aug code indices.
                    augCodeIndex = randGen.nextInt(30);
                    // max() will fail if collection is empty, hence the check.
                    if (!fileAugCodeIndices.isEmpty()) {
                        augCodeIndex += 1 + Collections.max(fileAugCodeIndices);
                    }
                    assertFalse(fileAugCodeIndices.contains(augCodeIndex));
                }
                else {
                    // ... if not then we are in find mode.
                    int randIndex = randGen.nextInt(fileAugCodeIndices.size());
                    augCodeIndex = fileAugCodeIndices.get(randIndex);
                    String key = fileIndex + "-" + augCodeIndex;
                    expected = expectedResults.get(key);
                    assertNotNull(expected);
                }

                // Create ret result before updating counters.
                Object[] retResult = { instance, fileIndex, augCodeIndex, expected };

                perFileTestCount++;
                
                // check whether we have exhausted all aug code indices per file way too
                // many times, and advance to next file if that's the case.
                int maxMissCount = 3;
                if (noResultCount > maxMissCount) {
                    // advance to next file and reset some counters.
                    fileIndex++;
                    perFileTestCount = 0;

                    // prepare for next file, or indicate that we are done.
                    if (fileIndex < allFileAugCodeIndices.size()) {
                        try {
                            instance.prepareForFile(fileIndex);
                        }
                        catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    else {
                        isDone = true;
                    }
                }

                return retResult;
            }
        };
    }
}
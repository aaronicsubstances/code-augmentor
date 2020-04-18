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

    @Test(dataProvider = "createTestGetGeneratedCodeData_0")
    public void testGetGeneratedCode_0(GeneratedCodeFetcher instance, 
            int fileIndex, int augCodeIndex,
            GeneratedCode expected) throws Exception {
        GeneratedCode actual = instance.getGeneratedCode(fileIndex, augCodeIndex);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestGetGeneratedCodeData_0() {
        return new GeneratedCodeFetcherTestDataProvider(0);
    }

    @Test(dataProvider = "createTestGetGeneratedCodeData_1")
    public void testGetGeneratedCode_1(GeneratedCodeFetcher instance, 
            int fileIndex, int augCodeIndex,
            GeneratedCode expected) throws Exception {
        GeneratedCode actual = instance.getGeneratedCode(fileIndex, augCodeIndex);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestGetGeneratedCodeData_1() {
        return new GeneratedCodeFetcherTestDataProvider(1);
    }

    @Test(dataProvider = "createTestGetGeneratedCodeData_2")
    public void testGetGeneratedCode_2(GeneratedCodeFetcher instance, 
            int fileIndex, int augCodeIndex,
            GeneratedCode expected) throws Exception {
        GeneratedCode actual = instance.getGeneratedCode(fileIndex, augCodeIndex);
        assertEquals(actual, expected);
    }

    @Test(dataProvider = "createTestGetGeneratedCodeData_5")
    public void testGetGeneratedCode_5(GeneratedCodeFetcher instance, 
            int fileIndex, int augCodeIndex,
            GeneratedCode expected) throws Exception {
        GeneratedCode actual = instance.getGeneratedCode(fileIndex, augCodeIndex);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestGetGeneratedCodeData_5() {
        return new GeneratedCodeFetcherTestDataProvider(5);
    }

    @DataProvider
    public Iterator<Object[]> createTestGetGeneratedCodeData_2() {
        return new GeneratedCodeFetcherTestDataProvider(2);
    }

    static class GeneratedCodeFetcherTestDataProvider implements Iterator<Object[]> {
        // per test invocation control fields.
        int overallTestCount;
        int indexIntoFileIndices;
        int perFileTestCount;

        // set up fields.
        List<Integer> fileIndices = new ArrayList<>();
        List<List<Integer>> allFileAugCodeIndices = new ArrayList<>();
        Map<String, GeneratedCode> expectedResults = new HashMap<>();            
        GeneratedCodeFetcher instance = null;

        Random randGen = new Random();
        
        static final int MAX_PER_FILE_AUG_CODE_COUNT = 3;
        static final int MAX_SRC_FILE_DESCRIPTOR_COUNT = 3;
        static final int MAX_PER_FILE_MISS_COUNT = 1;
        static final int TOTAL_OVERALL_TEST_COUNT = 10;
        
        public GeneratedCodeFetcherTestDataProvider(int genFileCount)
        {
            List<CodeGenerationResponse> buckets = new ArrayList<>();
            for (int i = 0; i < genFileCount; i++) {
                CodeGenerationResponse bucket = new CodeGenerationResponse(new ArrayList<>());
                buckets.add(bucket);
            }

            int srcFileDescriptorCount = 0;
            if (genFileCount > 0) {
                srcFileDescriptorCount = randGen.nextInt(MAX_SRC_FILE_DESCRIPTOR_COUNT);
            }

            // use this counter to check that implementation doesn't assume
            // fileIndex or augCodeIndex start from 0 or increment by 1.
            // Only requirement is both are unique and sorted in ascending order.
            int runningCounter = 0;

            // randomly dump generated codes into buckets.
            for (int i = 0; i < srcFileDescriptorCount; i++) {
                int fileIndex = runningCounter++ + (randGen.nextInt(2));
                fileIndices.add(fileIndex);

                List<Integer> fileAugCodeIndices = new ArrayList<>();
                allFileAugCodeIndices.add(fileAugCodeIndices);

                // ensure at least 1 aug code section.
                int augCodeCount = 1 + randGen.nextInt(MAX_PER_FILE_AUG_CODE_COUNT);
                for (int j = 0; j < augCodeCount; j++) {
                    int augCodeIndex = runningCounter++ + (randGen.nextInt(2));
                    fileAugCodeIndices.add(augCodeIndex);

                    GeneratedCode dummyGenCode = new GeneratedCode();
                    dummyGenCode.setIndex(augCodeIndex);
                    dummyGenCode.setSkipped(true);

                    expectedResults.put(fileIndex + "|" + augCodeIndex, dummyGenCode);

                    // get a random CodeGenerationResponse bucket
                    // to dump gen code corresponding to aug code index.
                    int bucketIndex = randGen.nextInt(genFileCount);
                    CodeGenerationResponse codeGenRes = buckets.get(bucketIndex);
                    SourceFileGeneratedCode lastGenCodeWrapper = null;
                    if (!codeGenRes.getSourceFileGeneratedCodes().isEmpty()) {
                        lastGenCodeWrapper = codeGenRes.getSourceFileGeneratedCodes().get(
                            codeGenRes.getSourceFileGeneratedCodes().size() - 1);
                        if (lastGenCodeWrapper.getFileIndex() != fileIndex) {
                            lastGenCodeWrapper = null; 
                        }
                    }
                    if (lastGenCodeWrapper == null) {
                        lastGenCodeWrapper = new SourceFileGeneratedCode(new ArrayList<>());
                        lastGenCodeWrapper.setFileIndex(fileIndex);
                        codeGenRes.getSourceFileGeneratedCodes().add(lastGenCodeWrapper);
                    }
                    lastGenCodeWrapper.getGeneratedCodes().add(dummyGenCode);
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
            indexIntoFileIndices = 0;
            perFileTestCount = 0;
            overallTestCount = 0;
            
            // finally create generated code fetcher instance
            int firstFileIndex = 0;
            boolean expectedFoundResult = false;
            if (!fileIndices.isEmpty()) {
                firstFileIndex = fileIndices.get(0);
                expectedFoundResult = true;
            }
            boolean actualFoundResult;
            try {
                instance = new GeneratedCodeFetcher(serializedBuckets.toArray());
                actualFoundResult = instance.prepareForFile(firstFileIndex);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            assertEquals(actualFoundResult, expectedFoundResult);
        }

        @Override
        public boolean hasNext() {
            return overallTestCount < TOTAL_OVERALL_TEST_COUNT;
        }

        @Override
        public Object[] next() {
            // set up return results. 
            int fileIdx;               
            int augCodeIndex;
            GeneratedCode expected;

            // start by getting aug code indices for current file index.
            List<Integer> fileAugCodeIndices = null;
            if (indexIntoFileIndices < fileIndices.size()) {
                fileIdx = fileIndices.get(indexIntoFileIndices);
                fileAugCodeIndices = allFileAugCodeIndices.get(indexIntoFileIndices);
            }
            else {
                fileIdx = randGen.nextInt();
            }

            // calculate how many times we may have generated miss result,
            // and hence determine whether we are in miss mode ... 
            int noResultCount = perFileTestCount;
            if (fileAugCodeIndices != null) {
                noResultCount -= fileAugCodeIndices.size();
            }

            if (noResultCount >= 0) {
                expected = null;
                // ensure augCodeIndex is invalid by adding
                // arbitrary positive amount to maximum of
                // valid aug code indices.
                augCodeIndex = randGen.nextInt(30);
                // max() will fail if collection is empty, hence the check.
                if (fileAugCodeIndices != null && !fileAugCodeIndices.isEmpty()) {
                    augCodeIndex += 1 + Collections.max(fileAugCodeIndices);
                    assertFalse(fileAugCodeIndices.contains(augCodeIndex));
                }
            }
            else {
                // ... if not then we are in find mode.
                int randIndex = randGen.nextInt(fileAugCodeIndices.size());
                augCodeIndex = fileAugCodeIndices.get(randIndex);
                String key = fileIdx + "|" + augCodeIndex;
                expected = expectedResults.get(key);
                assertNotNull(expected);
            }

            perFileTestCount++;
            overallTestCount++;
            
            // check whether we have exhausted all aug code indices per file way too
            // many times, and advance to next file if that's the case.
            if (noResultCount > MAX_PER_FILE_MISS_COUNT) {
                // advance to next file and reset some counters.
                // no need to advance already past fileIndices data.
                if (indexIntoFileIndices < fileIndices.size()) {
                    indexIntoFileIndices++;
                    perFileTestCount = 0;
                }
                
                // prepare for next file if we are not done.
                int nextFileIdx = fileIdx + 1; // shouldn't exist if fileIdx is the last one.
                boolean expectedFoundResult = false;
                if (indexIntoFileIndices < fileIndices.size()) {
                    nextFileIdx = fileIndices.get(indexIntoFileIndices);
                    expectedFoundResult = true;
                }
                boolean actualFoundResult;
                try {
                    actualFoundResult = instance.prepareForFile(nextFileIdx);
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                assertEquals(actualFoundResult, expectedFoundResult);
            }

            return new Object[]{ instance, fileIdx, augCodeIndex, expected };
        }
    }
}
package com.aaronicsubstances.code.augmentor.core.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.io.StringWriter;
import java.util.ArrayList;
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
            int fileId, int augCodeId,
            GeneratedCode expected) throws Exception {
        GeneratedCode actual = instance.getGeneratedCode(fileId, augCodeId);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestGetGeneratedCodeData_0() {
        return new GeneratedCodeFetcherTestDataProvider(0);
    }

    @Test(dataProvider = "createTestGetGeneratedCodeData_1")
    public void testGetGeneratedCode_1(GeneratedCodeFetcher instance, 
            int fileId, int augCodeId,
            GeneratedCode expected) throws Exception {
        GeneratedCode actual = instance.getGeneratedCode(fileId, augCodeId);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestGetGeneratedCodeData_1() {
        return new GeneratedCodeFetcherTestDataProvider(1);
    }

    @Test(dataProvider = "createTestGetGeneratedCodeData_2")
    public void testGetGeneratedCode_2(GeneratedCodeFetcher instance, 
            int fileId, int augCodeId,
            GeneratedCode expected) throws Exception {
        GeneratedCode actual = instance.getGeneratedCode(fileId, augCodeId);
        assertEquals(actual, expected);
    }

    @Test(dataProvider = "createTestGetGeneratedCodeData_5")
    public void testGetGeneratedCode_5(GeneratedCodeFetcher instance, 
            int fileId, int augCodeId,
            GeneratedCode expected) throws Exception {
        GeneratedCode actual = instance.getGeneratedCode(fileId, augCodeId);
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
        int indexIntoFileIds;
        int perFileTestCount;

        // set up fields.
        List<Integer> fileIds = new ArrayList<>();
        List<List<Integer>> allFileAugCodeIds = new ArrayList<>();
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
            // fileId or augCodeId start from 0 or increment by 1.
            // Only requirement is both are unique and file ids are sorted in ascending order.
            int runningCounter = 0;

            // randomly dump generated codes into buckets.
            for (int i = 0; i < srcFileDescriptorCount; i++) {
                int fileId = ++runningCounter + (randGen.nextInt(2));
                fileIds.add(fileId);

                List<Integer> fileAugCodeIds = new ArrayList<>();
                allFileAugCodeIds.add(fileAugCodeIds);

                // ensure at least 1 aug code section.
                int augCodeCount = 1 + randGen.nextInt(MAX_PER_FILE_AUG_CODE_COUNT);
                for (int j = 0; j < augCodeCount; j++) {
                    int augCodeId = ++runningCounter + (randGen.nextInt(2));
                    if (randGen.nextBoolean()) {
                        augCodeId = -augCodeId;
                    }
                    fileAugCodeIds.add(augCodeId);

                    GeneratedCode dummyGenCode = new GeneratedCode();
                    dummyGenCode.setId(augCodeId);
                    dummyGenCode.setSkipped(true);

                    expectedResults.put(fileId + "|" + augCodeId, dummyGenCode);

                    // get a random CodeGenerationResponse bucket
                    // to dump gen code corresponding to aug code id.
                    int bucketIndex = randGen.nextInt(genFileCount);
                    CodeGenerationResponse codeGenRes = buckets.get(bucketIndex);
                    SourceFileGeneratedCode lastGenCodeWrapper = null;
                    if (!codeGenRes.getSourceFileGeneratedCodes().isEmpty()) {
                        lastGenCodeWrapper = codeGenRes.getSourceFileGeneratedCodes().get(
                            codeGenRes.getSourceFileGeneratedCodes().size() - 1);
                        if (lastGenCodeWrapper.getFileId() != fileId) {
                            lastGenCodeWrapper = null; 
                        }
                    }
                    if (lastGenCodeWrapper == null) {
                        lastGenCodeWrapper = new SourceFileGeneratedCode(new ArrayList<>());
                        lastGenCodeWrapper.setFileId(fileId);
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
            indexIntoFileIds = 0;
            perFileTestCount = 0;
            overallTestCount = 0;
            
            // finally create generated code fetcher instance
            int firstFileId = 0;
            boolean expectedFoundResult = false;
            if (!fileIds.isEmpty()) {
                firstFileId = fileIds.get(0);
                expectedFoundResult = true;
            }
            boolean actualFoundResult;
            try {
                instance = new GeneratedCodeFetcher(serializedBuckets.toArray());
                actualFoundResult = instance.prepareForFile(firstFileId);
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
            int fileId;               
            int augCodeId;
            GeneratedCode expected;

            // start by getting aug code ids for current file id.
            List<Integer> fileAugCodeIds = null;
            if (indexIntoFileIds < fileIds.size()) {
                fileId = fileIds.get(indexIntoFileIds);
                fileAugCodeIds = allFileAugCodeIds.get(indexIntoFileIds);
            }
            else {
                fileId = randGen.nextInt();
            }

            // calculate how many times we may have generated miss result,
            // and hence determine whether we are in miss mode ... 
            int noResultCount = perFileTestCount;
            if (fileAugCodeIds != null) {
                noResultCount -= fileAugCodeIds.size();
            }

            if (noResultCount >= 0) {
                expected = null;
                // ensure augCodeId is invalid by adding
                // arbitrary positive amount to maximum of
                // valid aug code ids.
                augCodeId = randGen.nextInt(30);
                // max() will fail if collection is empty, hence the check.
                if (fileAugCodeIds != null && !fileAugCodeIds.isEmpty()) {
                    augCodeId += 1 + fileAugCodeIds.stream()
                        .map(x -> Math.abs(x))
                        .max(Integer::compareTo).get();
                    assertFalse(fileAugCodeIds.contains(augCodeId));
                }
            }
            else {
                // ... if not then we are in find mode.
                int randIndex = randGen.nextInt(fileAugCodeIds.size());
                augCodeId = fileAugCodeIds.get(randIndex);
                String key = fileId + "|" + augCodeId;
                expected = expectedResults.get(key);
                assertNotNull(expected);
            }

            perFileTestCount++;
            overallTestCount++;
            
            // check whether we have exhausted all aug code ids per file way too
            // many times, and advance to next file if that's the case.
            if (noResultCount > MAX_PER_FILE_MISS_COUNT) {
                // advance to next file and reset some counters.
                // no need to advance already past fileIds data.
                if (indexIntoFileIds < fileIds.size()) {
                    indexIntoFileIds++;
                    perFileTestCount = 0;
                }
                
                // prepare for next file if we are not done.
                int nextFileId = fileId + 1; // shouldn't exist if fileId is the last one.
                boolean expectedFoundResult = false;
                if (indexIntoFileIds < fileIds.size()) {
                    nextFileId = fileIds.get(indexIntoFileIds);
                    expectedFoundResult = true;
                }
                boolean actualFoundResult;
                try {
                    actualFoundResult = instance.prepareForFile(nextFileId);
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                assertEquals(actualFoundResult, expectedFoundResult);
            }

            return new Object[]{ instance, fileId, augCodeId, expected };
        }
    }
}
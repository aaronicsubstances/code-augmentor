package com.aaronicsubstances.code.augmentor.core.util;

import static org.testng.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeChangeSummary;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeChangeSummary.ChangedFile;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileGeneratedCode;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PersistenceTest {

    @Test(dataProvider = "createTestPreCodeAugmentationResultPersistenceData")
    public void testPreCodeAugmentationResultPersistence(int index, PreCodeAugmentationResult expected,
            boolean stream) throws Exception {
        // first, serialize
        StringWriter sw = new StringWriter();
        if (stream) {
            Object serializer = expected.beginSerialize(sw);
            for (SourceFileDescriptor file : expected.getFileDescriptors()) {
                file.serialize(serializer);
            }
            expected.endSerialize(serializer);
        }
        else {
            expected.serialize(sw);
        }
        String expectedOutput = sw.toString();
        //System.out.println(expectedOutput);
            
        // next, deserialize
        StringReader sr = new StringReader(expectedOutput);
        PreCodeAugmentationResult actual;
        if (stream) {
            actual = new PreCodeAugmentationResult();
            Object deserializer = actual.beginDeserialize(sr);
            SourceFileDescriptor file;
            while ((file = SourceFileDescriptor.deserialize(deserializer)) != null) {
                actual.getFileDescriptors().add(file);
            }
            actual.endDeserialize(deserializer);
        }
        else {
            actual = PreCodeAugmentationResult.deserialize(sr);
        }

        // finally, compare deserialized result with original
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestPreCodeAugmentationResultPersistenceData() {
        return new Iterator<Object[]>() {
            int count = 0;

            @Override
            public boolean hasNext() {
                return count < 10;
            }

            @Override
            public Object[] next() {
                PreCodeAugmentationResult instance = new PreCodeAugmentationResult(new ArrayList<>());
                instance.setEncoding(generateRandomString(false));
                instance.setGenCodeStartDirective(generateRandomString(false));
                instance.setGenCodeEndDirective(generateRandomString(false));
                if (count > 0) {
                    int fileDescriptorListSize = TestResourceLoader.RAND_GEN.nextInt(5);
                    for (int i = 0; i < fileDescriptorListSize; i++) {
                        SourceFileDescriptor s = new SourceFileDescriptor(new ArrayList<>());
                        instance.getFileDescriptors().add(s);

                        s.setFileId(i);
                        s.setRelativePath(generateRandomString(false));
                        s.setDir(generateRandomString(false));
                        s.setContentHash(generateRandomString(false));

                        int snippetListSize = TestResourceLoader.RAND_GEN.nextInt(5);
                        for (int j = 0; j < snippetListSize; j++) {
                            CodeSnippetDescriptor c = generateRandomCodeSnippetDescriptor();
                            s.getCodeSnippets().add(c);
                        }
                    }
                }
                return new Object[]{ count++, instance, TestResourceLoader.RAND_GEN.nextBoolean() };
            }
        };
    }

    @Test(dataProvider = "createTestCodeGenerationRequestPersistenceData")
    public void testCodeGenerationRequestPersistence(int index, CodeGenerationRequest expected,
            boolean serializeAllAsJson, boolean stream) throws Exception {
        // first, serialize
        StringWriter sw = new StringWriter();
        if (stream) {
            Object serializer = expected.beginSerialize(sw);
            for (SourceFileAugmentingCode augmentingCode : expected.getSourceFileAugmentingCodes()) {
                augmentingCode.serialize(serializer);
            }
            expected.endSerialize(serializer);
        }
        else {
            expected.serialize(sw, serializeAllAsJson);
        }
        String expectedOutput = sw.toString();
        //System.out.println(index + "\n" + expectedOutput);
            
        // next, deserialize
        StringReader sr = new StringReader(expectedOutput);
        CodeGenerationRequest actual;
        if (stream) {
            actual = new CodeGenerationRequest();
            Object deserializer = actual.beginDeserialize(sr);
            SourceFileAugmentingCode augmentingCode;
            while ((augmentingCode = SourceFileAugmentingCode.deserialize(deserializer)) != null) {
                actual.getSourceFileAugmentingCodes().add(augmentingCode);
            }
            actual.endDeserialize(deserializer);
        }
        else {
            actual = CodeGenerationRequest.deserialize(sr);
        }

        // finally, compare deserialized result with original
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestCodeGenerationRequestPersistenceData() {
        return new Iterator<Object[]>() {
            int count = 0;

            @Override
            public boolean hasNext() {
                return count < 10;
            }

            @Override
            public Object[] next() {
                List<SourceFileAugmentingCode> files = new ArrayList<>();
                CodeGenerationRequest instance = new CodeGenerationRequest(files);
                instance.getHeader().setGenCodeStartDirective(generateRandomString(false));
                instance.getHeader().setGenCodeEndDirective(generateRandomString(false));
                instance.getHeader().setSkipCodeStartDirective(generateRandomString(false));
                instance.getHeader().setSkipCodeEndDirective(generateRandomString(false));
                instance.getHeader().setEmbeddedStringDirective(generateRandomString(false));
                instance.getHeader().setEmbeddedJsonDirective(generateRandomString(false));
                instance.getHeader().setAugCodeDirective(generateRandomString(false));
                instance.getHeader().setInlineGenCodeDirective(generateRandomString(false));
                instance.getHeader().setNestedLevelStartMarker(generateRandomString(false));
                instance.getHeader().setNestedLevelEndMarker(generateRandomString(false));
                if (count > 0) {
                    int fileListSize = TestResourceLoader.RAND_GEN.nextInt(6);
                    for (int i = 0; i < fileListSize; i++) {
                        List<AugmentingCode> codeSnippets = new ArrayList<>();
                        SourceFileAugmentingCode fileAugCode = new SourceFileAugmentingCode(
                            codeSnippets);
                        files.add(fileAugCode);
                        fileAugCode.setFileId(i);
                        fileAugCode.setDir(generateRandomString(false));
                        fileAugCode.setRelativePath(generateRandomString(false));

                        int codeSnippetListSize = TestResourceLoader.RAND_GEN.nextInt(5);
                        for (int j = 0; j < codeSnippetListSize; j++) {
                            AugmentingCode codeSnippet = new AugmentingCode(new ArrayList<>());
                            codeSnippets.add(codeSnippet);
                            
                            codeSnippet.setId(i);
                            codeSnippet.setDirectiveMarker(generateRandomString(false));
                            codeSnippet.setIndent(randomIndent());
                            codeSnippet.setLineNumber(TestResourceLoader.RAND_GEN.nextInt());
                            codeSnippet.setNestedLevelNumber(TestResourceLoader.RAND_GEN.nextInt());
                            codeSnippet.setHasNestedLevelStartMarker(TestResourceLoader.RAND_GEN.nextBoolean());
                            codeSnippet.setHasNestedLevelEndMarker(TestResourceLoader.RAND_GEN.nextBoolean());
                            if (TestResourceLoader.RAND_GEN.nextBoolean()) {
                                codeSnippet.setExternalNestedContent(
                                    generateRandomString(true));
                                codeSnippet.setMatchingNestedLevelStartMarkerIndex(
                                    TestResourceLoader.RAND_GEN.nextInt());
                                codeSnippet.setMatchingNestedLevelEndMarkerIndex(
                                    TestResourceLoader.RAND_GEN.nextInt());
                            }
                            if (TestResourceLoader.RAND_GEN.nextBoolean()) {
                                codeSnippet.setGenCodeIndent(generateRandomString(false));
                            }
                            
                            int blockCount = TestResourceLoader.RAND_GEN.nextInt(5);
                            for (int k = 0; k < blockCount; k++) {
                                Block block = new Block();
                                codeSnippet.getBlocks().add(block);
                                block.setStringify(TestResourceLoader.RAND_GEN.nextBoolean());
                                block.setJsonify(TestResourceLoader.RAND_GEN.nextBoolean());
                                block.setContent(generateRandomString(true));
                            }
                        }
                    }
                }
                return new Object[]{ count++, instance, TestResourceLoader.RAND_GEN.nextBoolean(), 
                    TestResourceLoader.RAND_GEN.nextBoolean() };
            }
        };
    }

    @Test(dataProvider = "createTestCodeGenerationResponsePersistenceData")
    public void testCodeGenerationResponsePersistence(int index, CodeGenerationResponse expected,
            boolean serializeAllAsJson, boolean stream) throws Exception {
        // first, serialize
        StringWriter sw = new StringWriter();
        if (stream) {
            Object serializer = expected.beginSerialize(sw);
            for (SourceFileGeneratedCode generatedCode : expected.getSourceFileGeneratedCodes()) {
                generatedCode.serialize(serializer);
            }
            expected.endSerialize(serializer);
        }
        else {
            expected.serialize(sw, serializeAllAsJson);
        }
        String expectedOutput = sw.toString();
        //System.out.println(expectedOutput);
            
        // next, deserialize
        StringReader sr = new StringReader(expectedOutput);
        CodeGenerationResponse actual;
        if (stream) {
            actual = new CodeGenerationResponse();
            Object deserializer = actual.beginDeserialize(sr);
            SourceFileGeneratedCode generatedCode;
            while ((generatedCode = SourceFileGeneratedCode.deserialize(deserializer)) != null) {
                actual.getSourceFileGeneratedCodes().add(generatedCode);
            }
            actual.endDeserialize(deserializer);
        }
        else {
            actual = CodeGenerationResponse.deserialize(sr);
        }

        // finally, compare deserialized result with original
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestCodeGenerationResponsePersistenceData() {
        return new Iterator<Object[]>() {
            int count = 0;

            @Override
            public boolean hasNext() {
                return count < 10;
            }

            @Override
            public Object[] next() {
                List<SourceFileGeneratedCode> files = new ArrayList<>();
                CodeGenerationResponse instance = new CodeGenerationResponse(files);
                if (count > 0) {
                    int fileListSize = TestResourceLoader.RAND_GEN.nextInt(5);
                    for (int i = 0; i < fileListSize; i++) {
                        List<GeneratedCode> generatedCodeList = new ArrayList<>();
                        SourceFileGeneratedCode file = new SourceFileGeneratedCode(generatedCodeList);
                        files.add(file);
                        file.setFileId(i);
                        int generatedCodeListSize = TestResourceLoader.RAND_GEN.nextInt(5);
                        for (int j = 0; j < generatedCodeListSize; j++) {
                            GeneratedCode generatedCode = new GeneratedCode();
                            generatedCodeList.add(generatedCode);

                            generatedCode.setId(j);
                            generatedCode.setIndent(randomIndent());
                            generatedCode.setDisableEnsureEndingNewline(TestResourceLoader.RAND_GEN.nextBoolean());
                            generatedCode.setSkipped(TestResourceLoader.RAND_GEN.nextBoolean());
                            generatedCode.setReplaceAugCodeDirectives(TestResourceLoader.RAND_GEN.nextBoolean());
                            generatedCode.setReplaceGenCodeDirectives(TestResourceLoader.RAND_GEN.nextBoolean());

                            if (TestResourceLoader.RAND_GEN.nextBoolean()) {
                                generatedCode.setContentParts(new ArrayList<>());
                                int exactMatchRangeSize = TestResourceLoader.RAND_GEN.nextInt(6);
                                for (int k = 0; k < exactMatchRangeSize; k++) {
                                    String content = generateRandomString(true);
                                    ContentPart part = new ContentPart(content, 
                                        TestResourceLoader.RAND_GEN.nextBoolean());
                                    generatedCode.getContentParts().add(part);
                                }
                            }
                        }
                    }
                }
                return new Object[]{ count++, instance, TestResourceLoader.RAND_GEN.nextBoolean(), 
                    TestResourceLoader.RAND_GEN.nextBoolean() };
            }
        };
    }

    @Test(dataProvider = "createTestCodeChangeSummaryData")
    public void testCodeChangeSummary(int index, 
            CodeChangeSummary expected,
            boolean stream) throws Exception {
        // first, serialize
        StringWriter sw = new StringWriter();
        if (stream) {
            Object serializer = expected.beginSerialize(sw);
            for (ChangedFile cf : expected.getChangedFiles()) {
                cf.serialize(serializer);
            }
            expected.endSerialize(serializer);
        }
        else {
            expected.serialize(sw);
        }
        String expectedOutput = sw.toString();
        //System.out.println(expectedOutput);
            
        // next, deserialize
        StringReader sr = new StringReader(expectedOutput);
        CodeChangeSummary actual;
        if (stream) {
            actual = new CodeChangeSummary();
            Object deserializer = actual.beginDeserialize(sr);
            ChangedFile cf;
            while ((cf = ChangedFile.deserialize(deserializer)) != null) {
                actual.getChangedFiles().add(cf);
            }
            actual.endDeserialize(deserializer);
        }
        else {
            actual = CodeChangeSummary.deserialize(sr);
        }

        // finally, compare deserialized result with original
        assertEquals(actual, expected);
    }

    @DataProvider
    public Iterator<Object[]> createTestCodeChangeSummaryData() {
        return new Iterator<Object[]>() {
            int count = 0;

            @Override
            public boolean hasNext() {
                return count < 10;
            }

            @Override
            public Object[] next() {
                List<ChangedFile> files = new ArrayList<>();
                CodeChangeSummary instance = new CodeChangeSummary(files);
                if (count > 0) {
                    int fileListSize = TestResourceLoader.RAND_GEN.nextInt(5);
                    for (int i = 0; i < fileListSize; i++) {
                        ChangedFile cf = new ChangedFile(generateRandomString(false),
                            generateRandomString(false), generateRandomString(false));
                        files.add(cf);
                    }
                }
                return new Object[]{ count++, instance,
                    TestResourceLoader.RAND_GEN.nextBoolean() };
            }
        };
    }

    static CodeSnippetDescriptor generateRandomCodeSnippetDescriptor() {
        CodeSnippetDescriptor c = new CodeSnippetDescriptor();
        if (TestResourceLoader.RAND_GEN.nextBoolean()) {
            GeneratedCodeDescriptor g = new GeneratedCodeDescriptor();
            g.setStartDirectiveStartPos(TestResourceLoader.RAND_GEN.nextInt(1000));
            g.setStartDirectiveEndPos(TestResourceLoader.RAND_GEN.nextInt(1000));
            g.setEndDirectiveStartPos(TestResourceLoader.RAND_GEN.nextInt(1000));
            g.setEndDirectiveEndPos(TestResourceLoader.RAND_GEN.nextInt(1000));
            g.setInline(TestResourceLoader.RAND_GEN.nextBoolean());
            c.setGeneratedCodeDescriptor(g);
        }
        AugmentingCodeDescriptor d = new AugmentingCodeDescriptor();
        c.setAugmentingCodeDescriptor(d);
        d.setStartPos(TestResourceLoader.RAND_GEN.nextInt(1000));
        d.setEndPos(TestResourceLoader.RAND_GEN.nextInt(1000));
        d.setId(TestResourceLoader.RAND_GEN.nextInt(200));
        d.setIndent(randomIndent());
        d.setLineNumber(TestResourceLoader.RAND_GEN.nextInt());
        return c;
    }
    
    static String randomIndent() {
        if (TestResourceLoader.RAND_GEN.nextBoolean()) {
            if (TestResourceLoader.RAND_GEN.nextBoolean()) {
                int tabCount = TestResourceLoader.RAND_GEN.nextInt(4);
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < tabCount; i++) {
                    s.append("\t");
                }
                return s.toString();
            }
            else {
                return generateRandomString(false);
            }
        }
        return null;
    }

    static String generateRandomString(boolean includeNewLine) {
        int length = TestResourceLoader.RAND_GEN.nextInt(50);
        StringBuilder s = new StringBuilder();
        String chars = "x x x x x x  xxxxxxxxxxxxxxxxxx" + (includeNewLine ? "\n\n\n" : "");
        for (int i = 0; i < length; i++) {
            int randIndex = TestResourceLoader.RAND_GEN.nextInt(chars.length());
            s.append(chars.charAt(randIndex));
        }
        return s.toString();
    }
}
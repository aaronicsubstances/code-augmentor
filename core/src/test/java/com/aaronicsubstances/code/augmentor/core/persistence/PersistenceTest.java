package com.aaronicsubstances.code.augmentor.core.persistence;

import static org.testng.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
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
            Random randGen = new Random();

            @Override
            public boolean hasNext() {
                return count < 10;
            }

            @Override
            public Object[] next() {
                PreCodeAugmentationResult instance = new PreCodeAugmentationResult(new ArrayList<>());
                instance.setGenCodeStartDirective(generateRandomString(randGen, false));
                instance.setGenCodeEndDirective(generateRandomString(randGen, false));
                if (count > 0) {
                    int fileDescriptorListSize = randGen.nextInt(5);
                    for (int i = 0; i < fileDescriptorListSize; i++) {
                        SourceFileDescriptor s = new SourceFileDescriptor(new ArrayList<>());
                        instance.getFileDescriptors().add(s);

                        s.setFileIndex(i);
                        s.setRelativePath(generateRandomString(randGen, false));
                        s.setDir(generateRandomString(randGen, false));
                        s.setContentHash(generateRandomString(randGen, false));

                        int snippetListSize = randGen.nextInt(5);
                        for (int j = 0; j < snippetListSize; j++) {
                            CodeSnippetDescriptor c = generateRandomCodeSnippetDescriptor(randGen);
                            s.getBodySnippets().add(c);
                        }
                    }
                }
                return new Object[]{ count++, instance, randGen.nextBoolean() };
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
            for (SourceFileAugmentingCode augmentingCode : expected.getSourceFileAugmentingCodeList()) {
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
                actual.getSourceFileAugmentingCodeList().add(augmentingCode);
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
            Random randGen = new Random();

            @Override
            public boolean hasNext() {
                return count < 10;
            }

            @Override
            public Object[] next() {
                List<SourceFileAugmentingCode> files = new ArrayList<>();
                CodeGenerationRequest instance = new CodeGenerationRequest(files);
                if (count > 0) {
                    int fileListSize = randGen.nextInt(6);
                    for (int i = 0; i < fileListSize; i++) {
                        List<AugmentingCode> codeSnippets = new ArrayList<>();
                        SourceFileAugmentingCode fileAugCode = new SourceFileAugmentingCode(
                            codeSnippets);
                        files.add(fileAugCode);
                        fileAugCode.setFileIndex(i);
                        fileAugCode.setRelativePath(generateRandomString(randGen, false));

                        int codeSnippetListSize = randGen.nextInt(5);
                        for (int j = 0; j < codeSnippetListSize; j++) {
                            AugmentingCode codeSnippet = new AugmentingCode(new ArrayList<>());
                            codeSnippets.add(codeSnippet);
                            
                            codeSnippet.setIndex(i);
                            codeSnippet.setCommentSuffix(generateRandomString(randGen, false));
                            codeSnippet.setIndent(randomIndent(randGen));
                            
                            int blockCount = randGen.nextInt(5);
                            for (int k = 0; k < blockCount; k++) {
                                Block block = new Block();
                                codeSnippet.getBlocks().add(block);
                                block.setStringify(randGen.nextBoolean());
                                block.setContent(generateRandomString(randGen, true));
                            }
                        }
                    }
                }
                return new Object[]{ count++, instance, randGen.nextBoolean(), randGen.nextBoolean() };
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
            for (SourceFileGeneratedCode generatedCode : expected.getSourceFileGeneratedCodeList()) {
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
                actual.getSourceFileGeneratedCodeList().add(generatedCode);
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
            Random randGen = new Random();

            @Override
            public boolean hasNext() {
                return count < 10;
            }

            @Override
            public Object[] next() {
                List<SourceFileGeneratedCode> files = new ArrayList<>();
                CodeGenerationResponse instance = new CodeGenerationResponse(files);
                if (randGen.nextBoolean()) {
                    instance.setNewline(generateRandomString(randGen, true));
                }
                if (count > 0) {
                    int fileListSize = randGen.nextInt(5);
                    for (int i = 0; i < fileListSize; i++) {
                        List<GeneratedCode> generatedCodeList = new ArrayList<>();
                        SourceFileGeneratedCode file = new SourceFileGeneratedCode(generatedCodeList);
                        files.add(file);
                        file.setFileIndex(i);
                        if (randGen.nextBoolean()) {
                            file.setNewline(generateRandomString(randGen, true));
                        }
                        int generatedCodeListSize = randGen.nextInt(5);
                        for (int j = 0; j < generatedCodeListSize; j++) {
                            GeneratedCode generatedCode = new GeneratedCode();
                            generatedCodeList.add(generatedCode);

                            generatedCode.setIndex(j);
                            generatedCode.setIndent(randomIndent(randGen));
                            if (randGen.nextBoolean()) {
                                generatedCode.setError(true);
                            }
                            generatedCode.setBodyContent(generateRandomString(randGen, true));
                        }
                    }
                }
                return new Object[]{ count++, instance, randGen.nextBoolean(), randGen.nextBoolean() };
            }
        };
    }

    static CodeSnippetDescriptor generateRandomCodeSnippetDescriptor(Random randGen) {
        CodeSnippetDescriptor c = new CodeSnippetDescriptor();
        if (randGen.nextBoolean()) {
            GeneratedCodeDescriptor g = new GeneratedCodeDescriptor();
            g.setStartPos(randGen.nextInt(1000));
            g.setEndPos(randGen.nextInt(1000));
            c.setGeneratedCodeDescriptor(g);
        }
        AugmentingCodeDescriptor d = new AugmentingCodeDescriptor();
        c.setAugmentingCodeDescriptor(d);
        d.setStartPos(randGen.nextInt(1000));
        d.setEndPos(randGen.nextInt(1000));
        d.setHasNewlineEnding(randGen.nextBoolean());
        d.setIndex(randGen.nextInt(200));
        d.setIndent(randomIndent(randGen));
        return c;
    }
    
    static String randomIndent(Random randGen) {
        if (randGen.nextBoolean()) {
            if (randGen.nextBoolean()) {
                int tabCount = randGen.nextInt(4);
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < tabCount; i++) {
                    s.append("\t");
                }
                return s.toString();
            }
            else {
                return generateRandomString(randGen, false);
            }
        }
        return null;
    }

    static String generateRandomString(Random randGen, boolean includeNewLine) {
        int length = randGen.nextInt(50);
        StringBuilder s = new StringBuilder();
        String chars = "x x x x x x  xxxxxxxxxxxxxxxxxx" + (includeNewLine ? "\n\n\n" : "");
        for (int i = 0; i < length; i++) {
            int randIndex = randGen.nextInt(chars.length());
            s.append(chars.charAt(randIndex));
        }
        return s.toString();
    }
}
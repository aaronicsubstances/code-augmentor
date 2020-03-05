package com.aaronicsubstances.programmer.companion.ant.plugin.persistence;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeGenerationRequest;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeGenerationResponse;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.GeneratedCode;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.PreCodeAugmentationResult;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.SourceFileDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode.Block;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;

public class PersistenceTest {

    @Test(dataProvider = "createTestPreCodeAugmentationResultPersistenceData")
    public void testPreCodeAugmentationResultPersistence(int index, PreCodeAugmentationResult expected)
            throws Exception {
        assertNotNull(expected);
        
        // first, serialize
        StringWriter writer = new StringWriter();
        Object serializer = expected.beginSerialize(writer);
        for (SourceFileDescriptor fileDescriptor : expected.getFileDescriptors()) {
            fileDescriptor.beginSerialize(serializer);
            for (CodeSnippetDescriptor codeSnippetDescriptor : fileDescriptor.getBodySnippets()) {
                codeSnippetDescriptor.serialize(serializer);
            }
            fileDescriptor.endSerialize(serializer);
        }
        expected.endSerialize(serializer);
        String serialized = writer.toString();
        System.out.println(serialized);

        // next, deserialize 
        PreCodeAugmentationResult actual = new PreCodeAugmentationResult();
        Object deserializer = actual.beginDeserialize(new StringReader(serialized));
        SourceFileDescriptor s = new SourceFileDescriptor();
        while (s.beginDeserialize(deserializer)) {
            actual.getFileDescriptors().add(s);

            CodeSnippetDescriptor codeSnippetDescriptor = new CodeSnippetDescriptor();
            while (codeSnippetDescriptor.deserialize(deserializer)) {
                s.getBodySnippets().add(codeSnippetDescriptor);
                codeSnippetDescriptor = new CodeSnippetDescriptor();
            }

            s.endDeserialize(deserializer);
            s = new SourceFileDescriptor();
        }
        actual.endDeserialize(deserializer);

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
                instance.setCodeSnippetBlockEndDoubleSlash(generateRandomString(randGen, false));
                instance.setCodeSnippetBlockEndSlashStar(generateRandomString(randGen, false));
                instance.setCodeSnippetBlockStartDoubleSlash(generateRandomString(randGen, false));
                instance.setCodeSnippetBlockStartSlashStar(generateRandomString(randGen, false));
                if (count > 0) {
                    int fileDescriptorListSize = randGen.nextInt(5);
                    for (int i = 0; i < fileDescriptorListSize; i++) {
                        SourceFileDescriptor s = new SourceFileDescriptor(new ArrayList<>());
                        instance.getFileDescriptors().add(s);

                        s.setRelativePath(generateRandomString(randGen, false));
                        s.setDir(generateRandomString(randGen, false));
                        if (randGen.nextBoolean()) {
                            s.setHeaderSnippet(generateRandomCodeSnippetDescriptor(randGen));
                        }
                        int snippetListSize = randGen.nextInt(5);
                        for (int j = 0; j < snippetListSize; j++) {
                            CodeSnippetDescriptor c = generateRandomCodeSnippetDescriptor(randGen);
                            s.getBodySnippets().add(c);
                        }
                    }
                }
                return new Object[]{ count++, instance };
            }
        };
    }

	@Test(dataProvider = "createTestCodeGenerationRequestPersistenceData")
    public void testCodeGenerationRequestPersistence(int index, CodeGenerationRequest expected)
            throws Exception {
        assertNotNull(expected);
        
        // first, serialize
        StringWriter writer = new StringWriter();
        Object serializer = expected.beginSerialize(writer);
        for (AugmentingCode augCodeSnippet : expected.getAugmentingCodeSnippets()) {
            augCodeSnippet.serialize(serializer);
        }
        expected.endSerialize(serializer);
        String serialized = writer.toString();
        System.out.println(serialized);

        // next, deserialize 
        CodeGenerationRequest actual = new CodeGenerationRequest();
        Object deserializer = actual.beginDeserialize(new StringReader(serialized));
        AugmentingCode augCodeSnippet = new AugmentingCode();
        while (augCodeSnippet.deserialize(deserializer)) {
            actual.getAugmentingCodeSnippets().add(augCodeSnippet);
            augCodeSnippet = new AugmentingCode();
        }
        actual.endDeserialize(deserializer);

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
                List<AugmentingCode> codeSnippets = new ArrayList<>();
                CodeGenerationRequest instance = new CodeGenerationRequest(codeSnippets);
                if (count > 0) {
                    int codeSnippetListSize = randGen.nextInt(5);
                    for (int i = 0; i < codeSnippetListSize; i++) {
                        AugmentingCode codeSnippet = new AugmentingCode(new ArrayList<>());
                        codeSnippets.add(codeSnippet);

                        codeSnippet.setIndex(randGen.nextInt(30));
                        codeSnippet.setIndexInFile(randGen.nextInt(30));
                        codeSnippet.setRelativePath(generateRandomString(randGen, false));
                        
                        // ensure at least 1 block.
                        int blockCount = randGen.nextInt(5) + 1;
                        for (int j = 0; j < blockCount; j++) {
                            Block block = new Block();
                            codeSnippet.getBlocks().add(block);
                            block.setStringify(randGen.nextBoolean());
                            block.setContent(generateRandomString(randGen, true));
                        }
                    }
                }
                return new Object[]{ count++, instance };
            }
        };
    }

    @Test(dataProvider = "createTestCodeGenerationResponsePersistenceData")
    public void testCodeGenerationResponsePersistence(int index, CodeGenerationResponse expected) throws Exception {
        assertNotNull(expected);
        
        // first, serialize
        StringWriter writer = new StringWriter();
        Object serializer = expected.beginSerialize(writer);
        for (GeneratedCode generatedCode : expected.getGeneratedCodeSnippets()) {
            generatedCode.serialize(serializer);
        }
        expected.endSerialize(serializer);
        String serialized = writer.toString();
        System.out.println(serialized);

        // next, deserialize 
        CodeGenerationResponse actual = new CodeGenerationResponse();
        Object deserializer = actual.beginDeserialize(new StringReader(serialized));
        GeneratedCode generatedCode = new GeneratedCode();
        while (generatedCode.deserialize(deserializer)) {
            actual.getGeneratedCodeSnippets().add(generatedCode);
            generatedCode = new GeneratedCode();
        }
        actual.endDeserialize(deserializer);

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
                List<GeneratedCode> generatedCodeList = new ArrayList<>();
                CodeGenerationResponse instance = new CodeGenerationResponse(generatedCodeList);
                if (count > 0) {
                    int generatedCodeListSize = randGen.nextInt(5);
                    for (int i = 0; i < generatedCodeListSize; i++) {
                        GeneratedCode generatedCode = new GeneratedCode();
                        generatedCodeList.add(generatedCode);

                        generatedCode.setIndex(randGen.nextInt(30));
                        generatedCode.setIndexInFile(randGen.nextInt(30));
                        generatedCode.setError(randGen.nextBoolean());
                        
                        generatedCode.setRelativePath(generateRandomString(randGen, false));
                        if (randGen.nextBoolean()) {
                            generatedCode.setHeaderContent(generateRandomString(randGen, true));
                        }
                        generatedCode.setBodyContent(generateRandomString(randGen, true));
                    }
                }
                return new Object[]{ count++, instance };
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
        d.setIndexInFile(randGen.nextInt(1000));
        d.setAnnotatedWithSlashStar(randGen.nextBoolean());
        d.setIndex(randGen.nextInt(200));
        d.setIndent(generateRandomString(randGen, false));
        return c;
	}

    static String generateRandomString(Random randGen, boolean includeNewLine) {
        // ensure at least one string char,
        // since null is indistinguishable from empty string
        // in modified CSV format.
        int length = randGen.nextInt(50) + 1;
        StringBuilder s = new StringBuilder();
        String chars = "x x x x x x  xxxxxxxxxxxxxxxxxx" + (includeNewLine ? "\n\n\n" : "");
        for (int i = 0; i < length; i++) {
            int randIndex = randGen.nextInt(chars.length());
            s.append(chars.charAt(randIndex));
        }
        return s.toString();
    }
}
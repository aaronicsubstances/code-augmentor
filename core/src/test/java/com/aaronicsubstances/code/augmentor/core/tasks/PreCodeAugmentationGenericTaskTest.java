package com.aaronicsubstances.code.augmentor.core.tasks;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PreCodeAugmentationGenericTaskTest {

    public static class TaskLite {
        public String[] relativePaths;
        public String[] genCodeStartDirectives, genCodeEndDirectives;
        public String[] embeddedStringDirectives, embeddedJsonDirectives;
        public AugCodeSpec[] augCodeDirectives;

        public String prepFile;

        public static class AugCodeSpec {
            public String file;
            public String[] directives;
        }
    }
    
    public static PreCodeAugmentationGenericTask deserialize(String path) throws IOException {
        String text = TestResourceLoader.loadResource(path, PreCodeAugmentationGenericTaskTest.class);
        Gson gson = new Gson();
        TaskLite taskSpec = gson.fromJson(text, TaskLite.class);
        
        PreCodeAugmentationGenericTask task = new PreCodeAugmentationGenericTask();
        task.setCharset(StandardCharsets.UTF_8);
        task.setRelativePaths(new ArrayList<>());
        task.setBaseDirs(new ArrayList<>());

        // copy files to temp dir.
        File tempDir = FileUtils.getTempDirectory();
        for (String relativePath : taskSpec.relativePaths) {
            String contents = TestResourceLoader.loadResourceNewlinesNormalized(relativePath, 
                PreCodeAugmentationGenericTaskTest.class, "\r\n");
            FileUtils.write(new File(tempDir, relativePath), contents, task.getCharset());
            task.getRelativePaths().add(relativePath);
            task.getBaseDirs().add(tempDir);
        }
        
        task.setPrepFile(new File(tempDir, taskSpec.prepFile));

        task.setAugCodeProcessingSpecs(Arrays.asList(taskSpec.augCodeDirectives).stream()
            .map(x -> new AugCodeProcessingSpec(new File(tempDir, x.file),
                Arrays.asList(x.directives)))
            .collect(Collectors.toList()));

        task.setGenCodeStartDirectives(Arrays.asList(taskSpec.genCodeStartDirectives));
        task.setGenCodeEndDirectives(Arrays.asList(taskSpec.genCodeEndDirectives));
        task.setEmbeddedStringDirectives(Arrays.asList(
            taskSpec.embeddedStringDirectives));
        task.setEmbeddedJsonDirectives(Arrays.asList(
            taskSpec.embeddedJsonDirectives));

        return task;
    }

    //@Test(dataProvider = "createTestExecuteData")
    public void testExecute(String jsonPath) throws Exception {
        PreCodeAugmentationGenericTask task = deserialize(jsonPath);
        String expectedPrepFileContents = TestResourceLoader.loadResource(
            jsonPath.replace(".json", "-expected-prep.json"), getClass());
        PreCodeAugmentationResult expResult = PreCodeAugmentationResult.deserialize(
            new StringReader(expectedPrepFileContents));
        List<CodeGenerationRequest> expectedRequests = new ArrayList<>(); 
        for (int i = 0; i < task.getAugCodeProcessingSpecs().size(); i++) {
            String reqPath = jsonPath.replace(".json",
                "-expected-request-" + i + ".json");
            String contents = TestResourceLoader.loadResource(
                reqPath, getClass());
            CodeGenerationRequest exp = CodeGenerationRequest.deserialize(new StringReader(contents));
            expectedRequests.add(exp);
        }
        task.execute();
        if (!task.getAllErrors().isEmpty()) {
            fail("Unexpected errors: " + task.getAllErrors());
        }
        PreCodeAugmentationResult actualResult = PreCodeAugmentationResult.deserialize(
            task.getPrepFile());
        // to enable easier testing, don't require dir (we won't know temp dir on every
        // host dev't machine), and content hash
        for (SourceFileDescriptor f : actualResult.getFileDescriptors()) {
            f.setDir(null);
            f.setContentHash(null);
        }
        assertEquals(actualResult, expResult, 
            "Unexpected contents found in " + task.getPrepFile());
        for (int i = 0; i < task.getAugCodeProcessingSpecs().size(); i++) {
            File f = task.getAugCodeProcessingSpecs().get(i).getDestFile();
            CodeGenerationRequest expected = expectedRequests.get(i);
            CodeGenerationRequest actual = CodeGenerationRequest.deserialize(f);
            assertEquals(actual, expected, "Unexpected contents found in " + f);
        }
    }

    @DataProvider
    public Object[][] createTestExecuteData() {
        return new Object[][]{
            new Object[] { "task-spec-00.json" },
            new Object[] { "task-spec-01.json" },
            new Object[] { "task-spec-02.json" }
        };
    }
}
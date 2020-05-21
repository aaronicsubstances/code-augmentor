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
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
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
        public String[] skipCodeStartDirectives, skipCodeEndDirectives;
        public AugCodeSpec[] augCodeDirectives;
        public String[] inlineGenCodeDirectives;
        public String[] nestedLevelStartMarkers;
        public String[] nestedLevelEndMarkers;

        public String prepFile;

        public boolean loggingEnabled;

        public static class AugCodeSpec {
            public String file;
            public String[] directives;
        }
    }

    File tempDir = new File(FileUtils.getTempDirectory(), getClass().getName());
    
    public PreCodeAugmentationGenericTask deserialize(String path, String newline) throws IOException {
        String text = TestResourceLoader.loadResource(path, getClass());
        Gson gson = new Gson();
        TaskLite taskSpec = gson.fromJson(text, TaskLite.class);
        
        PreCodeAugmentationGenericTask task = new PreCodeAugmentationGenericTask();
        task.setCharset(StandardCharsets.UTF_8);
        task.setRelativePaths(new ArrayList<>());
        task.setBaseDirs(new ArrayList<>());

        // copy files to temp dir.
        tempDir.mkdir();
        for (String relativePath : taskSpec.relativePaths) {
            String contents = TestResourceLoader.loadResourceNewlinesNormalized(relativePath, 
                PreCodeAugmentationGenericTaskTest.class, newline);
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
        task.setEmbeddedJsonDirectives(Arrays.asList(taskSpec.embeddedJsonDirectives));
        if (taskSpec.skipCodeStartDirectives != null) {
            task.setSkipCodeStartDirectives(Arrays.asList(taskSpec.skipCodeStartDirectives));
        }
        if (taskSpec.skipCodeEndDirectives != null) {
            task.setSkipCodeEndDirectives(Arrays.asList(taskSpec.skipCodeEndDirectives));
        }
        if (taskSpec.inlineGenCodeDirectives != null) {
            task.setInlineGenCodeDirectives(Arrays.asList(taskSpec.inlineGenCodeDirectives));
        }
        if (taskSpec.nestedLevelStartMarkers != null) {
            task.setNestedLevelStartMarkers(Arrays.asList(taskSpec.nestedLevelStartMarkers));
        }
        if (taskSpec.nestedLevelEndMarkers != null) {
            task.setNestedLevelEndMarkers(Arrays.asList(taskSpec.nestedLevelEndMarkers));
        }

        if (taskSpec.loggingEnabled) {
            task.setLogAppender((level, msgSrc) -> 
                System.out.format("[%s] %s\n", level, msgSrc.get()));
        }

        return task;
    }

    @Test(dataProvider = "createTestExecuteData")
    public void testExecute(String jsonPath, String newline) throws Exception {
        PreCodeAugmentationGenericTask task = deserialize(jsonPath, newline);
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
        // host dev't machine)
        for (SourceFileDescriptor f : actualResult.getFileDescriptors()) {
            assertEquals(f.getDir(), tempDir.getPath());
            f.setDir(null);
        }
        assertEquals(actualResult, expResult, 
            "Unexpected contents found in " + task.getPrepFile());
        for (int i = 0; i < task.getAugCodeProcessingSpecs().size(); i++) {
            File f = task.getAugCodeProcessingSpecs().get(i).getDestFile();
            CodeGenerationRequest expected = expectedRequests.get(i);
            CodeGenerationRequest actual = CodeGenerationRequest.deserialize(f);
            // to enable easier testing, don't require dir (we won't know temp dir on every
            // host dev't machine)
            for (SourceFileAugmentingCode a : actual.getSourceFileAugmentingCodes()) {
                assertEquals(a.getDir(), tempDir.getPath());
                a.setDir(null);
            }
            assertEquals(actual, expected, "Unexpected contents found in " + f);
        }
    }

    @DataProvider
    public Object[][] createTestExecuteData() {
        return new Object[][]{
            new Object[] { "task-spec-00.json", "\r\n" },
            new Object[] { "task-spec-01.json", "\r\n" },
            new Object[] { "task-spec-02.json", "\r\n" },
            new Object[] { "task-spec-03.json", "\n" },
            new Object[] { "task-spec-04.json", "\n" }
        };
    }
}
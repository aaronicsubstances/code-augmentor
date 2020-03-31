package com.aaronicsubstances.code.augmentor.tasks;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.models.SourceFileDescriptor;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PreCodeAugmentationGenericTaskTest {

    public static class TaskLite {
        public String[] relativePaths;
        public String[] genCodeStartSuffixes, genCodeEndSuffixes, embeddedStringDoubleSlashSuffixes;
        public AugCodeSpec[] augCodeSuffixes;

        public String parseResultsFile;

        public static class AugCodeSpec {
            public String file;
            public String[] suffixes;
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
        
        task.setParseResultsFile(new File(tempDir, taskSpec.parseResultsFile));

        task.setAugCodeDestFiles(Arrays.asList(taskSpec.augCodeSuffixes).stream().
            map(x -> new File(tempDir, x.file)).collect(Collectors.toList()));
        task.setAugCodeSuffixes(Arrays.asList(taskSpec.augCodeSuffixes).stream().
            map(x -> Arrays.asList(x.suffixes)).collect(Collectors.toList()));

        task.setGenCodeStartSuffixes(Arrays.asList(taskSpec.genCodeStartSuffixes));
        task.setGenCodeEndSuffixes(Arrays.asList(taskSpec.genCodeEndSuffixes));
        task.setEmbeddedStringDoubleSlashSuffixes(Arrays.asList(
            taskSpec.embeddedStringDoubleSlashSuffixes));

        task.setTempDir(tempDir);

        return task;
    }

    @Test(dataProvider = "createTestExecuteData")
    public void testExecute(String jsonPath) throws Exception {
        PreCodeAugmentationGenericTask task = deserialize(jsonPath);
        String expectedPrepFileContents = TestResourceLoader.loadResource(
            jsonPath.replace(".json", "-expected-prep.json"), getClass());
        PreCodeAugmentationResult expResult = PreCodeAugmentationResult.deserialize(
            expectedPrepFileContents);
        List<CodeGenerationRequest> expectedRequests = new ArrayList<>(); 
        for (int i = 0; i < task.getAugCodeDestFiles().size(); i++) {
            String reqPath = jsonPath.replace(".json",
                "-expected-request-" + i + ".json");
            String contents = TestResourceLoader.loadResource(
                reqPath, getClass());
            CodeGenerationRequest exp = CodeGenerationRequest.deserialize(contents);
            expectedRequests.add(exp);
        }
        task.execute();
        if (!task.getAllErrors().isEmpty()) {
            fail("Unexpected errors: " + task.getAllErrors());
        }
        String actualPrepFileContents = FileUtils.readFileToString(task.getParseResultsFile(),
            task.getCharset());
        PreCodeAugmentationResult actualResult = PreCodeAugmentationResult.deserialize(
            actualPrepFileContents);
        // to enable easier testing, don't require dir (we won't know temp dir on every
        // host dev't machine), and content hash
        for (SourceFileDescriptor f : actualResult.getFileDescriptors()) {
            f.setDir(null);
            f.setContentHash(null);
        }
        assertEquals(actualResult, expResult, 
            "Unexpected contents found in " + task.getParseResultsFile());
        for (int i = 0; i < task.getAugCodeDestFiles().size(); i++) {
            File f = task.getAugCodeDestFiles().get(i);
            CodeGenerationRequest expected = expectedRequests.get(i);
            CodeGenerationRequest actual = CodeGenerationRequest.deserialize(f);
            assertEquals(actual, expected, "Unexpected contents found in " + f);
        }
    }

    @DataProvider
    public Object[][] createTestExecuteData() {
        return new Object[][]{
            new Object[] { "task-spec-00.json" }
        };
    }

    @Test
    public void testExecuteForErrors() {
        
    }    
}
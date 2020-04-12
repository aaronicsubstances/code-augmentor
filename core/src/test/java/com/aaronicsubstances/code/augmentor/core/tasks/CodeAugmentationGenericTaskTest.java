package com.aaronicsubstances.code.augmentor.core.tasks;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CodeAugmentationGenericTaskTest {

    public static class TaskLite {
        public String prepFile;
        public String[] generatedCodeFiles;
        public String destDir;
        public boolean loggingEnabled;
    }
    
    public static CodeAugmentationGenericTask deserialize(String path) throws IOException {
        String text = TestResourceLoader.loadResource(path, CodeAugmentationGenericTaskTest.class);
        Gson gson = new Gson();
        TaskLite taskSpec = gson.fromJson(text, TaskLite.class);
        
        CodeAugmentationGenericTask task = new CodeAugmentationGenericTask();
        task.setCharset(StandardCharsets.UTF_8);

        File TEMP_GEN_DIR = new File(FileUtils.getTempDirectory(), "code-augmentor-generated");
        TEMP_GEN_DIR.mkdir();
        task.setSrcDir(TEMP_GEN_DIR);

        // copy prep file to temp dir.
        String contents = TestResourceLoader.loadResource(taskSpec.prepFile, 
            CodeAugmentationGenericTaskTest.class);
        File f = new File(TEMP_GEN_DIR, taskSpec.prepFile);
        FileUtils.write(f, contents, task.getCharset());        
        task.setPrepFile(f);

        // copy generated code files to temp dir.
        task.setGeneratedCodeFiles(new ArrayList<>());
        for (String genCodePath : taskSpec.generatedCodeFiles) {            
            contents = TestResourceLoader.loadResourceNewlinesNormalized(genCodePath, 
                CodeAugmentationGenericTaskTest.class, "\r\n");
            f = new File(TEMP_GEN_DIR, genCodePath);
            FileUtils.write(f, contents, task.getCharset());
            task.getGeneratedCodeFiles().add(f);
        }

        if (taskSpec.destDir != null) {
            task.setDestDir(new File(FileUtils.getTempDirectory(), taskSpec.destDir));
            task.getDestDir().mkdir();
        }

        if (taskSpec.loggingEnabled) {
            task.setLogAppender((level, msgSrc) -> 
                System.out.format("[%s] %s\n", level, msgSrc.get()));
        }
        
        return task;
    }

    @Test(dataProvider = "createTestExecuteData")
    public void testExecute(String jsonPath, List<Integer> expectedChangeSetIndices) throws Exception {
        CodeAugmentationGenericTask task = deserialize(jsonPath);
        PreCodeAugmentationResult prepResult = PreCodeAugmentationResult.deserialize(
            task.getPrepFile());
        List<String> expectedGeneratedCodes = new ArrayList<>();
        // transfer class path resources for original source files to
        // temp directory.
        for (SourceFileDescriptor f : prepResult.getFileDescriptors()) {
            assertNull(f.getDir());
            String contents = TestResourceLoader.loadResourceNewlinesNormalized(
                f.getRelativePath(), getClass(), "\r\n");
            FileUtils.write(new File(task.getSrcDir(), f.getRelativePath()), contents, task.getCharset());

            // fetch expected generated codes.
            if (expectedChangeSetIndices.contains(f.getFileIndex())) {
                String baseName = FilenameUtils.getBaseName(f.getRelativePath());
                String ext = FilenameUtils.getExtension(f.getRelativePath());
                String expGenGode = TestResourceLoader.loadResourceNewlinesNormalized(
                    baseName + "-expected." + ext, getClass(), "\r\n");
                expectedGeneratedCodes.add(expGenGode);
            }
        }
        task.execute();
        int actualChangeSetSize = task.getSrcFiles().size();
        assertEquals(actualChangeSetSize, expectedChangeSetIndices.size());
        for (int i = 0; i < actualChangeSetSize; i++) {
            String expected = expectedGeneratedCodes.get(i);
            File f = task.getDestFiles().get(i);
            String actual = FileUtils.readFileToString(f, task.getCharset());            
            assertEquals(actual, expected, "Unexpected contents found in " + f);
        }
    }

    @DataProvider
    public Object[][] createTestExecuteData() {
        return new Object[][]{
            new Object[] { "task-spec-00.json", Arrays.asList() },
            new Object[] { "task-spec-01.json", Arrays.asList(0, 1) },
            new Object[] { "task-spec-02.json", Arrays.asList(0) }
        };
    }
}
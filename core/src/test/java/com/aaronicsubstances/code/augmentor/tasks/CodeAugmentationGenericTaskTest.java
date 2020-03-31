package com.aaronicsubstances.code.augmentor.tasks;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.models.SourceFileDescriptor;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CodeAugmentationGenericTaskTest {

    public static class TaskLite {
        public String prepfile;
        public String[] generatedCodeFiles;
        public boolean generate;
        public String destDir;
    }
    
    public static CodeAugmentationGenericTask deserialize(String path) throws IOException {
        String text = TestResourceLoader.loadResource(path, CodeAugmentationGenericTaskTest.class);
        Gson gson = new Gson();
        TaskLite taskSpec = gson.fromJson(text, TaskLite.class);
        
        CodeAugmentationGenericTask task = new CodeAugmentationGenericTask();
        task.setCharset(StandardCharsets.UTF_8);
        task.setNewline("\r\n");
        task.setGenerate(taskSpec.generate);

        File TEMP_GEN_DIR = new File(FileUtils.getTempDirectory(), "code-augmentor-generated");
        TEMP_GEN_DIR.mkdir();
        task.setTempDir(TEMP_GEN_DIR);

        // copy prep file to temp dir.
        String contents = TestResourceLoader.loadResource(taskSpec.prepfile, 
            CodeAugmentationGenericTaskTest.class);
        File f = new File(TEMP_GEN_DIR, taskSpec.prepfile);
        FileUtils.write(f, contents, task.getCharset());        
        task.setPrepfile(f);

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
            task.setDestdir(new File(TEMP_GEN_DIR, taskSpec.destDir));
            task.getDestdir().mkdir();
        }

        task.setTempDir(TEMP_GEN_DIR);

        return task;
    }

    @Test(dataProvider = "createTestExecuteData")
    public void testExecute(String jsonPath, boolean expectedUpToDate) throws Exception {
        CodeAugmentationGenericTask task = deserialize(jsonPath);
        PreCodeAugmentationResult prepResult = PreCodeAugmentationResult.deserialize(
            task.getPrepfile());
        List<String> expectedGeneratedCodes = new ArrayList<>();
        // transfer class path resources for original source files to
        // temp directory.
        for (SourceFileDescriptor f : prepResult.getFileDescriptors()) {
            assertNull(f.getDir());
            String contents = TestResourceLoader.loadResourceNewlinesNormalized(
                f.getRelativePath(), getClass(), "\r\n");
            FileUtils.write(new File(task.getTempDir(), f.getRelativePath()), contents, task.getCharset());

            // fetch expected generated codes.
            if (task.isGenerate()) {
                String baseName = FilenameUtils.getBaseName(f.getRelativePath());
                String ext = FilenameUtils.getExtension(f.getRelativePath());
                String expGenGode = TestResourceLoader.loadResourceNewlinesNormalized(
                    baseName + "-expected." + ext, getClass(), "\r\n");
                expectedGeneratedCodes.add(expGenGode);
            }
        }
        task.execute();
        assertEquals(task.isUpToDate(), expectedUpToDate);
        if (task.isGenerate()) {
            for (int i = 0; i < prepResult.getFileDescriptors().size(); i++) {
                String expected = expectedGeneratedCodes.get(i);
                SourceFileDescriptor s = prepResult.getFileDescriptors().get(i);
                File f = new File(task.getDestdir(), s.getRelativePath());
                String actual = FileUtils.readFileToString(f, task.getCharset());            
                assertEquals(actual, expected, "Unexpected contents found in " + f);
            }
        }
    }

    @DataProvider
    public Object[][] createTestExecuteData() {
        return new Object[][]{
            new Object[] { "task-spec-00.json", true },
            new Object[] { "task-spec-00-1.json", true }
        };
    }

    @Test
    public void testExecuteForErrors() {
        
    }    
}
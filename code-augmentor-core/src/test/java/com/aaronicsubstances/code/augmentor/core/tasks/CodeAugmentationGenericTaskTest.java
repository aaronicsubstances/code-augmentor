package com.aaronicsubstances.code.augmentor.core.tasks;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.CodeChangeSummary;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponseChangeSet;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileChangeSet;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeChangeSummary.ChangedFile;
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

    private final File TEMP_GEN_DIR;
    
    public CodeAugmentationGenericTaskTest() {
        TEMP_GEN_DIR = new File(FileUtils.getTempDirectory(), getClass().getName());
        TEMP_GEN_DIR.mkdir();
    }
    
    public CodeAugmentationGenericTask deserialize(String path) throws IOException {
        String text = TestResourceLoader.loadResource(path, getClass());
        Gson gson = new Gson();
        TaskLite taskSpec = gson.fromJson(text, TaskLite.class);
        
        CodeAugmentationGenericTask task = new CodeAugmentationGenericTask();
        task.setCharset(StandardCharsets.UTF_8);

        // copy prep file to temp dir.
        String contents = TestResourceLoader.loadResource(taskSpec.prepFile, 
            getClass());
        // replace any %TEMP_DIR% variable with temp dir
        contents = transformWithVars(contents);
        File f = new File(TEMP_GEN_DIR, taskSpec.prepFile);
        FileUtils.write(f, contents, task.getCharset());        
        task.setPrepFile(f);

        // copy generated code files to temp dir.
        task.setGeneratedCodeFiles(new ArrayList<>());
        for (String genCodePath : taskSpec.generatedCodeFiles) {            
            contents = TestResourceLoader.loadResourceNewlinesNormalized(genCodePath, 
                getClass(), "\r\n");
            f = new File(TEMP_GEN_DIR, genCodePath);
            FileUtils.write(f, contents, task.getCharset());
            task.getGeneratedCodeFiles().add(f);
        }

        if (taskSpec.destDir != null) {
            task.setDestDir(new File(TEMP_GEN_DIR, taskSpec.destDir));
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
        CodeGenerationResponseChangeSet expectedChanges = loadChanges(jsonPath);
        List<String> expectedGeneratedCodes = new ArrayList<>();
        // transfer class path resources for original source files to
        // temp directory.
        for (int i = 0; i < prepResult.getFileDescriptors().size(); i++) {
            SourceFileDescriptor f = prepResult.getFileDescriptors().get(i);
            assertNotNull(f.getDir());
            String contents = TestResourceLoader.loadResourceNewlinesNormalized(
                f.getRelativePath(), getClass(), "\r\n");
            FileUtils.write(new File(f.getDir(), f.getRelativePath()), contents, task.getCharset());

            // fetch expected generated codes.
            if (expectedChangeSetIndices.contains(i)) {
                String baseName = FilenameUtils.getBaseName(f.getRelativePath());
                String ext = FilenameUtils.getExtension(f.getRelativePath());
                String expGenGode = TestResourceLoader.loadResourceNewlinesNormalized(
                    baseName + "-expected." + ext, getClass(), "\r\n");
                expectedGeneratedCodes.add(expGenGode);
            }
        }
        task.execute();
        assertEquals(0, task.getAllErrors().size());
        assertEquals(task.isCodeChangeDetected(), !expectedChangeSetIndices.isEmpty());
        CodeChangeSummary changeSummary = CodeChangeSummary.deserialize(
            task.getChangeSummaryFile());
        int actualChangeSetSize = changeSummary.getChangedFiles().size();
        assertEquals(actualChangeSetSize, expectedChangeSetIndices.size());
        for (int i = 0; i < actualChangeSetSize; i++) {
            String expected = expectedGeneratedCodes.get(i);
            ChangedFile cf = changeSummary.getChangedFiles().get(i);
            File f = new File(cf.getDestDir(), cf.getRelativePath());
            String actual = FileUtils.readFileToString(f, task.getCharset());            
            assertEquals(actual, expected, "Unexpected contents found in " + f);
        }
        CodeGenerationResponseChangeSet actualChanges = CodeGenerationResponseChangeSet.deserialize(
            task.getChangeDetailsFile());
        assertEquals(actualChanges, expectedChanges);
        CodeChangeSummary expectedChangeSummary = new CodeChangeSummary(new ArrayList<>());
        for (SourceFileChangeSet s : actualChanges.getSourceFileChangeSets()) {
            ChangedFile cf = new ChangedFile(s.getRelativePath(), s.getSrcDir(), s.getDestDir());
            expectedChangeSummary.getChangedFiles().add(cf);
        }
        CodeChangeSummary actualChangeSummary = CodeChangeSummary.deserialize(
            task.getChangeSummaryFile());
        assertEquals(actualChangeSummary, expectedChangeSummary);
    }

    @DataProvider
    public Object[][] createTestExecuteData() {
        return new Object[][]{
            new Object[] { "task-spec-00.json", Arrays.asList() },
            new Object[] { "task-spec-01.json", Arrays.asList(0, 1) },
            new Object[] { "task-spec-02.json", Arrays.asList(0) }
        };
    }

    private CodeGenerationResponseChangeSet loadChanges(String jsonPath) throws Exception {
        String changesPath = jsonPath.replace(".json", "-CHANGES-expected.json");
        String contents = TestResourceLoader.loadResource(changesPath, getClass());
        contents = transformWithVars(contents);
        CodeGenerationResponseChangeSet c = CodeGenerationResponseChangeSet.deserialize(
            new StringReader(contents));
        return c;
    }

    private String transformWithVars(String contents) {
        // replace any %TEMP_DIR% variable with temp dir
        contents = contents.replace("%LINE_SEPARATOR%", File.separator
            .replace("\\", "\\\\"));
        contents = contents.replace("%TEMP_DIR%", TEMP_GEN_DIR.getPath()
            .replace("\\", "\\\\"));
        return contents;
    }
}
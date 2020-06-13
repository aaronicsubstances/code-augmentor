package com.aaronicsubstances.code.augmentor.core.tasks;

import static org.testng.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
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
        public boolean codeChangeDetectionDisabled;
    }

    private final File TEMP_GEN_DIR;
    
    public CodeAugmentationGenericTaskTest() {
        TEMP_GEN_DIR = new File(FileUtils.getTempDirectory(), getClass().getName());
        TEMP_GEN_DIR.mkdir();
    }

    private TaskLite startDeserialize(String path) throws IOException {
        String text = TestResourceLoader.loadResource(path, getClass());
        Gson gson = new Gson();
        TaskLite taskSpec = gson.fromJson(text, TaskLite.class);
        return taskSpec;
    }

    private File continueDeserialize(TaskLite taskSpec) throws IOException {
        // copy prep file to temp dir.
        String contents = TestResourceLoader.loadResource(taskSpec.prepFile, 
            getClass());
        // replace any %TEMP_DIR% variable with temp dir
        contents = transformWithVars(contents);
        File f = new File(TEMP_GEN_DIR, taskSpec.prepFile);
        FileUtils.write(f, contents, StandardCharsets.UTF_8);
        return f;
    }
    
    private CodeAugmentationGenericTask completeDeserialize(TaskLite taskSpec, 
            File prepFile, Charset taskCharset,
            String newline) throws IOException {
        CodeAugmentationGenericTask task = new CodeAugmentationGenericTask();
        task.setCodeChangeDetectionDisabled(taskSpec.codeChangeDetectionDisabled);
        
        task.setPrepFile(prepFile);

        String contents;
        File f;
        // copy generated code files to temp dir.
        task.setGeneratedCodeFiles(new ArrayList<>());
        for (String genCodePath : taskSpec.generatedCodeFiles) {            
            contents = TestResourceLoader.loadResourceNewlinesNormalized(genCodePath, 
                getClass(), newline);
            f = new File(TEMP_GEN_DIR, genCodePath);
            FileUtils.write(f, contents, taskCharset);
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
    public void testExecute(String jsonPath, String newline, List<Integer> expectedChangeSetIndices) throws Exception {
        TaskLite taskSpec = startDeserialize(jsonPath);
        File prepFile = continueDeserialize(taskSpec);
        PreCodeAugmentationResult prepResult = PreCodeAugmentationResult.deserialize(
            prepFile);
        final Charset taskCharset = Charset.forName(prepResult.getEncoding());
        CodeAugmentationGenericTask task = completeDeserialize(taskSpec, prepFile, 
            taskCharset, newline);
        CodeGenerationResponseChangeSet expectedChanges = null;
        if (!task.isCodeChangeDetectionDisabled()) {
            expectedChanges = loadChanges(jsonPath);
        }
        List<String> expectedGeneratedCodes = new ArrayList<>();
        // transfer class path resources for original source files to
        // temp directory.
        for (int i = 0; i < prepResult.getFileDescriptors().size(); i++) {
            SourceFileDescriptor f = prepResult.getFileDescriptors().get(i);
            assertNotNull(f.getDir());
            String contents = TestResourceLoader.loadResourceNewlinesNormalized(
                f.getRelativePath(), getClass(), newline);
            FileUtils.write(new File(f.getDir(), f.getRelativePath()), contents, taskCharset);

            // fetch expected generated codes.
            if (expectedChangeSetIndices.contains(i)) {
                String baseName = FilenameUtils.getBaseName(f.getRelativePath());
                String ext = FilenameUtils.getExtension(f.getRelativePath());
                String expGenGode = TestResourceLoader.loadResourceNewlinesNormalized(
                    baseName + "-expected." + ext, getClass(), newline);
                expectedGeneratedCodes.add(expGenGode);
            }
        }
        task.execute();
        assertThat(task.getAllErrors(), is(empty()));
        assertEquals(task.isCodeChangeDetected(), 
            !task.isCodeChangeDetectionDisabled() && !expectedChangeSetIndices.isEmpty());
        CodeChangeSummary changeSummary = CodeChangeSummary.deserialize(
            task.getChangeSummaryFile());
        int actualChangeSetSize = changeSummary.getChangedFiles().size();
        assertEquals(actualChangeSetSize, expectedChangeSetIndices.size());
        for (int i = 0; i < actualChangeSetSize; i++) {
            String expected = expectedGeneratedCodes.get(i);
            ChangedFile cf = changeSummary.getChangedFiles().get(i);
            File f = new File(cf.getDestDir(), cf.getRelativePath());
            String actual = FileUtils.readFileToString(f, taskCharset);            
            assertEquals(actual, expected, "Unexpected contents found in " + f);
        }
        if (task.isCodeChangeDetectionDisabled()) {
            assertNull(task.getChangeDetailsFile());
        }
        else {
            assertNotNull(task.getChangeDetailsFile());
            CodeGenerationResponseChangeSet actualChanges = CodeGenerationResponseChangeSet.deserialize(
                task.getChangeDetailsFile());
            assertEquals(actualChanges, expectedChanges);
            CodeChangeSummary expectedChangeSummary = new CodeChangeSummary(new ArrayList<>());
            for (SourceFileChangeSet s : actualChanges.getSourceFileChangeSets()) {
                ChangedFile cf = new ChangedFile(s.getRelativePath(), s.getSrcDir(), s.getDestDir());
                expectedChangeSummary.getChangedFiles().add(cf);
            }
            assertEquals(changeSummary, expectedChangeSummary);
        }
    }

    @DataProvider
    public Object[][] createTestExecuteData() {
        return new Object[][]{
            new Object[] { "task-spec-00.json", "\r\n", Arrays.asList() },
            new Object[] { "task-spec-01.json", "\r\n", Arrays.asList(0, 1) },
            new Object[] { "task-spec-02.json", "\n", Arrays.asList(0) },
            new Object[] { "task-spec-03.json", "\r\n", Arrays.asList(0, 1, 2) },
            new Object[] { "task-spec-04.json", "\r\n", Arrays.asList(0, 1) },
        };
    }
    
    @Test(dataProvider = "createTestExecuteForErrorsData")
    public void testExecuteForErrors(String jsonPath, String newline,
            List<Integer> expErrLineNums) throws Exception {
        TaskLite taskSpec = startDeserialize(jsonPath);
        File prepFile = continueDeserialize(taskSpec);
        PreCodeAugmentationResult prepResult = PreCodeAugmentationResult.deserialize(
            prepFile);
        final Charset taskCharset = Charset.forName(prepResult.getEncoding());
        CodeAugmentationGenericTask task = completeDeserialize(taskSpec, prepFile, 
            taskCharset, newline);
        // transfer class path resources for original source files to
        // temp directory.
        for (int i = 0; i < prepResult.getFileDescriptors().size(); i++) {
            SourceFileDescriptor f = prepResult.getFileDescriptors().get(i);
            assertNotNull(f.getDir());
            String contents = TestResourceLoader.loadResourceNewlinesNormalized(
                f.getRelativePath(), getClass(), newline);
            FileUtils.write(new File(f.getDir(), f.getRelativePath()), contents, taskCharset);
        }
        task.execute();
        assertEquals(task.getAllErrors().size(), expErrLineNums.size(),
            task.getAllErrors().toString());
        System.err.println(task.getAllErrors().toString());
        for (int i  = 0; i < expErrLineNums.size(); i++) {
            Integer expLineNum = expErrLineNums.get(i);
            if (expLineNum != null) {
                Throwable taskExc = task.getAllErrors().get(i);
                assertEquals(((GenericTaskException) taskExc).getLineNumber(), (int)expLineNum);
            }
        }
    }

    @DataProvider
    public Object[][] createTestExecuteForErrorsData() {
        return new Object[][]{
            // tests null content parts array, empty content parts array,
            // null/invalid file content hashes.
            new Object[] { "task-spec-05.json", "\r\n", Arrays.asList(null, 1, 2, null) },
            
            // tests non existent gen code section for an entire file,
            // non existent gen code for a particular aug code,
            // null content part, and null content field of a content part.
            new Object[] { "task-spec-06.json", "\r\n", Arrays.asList(2, 1, null, 2) }
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
package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * Completes code generation.
 */
public class CompletionTask extends DefaultTask {
    private final Property<Boolean> verbose;
    private final Property<String> encoding;
    private final ListProperty<Object> generatedCodeFiles;
    private final Property<Object> prepFile;
    private final Property<Object> destDir;
    private final Property<Boolean> codeChangeDetectionDisabled;
    private final Property<Boolean> failOnChanges;
    
    public CompletionTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        verbose = objectFactory.property(Boolean.class);
        encoding = objectFactory.property(String.class);
        prepFile = objectFactory.property(Object.class);
        generatedCodeFiles = objectFactory.listProperty(Object.class);
        destDir = objectFactory.property(Object.class);
        codeChangeDetectionDisabled = objectFactory.property(Boolean.class);
        failOnChanges = objectFactory.property(Boolean.class);
    }

    @TaskAction    
    public void execute() throws GradleException {
        try {
            boolean resolvedVerbose = verbose.get();
            String resolvedEncoding = encoding.get();
            File resolvedPrepFile = getProject().file(prepFile);
            List<File> resolvedGenCodeFiles = generatedCodeFiles.get().
                stream().map(x -> getProject().file(x)).collect(Collectors.toList());
            File resolvedDestDir = getProject().file(destDir);
            boolean resolvedCodeChangeDetectionDisabled = codeChangeDetectionDisabled.get();
            boolean resolvedFailOnChanges = failOnChanges.get();
            completeExecute(this, resolvedEncoding, resolvedVerbose,
                resolvedPrepFile, resolvedGenCodeFiles, resolvedDestDir,
                resolvedCodeChangeDetectionDisabled, resolvedFailOnChanges);
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new GradleException("General plugin error: " + ex, ex);
        }
    }

//:SKIP_CODE_START:
    static void completeExecute(DefaultTask task, String resolvedEncoding,
            boolean resolvedVerbose, File resolvedPrepFile,
            List<File> resolvedGenCodeFiles, File resolvedDestDir,
            boolean resolvedCodeChangeDetectionDisabled,
            boolean resolvedFailOnChanges) throws Exception {
        
        // validate
        Charset charset = Charset.forName(resolvedEncoding);
        for (int i = 0; i < resolvedGenCodeFiles.size(); i++) {
            File resolvedGenCodeFile = resolvedGenCodeFiles.get(i);
            if (resolvedGenCodeFile == null) {
                if (task instanceof CompletionTask) {
                    throw new GradleException("invaid null value found at generatedCodeFiles[" + i + "]");
                }
                else {
                    throw new RuntimeException("unexpected absence of genCodeFile");
                }
            }
        }

        // Validation complete, so start execution.
        
        Logger logger = task.getLogger();

        CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));
        genericTask.setPrepFile(resolvedPrepFile);
        genericTask.setGeneratedCodeFiles(resolvedGenCodeFiles);
        genericTask.setDestDir(resolvedDestDir);
        genericTask.setCodeChangeDetectionDisabled(resolvedCodeChangeDetectionDisabled);
        
        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            logger.info("Configuration properties:");
            logger.info("\tencoding: " + genericTask.getCharset());
            logger.info("\tdestDir: " + genericTask.getDestDir());
            if (task instanceof CompletionTask) {
                logger.info("\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getGeneratedCodeFiles().size(); i++) {
                    logger.info("\tgeneratedCodeFiles[" + i + "]: " + genericTask.getGeneratedCodeFiles().get(i));
                }
            }
            logger.info("\tresolvedCodeChangeDetectionDisabled: " + genericTask.isCodeChangeDetectionDisabled());
            logger.info("\tfailOnChanges: " + resolvedFailOnChanges);
            logger.info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
        }

        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause());
        }

        // fail build if there were errors.
        if (!genericTask.getAllErrors().isEmpty()) {
            String allExMsg = PluginUtils.stringifyPossibleScriptErrors(
                genericTask.getAllErrors(), false, null, null);
            throw new GradleException(allExMsg);
        }

        // also fail build if there were changed files.
        if (resolvedFailOnChanges && genericTask.isCodeChangeDetected()) {
            StringBuilder outOfSyncMsg = new StringBuilder();
            outOfSyncMsg.append("Some source file are now out of sync with generating code scripts. ");
            outOfSyncMsg.append("For details please look into top-level files of directory ");
            outOfSyncMsg.append(resolvedDestDir).append("\n");

            throw new GradleException(outOfSyncMsg.toString());
        }
    }
//:SKIP_CODE_END:

    @Internal
    public Property<Boolean> getVerbose() {
        return verbose;
    }

    /**
     * External source file encoding.
     * Task-generated files are always read and written in UTF-8.
     * @return encoding used to read and write external source code files. 
     */

    @Internal
    public Property<String> getEncoding() {
        return encoding;
    }

    @Internal
    public ListProperty<Object> getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }

    @Internal
    public Property<Object> getPrepFile() {
        return prepFile;
    }

    @Internal
    public Property<Object> getDestDir() {
        return destDir;
    }

    @Internal
    public Property<Boolean> getCodeChangeDetectionDisabled() {
        return codeChangeDetectionDisabled;
    }

    @Internal
    public Property<Boolean> getFailOnChanges() {
        return failOnChanges;
    }
}
package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

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
public class CompleteRunTask extends DefaultTask {
    private final Property<Boolean> verbose;
    private final Property<String> encoding;
    private final ListProperty<Object> generatedCodeFiles;
    private final Property<Object> prepFile;
    private final Property<Object> destDir;
    private final Property<Object> changeSetInfoFile;
    
    public CompleteRunTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        verbose = objectFactory.property(Boolean.class);
        encoding = objectFactory.property(String.class);
        prepFile = objectFactory.property(Object.class);
        generatedCodeFiles = objectFactory.listProperty(Object.class);
        destDir = objectFactory.property(Object.class);
        changeSetInfoFile = objectFactory.property(Object.class);
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
            File resolvedChangeSetInfoFile = getProject().file(changeSetInfoFile);
            completeExecute(this, resolvedEncoding, resolvedVerbose,
                resolvedPrepFile, resolvedGenCodeFiles, resolvedDestDir,
                resolvedChangeSetInfoFile);
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new GradleException("General plugin error: " + ex, ex);
        }
    }

    static void completeExecute(DefaultTask task, String resolvedEncoding,
            boolean resolvedVerbose, File resolvedPrepFile,
            List<File> resolvedGenCodeFiles, File resolvedDestDir,
            File resolvedChangeSetInfoFile) throws Exception {
        // validate
        Charset charset = Charset.forName(resolvedEncoding);
        for (int i = 0; i < resolvedGenCodeFiles.size(); i++) {
            File resolvedGenCodeFile = resolvedGenCodeFiles.get(i);
            if (resolvedGenCodeFile == null) {
                if (task instanceof CompleteRunTask) {
                    throw new GradleException("generatedCodeFiles[" + i + "] is null");
                }
                else {
                    throw new RuntimeException("unexpected null for genCodeFile");
                }
            }
        }

        // Validation complete. start execution by deleting contents
        // of destDir so generated output files is not confused with previous ones.
        Logger logger = task.getLogger();
        logger.info("Deleting contents of " + resolvedDestDir + "...");
        task.getProject().delete(task.getProject().fileTree(resolvedDestDir));

        CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));
        genericTask.setPrepFile(resolvedPrepFile);
        genericTask.setGeneratedCodeFiles(resolvedGenCodeFiles);
        genericTask.setDestDir(resolvedDestDir);
        
        if (resolvedVerbose) {
            // print task properties - generic task ones, and any ones outside
            logger.info("Configuration properties:");
            logger.info("\tencoding: " + genericTask.getCharset());
            logger.info("\tdestDir: " + genericTask.getDestDir());
            if (task instanceof CompleteRunTask) {
                logger.info("\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getGeneratedCodeFiles().size(); i++) {
                    logger.info("\tgeneratedCodeFiles[" + i + "]: " + genericTask.getGeneratedCodeFiles().get(i));
                }
            }
            logger.info("\tchangeSetInfoFile: " + resolvedChangeSetInfoFile);
            logger.info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
        }

        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause());
        }

        // Write out change set info file always even if there are no changes.
        // Because change set info is intended to be used by OS command line scripts,
        // use OS newline separator, default charset, and absolute paths.
        StringBuilder changeSetInfo = new StringBuilder();
        for (int i = 0; i < genericTask.getSrcFiles().size(); i++) {
            changeSetInfo.append(genericTask.getSrcFiles().get(i).getAbsolutePath());
            changeSetInfo.append(System.lineSeparator());
            changeSetInfo.append(genericTask.getDestFiles().get(i).getAbsolutePath());
            changeSetInfo.append(System.lineSeparator());
        }
        // ensure dir exists for changeSetInfoFile
        resolvedChangeSetInfoFile.getParentFile().mkdirs();
        try (Writer fWriter = new OutputStreamWriter(new 
                FileOutputStream(resolvedChangeSetInfoFile), Charset.defaultCharset())) {
            fWriter.write(changeSetInfo.toString());
        }
        catch (IOException ex) {
            throw new GradleException("Failed to write change set information to " +
                resolvedChangeSetInfoFile, ex);
        }

        // fail build if there were changed files.
        if (!genericTask.getSrcFiles().isEmpty()) {
            StringBuilder outOfSyncMsg = new StringBuilder();
            outOfSyncMsg.append("The following files are out of sync with generating code scripts:\n");
            for (int i = 0; i < genericTask.getSrcFiles().size(); i++) {
                outOfSyncMsg.append(" ").append(i+1).append(". ");
                outOfSyncMsg.append(genericTask.getSrcFiles().get(i).getPath());
                outOfSyncMsg.append("\n");
            }

            throw new GradleException(outOfSyncMsg.toString());
        }
    }

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
    public Property<Object> getChangeSetInfoFile() {
        return changeSetInfoFile;
    }
}
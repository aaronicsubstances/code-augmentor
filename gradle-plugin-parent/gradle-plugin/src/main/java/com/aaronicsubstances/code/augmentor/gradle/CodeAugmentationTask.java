package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * Completes code generation.
 */
public class CodeAugmentationTask extends DefaultTask {
    private final Property<String> encoding;
    private final ListProperty<Object> generatedCodeFiles;
    private final Property<Object> prepFile;
    private final Property<Object> destDir;
    private final Property<Object> changeSetInfoFile;
    
    public CodeAugmentationTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        encoding = objectFactory.property(String.class);
        prepFile = objectFactory.property(Object.class);
        generatedCodeFiles = objectFactory.listProperty(Object.class);
        destDir = objectFactory.property(Object.class);
        changeSetInfoFile = objectFactory.property(Object.class);
    }

    @TaskAction    
    public void execute() throws Exception {
        try {
            CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
            Charset charset = Charset.forName(encoding.get());
            genericTask.setCharset(charset);
            genericTask.setLogAppender(TaskUtils.createLogAppender(this));
            if (!prepFile.isPresent()) {
                throw new GradleException("prepFile property must be set");
            }
            File resolvedPrepFile = getProject().file(prepFile);
            genericTask.setPrepFile(resolvedPrepFile);
            genericTask.setGeneratedCodeFiles(generatedCodeFiles.get().
                stream().map(x -> getProject().file(x)).collect(Collectors.toList()));
            if (!destDir.isPresent()) {
                throw new GradleException("destDir property must be set");
            }
            File resolvedDestDir = getProject().file(destDir);
            genericTask.setDestDir(resolvedDestDir);
            if (!changeSetInfoFile.isPresent()) {
                throw new GradleException("changeSetInfoFile property must be set");
            }
            File resolvedChangeSetInfoFile = getProject().file(changeSetInfoFile);
            
            CodeAugmentorPluginExtension ext = getProject().getExtensions().findByType(
                CodeAugmentorPluginExtension.class);
            if (ext.getVerbose().get()) {
                // print task properties - generic task ones, and any ones outside
                getLogger().info("Configuration properties:");
                getLogger().info("\tencoding: " + genericTask.getCharset());
                getLogger().info("\tprepFile: " + genericTask.getPrepFile());
                getLogger().info("\tdestDir: " + genericTask.getDestDir());
                for (int i = 0; i < genericTask.getGeneratedCodeFiles().size(); i++) {
                    getLogger().info("\tgeneratedCodeFiles[" + i + "]: " + genericTask.getGeneratedCodeFiles().get(i));
                }
                getLogger().info("\tchangeSetInfoFile: " + resolvedChangeSetInfoFile);
                getLogger().info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            }
    
            try {
                genericTask.execute();
            }
            catch (GenericTaskException ex) {
                throw new GradleException(ex.getMessage(), ex.getCause());
            }
    
            // Write out change set info file.
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
        catch (Throwable ex) {
            if (ex instanceof GradleException) {
                throw ex;
            }
            throw new GradleException("General plugin error: " + ex, ex);
        }
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
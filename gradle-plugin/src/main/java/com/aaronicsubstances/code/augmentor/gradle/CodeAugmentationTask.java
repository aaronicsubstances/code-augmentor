package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
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
    private final ListProperty<RegularFile> generatedCodeFiles;
    private final RegularFileProperty prepFile;
    private final DirectoryProperty destDir;
    private final RegularFileProperty changeSetInfoFile;
    
    public CodeAugmentationTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        encoding = objectFactory.property(String.class);
        prepFile = objectFactory.fileProperty();
        generatedCodeFiles = objectFactory.listProperty(RegularFile.class);
        destDir = objectFactory.directoryProperty();
        changeSetInfoFile = objectFactory.fileProperty();
    }

    @TaskAction
    public void execute() {
        Charset charset = Charset.forName(encoding.get());
        BiConsumer<Integer, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            switch (logLevel) {
                case CodeAugmentationGenericTask.LOG_LEVEL_VERBOSE:
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug(msgFunc.get());
                    }
                    break;
                case CodeAugmentationGenericTask.LOG_LEVEL_INFO:
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info(msgFunc.get());
                    }
                    break;
                case CodeAugmentationGenericTask.LOG_LEVEL_WARN:
                    if (getLogger().isWarnEnabled()) {
                        getLogger().warn(msgFunc.get());
                    }
                    break;
            }
        };

        CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(logAppender);
        genericTask.setPrepFile(prepFile.get().getAsFile());
        genericTask.setGeneratedCodeFiles(generatedCodeFiles.get().
            stream().map(x -> x.getAsFile()).collect(Collectors.toList()));
        genericTask.setDestDir(destDir.get().getAsFile());
        File changeSetInfoFile = this.changeSetInfoFile.get().getAsFile();

        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause());
        }
        catch (Exception ex) {
            throw new GradleException("General plugin error", ex);
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
        try (Writer fWriter = new OutputStreamWriter(new 
                FileOutputStream(changeSetInfoFile), Charset.defaultCharset())) {
            fWriter.write(changeSetInfo.toString());
        }
        catch (IOException ex) {
            throw new GradleException("Failed to write change set information to " +
                changeSetInfoFile, ex);
        }

        // fail build if there were changed files.
        if (!genericTask.getSrcFiles().isEmpty()) {
            getLogger().warn("The following file(s) out of sync " +
                "with generating code scripts:");
            for (int i = 0; i < genericTask.getSrcFiles().size(); i++) {
                getLogger().warn(genericTask.getSrcFiles().get(i).getPath());
            }

            throw new GradleException(
                genericTask.getSrcFiles().size() +
                " file(s) out of sync " +
                "with generating code scripts. Regeneration needed.");
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
    public ListProperty<RegularFile> getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }

    @Internal
    public RegularFileProperty getPrepFile() {
        return prepFile;
    }

    @Internal
    public DirectoryProperty getDestDir() {
        return destDir;
    }

    @Internal
    public RegularFileProperty getChangeSetInfoFile() {
        return changeSetInfoFile;
    }
}
package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

/**
 * Completes code generation.
 */
public class CodeAugmentationTask extends DefaultTask {
    private String encoding;
    
    private List<File> generatedCodeFiles;

    private File prepFile;

    private File destDir;

    private File changeSetInfoFile;

    @TaskAction
    public void execute() {
        Charset charset = Charset.forName(encoding);
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
        genericTask.setPrepFile(prepFile);
        genericTask.setGeneratedCodeFiles(generatedCodeFiles);
        genericTask.setDestDir(destDir);
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
}
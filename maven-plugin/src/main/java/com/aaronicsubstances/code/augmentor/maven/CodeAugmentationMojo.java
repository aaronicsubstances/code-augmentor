package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * Completes code generation.
 */
@Mojo(name = "generate")
public class CodeAugmentationMojo extends AbstractMojo {
    @Parameter( defaultValue="${project.build.sourceEncoding}", readonly=true, required=true )
    private String encoding;
    
    @Parameter( required=true )
    private File[] generatedCodeFiles;

    @Parameter( required=true )
    private File prepFile;

    @Parameter( required=true )
    private File destDir;

    @Parameter
    private String newline;

    @Parameter( defaultValue = "${project.build.dir}/c.txt")
    private File changeSetInfoFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Charset charset = Charset.forName(encoding);
        if (newline == null) {
            newline = System.lineSeparator();
        }
        BiConsumer<Integer, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            switch (logLevel) {
                case CodeAugmentationGenericTask.LOG_LEVEL_VERBOSE:
                    if (getLog().isDebugEnabled()) {
                        getLog().debug(msgFunc.get());
                    }
                    break;
                case CodeAugmentationGenericTask.LOG_LEVEL_INFO:
                    if (getLog().isInfoEnabled()) {
                        getLog().info(msgFunc.get());
                    }
                    break;
                case CodeAugmentationGenericTask.LOG_LEVEL_WARN:
                    if (getLog().isWarnEnabled()) {
                        getLog().warn(msgFunc.get());
                    }
                    break;
            }
        };

        CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setNewline(newline);
        genericTask.setLogAppender(logAppender);
        genericTask.setPrepFile(prepFile);
        genericTask.setGeneratedCodeFiles(Arrays.asList(generatedCodeFiles));
        genericTask.setDestDir(destDir);
        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause());
        }
        catch (Exception ex) {
            throw new MojoFailureException("General plugin error", ex);
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
        try {
            FileUtils.fileWrite(changeSetInfoFile.getPath(), null, changeSetInfo.toString());
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Failed to write change set information to " +
                changeSetInfoFile, ex);
        }

        // fail build if there were changed files.
        if (!genericTask.getSrcFiles().isEmpty()) {
            getLog().warn("The following file(s) out of sync " +
                "with generating code scripts:");
            for (int i = 0; i < genericTask.getSrcFiles().size(); i++) {
                getLog().warn(genericTask.getSrcFiles().get(i).getPath());
            }

            throw new MojoExecutionException(
                genericTask.getSrcFiles().size() +
                " file(s) out of sync " +
                "with generating code scripts. Regeneration needed.");
        }
    }
}
package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Completes code generation.
 */
@Mojo(name = "complete")
public class CompletionMojo extends AbstractPluginMojo {

    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/generated", 
        required = false )
    private File destDir;

    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/changeSet.txt", 
        required = false )
    private File changeSetInfoFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {            
            CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
            Charset charset = Charset.forName(getEncoding());
            genericTask.setCharset(charset);
            genericTask.setLogAppender(TaskUtils.createLogAppender(this, isVerbose()));
            genericTask.setPrepFile(getPrepFile());
            genericTask.setGeneratedCodeFiles(Arrays.asList(getGeneratedCodeFiles()));
            genericTask.setDestDir(destDir);

            if (isVerbose()) {
                // print task properties - generic task ones, and any ones outside
                getLog().info("Configuration properties:");
                getLog().info("\tencoding: " + genericTask.getCharset());
                getLog().info("\tprepFile: " + genericTask.getPrepFile());
                getLog().info("\tdestDir: " + genericTask.getDestDir());
                for (int i = 0; i < genericTask.getGeneratedCodeFiles().size(); i++) {
                    getLog().info("\tgeneratedCodeFiles[" + i + "]: " + genericTask.getGeneratedCodeFiles().get(i));
                }
                getLog().info("\tchangeSetInfoFile: " + changeSetInfoFile);
                getLog().info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            }

            try {
                genericTask.execute();
            }
            catch (GenericTaskException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex.getCause());
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
                throw new MojoExecutionException("Failed to write change set information to " +
                    changeSetInfoFile, ex);
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

                throw new MojoExecutionException(outOfSyncMsg.toString());
            }
        }
        catch (Throwable ex) {
            if (ex instanceof MojoExecutionException) {
                throw (MojoExecutionException) ex;
            }
            if (ex instanceof MojoFailureException) {
                throw (MojoFailureException) ex;
            }
            throw new MojoFailureException("General plugin error: " + ex, ex);
        }
    }
}
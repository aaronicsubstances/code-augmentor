package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * Completes code generation.
 */
@Mojo(name = "complete")
public class CompletionMojo extends AbstractPluginMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            String resolvedEncoding = getEncoding();
            boolean resolvedVerbose = isVerbose();
            File resolvedPrepFile = getPrepFile();
            List<File> resolvedGenCodeFiles = Arrays.asList(getGeneratedCodeFiles());
            File resolvedDestDir = getDestDir();
            File resolvedChangeSetInfoFile = getChangeSetInfoFile();
            completeExecute(this, resolvedEncoding, resolvedVerbose, resolvedPrepFile, 
                resolvedGenCodeFiles, resolvedDestDir, resolvedChangeSetInfoFile);
        }
        catch (MojoExecutionException ex) {
            throw ex;
        }
        catch (MojoFailureException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new MojoFailureException("General plugin error: " + ex, ex);
        }
    }

//:SKIP_CODE_START:
    static void completeExecute(AbstractMojo task, String resolvedEncoding,
            boolean resolvedVerbose, File resolvedPrepFile,
            List<File> resolvedGenCodeFiles, File resolvedDestDir,
            File resolvedChangeSetInfoFile) throws Exception {
        
        // validate
        Charset charset = Charset.forName(resolvedEncoding);
        for (int i = 0; i < resolvedGenCodeFiles.size(); i++) {
            File resolvedGenCodeFile = resolvedGenCodeFiles.get(i);
            if (resolvedGenCodeFile == null) {
                if (task instanceof CompletionMojo) {
                    throw new MojoExecutionException("invaid null value found at generatedCodeFiles[" + i + "]");
                }
                else {
                    throw new RuntimeException("unexpected absence of genCodeFile");
                }
            }
        }

        // Validation complete. start execution by deleting contents
        // of destDir so generated output files is not confused with previous ones.
        Log logger = task.getLog();
        logger.info("Deleting " + resolvedDestDir + "...");
        FileUtils.deleteDirectory(resolvedDestDir);

        CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));
        genericTask.setPrepFile(resolvedPrepFile);
        genericTask.setGeneratedCodeFiles(resolvedGenCodeFiles);
        genericTask.setDestDir(resolvedDestDir);
        
        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            logger.info("Configuration properties:");
            logger.info("\tencoding: " + genericTask.getCharset());
            logger.info("\tdestDir: " + genericTask.getDestDir());
            if (task instanceof CompletionMojo) {
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
            throw new MojoExecutionException(ex.getMessage(), ex.getCause());
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
            throw new MojoExecutionException("Failed to write change set information to " +
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

            throw new MojoExecutionException(outOfSyncMsg.toString());
        }
    }
//:SKIP_CODE_END:
}
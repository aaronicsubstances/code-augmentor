package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

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
            boolean resolvedCodeChangeDetectionDisabled = getCodeChangeDetectionDisabled();
            boolean resolvedFailOnChanges = getFailOnChanges();
            completeExecute(this, resolvedEncoding, resolvedVerbose, resolvedPrepFile, 
                resolvedGenCodeFiles, resolvedDestDir, resolvedCodeChangeDetectionDisabled,
                resolvedFailOnChanges);
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

// :SKIP_CODE_START:
    static void completeExecute(AbstractMojo task, String resolvedEncoding,
            boolean resolvedVerbose, File resolvedPrepFile,
            List<File> resolvedGenCodeFiles, File resolvedDestDir,
            boolean resolvedCodeChangeDetectionDisabled,
            boolean resolvedFailOnChanges) throws Exception {
        
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

        // Validation complete, so start execution.
        Log logger = task.getLog();

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
            if (task instanceof CompletionMojo) {
                logger.info("\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getGeneratedCodeFiles().size(); i++) {
                    logger.info("\tgeneratedCodeFiles[" + i + "]: " + genericTask.getGeneratedCodeFiles().get(i));
                }
            }
            logger.info("\tcodeChangeDetectionDisabled: " + genericTask.isCodeChangeDetectionDisabled());
            logger.info("\tfailOnChanges: " + resolvedFailOnChanges);
            logger.info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
        }

        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause());
        }

        // fail build if there were errors.
        if (!genericTask.getAllErrors().isEmpty()) {
            String allExMsg = PluginUtils.stringifyPossibleScriptErrors(
                genericTask.getAllErrors(), false, null, null);
            throw new MojoExecutionException(allExMsg);
        }

        // also fail build if there were changed files.
        if (resolvedFailOnChanges && genericTask.isCodeChangeDetected()) {
            StringBuilder outOfSyncMsg = new StringBuilder();
            outOfSyncMsg.append("Some source file are now out of sync with generating code scripts. ");
            outOfSyncMsg.append("For details please look into top-level files of directory ");
            outOfSyncMsg.append(resolvedDestDir).append("\n");

            throw new MojoExecutionException(outOfSyncMsg.toString());
        }
    }
//:SKIP_CODE_END:
}
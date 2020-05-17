package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class CompletionTask extends Task {
    private boolean verbose;
    private String encoding;
    private File prepFile;
    private File destDir;
    private final List<GenCodeSpec> genCodeSpecs = new ArrayList<>();
    private boolean codeChangeDetectionDisabled;
    private boolean failOnChanges = true;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public void setPrepFile(File prepFile) {
        this.prepFile = prepFile;
    }

    public void setCodeChangeDetectionDisabled(boolean codeChangeDetectionDisabled) {
        this.codeChangeDetectionDisabled = codeChangeDetectionDisabled;
    }

    public void setFailOnChanges(boolean failOnChanges) {
        this.failOnChanges = failOnChanges;
    }

    public void addConfiguredGenCodeSpec(GenCodeSpec spec) {
        genCodeSpecs.add(spec);
    }
    
    public void execute() {
        try {
            List<File> resolvedGenCodeFiles = new ArrayList<>();
            for (GenCodeSpec genCodeSpec : genCodeSpecs) {
                File genCodeFile = null;
                if (genCodeSpec != null) {
                    genCodeFile = genCodeSpec.getFile();
                }
                resolvedGenCodeFiles.add(genCodeFile);
            }
            completeExecute(this, encoding, verbose, prepFile, 
                resolvedGenCodeFiles, destDir, codeChangeDetectionDisabled, failOnChanges);
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BuildException("General error: " + ex, ex);
        }
    }

//:SKIP_CODE_START:
    static void completeExecute(Task task, String resolvedEncoding,
            boolean resolvedVerbose, File resolvedPrepFile,
            List<File> resolvedGenCodeFiles, File resolvedDestDir,
            boolean resolvedCodeChangeDetectionDisabled,
            boolean resolvedFailOnChanges) throws Exception {
        // set up defaults
        if (resolvedEncoding == null) {
            resolvedEncoding = "UTF-8";
        }
        if (resolvedPrepFile == null) {
            resolvedPrepFile = TaskUtils.getDefaultPrepFile(task);
        }
        if (resolvedGenCodeFiles.isEmpty()) {
            resolvedGenCodeFiles.add(TaskUtils.getDefaultGenCodeFile(task));
        }
        if (resolvedDestDir == null) {
            resolvedDestDir = TaskUtils.getDefaultDestDir(task);
        }
        // validate
        Charset charset = Charset.forName(resolvedEncoding);
        for (int i = 0; i < resolvedGenCodeFiles.size(); i++) {
            File resolvedGenCodeFile = resolvedGenCodeFiles.get(i);
            if (resolvedGenCodeFile == null) {
                if (task instanceof CompletionTask) {
                    throw new BuildException("invaid null value found at genCodeSpecs[" + i + "]");
                }
                else {
                    throw new RuntimeException("unexpected absence of genCodeFile");
                }
            }
        }

        // Validation complete, so start execution.
        

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
            task.log("Configuration properties:");
            task.log("\tencoding: " + genericTask.getCharset());
            task.log("\tdestDir: " + genericTask.getDestDir());
            if (task instanceof CompletionTask) {
                task.log("\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getGeneratedCodeFiles().size(); i++) {
                    task.log("\tgenCodeSpecs[" + i + "].file: " + genericTask.getGeneratedCodeFiles().get(i));
                }
            }
            task.log("\tcodeChangeDetectionDisabled: " + genericTask.isCodeChangeDetectionDisabled());
            task.log("\tfailOnChanges: " + resolvedFailOnChanges);
            task.log("\tgenericTask.logAppender: " + genericTask.getLogAppender());
        }

        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new BuildException(ex.getMessage(), ex.getCause());
        }

        // fail build if there were errors.
        if (!genericTask.getAllErrors().isEmpty()) {
            String allExMsg = PluginUtils.stringifyPossibleScriptErrors(
                genericTask.getAllErrors(), false, null, null);
            throw new BuildException(allExMsg);
        }

        // also fail build if there were changed files.
        if (resolvedFailOnChanges && genericTask.isCodeChangeDetected()) {
            StringBuilder outOfSyncMsg = new StringBuilder();
            outOfSyncMsg.append("Some source file are now out of sync with generating code scripts. ");
            outOfSyncMsg.append("For details please look into top-level files of directory ");
            outOfSyncMsg.append(resolvedDestDir).append("\n");

            throw new BuildException(outOfSyncMsg.toString());
        }
    }
//:SKIP_CODE_END:
}
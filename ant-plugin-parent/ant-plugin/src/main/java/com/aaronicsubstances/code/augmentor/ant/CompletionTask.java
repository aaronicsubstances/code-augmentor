package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import groovy.util.Eval;

public class CompletionTask extends Task {
    private boolean verbose;
    private String encoding;
    private File prepFile;
    private File destDir;
    private File changeSetInfoFile;
    private final List<GenCodeSpec> genCodeSpecs = new ArrayList<>();

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

    public void setChangeSetInfoFile(File changeSetInfoFile) {
        this.changeSetInfoFile = changeSetInfoFile;
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
                resolvedGenCodeFiles, destDir, changeSetInfoFile);
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
            File resolvedChangeSetInfoFile) throws Exception {
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
        if (resolvedChangeSetInfoFile == null) {
            resolvedChangeSetInfoFile = TaskUtils.getDefaultChangeSetInfoFile(task);
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

        // Validation complete. start execution by deleting contents
        // of destDir so generated output files is not confused with previous ones.
        
        task.log("Deleting contents of " + resolvedDestDir + "...");
        Eval.me("x", resolvedDestDir, "x.deleteDir()");

        CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));
        genericTask.setPrepFile(resolvedPrepFile);
        genericTask.setGeneratedCodeFiles(resolvedGenCodeFiles);
        genericTask.setDestDir(resolvedDestDir);
        
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
            task.log("\tchangeSetInfoFile: " + resolvedChangeSetInfoFile);
            task.log("\tgenericTask.logAppender: " + genericTask.getLogAppender());
        }

        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new BuildException(ex.getMessage(), ex.getCause());
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
            throw new BuildException("Failed to write change set information to " +
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

            throw new BuildException(outOfSyncMsg.toString());
        }
    }
//:SKIP_CODE_END:
}
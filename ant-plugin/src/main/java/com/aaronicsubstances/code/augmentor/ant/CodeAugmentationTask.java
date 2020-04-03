package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.CodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class CodeAugmentationTask extends Task {
    private String encoding;
    private File prepfile;
    private File destdir;
    private String newline;
    private File changeSetInfoFile;
    private final List<CodeGenerationResponseSpecification> generatedCodeFiles = new ArrayList<>();

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    public void setPrepfile(File f) {
        this.prepfile = f;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    public void setChangeSetInfoFile(File changeSetInfoFile) {
        this.changeSetInfoFile = changeSetInfoFile;
    }

    public void addConfiguredSpec(CodeGenerationResponseSpecification f) {
        f.validate();
        generatedCodeFiles.add(f);
    }
    
    public void execute() {
        Charset charset = StandardCharsets.UTF_8;
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            }
            catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
                throw new BuildException("Invalid value for encoding attribute: " +
                    encoding, ex);
            }
        }

        if (destdir == null) {
            throw new BuildException("destdir attribute is required");
        }
        if (prepfile == null) {
            throw new BuildException("prepfile attribute is required");
        }
        if (changeSetInfoFile == null) {
            throw new BuildException("changeSetInfoFile attribute is required");
        }
        if (generatedCodeFiles.isEmpty()) {
            throw new BuildException("at least one spec nested element is required");
        }
        
        if (newline == null) {
            newline = System.lineSeparator();
        }
        BiConsumer<Integer, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            switch (logLevel) {
                case CodeAugmentationGenericTask.LOG_LEVEL_VERBOSE:
                    log(msgFunc.get(), Project.MSG_VERBOSE);
                    break;
                case CodeAugmentationGenericTask.LOG_LEVEL_INFO:
                    log(msgFunc.get(), Project.MSG_INFO);
                    break;
                case CodeAugmentationGenericTask.LOG_LEVEL_WARN:
                    log(msgFunc.get(), Project.MSG_WARN);
                    break;
            }
        };

        CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setNewline(newline);
        genericTask.setLogAppender(logAppender);
        genericTask.setPrepFile(prepfile);
        genericTask.setGeneratedCodeFiles(
            generatedCodeFiles.stream()
                .map(x -> x.getGenCodeFile())
                .collect(Collectors.toList()));
        genericTask.setDestDir(destdir);
        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new BuildException(ex.getMessage(), ex.getCause());
        }
        catch (Exception ex) {
            throw new BuildException("General plugin error", ex);
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
            try (Writer fWriter = new OutputStreamWriter(new 
                    FileOutputStream(changeSetInfoFile), Charset.defaultCharset())) {
                fWriter.write(changeSetInfo.toString());
            }
        }
        catch (IOException ex) {
            throw new BuildException("Failed to write change set information to " +
                changeSetInfoFile, ex);
        }

        // fail build if there were changed files.
        if (!genericTask.getSrcFiles().isEmpty()) {
            log("The following file(s) out of sync " +
                "with generating code scripts:", Project.MSG_WARN);
            for (int i = 0; i < genericTask.getSrcFiles().size(); i++) {
                log(genericTask.getSrcFiles().get(i).getPath(), Project.MSG_WARN);
            }

            throw new BuildException(
                genericTask.getSrcFiles().size() +
                " file(s) out of sync " +
                "with generating code scripts. Regeneration needed.");
        }
    }
}
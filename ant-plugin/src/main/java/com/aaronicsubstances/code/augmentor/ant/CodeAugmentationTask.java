package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.tasks.CodeAugmentationGenericTask;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class CodeAugmentationTask extends Task {
    private String encoding;
    private boolean verbose = false;
    private File prepfile;
    private File destdir;
    private boolean generate = false;
    private String newline;

    private final List<CodeGenerationResponseSpecification> generatedCodeFiles = new ArrayList<>();
    
    // validation results
	private Charset charset;

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    public void setGenerate(boolean generate) {
        this.generate = generate;
    }

    public void setPrepfile(File f) {
        this.prepfile = f;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    public void addConfiguredSpec(CodeGenerationResponseSpecification f) {
        f.validate();
        generatedCodeFiles.add(f);
    }
    
    public void execute() {
        Instant startInstant = Instant.now();
        validate();
        try {
            _execute();
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new BuildException("I/O error: " + ex.getMessage(), ex);
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new RuntimeException("Unexpected error: " + ex.getMessage(), ex);
        }
            
        Instant endInstant = Instant.now();
        long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
        //logVerbose("completed in %s ms", timeElapsed);
    }

    private void validate() {
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            }
            catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
                throw new BuildException("Invalid value for encoding attribute: " +
                    encoding, ex);
            }
        }

        if (generate && destdir == null) {
            throw new BuildException("destdir attribute must be set if generate property is true");
        }
        if (prepfile == null) {
            throw new BuildException("prepfile attribute is required");
        }
        if (generatedCodeFiles.isEmpty()) {
            throw new BuildException("at least one spec nested element is required");
        }

        // set defaults.
        if (encoding == null) {
            charset = StandardCharsets.UTF_8;
        }
        if (newline == null) {
            newline = System.lineSeparator();
        }
    }

    private void _execute() throws Exception {
        CodeAugmentationGenericTask genericTask = new CodeAugmentationGenericTask();
		genericTask.execute();
    }
}
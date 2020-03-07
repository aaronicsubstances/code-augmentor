package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * 
 */
public class CodeAugmentationTask extends Task {
    private String encoding;
    private boolean verbose = false;
    private File prepfile;
    private File destdir;
    private boolean overwrite = false;

    private final List<File> generatedCodeFiles = new ArrayList<>();
    
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

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void setPrepfile(File f) {
        this.prepfile = f;
    }

    public void addGen_code_file(File f) {
        generatedCodeFiles.add(f);
    }
    
    public void execute() {
        validate();
        

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

        if (!overwrite && destdir == null) {
            throw new BuildException("destdir attribute must be set if overwrite property is false");
        }
        if (prepfile == null) {
            throw new BuildException("prepfile attribute is required");
        }
        if (generatedCodeFiles.isEmpty()) {
            throw new BuildException("at least one gen_code_file nested element is required");
        }

        // set defaults.
        if (encoding == null) {
            charset = Charset.defaultCharset();
        }
    }

    private void logVerbose(String message, Object... args) {
        if (verbose) {
            log("[" + String.format(message, args) + "]");
        }
    }
}
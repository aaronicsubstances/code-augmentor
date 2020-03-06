package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.LogLevel;

/**
 * 
 */
public class CodeAugmentationTask extends Task {
    private String encoding;
    private boolean verbose = false;
    private boolean failonerror = true;
    private String errorProperty;
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

    public void setFailonerror(boolean failonerror) {
        this.failonerror = failonerror;
    }

    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void setPrepfile(File prepfile) {
        this.prepfile = prepfile;
    }

    public void addGen_code_file(File genCodeFile) {
        generatedCodeFiles.add(genCodeFile);
    }
    
    public void execute() {
        boolean valid = validate();
        if (!valid) {
            return;
        }
        _execute();
    }

    private boolean validate() {
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            }
            catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
                return failBuild("Invalid value for encoding attribute: " +
                    encoding, ex);
            }
        }

        if (!overwrite && destdir == null) {
            return failBuild("destdir attribute must be set if overwrite property is false", null);
        }
        if (prepfile == null) {
            return failBuild("prepfile attribute is required", null);
        }
        if (generatedCodeFiles.isEmpty()) {
            return failBuild("at least one gen_code_file nested element is required", null);
        }

        // set defaults.
        if (encoding == null) {
            charset = Charset.defaultCharset();
        }
        return true;
    }

    private boolean _execute() {
        return false;
    }

    private boolean failBuild(String message, Throwable cause) {
        if (errorProperty != null && !errorProperty.isEmpty()) {
            getProject().setNewProperty(errorProperty, "" + true);
        }
        if (failonerror) {
            throw new BuildException(message, cause);
        }
        else {
            log(message, cause, LogLevel.WARN.getLevel());
            return false;
        }
    }

    private void logVerbose(String message, Object... args) {
        if (verbose) {
            log("[" + String.format(message, args) + "]");
        }
    }
}
package com.aaronicsubstances.programmer.companion.ant.plugin;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

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
    private File parseResultXml;
    private File codeGenXml;
    private File destdir;
    private boolean overwrite = false;
    
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

    public void setParseResultXml(File parseResultXml) {
        this.parseResultXml = parseResultXml;
    }

    public void setCodeGenXml(File codeGenXml) {
        this.codeGenXml = codeGenXml;
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
                return failBuild("Invalid value for encoding property: " +
                    encoding, ex);
            }
        }

        if (!overwrite && destdir == null) {
            return failBuild("destdir property must be set if overwrite property is false", null);
        }
        if (parseResultXml == null) {
            return failBuild("parseResultXml property is required", null);
        }
        if (codeGenXml == null) {
            return failBuild("codeGenXml property is required", null);
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
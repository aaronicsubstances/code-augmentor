package com.aaronicsubstances.programmer.companion.ant.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.java.JavaLexer;
import com.aaronicsubstances.programmer.companion.java.JavaParser;
import com.aaronicsubstances.programmer.companion.java.JavaSourceCodeWrapper;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.LogLevel;
import org.apache.tools.ant.util.FileUtils;

/**
 * 
 */
public class ProgrammerSnippetPlugin extends Task {
    private String encoding;
    private boolean verbose = false;
    private boolean failonerror = true;
    private String errorProperty;
    private boolean listfiles = false;
    private File tempdir;
    private String updatedProperty;
    private File destdir;
    private boolean overwrite = false;
    private final List<FileSet> srcDirs = new ArrayList<>();

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

    public void setListfiles(boolean listfiles) {
        this.listfiles = listfiles;
    }

    public void setTempdir(File tempdir) {
        this.tempdir = tempdir;
    }

    public void setUpdatedProperty(String updatedProperty) {
        this.updatedProperty = updatedProperty;
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void addSrc(FileSet srcdir) {
        srcDirs.add(srcdir);
    }

    public void execute() {
        boolean valid = validate();
        if (valid) {
            executeAfterValidate();
        }
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
        if (srcDirs.isEmpty()) {
            return failBuild("at least 1 nested src element is required", null);
        }

        // set defaults.
        if (encoding == null) {
            charset = Charset.defaultCharset();
        }
        if (tempdir == null) {
            tempdir = new File(System.getProperty("java.io.tmpdir"));
        }
        return true;
    }

    private boolean executeAfterValidate() {
        List<String> uniqueFilePaths = new ArrayList<>();
        List<String> sourceFilenames = new ArrayList<>();
        List<File> baseDirs = new ArrayList<>();
        for (FileSet srcdir : srcDirs) {
            DirectoryScanner ds = srcdir.getDirectoryScanner(getProject());
            String[] includedFiles = ds.getIncludedFiles();
            for (String filename : includedFiles) {
                String filePath = new File(ds.getBasedir(), filename).getAbsolutePath();
                // Skip duplicates.
                if (uniqueFilePaths.contains(filePath)) {
                    continue;
                }
                uniqueFilePaths.add(filePath);
                sourceFilenames.add(filename);
                baseDirs.add(ds.getBasedir());
            }
        }

        log(String.format("Preprocessing %s file(s)", uniqueFilePaths.size()));

        // List files if requested.
        if (listfiles) {
            for (String sourceFilePath : uniqueFilePaths) {
                log(sourceFilePath);
            }
        }

        for (int i = 0; i < uniqueFilePaths.size(); i++) {
            String srcPath = uniqueFilePaths.get(i);
            logVerbose("Preprocessing %s", srcPath);
            Instant startInstant = Instant.now();
            String input;
            try (Reader rdr = new InputStreamReader(new FileInputStream(srcPath),
                    charset)) {
                input = FileUtils.readFully(rdr);
            }
            catch (IOException ex) {
                return failBuild("Failed to read from " + srcPath, ex);
            }
            JavaParser instance = new JavaParser(new JavaSourceCodeWrapper(input));
            List<Token> tokens = instance.parse();
            // write to dest, skipping comments.

            String destPath = getDestFilename(sourceFilenames.get(i), baseDirs.get(i));
            File destFile = new File(destPath);
            try (PrintWriter writer = new PrintWriter(destFile, charset.name())) {
                for (Token token : tokens) {
                    switch (token.type) {
                        case JavaLexer.TOKEN_TYPE_COMMENT_CONTENT:
                        case JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_START:
                        case JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT_END:
                        case JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_START:
                        case JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT_END:
                            break;
                        default:
                            if (token.text != null) {
                                writer.print(token.text);
                            }
                            break;
                    }
                }
            }
            catch (IOException ex) {
                return failBuild("Failed to write to " + destPath, ex);
            }
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            logVerbose("done in %s ms", timeElapsed);
        }

        if (updatedProperty != null && !updatedProperty.isEmpty()) {
            getProject().setNewProperty(updatedProperty, "" + true);
        }
        return true;
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

    private String getDestFilename(String srcFilename, File baseDir) {
        if (destdir != null) {
            baseDir = destdir;
        }
        String destFilename = new File(baseDir, srcFilename).getAbsolutePath();
        return destFilename;
    }

    private void logVerbose(String message, Object... args) {
        if (verbose) {
            log("[" + String.format(message, args) + "]");
        }
    }
}
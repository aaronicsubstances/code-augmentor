package com.aaronicsubstances.programmer.companion.ant.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
public class PreCodeAugmentationTask extends Task {
    private String encoding;
    private boolean verbose = false;
    private boolean failonerror = true;
    private String errorProperty;
    private boolean listfiles;
    private File parseResultXml;
    private final List<FileSet> srcDirs = new ArrayList<>();

    /*
     * Comment markers for augmenting code and generated code snippets generally match.
     * The few restrictions are:
     *  - if raw code is to be included in augmenting code, then // must be used.
     *  - upon return, if generated code is multiline, then  // must be used.
     * About indentation,
     *  - /* continues right after augmenting code, and just dumps verbatim.
     *  - only // tries to indent generated code and start on its own new line
     *  - however // ignores indent if generated code has multiline strings.
     */ 
    private List<String> augmentingCodeBlockStartDoubleSlash;
    private List<String> augmentingCodeBlockEndDoubleSlash;
    private List<String> augmentingCodeBlockContinuationDoubleSlash;
    private List<String> augmentingCodeBlockStartSlashStar;

    // for these prefer the very first one during code generation.
    private List<String> codeSnippetBlockStartDoubleSlash;
    private List<String> codeSnippetBlockEndDoubleSlash;
    private List<String> codeSnippetBlockStartSlashStar;
    private List<String> codeSnippetBlockEndSlashStar;

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
    
    public void setParseResultXml(File parseResultXml) {
        this.parseResultXml = parseResultXml;
    }

    public void addSrc(FileSet srcdir) {
        srcDirs.add(srcdir);
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
        if (srcDirs.isEmpty()) {
            return failBuild("at least 1 nested src element is required", null);
        }
        if (parseResultXml == null) {
            return failBuild("parseResultXml property is required", null);
        }

        // set defaults.
        if (encoding == null) {
            charset = Charset.defaultCharset();
        }
        return true;
    }

    public void execute() {
        boolean valid = validate();
        if (!valid) {
            return;
        }
        _execute();
    }

    private boolean _execute() {
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

        log(String.format("Found %s file(s)", uniqueFilePaths.size()));

        // List files if requested.
        if (listfiles) {
            for (String sourceFilePath : uniqueFilePaths) {
                log(sourceFilePath);
            }
        }

        for (int i = 0; i < uniqueFilePaths.size(); i++) {
            String srcPath = uniqueFilePaths.get(i);
            logVerbose("Preparing %s", srcPath);
            Instant startInstant = Instant.now();
            String input;
            try (Reader rdr = new InputStreamReader(new FileInputStream(srcPath),
                    charset)) {
                input = FileUtils.readFully(rdr);
            }
            catch (IOException ex) {
                return failBuild("Failed to read from " + srcPath, ex);
            }

            // use file extension to parse as Java/Kotlin code.
            JavaParser instance = new JavaParser(new JavaSourceCodeWrapper(input));
            List<Token> tokens = instance.parse();
            
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            logVerbose("done in %s ms", timeElapsed);
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

    private void logVerbose(String message, Object... args) {
        if (verbose) {
            log("[" + String.format(message, args) + "]");
        }
    }
}
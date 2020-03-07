package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.java.JavaParser;
import com.aaronicsubstances.programmer.companion.java.JavaSourceCodeWrapper;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

/**
 * 
 */
public class PreCodeAugmentationTask extends Task {
    private String encoding;
    private boolean verbose = false;
    private boolean listfiles;
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
    private final List<CodeGenerationRequestSpecification> requestSpecList = new ArrayList<>();
    private final List<String> headerDoubleSlashSuffixes = new ArrayList<>();

    // for these prefer the very first one during code generation.
    private final List<String> genCodeStartSuffixes = new ArrayList<>();
    private final List<String> genCodeEndSuffixes = new ArrayList<>();
    
    private File parseResultsFile;

    // validation results
    private Charset charset;

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setListfiles(boolean listfiles) {
        this.listfiles = listfiles;
    }

    public void setPrepfile(File f) {
        this.parseResultsFile = f;
    }

    public void addSrc(FileSet d) {
        srcDirs.add(d);
    }

    public void addConfiguredSpec(CodeGenerationRequestSpecification spec) {
        spec.validate();
        requestSpecList.add(spec);
    }

    public void addHeader_dslash_suffix(String suffix) {
        suffix = CodeGenerationRequestSpecification.validateCommentMarkerSuffix(suffix);
        if (!headerDoubleSlashSuffixes.contains(suffix)) {
            headerDoubleSlashSuffixes.add(suffix);
        }
    }

    public void addGen_code_start_suffix(String suffix) {
        suffix = CodeGenerationRequestSpecification.validateCommentMarkerSuffix(suffix);
        if (!genCodeStartSuffixes.contains(suffix)) {
            genCodeStartSuffixes.add(suffix);
        }
    }

    public void addGen_code_end_suffix(String suffix) {
        suffix = CodeGenerationRequestSpecification.validateCommentMarkerSuffix(suffix);
        if (!genCodeEndSuffixes.contains(suffix)) {
            genCodeEndSuffixes.add(suffix);
        }
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
        if (srcDirs.isEmpty()) {
            throw new BuildException("at least 1 nested src element is required");
        }
        if (requestSpecList.isEmpty()) {
            throw new BuildException("at least 1 nested spec element is required");
        }
        if (parseResultsFile == null) {
            throw new BuildException("prepfile attribute is required");
        }
        if (headerDoubleSlashSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested header_dslash_suffix element is required");
        }
        if (genCodeStartSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested gen_code_start_suffix element is required");
        }
        if (genCodeEndSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested gen_code_end_suffix element is required");
        }

        // Ensure uniqueness across comment suffixes.
        Set<String> allSuffixes = new HashSet<>();
        int totalSuffixCount = 0;        
        allSuffixes.addAll(headerDoubleSlashSuffixes);
        totalSuffixCount += headerDoubleSlashSuffixes.size();
        allSuffixes.addAll(genCodeStartSuffixes);
        totalSuffixCount += genCodeStartSuffixes.size();
        allSuffixes.addAll(genCodeEndSuffixes);
        totalSuffixCount += genCodeEndSuffixes.size();
        for (CodeGenerationRequestSpecification spec : requestSpecList) {
            allSuffixes.addAll(spec.getStartSuffixes());
            totalSuffixCount += spec.getStartSuffixes().size();
            allSuffixes.addAll(spec.getContinuationDoubleSlashSuffixes());
            totalSuffixCount += spec.getContinuationDoubleSlashSuffixes().size();
            allSuffixes.addAll(spec.getClosingDoubleSlashSuffixes());
            totalSuffixCount += spec.getClosingDoubleSlashSuffixes().size();
        }
        if (totalSuffixCount != allSuffixes.size()) {
            throw new BuildException("Duplicates detected across comment marker suffixes");
        }
        
        // set defaults.
        if (encoding == null) {
            charset = Charset.defaultCharset();
        }
    }

    public void execute() {
        validate();
        
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
                throw new BuildException("Failed to read from " + srcPath, ex);
            }

            // use file extension to parse as Java/Kotlin code.
            JavaParser instance = new JavaParser(new JavaSourceCodeWrapper(input));
            List<Token> tokens = instance.parse();
            
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            logVerbose("done in %s ms", timeElapsed);
        }
    }

    private void logVerbose(String message, Object... args) {
        if (verbose) {
            log("[" + String.format(message, args) + "]");
        }
    }
}
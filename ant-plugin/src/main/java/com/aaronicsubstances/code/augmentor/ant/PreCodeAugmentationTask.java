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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

public class PreCodeAugmentationTask extends Task {
    private String encoding;
    private boolean verbose = false;
    private boolean listfiles;
    private final List<FileSet> srcDirs = new ArrayList<>();
    private File tempDir;

    private final List<CodeGenerationRequestSpecification> requestSpecList = new ArrayList<>();
    private final List<SuffixSpec> embeddedStringDoubleSlashSuffixes = new ArrayList<>();

    // for these prefer the very first one during code generation.
    private final List<SuffixSpec> genCodeStartSuffixes = new ArrayList<>();
    private final List<SuffixSpec> genCodeEndSuffixes = new ArrayList<>();

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

    public void setTempdir(File f) {
        this.tempDir = f;
    }

    public void addSrc(FileSet d) {
        srcDirs.add(d);
    }

    public void addConfiguredSpec(CodeGenerationRequestSpecification spec) {
        spec.validate();
        requestSpecList.add(spec);
    }

    public void addConfiguredEmbedded_string_dslash_suffix(SuffixSpec suffix) {
        suffix = CodeGenerationRequestSpecification.validateCommentMarkerSuffix(suffix);
        if (!embeddedStringDoubleSlashSuffixes.contains(suffix)) {
            embeddedStringDoubleSlashSuffixes.add(suffix);
        }
    }

    public void addConfiguredGen_code_start_suffix(SuffixSpec suffix) {
        suffix = CodeGenerationRequestSpecification.validateCommentMarkerSuffix(suffix);
        if (!genCodeStartSuffixes.contains(suffix)) {
            genCodeStartSuffixes.add(suffix);
        }
    }

    public void addConfiguredGen_code_end_suffix(SuffixSpec suffix) {
        suffix = CodeGenerationRequestSpecification.validateCommentMarkerSuffix(suffix);
        if (!genCodeEndSuffixes.contains(suffix)) {
            genCodeEndSuffixes.add(suffix);
        }
    }

    private void validate() {
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
                throw new BuildException("Invalid value for encoding attribute: " + encoding, ex);
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
        if (embeddedStringDoubleSlashSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested embedded_string_dslash_suffix element is required");
        }
        if (genCodeStartSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested gen_code_start_suffix element is required");
        }
        if (genCodeEndSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested gen_code_end_suffix element is required");
        }

        // Ensure uniqueness across comment suffixes.
        Set<SuffixSpec> allSuffixes = new HashSet<>();
        int totalSuffixCount = 0;
        allSuffixes.addAll(embeddedStringDoubleSlashSuffixes);
        totalSuffixCount += embeddedStringDoubleSlashSuffixes.size();
        allSuffixes.addAll(genCodeStartSuffixes);
        totalSuffixCount += genCodeStartSuffixes.size();
        allSuffixes.addAll(genCodeEndSuffixes);
        totalSuffixCount += genCodeEndSuffixes.size();
        for (CodeGenerationRequestSpecification spec : requestSpecList) {
            allSuffixes.addAll(spec.getAugCodeSuffixes());
            totalSuffixCount += spec.getAugCodeSuffixes().size();
        }
        if (totalSuffixCount != allSuffixes.size()) {
            throw new BuildException("Duplicates detected across comment marker suffixes");
        }

        // set defaults.
        if (encoding == null) {
            charset = StandardCharsets.UTF_8;
        }
        if (tempDir == null) {
            tempDir = new File(System.getProperty("java.io.tmpdir"));
        }
    }

    public void execute() {
        Instant startInstant = Instant.now();
        validate();
        try {
            _execute();
        } catch (BuildException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BuildException("I/O error: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected error: " + ex.getMessage(), ex);
        }

        Instant endInstant = Instant.now();
        long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
        //logVerbose("completed in %s ms", timeElapsed);
    }

    private void _execute() throws Exception {
        /*List<String> uniqueFilePaths = new ArrayList<>();*/
        List<String> sourceFilenames = new ArrayList<>();
        List<File> baseDirs = new ArrayList<>();
        for (FileSet srcdir : srcDirs) {
            DirectoryScanner ds = srcdir.getDirectoryScanner(getProject());
            String[] includedFiles = ds.getIncludedFiles();
            for (String filename : includedFiles) {
                /*String filePath = new File(ds.getBasedir(), filename).getAbsolutePath();
                // Skip duplicates.
                if (uniqueFilePaths.contains(filePath)) {
                    continue;
                }
                uniqueFilePaths.add(filePath);*/
                sourceFilenames.add(filename);
                baseDirs.add(ds.getBasedir());
            }
        }

        log(String.format("Found %s file(s)", sourceFilenames.size()));

        // List files if requested.
        if (listfiles) {
            for (int i = 0; i < sourceFilenames.size(); i++) {
                File sourceFile = new File(baseDirs.get(i), sourceFilenames.get(i));
                log(sourceFile.getPath());
            }
        }
		
        PreCodeAugmentationGenericTask genericTask = new PreCodeAugmentationGenericTask();
		genericTask.execute();
    }
}
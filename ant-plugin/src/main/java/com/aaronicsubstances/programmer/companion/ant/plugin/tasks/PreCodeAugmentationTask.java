package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.programmer.companion.ParserException;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeGenerationRequest;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.PreCodeAugmentationResult;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.SourceFileDescriptor;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.LogLevel;

public class PreCodeAugmentationTask extends Task {
    private String encoding;
    private boolean verbose = false;
    private boolean listfiles;
    private final List<FileSet> srcDirs = new ArrayList<>();
    private File tempDir;

    /*
     * Comment markers for augmenting code and generated code snippets generally
     * match. The few restrictions are: - if raw code is to be included in
     * augmenting code, then // must be used. - upon return, if generated code is
     * multiline, then // must be used. About indentation, - /* continues right
     * after augmenting code, and just dumps verbatim. - only // tries to indent
     * generated code and start on its own new line - however // ignores indent if
     * generated code has multiline strings.
     */
    private final List<CodeGenerationRequestSpecification> requestSpecList = new ArrayList<>();
    private final List<String> headerDoubleSlashSuffixes = new ArrayList<>();
    private final List<String> embeddedStringDoubleSlashSuffixes = new ArrayList<>();

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

    public void addHeader_dslash_suffix(String suffix) {
        suffix = CodeGenerationRequestSpecification.validateCommentMarkerSuffix(suffix);
        if (!headerDoubleSlashSuffixes.contains(suffix)) {
            headerDoubleSlashSuffixes.add(suffix);
        }
    }

    public void addEmbedded_string_dslash_suffix(String suffix) {
        suffix = CodeGenerationRequestSpecification.validateCommentMarkerSuffix(suffix);
        if (!embeddedStringDoubleSlashSuffixes.contains(suffix)) {
            embeddedStringDoubleSlashSuffixes.add(suffix);
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
        if (headerDoubleSlashSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested header_dslash_suffix element is required");
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
        Set<String> allSuffixes = new HashSet<>();
        int totalSuffixCount = 0;
        allSuffixes.addAll(headerDoubleSlashSuffixes);
        totalSuffixCount += headerDoubleSlashSuffixes.size();
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
            charset = Charset.defaultCharset();
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
            throw new BuildException("I/O error", ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected error", ex);
        }

        Instant endInstant = Instant.now();
        long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
        logVerbose("completed in %s ms", timeElapsed);
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

        PreCodeAugmentationResult prepResult = new PreCodeAugmentationResult();
        prepResult.setTempDir(tempDir.getPath());
        prepResult.setGenCodeStartSuffix(genCodeStartSuffixes.get(0));
        prepResult.setGenCodeEndSuffix(genCodeEndSuffixes.get(0));
        Object resultWriter = prepResult.beginSerialize(parseResultsFile);

        List<Object> codeGenRequestWriters = new ArrayList<>();
        List<CodeGenerationRequest> codeGenRequests = new ArrayList<>();
        for (CodeGenerationRequestSpecification spec : requestSpecList) {
            CodeGenerationRequest codeGenRequest = new CodeGenerationRequest();
            codeGenRequests.add(codeGenRequest);
            boolean useXml = !"csv".equals(TaskUtils.getFileExt(spec.getAugCodeDestFile().getName()));
            Object requestWriter = codeGenRequest.beginSerialize(spec.getAugCodeDestFile(), useXml);
            codeGenRequestWriters.add(requestWriter);
        }

        CodeGenerationRequestCreator codeGenerationRequestCreator =
            new CodeGenerationRequestCreator(this.headerDoubleSlashSuffixes,
                this.genCodeStartSuffixes, this.genCodeEndSuffixes,
                this.embeddedStringDoubleSlashSuffixes, requestSpecList);

        List<List<AugmentingCode>> specAugCodesList = new ArrayList<>();
        for (int i = 0; i < requestSpecList.size(); i++) {
            specAugCodesList.add(new ArrayList<>());
        }

        Map<String, List<ParserException>> errorMap = null;

        for (int i = 0; i < sourceFilenames.size(); i++) {
            String relativePath = sourceFilenames.get(i);
            File baseDir = baseDirs.get(i);
            File srcFile = new File(baseDir, relativePath); 
            logVerbose("Preparing %s", srcFile);
            Instant startInstant = Instant.now();
            String input = TaskUtils.readFile(srcFile, charset);
            String inputHash = TaskUtils.calcHash(input, charset);

            // use file extension to parse as Java/Kotlin code.
            List<Token> tokens = new ArrayList<>();
            ParserInputSource inputSource = TaskUtils.parseSourceCode(relativePath, input, tokens);

            // Reset receiver variables.
            for (List<AugmentingCode> specAugCodes : specAugCodesList) {
                specAugCodes.clear();
            }

            List<ParserException> errors = new ArrayList<>();
        
            SourceFileDescriptor s = codeGenerationRequestCreator.processSourceFile(inputSource, tokens, 
                specAugCodesList, errors);
            if (s != null) {
                if (errorMap == null) {
                    // write out descriptor.
                    s.setDir(baseDir.getPath());
                    s.setRelativePath(relativePath);
                    s.setContentHash(inputHash);
                    s.serialize(resultWriter);

                    // serialize aug codes
                    for (int j = 0; j < requestSpecList.size(); j++) {
                        Object requestWriter = codeGenRequestWriters.get(j);
                        List<AugmentingCode> specAugCodes = specAugCodesList.get(j);
                        for (AugmentingCode specAugCode : specAugCodes) {
                            specAugCode.setRelativePath(s.getRelativePath());
                            specAugCode.serialize(requestWriter);
                        }
                    }
                }
            }
            else {
                // investigate errors.
                if (errorMap == null) {
                    errorMap = new LinkedHashMap<>();
                }
                log(String.format("%s error(s) encountered in %s", errors.size(), srcFile),
                    LogLevel.WARN.getLevel());
                errorMap.put(relativePath, errors);
            }
            
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            logVerbose("done in %s ms", timeElapsed);
        }

        // close writers
        prepResult.endSerialize(resultWriter);
        for (int i = 0; i < codeGenRequests.size(); i++) {
            CodeGenerationRequest codeGenRequest = codeGenRequests.get(i);
            Object codeGenRequestWriter = codeGenRequestWriters.get(i);
            codeGenRequest.endSerialize(codeGenRequestWriter);
        }

        // if there are errors, fail build
        if (errorMap != null) { 
            int errorCount = 0;
            for (Map.Entry<String, List<ParserException>> sourceFileErrors : errorMap.entrySet()) {
                String srcPath = sourceFileErrors.getKey();
                List<ParserException> errors = sourceFileErrors.getValue();
                for (ParserException error: errors) {
                    log("Error in " + srcPath + ":" + error, LogLevel.WARN.getLevel());
                    errorCount++;
                }
            }
            throw new BuildException(errorCount + "error(s) encountered.");
        }
    }

    private void logVerbose(String message, Object... args) {
        if (verbose) {
            log("[" + String.format(message, args) + "]");
        }
    }
}
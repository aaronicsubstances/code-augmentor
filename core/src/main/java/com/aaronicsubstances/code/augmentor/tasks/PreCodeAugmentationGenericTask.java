package com.aaronicsubstances.code.augmentor.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.parsing.Token;
import com.aaronicsubstances.code.augmentor.parsing.TokenSupplier;

public class PreCodeAugmentationGenericTask {
    public static final int LOG_LEVEL_VERBOSE = 1;
    public static final int LOG_LEVEL_INFO = 2;
    public static final int LOG_LEVEL_WARN = 3;
    private BiConsumer<Integer, Supplier<String>> logAppender;

    private List<String> relativePaths;
    private List<File> baseDirs;
    private List<String> genCodeStartSuffixes, genCodeEndSuffixes, embeddedStringDoubleSlashSuffixes;
    private List<List<String>> augCodeSuffixes;

    private Charset charset;
    private List<File> augCodeDestFiles;
    private File parseResultsFile;
    private File tempDir;

    private final List<ParserException> allErrors = new ArrayList<>();

    public void execute() throws Exception {
        PreCodeAugmentationResult prepResult = new PreCodeAugmentationResult();
        prepResult.setGenCodeStartSuffix(genCodeStartSuffixes.get(0));
        prepResult.setGenCodeEndSuffix(genCodeEndSuffixes.get(0));
        Object resultWriter = prepResult.beginSerialize(parseResultsFile);

        List<Object> codeGenRequestWriters = new ArrayList<>();
        List<CodeGenerationRequest> codeGenRequests = new ArrayList<>();
        for (File augCodeDestFile : augCodeDestFiles) {
            CodeGenerationRequest codeGenRequest = new CodeGenerationRequest();
            codeGenRequests.add(codeGenRequest);
            Object requestWriter = codeGenRequest.beginSerialize(augCodeDestFile);
            codeGenRequestWriters.add(requestWriter);
        }
         
        CodeGenerationRequestCreator codeGenerationRequestCreator =
            new CodeGenerationRequestCreator(
                genCodeStartSuffixes,
                genCodeEndSuffixes,
                embeddedStringDoubleSlashSuffixes,
                augCodeSuffixes);

        List<List<AugmentingCode>> specAugCodesList = new ArrayList<>();
        for (int i = 0; i < augCodeSuffixes.size(); i++) {
            specAugCodesList.add(new ArrayList<>());
        }

        allErrors.clear();

        for (int i = 0; i < relativePaths.size(); i++) {
            String relativePath = relativePaths.get(i);
            File baseDir = baseDirs.get(i);
            File srcFile = new File(baseDir, relativePath); 
            logVerbose("Preparing %s", srcFile);
            Instant startInstant = Instant.now();
            String input = TaskUtils.readFile(srcFile, charset);
            String inputHash = TaskUtils.calcHash(input, charset);

            // use file extension to parse as Java/Kotlin code.
            TokenSupplier tokenSupplier = TaskUtils.parseSourceCode(relativePath, input);
            List<Token> tokens = tokenSupplier.parse();

            // Reset receiver variables.
            for (List<AugmentingCode> specAugCodes : specAugCodesList) {
                specAugCodes.clear();
            }
            List<ParserException> errors = new ArrayList<>();
        
            SourceFileDescriptor s = codeGenerationRequestCreator.processSourceFile(
                tokenSupplier.getInputSource(), tokens, specAugCodesList, errors);
            if (s != null) {
                // don't bother to serialize any further if there are
                // previous errors.
                if (allErrors.isEmpty()) {
                    // write out descriptor.
                    s.setDir(baseDir.getPath());
                    s.setRelativePath(relativePath);
                    s.setContentHash(inputHash);
                    s.serialize(resultWriter);

                    // serialize aug codes
                    int identifiedAugCodeCount = 0;
                    for (int j = 0; j < codeGenRequestWriters.size(); j++) {
                        Object requestWriter = codeGenRequestWriters.get(j);
                        List<AugmentingCode> specAugCodes = specAugCodesList.get(j);
                        if (specAugCodes.isEmpty()) {
                            continue;
                        }
                        identifiedAugCodeCount += specAugCodes.size();
                        SourceFileAugmentingCode sourceFileAugCode = new SourceFileAugmentingCode(
                            specAugCodes);
                        sourceFileAugCode.setFileIndex(i);
                        sourceFileAugCode.setRelativePath(s.getRelativePath());
                        sourceFileAugCode.serialize(requestWriter);
                    }

                    if (identifiedAugCodeCount > 0) {
                        logInfo("%s aug code(s) identified in %s", identifiedAugCodeCount, srcFile);
                    }
                }
            }
            else {
                logWarn("%s error(s) encountered in %s", errors.size(), srcFile);
                for (ParserException e : errors) {
                    allErrors.add(new ParserException(e.getMessage(), e.getCause(),
                        e.getLineNumber(), e.getColumnNumber(), e.getSnippet(),
                        baseDir, relativePath));
                }
            }
            
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            logVerbose("done in %s ms", timeElapsed);
        }

        // close writers
        prepResult.endSerialize(resultWriter);
        for (int i = 0; i < codeGenRequestWriters.size(); i++) {
            Object codeGenRequestWriter = codeGenRequestWriters.get(i);
            CodeGenerationRequest codeGenRequest = codeGenRequests.get(i);
            codeGenRequest.endSerialize(codeGenRequestWriter);
        }
    }

    private void logVerbose(String format, Object... args) {
        if (logAppender == null) {
            return;
        }
        logAppender.accept(LOG_LEVEL_VERBOSE, () -> String.format(format, args));
    }

    private void logInfo(String format, Object... args) {
        if (logAppender == null) {
            return;
        }
        logAppender.accept(LOG_LEVEL_INFO, () -> String.format(format, args));        
    }

    private void logWarn(String format, Object... args) {
        if (logAppender == null) {
            return;
        }
        logAppender.accept(LOG_LEVEL_WARN, () -> String.format(format, args));        
    }

    public BiConsumer<Integer, Supplier<String>> getLogAppender() {
        return logAppender;
    }

    public void setLogAppender(BiConsumer<Integer, Supplier<String>> logAppender) {
        this.logAppender = logAppender;
    }

    public List<String> getRelativePaths() {
        return relativePaths;
    }

    public void setRelativePaths(List<String> relativePaths) {
        this.relativePaths = relativePaths;
    }

    public List<File> getBaseDirs() {
        return baseDirs;
    }

    public void setBaseDirs(List<File> baseDirs) {
        this.baseDirs = baseDirs;
    }

    public List<String> getGenCodeStartSuffixes() {
        return genCodeStartSuffixes;
    }

    public void setGenCodeStartSuffixes(List<String> genCodeStartSuffixes) {
        this.genCodeStartSuffixes = genCodeStartSuffixes;
    }

    public List<String> getGenCodeEndSuffixes() {
        return genCodeEndSuffixes;
    }

    public void setGenCodeEndSuffixes(List<String> genCodeEndSuffixes) {
        this.genCodeEndSuffixes = genCodeEndSuffixes;
    }

    public List<String> getEmbeddedStringDoubleSlashSuffixes() {
        return embeddedStringDoubleSlashSuffixes;
    }

    public void setEmbeddedStringDoubleSlashSuffixes(List<String> embeddedStringDoubleSlashSuffixes) {
        this.embeddedStringDoubleSlashSuffixes = embeddedStringDoubleSlashSuffixes;
    }

    public List<List<String>> getAugCodeSuffixes() {
        return augCodeSuffixes;
    }

    public void setAugCodeSuffixes(List<List<String>> augCodeSuffixes) {
        this.augCodeSuffixes = augCodeSuffixes;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public List<File> getAugCodeDestFiles() {
        return augCodeDestFiles;
    }

    public void setAugCodeDestFiles(List<File> augCodeDestFiles) {
        this.augCodeDestFiles = augCodeDestFiles;
    }

    public File getParseResultsFile() {
        return parseResultsFile;
    }

    public void setParseResultsFile(File parseResultsFile) {
        this.parseResultsFile = parseResultsFile;
    }

    public File getTempDir() {
        return tempDir;
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public List<ParserException> getAllErrors() {
        return allErrors;
    }
}
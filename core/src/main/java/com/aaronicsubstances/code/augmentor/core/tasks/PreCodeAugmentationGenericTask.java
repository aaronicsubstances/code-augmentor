package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.parsing.ParserException;

public class PreCodeAugmentationGenericTask {
    public static final int LOG_LEVEL_VERBOSE = 1;
    public static final int LOG_LEVEL_INFO = 2;
    public static final int LOG_LEVEL_WARN = 3;

    // input properties
    private BiConsumer<Integer, Supplier<String>> logAppender;
    private List<String> relativePaths;
    private List<File> baseDirs;
    private List<String> genCodeStartDirectives;
    private List<String> genCodeEndDirectives;
    private List<String> embeddedStringDirectives;
    private List<String> enableScanDirectives, disableScanDirectives;
    private List<List<String>> augCodeDirectives;
    private Charset charset;
    private List<File> augCodeDestFiles;
    private File prepFile;

    // output properties
    private final List<ParserException> allErrors = new ArrayList<>();

    public void execute() throws Exception {
        PreCodeAugmentationResult prepResult = new PreCodeAugmentationResult();
        prepResult.setGenCodeStartDirective(genCodeStartDirectives.get(0));
        prepResult.setGenCodeEndDirective(genCodeEndDirectives.get(0));
        Object resultWriter = prepResult.beginSerialize(prepFile);

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
                genCodeStartDirectives,
                genCodeEndDirectives,
                embeddedStringDirectives,
                augCodeDirectives,
                enableScanDirectives,
                disableScanDirectives);

        List<List<AugmentingCode>> specAugCodesList = new ArrayList<>();
        for (int i = 0; i < augCodeDirectives.size(); i++) {
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

            // Reset receiver variables.
            for (List<AugmentingCode> specAugCodes : specAugCodesList) {
                specAugCodes.clear();
            }
            List<ParserException> errors = new ArrayList<>();
        
            SourceFileDescriptor s = new SourceFileDescriptor();
            s.setFileIndex(i);
            s.setDir(baseDir.getPath());
            s.setRelativePath(relativePath);
            s.setContentHash(inputHash);
            codeGenerationRequestCreator.processSourceFile(s,
                input, specAugCodesList, errors);
            if (errors.isEmpty()) {
                // don't bother to serialize any further if there are
                // previous errors.
                int identifiedAugCodeCount = 0;
                if (allErrors.isEmpty()) {
                    // serialize aug codes
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
                        sourceFileAugCode.setRelativePath(relativePath);
                        sourceFileAugCode.serialize(requestWriter);
                    }
                }

                if (identifiedAugCodeCount > 0) {
                    logInfo("%s aug code(s) identified in %s", identifiedAugCodeCount, srcFile);

                    // write out descriptor.
                    s.serialize(resultWriter);
                }
            }
            else {
                logWarn("%s error(s) encountered in %s", errors.size(), srcFile);
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

    public File getPrepFile() {
        return prepFile;
    }

    public void setPrepFile(File prepFile) {
        this.prepFile = prepFile;
    }

    public List<String> getGenCodeStartDirectives() {
        return genCodeStartDirectives;
    }

    public void setGenCodeStartDirectives(List<String> genCodeStartDirectives) {
        this.genCodeStartDirectives = genCodeStartDirectives;
    }

    public List<String> getGenCodeEndDirectives() {
        return genCodeEndDirectives;
    }

    public void setGenCodeEndDirectives(List<String> genCodeEndDirectives) {
        this.genCodeEndDirectives = genCodeEndDirectives;
    }

    public List<String> getEmbeddedStringDirectives() {
        return embeddedStringDirectives;
    }

    public void setEmbeddedStringDirectives(List<String> embeddedStringDirectives) {
        this.embeddedStringDirectives = embeddedStringDirectives;
    }

    public List<String> getEnableScanDirectives() {
        return enableScanDirectives;
    }

    public void setEnableScanDirectives(List<String> enableScanDirectives) {
        this.enableScanDirectives = enableScanDirectives;
    }

    public List<String> getDisableScanDirectives() {
        return disableScanDirectives;
    }

    public void setDisableScanDirectives(List<String> disableScanDirectives) {
        this.disableScanDirectives = disableScanDirectives;
    }

    public List<List<String>> getAugCodeDirectives() {
        return augCodeDirectives;
    }

    public void setAugCodeDirectives(List<List<String>> augCodeDirectives) {
        this.augCodeDirectives = augCodeDirectives;
    }

    public List<ParserException> getAllErrors() {
        return allErrors;
    }
}
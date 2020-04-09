package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.util.ParserException;
import com.aaronicsubstances.code.augmentor.core.util.SourceCodeTokenizer;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;
import com.aaronicsubstances.code.augmentor.core.util.Token;

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
	private List<String> embeddedJsonDirectives;
    private List<String> enableScanDirectives;
    private List<String> disableScanDirectives;
    private List<AugCodeProcessingSpec> augCodeProcessingSpecs;
    private Charset charset;
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
        for (AugCodeProcessingSpec augCodeSpec : augCodeProcessingSpecs) {
            CodeGenerationRequest codeGenRequest = new CodeGenerationRequest();
            codeGenRequests.add(codeGenRequest);
            Object requestWriter = codeGenRequest.beginSerialize(augCodeSpec.getDestFile());
            codeGenRequestWriters.add(requestWriter);
        }

        List<List<String>> augCodeDirectiveSets = augCodeProcessingSpecs.stream().
            map(x -> x.getDirectives()).collect(Collectors.toList());
        SourceCodeTokenizer tokenizer = new SourceCodeTokenizer(
                genCodeStartDirectives, genCodeEndDirectives, 
                embeddedStringDirectives, embeddedJsonDirectives,
                enableScanDirectives, disableScanDirectives,
                augCodeDirectiveSets);

        List<List<AugmentingCode>> specAugCodesList = new ArrayList<>();
        for (int i = 0; i < augCodeProcessingSpecs.size(); i++) {
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

            List<Token> inputTokens = tokenizer.tokenizeSource(input);

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
            List<CodeSnippetDescriptor> bodySnippets = 
                CodeGenerationRequestCreator.processSourceFile(inputTokens, srcFile,
                    specAugCodesList, errors);
            if (errors.isEmpty()) {
                // don't bother to serialize any further if there are
                // previous errors.
                // also skip seriaize if no snippets were generated.
                if (allErrors.isEmpty() && !bodySnippets.isEmpty()) {
                    // write out descriptor.
                    s.setBodySnippets(bodySnippets);
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
                        SourceFileAugmentingCode sourceFileAugCode = new SourceFileAugmentingCode(specAugCodes);
                        sourceFileAugCode.setFileIndex(i);
                        sourceFileAugCode.setRelativePath(relativePath);
                        sourceFileAugCode.serialize(requestWriter);
                    }

                    logInfo("%s aug code(s) identified in %s", identifiedAugCodeCount, srcFile);
                }
            } else {
                logWarn("%s error(s) encountered in %s", errors.size(), srcFile);
                allErrors.addAll(errors);
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

	public List<String> getEmbeddedJsonDirectives() {
        return embeddedJsonDirectives;
	}

	public void setEmbeddedJsonDirectives(List<String> embeddedJsonDirectives) {
        this.embeddedJsonDirectives = embeddedJsonDirectives;
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

    public List<AugCodeProcessingSpec> getAugCodeProcessingSpecs() {
        return augCodeProcessingSpecs;
    }

    public void setAugCodeProcessingSpecs(List<AugCodeProcessingSpec> augCodeProcessingSpecs) {
        this.augCodeProcessingSpecs = augCodeProcessingSpecs;
    }

    public List<ParserException> getAllErrors() {
        return allErrors;
    }
}
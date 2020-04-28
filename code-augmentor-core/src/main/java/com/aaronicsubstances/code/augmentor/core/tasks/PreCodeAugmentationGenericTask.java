package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.util.CodeGenerationRequestCreator;
import com.aaronicsubstances.code.augmentor.core.util.SourceCodeTokenizer;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;
import com.aaronicsubstances.code.augmentor.core.util.Token;

public class PreCodeAugmentationGenericTask {
    // input properties
    private BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender;
    private List<String> relativePaths;
    private List<File> baseDirs;
    private List<String> genCodeStartDirectives;
    private List<String> genCodeEndDirectives;
    private List<String> embeddedStringDirectives;
	private List<String> embeddedJsonDirectives;
    private List<String> skipCodeStartDirectives;
    private List<String> skipCodeEndDirectives;
    private List<String> inlineGenCodeDirectives;
    private List<String> nestedLevelStartMarkers;
    private List<String> nestedLevelEndMarkers;
    private List<AugCodeProcessingSpec> augCodeProcessingSpecs;
    private Charset charset;
    private File prepFile;

    // output properties
    private final List<Throwable> allErrors = new ArrayList<>();

    public void execute() throws Exception {
        allErrors.clear();

        PreCodeAugmentationResult prepResult = new PreCodeAugmentationResult();
        prepResult.setGenCodeStartDirective(genCodeStartDirectives.get(0));
        prepResult.setGenCodeEndDirective(genCodeEndDirectives.get(0));
        // ensure dir exists for prepFile
        prepFile.getParentFile().mkdirs();
        Object resultWriter = prepResult.beginSerialize(prepFile);

        List<Object> codeGenRequestWriters = new ArrayList<>();
        List<CodeGenerationRequest> codeGenRequests = new ArrayList<>();
        for (AugCodeProcessingSpec augCodeSpec : augCodeProcessingSpecs) {
            CodeGenerationRequest codeGenRequest = new CodeGenerationRequest();
            codeGenRequests.add(codeGenRequest);

            // initialize header.
            codeGenRequest.setGenCodeStartDirective(prepResult.getGenCodeStartDirective());
            codeGenRequest.setGenCodeEndDirective(prepResult.getGenCodeEndDirective());
            codeGenRequest.setAugCodeDirective(augCodeSpec.getDirectives().get(0));
            codeGenRequest.setEmbeddedStringDirective(embeddedStringDirectives.get(0));
            codeGenRequest.setEmbeddedJsonDirective(embeddedJsonDirectives.get(0));
            if (skipCodeStartDirectives != null && !skipCodeStartDirectives.isEmpty()) {
                codeGenRequest.setSkipCodeStartDirective(skipCodeStartDirectives.get(0));
            }
            if (skipCodeEndDirectives != null && !skipCodeEndDirectives.isEmpty()) {
                codeGenRequest.setSkipCodeEndDirective(skipCodeEndDirectives.get(0));
            }
            if (inlineGenCodeDirectives != null && !inlineGenCodeDirectives.isEmpty()) {
                codeGenRequest.setInlineGenCodeDirective(inlineGenCodeDirectives.get(0));
            }
            if (nestedLevelStartMarkers != null && !nestedLevelStartMarkers.isEmpty()) {
                codeGenRequest.setNestedLevelStartMarker(nestedLevelStartMarkers.get(0));
            }
            if (nestedLevelEndMarkers != null && !nestedLevelEndMarkers.isEmpty()) {
                codeGenRequest.setNestedLevelEndMarker(nestedLevelEndMarkers.get(0));
            }

            // ensure dir exists for destFile
            augCodeSpec.getDestFile().getParentFile().mkdirs();
            Object requestWriter = codeGenRequest.beginSerialize(augCodeSpec.getDestFile());
            codeGenRequestWriters.add(requestWriter);
        }

        List<List<String>> augCodeDirectiveSets = augCodeProcessingSpecs.stream().
            map(x -> x.getDirectives()).collect(Collectors.toList());
        SourceCodeTokenizer tokenizer = new SourceCodeTokenizer(
                genCodeStartDirectives, genCodeEndDirectives, 
                embeddedStringDirectives, embeddedJsonDirectives,
                skipCodeStartDirectives, skipCodeEndDirectives,
                augCodeDirectiveSets, inlineGenCodeDirectives,
                nestedLevelStartMarkers, nestedLevelEndMarkers);

        List<List<AugmentingCode>> specAugCodesList = new ArrayList<>();
        for (int i = 0; i < augCodeProcessingSpecs.size(); i++) {
            specAugCodesList.add(new ArrayList<>());
        }

        Set<String> processedSrcPaths = new HashSet<>();
        for (int i = 0; i < relativePaths.size(); i++) {
            String relativePath = relativePaths.get(i);
            File baseDir = baseDirs.get(i);
            File srcFile = new File(baseDir, relativePath);
            String normalizedSrcPath = srcFile.getCanonicalPath();
            if (processedSrcPaths.contains(normalizedSrcPath)) {
                TaskUtils.logVerbose(logAppender, "Processed %s already", srcFile);
                continue;
            }
            processedSrcPaths.add(normalizedSrcPath);
            TaskUtils.logVerbose(logAppender, "Preparing %s", srcFile);
            Instant startInstant = Instant.now();
            String input = TaskUtils.readFile(srcFile, charset);
            String inputHash = TaskUtils.calcHash(input, charset);

            List<Token> inputTokens = tokenizer.tokenizeSource(input);

            // Reset receiver variables.
            for (List<AugmentingCode> specAugCodes : specAugCodesList) {
                specAugCodes.clear();
            }
            List<Exception> errors = new ArrayList<>();

            SourceFileDescriptor s = new SourceFileDescriptor();
            s.setFileId(i + 1); // 1-based, so 0 signals not set.
            s.setDir(baseDir.getPath());
            s.setRelativePath(relativePath);
            s.setContentHash(inputHash);
            List<CodeSnippetDescriptor> codeSnippets = 
                CodeGenerationRequestCreator.processSourceFile(inputTokens, srcFile,
                    specAugCodesList, errors);
            if (errors.isEmpty()) {
                int identifiedAugCodeCount = 0;
                // don't bother to serialize any further if there are
                // previous errors.
                // also skip seriaize if no snippets were generated.
                if (allErrors.isEmpty() && !codeSnippets.isEmpty()) {
                    // write out descriptor.
                    s.setCodeSnippets(codeSnippets);
                    s.serialize(resultWriter);

                    // serialize aug codes
                    for (int j = 0; j < codeGenRequestWriters.size(); j++) {
                        Object requestWriter = codeGenRequestWriters.get(j);
                        List<AugmentingCode> specAugCodes = specAugCodesList.get(j);
                        if (specAugCodes.isEmpty()) {
                            continue;
                        }
                        identifiedAugCodeCount += specAugCodes.size();
                        SourceFileAugmentingCode sourceFileAugCode = new SourceFileAugmentingCode(specAugCodes);
                        sourceFileAugCode.setFileId(s.getFileId());
                        sourceFileAugCode.setDir(s.getDir());
                        sourceFileAugCode.setRelativePath(s.getRelativePath());
                        sourceFileAugCode.serialize(requestWriter);
                    }
                }
                    
                if (identifiedAugCodeCount == 0) {
                    TaskUtils.logVerbose(logAppender, "0 aug codes identified in %s", srcFile);
                }
                else {
                    TaskUtils.logInfo(logAppender, "%s aug code(s) identified in %s", identifiedAugCodeCount, srcFile);
                }
            } 
            else {
                TaskUtils.logWarn(logAppender, "%s error(s) encountered in %s", errors.size(), srcFile);
                allErrors.addAll(errors);
            }

            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            TaskUtils.logInfo(logAppender, "Done processing %s in %d ms", srcFile, timeElapsed);
        }

        // close writers
        prepResult.endSerialize(resultWriter);
        for (int i = 0; i < codeGenRequestWriters.size(); i++) {
            Object codeGenRequestWriter = codeGenRequestWriters.get(i);
            CodeGenerationRequest codeGenRequest = codeGenRequests.get(i);
            codeGenRequest.endSerialize(codeGenRequestWriter);
        }
    }

    public BiConsumer<GenericTaskLogLevel, Supplier<String>> getLogAppender() {
        return logAppender;
    }

    public void setLogAppender(BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender) {
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

    public List<String> getSkipCodeStartDirectives() {
        return skipCodeStartDirectives;
    }

    public void setSkipCodeStartDirectives(List<String> skipCodeEndDirectives) {
        this.skipCodeStartDirectives = skipCodeEndDirectives;
    }

    public List<String> getSkipCodeEndDirectives() {
        return skipCodeEndDirectives;
    }

    public void setSkipCodeEndDirectives(List<String> skipCodeEndDirectives) {
        this.skipCodeEndDirectives = skipCodeEndDirectives;
    }

    public List<String> getInlineGenCodeDirectives() {
        return inlineGenCodeDirectives;
    }

    public void setInlineGenCodeDirectives(List<String> inlineGenCodeDirectives) {
        this.inlineGenCodeDirectives = inlineGenCodeDirectives;
    }

    public List<String> getNestedLevelStartMarkers() {
        return nestedLevelStartMarkers;
    }

    public void setNestedLevelStartMarkers(List<String> nestedLevelStartMarkers) {
        this.nestedLevelStartMarkers = nestedLevelStartMarkers;
    }

    public List<String> getNestedLevelEndMarkers() {
        return nestedLevelEndMarkers;
    }

    public void setNestedLevelEndMarkers(List<String> nestedLevelEndMarkers) {
        this.nestedLevelEndMarkers = nestedLevelEndMarkers;
    }

    public List<AugCodeProcessingSpec> getAugCodeProcessingSpecs() {
        return augCodeProcessingSpecs;
    }

    public void setAugCodeProcessingSpecs(List<AugCodeProcessingSpec> augCodeProcessingSpecs) {
        this.augCodeProcessingSpecs = augCodeProcessingSpecs;
    }

    public List<Throwable> getAllErrors() {
        return allErrors;
    }
}
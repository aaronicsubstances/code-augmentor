package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

/**
 * Implements preparation stage of Code Augmentor, which is the first stage.
 */
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

    /**
     * Executes prepare stage of Code Augmentor. Upon successful completion
     * prepFile will be created for use as input to the completion stage.
     * Each {@link AugCodeProcessingSpec#getDestFile()} specified will be created for 
     * use as input to the processing stage.
     * @throws Exception
     */
    public void execute() throws Exception {
        // validate input properties
        Objects.requireNonNull(baseDirs, "baseDirs property is not set");
        Objects.requireNonNull(relativePaths, "relativePaths property is not set");
        Objects.requireNonNull(prepFile, "prepFile property is not set");
        Objects.requireNonNull(augCodeProcessingSpecs, "augCodeProcessingSpecs property is not set");
        Objects.requireNonNull(charset, "charset property is not set");

        allErrors.clear();

        PreCodeAugmentationResult prepResult = new PreCodeAugmentationResult();
        prepResult.setEncoding(charset.name());
        if (genCodeStartDirectives != null && !genCodeStartDirectives.isEmpty()) {
            prepResult.setGenCodeStartDirective(genCodeStartDirectives.get(0));
        }
        if (genCodeEndDirectives != null && !genCodeEndDirectives.isEmpty()) {
            prepResult.setGenCodeEndDirective(genCodeEndDirectives.get(0));
        }
        // ensure dir exists for prepFile
        if (prepFile.getParentFile() != null) {
            prepFile.getParentFile().mkdirs();
        }
        Object resultWriter = prepResult.beginSerialize(prepFile);

        List<Object> codeGenRequestWriters = new ArrayList<>();
        List<CodeGenerationRequest> codeGenRequests = new ArrayList<>();

        for (AugCodeProcessingSpec augCodeSpec : augCodeProcessingSpecs) {
            CodeGenerationRequest codeGenRequest = new CodeGenerationRequest();
            codeGenRequests.add(codeGenRequest);

            // initialize header.
            codeGenRequest.setGenCodeStartDirective(prepResult.getGenCodeStartDirective());
            codeGenRequest.setGenCodeEndDirective(prepResult.getGenCodeEndDirective());
            if (augCodeSpec.getDirectives() != null && !augCodeSpec.getDirectives().isEmpty()) {
                codeGenRequest.setAugCodeDirective(augCodeSpec.getDirectives().get(0));
            }
            if (embeddedStringDirectives != null && !embeddedStringDirectives.isEmpty()) {
                codeGenRequest.setEmbeddedStringDirective(embeddedStringDirectives.get(0));
            }
            if (embeddedJsonDirectives != null && !embeddedJsonDirectives.isEmpty()) {
                codeGenRequest.setEmbeddedJsonDirective(embeddedJsonDirectives.get(0));
            }
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
            File destFileParent = augCodeSpec.getDestFile().getParentFile();
            if (destFileParent != null) {
                destFileParent.mkdirs();
            }
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
                // used to also skip if no snippets were found, but not anymore
                // in order to cater for code change detection being disabled
                // (since in that case we want all files to show up in generated file 
                // regardless).
                if (allErrors.isEmpty()) {
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

    /**
     * Sets logging procedure for this task. By default this property is null, 
     * and so no logging is done.
     * @param logAppender
     */
    public void setLogAppender(BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender) {
        this.logAppender = logAppender;
    }

    public List<String> getRelativePaths() {
        return relativePaths;
    }

    /**
     * Sets the relative paths of the source files which will be processed by this
     * task. Each path is relative to the entry in baseDirs property at the same
     * index as the path here.
     * @param relativePaths should be of the same size as baseDirs property.
     */
    public void setRelativePaths(List<String> relativePaths) {
        this.relativePaths = relativePaths;
    }

    public List<File> getBaseDirs() {
        return baseDirs;
    }

    /**
     * Sets the directories of the source file sets which will be processed by this
     * task. The paths of the files in a directory entry of this property are specified by
     * the entry in relativePaths property at the same index as the directory here.
     * @param baseDirs should be of the same size as relativePaths property
     */
    public void setBaseDirs(List<File> baseDirs) {
        this.baseDirs = baseDirs;
    }

    public Charset getCharset() {
        return charset;
    }

    /**
     * Sets the encoding of files which will be read and processed by this task.
     * @param charset
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public File getPrepFile() {
        return prepFile;
    }

    /**
     * Sets the path of the prepFile output from this task. prepFile is currently
     * not meant for consumption by library clients, as it contains data mostly of
     * interest to the completion stage of Code Augmentor. It exists only because
     * prepare stage is separated from completion stage by a processing stage. 
     * Thus library clients should just see this file as something to keep around
     * during processing stage and just hand it over to completion stage.
     * @param prepFile
     */
    public void setPrepFile(File prepFile) {
        this.prepFile = prepFile;
    }

    public List<String> getGenCodeStartDirectives() {
        return genCodeStartDirectives;
    }

    /**
     * This property defines generated code directives for this task.
     * A generated code start directive is text which if found as a prefix of
     * a line, indicates that the next line begins a generated code section. The generated 
     * code section will then have to end with a corresponding generated code end
     * directive.
     * @param genCodeStartDirectives
     */
    public void setGenCodeStartDirectives(List<String> genCodeStartDirectives) {
        this.genCodeStartDirectives = genCodeStartDirectives;
    }

    public List<String> getGenCodeEndDirectives() {
        return genCodeEndDirectives;
    }

    /**
     * Defines generated code end directives for this task. Each directive is text
     * which if found to start a line, will causes this task to consider the
     * previous line as the last line of a generated code section.
     * @param genCodeEndDirectives
     * 
     * @see #setGenCodeStartDirectives(List)
     */
    public void setGenCodeEndDirectives(List<String> genCodeEndDirectives) {
        this.genCodeEndDirectives = genCodeEndDirectives;
    }

    public List<String> getEmbeddedStringDirectives() {
        return embeddedStringDirectives;
    }

    /**
     * This property defines embedded string directives for this task.
     * Inside an augmenting code section, directives can be used to
     * specify that a portion of the augmenting code section is meant
     * to be used a string by processing stage.
     * Each directive is text which if found as the prefix of a line
     * inside an augmenting code section, will cause this task to
     * treat the remainder of line after the directive as an embedded string.
     * A contiguous set of such lines will be treated together as one embedded string.
     * @param embeddedStringDirectives
     * 
     * @see #setAugCodeProcessingSpecs(List)
     */
    public void setEmbeddedStringDirectives(List<String> embeddedStringDirectives) {
        this.embeddedStringDirectives = embeddedStringDirectives;
    }

	public List<String> getEmbeddedJsonDirectives() {
        return embeddedJsonDirectives;
	}

    /**
     * This property defines embedded JSON directives for this task.
     * Inside an augmenting code section, directives can be used to
     * specify that a portion of the augmenting code section is meant
     * to be used a JSON value by processing stage.
     * Each directive is text which if found as the prefix of a line
     * inside an augmenting code section, will cause this task to
     * treat the remainder of line after the directive as an embedded JSON value.
     * A contiguous set of such lines will be treated together as one embedded JSON value.
     * @param embeddedJsonDirectives
     * 
     * @see #setAugCodeProcessingSpecs(List)
     */
	public void setEmbeddedJsonDirectives(List<String> embeddedJsonDirectives) {
        this.embeddedJsonDirectives = embeddedJsonDirectives;
	}

    public List<String> getSkipCodeStartDirectives() {
        return skipCodeStartDirectives;
    }

    /**
     * This property defines skip code start directives. Each directive is text
     * which if found to start a line, will cause this task to skip processing
     * augmenting code sections until a skip code end directive is encountered.
     * This task uses this property to skip over sections of source file. Generated 
     * code sections will however not be skipped over, and so it is an error for generated and
     * skip code sections to overlap.
     * @param skipCodeEndDirectives
     */
    public void setSkipCodeStartDirectives(List<String> skipCodeEndDirectives) {
        this.skipCodeStartDirectives = skipCodeEndDirectives;
    }

    public List<String> getSkipCodeEndDirectives() {
        return skipCodeEndDirectives;
    }

    /**
     * Defines skip code end directives for this task. Each directive is text
     * which if found to start a line, will causes this task to end
     * skipping of source file lines, and begin parsing augmenting code
     * sections again.
     * @param skipCodeEndDirectives
     * 
     * @see #setSkipCodeStartDirectives(List)
     */
    public void setSkipCodeEndDirectives(List<String> skipCodeEndDirectives) {
        this.skipCodeEndDirectives = skipCodeEndDirectives;
    }

    public List<String> getInlineGenCodeDirectives() {
        return inlineGenCodeDirectives;
    }

    /**
     * This property defines for this task inline generated code directives. Each
     * directive is text which if found to start a line, will cause this task
     * to consider the remainder of the line after the text as a generated code
     * section. Thus with this directive the number of lines needed to represent
     * a generated code section can be reduced by 2 (no start/end directives), but
     * on the other hand requires every inline generated code section to start with directive.
     * A contiguous set of lines beginning with one of these directives will be treated by
     * this task as a single generated code section.
     * @param inlineGenCodeDirectives
     */
    public void setInlineGenCodeDirectives(List<String> inlineGenCodeDirectives) {
        this.inlineGenCodeDirectives = inlineGenCodeDirectives;
    }

    public List<String> getNestedLevelStartMarkers() {
        return nestedLevelStartMarkers;
    }

    /**
     * Sets the markers which this task will use to nest and assign nested levels to
     * augmenting code sections. Each marker is text which if found to follow an
     * augmenting code directive (embedded string and JSON directives excluded),
     * will cause this task to assign to subsequent augmenting code section
     * 1 plus the nested level of the augmeting code section the marker was found in.
     * <p>
     * Nested level numbering start from 0.
     * At the end of parsing a source file, appropriate nested level end markers must
     * be used to end the nested level with 0 or an error occurs.
     * @param nestedLevelStartMarkers
     * 
     * @see #setAugCodeProcessingSpecs(List)
     */
    public void setNestedLevelStartMarkers(List<String> nestedLevelStartMarkers) {
        this.nestedLevelStartMarkers = nestedLevelStartMarkers;
    }

    public List<String> getNestedLevelEndMarkers() {
        return nestedLevelEndMarkers;
    }

    /**
     * Sets the markers which this task uses to detect the end of nesting of augmenting 
     * code sections. Each marker is text which if found to follow an augmenting code
     * directive (embedded string and JSON excluded) will cause this task to
     * decrease by 1 the nested level number it would otherwise have assigned to the augmenting code 
     * section the marker was found in.
     * Subsequent augmenting code sections will be assigned the decreased 
     * nested level number.
     * <p>
     * Nested level numbers cannot be negative. It is therefore an error to encounter
     * a nested level end marker when nested level number to be assigned is 0. That will mean
     * that a corresponding nested level start marker was not previously seen.
     * @param nestedLevelEndMarkers
     * 
     * @see #setAugCodeProcessingSpecs(List)
     */
    public void setNestedLevelEndMarkers(List<String> nestedLevelEndMarkers) {
        this.nestedLevelEndMarkers = nestedLevelEndMarkers;
    }

    public List<AugCodeProcessingSpec> getAugCodeProcessingSpecs() {
        return augCodeProcessingSpecs;
    }

    /**
     * Sets both augmenting code directives and the output file for storing the
     * augmenting code associated with these directives. Each directive is text which 
     * if found to start a line, will cause this task to take the remainder of the line
     * after the directive as an augmenting code section. A contiguous set of lines
     * starting with these directives, or embedded string/JSON directives will be treated
     * as a single augmenting code section.
     * <p>
     * The data of an augmenting code section (remainders of lines with augmenting code directives
     * or embedded string/JSON directives) are stored in files for use as input to the processing
     * stage.
     * <p>
     * This property can be set with one or more output files to enable source files to 
     * have augmenting codes which are not all processed in the same way. For each
     * output file, this task only writes to it augmenting code sections identified by the 
     * augmenting code directives that are assigned to that output file. Thus preparation stage
     * can produce multiple files containing augmenting codes, and for that reason completion stage
     * too can receive multiple files containing generated codes.
     * @param augCodeProcessingSpecs
     */
    public void setAugCodeProcessingSpecs(List<AugCodeProcessingSpec> augCodeProcessingSpecs) {
        this.augCodeProcessingSpecs = augCodeProcessingSpecs;
    }

    /**
     * Gets the error results of executing this task.
     * @return empty list if task execution was successful; non-empty list if
     * task execution failed.
     */
    public List<Throwable> getAllErrors() {
        return allErrors;
    }
}
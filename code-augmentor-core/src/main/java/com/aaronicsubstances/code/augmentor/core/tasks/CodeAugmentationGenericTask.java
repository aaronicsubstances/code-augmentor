package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponseChangeSet;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetChangeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileChangeSet;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.util.CodeGenerationResponseProcessor;
import com.aaronicsubstances.code.augmentor.core.util.GeneratedCodeFetcher;
import com.aaronicsubstances.code.augmentor.core.util.GeneratedCodeSimilarityChecker;
import com.aaronicsubstances.code.augmentor.core.util.SourceCodeTransformer;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

public class CodeAugmentationGenericTask {
    private static final String changeSummaryFileName = "CHANGE-SUMMARY.txt";
    private static final String changeDetailsFileName = "CHANGE-DETAILS.json";

    // input properties.
    private BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender;
    private Charset charset;
    private File prepFile;
    private List<File> generatedCodeFiles;
    private File srcDir;
    private File destDir;
    private boolean codeChangeDetectionDisabled;

    // output properties
    private final List<Throwable> allErrors = new ArrayList<>();
    private final List<File> srcFiles = new ArrayList<>();
    private final List<File> destFiles = new ArrayList<>();
    private final Map<String, String> destSubDirNameMap  = new HashMap<>();
    
    public void execute() throws Exception {
        allErrors.clear();
        srcFiles.clear();
        destFiles.clear();
        destSubDirNameMap.clear();
        destDir.mkdirs();

        PreCodeAugmentationResult result = new PreCodeAugmentationResult();
        Object resultReader = result.beginDeserialize(prepFile);

        GeneratedCodeFetcher generatedCodeFetcher = new GeneratedCodeFetcher(generatedCodeFiles);
        
        CodeGenerationResponseChangeSet resultChangeSet = null;
        Object resultChangeSetWriter = null;
        PrintWriter resultChangeSummaryWriter = null;
        if (!codeChangeDetectionDisabled) {
            resultChangeSet = new CodeGenerationResponseChangeSet();
            resultChangeSetWriter = resultChangeSet.beginSerialize(new File(
                destDir, changeDetailsFileName));
            
            // use OS platform default charset encoding for change summary 
            // since it is intended to be simple enough for shell scripts.
            resultChangeSummaryWriter = new PrintWriter(new File(destDir,
                changeSummaryFileName));
        }

        SourceFileDescriptor sourceFileDescriptor;
        while ((sourceFileDescriptor = SourceFileDescriptor.deserialize(resultReader)) != null) {
            // use this for testing only. under normal circumstances dir should be set.
            if (sourceFileDescriptor.getDir() == null) {
                sourceFileDescriptor.setDir(srcDir.getPath());
            }
            File srcFile = new File(sourceFileDescriptor.getDir(), sourceFileDescriptor.getRelativePath());
            TaskUtils.logVerbose(logAppender, "Processing %s", srcFile);

            Instant startInstant = Instant.now();
            String sourceCode = TaskUtils.readFile(srcFile, charset);
            if (sourceFileDescriptor.getContentHash() != null) {
                String inputHash = TaskUtils.calcHash(sourceCode, charset);
                if (!inputHash.equals(sourceFileDescriptor.getContentHash())) {
                    GenericTaskException fileIntegrityError = createException(
                            "Source file has changed unexpectedly. Regeneration required.", null, srcFile);
                    TaskUtils.logWarn(logAppender, fileIntegrityError.getMessage());
                    allErrors.add(fileIntegrityError);
                    continue;
                }
            }

            if (!generatedCodeFetcher.prepareForFile(sourceFileDescriptor.getFileId())) {
                GenericTaskException missingFileAugCodesError = createException(
                        "Could not locate generated codes for file with id " + sourceFileDescriptor.getFileId(), null,
                        srcFile);
                TaskUtils.logWarn(logAppender, missingFileAugCodesError.getMessage());
                allErrors.add(missingFileAugCodesError);
                continue;
            }

            int beginErrorCount = allErrors.size();

            // fetch applicable generated code per aug code descriptor.
            List<GeneratedCode> generatedCodes = new ArrayList<>();
            List<int[]> replacementRanges = new ArrayList<>();
            for (CodeSnippetDescriptor snippetDescriptor : sourceFileDescriptor.getCodeSnippets()) {
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                GeneratedCode genCode = generatedCodeFetcher.getGeneratedCode(sourceFileDescriptor.getFileId(),
                        augCodeDescriptor.getId());
                if (genCode == null) {
                    allErrors.add(createException("Could not find generated code with id " + augCodeDescriptor.getId(),
                            augCodeDescriptor, srcFile));
                } else {
                    // Don't process skipped aug codes.
                    if (genCode.isSkipped()) {
                        continue;
                    }
                    validateContentParts(genCode, augCodeDescriptor, srcFile, allErrors);
                }

                // as long as there are errors from either previous iterations or
                // this current one, don't proceed further.
                if (!allErrors.isEmpty()) {
                    continue;
                }

                generatedCodes.add(genCode);

                int[] replacementRange = CodeGenerationResponseProcessor.determineReplacementRange(snippetDescriptor,
                        genCode);
                replacementRanges.add(replacementRange);

                // modify content parts to end with newline if necessary and
                // correct split CR-LFs
                String newline = augCodeDescriptor.getLineSeparator();
                ContentPart lastContentPart = genCode.getContentParts().get(genCode.getContentParts().size() - 1);
                lastContentPart.setContent(
                        CodeGenerationResponseProcessor.ensureEndingNewline(lastContentPart.getContent(), newline));
                CodeGenerationResponseProcessor.repairSplitCrLfs(genCode.getContentParts());

                // format content parts to consititute replacement text if possible.
                String indent = CodeGenerationResponseProcessor.getEffectiveIndent(augCodeDescriptor, genCode);
                if (!indent.isEmpty()) {
                    CodeGenerationResponseProcessor.indentCode(genCode.getContentParts(), indent);
                }
                if (CodeGenerationResponseProcessor.shouldWrapInGenCodeDirectives(genCode,
                        snippetDescriptor.getGeneratedCodeDescriptor())) {
                    // employ default behaviour of ensuring generated code
                    // occurs within directive markers.
                    List<ContentPart> updatedContentParts = new ArrayList<>(genCode.getContentParts());
                    updatedContentParts.add(0,
                            new ContentPart(indent + result.getGenCodeStartDirective() + newline, false));
                    updatedContentParts.add(new ContentPart(indent + result.getGenCodeEndDirective() + newline, false));
                    genCode.setContentParts(updatedContentParts);
                }
            }

            // don't waste time merging changes if there are errors from previous
            // iterations or this current one.
            if (!allErrors.isEmpty()) {
                if (allErrors.size() > beginErrorCount) {
                    TaskUtils.logWarn(logAppender, "%s error(s) encountered in %s", allErrors.size() - beginErrorCount,
                            srcFile);
                }
                continue;
            }

            // Now merge generated code into source code,
            // and try and detect changes.
            List<CodeSnippetChangeDescriptor> changes = new ArrayList<>();
            SourceCodeTransformer transformer = new SourceCodeTransformer(sourceCode);
            if (codeChangeDetectionDisabled) {
                for (int i = 0; i < generatedCodes.size(); i++) {
                    GeneratedCode genCode = generatedCodes.get(i);
                    String replacementText = genCode.getWholeContent();
                    int[] replacementRange = replacementRanges.get(i);
                    transformer.addTransform(replacementText, replacementRange[0], replacementRange[1]);
                }
            }
            else {
                int changeLen = 0;
                for (int i = 0; i < generatedCodes.size(); i++) {
                    GeneratedCode genCode = generatedCodes.get(i);
                    String replacementText = genCode.getWholeContent();
                    int[] replacementRange = replacementRanges.get(i);
                    String textToBeReplaced = sourceCode.substring(replacementRange[0], replacementRange[1]);

                    // check for changes if there may be changes.
                    CodeSnippetChangeDescriptor codeChange;
                    if (textToBeReplaced.equals(replacementText)) {
                        // definitely there are no changes if texts are exactly equal.
                        codeChange = null;
                    }
                    else {
                        // determine whether changes are superficial or significant.
                        GeneratedCodeSimilarityChecker similarityAlg = new GeneratedCodeSimilarityChecker(
                                genCode.getContentParts());
                        codeChange = similarityAlg.match(textToBeReplaced);
                        if (codeChange != null) {
                            codeChange.setId(genCode.getId());
                            setSrcLineAndColumnNumbers(codeChange, sourceCode, replacementRange[0]);
                            setDestLineAndColumnNumbers(codeChange, transformer, replacementRange[0],
                                changeLen);
                        }
                    }
                    
                    if (codeChange != null) {
                        changes.add(codeChange);
                        transformer.addTransform(replacementText, replacementRange[0], replacementRange[1]);
                        changeLen += replacementText.length() - textToBeReplaced.length();
                    }
                }
            }

            // always generate files if code change detection is disabled.

            if (!codeChangeDetectionDisabled && changes.isEmpty()) {
                TaskUtils.logVerbose(logAppender, "No changes needed for %s", srcFile);
            } else {
                String destSubDirName = destSubDirNameMap.get(sourceFileDescriptor.getDir());
                if (destSubDirName == null) {
                    destSubDirName = new File(sourceFileDescriptor.getDir()).getName();
                    destSubDirName = TaskUtils.modifyNameToBeAbsent(destSubDirNameMap.values(), destSubDirName);
                    destSubDirNameMap.put(sourceFileDescriptor.getDir(), destSubDirName);
                }
                File destSubDir = new File(destDir, destSubDirName);
                File destFile = new File(destSubDir, sourceFileDescriptor.getRelativePath());
                destFile.getParentFile().mkdirs();

                String transformedCode = transformer.getTransformedText();
                TaskUtils.writeFile(destFile, charset, transformedCode);
                srcFiles.add(srcFile);
                destFiles.add(destFile);

                if (!codeChangeDetectionDisabled) {
                    // write out change summary.
                    // normalize file paths for intended shell scripts.
                    resultChangeSummaryWriter.println(srcFile.getCanonicalPath());
                    resultChangeSummaryWriter.println(destFile.getCanonicalPath());

                    // write out change details
                    SourceFileChangeSet s = new SourceFileChangeSet(changes);
                    s.setFileId(sourceFileDescriptor.getFileId());
                    s.setRelativePath(sourceFileDescriptor.getRelativePath());
                    s.setSrcDir(sourceFileDescriptor.getDir());
                    s.setDestDir(destSubDir.getPath());
                    s.serialize(resultChangeSetWriter);
                }

                TaskUtils.logInfo(logAppender, "Changes needed for %s successfully written to\n %s", srcFile, destFile);
            }

            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            TaskUtils.logInfo(logAppender, "Done processing %s in %d ms", srcFile, timeElapsed);
        }

        // close readers
        result.endDeserialize(resultReader);
        generatedCodeFetcher.close();

        // close writers
        if (!codeChangeDetectionDisabled) {
            resultChangeSet.endSerialize(resultChangeSetWriter);
            resultChangeSummaryWriter.close();
        }
    }

    private static void validateContentParts(
            GeneratedCode genCode,
            AugmentingCodeDescriptor augCodeDescriptor, File srcFile, 
            List<Throwable> errors) {
        List<ContentPart> parts = genCode.getContentParts();
        if (parts == null || parts.isEmpty()) {
            errors.add(createException("Found null/empty content parts",
                augCodeDescriptor, srcFile));
            return;
        }
        for (int i = 0; i < parts.size(); i++) {
            ContentPart part = parts.get(i);
            if (part == null || part.getContent() == null) {
                errors.add(createException("Found null part/content at index " + i,
                    augCodeDescriptor, srcFile));
            }
        }
    }

    private static void setSrcLineAndColumnNumbers(CodeSnippetChangeDescriptor codeChange,
            String sourceCode, int replacementRangeStart) {
        codeChange.setSrcCharIndex(codeChange.getCharIndex() + replacementRangeStart);
        int[] result = LexerSupport.calculateLineAndColumnNumbers(sourceCode, 
            codeChange.getSrcCharIndex());
        codeChange.setSrcLineNumber(result[0]);
        codeChange.setSrcColumnNumber(result[1]);
    }

    private static void setDestLineAndColumnNumbers(CodeSnippetChangeDescriptor codeChange,
            SourceCodeTransformer sourceCode, int replacementRangeStart, int changeLen) {
        codeChange.setDestCharIndex(codeChange.getCharIndex() + replacementRangeStart +
            changeLen);
        int[] result = LexerSupport.calculateLineAndColumnNumbers(sourceCode.getTransformedText(), 
            codeChange.getDestCharIndex());
        codeChange.setDestLineNumber(result[0]);
        codeChange.setDestColumnNumber(result[1]);
    }

    private static GenericTaskException createException(String errorMessage, 
            AugmentingCodeDescriptor augCodeDescriptor, File srcFile) {
        String srcPath = null;
        if (srcFile != null) {
            srcPath = srcFile.getPath();
        }
        int lineNumber = 0;
        if (augCodeDescriptor != null) {
            lineNumber = augCodeDescriptor.getLineNumber();
        }
        return GenericTaskException.create(null, errorMessage, srcPath, lineNumber, null);
    }

    public BiConsumer<GenericTaskLogLevel, Supplier<String>> getLogAppender() {
        return logAppender;
    }

    public void setLogAppender(BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender) {
        this.logAppender = logAppender;
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

    public List<File> getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }

    public void setGeneratedCodeFiles(List<File> generatedCodeFiles) {
        this.generatedCodeFiles = generatedCodeFiles;
    }

    public File getSrcDir() {
        return srcDir;
    }

    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    public File getDestDir() {
        return destDir;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public boolean isCodeChangeDetectionDisabled() {
        return codeChangeDetectionDisabled;
    }

    public void setCodeChangeDetectionDisabled(boolean codeChangeDetectionDisabled) {
        this.codeChangeDetectionDisabled = codeChangeDetectionDisabled;
    }

    public List<Throwable> getAllErrors() {
        return allErrors;
    }

    public List<File> getSrcFiles() {
        return srcFiles;
    }

    public List<File> getDestFiles() {
        return destFiles;
    }

    public Map<String, String> getDestSubDirNameMap() {
        return destSubDirNameMap;
    }
}
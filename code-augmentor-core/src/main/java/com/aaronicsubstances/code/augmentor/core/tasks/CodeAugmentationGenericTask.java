package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.models.CodeChangeSummary;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.util.CodeGenerationResponseProcessor;
import com.aaronicsubstances.code.augmentor.core.util.GeneratedCodeFetcher;
import com.aaronicsubstances.code.augmentor.core.util.SourceCodeTransformer;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

/**
 * Implements completion stage of Code Augmentor.
 */
public class CodeAugmentationGenericTask {
    /**
     * Name of file used to store generated files when code change detection is enabled.
     */
    public static final String CHANGE_SUMMARY_FILE_NAME = "CHANGE-SUMMARY.txt";

    /**
     * Name of file used to store generated files when code change detection is disabled.
     */
    public static final String WITHOUT_CHANGE_DETECTION_SUMMARY_FILE_NAME = "OUTPUT-SUMMARY.txt";

    /**
     * Base name of OS shell script for viewing and making changes to source files in which
     * code changes were detected.
     */
    public static final String SHELL_SCRIPT_PREFIX = "EFFECT-CHANGES";

    // input properties.
    private BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender;
    private File prepFile;
    private List<File> generatedCodeFiles;
    private File destDir;
    private boolean codeChangeDetectionDisabled;

    // output properties
    private final List<Throwable> allErrors = new ArrayList<>();
    private File changeSummaryFile;
    private boolean codeChangeDetected;
    
    /**
     * Runs completion stage. Will be successful only if allErrors property
     * is empty after completion. If code change detection is enabled,
     * then changeSummaryFile property will be used to store names of
     * all files in which code changes were detected. Else changeSummaryFile property
     * will be used to store all files except those for which all code generation
     * requests were explicitly skipped.
     * 
     * @throws Exception
     */
    public void execute() throws Exception {
        allErrors.clear();
        codeChangeDetected = false;

        // clean destination directory.
        TaskUtils.deleteDir(destDir);
        destDir.mkdirs();
        
        Map<String, String> destSubDirNameMap = new HashMap<>();

        PreCodeAugmentationResult result = new PreCodeAugmentationResult();
        Object resultReader = result.beginDeserialize(prepFile);
        Charset charset = Charset.forName(result.getEncoding());

        GeneratedCodeFetcher generatedCodeFetcher = new GeneratedCodeFetcher(generatedCodeFiles);
        
        changeSummaryFile = new File(destDir, codeChangeDetectionDisabled ?
            WITHOUT_CHANGE_DETECTION_SUMMARY_FILE_NAME : CHANGE_SUMMARY_FILE_NAME);
        CodeChangeSummary resultChangeSummary = new CodeChangeSummary();
        Object resultChangeSummaryWriter = resultChangeSummary.beginSerialize(changeSummaryFile);

        SourceFileDescriptor sourceFileDescriptor;
        while ((sourceFileDescriptor = SourceFileDescriptor.deserialize(resultReader)) != null) {
            File srcFile = new File(sourceFileDescriptor.getDir(), sourceFileDescriptor.getRelativePath());
            TaskUtils.logVerbose(logAppender, "Processing %s", srcFile);

            Instant startInstant = Instant.now();

            boolean someGenCodeExistsForFile = generatedCodeFetcher.prepareForFile(
                sourceFileDescriptor.getFileId());
            
            String sourceCode = TaskUtils.readFile(srcFile, charset);
            String inputHash = TaskUtils.calcHash(sourceCode, charset);
            if (!inputHash.equals(sourceFileDescriptor.getContentHash())) {
                GenericTaskException fileIntegrityError = createException(
                        "Source file has changed unexpectedly. Regeneration required.", null, srcFile);
                TaskUtils.logWarn(logAppender, fileIntegrityError.getMessage());
                allErrors.add(fileIntegrityError);
                continue;
            }

            int beginErrorCount = allErrors.size();
            int skipCount = 0;

            // fetch applicable generated code per aug code descriptor.
            List<GeneratedCode> generatedCodes = new ArrayList<>();
            List<int[]> replacementRanges = new ArrayList<>();
            for (CodeSnippetDescriptor snippetDescriptor : sourceFileDescriptor.getCodeSnippets()) {
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                GeneratedCode genCode = generatedCodeFetcher.getGeneratedCode(sourceFileDescriptor.getFileId(),
                        augCodeDescriptor.getId());
                if (genCode == null) {                    
                    if (!someGenCodeExistsForFile) {
                        GenericTaskException missingFileAugCodesError = createException(
                                "Could not locate generated codes for file with id " + sourceFileDescriptor.getFileId(), null,
                                srcFile);
                        TaskUtils.logWarn(logAppender, missingFileAugCodesError.getMessage());
                        allErrors.add(missingFileAugCodesError);
                        // don't waste time looking for other gen codes since
                        // they won't be found anyway
                        break;
                    }
                    allErrors.add(createException("Could not find generated code with id " + augCodeDescriptor.getId(),
                            augCodeDescriptor, srcFile));
                } 
                else {
                    // Don't process skipped aug codes.
                    if (genCode.isSkipped()) {
                        skipCount++;
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
                boolean shouldEnsureEndingNewline = CodeGenerationResponseProcessor.
                    getShouldEnsureEndingNewline(genCode);
                if (shouldEnsureEndingNewline) {
                    ContentPart lastContentPart = genCode.getContentParts().get(
                        genCode.getContentParts().size() - 1);
                    lastContentPart.setContent(CodeGenerationResponseProcessor.
                        ensureEndingNewline(lastContentPart.getContent(), newline));
                }
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
                    if (TaskUtils.isBlank(result.getGenCodeStartDirective()) ||
                            TaskUtils.isBlank(result.getGenCodeEndDirective())) {
                        boolean nullsPresent = result.getGenCodeStartDirective() == null ||
                                result.getGenCodeEndDirective() == null;
                        allErrors.add(createException((nullsPresent ? "No/Null" : "Invalid blank") +
                            " start/end directive markers found in prep file " +
                            "with which to insert generated code for augmenting code section with id " + 
                            augCodeDescriptor.getId(), augCodeDescriptor, srcFile));
                        continue;
                    }
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

            // If there are no code generation requests, or all code generation requests 
            // for aug codes in file were skipped, don't generate file even if code change 
            // detection is disabled.
            if (skipCount == sourceFileDescriptor.getCodeSnippets().size()) {
                continue;
            }

            // Now merge generated code into source code,
            // and try and detect changes.
            SourceCodeTransformer transformer = new SourceCodeTransformer(sourceCode);
            boolean srcFileHasChanged = false;
            for (int i = 0; i < generatedCodes.size(); i++) {
                GeneratedCode genCode = generatedCodes.get(i);
                String replacementText = genCode.getWholeContent();
                int[] replacementRange = replacementRanges.get(i);
                transformer.addTransform(replacementText, replacementRange[0], replacementRange[1]);
                if (!codeChangeDetectionDisabled && !srcFileHasChanged) {            
                    String textToBeReplaced = sourceCode.substring(replacementRange[0],
                        replacementRange[1]);
                    srcFileHasChanged = !textToBeReplaced.equals(replacementText);
                }
            }

            // always generate files if code change detection is disabled.

            if (codeChangeDetectionDisabled || srcFileHasChanged) {
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

                // write out change summary.
                // normalize file paths for intended shell scripts.
                new CodeChangeSummary.ChangedFile(sourceFileDescriptor.getRelativePath(),
                    new File(sourceFileDescriptor.getDir()).getCanonicalPath(),
                    destSubDir.getCanonicalPath()).serialize(resultChangeSummaryWriter);

                if (srcFileHasChanged) {
                    codeChangeDetected = true;
                    TaskUtils.logInfo(logAppender, "Changes needed for %s successfully written to\n %s", srcFile, destFile);
                }
            }
            else {                
                TaskUtils.logVerbose(logAppender, "No changes needed for %s", srcFile);
            }

            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            TaskUtils.logInfo(logAppender, "Done processing %s in %d ms", srcFile, timeElapsed);
        }

        // close readers
        result.endDeserialize(resultReader);
        generatedCodeFetcher.close();

        // close writers
        resultChangeSummary.endSerialize(resultChangeSummaryWriter);

        // generate shell scripts for effecting code changes.
        if (!codeChangeDetectionDisabled) {
            InputStream shellScriptRes = getClass().getResourceAsStream("windows-copy-batch-file.bat");
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            TaskUtils.copyStream(shellScriptRes, outStream);
            String contents = new String(outStream.toByteArray(), Charset.defaultCharset());
            TaskUtils.writeFile(new File(destDir, SHELL_SCRIPT_PREFIX + ".bat"), 
                Charset.defaultCharset(), contents);
            
            shellScriptRes = getClass().getResourceAsStream("unix-copy-bash-file.sh");
            outStream = new ByteArrayOutputStream();
            TaskUtils.copyStream(shellScriptRes, outStream);
            contents = new String(outStream.toByteArray(), Charset.defaultCharset());
            TaskUtils.writeFile(new File(destDir, SHELL_SCRIPT_PREFIX), 
                Charset.defaultCharset(), contents);
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
    
    public File getChangeSummaryFile() {
        return changeSummaryFile;
    }

    public boolean isCodeChangeDetected() {
        return codeChangeDetected;
    }
}
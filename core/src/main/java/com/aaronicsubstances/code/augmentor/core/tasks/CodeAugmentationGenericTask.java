package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.util.SourceCodeTransformer;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

public class CodeAugmentationGenericTask {
    // input properties.
    private BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender;
    private Charset charset;
    private File prepFile;
    private List<File> generatedCodeFiles;
    private File srcDir;
    private File destDir;

    // output properties
    private final List<File> srcFiles = new ArrayList<>();
    private final List<File> destFiles = new ArrayList<>();
    private final Map<String, String> destSubDirNameMap  = new HashMap<>();
    
    public void execute() throws Exception {
        srcFiles.clear();
        destFiles.clear();
        destSubDirNameMap.clear();

        PreCodeAugmentationResult result = new PreCodeAugmentationResult();
        Object resultReader = result.beginDeserialize(prepFile);

        GeneratedCodeFetcher generatedCodeFetcher = new GeneratedCodeFetcher(generatedCodeFiles);

        SourceFileDescriptor sourceFileDescriptor;
        while ((sourceFileDescriptor = SourceFileDescriptor.deserialize(resultReader)) != null) {
            // use this for testing only. under normal circumstances dir should be set.
            if (sourceFileDescriptor.getDir() == null) {
                sourceFileDescriptor.setDir(srcDir.getPath());
            }
            File srcFile = new File(sourceFileDescriptor.getDir(),
                sourceFileDescriptor.getRelativePath());
            TaskUtils.logVerbose(logAppender, "Processing %s", srcFile);
            
            Instant startInstant = Instant.now();
            String sourceCode = TaskUtils.readFile(srcFile, charset);
            if (sourceFileDescriptor.getContentHash() != null) {
                String inputHash = TaskUtils.calcHash(sourceCode, charset);
                if (!inputHash.equals(sourceFileDescriptor.getContentHash())) {
                    throw createException("Source file has changed unexpectedly. Regeneration required.",
                        null, srcFile);
                }
            }

            if (!generatedCodeFetcher.prepareForFile(sourceFileDescriptor.getFileIndex())) {
                throw createException("Could not locate generated codes",
                    null, srcFile);
            }

            // fetch applicable generated code per aug code descriptor.
            List<GeneratedCode> generatedCodes = new ArrayList<>();
            List<String> replacementTexts = new ArrayList<>();
            List<int[]> replacementRanges = new ArrayList<>();
            List<Integer> exactMatchAdjustments = new ArrayList<>();
            for (CodeSnippetDescriptor snippetDescriptor : sourceFileDescriptor.getCodeSnippets()) {
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                int augCodeIndex = augCodeDescriptor.getIndex();

                GeneratedCode genCode = generatedCodeFetcher.getGeneratedCode(
                    sourceFileDescriptor.getFileIndex(), augCodeIndex);
                if (genCode == null) {
                    throw createException("Could not find generated code", augCodeDescriptor, srcFile);
                }
                if (genCode.isSkipped()) {
                    GenericTaskException warning = createException("Code generation skipped", 
                        augCodeDescriptor, srcFile);
                    TaskUtils.logWarn(logAppender, warning.getMessage());
                    continue;
                }
                if (genCode.getContent() == null) {
                    throw createException("Received null for generated code content",
                        augCodeDescriptor, srcFile);
                }

                generatedCodes.add(genCode);

                int[] replacementRange = CodeGenerationResponseProcessor.determineReplacementRange(snippetDescriptor,
                    genCode);
                if (replacementRange != null) {
                    GenericTaskException warning = createException("Advanced replacement usage detected.", 
                        augCodeDescriptor, srcFile);
                    TaskUtils.logWarn(logAppender, warning.getMessage());
                }
                replacementRanges.add(replacementRange);

                String newline = augCodeDescriptor.getLineSeparator();

                String formattedGenCode = CodeGenerationResponseProcessor.ensureEndingNewline(
                    genCode.getContent(), newline);
                String indent = "";
                boolean hasExactMatches = genCode.getExactMatchRanges() != null &&
                    !genCode.getExactMatchRanges().isEmpty();
                if (!hasExactMatches) {
                    indent = CodeGenerationResponseProcessor.getEffectiveIndent(
                        augCodeDescriptor, genCode);
                    if (!indent.isEmpty()) {
                        formattedGenCode = CodeGenerationResponseProcessor.indentCode(
                            formattedGenCode, indent);
                    }
                }
                int exactMatchAdjustment = 0;
                if (replacementRange == null) {
                    // employ default behaviour of ensuring generated code
                    // occurs within directive markers. 
                    if (snippetDescriptor.getGeneratedCodeDescriptor() == null) {
                        String startDrv = result.getGenCodeStartDirective();
                        String endDrv = result.getGenCodeEndDirective();
                        String wrapPrefix = indent + startDrv + newline;
                        String wrapSuffix = indent + endDrv + newline;
                        formattedGenCode = wrapPrefix + formattedGenCode + wrapSuffix;
                        exactMatchAdjustment = wrapPrefix.length();
                    }
                }
                replacementTexts.add(formattedGenCode);
                exactMatchAdjustments.add(exactMatchAdjustment);
            }

            // Now merge generated code into source code,
            // and try and detect changes.
            boolean changesDetected = false;
            SourceCodeTransformer transformer = new SourceCodeTransformer(sourceCode);
            for (int i = 0; i < replacementTexts.size(); i++) {
                String replacmentText = replacementTexts.get(i);
                int[] replacementRange = replacementRanges.get(i);
                String textToBeReplaced;

                // use replacement range if specified.
                if (replacementRange != null) {
                    // expected for advanced usage only.
                    transformer.addTransform(replacmentText, replacementRange[0], replacementRange[1]);
                    textToBeReplaced = sourceCode.substring(replacementRange[0],
                        replacementRange[1]);
                }
                else {
                    // resort to default behaviour
                    CodeSnippetDescriptor snippetDescriptor = sourceFileDescriptor.getCodeSnippets().get(i);
                    GeneratedCodeDescriptor genCodeDescriptor = snippetDescriptor.getGeneratedCodeDescriptor();
                    if (genCodeDescriptor != null) {                    
                        // by default range of generated code excludes directive markers.
                        // it starts from just after the start directive marker,
                        // and ends just before the end directive marker.
                        int genCodeStartPos = genCodeDescriptor.getStartDirectiveEndPos();
                        int genCodeEndPos = genCodeDescriptor.getEndDirectiveStartPos(); 
                        transformer.addTransform(replacmentText, genCodeStartPos, genCodeEndPos);
                        textToBeReplaced = sourceCode.substring(genCodeStartPos,
                            genCodeEndPos);
                    }
                    else {
                        AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                        transformer.addTransform(replacmentText, augCodeDescriptor.getEndPos());
                        textToBeReplaced = "";
                    }
                }
                // check for changes only if we still haven't detected them.
                if (changesDetected) {
                    // skip this check.
                }
                else if (textToBeReplaced.equals(replacmentText)) {
                    // definitely there are no changes then.
                }
                else {
                    // determine whether changes are superficial or significant.
                    GeneratedCode genCode = generatedCodes.get(i);
                    int exactMatchAdjustment = exactMatchAdjustments.get(i);
                    boolean similar = CodeGenerationResponseProcessor.areTextsSimiliar(
                        textToBeReplaced, replacmentText,
                        genCode.getExactMatchRanges(), exactMatchAdjustment);
                    if (!similar) {
                        changesDetected = true;
                    }
                }
            }

            if (!changesDetected) {
                TaskUtils.logVerbose(logAppender, "No changes needed for %s", srcFile);
            }
            else {
                String destSubDirName = destSubDirNameMap.get(sourceFileDescriptor.getDir());
                if (destSubDirName == null) {
                    destSubDirName = new File(sourceFileDescriptor.getDir()).getName();
                    destSubDirName = TaskUtils.modifyNameToBeAbsent(
                        destSubDirNameMap.values(), destSubDirName);
                    destSubDirNameMap.put(sourceFileDescriptor.getDir(), destSubDirName);
                }
                File destSubDir = new File(destDir, destSubDirName);
                File destFile = new File(destSubDir, sourceFileDescriptor.getRelativePath());
                destFile.getParentFile().mkdirs();

                String transformedCode = transformer.getTransformedText();
                TaskUtils.writeFile(destFile, charset, transformedCode);
                srcFiles.add(srcFile);
                destFiles.add(destFile);

                TaskUtils.logInfo(logAppender, "Changes needed for %s successfully written to\n %s", srcFile, destFile);
            }
            
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            TaskUtils.logInfo(logAppender, "Done processing %s in %d ms", srcFile, timeElapsed);
        }

        // close readers
        result.endDeserialize(resultReader);
        generatedCodeFetcher.close();
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
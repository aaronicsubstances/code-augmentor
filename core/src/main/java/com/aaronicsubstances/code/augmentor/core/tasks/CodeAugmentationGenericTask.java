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
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;

public class CodeAugmentationGenericTask {
    public static final int LOG_LEVEL_VERBOSE = 1;
    public static final int LOG_LEVEL_INFO = 2;
    public static final int LOG_LEVEL_WARN = 3;

    // input properties.
    private BiConsumer<Integer, Supplier<String>> logAppender;
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
            logVerbose("Processing %s", srcFile);
            
            Instant startInstant = Instant.now();
            String sourceCode = TaskUtils.readFile(srcFile, charset);
            if (sourceFileDescriptor.getContentHash() != null) {
                String inputHash = TaskUtils.calcHash(sourceCode, charset);
                if (!inputHash.equals(sourceFileDescriptor.getContentHash())) {
                    throw new GenericTaskException(String.format("Changes to source files detected in %s. Regeneration required.",
                        srcFile));
                }
            }

            generatedCodeFetcher.prepareForFile(sourceFileDescriptor.getFileIndex());

            // fetch applicable generated code per aug code descriptor.
            List<String> generatedCodes = new ArrayList<>();
            for (CodeSnippetDescriptor snippetDescriptor : sourceFileDescriptor.getBodySnippets()) {
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                int augCodeIndex = augCodeDescriptor.getIndex();

                StringBuilder newlineReceiver = new StringBuilder();
                GeneratedCode genCode = generatedCodeFetcher.getGeneratedCode(sourceFileDescriptor.getFileIndex(), 
                    augCodeIndex, newlineReceiver);
                String newline = newlineReceiver.toString();
                if (genCode == null) {
                    throw new GenericTaskException("Could not find generated code for " +
                        describeAugCodeSection(sourceCode, augCodeDescriptor, srcFile));
                }
                if (genCode.isError()) {
                    throw new GenericTaskException("Generation of code failed for " +
                        describeAugCodeSection(sourceCode, augCodeDescriptor, srcFile) + ":\n" +
                        genCode.getBodyContent());
                }
                String indent = getEffectiveIndent(augCodeDescriptor, genCode);
                String formattedGenCode = indentCodeAndEnsureNewlineEnding(genCode.getBodyContent(),
                    indent, newline);
                if (snippetDescriptor.getGeneratedCodeDescriptor() == null) {
                    formattedGenCode = wrapInGeneratedCodeDirectives(formattedGenCode, 
                        result.getGenCodeStartDirective(), result.getGenCodeEndDirective(),
                        augCodeDescriptor.getHasNewlineEnding(),
                        indent, newline);
                }
                generatedCodes.add(formattedGenCode);
            }

            // Now merge generated code into source code.
            SourceCodeTransformer transformer = new SourceCodeTransformer(sourceCode);
            boolean changesDetected = false;
            for (int i = 0; i < generatedCodes.size(); i++) {
                CodeSnippetDescriptor snippetDescriptor = sourceFileDescriptor.getBodySnippets().get(i);
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                GeneratedCodeDescriptor genCodeDescriptor = snippetDescriptor.getGeneratedCodeDescriptor();
                String genCode = generatedCodes.get(i);
                if (genCodeDescriptor != null) {
                    transformer.addTransform(genCode, genCodeDescriptor.getStartPos(),
                        genCodeDescriptor.getEndPos());
                    if (!changesDetected) {
                        String prevGenCode = sourceCode.substring(genCodeDescriptor.getStartPos(), 
                            genCodeDescriptor.getEndPos());
						if (!genCode.equals(prevGenCode)) {
							changesDetected = true;
						}
                    }
                }
                else {
                    transformer.addTransform(genCode, augCodeDescriptor.getEndPos());
                    changesDetected = true;
                }
            }

            String transformedCode = transformer.getTransformedText();
            if (changesDetected) {
                String destSubDirName = destSubDirNameMap.get(sourceFileDescriptor.getDir());
                if (destSubDirName == null) {
                    String origDirName = new File(sourceFileDescriptor.getDir()).getName();
                    // ensure uniqueness.
                    if (!destSubDirNameMap.values().stream()
                            .anyMatch(x -> x.equals(origDirName))) {
                        destSubDirName = origDirName;
                    }
                    else {
                        StringBuilder dirName = new StringBuilder(origDirName);
                        int index = 1;
                        dirName.append("-").append(index);
                        while (destSubDirNameMap.values().stream()
                                .anyMatch(x -> x.equals(dirName.toString()))) {   
                            index++;
                            dirName.setLength(origDirName.length());
                            dirName.append("-").append(index);
                        }
                        destSubDirName = dirName.toString();
                    }
                    destSubDirNameMap.put(sourceFileDescriptor.getDir(), destSubDirName);
                }
                File destSubDir = new File(destDir, destSubDirName);
                destSubDir.mkdirs();
                File destFile = new File(destSubDir, sourceFileDescriptor.getRelativePath());
                TaskUtils.writeFile(destFile, charset, transformedCode);
                srcFiles.add(srcFile);
                destFiles.add(destFile);
            }
            
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            logVerbose("done in %s ms", timeElapsed);
        }

        // close readers
        result.endDeserialize(resultReader);
        generatedCodeFetcher.close();
    }

    private static String getEffectiveIndent(AugmentingCodeDescriptor augCodeDescriptor, GeneratedCode genCode) {
        String indent = genCode.getIndent();
        if (indent == null) {
            indent = augCodeDescriptor.getIndent();
        }
        return indent;
    }

    static String indentCodeAndEnsureNewlineEnding(String code, String indent, String newline) {
        if (!TaskUtils.isEmpty(indent)) {
            List<String> splitCode = TaskUtils.splitIntoLines(code);
            StringBuilder codeBuffer = new StringBuilder();
            for (int i = 0; i < splitCode.size(); i+=2) {
                String line = splitCode.get(i);
                if (!TaskUtils.isBlank(line)) {
                    codeBuffer.append(indent).append(line);
                }
                String terminator = splitCode.get(i + 1);
                if (terminator == null) {
                    break;
                }
                codeBuffer.append(terminator);
            }
            code = codeBuffer.toString();
        }
        boolean endsWithNewline = false;
        if (!code.isEmpty()) {
            char lastChar = code.charAt(code.length() - 1);
            if (TaskUtils.isNewLine(lastChar)) {
                endsWithNewline = true;
            }
        }
        if (!endsWithNewline) {
            code += newline;
        }
        return code;
    }

    private String wrapInGeneratedCodeDirectives(String code, String genCodeStartDirective,
            String genCodeEndDirective, boolean augCodeHasNewlineEnding, 
            String indent, String newline) {
        if (indent == null) {
            indent = "";
        }
        return (augCodeHasNewlineEnding ? "" : newline) +
            indent + genCodeStartDirective + newline +
            code +
            indent + genCodeEndDirective + newline;
    }

    private static String describeAugCodeSection(String input, AugmentingCodeDescriptor augCodeDescriptor, 
            File srcFile) {
        int lineNumber = TaskUtils.calculateLineNumber(input, 
            augCodeDescriptor.getStartPos());
        String msg = String.format("aug code section at line %s (index %s to %s) in %s", 
            lineNumber, augCodeDescriptor.getStartPos(), augCodeDescriptor.getEndPos(), srcFile);
        return msg;
    }

    private void logVerbose(String format, Object... args) {
        if (logAppender == null) {
            return;
        }
        logAppender.accept(LOG_LEVEL_VERBOSE, () -> String.format(format, args));
    }

    @SuppressWarnings("unused")
    private void logInfo(String format, Object... args) {
        if (logAppender == null) {
            return;
        }
        logAppender.accept(LOG_LEVEL_INFO, () -> String.format(format, args));        
    }

    @SuppressWarnings("unused")
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
package com.aaronicsubstances.code.augmentor.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.parsing.Token;

public class CodeAugmentationGenericTask {
    public static final int LOG_LEVEL_VERBOSE = 1;
    public static final int LOG_LEVEL_INFO = 2;
    public static final int LOG_LEVEL_WARN = 3;
    private BiConsumer<Integer, Supplier<String>> logAppender;
    private Charset charset;

    private File prepfile;
    private List<File> generatedCodeFiles;
    private File destdir;
    private boolean generate;
    private String newline;
    private File tempDir;

    private boolean upToDate;
    
    public void execute() throws Exception {
        upToDate = true;

        PreCodeAugmentationResult result = new PreCodeAugmentationResult();
        Object resultReader = result.beginDeserialize(prepfile);

        GeneratedCodeFetcher generatedCodeFetcher = new GeneratedCodeFetcher(generatedCodeFiles);

        SourceFileDescriptor sourceFileDescriptor;
        while ((sourceFileDescriptor = SourceFileDescriptor.deserialize(resultReader)) != null) {
            if (sourceFileDescriptor.getDir() == null) {
                sourceFileDescriptor.setDir(tempDir.getPath());
            }
            File srcFile = new File(sourceFileDescriptor.getDir(),
                sourceFileDescriptor.getRelativePath());
            logVerbose("Processing %s", srcFile);

            // don't bother to touch file if it doesn't contain any snippets of 
            // augmenting code.
            if (sourceFileDescriptor.getBodySnippets().isEmpty()) {           
                if (generate) {
                    File destFile = new File(destdir, sourceFileDescriptor.getRelativePath());
                    TaskUtils.copyFile(srcFile, destFile);
                }
                continue;
            }
            
            Instant startInstant = Instant.now();
            String sourceCode = TaskUtils.readFile(srcFile, charset);
            String inputHash = TaskUtils.calcHash(sourceCode, charset);
            if (!inputHash.equals(sourceFileDescriptor.getContentHash())) {
                throw new GenericTaskException(String.format("Changes to source files detected in %s. Regeneration required.",
                    srcFile));
            }

            generatedCodeFetcher.prepareForFile(sourceFileDescriptor.getFileIndex());

            // fetch applicable generated code per aug code descriptor.
            List<String> generatedCodes = new ArrayList<>();
            List<String> sourceFileImports = new ArrayList<>();
            for (CodeSnippetDescriptor snippetDescriptor : sourceFileDescriptor.getBodySnippets()) {
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                int augCodeIndex = augCodeDescriptor.getIndex();

                GeneratedCode genCode = generatedCodeFetcher.getGeneratedCode(sourceFileDescriptor.getFileIndex(), 
                    augCodeIndex);
                if (genCode == null) {
                    throw new GenericTaskException("Could not find generated code for " +
                        describeAugCodeSection(sourceCode, augCodeDescriptor, srcFile));
                }
                if (genCode.isError()) {
                    throw new GenericTaskException("Generation of code failed for " +
                        describeAugCodeSection(sourceCode, augCodeDescriptor, srcFile) + ":\n" +
                        genCode.getBodyContent());
                }
                List<String> headerImports;
                try {
                    headerImports = parseHeaderImports(sourceFileDescriptor.getRelativePath(),
                        genCode);
                }
                catch (ParserException ex) {
                    throw new GenericTaskException("Invalid generated header code for " +
                        describeAugCodeSection(sourceCode, augCodeDescriptor, srcFile), 
                        ex);
                }
                List<Token> tokens;
                try {
                    tokens = TaskUtils.parseSourceCode(sourceFileDescriptor.getRelativePath(), 
                        genCode.getBodyContent()).parse();
                }
                catch (ParserException ex) {
                    throw new GenericTaskException("Invalid generated code for " +
                        describeAugCodeSection(sourceCode, augCodeDescriptor, srcFile), 
                        ex);
                }
                boolean canIndent = canIndentCode(augCodeDescriptor, tokens);
                String formattedCode = genCode.getBodyContent();
                if (canIndent) {
                    formattedCode = indentCode(formattedCode, augCodeDescriptor.getIndent());
                }
                formattedCode = wrapInGeneratedCodeComments(formattedCode, 
                    result.getGenCodeStartSuffix(), result.getGenCodeEndSuffix(),
                    augCodeDescriptor, canIndent);
                generatedCodes.add(formattedCode);
                sourceFileImports.addAll(headerImports);
            }

            // identify new imports.
            String headerImport = null;
            if (!sourceFileImports.isEmpty()) {
                headerImport = String.join(newline, filterImports(sourceFileImports, 
                    sourceFileDescriptor.getImportStatements()));
                // surround with newlines.
                headerImport = newline + headerImport + newline;
            }

            // Now merge generated code into source code.
            SourceCodeTransformer transformer = new SourceCodeTransformer(sourceCode);
            int headerPosInc = 0;
            for (int i = 0; i < generatedCodes.size(); i++) {
                CodeSnippetDescriptor snippetDescriptor = sourceFileDescriptor.getBodySnippets().get(i);
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                GeneratedCodeDescriptor genCodeDescriptor = snippetDescriptor.getGeneratedCodeDescriptor();
                String genCode = generatedCodes.get(i);
                int diff;
                if (genCodeDescriptor != null) {
                    diff = transformer.addTransform(genCode, genCodeDescriptor.getStartPos(),
                        genCodeDescriptor.getEndPos());
                }
                else {
                    diff = transformer.addTransform(genCode, augCodeDescriptor.getEndPos());
                }
                if (headerImport != null && sourceFileDescriptor.getHeaderInsertPos() < augCodeDescriptor.getStartPos()) {
                    headerPosInc += diff;
                }                
            }

            String transformedCode = transformer.getTransformedText();
            File destFile = new File(destdir, sourceFileDescriptor.getRelativePath());
            if (!sourceCode.equals(transformedCode)) {
                if (generate) {
                    if (headerImport != null) {
                        StringBuilder s = new StringBuilder(transformedCode);
                        s.insert(sourceFileDescriptor.getHeaderInsertPos() +
                            headerPosInc, headerImport);
                        transformedCode = s.toString();
                    }
                    TaskUtils.writeFile(destFile, charset, transformedCode);
                }
                else {
                    logWarn("Out of sync source files detected in %s. Regeneration required.",
                        srcFile);
                    upToDate = false;
                }
            }
            else if (generate) {
                TaskUtils.copyFile(srcFile, destFile);
            }
            
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            logVerbose("done in %s ms", timeElapsed);
        }

        // close readers
        result.endDeserialize(resultReader);
        generatedCodeFetcher.close();
    }

    static List<String> filterImports(List<String> sourceFileImports, List<String> existingImports) {
        if (existingImports.isEmpty()) {
            return sourceFileImports;
        }
        List<String> filtered = new ArrayList<>();
        for (String imp : sourceFileImports) {
            if (existingImports.contains(imp)) {
                continue;
            }
            if (!imp.contains(" as ") && !imp.endsWith(".*")) {
                boolean wildcardMatchFound = false;
                for (String e : existingImports) {
                    if (e.endsWith(".*")) {
                        String ePrefix = e.substring(0, e.length() - ".*".length());
                        if (imp.equals(ePrefix)) {
                            wildcardMatchFound = true;
                            break;
                        }
                    }
                }
                if (wildcardMatchFound) {
                    continue;
                }
            }
            filtered.add(imp);
        }
        return filtered;
    }

    private static boolean canIndentCode(AugmentingCodeDescriptor augCodeDescriptor, List<Token> tokens) {
        if (TaskUtils.isNullOrEmpty(augCodeDescriptor.getIndent())) {
            return false;
        }
        for (Token token : tokens) {
            if (token.type == Token.TYPE_LITERAL_STRING_CONTENT) {
                if (LexerSupport.NEW_LINE_REGEX.matcher(token.text).find()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String indentCode(String code, String indent) {
        // apply indent.
        String[] lines = LexerSupport.NEW_LINE_REGEX.split(code, -1);
        StringBuilder indentedContent = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i > 0) {
                indentedContent.append(newline);
            }
            indentedContent.append(indent).append(line);
        }
        return indentedContent.toString();
    }

    private String wrapInGeneratedCodeComments(String content, String genCodeStart, String genCodeEnd,
            AugmentingCodeDescriptor augmentingCodeDescriptor, boolean canIndent) {
        boolean annotatedWithSlashStar = augmentingCodeDescriptor.isAnnotatedWithSlashStar();
        String formattedCode;
        if (annotatedWithSlashStar && !LexerSupport.NEW_LINE_REGEX.matcher(content).find()) {
            // use slash star
            formattedCode = "/*" + genCodeStart + "*/" + content +
                "/*" + genCodeEnd + "*/";
        }
        else {
            //use double slashes
            String indent  = canIndent ? augmentingCodeDescriptor.getIndent() : "";
            formattedCode = newline + indent + "//" + genCodeStart + newline + content + newline +
                indent + "//" + genCodeEnd + newline;
        }
        return formattedCode;
    }

    private static List<String> parseHeaderImports(String relativePath, GeneratedCode genCode) {
        String headerContent = genCode.getHeaderContent();
        if (headerContent == null) {
            return Arrays.asList();
        }
        
        List<Token> tokens = TaskUtils.parseSourceCode(relativePath, headerContent).parse();
        List<String> headerImports = CodeGenerationRequestCreator.getNormalizedImportStatements(tokens);
        return headerImports;
    }

    private static String describeAugCodeSection(String input, AugmentingCodeDescriptor augCodeDescriptor, 
            File srcFile) {
        int lineNumber = LexerSupport.calculateLineAndColumnNumbers(input, 
            augCodeDescriptor.getStartPos())[0];
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

    public File getPrepfile() {
        return prepfile;
    }

    public void setPrepfile(File prepfile) {
        this.prepfile = prepfile;
    }

    public List<File> getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }

    public void setGeneratedCodeFiles(List<File> generatedCodeFiles) {
        this.generatedCodeFiles = generatedCodeFiles;
    }

    public File getDestdir() {
        return destdir;
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    public boolean isGenerate() {
        return generate;
    }

    public void setGenerate(boolean generate) {
        this.generate = generate;
    }

    public String getNewline() {
        return newline;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    public File getTempDir() {
        return tempDir;
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public boolean isUpToDate() {
        return upToDate;
    }
}
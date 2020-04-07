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
import com.aaronicsubstances.code.augmentor.core.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.core.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.core.parsing.Token;

public class CodeAugmentationGenericTask {
    public static final int LOG_LEVEL_VERBOSE = 1;
    public static final int LOG_LEVEL_INFO = 2;
    public static final int LOG_LEVEL_WARN = 3;
    private BiConsumer<Integer, Supplier<String>> logAppender;
    private Charset charset;

    private File prepFile;
    private List<File> generatedCodeFiles;
    private File srcDir;
    private File destDir;
    private String newline;

    private final List<File> srcFiles = new ArrayList<>();
    private final List<File> destFiles = new ArrayList<>();
    private final Map<String, String> destSubDirNameMap  = new HashMap<>();
    
    public void execute() throws Exception {
        srcFiles.clear();
        destFiles.clear();
        destSubDirNameMap.clear();;

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
            List<List<Token>> parsedGeneratedCodes = new ArrayList<>();
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
                List<Token> tokens;
                try {
                    tokens = parseSourceCode(sourceFileDescriptor.getRelativePath(), 
                        genCode.getBodyContent());
                }
                catch (ParserException ex) {
                    throw new GenericTaskException("Invalid generated code for " +
                        describeAugCodeSection(sourceCode, augCodeDescriptor, srcFile), 
                        ex);
                }
                boolean canIndent = canIndentCode(augCodeDescriptor, tokens);
                if (canIndent) {
                    tokens = indentCode(tokens, augCodeDescriptor.getIndent());
                }
                tokens = wrapInGeneratedCodeComments(tokens, 
                    result.getGenCodeStartSuffix(), result.getGenCodeEndSuffix(),
                    augCodeDescriptor, canIndent);
                parsedGeneratedCodes.add(tokens);
            }

            // Now merge generated code into source code.
            SourceCodeTransformer transformer = new SourceCodeTransformer(sourceCode);
            boolean changesDetected = false;
            for (int i = 0; i < parsedGeneratedCodes.size(); i++) {
                CodeSnippetDescriptor snippetDescriptor = sourceFileDescriptor.getBodySnippets().get(i);
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                GeneratedCodeDescriptor genCodeDescriptor = snippetDescriptor.getGeneratedCodeDescriptor();
                List<Token> parsedGenCode = parsedGeneratedCodes.get(i);
                StringBuilder genCode = new StringBuilder(); 
                for (Token t : parsedGenCode) {
                    genCode.append(t.text);
                }
                if (genCodeDescriptor != null) {
					String genCodeStr = genCode.toString();
                    transformer.addTransform(genCodeStr, genCodeDescriptor.getStartPos(),
                        genCodeDescriptor.getEndPos());
                    if (!changesDetected) {
                        String prevGenCode = sourceCode.substring(genCodeDescriptor.getStartPos(), 
                            genCodeDescriptor.getEndPos());
						if (!genCodeStr.equals(prevGenCode)) {
							changesDetected = true;
						}
                    }
                }
                else {
                    transformer.addTransform(genCode.toString(), augCodeDescriptor.getEndPos());
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

    private List<Token> parseSourceCode(String relativePath, String sourceCode) {
        List<Token> tokens = TaskUtils.parseSourceCode(relativePath, sourceCode).parse();
        return tokens;
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

    static List<Token> indentCode(List<Token> codeTokens, String indent) {
        List<Token> indentedCodeTokens = new ArrayList<>();
        Token indentToken = new Token(Token.TYPE_NON_NEWLINE_WHITESPACE, indent, 
            0, 0, 0);
        indentedCodeTokens.add(indentToken);
        for (int i = 0; i < codeTokens.size(); i++) {
            Token t = codeTokens.get(i);
            indentedCodeTokens.add(t);
            // add indent after newlines, unless newline is the last token in source code.
            if (t.type == Token.TYPE_NEWLINE && i + 1 < codeTokens.size()) {
                indentToken = new Token(Token.TYPE_NON_NEWLINE_WHITESPACE, indent, 
                    0, 0, 0); 
                indentedCodeTokens.add(indentToken);
            }
        }
        return indentedCodeTokens;
    }

    private List<Token> wrapInGeneratedCodeComments(List<Token> contentTokens, 
            String genCodeStartSuffix, String genCodeEndSuffix,
            AugmentingCodeDescriptor augmentingCodeDescriptor, boolean canIndent) {
        List<Token> wrappedContentTokens = new ArrayList<>();
        if (augmentingCodeDescriptor.isAnnotatedWithSlashStar()) {
            // use slash star
            String commentStart = "/*" + genCodeStartSuffix + "*/";
            Token genCodeStart = new Token(Token.TYPE_MULTI_LINE_COMMENT, 
                commentStart, 0, 0, 0);
            wrappedContentTokens.add(genCodeStart);

            wrappedContentTokens.addAll(contentTokens);

            String commentEnd = "/*" + genCodeEndSuffix + "*/";
            Token genCodeEnd = new Token(Token.TYPE_MULTI_LINE_COMMENT,
                commentEnd, 0, 0, 0);
            wrappedContentTokens.add(genCodeEnd);
        }
        else {
            //use double slashes

            // always add a first new line since newlines ending double slashes are always
            // included in generated code range.
            Token newlineToken = new Token(Token.TYPE_NEWLINE, newline, 
                0, 0, 0); 
            wrappedContentTokens.add(newlineToken);

            if (canIndent) {
                Token indentToken = new Token(Token.TYPE_NON_NEWLINE_WHITESPACE,
                    augmentingCodeDescriptor.getIndent(), 
                    0, 0, 0); 
                wrappedContentTokens.add(indentToken);
            }

            String commentStart = "//" + genCodeStartSuffix;
            Token genCodeStart = new Token(Token.TYPE_SINGLE_LINE_COMMENT, 
                commentStart, 0, 0, 0);
            wrappedContentTokens.add(genCodeStart);
            newlineToken = new Token(Token.TYPE_NEWLINE, newline, 
                0, 0, 0); 
            wrappedContentTokens.add(newlineToken);

            wrappedContentTokens.addAll(contentTokens);
            newlineToken = new Token(Token.TYPE_NEWLINE, newline, 
                0, 0, 0);

            wrappedContentTokens.add(newlineToken);
            if (canIndent) {
                Token indentToken = new Token(Token.TYPE_NON_NEWLINE_WHITESPACE,
                    augmentingCodeDescriptor.getIndent(), 
                    0, 0, 0); 
                wrappedContentTokens.add(indentToken);
            }

            String commentEnd = "//" + genCodeEndSuffix;
            Token genCodeEnd = new Token(Token.TYPE_SINGLE_LINE_COMMENT,
                commentEnd, 0, 0, 0);
            wrappedContentTokens.add(genCodeEnd);
            newlineToken = new Token(Token.TYPE_NEWLINE, newline, 
                0, 0, 0);
            wrappedContentTokens.add(newlineToken);
        }
        return wrappedContentTokens;
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

    public String getNewline() {
        return newline;
    }

    public void setNewline(String newline) {
        this.newline = newline;
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
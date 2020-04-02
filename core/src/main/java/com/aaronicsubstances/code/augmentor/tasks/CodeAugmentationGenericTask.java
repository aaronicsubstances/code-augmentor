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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            List<Token> sourceFileImports = new ArrayList<>();
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
                List<Token> headerImports;
                try {
                    headerImports = parseOutHeaderImports(sourceFileDescriptor.getRelativePath(),
                        genCode);
                }
                catch (ParserException ex) {
                    throw new GenericTaskException("Invalid generated header code for " +
                        describeAugCodeSection(sourceCode, augCodeDescriptor, srcFile), 
                        ex);
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
                int diff;
                if (genCodeDescriptor != null) {
                    diff = transformer.addTransform(genCode.toString(), genCodeDescriptor.getStartPos(),
                        genCodeDescriptor.getEndPos());
                    if (!changesDetected) {
                        String prevGenCode = sourceCode.substring(genCodeDescriptor.getStartPos(), 
                            genCodeDescriptor.getEndPos());
                        if (isSignificantlyDifferent(parsedGenCode, prevGenCode)) {
                            changesDetected = true;
                        }
                    }
                }
                else {
                    diff = transformer.addTransform(genCode.toString(), augCodeDescriptor.getEndPos());
                    changesDetected = true;
                }
                if (headerImport != null && sourceFileDescriptor.getHeaderInsertPos() > augCodeDescriptor.getStartPos()) {
                    headerPosInc += diff;
                }                
            }

            String transformedCode = transformer.getTransformedText();
            File destFile = new File(destdir, sourceFileDescriptor.getRelativePath());
            if (changesDetected) {
                if (generate) {
                    // Only insert header if changes are to be made.
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

    private List<Token> parseSourceCode(String relativePath, String sourceCode) {
        List<Token> tokens = TaskUtils.parseSourceCode(relativePath, sourceCode).parse();
        return tokens;
    }

    private static List<Token> parseOutHeaderImports(String relativePath, GeneratedCode genCode) {
        String headerContent = genCode.getHeaderContent();
        if (headerContent == null) {
            return Arrays.asList();
        }
        
        List<Token> tokens = TaskUtils.parseSourceCode(relativePath, headerContent).parse();
        List<Token> headerImports = tokens.stream()
            .filter(x -> x.type == Token.TYPE_IMPORT_STATEMENT)
            .collect(Collectors.toList());
        return headerImports;
    }

    static List<String> filterImports(List<Token> sourceFileImports, List<String> existingImports) {
        List<String> filtered = new ArrayList<>();
        for (Token impToken : sourceFileImports) {
            String imp = (String)impToken.value.get(Token.VALUE_KEY_IMPORT_STATEMENT);
            if (existingImports.contains(imp)) {
                continue;
            }
            // Don't process Kotlin "as" keyword when
            // looking for wild card matches.
            // Ignore imperfections with Kotlin backticks, as long as
            // Java wilcard matches works perfectly fine.
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
            filtered.add(impToken.text);
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

    static boolean isSignificantlyDifferent(List<Token> parsedGenCode, String prevGenCode) {
        // build regex out of parsedGenCode and match against prevGenCode
        StringBuilder regexBuilder = new StringBuilder();
        for (int tIndex = 0; tIndex < parsedGenCode.size(); tIndex++) {
            Token t = parsedGenCode.get(tIndex);
            // exclude from generic whitespace newlines which terminate double slash comments
            // and Kotlin shebang first line
            boolean ordinaryNewline = false;
            if (t.type == Token.TYPE_NEWLINE) {
                if (tIndex == 0) {
                    ordinaryNewline = true;
                }
                else {
                    Token prevToken = parsedGenCode.get(tIndex - 1);
                    if (prevToken.type != Token.TYPE_SINGLE_LINE_COMMENT &&
                            prevToken.type != Token.TYPE_SHEBANG) {
                        ordinaryNewline = true;
                    }
                }
            }
            if (t.type == Token.TYPE_NON_NEWLINE_WHITESPACE ||
                    ordinaryNewline) {
                regexBuilder.append("\\s");
                if (t.value != null && Boolean.TRUE.equals(t.value.get(Token.VALUE_KEY_WS_REQD))) {
                    regexBuilder.append("+");
                }
                else {
                    regexBuilder.append("*");
                }
            }
            else {
                regexBuilder.append(Pattern.quote(t.text));
            }
        }
        Pattern regex = Pattern.compile(regexBuilder.toString());
        boolean similar = regex.matcher(prevGenCode).matches();
        return !similar;
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
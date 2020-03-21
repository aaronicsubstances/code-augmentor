package com.aaronicsubstances.code.augmentor.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.models.PreCodeAugmentationResult;
import com.aaronicsubstances.code.augmentor.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.parsing.Token;
import com.aaronicsubstances.code.augmentor.parsing.java.JavaParser;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class CodeAugmentationTask extends Task {
    private String encoding;
    private boolean verbose = false;
    private File prepfile;
    private File destdir;
    private boolean generate = false;
    private String newline;

    private final List<File> generatedCodeFiles = new ArrayList<>();
    
    // validation results
	private Charset charset;

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    public void setGenerate(boolean generate) {
        this.generate = generate;
    }

    public void setPrepfile(File f) {
        this.prepfile = f;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    public void addGen_code_file(File f) {
        generatedCodeFiles.add(f);
    }
    
    public void execute() {
        Instant startInstant = Instant.now();
        validate();
        try {
            _execute();
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new BuildException("I/O error", ex);
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
            
        Instant endInstant = Instant.now();
        long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
        logVerbose("completed in %s ms", timeElapsed);
    }

    private void validate() {
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            }
            catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
                throw new BuildException("Invalid value for encoding attribute: " +
                    encoding, ex);
            }
        }

        if (generate && destdir == null) {
            throw new BuildException("destdir attribute must be set if generate property is true");
        }
        if (prepfile == null) {
            throw new BuildException("prepfile attribute is required");
        }
        if (generatedCodeFiles.isEmpty()) {
            throw new BuildException("at least one gen_code_file nested element is required");
        }

        // set defaults.
        if (encoding == null) {
            charset = Charset.defaultCharset();
        }
        if (newline == null) {
            newline = System.lineSeparator();
        }
    }

    private void _execute() throws Exception {
        PreCodeAugmentationResult result = new PreCodeAugmentationResult();
        Object resultReader = result.beginDeserializer(prepfile);

        GeneratedCodeFetcher generatedCodeFetcher = new GeneratedCodeFetcher(generatedCodeFiles);

        SourceFileDescriptor sourceFileDescriptor = new SourceFileDescriptor();
        while ((sourceFileDescriptor.deserialize(resultReader))) {
            File srcFile = new File(sourceFileDescriptor.getDir(),
                sourceFileDescriptor.getRelativePath());
            logVerbose("Processing %s", srcFile);
            Instant startInstant = Instant.now();
            String sourceCode = TaskUtils.readFile(srcFile, charset);
            String inputHash = TaskUtils.calcHash(sourceCode, charset);
            if (!inputHash.equals(sourceFileDescriptor.getContentHash())) {
                throw new BuildException(String.format("Changes to source files detected starting with %s. Regeneration required.",
                    srcFile));
            }

            // fetch applicable generated code per aug code descriptor.
            List<String> generatedCodes = new ArrayList<>();
            List<String> sourceFileImports = new ArrayList<>();
            for (CodeSnippetDescriptor snippetDescriptor : sourceFileDescriptor.getBodySnippets()) {
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                int augCodeIndex = augCodeDescriptor.getIndex();

                GeneratedCode genCode = generatedCodeFetcher.getGeneratedCode(augCodeIndex);
                if (genCode == null) {
                    throw new BuildException("Could not find generated code for aug code section " +
                        String.format("at %s - %s in %s", augCodeDescriptor.getStartPos(),
                        augCodeDescriptor.getEndPos(), srcFile));
                }
                if (genCode.isError()) {
                    throw new BuildException("Generation of code failed for aug code section " +
                        String.format("at %s - %s in %s: %s\n%s", augCodeDescriptor.getStartPos(),
                        augCodeDescriptor.getEndPos(), srcFile,
                        genCode.getHeaderContent(),
                        genCode.getBodyContent()));
                }
                List<String> headerImports;
                try {
                    headerImports = parseHeaderImports(sourceFileDescriptor.getRelativePath(),
                        genCode);
                }
                catch (ParserException ex) {
                    throw new BuildException("Invalid generated header code for aug code section " +
                        String.format("at %s - %s in %s", augCodeDescriptor.getStartPos(),
                        augCodeDescriptor.getEndPos(), srcFile), ex);
                }
                if (!headerImports.isEmpty() && sourceFileDescriptor.getHeaderSnippet() == null) {
                    throw new BuildException("No header code section specified to place generated header " +
                        "code for aug code section " +
                        String.format("at %s - %s in %s", augCodeDescriptor.getStartPos(),
                        augCodeDescriptor.getEndPos(), srcFile));
                }
                List<Token> tokens;
                try {
                    tokens = TaskUtils.parseSourceCode(sourceFileDescriptor.getRelativePath(), 
                        genCode.getBodyContent()).parse();
                }
                catch (ParserException ex) {
                    throw new BuildException("Invalid generated code for aug code section " +
                        String.format("at %s - %s in %s", augCodeDescriptor.getStartPos(),
                        augCodeDescriptor.getEndPos(), srcFile), ex);
                }
                boolean canIndent = canIndentCode(augCodeDescriptor, tokens);
                String formattedCode = genCode.getBodyContent();
                if (canIndent) {
                    formattedCode = indentCode(formattedCode, augCodeDescriptor.getIndent());
                }
                formattedCode = wrapInGeneratedCodeComments(formattedCode, 
                    result.getGenCodeStartSuffix(), result.getGenCodeEndSuffix());
                if (!augCodeDescriptor.isAnnotatedWithSlashStar()) {
                    formattedCode = newline + formattedCode;
                }
                generatedCodes.add(formattedCode);
                sourceFileImports.addAll(headerImports);
            }

            // identify new imports.
            String headerImport = null;
            if (sourceFileDescriptor.getHeaderSnippet() != null) {
                String indent = sourceFileDescriptor.getHeaderSnippet()
                    .getAugmentingCodeDescriptor().getIndent();
                List<String> existingImports = sourceFileDescriptor.getImportStatements();
                headerImport = String.join(newline, sourceFileImports.stream().distinct()
                    .filter(imp -> !existingImports.contains(imp))
                    .sorted()
                    .collect(Collectors.toList()));
                if (!TaskUtils.isNullOrEmpty(indent)) {
                    headerImport = indentCode(headerImport, indent);
                }
                headerImport = wrapInGeneratedCodeComments(headerImport, 
                    result.getGenCodeStartSuffix(), result.getGenCodeEndSuffix());
                headerImport = newline + headerImport;
            }

            // Now merge generated code into source code.
            SourceCodeTransformer transformer = new SourceCodeTransformer(sourceCode);
            int i;
            for (i = 0; i < generatedCodes.size(); i++) {
                CodeSnippetDescriptor snippetDescriptor = sourceFileDescriptor.getBodySnippets().get(i);
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                GeneratedCodeDescriptor genCodeDescriptor = snippetDescriptor.getGeneratedCodeDescriptor();
                if (sourceFileDescriptor.getHeaderSnippet() != null) {
                    CodeSnippetDescriptor headerSnippet = sourceFileDescriptor.getHeaderSnippet();
                    AugmentingCodeDescriptor headerAugCodeDescriptor = headerSnippet.getAugmentingCodeDescriptor();
                    if (headerAugCodeDescriptor.getStartPos() < augCodeDescriptor.getStartPos()) {
                        break;
                    }
                }
                String genCode = generatedCodes.get(i);
                if (genCodeDescriptor != null) {
                    transformer.addTransform(genCode, genCodeDescriptor.getStartPos(),
                        genCodeDescriptor.getEndPos());
                }
                else {
                    transformer.addTransform(genCode, augCodeDescriptor.getEndPos());
                }
            }
            if (sourceFileDescriptor.getHeaderSnippet() != null) {
                CodeSnippetDescriptor headerSnippet = sourceFileDescriptor.getHeaderSnippet();
                AugmentingCodeDescriptor headerAugCodeDescriptor = headerSnippet.getAugmentingCodeDescriptor();
                GeneratedCodeDescriptor genCodeDescriptor = headerSnippet.getGeneratedCodeDescriptor();
                String genCode = headerImport;
                if (genCodeDescriptor != null) {
                    transformer.addTransform(genCode, genCodeDescriptor.getStartPos(),
                        genCodeDescriptor.getEndPos());
                }
                else {
                    transformer.addTransform(genCode, headerAugCodeDescriptor.getEndPos());
                }
            }
            for (; i < generatedCodes.size(); i++) {
                CodeSnippetDescriptor snippetDescriptor = sourceFileDescriptor.getBodySnippets().get(i);
                AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
                GeneratedCodeDescriptor genCodeDescriptor = snippetDescriptor.getGeneratedCodeDescriptor();
                String genCode = generatedCodes.get(i);
                if (genCodeDescriptor != null) {
                    transformer.addTransform(genCode, genCodeDescriptor.getStartPos(),
                        genCodeDescriptor.getEndPos());
                }
                else {
                    transformer.addTransform(genCode, augCodeDescriptor.getEndPos());
                }
            }

            String transformedCode = transformer.getTransformedText();
            if (!sourceCode.equals(transformedCode)) {
                if (generate) {
                    File destFile = new File(destdir, sourceFileDescriptor.getRelativePath());
                    TaskUtils.writeFile(destFile, charset, transformedCode);
                }
                else {
                    throw new BuildException(String.format("Augmenting code needs regeneration " +
                        "starting with %s.", srcFile));
                }
            }
            
            Instant endInstant = Instant.now();
            long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
            logVerbose("done in %s ms", timeElapsed);

            sourceFileDescriptor = new SourceFileDescriptor();
        }

        // close readers
        result.endDeserialize(resultReader);
        generatedCodeFetcher.close();
    }

    private static boolean canIndentCode(AugmentingCodeDescriptor augCodeDescriptor, List<Token> tokens) {
        if (TaskUtils.isNullOrEmpty(augCodeDescriptor.getIndent())) {
            return false;
        }
        for (Token token : tokens) {
            if (token.type == JavaParser.TOKEN_TYPE_LITERAL_STRING_CONTENT) {
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
        for (String line : lines) {
            if (indentedContent.length() > 0) {
                indentedContent.append(newline);
            }
            indentedContent.append(indent).append(line);
        }
        return indentedContent.toString();
    }

    private String wrapInGeneratedCodeComments(String content, String genCodeStart, String genCodeEnd) {
        if (LexerSupport.NEW_LINE_REGEX.matcher(content).find()) {
            //use double slashes
            return "//" + genCodeStart + newline + content +
                "//" + genCodeEnd + newline;
        }
        else {
            // use slash star
            return "/*" + genCodeStart + "*/" + content +
                "/*" + genCodeEnd + "*/";
        }
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

    private void logVerbose(String message, Object... args) {
        if (verbose) {
            log("[" + String.format(message, args) + "]");
        }
    }
}
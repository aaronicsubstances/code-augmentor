package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileDescriptor;
import com.aaronicsubstances.code.augmentor.core.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.core.parsing.Token;

public class CodeGenerationRequestCreator {
    static final int TOKEN_TYPE_DIRECTIVE = 1;
    static final int TOKEN_TYPE_BLANK = 2;
    static final int TOKEN_TYPE_OTHER = 3;

    static final int DIRECTIVE_TYPE_GEN_CODE_START = 1;
    static final int DIRECTIVE_TYPE_GEN_CODE_END = 2;
    static final int DIRECTIVE_TYPE_EMB_STRING = 3;
    static final int DIRECTIVE_TYPE_AUG_CODE = 4;
    static final int DIRECTIVE_TYPE_ENABLE_SCAN = 5;
    static final int DIRECTIVE_TYPE_DISABLE_SCAN = 6;

    private final List<String> genCodeStartDirectives;
    private final List<String> genCodeEndDirectives;
    private final List<String> embeddedStringDirectives;
    private final List<List<String>> dataDrivenAugCodeDirectiveSets;
    private final List<List<String>> uncheckedAugCodeDirectiveSets;
    private final List<String> enableScanDirectives;
    private final List<String> disableScanDirectives;

    public CodeGenerationRequestCreator(
            List<String> genCodeStartDirectives,
            List<String> genCodeEndDirectives,
            List<String> embeddedStringDirectives,
            List<List<String>> dataDrivenAugCodeDirectiveSets,
            List<List<String>> uncheckedAugCodeDirectiveSets,
            List<String> enableScanDirectives, 
            List<String> disableScanDirectives) {
        this.genCodeStartDirectives = genCodeStartDirectives;
        this.genCodeEndDirectives = genCodeEndDirectives;
        this.embeddedStringDirectives = embeddedStringDirectives;
        this.dataDrivenAugCodeDirectiveSets = dataDrivenAugCodeDirectiveSets;
        this.uncheckedAugCodeDirectiveSets = uncheckedAugCodeDirectiveSets;
        this.enableScanDirectives = enableScanDirectives;
        this.disableScanDirectives = disableScanDirectives;
    }

    public void processSourceFile(SourceFileDescriptor sourceDescriptor, String source,
            List<List<AugmentingCode>> specAugCodesList, List<ParserException> errors) {        
        // 1. first, tokenize source code and mark out all directive tokens.
        List<Token> tokens = tokenizeSource(source);
        
        // 2. next, identify aug code sections.
        List<List<Token>> augCodeSections = identifyAugCodeSections(tokens, sourceDescriptor, 
            errors);
        
        // 3. validate aug code sections.
        for (List<Token> augCodeSection : augCodeSections) {
            ParserException error = validateAugCodeSection(augCodeSection,
                sourceDescriptor);
            if (error != null) {
                if (errors == null) {
                    throw error;
                }
                errors.add(error);
            }
        }

        // 4a. If there are no validation errors,
        if (errors != null && !errors.isEmpty()) {
            return;
        }

        // 4b. generate aug code blocks and associated descriptors
        List<CodeSnippetDescriptor> bodySnippets = new ArrayList<>();
        for (int i = 0; i < augCodeSections.size(); i++) {
            List<Token> augCodeSection = augCodeSections.get(i);
            Token firstToken = augCodeSection.get(0);
            Token lastToken = augCodeSection.get(augCodeSection.size() - 1);
            
            // a. create aug code descriptor.
            AugmentingCodeDescriptor augCodeDescriptor = new AugmentingCodeDescriptor();
            augCodeDescriptor.setIndex(i);
            augCodeDescriptor.setStartPos(firstToken.startPos);
            augCodeDescriptor.setEndPos(lastToken.endPos);
            String indent = augCodeSection.stream().map(x -> x.indent)
                .min((x, y) -> new Integer(x.length()).compareTo(y.length())).get();
            augCodeDescriptor.setIndent(indent);

            // b. create gen code descriptor.
            GeneratedCodeDescriptor genCodeDescriptor = createGeneratedCodeDescriptor(tokens, 
                lastToken.index);

            CodeSnippetDescriptor bodySnippet = new CodeSnippetDescriptor();
            bodySnippet.setAugmentingCodeDescriptor(augCodeDescriptor);
            bodySnippet.setGeneratedCodeDescriptor(genCodeDescriptor);
            bodySnippets.add(bodySnippet);

            // c. create aug code.
            List<Block> blocks = createAugmentingCodeBlocks(augCodeSection);
            AugmentingCode augmentingCode = new AugmentingCode(blocks);
            augmentingCode.setIndex(i);
            augmentingCode.setIndent(augCodeDescriptor.getIndent());
            augmentingCode.setDirectiveMarker(firstToken.directiveMarker);
            augmentingCode.setDataDriven(!firstToken.uncheckedAugCodeDirective);

            // d. validate data-driven string block as valid JSON array.
            if (augmentingCode.isDataDriven()) {
                if (blocks.size() != 2) {                    
                    ParserException error = createParserException(
                        "Missing embedded string section of data-driven augmenting code.", 
                        firstToken, sourceDescriptor);
                    if (errors == null) {
                        throw error;
                    }
                    errors.add(error);
                }
                else {
                    boolean isValidJsonArray = TaskUtils.isValidJsonArray(blocks.get(1).getContent());
                    if (!isValidJsonArray) {
                        Token embStrTokenStart = augCodeSection.get(1);
                        ParserException error = createParserException(
                            "Embedded string section of data-driven augmenting code must be a " +
                            "valid JSON array", 
                            embStrTokenStart, sourceDescriptor);
                        if (errors == null) {
                            throw error;
                        }
                        errors.add(error);
                    }
                }
            }
            
            List<AugmentingCode> applicableAugCodeList = specAugCodesList.get(
                firstToken.augCodeSpecIndex);
            applicableAugCodeList.add(augmentingCode);
        }

        sourceDescriptor.setBodySnippets(bodySnippets);
    }

    List<Token> tokenizeSource(String source) {
        List<String> splitSource = TaskUtils.splitIntoLines(source);
        List<Token> tokens = new ArrayList<>();
        int startPos = 0;
        for (int i = 0; i < splitSource.size(); i+=2) {
            String line = splitSource.get(i);
            String terminator = splitSource.get(i + 1);
            String lineWithoutIndent = line.trim();
            Token t = null;
            if (lineWithoutIndent.isEmpty()) {
                t = new Token(TOKEN_TYPE_BLANK);
            }
            if (t == null) {
                // Pick longest token out of all candidate
                // directive tokens.
                List<Token> candidateTokens = new ArrayList<>();
                for (String d : genCodeStartDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        candidateTokens.add(createToken(DIRECTIVE_TYPE_GEN_CODE_START, d, line));
                        break;
                    }
                }
                
                for (String d : genCodeEndDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        candidateTokens.add(createToken(DIRECTIVE_TYPE_GEN_CODE_END, d, line));
                        break;
                    }
                }
                
                for (String d : embeddedStringDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        candidateTokens.add(createToken(DIRECTIVE_TYPE_EMB_STRING, d, line));
                        break;
                    }
                }
                
                if (enableScanDirectives != null) {
                    for (String d : enableScanDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            candidateTokens.add(createToken(DIRECTIVE_TYPE_ENABLE_SCAN, d, line));
                            break;
                        }
                    }
                }
                
                if (disableScanDirectives != null) {
                    for (String d : disableScanDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            candidateTokens.add(createToken(DIRECTIVE_TYPE_DISABLE_SCAN, d, line));
                            break;
                        }
                    }
                }
                
                outer1: for (int j = 0; j < dataDrivenAugCodeDirectiveSets.size(); j++) {
                    List<String> augCodeDirectives = dataDrivenAugCodeDirectiveSets.get(j);
                    if (augCodeDirectives == null) {
                        continue;
                    }
                    for (String d : augCodeDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            Token c = createToken(DIRECTIVE_TYPE_AUG_CODE, d, line);
                            c.augCodeSpecIndex = j;
                            candidateTokens.add(c);
                            break outer1;
                        }
                    }
                }
                
                outer2: for (int j = 0; j < uncheckedAugCodeDirectiveSets.size(); j++) {
                    List<String> augCodeDirectives = uncheckedAugCodeDirectiveSets.get(j);
                    if (augCodeDirectives == null) {
                        continue;
                    }
                    for (String d : augCodeDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            Token c = createToken(DIRECTIVE_TYPE_AUG_CODE, d, line);
                            c.augCodeSpecIndex = j;
                            c.uncheckedAugCodeDirective = true;
                            candidateTokens.add(c);
                            break outer2;
                        }
                    }
                }

                Optional<Token> tOpt = candidateTokens.stream().max(
                    (x, y) -> new Integer(x.text.length()).compareTo(y.text.length()));
                if (tOpt.isPresent()) {
                    t = tOpt.get();
                }
            }
            if (t == null) {
                // token must be of some other type.
                t = new Token(TOKEN_TYPE_OTHER);
            }
            t.text = line;
            if (terminator != null) {
                t.text += terminator;
                t.newline = terminator;
            }
            t.startPos = startPos;
            t.endPos = startPos + t.text.length();
            // collapse (line, terminator) pair from split source
            // to single index.
            t.index = i / 2;
            t.lineNumber = t.index + 1;
            
            tokens.add(t);

            startPos = t.endPos;
        }
        return tokens;
    }

    static List<List<Token>> identifyAugCodeSections(List<Token> tokens,
            SourceFileDescriptor sourceDescriptor, List<ParserException> errors) { 
        List<List<Token>> groups = new ArrayList<>();
        boolean scanEnabled = true;
        // group tokens which strictly follow each other consecutively in line numbers.
        int expectedLineNumber = 0;
        List<Token> currentGroup = new ArrayList<>();
        for (Token t : tokens) {
            if (!scanEnabled) {
                if (t.directiveType == DIRECTIVE_TYPE_ENABLE_SCAN) {
                    scanEnabled = true;
                }
                continue;
            }
            if (t.directiveType == DIRECTIVE_TYPE_AUG_CODE || 
                    t.directiveType == DIRECTIVE_TYPE_EMB_STRING ||
                    t.directiveType == DIRECTIVE_TYPE_GEN_CODE_START ||
                    t.directiveType == DIRECTIVE_TYPE_GEN_CODE_END) {
                // ensure newline ending.
                if (t.newline == null) {
                    ParserException error = createParserException(
                        "Augmenting or generated code directives must end with a newline", 
                        t, sourceDescriptor);
                    if (errors != null) {
                        errors.add(error);
                    }
                    else {
                        throw error;
                    }
                }
            }
            switch (t.directiveType) {
                case DIRECTIVE_TYPE_AUG_CODE:
                case DIRECTIVE_TYPE_EMB_STRING:
                    if (expectedLineNumber == 0 || expectedLineNumber == t.lineNumber) {
                        expectedLineNumber = t.lineNumber + 1;
                    }
                    else {
                        assert !currentGroup.isEmpty();
                        groups.add(currentGroup);
                        currentGroup = new ArrayList<>();
                        expectedLineNumber = 0;
                    }
                    currentGroup.add(t);
                    break;
                default:
                    if (!currentGroup.isEmpty()) {
                        groups.add(currentGroup);
                        currentGroup = new ArrayList<>();
                        expectedLineNumber = 0;
                    }
                    if (t.directiveType == DIRECTIVE_TYPE_DISABLE_SCAN) {
                        scanEnabled = false;
                    }  
                    break;
            }
        }
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        return groups;
    }

    static ParserException validateAugCodeSection(List<Token> tokenGroup, 
            SourceFileDescriptor sourceDescriptor) {
        Token token = tokenGroup.get(0);
        if (token.directiveType == DIRECTIVE_TYPE_AUG_CODE) {
            int expectedAugCodeSpecIndex = token.augCodeSpecIndex;
            boolean expectedUncheckedStatus = token.uncheckedAugCodeDirective;
            for (int i = 1; i < tokenGroup.size(); i++) {
                token = tokenGroup.get(i);
                switch (token.directiveType) {
                    case DIRECTIVE_TYPE_EMB_STRING:
                        break;
                    default:
                        assert token.directiveType == DIRECTIVE_TYPE_AUG_CODE;
                        if (expectedUncheckedStatus != token.uncheckedAugCodeDirective) {
                            return createParserException("Mixing data-driven and unchecked " +
                                "augmenting code directives not allowed.", 
                                token, sourceDescriptor);
                        }
                        if (expectedUncheckedStatus) {
                            if (expectedAugCodeSpecIndex != token.augCodeSpecIndex) {
                                return createParserException("Different augmenting code directives in " +
                                    "same section not allowed", token, sourceDescriptor);
                            }
                        }
                        else {
                            return createParserException("Data-driven augmenting code directive can " +
                                "only start a section", token, sourceDescriptor);
                        }
                        break;
                }
            }
        }
        else {
            assert token.directiveType == DIRECTIVE_TYPE_EMB_STRING;
            return createParserException("Embedded string directive cannot start an " +
                "augmenting code section", token, sourceDescriptor);
        }
        return null;
    }

    static GeneratedCodeDescriptor createGeneratedCodeDescriptor(List<Token> sourceTokens, 
            int augCodeEndIndex) {
        // search for gen code start.
        int startIndex = -1;
        for (int i = augCodeEndIndex + 1; i < sourceTokens.size(); i++) {
            Token t = sourceTokens.get(i);
            // only tolerate blank lines as the only different token to expect,
            // not even gen code end.
            if (t.type == TOKEN_TYPE_BLANK) {
                continue;
            }
            if (t.directiveType == DIRECTIVE_TYPE_GEN_CODE_START) {
                startIndex = i;
            }
            break;
        }

        if (startIndex != -1) {
            // search for gen code end.
            for (int i = startIndex + 1; i < sourceTokens.size(); i++) {
                Token t = sourceTokens.get(i);
                // skip any non-directives found.
                if (t.type != TOKEN_TYPE_DIRECTIVE) {
                    continue;
                }
                if (t.directiveType == DIRECTIVE_TYPE_GEN_CODE_END) {
                    Token st = sourceTokens.get(startIndex);
                    GeneratedCodeDescriptor generatedCodeDescriptor = new GeneratedCodeDescriptor();
                    generatedCodeDescriptor.setStartDirectiveStartPos(st.startPos);
                    generatedCodeDescriptor.setStartDirectiveEndPos(st.endPos);
                    generatedCodeDescriptor.setEndDirectiveStartPos(t.startPos);
                    generatedCodeDescriptor.setEndDirectiveEndPos(t.endPos);
                    return generatedCodeDescriptor;
                }
                else {
                    // different directive encountered. conclude as if there is
                    // no generated code section.
                    break;
                }
            }
        }

        return null;
    }

    static List<Block> createAugmentingCodeBlocks(List<Token> augCodeSection) {
        // Consolidate tokens
        List<Block> blocks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        Block lastBlock = new Block(); // first block definitely not stringified.
        blocks.add(lastBlock);
        for (int i = 0; i < augCodeSection.size(); i++) {
            Token t = augCodeSection.get(i);
            String contentLine = t.directiveContent;
            boolean stringify = t.directiveType == DIRECTIVE_TYPE_EMB_STRING;
            
            if (lastBlock.isStringify() == stringify) {
                // Add new lines to content lines, with requirement that no section
                // has either preceding or trailing new line.
                if (i > 0) {
                    sb.append(augCodeSection.get(i - 1).newline);
                }
                sb.append(contentLine);
            }
            else {
                lastBlock.setContent(sb.toString());

                // reset for use with next block.
                lastBlock = new Block();
                lastBlock.setStringify(stringify);
                blocks.add(lastBlock);
                sb.setLength(0);
                sb.append(contentLine);
            }
        }

        // complete last block.
        lastBlock.setContent(sb.toString());
        return blocks;
    }

    private static Token createToken(int directiveType, String directiveMarker, String line) {
        Token t = new Token(TOKEN_TYPE_DIRECTIVE);
        t.directiveType = directiveType; 
        t.directiveMarker = directiveMarker;
        int dIndex = line.indexOf(directiveMarker);
        t.indent = line.substring(0, dIndex);
        t.directiveContent = line.substring(dIndex + directiveMarker.length());
        // set text even though we don't have newline, so search 
        // for directive token with max length can work.
        t.text = line;
        return t;
    }

    private static ParserException createParserException(String errorMessage, Token token,
            SourceFileDescriptor sourceDescriptor) {
        String fullMessage = String.format("in %s at line %s: %s\n\n%s",
            new File(sourceDescriptor.getDir(), sourceDescriptor.getRelativePath()),
            token.lineNumber, errorMessage, token.text);
        return new ParserException(fullMessage);
    }
}
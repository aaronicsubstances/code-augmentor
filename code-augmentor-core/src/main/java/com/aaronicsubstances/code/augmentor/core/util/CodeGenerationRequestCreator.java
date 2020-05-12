package com.aaronicsubstances.code.augmentor.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;

public class CodeGenerationRequestCreator {
    private static final Pattern GSON_ERROR_MESSAGE_REGEX = Pattern.compile(
        "at\\s+line\\s+(\\d+)[^c]*(?:column\\s+(\\d+))?", Pattern.CASE_INSENSITIVE);

    public static List<CodeSnippetDescriptor> processSourceFile(
            List<Token> tokens, File srcFile,
            List<List<AugmentingCode>> specAugCodesList, List<Exception> errors) {        
        // 1. First identify aug code sections.
        List<List<Token>> augCodeSections = identifyAugCodeSections(tokens, srcFile, 
            errors);
        
        // 2. validate aug code sections.
        for (List<Token> augCodeSection : augCodeSections) {
            validateAugCodeSection(augCodeSection, srcFile, errors);
        }

        // 3a. If there are no validation errors,
        if (errors != null && !errors.isEmpty()) {
            return null;
        }

        // 3b. generate aug code blocks and associated descriptors
        List<CodeSnippetDescriptor> bodySnippets = new ArrayList<>();
        for (int i = 0; i < augCodeSections.size(); i++) {
            List<Token> augCodeSection = augCodeSections.get(i);
            Token firstToken = augCodeSection.get(0);
            Token lastToken = augCodeSection.get(augCodeSection.size() - 1);
            
            // a. create aug code descriptor.
            AugmentingCodeDescriptor augCodeDescriptor = new AugmentingCodeDescriptor();
            augCodeDescriptor.setId(i + 1); // 1-based, so 0 signals not set.
            augCodeDescriptor.setStartPos(firstToken.startPos);
            augCodeDescriptor.setLineNumber(firstToken.lineNumber);
            augCodeDescriptor.setEndPos(lastToken.endPos);
            augCodeDescriptor.setLineSeparator(lastToken.newline);
            String indent = augCodeSection.stream().map(x -> x.indent)
                .min((x, y) -> Integer.compare(x.length(), y.length())).get();
            augCodeDescriptor.setIndent(indent);

            // b. create gen code descriptor.
            GeneratedCodeDescriptor genCodeDescriptor = createGeneratedCodeDescriptor(tokens,
                lastToken.index);

            CodeSnippetDescriptor bodySnippet = new CodeSnippetDescriptor();
            bodySnippet.setAugmentingCodeDescriptor(augCodeDescriptor);
            bodySnippet.setGeneratedCodeDescriptor(genCodeDescriptor);
            bodySnippets.add(bodySnippet);

            // c. create aug code.
            List<Integer> blockDelimiters = new ArrayList<>();
            List<Block> blocks = createAugmentingCodeBlocks(augCodeSection, blockDelimiters);
            AugmentingCode augmentingCode = new AugmentingCode(blocks);
            augmentingCode.setId(augCodeDescriptor.getId());
            augmentingCode.setIndent(augCodeDescriptor.getIndent());
            augmentingCode.setDirectiveMarker(firstToken.directiveMarker);
            augmentingCode.setLineNumber(augCodeDescriptor.getLineNumber());
            augmentingCode.setLineSeparator(augCodeDescriptor.getLineSeparator());
            augmentingCode.setNestedLevelNumber(firstToken.nestedLevelNumber);
            augmentingCode.setHasNestedLevelStartMarker(firstToken.nestedLevelStartMarker != null);
            augmentingCode.setHasNestedLevelEndMarker(firstToken.nestedLevelEndMarker != null);
            
            // d. validate json directive contents.
            for (int j = 0; j < blocks.size(); j++) {
                Block b = blocks.get(j);
                if (b.isJsonify() && TaskUtils.validateJson(b.getContent()) != null) {
                    GenericTaskException jsonValidationError = createJsonValidationError(j,
                        augCodeSection, blockDelimiters, srcFile);
                    saveOrThrowError(jsonValidationError, errors);
                }
            }
            
            List<AugmentingCode> applicableAugCodeList = specAugCodesList.get(
                firstToken.augCodeSpecIndex);
            applicableAugCodeList.add(augmentingCode);
        }

        return bodySnippets;
    }

    static List<List<Token>> identifyAugCodeSections(List<Token> tokens,
            File srcFile, List<Exception> errors) { 
        List<List<Token>> groups = new ArrayList<>();
        Token skipCodeStartToken = null;
        int expectedLineNumber = 0;
        Stack<Token> nestedLevelStartTokens = new Stack<>();
        // group tokens which strictly follow each other consecutively in line numbers.
        List<Token> currentGroup = new ArrayList<>();
        for (Token t : tokens) {
            // As long as we are inside a skip code section, ignore all
            // directives until we hit a directive indicating end of skip code
            // section.
            if (skipCodeStartToken != null) {
                if (t.type == Token.DIRECTIVE_TYPE_SKIP_CODE_START) {
                    GenericTaskException error = createException(
                        "Expecting end of " +
                        (skipCodeStartToken.isGeneratedCodeMarker ? "generated" : "skipped") + 
                        " code section before encountering another start directive at line " +
                        t.lineNumber, 
                        skipCodeStartToken, srcFile);
                    saveOrThrowError(error, errors);
                    if (t.isInlineGeneratedCodeMarker) {
                        skipCodeStartToken = null;
                    }
                    else {
                        skipCodeStartToken = t;
                    }
                }
                if (t.type == Token.DIRECTIVE_TYPE_SKIP_CODE_END) {
                    if (t.isGeneratedCodeMarker != skipCodeStartToken.isGeneratedCodeMarker) {
                        GenericTaskException error = createException(
                            "Different end directive encountered for " +                            
                            (skipCodeStartToken.isGeneratedCodeMarker ? "generated" : "skipped") + 
                            " code section from line " + skipCodeStartToken.lineNumber,
                            t, srcFile);
                        saveOrThrowError(error, errors);
                    }
                    skipCodeStartToken = null;
                }
                continue;
            }
            
            // handle group assignment.
            switch (t.type) {
                case Token.DIRECTIVE_TYPE_AUG_CODE:
                case Token.DIRECTIVE_TYPE_EMB_STRING:
                case Token.DIRECTIVE_TYPE_EMB_JSON:
                    if (expectedLineNumber == 0 || expectedLineNumber == t.lineNumber) {
                        // all's well. don't create a new group before adding token.
                    }
                    else {
                        // line number is not what's expected.
                        // create a new group, before adding token.
                        assert !currentGroup.isEmpty();
                        groups.add(currentGroup);
                        currentGroup = new ArrayList<>();
                    }
                    currentGroup.add(t);
                    expectedLineNumber = t.lineNumber + 1;
                    break;
                default:
                    if (!currentGroup.isEmpty()) {
                        // Expected aug/emb directive but found something else. 
                        // Create new group.
                        groups.add(currentGroup);
                        currentGroup = new ArrayList<>();
                        // set to 0 so a new aug/emb token is definitely added. 
                        expectedLineNumber = 0;
                    }
                    break;
            }

            // handle start of skip/generated code sections.
            if (t.type == Token.DIRECTIVE_TYPE_SKIP_CODE_END) {
                GenericTaskException error = createException(
                    "Encountered end directive for " +
                    (t.isGeneratedCodeMarker ? "generated" : "skipped") +
                    " code section without a previous start directive.",
                    t, srcFile);
                saveOrThrowError(error, errors);
            }
            else if (t.type == Token.DIRECTIVE_TYPE_SKIP_CODE_START) {
                if (!t.isInlineGeneratedCodeMarker) {
                    skipCodeStartToken = t;
                }
            }

            else {                
                // set nested level number for tokens not skipped by default, and 
                // later set nested level end token differently.
                t.nestedLevelNumber = nestedLevelStartTokens.size();

                // handle nesting of aug code sections.
                if (t.type == Token.DIRECTIVE_TYPE_AUG_CODE) {
                    if (t.nestedLevelStartMarker != null) {
                        nestedLevelStartTokens.push(t);
                    }
                    else if (t.nestedLevelEndMarker != null) {
                        if (nestedLevelStartTokens.isEmpty()) {
                            GenericTaskException error = createException(
                                "Encountered nested level end marker for aug " +
                                "code section without a previous matching start marker.",
                                t, srcFile);
                            saveOrThrowError(error, errors);
                        }
                        else {
                            Token correspondingStartToken = nestedLevelStartTokens.pop();
                            t.nestedLevelNumber--;
                            if (correspondingStartToken.augCodeSpecIndex != t.augCodeSpecIndex) {
                                GenericTaskException error = createException(
                                    "Encountered nested level end marker for aug " +
                                    "code section of a different kind than its " +
                                    "matching start marker from line " +
                                    correspondingStartToken.lineNumber,
                                    t, srcFile);
                                saveOrThrowError(error, errors);
                            }
                        }
                    }
                }
            }
        }
        if (!currentGroup.isEmpty()) {
            // Create final group.
            groups.add(currentGroup);
        }
        if (skipCodeStartToken != null) {
            GenericTaskException error = createException(
                "Could not find end of " +
                (skipCodeStartToken.isGeneratedCodeMarker ? "generated" : "skipped") +
                " code section", 
                skipCodeStartToken, srcFile);
            saveOrThrowError(error, errors);
        }
        while (!nestedLevelStartTokens.isEmpty()) {
            Token t = nestedLevelStartTokens.pop();
            GenericTaskException error = createException(
                "Could not find nested level end marker for aug" +
                " code section start marker",
                t, srcFile);
            saveOrThrowError(error, errors);
        }
        if (!tokens.isEmpty()) {
            ensureDirectiveNewlineEnding(tokens.get(tokens.size() - 1), srcFile, errors);
        }
        return groups;
    }

    static void ensureDirectiveNewlineEnding(Token t, File srcFile, List<Exception> errors) {
        if (t.type != Token.TYPE_BLANK && t.type != Token.TYPE_OTHER && t.newline == null) {
            String desc;
            switch (t.type) {
                case Token.DIRECTIVE_TYPE_AUG_CODE:
                    desc = "Augmenting code";
                    break;
                case Token.DIRECTIVE_TYPE_EMB_STRING:
                    desc = "Embedded string";
                    break;
                case Token.DIRECTIVE_TYPE_EMB_JSON:
                    desc = "Embedded JSON";
                    break;
                case Token.DIRECTIVE_TYPE_SKIP_CODE_START:
                    if (t.isGeneratedCodeMarker) {
                        if (t.isInlineGeneratedCodeMarker) {
                            desc = "Inline generated code";
                        }
                        else {
                            desc = "Generated code start";
                        }
                    }
                    else {
                        desc = "Skip code start";
                    }
                    break;
                case Token.DIRECTIVE_TYPE_SKIP_CODE_END:
                    if (t.isGeneratedCodeMarker) {
                        desc = "Generated code end";
                    }
                    else {
                        desc = "Skip code end";
                    }
                    break;
                default:
                    throw new RuntimeException("Unexpected token type: " + t.type);
            }
            GenericTaskException error = createException(
                desc + " directive must end with a newline",
                t, srcFile);
            saveOrThrowError(error, errors);
        }
    }

    static void validateAugCodeSection(List<Token> tokenGroup, File srcFile, 
            List<Exception> errors) {
        Token token = tokenGroup.get(0);
        int expectedAugCodeSpecIndex = -1;
        if (token.type == Token.DIRECTIVE_TYPE_AUG_CODE) {
            expectedAugCodeSpecIndex = token.augCodeSpecIndex;
        }
        else {
            if (token.type == Token.DIRECTIVE_TYPE_EMB_JSON) {
                saveOrThrowError(createException("Embedded JSON directive cannot start an " +
                    "augmenting code section", token, srcFile), errors);
            }
            else {
                assert token.type == Token.DIRECTIVE_TYPE_EMB_STRING;
                saveOrThrowError(createException("Embedded string directive cannot start an " +
                    "augmenting code section", token, srcFile), errors);
            }
        }
        for (int i = 1; i < tokenGroup.size(); i++) {
            token = tokenGroup.get(i);
            switch (token.type) {
                case Token.DIRECTIVE_TYPE_EMB_STRING:
                case Token.DIRECTIVE_TYPE_EMB_JSON:
                    break;
                default:
                    assert token.type == Token.DIRECTIVE_TYPE_AUG_CODE;
                    if (expectedAugCodeSpecIndex != -1 &&
                            expectedAugCodeSpecIndex != token.augCodeSpecIndex) {
                        saveOrThrowError(createException("Different kinds of augmenting code directives in " +
                            "same section not allowed", token, srcFile), errors);
                    }
                    if (token.nestedLevelStartMarker != null) {
                        saveOrThrowError(createException("Only start of augmenting code section " +
                            "can be marked to start a nested level.", token, srcFile), errors);
                    }
                    if (token.nestedLevelEndMarker != null) {
                        saveOrThrowError(createException("Only start of augmenting code section " +
                            "can be marked to end a nested level.", token, srcFile), errors);
                    }
                    break;
            }
        }
    }

    static GeneratedCodeDescriptor createGeneratedCodeDescriptor(List<Token> sourceTokens,
            int augCodeEndIndex) {
        // search for gen code start.
        int startIndex = -1;
        for (int i = augCodeEndIndex + 1; i < sourceTokens.size(); i++) {
            Token t = sourceTokens.get(i);
            // only tolerate blank lines as the only different token to expect,
            // not even gen code end.
            if (t.type == Token.TYPE_BLANK) {
                continue;
            }
            if (t.type == Token.DIRECTIVE_TYPE_SKIP_CODE_START && t.isGeneratedCodeMarker) {
                startIndex = i;
                break;
            }
            else {
                // token other than gen code start found,
                // quit search.
                break;
            }
        }

        if (startIndex == -1) {
            return null;
        }

        Token st = sourceTokens.get(startIndex);

        // depending on whether gen code directive is inline or section start,
        // proceed differently.
        if (st.isInlineGeneratedCodeMarker) {
            GeneratedCodeDescriptor generatedCodeDescriptor = new GeneratedCodeDescriptor();
            generatedCodeDescriptor.setStartDirectiveStartPos(st.startPos);
            generatedCodeDescriptor.setEndDirectiveEndPos(st.endPos);
            generatedCodeDescriptor.setInline(true);
            // look for tokens of the same type as inline gen code,
            // and consecutive in line numbers.
            int expectedLineNumber = st.lineNumber + 1;
            for (int i = startIndex + 1; i < sourceTokens.size(); i++) {
                Token t = sourceTokens.get(i);
                if (t.isInlineGeneratedCodeMarker && t.lineNumber == expectedLineNumber) {
                    generatedCodeDescriptor.setEndDirectiveEndPos(t.endPos);
                    expectedLineNumber++;
                }
                else {
                    break;
                }
            }
            return generatedCodeDescriptor;
        }
        else {
            // search for gen code end.
            for (int i = startIndex + 1; i < sourceTokens.size(); i++) {
                Token t = sourceTokens.get(i);
                // skip all other tokens, except for gen/skip code starts, and
                // skip code end. These three are interpreted as section breakers, and
                // hence current search must end.
                if (t.type == Token.DIRECTIVE_TYPE_SKIP_CODE_END) {
                    if (!t.isGeneratedCodeMarker) {
                        break;
                    }
                    GeneratedCodeDescriptor generatedCodeDescriptor = new GeneratedCodeDescriptor(
                        st.startPos, st.endPos, t.startPos, t.endPos);
                    return generatedCodeDescriptor;
                }
                else if (t.type == Token.DIRECTIVE_TYPE_SKIP_CODE_START) {
                    break;
                }
            }

            throw new RuntimeException("Could not find ending of a generated code section");
        }
    }

    static List<Block> createAugmentingCodeBlocks(List<Token> augCodeSection,
            List<Integer> blockDelimiters) {
        // Consolidate tokens
        List<Block> blocks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        // first block definitely not stringified or jsonified.
        Block lastBlock = new Block(null, false, false);
        blocks.add(lastBlock);
        blockDelimiters.add(0);
        for (int i = 0; i < augCodeSection.size(); i++) {
            Token t = augCodeSection.get(i);
            String contentLine = t.directiveContent;
            boolean stringify = t.type == Token.DIRECTIVE_TYPE_EMB_STRING;
            boolean jsonify = t.type == Token.DIRECTIVE_TYPE_EMB_JSON;
            
            if (lastBlock.isStringify() == stringify && lastBlock.isJsonify() == jsonify) {
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
                lastBlock.setJsonify(jsonify);
                blocks.add(lastBlock);
                sb.setLength(0);
                sb.append(contentLine);
                blockDelimiters.add(i);
            }
        }

        // complete last block.
        lastBlock.setContent(sb.toString());
        return blocks;
    }

    static GenericTaskException createJsonValidationError(int blockIndex, 
            List<Token> augCodeSection, List<Integer> blockDelimiters, File srcFile) {
        String paddedContent = createPaddedJsonEquivalent(blockIndex, augCodeSection, blockDelimiters);
        String jsonValidationError = TaskUtils.validateJson(paddedContent);
        assert jsonValidationError != null;

        // set defaults
        Token blockStartToken = augCodeSection.get(blockIndex);
        String errorMessage = jsonValidationError;
        int lineNumber = blockStartToken.lineNumber;
        String snippet = blockStartToken.text;
        String srcPath = null;
        if (srcFile != null) {
            srcPath = srcFile.getPath();
        }

        // try and do better by identifying snippet corresponding to line number
        // embedded in validation error
        Matcher matcher = GSON_ERROR_MESSAGE_REGEX.matcher(jsonValidationError);
        if (matcher.find()) {
            int embLineNumber = Integer.parseInt(matcher.group(1));
            Optional<Token> errorBlockTokenSearch = augCodeSection.stream()
                .filter(x -> x.lineNumber == embLineNumber)
                .findFirst();
            if (errorBlockTokenSearch.isPresent()) {
                Token errorBlockToken = errorBlockTokenSearch.get();
                lineNumber = errorBlockToken.lineNumber;
                snippet = errorBlockToken.text;

                // if column number available, do even better by pointing out 
                // error position.
                String colStr = matcher.group(2);
                if (!TaskUtils.isEmpty(colStr)) {
                    int embCol = Integer.parseInt(colStr);
                    // NB: col starts from 1. Also, Gson column numbers
                    // may indicate the char after the actual trouble char.
                    // So just point to two chars instead of 1.
                    String pointerPadding = TaskUtils.strMultiply(" ", embCol - 2);
                    // since directive token's text already ends with newline,
                    // no need to add intervening line.
                    snippet += pointerPadding + "^^";
                }
            }
        }
        
        return GenericTaskException.create(null, "Embedded JSON section of augmenting code is not " +
            "valid: " + errorMessage, srcPath, lineNumber, snippet);
    }

    static String createPaddedJsonEquivalent(int blockIndex,
            List<Token> augCodeSection, List<Integer> blockDelimiters) {
        // prefix content with sufficient number of newlines and indents.
        List<Token> blockTokens = new ArrayList<>();
        Token stopToken = null;
        if (blockIndex + 1 < blockDelimiters.size()) {
            stopToken = augCodeSection.get(blockDelimiters.get(blockIndex + 1));
        }
        for (int i = blockDelimiters.get(blockIndex); i < augCodeSection.size(); i++) {
            Token t = augCodeSection.get(i);
            if (t == stopToken) {
                break;
            }
            blockTokens.add(t);
        }
        
        // construct equivalent JSON with whitespace
        StringBuilder paddedContent = new StringBuilder();

        // total number of newlines padded should be 1 less than
        // block line number (which starts from 1).
        Token blockStartToken = blockTokens.get(0);
        paddedContent.append(TaskUtils.strMultiply("\n", blockStartToken.lineNumber - 1));

        for (int i = 0; i < blockTokens.size(); i++) {
            if (i > 0) {
                paddedContent.append("\n");
            }
            Token t = blockTokens.get(i);
            paddedContent.append(t.indent);
            // replace directive marker with whitespace
            paddedContent.append(TaskUtils.strMultiply(" ", t.directiveMarker.length()));
            paddedContent.append(t.directiveContent);
        }
        return paddedContent.toString();
    }

    private static GenericTaskException createException(String errorMessage, Token token,
            File srcFile) {
        String srcPath = null;
        if (srcFile != null) {
            srcPath  = srcFile.getPath();
        }
        return GenericTaskException.create(null, errorMessage, srcPath, token.lineNumber, token.text);
    }

    private static void saveOrThrowError(GenericTaskException error, List<Exception> errors) {
        if (errors == null) {
            throw error;
        }
        errors.add(error);
    }
}
package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.util.ParserException;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;
import com.aaronicsubstances.code.augmentor.core.util.Token;

public class CodeGenerationRequestCreator {

    public static List<CodeSnippetDescriptor> processSourceFile(
            List<Token> tokens, File srcFile,
            List<List<AugmentingCode>> specAugCodesList, List<ParserException> errors) {        
        // 1. First identify aug code sections.
        List<List<Token>> augCodeSections = identifyAugCodeSections(tokens, srcFile, 
            errors);
        
        // 2. validate aug code sections.
        for (List<Token> augCodeSection : augCodeSections) {
            ParserException error = validateAugCodeSection(augCodeSection,
                srcFile);
            if (error != null) {
                saveOrThrowError(error, errors);
            }
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
            augCodeDescriptor.setIndex(i);
            augCodeDescriptor.setStartPos(firstToken.startPos);
            augCodeDescriptor.setEndPos(lastToken.endPos);
            String indent = augCodeSection.stream().map(x -> x.indent)
                .min((x, y) -> new Integer(x.length()).compareTo(y.length())).get();
            augCodeDescriptor.setIndent(indent);

            // b. create gen code descriptor.
            GeneratedCodeDescriptor genCodeDescriptor = null;
            Object genCodeDescriptorOrError = createGeneratedCodeDescriptor(tokens, srcFile,
                lastToken.index);
            if (genCodeDescriptorOrError != null) {
                if (genCodeDescriptorOrError instanceof GeneratedCodeDescriptor) {
                    genCodeDescriptor = (GeneratedCodeDescriptor) genCodeDescriptorOrError;
                }
                else {
                    saveOrThrowError((ParserException) genCodeDescriptorOrError, errors);
                }
            }

            CodeSnippetDescriptor bodySnippet = new CodeSnippetDescriptor();
            bodySnippet.setAugmentingCodeDescriptor(augCodeDescriptor);
            bodySnippet.setGeneratedCodeDescriptor(genCodeDescriptor);
            bodySnippets.add(bodySnippet);

            // c. create aug code.
            List<Integer> blockDelimiters = new ArrayList<>();
            List<Block> blocks = createAugmentingCodeBlocks(augCodeSection, blockDelimiters);
            AugmentingCode augmentingCode = new AugmentingCode(blocks);
            augmentingCode.setIndex(i);
            augmentingCode.setIndent(augCodeDescriptor.getIndent());
            augmentingCode.setDirectiveMarker(firstToken.directiveMarker);
            
            // d. validate json directive contents.
            for (int j = 0; j < blocks.size(); j++) {
                Block b = blocks.get(j);
                if (b.isJsonify()) {
                    boolean isValidJson = TaskUtils.isValidJson(b.getContent());
                    if (!isValidJson) {
                        int blockStartTokenIndex = blockDelimiters.get(j);
                        Token blockStartToken = augCodeSection.get(blockStartTokenIndex);
                        ParserException error = createParserException(
                            "Embedded JSON section of augmenting code is not " +
                            "valid.",
                            blockStartToken, srcFile);
                        saveOrThrowError(error, errors);
                    }
                }
            }
            
            List<AugmentingCode> applicableAugCodeList = specAugCodesList.get(
                firstToken.augCodeSpecIndex);
            applicableAugCodeList.add(augmentingCode);
        }

        return bodySnippets;
    }

    static List<List<Token>> identifyAugCodeSections(List<Token> tokens,
            File srcFile, List<ParserException> errors) { 
        List<List<Token>> groups = new ArrayList<>();
        final int DISABLE_SCAN = 1;
        final int IN_GEN_CODE = 2;
        int escapeMode = 0;
        // group tokens which strictly follow each other consecutively in line numbers.
        int expectedLineNumber = 0;
        List<Token> currentGroup = new ArrayList<>();
        for (Token t : tokens) {
            // As long as we are inside a generated code section, ignore all
            // directives until we hit a directive indicating end of generated code
            // section.
            // Similarly, as long as a disable scan is in force, ignore all
            // directives until we hit an enable scan.
            // Note that the effect then is that generated code section doesn't recognize
            // enable/disable scan directives, and enable/disable scan directives don't recognize
            // generated code directives.
            if (escapeMode != 0) {
                if (escapeMode == DISABLE_SCAN &&
                        t.type == Token.DIRECTIVE_TYPE_ENABLE_SCAN) {
                    escapeMode = 0;
                }
                if (escapeMode == IN_GEN_CODE && 
                        t.type == Token.DIRECTIVE_TYPE_GEN_CODE_END) {
                    escapeMode = 0;
                    // ensure newline ending.
                    if (t.newline == null) {
                        ParserException error = createParserException(
                            "Generated code section must end with a newline", 
                            t, srcFile);
                        saveOrThrowError(error, errors);
                    }
                }
                continue;
            }
            
            if (t.type == Token.DIRECTIVE_TYPE_AUG_CODE || 
                    t.type == Token.DIRECTIVE_TYPE_EMB_STRING ||
                    t.type == Token.DIRECTIVE_TYPE_EMB_JSON) {
                // ensure newline ending.
                if (t.newline == null) {
                    ParserException error = createParserException(
                        "Augmenting code section must end with a newline", 
                        t, srcFile);
                    saveOrThrowError(error, errors);
                }
            }
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
                    if (t.type == Token.DIRECTIVE_TYPE_DISABLE_SCAN) {
                        escapeMode = DISABLE_SCAN;
                    }
                    if (t.type == Token.DIRECTIVE_TYPE_GEN_CODE_START) {
                        escapeMode = IN_GEN_CODE;
                    }
                    break;
            }
        }
        if (!currentGroup.isEmpty()) {
            // Create final group.
            groups.add(currentGroup);
        }
        return groups;
    }

    static ParserException validateAugCodeSection(List<Token> tokenGroup, File srcFile) {
        Token token = tokenGroup.get(0);
        if (token.type == Token.DIRECTIVE_TYPE_AUG_CODE) {
            int expectedAugCodeSpecIndex = token.augCodeSpecIndex;
            for (int i = 1; i < tokenGroup.size(); i++) {
                token = tokenGroup.get(i);
                switch (token.type) {
                    case Token.DIRECTIVE_TYPE_EMB_STRING:
                    case Token.DIRECTIVE_TYPE_EMB_JSON:
                        break;
                    default:
                        assert token.type == Token.DIRECTIVE_TYPE_AUG_CODE;
                        if (expectedAugCodeSpecIndex != token.augCodeSpecIndex) {
                            return createParserException("Different augmenting code directives in " +
                                "same section not allowed", token, srcFile);
                        }
                        break;
                }
            }
        }
        else {
            if (token.type == Token.DIRECTIVE_TYPE_EMB_JSON) {
                return createParserException("Embedded JSON directive cannot start an " +
                    "augmenting code section", token, srcFile);
            }
            else {
                assert token.type == Token.DIRECTIVE_TYPE_EMB_STRING;
                return createParserException("Embedded string directive cannot start an " +
                    "augmenting code section", token, srcFile);
            }
        }
        return null;
    }

    static Object createGeneratedCodeDescriptor(List<Token> sourceTokens, File srcFile,
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
            if (t.type == Token.DIRECTIVE_TYPE_GEN_CODE_START) {
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

        // search for gen code end.
        for (int i = startIndex + 1; i < sourceTokens.size(); i++) {
            Token t = sourceTokens.get(i);
            if (t.type == Token.DIRECTIVE_TYPE_GEN_CODE_END) {
                GeneratedCodeDescriptor generatedCodeDescriptor = new GeneratedCodeDescriptor();
                generatedCodeDescriptor.setStartDirectiveStartPos(st.startPos);
                generatedCodeDescriptor.setStartDirectiveEndPos(st.endPos);
                generatedCodeDescriptor.setEndDirectiveStartPos(t.startPos);
                generatedCodeDescriptor.setEndDirectiveEndPos(t.endPos);
                return generatedCodeDescriptor;
            }

            // skip any other token, except for gen code start, which
            // is interpreted as start of another section, and hence
            // current search must end.
            if (t.type == Token.DIRECTIVE_TYPE_GEN_CODE_START) {
                break;
            }
            else {
                continue;
            }
        }

        // getting here means we couldn't find end directive corresponding to
        // generated code start directive.
        // signal error.
        ParserException error = createParserException(
            "Could not find ending of generated code section",
            st, srcFile);
        return error;
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

    private static ParserException createParserException(String errorMessage, Token token,
            File srcFile) {
        String fullMessage = String.format("at line %s: %s\n\n%s",
            token.lineNumber, errorMessage, token.text);
        if (srcFile != null) {
            fullMessage = "in " + srcFile + " " + fullMessage;
        }
        return new ParserException(token.lineNumber, fullMessage);
    }

    private static void saveOrThrowError(ParserException error, List<ParserException> errors) {
        if (errors == null) {
            throw error;
        }
        errors.add(error);
    }
}
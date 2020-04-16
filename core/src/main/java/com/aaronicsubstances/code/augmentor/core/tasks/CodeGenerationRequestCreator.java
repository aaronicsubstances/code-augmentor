package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;
import com.aaronicsubstances.code.augmentor.core.util.Token;

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
            GenericTaskException error = validateAugCodeSection(augCodeSection,
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
            augCodeDescriptor.setLineNumber(firstToken.lineNumber);
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
            List<Integer> blockDelimiters = new ArrayList<>();
            List<Block> blocks = createAugmentingCodeBlocks(augCodeSection, blockDelimiters);
            AugmentingCode augmentingCode = new AugmentingCode(blocks);
            augmentingCode.setIndex(i);
            augmentingCode.setIndent(augCodeDescriptor.getIndent());
            augmentingCode.setDirectiveMarker(firstToken.directiveMarker);
            augmentingCode.setLineNumber(augCodeDescriptor.getLineNumber());
            
            // d. validate json directive contents.
            for (int j = 0; j < blocks.size(); j++) {
                Block b = blocks.get(j);
                if (b.isJsonify()) {
                    GenericTaskException jsonValidationError = validateJson(j, b.getContent(),
                        augCodeSection, blockDelimiters, srcFile);
                    if (jsonValidationError != null) {
                        saveOrThrowError(jsonValidationError, errors);
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
            File srcFile, List<Exception> errors) { 
        List<List<Token>> groups = new ArrayList<>();
        final int DISABLE_SCAN = 1;
        final int IN_GEN_CODE = 2;
        int escapeMode = 0;
        Token genCodeStartToken = null;
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
                if (escapeMode == IN_GEN_CODE) {
                    if (t.type == Token.DIRECTIVE_TYPE_GEN_CODE_START) {
                        GenericTaskException error = createException(
                            "Could not find end of generated code section", 
                            genCodeStartToken, srcFile);
                        saveOrThrowError(error, errors);
                        genCodeStartToken = t;
                    }
                    if (t.type == Token.DIRECTIVE_TYPE_GEN_CODE_END) {
                        escapeMode = 0;
                        genCodeStartToken = null;
                        // ensure newline ending.
                        if (t.newline == null) {
                            GenericTaskException error = createException(
                                "Generated code section must end with a newline",
                                t, srcFile);
                            saveOrThrowError(error, errors);
                        }
                    }
                }
                continue;
            }
            
            if (t.type == Token.DIRECTIVE_TYPE_AUG_CODE || 
                    t.type == Token.DIRECTIVE_TYPE_EMB_STRING ||
                    t.type == Token.DIRECTIVE_TYPE_EMB_JSON) {
                // ensure newline ending.
                if (t.newline == null) {
                    GenericTaskException error = createException(
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
                    if (t.type == Token.DIRECTIVE_TYPE_GEN_CODE_END) {
                        GenericTaskException error = createException(
                            "Could not find start of generated code section",
                            t, srcFile);
                        saveOrThrowError(error, errors);
                    }
                    if (t.type == Token.DIRECTIVE_TYPE_DISABLE_SCAN) {
                        escapeMode = DISABLE_SCAN;
                    }
                    if (t.type == Token.DIRECTIVE_TYPE_GEN_CODE_START) {
                        escapeMode = IN_GEN_CODE;
                        genCodeStartToken = t;
                    }
                    break;
            }
        }
        if (!currentGroup.isEmpty()) {
            // Create final group.
            groups.add(currentGroup);
        }
        if (escapeMode == IN_GEN_CODE) {
            GenericTaskException error = createException(
                "Could not find end of generated code section", 
                genCodeStartToken, srcFile);
            saveOrThrowError(error, errors);
        }
        return groups;
    }

    static GenericTaskException validateAugCodeSection(List<Token> tokenGroup, File srcFile) {
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
                            return createException("Different augmenting code directives in " +
                                "same section not allowed", token, srcFile);
                        }
                        break;
                }
            }
        }
        else {
            if (token.type == Token.DIRECTIVE_TYPE_EMB_JSON) {
                return createException("Embedded JSON directive cannot start an " +
                    "augmenting code section", token, srcFile);
            }
            else {
                assert token.type == Token.DIRECTIVE_TYPE_EMB_STRING;
                return createException("Embedded string directive cannot start an " +
                    "augmenting code section", token, srcFile);
            }
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

        throw new RuntimeException("Could not find ending of a generated code section");
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

    static GenericTaskException validateJson(int blockIndex, String blockContent,
            List<Token> augCodeSection, List<Integer> blockDelimiters, File srcFile) {
        if (TaskUtils.validateJson(blockContent) == null) {
            return null;
        }
        // for error messages to show line numbers nicely,
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

        assert blockTokens.size() == blockDelimiters.size();
        Token blockStartToken = blockTokens.get(0);
        
        // construct equivalent JSON with whitespace
        StringBuilder paddedContent = new StringBuilder();
        for (int i = 0; i < blockTokens.size(); i++) {
            if (i > 0) {
                paddedContent.append("\n");
            }
            Token t = blockTokens.get(i);
            paddedContent.append(t.indent);
            // replace directive marker with whitespace
            for (int j = 0; j < t.directiveMarker.length(); j++) {
                paddedContent.append(' ');
            }
            paddedContent.append(t.directiveContent);
        }

        // total number of newlines padded should be 1 less than
        // block line number (which starts from 1).
        for (int i = 1; i < blockStartToken.lineNumber; i++) {
            paddedContent.insert(0, "\n");
        }

        String jsonValidationError = TaskUtils.validateJson(paddedContent.toString());
        assert jsonValidationError != null;

        // set defaults
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
            int emdLineNumber = Integer.parseInt(matcher.group(1));
            int errorBlockTokenIndex = emdLineNumber - lineNumber;
            Token errorBlockToken = blockTokens.get(errorBlockTokenIndex);
            assert errorBlockToken.lineNumber == emdLineNumber;
            lineNumber = errorBlockToken.lineNumber;
            snippet = errorBlockToken.text;

            // if column number available, do even better by pointing out 
            // error position.
            String colStr = matcher.group(2);
            if (!TaskUtils.isEmpty(colStr)) {
                int embCol = Integer.parseInt(colStr);
                StringBuilder pointerPadding = new StringBuilder();
                // NB: col starts from 1
                for (int i = 1; i < embCol; i++) {
                    pointerPadding.append(' ');
                }
                pointerPadding.append('^');
                // since directive token's text already ends with newline,
                // no need to add intervening line.
                snippet += pointerPadding;
            }
        }
        
        return GenericTaskException.create(null, "Embedded JSON section of augmenting code is not " +
            "valid: " + errorMessage, srcPath, lineNumber, snippet);
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
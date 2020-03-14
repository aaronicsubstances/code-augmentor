package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aaronicsubstances.programmer.companion.ParserException;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.SourceFileDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode.Block;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.programmer.companion.java.JavaLexer;

/**
 * CodeGenerationRequestCreator
 */
public class CodeGenerationRequestCreator {    
    static final int SUFFIX_TYPE_HEADER = 1;
    static final int SUFFIX_TYPE_GEN_CODE_START = 10;
    static final int SUFFIX_TYPE_GEN_CODE_END = 11;
    static final int SUFFIX_TYPE_EMB_STRING = 21;
    static final int SUFFIX_TYPE_AUG_CODE = 31;
    static final String TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR = "suffix_descriptor";
    static final String TOKEN_ATTRIBUTE_INDEX_IN_SOURCE = "index_in_source";
    static final String TOKEN_ATTRIBUTE_INDENT = "indent";
    static final String TOKEN_ATTRIBUTE_FF_NEWLINE = "ff_newline";

    public static class SuffixDescriptor implements Comparable<SuffixDescriptor> {
        public final String suffix;
        public final int suffixType;
        public final int augCodeSpecIndex;

        public SuffixDescriptor(String suffix) {
            this(suffix, -1, -1);
        }

        public SuffixDescriptor(String suffix, int suffixType, int augCodeSpecIndex) {
            this.suffix = suffix;
            this.suffixType = suffixType;
            this.augCodeSpecIndex = augCodeSpecIndex;
        }

        @Override
        public int compareTo(SuffixDescriptor o) {
            return suffix.compareTo(o.suffix);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + augCodeSpecIndex;
            result = prime * result + ((suffix == null) ? 0 : suffix.hashCode());
            result = prime * result + suffixType;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SuffixDescriptor other = (SuffixDescriptor) obj;
            if (augCodeSpecIndex != other.augCodeSpecIndex)
                return false;
            if (suffix == null) {
                if (other.suffix != null)
                    return false;
            } else if (!suffix.equals(other.suffix))
                return false;
            if (suffixType != other.suffixType)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "SuffixDescriptor{augCodeSpecIndex=" + augCodeSpecIndex + ", suffix=" + suffix + ", suffixType="
                    + suffixType + "}";
        }
    }

    private final List<SuffixDescriptor> suffixDescriptors;
    private final Pattern DOUBLE_SLASH_PATTERN;
    private final Pattern SLASH_STAR_PATTERN;

    private int runningIndex;

    public CodeGenerationRequestCreator(
            List<String> headerDoubleSlashSuffixes,
            List<String> genCodeStartSuffixes,
            List<String> genCodeEndSuffixes,
            List<String> embeddedStringDoubleSlashSuffixes,
            List<CodeGenerationRequestSpecification> requestSpecList) {
        suffixDescriptors = new ArrayList<SuffixDescriptor>();
        List<String> doubleSlashSuffixes = new ArrayList<>();
        List<String> slashStarSuffixes = new ArrayList<>();

        // add suffixes for file header (ie import section)
        for (String s : headerDoubleSlashSuffixes) {
            suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_HEADER, -1));
            doubleSlashSuffixes.add(s);
        }

        // add suffixes for beginning of generated code.
        for (String s : genCodeStartSuffixes) {
            suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_GEN_CODE_START, -1));            
            doubleSlashSuffixes.add(s);
            slashStarSuffixes.add(s);
        }

        // add suffixes for end of generated code.
        for (String s : genCodeEndSuffixes) {
            suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_GEN_CODE_END, -1));
            doubleSlashSuffixes.add(s);
            slashStarSuffixes.add(s);
        }

        // add suffixes for embedded code
        for (String s : embeddedStringDoubleSlashSuffixes) {
            suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_EMB_STRING, -1));
            doubleSlashSuffixes.add(s);
        }

        // add suffixes for augmenting code.
        for (int i = 0; i < requestSpecList.size(); i++) {
            CodeGenerationRequestSpecification spec = requestSpecList.get(i);
            for (String s : spec.getAugCodeSuffixes()) {
                suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_AUG_CODE, i));
                doubleSlashSuffixes.add(s);
                slashStarSuffixes.add(s);
            }
        }

        // sort so that binary search can be used.
        suffixDescriptors.sort(null);

        // reverse sort prior to regex creation to ensure if
        // there are clash of prefix matches, longer one always wins.
        Comparator<String> reverseSorter = (s1, s2) -> s2.compareTo(s1);
        doubleSlashSuffixes.sort(reverseSorter);
        slashStarSuffixes.sort(reverseSorter);

        StringBuilder doubleSlashRegex = new StringBuilder();
        for (String s : doubleSlashSuffixes) {
            if (doubleSlashRegex.length() > 0) {
                doubleSlashRegex.append("|");
            }
            doubleSlashRegex.append(Pattern.quote(s));
        }
        doubleSlashRegex.insert(0, "^(");
        doubleSlashRegex.append(")");
        DOUBLE_SLASH_PATTERN = Pattern.compile(doubleSlashRegex.toString());
        
        StringBuilder slashStartRegex = new StringBuilder();
        for (String s : slashStarSuffixes) {
            if (slashStartRegex.length() > 0) {
                slashStartRegex.append("|");
            }
            slashStartRegex.append(Pattern.quote(s));
        }
        slashStartRegex.insert(0, "^(");
        slashStartRegex.append(")");
        SLASH_STAR_PATTERN = Pattern.compile(slashStartRegex.toString());
    }

    SuffixDescriptor getSuffixDescriptor(Token t) {
        Matcher regexMatcher;
        if (t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT) {
            regexMatcher = DOUBLE_SLASH_PATTERN.matcher(getCommentContentWithoutSuffix(t, ""));
        }
        else if (t.type == JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT) {
            regexMatcher = SLASH_STAR_PATTERN.matcher(getCommentContentWithoutSuffix(t, ""));
        }
        else {
            return null;
        }
        if (regexMatcher.find()) {
            int idx = Collections.binarySearch(suffixDescriptors, new SuffixDescriptor(
                regexMatcher.group(1)));
            assert idx >= 0;
            SuffixDescriptor suffixDesc = suffixDescriptors.get(idx);
            return suffixDesc;
        }
        else {
            return null;
        }
    }

    public SourceFileDescriptor processSourceFile(
            ParserInputSource inputSource, List<Token> sourceTokens,
            List<List<AugmentingCode>> specAugCodesList, List<ParserException> errors) {
        // 1. first get all slash star comments relevant as aug code.
        List<Token> slashStarRelevantTokens = getSlashStarRelevantTokens(sourceTokens);

        // 2. next, get all double slash comments relevant as aug code, emb string or header.
        List<Token> doubleSlashRelevantTokens = getDoubleSlashReleventTokens(sourceTokens);
        
        // 3. group and validate double slash relevant tokens. slash star relevant tokens
        //    are valid already.
        List<List<Token>> doubleSlashRelevantTokenGroups = groupDoubleSlashReleventTokens(
            doubleSlashRelevantTokens);
        for (List<Token> relevantTokenGroup : doubleSlashRelevantTokenGroups) {
            ParserException error = validateDoubleSlashRelevantTokenGroup(relevantTokenGroup, inputSource);
            if (error != null) {
                if (errors == null) {
                    throw error;
                }
                errors.add(error);
            }
        }
        if (errors != null && errors.isEmpty()) {
            // assert at most 1 header section.
            boolean headerBlockSeen = false;
            for (List<Token> relevantTokenGroup : doubleSlashRelevantTokenGroups) {
                Token t = relevantTokenGroup.get(0);
                SuffixDescriptor suffixDescriptor = getTokenAttributeSuffixDescriptor(t.value);
                if (suffixDescriptor.suffixType == SUFFIX_TYPE_HEADER) {
                    if (!headerBlockSeen) {
                        headerBlockSeen = true;
                    }
                    else {
                        ParserException error = inputSource.createAbortException("Duplicate header section", t);                        
                        if (errors == null) {
                            throw error;
                        }
                        errors.add(error);
                    }
                }
            }
        }

        // 4. If there are no validation errors,
        //    combine slash star and double slash aug codes, and sort them.
        if (errors != null && !errors.isEmpty()) {
            return null;
        }
        List<Object> combined = combineAndSortRelevantTokens(slashStarRelevantTokens, 
            doubleSlashRelevantTokenGroups);

        // 5. generate aug code blocks and associated descriptors
        List<CodeSnippetDescriptor> bodySnippets = new ArrayList<>();
        CodeSnippetDescriptor headerSnippet = null;
        for (Object o : combined) {
            AugmentingCodeDescriptor augCodeDescriptor;
            GeneratedCodeDescriptor genCodeDescriptor;
            AugmentingCode augmentingCode;
            int augCodeSpecIndex;
            if (o instanceof List<?>) {
                List<Token> doubleSlashGroup = (List<Token>)o;
                Token firstToken = doubleSlashGroup.get(0);
                Token lastToken = doubleSlashGroup.get(doubleSlashGroup.size() - 1);
                
                // a. create aug code descriptor.
                augCodeDescriptor = new AugmentingCodeDescriptor();
                augCodeDescriptor.setStartPos(firstToken.startPos);
                augCodeDescriptor.setEndPos(lastToken.endPos);

                // b. create gen code descriptor.
                Map<String, Object> tokenAttributes = (Map<String, Object>)lastToken.value;
                int tokenIndex = (int)tokenAttributes.get(TOKEN_ATTRIBUTE_INDEX_IN_SOURCE);
                genCodeDescriptor = createGeneratedCodeDescriptor(sourceTokens, tokenIndex + 1,
                    false);

                // c. create aug code and set indent.
                tokenAttributes = (Map<String, Object>)firstToken.value;
                SuffixDescriptor suffixDescriptor = (SuffixDescriptor)tokenAttributes.get(TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR);
                StringBuilder indentBuilder = new StringBuilder();
                augmentingCode = createDoubleSlashAugCode(doubleSlashGroup, indentBuilder);
                augCodeDescriptor.setIndent(indentBuilder.toString());

                if (suffixDescriptor.suffixType == SUFFIX_TYPE_HEADER) {
                    assert headerSnippet == null;

                    headerSnippet = new CodeSnippetDescriptor();
                    headerSnippet.setAugmentingCodeDescriptor(augCodeDescriptor);
                    headerSnippet.setGeneratedCodeDescriptor(genCodeDescriptor);
                    
                    continue;
                }
                else {
                    assert suffixDescriptor.suffixType == SUFFIX_TYPE_AUG_CODE;                    
                    augCodeSpecIndex = suffixDescriptor.augCodeSpecIndex;
                }
            }
            else {
                Token starSlashSingle = (Token)o;

                // a. create aug code descriptor.
                augCodeDescriptor = new AugmentingCodeDescriptor();
                augCodeDescriptor.setAnnotatedWithSlashStar(true);
                augCodeDescriptor.setStartPos(starSlashSingle.startPos);
                augCodeDescriptor.setEndPos(starSlashSingle.endPos);

                // b. create gen code descriptor.
                Map<String, Object> tokenAttributes = (Map<String, Object>)starSlashSingle.value;
                int tokenIndex = (int)tokenAttributes.get(TOKEN_ATTRIBUTE_INDEX_IN_SOURCE);
                genCodeDescriptor = createGeneratedCodeDescriptor(sourceTokens, tokenIndex + 1,
                    true);

                // c. create aug code.
                SuffixDescriptor suffixDescriptor = (SuffixDescriptor)tokenAttributes.get(TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR);
                Block blockSingle = new Block();
                String content = getCommentContentWithoutSuffix(starSlashSingle, 
                    suffixDescriptor.suffix);
                blockSingle.setContent(content);
                augmentingCode = new AugmentingCode(Arrays.asList(blockSingle));
                augmentingCode.setCommentSuffix(suffixDescriptor.suffix);
                augCodeSpecIndex = suffixDescriptor.augCodeSpecIndex;
            }

            augCodeDescriptor.setIndex(runningIndex);
            CodeSnippetDescriptor bodySnippet = new CodeSnippetDescriptor();
            bodySnippet.setAugmentingCodeDescriptor(augCodeDescriptor);
            bodySnippet.setGeneratedCodeDescriptor(genCodeDescriptor);
            bodySnippets.add(bodySnippet);
            
            augmentingCode.setIndex(runningIndex);
            List<AugmentingCode> applicableAugCodeList = specAugCodesList.get(augCodeSpecIndex);
            applicableAugCodeList.add(augmentingCode);

            runningIndex++;
        }

        // 5. finally get all imports and normalize them.
        List<String> normalizedImports;
        if (headerSnippet != null) {
            normalizedImports = getNormalizedImportStatements(sourceTokens);
        }
        else {
            normalizedImports = Arrays.asList();
        }
        SourceFileDescriptor sourceDescriptor = new SourceFileDescriptor(
            normalizedImports, bodySnippets);
        sourceDescriptor.setHeaderSnippet(headerSnippet);
        return sourceDescriptor;
    }

    List<Token> getSlashStarRelevantTokens(List<Token> sourceTokens) {
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < sourceTokens.size(); i++) {
            Token t = sourceTokens.get(i);
            if (t.type == JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT) {
                SuffixDescriptor suffixDescriptor = getSuffixDescriptor(t);
                if (suffixDescriptor != null && suffixDescriptor.suffixType == SUFFIX_TYPE_AUG_CODE) {
                    Map<String, Object> tokenAttributes = new HashMap<>();
                    tokenAttributes.put(TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR, suffixDescriptor);
                    tokenAttributes.put(TOKEN_ATTRIBUTE_INDEX_IN_SOURCE, i);
                    t.value = tokenAttributes;
                    tokens.add(t);
                }
            }
        }
        return tokens;
    }

    List<Token> getDoubleSlashReleventTokens(List<Token> sourceTokens) {
        List<Token> tokens = new ArrayList<>();
        boolean skipDslashSearch = false;
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < sourceTokens.size(); i++) {            
            Token t = sourceTokens.get(i);
            if (t.type == JavaLexer.TOKEN_TYPE_NEWLINE) {
                skipDslashSearch = false;
                indentBuilder.setLength(0);
                continue;
            }
            else if (skipDslashSearch) {
                continue;
            }

            if (t.type == JavaLexer.TOKEN_TYPE_NON_NEWLINE_WHITESPACE) {
                indentBuilder.append(t.text);
            }
            else if (t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT) {
                SuffixDescriptor suffixDescriptor = getSuffixDescriptor(t);
                if (suffixDescriptor != null && 
                        suffixDescriptor.suffixType != SUFFIX_TYPE_GEN_CODE_START &&
                        suffixDescriptor.suffixType != SUFFIX_TYPE_GEN_CODE_END) {
                    Map<String, Object> tokenAttributes = new HashMap<>();
                    tokenAttributes.put(TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR, suffixDescriptor);
                    tokenAttributes.put(TOKEN_ATTRIBUTE_INDEX_IN_SOURCE, i);
                    tokenAttributes.put(TOKEN_ATTRIBUTE_INDENT, indentBuilder.toString());
                    if (i + 1 < sourceTokens.size()) {
                        Token ffNewline = sourceTokens.get(i + 1);
                        assert ffNewline.type == JavaLexer.TOKEN_TYPE_NEWLINE;
                        tokenAttributes.put(TOKEN_ATTRIBUTE_FF_NEWLINE, ffNewline);
                    }
                    t.value = tokenAttributes;
                    tokens.add(t);
                }
                else {
                    skipDslashSearch = true;
                }
            }
            else {
                skipDslashSearch = true;
            }
        } 
        return tokens;
    }

    GeneratedCodeDescriptor createGeneratedCodeDescriptor(List<Token> sourceTokens, 
            int startIndex, boolean isSlashStar) {
        // search for gen code start.
        int found = -1;
        int i = startIndex;
        for (; i < sourceTokens.size(); i++) {
            Token t = sourceTokens.get(i);
            // only tolerate whitespace as the only different token to expect,
            // not even gen code end.
            if (t.type == JavaLexer.TOKEN_TYPE_NON_NEWLINE_WHITESPACE ||
                    t.type == JavaLexer.TOKEN_TYPE_NEWLINE) {
                continue;
            }
            // don't bother checking if relevant tokens other than gen code start or 
            // end are encountered.
            if (t.value == null) {
                SuffixDescriptor suffixDescriptor = getSuffixDescriptor(t);
                if (suffixDescriptor != null && 
                        suffixDescriptor.suffixType == SUFFIX_TYPE_GEN_CODE_START) {
                    found = t.type;
                }
            }
            break;
        }

        if (found != -1) {
            // search for gen code end.
            i++;
            for (; i < sourceTokens.size(); i++) {
                Token t = sourceTokens.get(i);
                if (t.value != null) {
                    // relevant tokens other than gen code end 
                    // are encountered. conclude not found.
                    break;
                }
                SuffixDescriptor suffixDescriptor = getSuffixDescriptor(t);
                if (suffixDescriptor == null) {
                    continue;
                }
                if (suffixDescriptor.suffixType == SUFFIX_TYPE_GEN_CODE_END && t.type == found) {
                    GeneratedCodeDescriptor generatedCodeDescriptor = new GeneratedCodeDescriptor();
                    generatedCodeDescriptor.setStartPos(sourceTokens.get(startIndex).startPos);
                    generatedCodeDescriptor.setEndPos(t.endPos);
                    // consume following new line if double slash comment.
                    if (t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT && 
                            i + 1 < sourceTokens.size()) {
                        Token nextToken = sourceTokens.get(i + 1);
                        assert nextToken.type == JavaLexer.TOKEN_TYPE_NEWLINE;
                        generatedCodeDescriptor.setEndPos(nextToken.endPos);
                    }
                    return generatedCodeDescriptor;
                }
                else {
                    // either gen code start encountered, or gen code end encountered but of a 
                    // different commment style: conclude not found.
                    break;
                }
            }
        }

        // Ensure terminating newline is removed for double slash comments
        // by treating them like generated code.
        // This enables us to always make sure to insert a new line before
        // inserting generated code.
        if (!isSlashStar && startIndex < sourceTokens.size()) {
            Token t = sourceTokens.get(startIndex);
            GeneratedCodeDescriptor generatedCodeDescriptor = new GeneratedCodeDescriptor();
            generatedCodeDescriptor.setStartPos(t.startPos);
            generatedCodeDescriptor.setEndPos(t.endPos);
            return generatedCodeDescriptor;
        }

        return null;
    }

    static ParserException validateDoubleSlashRelevantTokenGroup(
            List<Token> tokenGroup, 
            ParserInputSource inputSource) {
        Token token = tokenGroup.get(0);
        SuffixDescriptor suffixDescriptor = getTokenAttributeSuffixDescriptor(token.value);
        if (suffixDescriptor.suffixType == SUFFIX_TYPE_HEADER) {
            for (int i = 1; i < tokenGroup.size(); i++) {
                token = tokenGroup.get(i);
                suffixDescriptor = getTokenAttributeSuffixDescriptor(token.value);
                switch (suffixDescriptor.suffixType) {
                    case SUFFIX_TYPE_HEADER:
                        break;
                    default:
                        return inputSource.createAbortException("Expected header comment marker suffix", token);
                }
            }
        }
        else if (suffixDescriptor.suffixType == SUFFIX_TYPE_AUG_CODE) {
            int expectedAugCodeSpecIndex = suffixDescriptor.augCodeSpecIndex;
            for (int i = 1; i < tokenGroup.size(); i++) {
                token = tokenGroup.get(i);
                suffixDescriptor = getTokenAttributeSuffixDescriptor(token.value);
                switch (suffixDescriptor.suffixType) {
                    case SUFFIX_TYPE_HEADER:
                        return inputSource.createAbortException("Unexpected header comment marker suffix", token);
                    case SUFFIX_TYPE_EMB_STRING:
                        break;
                    default:
                        assert suffixDescriptor.suffixType == SUFFIX_TYPE_AUG_CODE;
                        if (expectedAugCodeSpecIndex != suffixDescriptor.augCodeSpecIndex) {
                            return inputSource.createAbortException("Different augmenting comment marker suffixes in same section not allowed", token);
                        }
                }
            }
        }
        else {
            assert suffixDescriptor.suffixType == SUFFIX_TYPE_EMB_STRING;
            return inputSource.createAbortException("Embedded string comment marker suffix cannot start an augmenting code section", token);
        }
        return null;
    }

    static List<Object> combineAndSortRelevantTokens(List<Token> slashStarRelevantTokens,
            List<List<Token>> doubleSlashRelevantTokenGroups) {
        List<Object> combined = new ArrayList<>();
        combined.addAll(slashStarRelevantTokens);
        combined.addAll(doubleSlashRelevantTokenGroups);
        // sort by startPos rather than lineNumber,
        // since multiple slash star tokens can occupy same line.
        Collections.sort(combined, new Comparator<Object>() {

            @Override
            public int compare(Object o1, Object o2) {
                Token t1, t2;
                if (o1 instanceof List<?>) {
                    t1 = ((List<Token>)o1).get(0);
                }
                else {
                    t1 = (Token)o1;
                }
                if (o2 instanceof List<?>) {
                    t2 = ((List<Token>)o2).get(0);
                }
                else {
                    t2 = (Token)o2;
                }
                if (t1.startPos < t2.startPos) {
                    return -1;
                }
                if (t1.startPos > t2.startPos) {
                    return 1;
                }
                return 0;
            }
        });
        return combined;
    }

    static List<List<Token>> groupDoubleSlashReleventTokens(List<Token> tokens) {
        List<List<Token>> groups = new ArrayList<>();
        // group tokens which strictly follow each other consecutively in line numbers.
        int expectedLineNumber = tokens.isEmpty() ? 0 : tokens.get(0).lineNumber;
        List<Token> currentGroup = new ArrayList<>();
        for (Token t : tokens) {
            if (t.lineNumber != expectedLineNumber) {
                assert !currentGroup.isEmpty();
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
            currentGroup.add(t);
            expectedLineNumber = t.lineNumber + 1;
        }
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        return groups;
    }

    static AugmentingCode createDoubleSlashAugCode(
            List<Token> doubleSlashGroup, StringBuilder indentReceiver) {
        AugmentingCode augCode = new AugmentingCode();

        /*
         * Set minimum indent and consolidate with new lines.
         * Cater for both header and real aug code.
         */

        String[] contentLines = new String[doubleSlashGroup.size()];
        boolean[] stringifyStatuses = new boolean[doubleSlashGroup.size()];
        String[] terminatingNewLines = new String[doubleSlashGroup.size()];

        for (int i = 0; i < doubleSlashGroup.size(); i++) {
            Token t = doubleSlashGroup.get(i);
            Map<String, Object> tokenAttributes = (Map<String, Object>)t.value;

            // set minimum indent.
            String indent = (String)tokenAttributes.get(TOKEN_ATTRIBUTE_INDENT);
            if (i == 0 || indent.length() < indentReceiver.length()) {
                indentReceiver.setLength(0);
                indentReceiver.append(indent);
            }

            // set values intended ultimately for block properties
            SuffixDescriptor suffixDescriptor = (SuffixDescriptor)tokenAttributes.get(
                TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR);            
            contentLines[i] = getCommentContentWithoutSuffix(t, suffixDescriptor.suffix);
            if (suffixDescriptor.suffixType == SUFFIX_TYPE_EMB_STRING) {
                stringifyStatuses[i] = true;
            }
            else {
                if (augCode.getCommentSuffix() == null) { 
                    augCode.setCommentSuffix(suffixDescriptor.suffix);
                }
            }
            
            // Fetch new lines terminating comments for use in next step.
            Token ffNewline = (Token)tokenAttributes.get(TOKEN_ATTRIBUTE_FF_NEWLINE);
            if (ffNewline != null) {
                terminatingNewLines[i] = ffNewline.text;
            }
        }

        // 2. consolidate
        List<Block> blocks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        Block lastBlock = new Block(); // first block definitely not stringified.
        blocks.add(lastBlock);
        for (int i = 0; i < contentLines.length; i++) {
            String contentLine = contentLines[i];
            boolean stringify = stringifyStatuses[i];
            
            if (lastBlock.isStringify() == stringify) {
                if (i > 0) {
                    sb.append(terminatingNewLines[i - 1]);
                }
                sb.append(contentLine);
            }
            else {
                // Add new lines to content lines, with requirement that
                // 1. first section should not have preceding new line.
                // 2. last section should not have terminating new line.
                // 3. sections to be stringified should neither have preceding or trailing new lines.
                if (stringify) {
                    // let outgoing raw aug code section have
                    // trailing newline.
                    sb.append(terminatingNewLines[i]);
                }
                lastBlock.setContent(sb.toString());

                // reset for use with next block.
                lastBlock = new Block();
                lastBlock.setStringify(stringify);
                blocks.add(lastBlock);
                sb.setLength(0);
                if (!stringify) {
                    // let incoming raw aug code section have preceding
                    // newline.
                    sb.append(terminatingNewLines[i]);
                }
                sb.append(contentLine);
            }
        }

        // complete last block.
        lastBlock.setContent(sb.toString());

        augCode.setBlocks(blocks);
        return augCode;
    }

    static List<String> getNormalizedImportStatements(List<Token> sourceTokens) {
        List<String> normalizedImports = new ArrayList<>();
        int i = 0;
        while (i < sourceTokens.size()) {
            Token t = sourceTokens.get(i);
            if (t.type == JavaLexer.TOKEN_TYPE_IMPORT_KEYWORD) {
                StringBuilder importStatement = new StringBuilder(t.text);
                i++;
                while (i < sourceTokens.size()) {
                    t = sourceTokens.get(i);
                    if (t.type == JavaLexer.TOKEN_TYPE_SEMI_COLON || 
                            t.type == JavaLexer.TOKEN_TYPE_NEWLINE) {
                        break;
                    }
                    // skip comments.
                    if (t.type != JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT &&
                            t.type != JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT) {
                        importStatement.append(t.text);
                    }
                    i++;
                }

                // normalize import by using a common whitespace separator.
                String normaizedImport = importStatement.toString().trim().replaceAll("\\s+", " ");
                normalizedImports.add(normaizedImport);
            }
            else {
                i++;
            }
        }
        return normalizedImports;
    }

    static SuffixDescriptor getTokenAttributeSuffixDescriptor(Object tokenValue) {
        Map<String, Object> tokenAttributes = (Map<String, Object>)tokenValue;
        SuffixDescriptor suffixDescriptor = (SuffixDescriptor)tokenAttributes.get(TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR);
        return suffixDescriptor;
    }

    static String getCommentContentWithoutSuffix(Token t, String suffix) {
        if (t.type == JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT) {
            return t.text.substring(("/*" + suffix).length(), t.text.length() - "*/".length());
        }
        else {
            assert t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT;
            return t.text.substring(("//" + suffix).length());
        }
    }
}
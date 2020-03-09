package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.SourceFileDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.AugmentingCode.Block;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.programmer.companion.java.JavaLexer;

/**
 * CodeGenerationRequestCreator
 */
public class CodeGenerationRequestCreator {    
    private static final int SUFFIX_TYPE_HEADER = 1;
    private static final int SUFFIX_TYPE_GEN_CODE_START = 10;
    private static final int SUFFIX_TYPE_GEN_CODE_END = 11;
    private static final int SUFFIX_TYPE_EMB_CODE = 21;
    private static final int SUFFIX_TYPE_AUG_CODE = 31;
    private static final String TOKEN_ATTRIBUTE_SUFFIX_DESCRIPTOR = "suffix_descriptor";
    private static final String TOKEN_ATTRIBUTE_INDEX_IN_SOURCE = "index_in_source";
    private static final String TOKEN_ATTRIBUTE_INDENT = "indent";

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
    }

    private final List<SuffixDescriptor> suffixDescriptors;
    private final Pattern DOUBLE_SLASH_PATTERN;
    private final Pattern SLASH_STAR_PATTERN;

    private int runningIndex, runningIndexInFile;

    public CodeGenerationRequestCreator(
            List<String> headerDoubleSlashSuffixes,
            List<String> genCodeStartSuffixes,
            List<String> genCodeEndSuffixes,
            List<String> embeddedCodeDoubleSlashSuffixes,
            List<CodeGenerationRequestSpecification> requestSpecList) {
        suffixDescriptors = new ArrayList<SuffixDescriptor>();
        StringBuilder doubleSlashRegex = new StringBuilder();
        StringBuilder slashStartRegex = new StringBuilder();

        // add suffixes for file header (ie import section)
        for (String s : headerDoubleSlashSuffixes) {
            suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_HEADER, -1));
            if (doubleSlashRegex.length() > 0) {
                doubleSlashRegex.append("|");
            }
            doubleSlashRegex.append(Pattern.quote(s));
        }

        // add suffixes for beginning of generated code.
        for (String s : genCodeStartSuffixes) {
            suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_GEN_CODE_START, -1));
            if (doubleSlashRegex.length() > 0) {
                doubleSlashRegex.append("|");
            }
            if (slashStartRegex.length() > 0) {
                slashStartRegex.append("|");
            }
            s = Pattern.quote(s);
            doubleSlashRegex.append(s);
            slashStartRegex.append(s);
        }

        // add suffixes for end of generated code.
        // by now regex string builders will not be empty,
        // so always prepend with '|'
        for (String s : genCodeEndSuffixes) {
            suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_GEN_CODE_END, -1));
            s = Pattern.quote(s);
            doubleSlashRegex.append("|").append(s);
            slashStartRegex.append("|").append(s);
        }

        // add suffixes for embedded code
        for (String s : embeddedCodeDoubleSlashSuffixes) {
            suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_EMB_CODE, -1));
            doubleSlashRegex.append("|").append(Pattern.quote(s));
        }

        // add suffixes for augmenting code.
        for (int i = 0; i < requestSpecList.size(); i++) {
            CodeGenerationRequestSpecification spec = requestSpecList.get(i);
            for (String s : spec.getAugCodeSuffixes()) {
                suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_AUG_CODE, i));
                s = Pattern.quote(s);
                doubleSlashRegex.append("|").append(s);
                slashStartRegex.append("|").append(s);
            }
        }

        suffixDescriptors.sort(null);

        doubleSlashRegex.insert(0, "^(");
        doubleSlashRegex.append(")");
        DOUBLE_SLASH_PATTERN = Pattern.compile(doubleSlashRegex.toString());
        
        slashStartRegex.insert(0, "^(");
        slashStartRegex.append(")");
        SLASH_STAR_PATTERN = Pattern.compile(slashStartRegex.toString());
    }

    public SourceFileDescriptor processSourceFile(List<Token> source,
            List<List<AugmentingCode>> specAugCodesList, List<String> errors) {
        runningIndexInFile = 0;
        SourceFileDescriptor sourceDescriptor = new SourceFileDescriptor(new ArrayList<>(),
            new ArrayList<>());

        // 1. first get all slash star comments relevant as aug code.
        List<Token> slashStarRelevantTokens = getSlashStarRelevantTokens(source);

        // 2. next, get all double slash comments relevant as aug code, emb code or header.
        List<Token> doubleSlashRelevantTokens = getDoubleSlashReleventTokens(source);
        
        // 3. group and validate double slash relevant tokens. slash star relevant tokens
        //    are valid already.
        List<List<Token>> doubleSlashRelevantTokenGroups = groupDoubleSlashReleventTokens(
            doubleSlashRelevantTokens);
        for (List<Token> relevantTokenGroup : doubleSlashRelevantTokenGroups) {
            String error = validateDoubleSlashRelevantTokenGroup(relevantTokenGroup);
            if (error != null) {
                errors.add(error);
            }
        }

        // 4. If there are no validation errors,
        //    combine slash star and double slash aug codes, and sort them.
        if (!errors.isEmpty()) {
            return null;
        }

        // 5. locate gen code section for each aug code.


        // 6. finally get all imports and normalize them.
        List<String> normalizedImports = getNormalizedImportStatements(source);
        sourceDescriptor.setImportStatements(normalizedImports);
        return sourceDescriptor;
    }

    private String validateDoubleSlashRelevantTokenGroup(List<Token> relevantTokenGroup) {
        return null;
    }

    private List<String> getNormalizedImportStatements(List<Token> source) {
        List<String> normalizedImports = new ArrayList<>();
        int i = 0;
        while (i < source.size()) {
            Token t = source.get(i);
            if (t.type == JavaLexer.TOKEN_TYPE_IMPORT_KEYWORD) {
                StringBuilder importStatement = new StringBuilder(t.text);
                i++;
                while (i < source.size()) {
                    t = source.get(i);
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

    private List<Token> getSlashStarRelevantTokens(List<Token> source) {
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            Token t = source.get(i);
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

    private List<Token> getDoubleSlashReleventTokens(List<Token> source) {
        List<Token> tokens = new ArrayList<>();
        boolean skipDslashSearch = false;
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < source.size(); i++) {            
            Token t = source.get(i);
            if (t.type == JavaLexer.TOKEN_TYPE_NEWLINE) {
                skipDslashSearch = false;
                indentBuilder.setLength(0);
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

    private List<List<Token>> groupDoubleSlashReleventTokens(List<Token> tokens) {
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

    private SuffixDescriptor getSuffixDescriptor(Token t) {
        Matcher regexMatcher;
        if (t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT) {
            regexMatcher = DOUBLE_SLASH_PATTERN.matcher(t.text.substring("//".length()));
        }
        else if (t.type == JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT) {
            regexMatcher = SLASH_STAR_PATTERN.matcher(
                t.text.substring("/*".length(), t.text.length() - "*/".length()));
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

    private static String getCommentContentWithoutSuffix(Token t, String suffix) {
        if (t.type == JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT) {
            return t.text.substring(("/*" + suffix).length(), "*/".length());
        }
        else {
            assert t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT;
            return t.text.substring(("//" + suffix).length());
        }
    }
}
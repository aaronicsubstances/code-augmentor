package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private static final int SUFFIX_TYPE_AUG_CODE_START = 21;
    private static final int SUFFIX_TYPE_AUG_CODE_CONTINUATION = 22;
    private static final int SUFFIX_TYPE_AUG_CODE_END = 23;

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

    public CodeGenerationRequestCreator(
            List<String> headerDoubleSlashSuffixes,
            List<String> genCodeStartSuffixes,
            List<String> genCodeEndSuffixes,    
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
            doubleSlashRegex.append(s);
            slashStartRegex.append(s);
        }

        // add suffixes for augmenting code.
        for (int i = 0; i < requestSpecList.size(); i++) {
            CodeGenerationRequestSpecification spec = requestSpecList.get(i);
            for (String s : spec.getStartSuffixes()) {
                suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_AUG_CODE_START, i));
                s = Pattern.quote(s);
                doubleSlashRegex.append("|").append(s);
                slashStartRegex.append("|").append(s);
            }
            for (String s : spec.getContinuationDoubleSlashSuffixes()) {
                suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_AUG_CODE_CONTINUATION, i));
                doubleSlashRegex.append("|").append(Pattern.quote(s));
            }
            for (String s : spec.getClosingDoubleSlashSuffixes()) {
                suffixDescriptors.add(new SuffixDescriptor(s, SUFFIX_TYPE_AUG_CODE_END, i));
                doubleSlashRegex.append("|").append(Pattern.quote(s));
            }
        }

        suffixDescriptors.sort(null);

        DOUBLE_SLASH_PATTERN = Pattern.compile(doubleSlashRegex.toString());
        
        SLASH_STAR_PATTERN = Pattern.compile(slashStartRegex.toString());
    }

    public SourceFileDescriptor processSourceFile(List<Token> source,
            List<List<AugmentingCode>> specAugCodesList) {
        SourceFileDescriptor sourceDescriptor = new SourceFileDescriptor(new ArrayList<>(),
            new ArrayList<>());
        for (int i = 0; i < source.size(); i++) {
            Token t = source.get(i);
            
            // 1. First find a header suffix or aug code suffix
            //    (ignore gen code suffixes)
            boolean headerFound = false;
            boolean augCodeBlockFound = false;
            String augCodeBlock = null;
            boolean annotatedWithSlashStar = false;
            Matcher regexMatcher;
            SuffixDescriptor suffixDesc = null;
            if (t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT) {
                augCodeBlock = t.text.substring("//".length());
                regexMatcher = DOUBLE_SLASH_PATTERN.matcher(augCodeBlock);
                if (regexMatcher.find()) {
                    int idx = Collections.binarySearch(suffixDescriptors, new SuffixDescriptor(
                        regexMatcher.group()));
                    suffixDesc = suffixDescriptors.get(idx);
                    switch (suffixDesc.suffixType) {
                        case SUFFIX_TYPE_AUG_CODE_START:
                            augCodeBlockFound = true;
                            annotatedWithSlashStar = false;
                            break;
                        case SUFFIX_TYPE_HEADER:
                            headerFound = true;
                            break;
                        case SUFFIX_TYPE_AUG_CODE_CONTINUATION:
                        case SUFFIX_TYPE_AUG_CODE_END:
                            // error.
                            break;
                    }
                }
            }
            else if (t.type == JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT) {
                augCodeBlock = t.text.substring("/*".length(), t.text.length() -
                    "*/".length());
                regexMatcher = SLASH_STAR_PATTERN.matcher(augCodeBlock);
                if (regexMatcher.find()) {
                    int idx = Collections.binarySearch(suffixDescriptors, new SuffixDescriptor(
                        regexMatcher.group()));
                    suffixDesc = suffixDescriptors.get(idx);
                    if (suffixDesc.suffixType == SUFFIX_TYPE_AUG_CODE_START) {
                        augCodeBlockFound = true;
                        annotatedWithSlashStar = true;
                    }
                }
            }
            if (headerFound) {
                
            }
            if (augCodeBlockFound) {
                CodeSnippetDescriptor augCodeDescriptor = new CodeSnippetDescriptor();
                augCodeDescriptor.setAugmentingCodeDescriptor(new AugmentingCodeDescriptor());
                if (annotatedWithSlashStar) {

                }
                else {
                    createDoubleSlashAugmentingCode(source, i, suffixDesc);
                }
            }
        }
        return sourceDescriptor;
    }

    private void createDoubleSlashAugmentingCode(List<Token> source, int start,
            SuffixDescriptor firstSuffixDescriptor) {
        AugmentingCode augCode = new AugmentingCode(new ArrayList<>());
        List<String> newLineList = new ArrayList<>();
        StringBuilder tempBuilder = new StringBuilder();

        // Find first indent.
        int i;
        for (i = start - 1; i >= 0; i--) {
            Token t = source.get(i);
            if (t.type == JavaLexer.TOKEN_TYPE_NON_NEWLINE_WHITESPACE) {
                tempBuilder.append(t.text);
            }
            else {
                if (t.type != JavaLexer.TOKEN_TYPE_NEWLINE) {
                    tempBuilder.setLength(0);
                }
                break;
            }
        }

        String minIndent = tempBuilder.toString();

        Block block = new Block();
        block.setContent(trimStartSuffix(source.get(start), firstSuffixDescriptor.suffix, false));
        augCode.getBlocks().add(block);

        boolean specialCommentSeen = false;

        // The ff two variables are set depending on kind of 
        // first suffix.
        boolean exitAfterNewLine;
        boolean includeNonCommentBlock;
        switch (firstSuffixDescriptor.suffixType) {
            case SUFFIX_TYPE_AUG_CODE_CONTINUATION:
                exitAfterNewLine = false;
                includeNonCommentBlock = true;
                break;
            case SUFFIX_TYPE_AUG_CODE_END:
                exitAfterNewLine = true;
                includeNonCommentBlock = false;
                break;
            case SUFFIX_TYPE_AUG_CODE_START:
                exitAfterNewLine = false;
                includeNonCommentBlock = false;
                break;
            default:
                throw new RuntimeException("Unexpected suffix type: " + 
                    firstSuffixDescriptor.suffixType);
        }
        
        // immediately assert new line for first block.
        boolean assertNewLine = true;

        for (i = start + 1; i < source.size(); i++) {
            Token t = source.get(i);
            if (assertNewLine) {
                if (t.type != JavaLexer.TOKEN_TYPE_NEWLINE) {
                    // error.
                }
            }

            if (t.type == JavaLexer.TOKEN_TYPE_NEWLINE) {
                newLineList.add(t.text);
                if (specialCommentSeen) {
                    minIndent = updateMinIndent(minIndent, tempBuilder);
                }
                if (exitAfterNewLine) {
                    break;
                }
                // reset temp builder and other loop variables.
                tempBuilder.setLength(0);
                assertNewLine = false;
                specialCommentSeen = false;

                continue;
            }

            else if (t.type == JavaLexer.TOKEN_TYPE_NON_NEWLINE_WHITESPACE) {
                tempBuilder.append(t.text);

                continue;
            }

            else if (t.type == JavaLexer.TOKEN_TYPE_MULTI_LINE_COMMENT) {
                // Check for any slash star comment of interest: either aug code start,
                // gen code start, or gen code end, and exit loop immediately if one
                // is found. 
                SuffixDescriptor suffixDesc = getSuffixDescriptor(t);
                if (suffixDesc != null) {
                    break;
                }

                // fall through
            }

            else if (t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT) {
                SuffixDescriptor suffixDesc = getSuffixDescriptor(t);
                // Check for any double slash comment of interest
                // only aug code end or continuation should prevent an immediate exit.
                if (suffixDesc != null) {
                    if (suffixDesc.suffixType == SUFFIX_TYPE_AUG_CODE_END) {
                        exitAfterNewLine = true;
                    }
                    else if (suffixDesc.suffixType == SUFFIX_TYPE_AUG_CODE_CONTINUATION) {
                        includeNonCommentBlock = true;
                    }
                    else {
                        break;
                    }
                    block = new Block();
                    block.setContent(trimStartSuffix(t, suffixDesc.suffix, false));
                    augCode.getBlocks().add(block);
                    assertNewLine = true;
                    specialCommentSeen = true;

                    continue;
                }

                // fall through
            }
            
            // getting here means we have raw code
            if (!includeNonCommentBlock) {
                break;
            }

            // consume tokens until new line,
            // even if tokens are of the suffix kind.
            while (i < source.size()) {
                t = source.get(i);
                if (t.type == JavaLexer.TOKEN_TYPE_NEWLINE) {
                    break;
                }
                if (t.text != null) {
                    tempBuilder.append(t.text);
                }
                i++;
            }
            i--; // restore for for-loop sake.
            assertNewLine = true;
            block = new Block();
            block.setStringify(true);
            block.setContent(tempBuilder.toString());
            augCode.getBlocks().add(block);
        }
    }

    private static String updateMinIndent(String minIndent, StringBuilder tempBuilder) {
        if (tempBuilder.length() < minIndent.length()) {
            return tempBuilder.toString();
        }
        return minIndent;
    }

    private static String trimStartSuffix(Token t, String suffix, boolean isSlashStar) {
        if (isSlashStar) {
            return t.text.substring(("/*" + suffix).length(), "*/".length());
        }
        else {
            return t.text.substring(("//" + suffix).length());
        }
    }

    private SuffixDescriptor getSuffixDescriptor(Token t) {
        Matcher regexMatcher;
        if (t.type == JavaLexer.TOKEN_TYPE_SINGLE_LINE_COMMENT) {
            regexMatcher = DOUBLE_SLASH_PATTERN.matcher(t.text.substring("//".length()));
        }
        else {
            regexMatcher = SLASH_STAR_PATTERN.matcher(
                t.text.substring("/*".length(), t.text.length() - "*/".length()));
        }
        if (regexMatcher.find()) {
            int idx = Collections.binarySearch(suffixDescriptors, new SuffixDescriptor(
                regexMatcher.group()));
            SuffixDescriptor suffixDesc = suffixDescriptors.get(idx);
            return suffixDesc;
        }
        else {
            return null;
        }
    }
}
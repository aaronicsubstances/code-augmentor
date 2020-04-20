package com.aaronicsubstances.code.augmentor.core.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentRange;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

public class CodeGenerationResponseProcessor {
    private static final Pattern NON_NEWLINE_WS_MULTIPLE_REGEX = Pattern.compile("[ \t\f]+");

    static int[] determineReplacementRange(CodeSnippetDescriptor snippetDescriptor, 
            GeneratedCode genCode) {
        int[] replacementRange;
        AugmentingCodeDescriptor augCodeDescriptor = snippetDescriptor.getAugmentingCodeDescriptor();
        GeneratedCodeDescriptor genCodeDescriptor = snippetDescriptor.getGeneratedCodeDescriptor();
        if (genCode.isReplaceAugCodeDirectives()) {
            replacementRange = new int[]{ augCodeDescriptor.getStartPos(),
                augCodeDescriptor.getEndPos() };
            if (genCode.isReplaceGenCodeDirectives() && genCodeDescriptor != null) {
                replacementRange[1] = genCodeDescriptor.getEndDirectiveEndPos();
            }
        }                   
        else if (genCode.isReplaceGenCodeDirectives()) {
            if (genCodeDescriptor != null) {
                replacementRange = new int[]{ augCodeDescriptor.getEndPos(),
                    genCodeDescriptor.getEndDirectiveEndPos() };
            }
            else {             
                // resort to empty range positioned at the end of the aug code.   
                replacementRange = new int[]{ augCodeDescriptor.getEndPos(),
                    augCodeDescriptor.getEndPos() };
            }
        }
        else {
            // default
            replacementRange = null;
        }
        return replacementRange;
    }

    static String ensureEndingNewline(String code, String newline) {
        boolean genCodeEndsWithNewline = false;
        if (!code.isEmpty()) {
            char lastChar = code.charAt(code.length() - 1);
            if (TaskUtils.isNewLine(lastChar)) {
                genCodeEndsWithNewline = true;
            }
        }
        if (!genCodeEndsWithNewline) {
            code += newline;
        }
        return code;
    }

    static String getEffectiveIndent(AugmentingCodeDescriptor augCodeDescriptor, GeneratedCode genCode) {
        String indent = genCode.getIndent();
        if (indent == null) {
            indent = augCodeDescriptor.getIndent();
        }
        return indent;
    }

    static String indentCode(String code, String indent) {
        List<String> splitCode = TaskUtils.splitIntoLines(code);
        StringBuilder codeBuffer = new StringBuilder();
        for (int i = 0; i < splitCode.size(); i+=2) {
            String line = splitCode.get(i);
            if (!TaskUtils.isBlank(line)) {
                codeBuffer.append(indent).append(line);
            }
            String terminator = splitCode.get(i + 1);
            codeBuffer.append(terminator);
        }
        return codeBuffer.toString();
    }

	public static boolean areTextsSimiliar(String textToBeReplaced, String replacmentText,
			List<ContentRange> exactMatchRanges, int exactMatchAdjustment) {
        // build regex out of replacement text and match it against all of textToBeReplaced
        StringBuilder regexBuilder = new StringBuilder();
        if (exactMatchRanges == null || exactMatchRanges.isEmpty()) {
            appendReplacementTextRegex(regexBuilder, replacmentText, true, true);
        }
        else {
            // Else exact matches exist. 
            // In that case split replacementText into parts, where each part
            // is annotated with 3 pieces of information: includedInExactMatch,
            // hasNewlineStart, hasNewlineEnd
            
            List<Boolean> includedStates = new ArrayList<>();
            List<Boolean> startsNewlineStates = new ArrayList<>();
            List<Boolean> endsWithNewlineStates = new ArrayList<>();
            List<String> sections = partitionReplacementText(replacmentText, exactMatchRanges,
                exactMatchAdjustment, includedStates, startsNewlineStates, 
                endsWithNewlineStates);
            for (int i = 0; i < sections.size(); i++) {
                String section = sections.get(i);
                if (includedStates.get(i)) {
                    regexBuilder.append(Pattern.quote(section));
                }
                else {
                    boolean startsNewline = startsNewlineStates.get(i);
                    boolean endsWithNewline = endsWithNewlineStates.get(i);
                    appendReplacementTextRegex(regexBuilder, section, startsNewline, 
                        endsWithNewline);
                }
            }
        }
        Pattern regex = Pattern.compile(regexBuilder.toString());
        boolean similar = regex.matcher(textToBeReplaced).matches();
        return similar;
	}

    private static List<String> partitionReplacementText(String replacementText,
            List<ContentRange> exactMatchRanges, int exactMatchAdjustment,
            List<Boolean> includedStates,
            List<Boolean> startWithNewlineStates,
            List<Boolean> endWithNewlineStates) {
        List<String> sections = new ArrayList<>();
        int startTextIndex = 0;
        for (int i = 0; i < exactMatchRanges.size(); i++) {
            ContentRange exactMatchRange = exactMatchRanges.get(i);
            if (exactMatchRange == null) {
                continue;
            }
            int effStartPos = exactMatchRange.getStartPos() + exactMatchAdjustment;
            int effEndPos = exactMatchRange.getEndPos() + exactMatchAdjustment;
            if (effStartPos < 0 || effStartPos >= replacementText.length()) {
                continue;
            }
            if (effEndPos < 0 || effEndPos >= replacementText.length()) {
                continue;
            }
            if (effEndPos < effStartPos) {
                continue;
            }
            if (startTextIndex < effStartPos) {
                // meaning ranges are not sorted.
                continue;
            }
            String relaxedMatchSection = replacementText.substring(startTextIndex, effEndPos);
            if (!relaxedMatchSection.isEmpty()) {
                sections.add(relaxedMatchSection);
                includedStates.add(false);
                boolean startsNewline = true;
                if (startTextIndex > 0) {
                    char prevChar = replacementText.charAt(startTextIndex - 1);
                    if (!TaskUtils.isNewLine(prevChar)) {
                        startsNewline = false;
                    }
                }
                startWithNewlineStates.add(startsNewline);

                boolean endsNewline = true;
                if (effEndPos < replacementText.length()) {
                    char lastSectionChar = replacementText.charAt(effEndPos - 1);
                    if (!TaskUtils.isNewLine(lastSectionChar)) {
                        startsNewline = false;
                    }
                }
                endWithNewlineStates.add(endsNewline);
            }
            String exactMatchSection = replacementText.substring(effStartPos, effEndPos);
            if (!exactMatchSection.isEmpty()) {
                sections.add(exactMatchSection);
                includedStates.add(true);
                // don't really care about these, except for maintaining
                // corresponding indices.
                startWithNewlineStates.add(false);
                endWithNewlineStates.add(false);
            }
            startTextIndex = effEndPos;
        }

        // deal with remainder.
        String remainder = replacementText.substring(startTextIndex);
        if (!remainder.isEmpty()) {
            sections.add(remainder);
            includedStates.add(false);
            boolean startsNewline = true;
            if (startTextIndex > 0) {
                char prevChar = replacementText.charAt(startTextIndex - 1);
                if (!TaskUtils.isNewLine(prevChar)) {
                    startsNewline = false;
                }
            }
            startWithNewlineStates.add(startsNewline);
            endWithNewlineStates.add(true);
        }
        
        return sections;
    }

    private static void appendReplacementTextRegex(StringBuilder regexBuilder, String section,
            boolean startsNewline, boolean endsWithNewline) {
        // if there are no exact matches concerns, then we can
        // split the section into lines, and introduce relaxation that
        // all non newline whitespace(NNWS for short) occuring within
        // a line are similar to one NNWS char.
        // also, leading NNWS and trailing NNWS are similar to no NNWS at all.
        List<String> splitText = TaskUtils.splitIntoLines(section);
        for (int i = 0; i < splitText.size(); i+=2) {
            String line = splitText.get(i).trim();
            // determine leading indent similarity test
            // only ignore leading indent test if we are on
            // first line and it does not start with a newline in
            // original larger text.
            if (i > 0 || startsNewline) {
                regexBuilder.append("[ \f\t]*");
            }
            Matcher nnWsMatcher = NON_NEWLINE_WS_MULTIPLE_REGEX.matcher(line);
            int start = 0;
            boolean midNnwsTestAdded = false;
            if (nnWsMatcher.find(start)) {
                // add substring before ws
                regexBuilder.append(Pattern.quote(line.substring(start, 
                    nnWsMatcher.start())));
                regexBuilder.append("[ \f\t]+");
                start = nnWsMatcher.end();
                midNnwsTestAdded = true;
            }
            // cater for remainder and if not empty, deal with trailing indent test.
            // if remainder is empty, then it means the last NNWS test has to
            // be modified from 'at least one' to 'zero or more' to serve as a
            // trailing indent test.
            if (start < line.length()) {
                regexBuilder.append(Pattern.quote(line.substring(start)));
                // determine trailing indent similarity test
                // only ignore trailing indent test if we are on
                // last line and it does not end with a newline in
                // original larger text. 
                if (i + 2 < splitText.size() || endsWithNewline) {
                    regexBuilder.append("[ \f\t]*");
                }
            }
            else if (midNnwsTestAdded) {
                if (i + 2 < splitText.size() || endsWithNewline) {
                    regexBuilder.setCharAt(regexBuilder.length() - 1, '*');
                }
            }
            String terminator = splitText.get(i + 1);
            if (terminator != null) {
                regexBuilder.append(Pattern.quote(terminator));
            }
        }
    }
}
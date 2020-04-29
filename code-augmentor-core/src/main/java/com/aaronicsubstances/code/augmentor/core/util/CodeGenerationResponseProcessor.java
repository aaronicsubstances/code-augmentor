package com.aaronicsubstances.code.augmentor.core.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

public class CodeGenerationResponseProcessor {
    static final String NON_NEWLINE_WS_SINGLE_RGX = "[ \f\t]";
    private static final Pattern NON_NEWLINE_WS_MULTIPLE_REGEX = Pattern.compile(
        NON_NEWLINE_WS_SINGLE_RGX + "+");

    public static int[] determineReplacementRange(CodeSnippetDescriptor snippetDescriptor, 
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
            // resort to default behaviour
            if (genCodeDescriptor != null) {
                // treat default and inline descriptors differently.
                if (genCodeDescriptor.isInline()) {
                    replacementRange = new int[]{ genCodeDescriptor.getStartDirectiveStartPos(),
                        genCodeDescriptor.getEndDirectiveEndPos() };
                } 
                else {
                    // by default range of generated code excludes directive markers.
                    // it starts from just after the start directive marker,
                    // and ends just before the end directive marker.
                    replacementRange = new int[]{ genCodeDescriptor.getStartDirectiveEndPos(),
                        genCodeDescriptor.getEndDirectiveStartPos() };
                }
            }
            else {
                // resort to empty range positioned at the end of the aug code.   
                replacementRange = new int[]{ augCodeDescriptor.getEndPos(),
                    augCodeDescriptor.getEndPos() };
            }
        }
        return replacementRange;
    }

    public static String ensureEndingNewline(String code, String newline) {
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

    public static void repairSplitCrLfs(List<ContentPart> contentParts) {
        for (int i = 0; i < contentParts.size() - 1; i++) {
            ContentPart curr = contentParts.get(i);
            if (curr.getContent().endsWith("\r")) {
                ContentPart next = contentParts.get(i + 1);
                if (next.getContent().startsWith("\n")) {
                    // move the \n from next to curr
                    curr.setContent(curr.getContent() + "\n");
                    next.setContent(next.getContent().substring(1));
                }
            }
        }
    }

    public static String getEffectiveIndent(AugmentingCodeDescriptor augCodeDescriptor, 
            GeneratedCode genCode) {
        if (genCode.isDisableAutoIndent()) {
            return "";
        }
        if (!TaskUtils.isEmpty(genCode.getIndent())) {
            return genCode.getIndent();
        }
        return augCodeDescriptor.getIndent();
    }

    public static void indentCode(List<ContentPart> contentParts, String indent) {
        for (int i = 0; i < contentParts.size(); i++) {
            ContentPart code = contentParts.get(i);
            if (code.getContent().isEmpty()) {
                continue;
            }
            boolean startsOnNewline = doesPartBeginOnNewline(contentParts, i);
            List<String> splitCode = TaskUtils.splitIntoLines(code.getContent());
            StringBuilder codeBuffer = new StringBuilder();
            for (int j = 0; j < splitCode.size(); j+=2) {
                String line = splitCode.get(j);
                if (j > 0 || startsOnNewline) {
                    // as a policy don't indent empty lines, similar
                    // to what IDEs do.
                    // NB: this policy also enables CRLF split across content parts to 
                    // work automatically without repairing them.
                    // any change in policy of not indenting empty lines
                    // must cater for CRLF splits as well.
                    if (!line.isEmpty()) {
                        codeBuffer.append(indent);
                    }
                }
                codeBuffer.append(line);
                String terminator = splitCode.get(j + 1);
                if (terminator != null) {
                    codeBuffer.append(terminator);
                }
            }
            code.setContent(codeBuffer.toString());
        }
    }

	public static boolean shouldWrapInGenCodeDirectives(GeneratedCode genCode,
			GeneratedCodeDescriptor generatedCodeDescriptor) {
        if (genCode.isReplaceAugCodeDirectives() || genCode.isReplaceGenCodeDirectives()) {
            return false;
        }
        if (generatedCodeDescriptor == null || generatedCodeDescriptor.isInline()) {
            return true;
        }
		return false;
	}

	public static boolean areTextsSimilar(String textToBeReplaced,
			List<ContentPart> contentParts) {
        String similarityRegexStr = buildSimilarityRegex(contentParts);
        Pattern regex = Pattern.compile(similarityRegexStr);
        boolean similar = regex.matcher(textToBeReplaced).matches();
        return similar;
    }
    
    static String buildSimilarityRegex(List<ContentPart> contentParts) {
        StringBuilder regexBuilder = new StringBuilder();
        for (int i = 0; i < contentParts.size(); i++) {
            ContentPart part = contentParts.get(i);
            String content = part.getContent();
            if (content.isEmpty()) {
                continue;
            }
            if (part.isExactMatch()) {
                regexBuilder.append(Pattern.quote(content));
            }
            else {
                boolean startsOnNewline = doesPartBeginOnNewline(contentParts, i);
                boolean endsWithNewline = doesPartEndWithNewline(contentParts, i);

                appendReplacementTextRegex(regexBuilder, content, startsOnNewline, 
                    endsWithNewline);
            }
        }
        if (regexBuilder.length() == 0) {
            regexBuilder.append(NON_NEWLINE_WS_SINGLE_RGX).append("*");
        }
        return regexBuilder.toString();
    }

    static boolean doesPartBeginOnNewline(List<ContentPart> contentParts, int partIndex) {
        for (int i = partIndex - 1; i >= 0; i--) {
            String prevContent = contentParts.get(i).getContent();
            if (prevContent.isEmpty()) {
                continue;
            }
            char prevContentLastChar = prevContent.charAt(prevContent.length() - 1);
            return TaskUtils.isNewLine(prevContentLastChar);
        }
        // getting here means contentParts[partIndex] is the first non-empty content.
        return true;
    }

    static boolean doesPartEndWithNewline(List<ContentPart> contentParts, int partIndex) {
        if (partIndex < contentParts.size() - 1) {
            String content = contentParts.get(partIndex).getContent();
            char lastContentChar = content.charAt(content.length() - 1);
            return TaskUtils.isNewLine(lastContentChar);
        }
        // getting here means contentParts[partIndex] is the last content.
        // treat as end of entire input as equivalent to end of line
        // whether it actually has a newline ending or not.
        return true;
    }

    private static void appendReplacementTextRegex(
            StringBuilder regexBuilder,
            String section,
            boolean firstSplitStartsOnNewline, 
            boolean lastSplitEndsWithNewline) {
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
            if (i > 0 || firstSplitStartsOnNewline) {
                regexBuilder.append(NON_NEWLINE_WS_SINGLE_RGX).append("*");
            }
            Matcher nnWsMatcher = NON_NEWLINE_WS_MULTIPLE_REGEX.matcher(line);
            int start = 0;
            boolean midNnwsTestAdded = false;
            if (nnWsMatcher.find(start)) {
                // add substring before ws
                regexBuilder.append(Pattern.quote(line.substring(start, 
                    nnWsMatcher.start())));
                regexBuilder.append(NON_NEWLINE_WS_SINGLE_RGX).append("+");
                start = nnWsMatcher.end();
                midNnwsTestAdded = true;
            }
            // cater for remainder and if not empty, deal with trailing indent test.
            if (start < line.length()) {
                regexBuilder.append(Pattern.quote(line.substring(start)));
                // determine trailing indent similarity test
                // only ignore trailing indent test if we are on
                // last line and it does not end with a newline in
                // original larger text. 
                if (i + 2 < splitText.size() || lastSplitEndsWithNewline) {
                    regexBuilder.append(NON_NEWLINE_WS_SINGLE_RGX).append("*");
                }
            }
            else {
                // if remainder is empty, then it means the last NNWS test may have to
                // be modified from 'at least one' to 'zero or more' to serve as a
                // trailing indent test.
                if (midNnwsTestAdded) {
                    if (i + 2 < splitText.size() || lastSplitEndsWithNewline) {
                        regexBuilder.setCharAt(regexBuilder.length() - 1, '*');
                    }
                }
            }
            String terminator = splitText.get(i + 1);
            if (terminator != null) {
                regexBuilder.append(Pattern.quote(terminator));
            }
        }
    }
}
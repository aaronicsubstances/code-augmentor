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
    private static final Pattern NON_NEWLINE_WS_MULTIPLE_REGEX = Pattern.compile("[ \t\f]+");

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
            // default
            replacementRange = null;
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

    public static String getEffectiveIndent(AugmentingCodeDescriptor augCodeDescriptor, GeneratedCode genCode) {
        // don't indent code if generated code contains any content part requiring exact
        // matching.
        for (ContentPart part : genCode.getContentParts()) {
            if (part.isExactMatch()) {
                return "";
            }
        }
        String indent = genCode.getIndent();
        if (TaskUtils.isEmpty(indent)) {
            indent = augCodeDescriptor.getIndent();
        }
        return indent;
    }

    public static String indentCode(String code, String indent) {
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
    
    public static String wrapInGeneratedCodeDirectives(String code, String genCodeStartDirective,
            String genCodeEndDirective,
            String indent, String newline) {
        return indent + genCodeStartDirective + newline +
            code +
            indent + genCodeEndDirective + newline;
    }

	public static boolean areTextsSimilar(String textToBeReplaced, String replacementText,
			List<ContentPart> contentParts) {
        long exactMatchCount = contentParts.stream().filter(x -> x.isExactMatch()).count();
        if (exactMatchCount == contentParts.size()) {
            return replacementText.equals(textToBeReplaced);
        }
        // build regex out of replacement text and match it against all of textToBeReplaced
        StringBuilder regexBuilder = new StringBuilder();
        if (exactMatchCount == 0) {
            appendReplacementTextRegex(regexBuilder, replacementText, true, true);
        }
        else {
            // Else exact matches exist. 
            // In that case annotate each part with 3 pieces of information: includedInExactMatch,
            // hasNewlineStart, hasNewlineEnd.
            for (int i = 0; i < contentParts.size(); i++) {
                ContentPart part = contentParts.get(i);
                String content = part.getContent();
                if (content.isEmpty()) {
                    continue;
                }
                if (!part.isExactMatch()) {
                    boolean startsNewline = true;
                    if (i > 0) {
                        String prevContent = contentParts.get(i - 1).getContent();
                        char prevContentLastChar = prevContent.charAt(prevContent.length() - 1);
                        if (!TaskUtils.isNewLine(prevContentLastChar)) {
                            startsNewline = false;
                        }
                    }

                    boolean endsWithNewline = true;
                    if (i < contentParts.size() - 1) {
                        char lastContentChar = content.charAt(content.length() - 1);
                        endsWithNewline = TaskUtils.isNewLine(lastContentChar);
                    }

                    appendReplacementTextRegex(regexBuilder, content, startsNewline, 
                        endsWithNewline);
                }
                else {
                    regexBuilder.append(Pattern.quote(content));
                }
            }
        }
        Pattern regex = Pattern.compile(regexBuilder.toString());
        boolean similar = regex.matcher(textToBeReplaced).matches();
        return similar;
	}

    private static void appendReplacementTextRegex(StringBuilder regexBuilder, String section,
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
            if (start < line.length()) {
                regexBuilder.append(Pattern.quote(line.substring(start)));
                // determine trailing indent similarity test
                // only ignore trailing indent test if we are on
                // last line and it does not end with a newline in
                // original larger text. 
                if (i + 2 < splitText.size() || lastSplitEndsWithNewline) {
                    regexBuilder.append("[ \f\t]*");
                }
            }
            else {
                // if remainder is empty, then it means the last NNWS test has to
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
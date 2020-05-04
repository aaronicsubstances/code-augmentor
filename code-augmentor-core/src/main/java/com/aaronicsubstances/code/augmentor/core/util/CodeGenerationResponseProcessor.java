package com.aaronicsubstances.code.augmentor.core.util;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

public class CodeGenerationResponseProcessor {

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

    /**
     * Modified content parts in place to insert indents before each occurring line.
     * <p>
     * Precondition: {@link #repairSplitCrLfs(List)} should have been
     * called on content parts.
     * <p>
     * Postcondition: the result of the indentation should be the same
     * as indenting the string resulting from concatenating the content parts directly.
     * 
     * @param contentParts
     * @param indent
     */
    public static void indentCode(List<ContentPart> contentParts, String indent) {
        for (int i = 0; i < contentParts.size(); i++) {
            ContentPart code = contentParts.get(i);
            boolean startsOnNewline = doesPartBeginOnNewline(contentParts, i);
            List<String> splitCode = TaskUtils.splitIntoLines(code.getContent());
            StringBuilder codeBuffer = new StringBuilder();
            for (int j = 0; j < splitCode.size(); j+=2) {
                String line = splitCode.get(j);
                if (j > 0 || startsOnNewline) {
                    // as a policy don't indent empty lines, similar
                    // to what IDEs do.
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

    static boolean doesPartBeginOnNewline(List<ContentPart> contentParts, int partIndex) {
        if (contentParts.get(partIndex).getContent().isEmpty()) {
            return false;
        }
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
}
package com.aaronicsubstances.code.augmentor.core.util;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

/**
 * Contains helper methods used by code generation responses processing to merge
 * generated code content into source code files.
 */
public class CodeGenerationResponseProcessor {

    /**
     * Determines the substring of a source code file which should be replaced by
     * generated code content.
     * @param snippetDescriptor object containing location descriptors for a
     * generated code object and its corresponding augmenting code object.
     * @param genCode generated code object.
     * @return 2-element int array in which first int is starting index (inclusive) 
     * of substring and second int is ending index (exclusive) of substring.
     */
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
                // NB: starting index is aug code section ending rather than gen code
                // section beginning. This ensures blank lines between aug code
                // and gen code are replaced.
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

    /**
     * Determines whether content of generated code should be appended with 
     * newline if it doesn't end with one.
     * @param genCode generated code object
     * @return true to indicate that newline should be appended; 
     * false to skip appending of newline.
     */
	public static boolean getShouldEnsureEndingNewline(GeneratedCode genCode) {
        // if replace aug or gen code directives, then let gen code completely control
        // whether or not to ensure ending newline.
        if (genCode.isReplaceAugCodeDirectives() || genCode.isReplaceGenCodeDirectives()) {
            return !genCode.isDisableEnsureEndingNewline();
        }
        // always ensure ending newline by default.
        return true;
	}

    /**
     * Appends a newline to a string unless the string already ends with a newline.
     * @param code string to append newline to
     * @param newline newline variant to use
     * @return string which ends with newline.
     */
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

    /**
     * Modifies content parts to remove split CR-LFs, that is, a sequence of 
     * carriage return and line feeds which are split across content parts, so
     * that the carriage return character ends a content part, and the following content part
     * starts with the line feed character. 
     * <p>
     * The {@link #indentCode(List, String)} method and the similarity algorithm
     * implemented by {@link GeneratedCodeSimilarityChecker} both depend on the absence of 
     * split CR-LFs.
     * 
     * @param contentParts content parts to be modified.
     */
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

    /**
     * Determines effective indent to apply to generated code content.
     * @param snippetDescriptor descriptor of augmenting code and
     * generated code sections corresponding to generated code object.
     * @param genCode generated code object.
     * @return indent to apply or empty string if no indent should be applied.
     */
    public static String getEffectiveIndent(CodeSnippetDescriptor snippetDescriptor, 
            GeneratedCode genCode) {
        if (genCode.getIndent() != null) {
            return genCode.getIndent();
        }
        // if replace aug or gen code directives, then let gen code completely control
        // indentation.
        if (genCode.isReplaceAugCodeDirectives() || genCode.isReplaceGenCodeDirectives()) {
            return "";
        }
        // by default use gen code descriptor's indent, and if it is not available use 
        // aug code descriptor's indent.
        GeneratedCodeDescriptor genCodeDescriptor = snippetDescriptor.getGeneratedCodeDescriptor();
        String indent = null;
        if (genCodeDescriptor != null) {
            indent = genCodeDescriptor.getIndent();
        }
        if (indent == null) {
            indent = snippetDescriptor.getAugmentingCodeDescriptor().getIndent();
        }
        return indent;
    }

    /**
     * Determines whether generated code content should be wrapped in start/end
     * directives.
     * 
     * @param genCode generated code object
     * @param generatedCodeDescriptor generated code descriptor
     * 
     * @return true if wrapping should be done; false if wrapping should be
     * skipped.
     */
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
     * @param contentParts content parts to be modified.
     * @param indent indent to apply.
     */
    public static void indentCode(List<ContentPart> contentParts, String indent) {
        for (int i = 0; i < contentParts.size(); i++) {
            ContentPart code = contentParts.get(i);
            boolean startsOnNewline = doesPartBeginOnNewline(contentParts, i);
            List<String> splitCode = TaskUtils.splitIntoLines(code.getContent(), true);
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
package com.aaronicsubstances.code.augmentor.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

public class CodeGenerationResponseProcessor {
    static final String NON_NEWLINE_WS_CHARS = " \t\f";
    static final String NON_NEWLINE_WS_SINGLE_RGX = "["+ NON_NEWLINE_WS_CHARS +"]";

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

    /**
     * Compares a string against the whole string of content parts
     * for similarity, by treating certain non newline spaces inside 
     * the content parts as insignificant.
     * 
     * <p>
     * The rules involve either completely removing a contiguous space ("removed" for short),
     * or reducing a contiguous space to a single space ("reduced" for short) inside
     * non-exact content parts. Every contiguous space inside an exact content part cannot be
     * removed or reduced at all.
     * <p>
     * By default,
     *  <ul>
     *    <li>Leading space of lines or a contiguous space which begins the entire
     *        content part list are removed.</li>
     *    <li>Trailing space of lines or a contiguous space which ends the entire
     *        content part list are removed.</li>
     *    <li>Any contiguous space inside lines (ie surrounded on both sides by 
     *        non-newline characters) are reduced.</li>
     *  </ul>
     * When exact content parts are present, the following exceptions apply:
     *  <ul>
     *    <li>any contiguous space immediately followed by an exact content part
     *        is not removed, but reduced.</li>
     *    <li>any contiguous space which ends the entire content part list and
     *        immediately follows an exact content part is not removed, but reduced.</li>
     *    <li>if an exact content part does not end with a newline, then any
     *        contiguous space immediately following it is not removed, but reduced.</li>
     *  </ul>
     * 
     * Precondition: {@link #repairSplitCrLfs(List)} should have been
     * called on content parts.
     * 
     * @param textToBeReplaced
     * @param contentParts
     * @return true if differences between textToBeReplaced and contentParts are
     * superficial/insignificant; false if differences are significant.
     */
	public static boolean areTextsSimilar(String textToBeReplaced,
			List<ContentPart> contentParts, boolean enableLogging) {
        String similarityRegexStr = buildSimilarityRegex(contentParts, enableLogging);
        Pattern regex = Pattern.compile(similarityRegexStr);
        boolean similar = regex.matcher(textToBeReplaced).matches();
        return similar;
    }

    static String buildSimilarityRegex(List<ContentPart> contentParts, boolean enableLogging) {
        /*
         * 3 passes yielding object to be used in completion phase.
         * object props: 
         *  - list of tokens
         *  - exactMatchSeen: bool
         *  - applyLeadingIndentAtBeginningOfInput: String
         *  - applyLeadingIndentAtEndOfInput: String
         * token props:
         *  - type: exact match, ws, newline, other
         *  - value: string, not empty.
         *  - applyLeadingIndentAfter: bool
         */
        class Tk {
            public Tk(int type, String value) {
                this.type = type;
                this.value = value;
                if (type == Tk.TYPE_EXACT_MATCH) {
                    char firstChar = value.charAt(0);
                    startsWithNewline = TaskUtils.isNewLine(firstChar);
                    char lastChar = value.charAt(value.length() - 1);
                    endsWithNewline = TaskUtils.isNewLine(lastChar);
                }
                else {
                    startsWithNewline = type == TYPE_NEW_LINE;
                    endsWithNewline = startsWithNewline;
                }
            }

            static final int TYPE_EXACT_MATCH = 1;
            static final int TYPE_SPACE = 2;
            static final int TYPE_NEW_LINE = 3;
            static final int TYPE_OTHER = 4;

            final int type;
            final String value;
            final boolean startsWithNewline;
            final boolean endsWithNewline;
            boolean applyLeadingIndentAfter = false;

            @Override
            public String toString() {
                String typeName;
                switch (type) {
                    case TYPE_EXACT_MATCH:
                        typeName = "EXACT_MATCH";
                        break;
                    case TYPE_SPACE:
                        typeName = "SPACE";
                        break;
                    case TYPE_NEW_LINE:
                        typeName = "NEW_LINE";
                        break;
                    case TYPE_OTHER:
                        typeName = "OTHER";
                        break;
                    default:
                        throw new RuntimeException("Unexpected type: " + type);
                }
                String formattedValue = PersistenceUtil.serializeCompactlyToJson(value);
                return "{type=" + typeName + ", value=" + formattedValue +
                    ", applyLeadingIndentAfter=" + applyLeadingIndentAfter +  
                    ", startsWithNewline=" + startsWithNewline +
                    ", endsWithNewline=" + endsWithNewline + "}";
            }
        }
        class AlgContext {
            List<Tk> tokens = new ArrayList<>();
            boolean exactMatchSeen;
            String applyLeadingIndentAtBOI;
            String applyTrailingIndentAtEOI;

            @Override
            public String toString() {
                return "AlgContext{applyLeadingIndentAtBOI=" + applyLeadingIndentAtBOI + ", applyTrailingIndentAtEOI="
                        + applyTrailingIndentAtEOI + ", exactMatchSeen=" + exactMatchSeen + ", tokens=" + tokens + "}";
            }
        }

         /*  
         * building of completion object phase:
         *  phase 1: build tokens, set exactMatchSeen, ignore empty strings,
         *     through all content parts
         *        if exact match - treat as such.
         *        if newline - treat as such.
         *        if whitespace, look for contiguous non-newline whitespace
         *        else look for contigious text other than whitespace.
         */

        AlgContext algContext = new AlgContext();
        algContext.exactMatchSeen = contentParts.stream().anyMatch(x -> x.isExactMatch());
        algContext.tokens = new ArrayList<Tk>();

        Predicate<Character> spaceCondition = c -> {
            return NON_NEWLINE_WS_CHARS.contains(c.toString());
        };
        Predicate<Character> otherCondition = c -> {
            return !spaceCondition.test(c) && !TaskUtils.isNewLine(c);
        };

        int partIndex = 0;
        int contentIndexToStartFrom = 0; 
        while (partIndex < contentParts.size()) {
            ContentPart contentPart = contentParts.get(partIndex);
            String content = contentPart.getContent();
            // ignore empty strings even if it is exact match.
            if (content.isEmpty()) {
                partIndex++;
                continue;
            }
            if (contentPart.isExactMatch()) {
                algContext.tokens.add(new Tk(Tk.TYPE_EXACT_MATCH, content));
                partIndex++;
                continue;
            }
            int i = 0;
            if (contentIndexToStartFrom > 0) {
                i = contentIndexToStartFrom;
                contentIndexToStartFrom = 0;
            }
            int partIndexToUseNext = -1;
            for (; i < content.length(); i++) {
                char curr = content.charAt(i);
                if (TaskUtils.isNewLine(curr)) {
                    if (curr == '\r' &&
                            i + 1 < content.length() &&
                            content.charAt(i + 1) == '\n') {
                        algContext.tokens.add(new Tk(Tk.TYPE_NEW_LINE, "\r\n")); 
                        i++;
                    }
                    else {
                        algContext.tokens.add(new Tk(Tk.TYPE_NEW_LINE, "" + curr));
                    }
                }
                else {
                    int type;
                    Predicate<Character> condition;
                    if (spaceCondition.test(curr)) {
                        condition = spaceCondition;
                        type = Tk.TYPE_SPACE;
                    }
                    else {
                        assert otherCondition.test(curr);
                        condition = otherCondition;
                        type = Tk.TYPE_OTHER;
                    }
                    int[] nextIndices = { partIndex, i };
                    String value = fetchContiguousTkVal(contentParts, nextIndices, condition);
                    algContext.tokens.add(new Tk(type, value)); 
                    partIndexToUseNext = nextIndices[0];
                    contentIndexToStartFrom = nextIndices[1];
                    if (partIndexToUseNext < partIndex) { 
                        throw new RuntimeException(
                            String.format("Expected no decrement in partIndex "+
                            "but instead %s dropped to %s", partIndex, partIndexToUseNext));
                    }
                    if (partIndexToUseNext == partIndex) {
                        if (contentIndexToStartFrom <= i) { 
                            throw new RuntimeException(
                                String.format("Expected strict increment in contentIndex "+
                                    "but instead %s remained at / dropped to %s", 
                                    i, contentIndexToStartFrom));
                        }
                    }
                    break;
                }
            }
            if (partIndexToUseNext != -1) {
                partIndex = partIndexToUseNext;
            }
            else {
                partIndex++;
            }
        }

        if (enableLogging) {
            System.out.format("After phase 1, context = %s\n\n", algContext);
        }

        /*
         *   phase 2: classifying whitespace significance
         *     if any leading whitespace tokens are found
         *         set applyLeadingIndentAtBOI to optional space and remove them
         *     if any trailing whitespace tokens are found
         *         set applyTrailingIndentAtEOI to optional space and remove them
         *     through all tokens except first and last.
         *         remove whitespace tokens as we go along according to the ff:
         *             if preceding or following token is exact, mark as insignificant
         *               only if following token is not exact and preceding token ends with
         *                   newline.
         *             else if preceding or following token is newline, mark as insignificant
         */

        while (!algContext.tokens.isEmpty()) {
            if (algContext.tokens.get(0).type == Tk.TYPE_SPACE) {
                algContext.tokens.remove(0);
                algContext.applyLeadingIndentAtBOI = "*";
            }
            else {
                break;
            }
        }
        while (!algContext.tokens.isEmpty()) {
            if (algContext.tokens.get(algContext.tokens.size() - 1).type == Tk.TYPE_SPACE) {
                algContext.tokens.remove(algContext.tokens.size() - 1);
                algContext.applyTrailingIndentAtEOI = "*";
            }
            else {
                break;
            }
        }
        int tkIndex = 1;
        while (tkIndex < algContext.tokens.size() - 1) {
            Tk token = algContext.tokens.get(tkIndex);
            boolean significant = true;
            if (token.type == Tk.TYPE_SPACE) {
                Tk prevToken = algContext.tokens.get(tkIndex - 1);
                Tk nextToken = algContext.tokens.get(tkIndex + 1);
                if (prevToken.type == Tk.TYPE_EXACT_MATCH ||
                        nextToken.type == Tk.TYPE_EXACT_MATCH) {
                    if (nextToken.type != Tk.TYPE_EXACT_MATCH &&
                            prevToken.endsWithNewline) {
                        significant = false;
                    }
                }
                else if (prevToken.type == Tk.TYPE_NEW_LINE || 
                        nextToken.type == Tk.TYPE_NEW_LINE) {
                    significant = false;
                }
            }
            if (significant) {
                tkIndex++;
            }
            else {
                algContext.tokens.remove(tkIndex);
            }
        }

        if (enableLogging) {
            System.out.format("After phase 2, context = %s\n\n", algContext);
        }

        /*
         *  phase 3: determine condition for applying indents
         *     always apply trailing indent before a newline token
         *     if token list is not empty and even has only 1 element:
         *         if first token is other, 
         *             set applyLeadingIndentAtBOI to optional space
         *         else if first token is newline
         *             unset applyLeadingIndentAtBOI since newline trailing indent will
         *             do same anyway.
         *         else if first token is exact AND applyLeadingIndentAtBOI is
         *              already set due to leading whitespace
         *              chanage applyLeadingIndentAtBOI to required space.
         *     if token list is not empty and even has only 1 element:
         *          if last token is other or newline
         *              set applyTrailingIndentAtEOI to optional space.
         *         else if last token is exact AND applyTrailingIndentAtEOI is
         *              already set due to trailing whitespace
         *              chanage applyLeadingIndentAtBOI to required space.
         *     
         *     if last token is a newline, then don't apply leading indent afterwards
         *     (applyTrailingIndentATEOI caters for that)
         *     through all tokens except last one
         *         if newline token is followed by another newline
         *         then don't apply leading indent afterwards, else apply it
         *         if exact token has newline ending and is followed by other token,
         *         also apply leading indent afterwards
         * 
         */

        if (!algContext.tokens.isEmpty()) {
            Tk firstToken = algContext.tokens.get(0);
            if (firstToken.type == Tk.TYPE_OTHER) {
                algContext.applyLeadingIndentAtBOI = "*";
            }
            else if (firstToken.type == Tk.TYPE_NEW_LINE) {
                algContext.applyLeadingIndentAtBOI = null;
            }
            else if (firstToken.type == Tk.TYPE_EXACT_MATCH) {
                if (algContext.applyLeadingIndentAtBOI != null) {
                    algContext.applyLeadingIndentAtBOI = "+";
                }
            }
        }
        if (!algContext.tokens.isEmpty()) {
            Tk lastToken = algContext.tokens.get(algContext.tokens.size() - 1);
            if (lastToken.type == Tk.TYPE_OTHER || lastToken.type == Tk.TYPE_NEW_LINE) {
                algContext.applyTrailingIndentAtEOI = "*";
            }
            else if (lastToken.type == Tk.TYPE_EXACT_MATCH) {
                if (algContext.applyTrailingIndentAtEOI != null) {
                    algContext.applyTrailingIndentAtEOI = "+";
                }
            }
        }
        for (tkIndex = 0; tkIndex < algContext.tokens.size() - 1; tkIndex++) {
            Tk token = algContext.tokens.get(tkIndex);
            if (token.type == Tk.TYPE_NEW_LINE) {
                Tk nextToken = algContext.tokens.get(tkIndex + 1);
                token.applyLeadingIndentAfter = nextToken.type != Tk.TYPE_NEW_LINE;
            }
            else if (token.type == Tk.TYPE_EXACT_MATCH && token.endsWithNewline) {
                Tk nextToken = algContext.tokens.get(tkIndex + 1);
                token.applyLeadingIndentAfter = nextToken.type == Tk.TYPE_OTHER;
            }
        }

        if (enableLogging) {
            System.out.format("After phase 3, context = %s\n\n", algContext);
        }

        /*
         * completion phase:
         *  1. if applyLeadingIndentAtBOI is set, use it to append non newline whitespace regex
         *  2. for each significant token, 
         *      if exact match or other, quote and append to regex builder
         *      if ws, append one or more non newline whitespace regex
         *      if newline
         *           append zero or more non newline whitespace regex 
         *           quote and append newline to regex builder
         *      if applyLeadingIndentAfter, append zero or more non newline whitespace regex
         * 3. if applyTrailingIndentAtEOI is set, use it to append non newline whitespace regex
         * 4. if regex builder is empty at this stage,
         *        if exactMatchSeen, then leave it empty
         *        else add zero or more non newline whitespace regex to it.
         */
        StringBuilder regexBuilder = new StringBuilder();
        if (algContext.applyLeadingIndentAtBOI != null) {
            regexBuilder.append(NON_NEWLINE_WS_SINGLE_RGX);
            regexBuilder.append(algContext.applyLeadingIndentAtBOI);
        }
        String anySpaceRgx = NON_NEWLINE_WS_SINGLE_RGX + '*';
        for (Tk token : algContext.tokens) {
            if (token.type == Tk.TYPE_EXACT_MATCH || 
                    token.type == Tk.TYPE_OTHER) {
                regexBuilder.append(Pattern.quote(token.value));
            }
            else if (token.type == Tk.TYPE_SPACE) {
                regexBuilder.append(NON_NEWLINE_WS_SINGLE_RGX).append('+');
            }
            else {
                assert token.type == Tk.TYPE_NEW_LINE;
                regexBuilder.append(anySpaceRgx);
                // no need quote. already valid regex. and helps with test results.
                regexBuilder.append(token.value);
            }
            if (token.applyLeadingIndentAfter) {
                regexBuilder.append(anySpaceRgx);
            }
        }
        if (algContext.applyTrailingIndentAtEOI != null) {
            regexBuilder.append(NON_NEWLINE_WS_SINGLE_RGX);
            regexBuilder.append(algContext.applyTrailingIndentAtEOI);
        }
        if (regexBuilder.length() == 0) {
            if (!algContext.exactMatchSeen) {
                regexBuilder.append(anySpaceRgx);
            }
        }

        if (enableLogging) {
            System.out.format("After completion phase, regex = %s\n\n\n", 
                PersistenceUtil.serializeCompactlyToJson(regexBuilder));
        }

        return regexBuilder.toString();
    }

    private static String fetchContiguousTkVal(List<ContentPart> contentParts, 
            int[] nextIndices, Predicate<Character> condition) {
        StringBuilder tkVal = new StringBuilder();
        boolean contentIndexUsed = false;
        outer: for (; nextIndices[0] < contentParts.size(); nextIndices[0]++) {
            ContentPart part = contentParts.get(nextIndices[0]);
            String content = part.getContent();
            int j = 0;
            if (!contentIndexUsed) {
                contentIndexUsed = true;
                j = nextIndices[1];
                // assert
                char validChar = content.charAt(j);
                assert condition.test(validChar): "Received position invalid with supplied " +
                        " test conditon at part " + nextIndices[0] + ", content char " + j;
                assert !part.isExactMatch();
                tkVal.append(validChar);
                j++;
                nextIndices[1] = 0;
            }
            // ignore empty strings even if it is exact match.
            if (content.isEmpty()) {
                continue;
            }
            if (part.isExactMatch()) {
                break;
            }
            for (; j < content.length(); j++) {
                char curr = content.charAt(j);
                if (condition.test(curr)) {
                    tkVal.append(curr);
                }
                else {                    
                    nextIndices[1] = j;
                    break outer;
                }
            }
        }
        assert contentIndexUsed: "Received invalid part index: " + nextIndices[0];
        return tkVal.toString();
    }
}
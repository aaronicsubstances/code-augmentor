package com.aaronicsubstances.code.augmentor.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.FiniteStateAutomaton;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.GraphAlgorithms;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.RegexAlgorithms;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.ConcatRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.KleeneClosureRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.LiteralStringRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.NfaSimulator;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.RegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.RegexToNfaConvertor;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.UnionRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.NfaSimulator.Observation;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetChangeDescriptor;
import com.aaronicsubstances.code.augmentor.core.models.CodeSnippetChangeDescriptor.ExactValue;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

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
 * Precondition: {@link CodeGenerationResponseProcessor#repairSplitCrLfs(List)} should have been
 * called on content parts.
 */
public class GeneratedCodeSimilarityChecker {
    static final int MATCH_TYPE_ANY_SPACES = 0;
    static final int MATCH_TYPE_REQUIRE_SPACE = 1;

    static final String MISMATCH_TYPE_END_OF_SECTION = "end_of_section";
    static final String MISMATCH_TYPE_EXACT = "exact_value";
    static final String MISMATCH_TYPE_REQUIRED_SPACE = "required_spacing";

    private static final String NON_NEWLINE_WS_CHARS;
    private static final RegexNode optionalMultipleSpaceRegex;
    private static final RegexNode requiredMultipleSpaceRegex;

    private static final int MAX_EXPECTED_SUBSTRING_LEN = 30;

    static {
        NON_NEWLINE_WS_CHARS = " \t\f";
        List<RegexNode> spaceNodes = new ArrayList<>();
        NON_NEWLINE_WS_CHARS.codePoints().forEach(c -> {
            spaceNodes.add(new LiteralStringRegexNode(c));
        });
        UnionRegexNode spaceRegex = new UnionRegexNode(spaceNodes);
        optionalMultipleSpaceRegex =
            new KleeneClosureRegexNode(spaceRegex);

        requiredMultipleSpaceRegex = new ConcatRegexNode(
            spaceRegex.generateCopy(), optionalMultipleSpaceRegex.generateCopy());
    }

    private final List<ContentPart> contentParts;
    private final List<Object> similarityRegex;
    private final List<Integer> regexPositions;

    /**
     * Creates new instance for testing strings for equivalence with
     * a given list of content parts.
     * @param contentParts list of content parts.
     */
    public GeneratedCodeSimilarityChecker(List<ContentPart> contentParts) {
        this(contentParts, false);
    }

    /**
     * Constructor for testing.
     * @param contentParts content parts against which strings will be compared for
     * equivalence.
     * @param loggingEnabled whether diagnostic messages should be printed to standard output during
     * building of internal regular expression object.
     */
    GeneratedCodeSimilarityChecker(List<ContentPart> contentParts,
            boolean loggingEnabled) {
        this.contentParts = contentParts;
        this.similarityRegex = new ArrayList<>();
        this.regexPositions = new ArrayList<>();
        buildSimilarityRegex(loggingEnabled);
    }

    List<Object> getSimilarityRegex() {
        return similarityRegex;
    }

    List<Integer> getRegexPositions() {
        return regexPositions;
    }

    /**
     * Determines whether all of a string matches this instance's
     * content parts.
     * 
     * @param text string to match.
     * @return null if there is similarity; an object describing significant difference if
     * otherwise.
     */
	public CodeSnippetChangeDescriptor match(String text) {
        Map<Integer, Integer> stateToRegexIndexMap = new HashMap<>();
        List<FiniteStateAutomaton> childNfas = new ArrayList<>();
        // use same convertor for all regex to nfa conversions to preserve state numbers.
        RegexToNfaConvertor nfaCreator = new RegexToNfaConvertor(null);
        for (int i = 0; i < similarityRegex.size(); i++) {
            Object regexSpec = similarityRegex.get(i);
            RegexNode regexNode;
            if (regexSpec.equals(MATCH_TYPE_ANY_SPACES)) {
                regexNode = optionalMultipleSpaceRegex;
            }
            else if (regexSpec.equals(MATCH_TYPE_REQUIRE_SPACE)) {
                regexNode = requiredMultipleSpaceRegex;
            }
            else {
                regexNode = new LiteralStringRegexNode(RegexAlgorithms.getLiteralString(regexSpec));
            }
            FiniteStateAutomaton childNfa = (FiniteStateAutomaton) regexNode.accept(nfaCreator);
            childNfas.add(childNfa);
            // add single final state and also all states reachable from start state by exactly 1
            // non null symbol, possibly transitioning on many null symbols along the way.
            for (int s : childNfa.getFinalStates()) {
                stateToRegexIndexMap.put(s, i);
            }
            Set<Integer> entryStates = getStatesReachableFromStartStateViaOneNonNullSymbol(
                childNfa);
            for (int s : entryStates) {
                stateToRegexIndexMap.put(s, i);
            }
        }

        FiniteStateAutomaton nfa = nfaCreator.makeConcatNfa(childNfas);

        NfaSimulator nfaSimulator = new NfaSimulator(nfa);
        int[] textAsCodePoints = RegexAlgorithms.getLiteralString(text);
        int errorIndex = nfaSimulator.simulate(textAsCodePoints, 0, textAsCodePoints.length,
            stateToRegexIndexMap.keySet());
        if (errorIndex == -1) {
            return null;
        }

        int regexSpecIndex = 0;
        int indexOfRegexEntry = -1;
        if (!nfaSimulator.getObservations().isEmpty()) {
            Observation lastObservation = nfaSimulator.getObservations().get(
                nfaSimulator.getObservations().size() - 1);
            // get rightmost observed state.
            int lastObservedState = lastObservation.getStates().stream()
                .max(Integer::compare).get();
            regexSpecIndex = stateToRegexIndexMap.get(lastObservedState);
            boolean isStateFinal = childNfas.get(regexSpecIndex).getFinalStates().contains(
                lastObservedState);
            if (isStateFinal) {
                regexSpecIndex++;
            }
            else if (lastObservation.getEndIndex() > 0) {
                indexOfRegexEntry = lastObservation.getEndIndex() - 1;
            }
        }

        // end of section is the expectation if all regex specs were traversed
        // (or regex specs was empty to start with).
        String mismatchType = MISMATCH_TYPE_END_OF_SECTION;
        int destCharIndex = contentParts.stream().collect(
            Collectors.summingInt(c -> c.getContent().length()));
        ExactValue expected = null;

        if (regexSpecIndex < similarityRegex.size()) {
            destCharIndex = regexPositions.get(regexSpecIndex);
            Object expectedRegexSpec = similarityRegex.get(regexSpecIndex);
            if (expectedRegexSpec instanceof Integer) {
                assert expectedRegexSpec.equals(MATCH_TYPE_REQUIRE_SPACE);
                mismatchType = MISMATCH_TYPE_REQUIRED_SPACE;
            }
            else {
                mismatchType = MISMATCH_TYPE_EXACT;
                expected = new ExactValue();
                String exactValue = (String) expectedRegexSpec;
                expected.setLength(exactValue.length());
                int indexInExactValue = 0;
                if (indexOfRegexEntry != -1) {
                    indexInExactValue = errorIndex - indexOfRegexEntry;
                    destCharIndex += indexInExactValue;
                }
                expected.setUpdatedSectionOffset(indexInExactValue);
                expected.setUpdatedSection(fetchPrefix(exactValue, indexInExactValue, exactValue.length()));
                if (expected.getUpdatedSection().length() < exactValue.length()) {
                    expected.setPrefix(fetchPrefix(exactValue, 0, indexInExactValue));
                    if (expected.getPrefix().length() + expected.getUpdatedSection().length() < 
                            exactValue.length()) {
                        expected.setSuffix(fetchSuffix(exactValue, indexInExactValue +
                            expected.getUpdatedSection().length()));
                    }
                }
            }
        }
        
        CodeSnippetChangeDescriptor codeChange = new CodeSnippetChangeDescriptor();
        codeChange.setType(mismatchType);
        codeChange.setSrcCharIndex(errorIndex);
        codeChange.setCurrentSection(fetchPrefix(text, errorIndex, 
            errorIndex + MAX_EXPECTED_SUBSTRING_LEN));
        codeChange.setDestCharIndex(destCharIndex);
        codeChange.setExpectedExactValue(expected);
        return codeChange;
    }

    static Set<Integer> getStatesReachableFromStartStateViaOneNonNullSymbol(FiniteStateAutomaton nfa) {
        Map<Integer, Set<Integer>> nfaGraph = new HashMap<>();
        for (int state : nfa.getNfaTransitionTable().keySet()) {
            Set<Integer> nextStates = FiniteStateAutomaton.newSet();
            for (Set<Integer> stateSet : nfa.getNfaTransitionTable().get(state)
                    .values()) {
                nextStates.addAll(stateSet);
            }
            nfaGraph.put(state, nextStates);
        }
        BiFunction<Integer, Integer, Double> weightFunction = (u, v) -> {
            Map<Integer, Set<Integer>> outTransitions = nfa.getNfaTransitionTable().get(u);
            if (outTransitions.containsKey(FiniteStateAutomaton.NULL_SYMBOL)) {
                if (outTransitions.get(FiniteStateAutomaton.NULL_SYMBOL).contains(v)) {
                    return 0.0;
                }
            }
            return 1.0;
        };
        Map<Integer, Map<String, Object>> shortestPathData = 
            GraphAlgorithms.dijkstraShortestPathAlgorithm(Arrays.asList(nfaGraph), 
                weightFunction, nfa.getStartState(), null);
        Set<Integer> entryStates = FiniteStateAutomaton.newSet();
        for (int s : shortestPathData.keySet()) {
            Double shortestPath = (Double) shortestPathData.get(s).get(
                GraphAlgorithms.VERTEX_ATTRIBUTE_DIST);
            if (shortestPath != null && shortestPath == 1.0) {
                entryStates.add(s);
            }
        }
        return entryStates;
    }

    static String fetchPrefix(String s, int start, int maxEnd) {
        int len = Math.min(maxEnd - start, MAX_EXPECTED_SUBSTRING_LEN);
        len = Math.min(len, s.length() - start);
        len = Math.max(len, 0);
        String prefix = s.substring(start, start + len);
        // preserve CRLFs
        if (!prefix.isEmpty() && (start + len + 1) < s.length()) {
            char next = s.charAt(start + len + 1);
            if (next == '\n' && prefix.charAt(prefix.length() - 1) == '\r') {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
        }
        return prefix;
    }

    static String fetchSuffix(String s, int minStart) {
        int len = Math.min(s.length() - minStart, MAX_EXPECTED_SUBSTRING_LEN);
        len = Math.max(len, 0);
        return s.substring(s.length() - len);
    }

    private void buildSimilarityRegex(boolean loggingEnabled) {
        /*
         * 3 passes yielding object to be used in completion phase.
         * object props: 
         *  - list of tokens
         *  - exactMatchSeen: bool
         * token props:
         *  - type: exact match, ws, newline, other
         *  - value: string, not empty.
         *  - applyTrailingIndentBefore: bool
         *  - applyLeadingIndentAfter: bool
         */

         /*  
         * building of completion object phase:
         *  phase 1: build tokens, ignore empty strings,
         *     through all content parts
         *        if exact match - treat as such.
         *        if newline - treat as such.
         *        if whitespace, look for contiguous non-newline whitespace
         *        else look for contigious text other than whitespace.
         */

        List<Tk> tokens = new ArrayList<>();

        Predicate<Character> spaceCondition = c -> {
            return NON_NEWLINE_WS_CHARS.contains(c.toString());
        };
        Predicate<Character> otherCondition = c -> {
            return !spaceCondition.test(c) && !TaskUtils.isNewLine(c);
        };

        int partIndex = 0;
        int contentIndexToStartFrom = 0;
        int absolutePos = 0;
        while (partIndex < contentParts.size()) {
            ContentPart contentPart = contentParts.get(partIndex);
            String content = contentPart.getContent();
            // ignore empty strings even if it is exact match.
            if (content.isEmpty()) {
                partIndex++;
                continue;
            }
            if (contentPart.isExactMatch()) {
                tokens.add(new Tk(Tk.TYPE_EXACT_MATCH, content, absolutePos));
                partIndex++;
                absolutePos += content.length();
                continue;
            }
            int i = 0;
            if (contentIndexToStartFrom > 0) {
                i = contentIndexToStartFrom;
                contentIndexToStartFrom = 0;
            }
            int partIndexToUseNext = -1;
            for (; i < content.length(); i++, absolutePos++) {
                char curr = content.charAt(i);
                if (TaskUtils.isNewLine(curr)) {
                    if (curr == '\r' &&
                            i + 1 < content.length() &&
                            content.charAt(i + 1) == '\n') {
                        tokens.add(new Tk(Tk.TYPE_NEW_LINE, "\r\n", absolutePos));
                        i++;
                        absolutePos++;
                    }
                    else {
                        tokens.add(new Tk(Tk.TYPE_NEW_LINE, "" + curr, absolutePos));
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
                    int[] nextIndices = { partIndex, i, absolutePos };
                    String value = fetchContiguousTkVal(nextIndices, condition);
                    tokens.add(new Tk(type, value, absolutePos));
                    partIndexToUseNext = nextIndices[0];
                    contentIndexToStartFrom = nextIndices[1];
                    absolutePos = nextIndices[2];
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

        assert absolutePos == contentParts.stream().collect(
            Collectors.summingInt(c -> c.getContent().length()));

        if (loggingEnabled) {
            System.out.format("After phase 1, tokens = %s\n\n", tokens);
        }

        /*
         *   phase 2: classifying whitespace significance
         *     through all tokens
         *         remove whitespace tokens as we go along according to the ff:
         *             if preceding or following token is exact, don't remove
         *               except if following token is not exact and preceding token ends with
         *                   newline.
         *             else if preceding or following token is newline, remove
         *             else if whitespace is the last token, remove
         */
        
        int tkIndex = 0;
        while (tkIndex < tokens.size()) {
            Tk token = tokens.get(tkIndex);
            boolean significant = true;
            if (token.type == Tk.TYPE_SPACE) {
                Tk prevToken = null, nextToken = null;
                if (tkIndex - 1 >= 0) {
                    prevToken = tokens.get(tkIndex - 1);
                }
                if (tkIndex + 1 < tokens.size()) {
                    nextToken = tokens.get(tkIndex + 1);
                }
                if (prevToken == null || nextToken == null) {
                    // space is first or last in combined content parts.
                    // by default remove, but keep if adjacent to an exact content part.
                    if ((prevToken != null && prevToken.type == Tk.TYPE_EXACT_MATCH) ||
                            (nextToken != null && nextToken.type == Tk.TYPE_EXACT_MATCH)) {
                        // significant, don't remove.
                    }
                    else {
                        significant = false;
                    }
                }
                else if (prevToken.type == Tk.TYPE_EXACT_MATCH ||
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
                tokens.remove(tkIndex);
            }
        }

        if (loggingEnabled) {
            System.out.format("After phase 2, tokens = %s\n\n", tokens);
        }

        /*
         *  phase 3: determine condition for applying indents
         *     through all tokens
         *         if token is first and is 'other' or newline, apply trailing indent before
         *         else if token is newline and previous token is other, newline or 
         *             exact with newline ending, then apply trailing indent before
         *         NB: including newline type in previous tokens to look out for ensures
         *             that when there are consecutive newlines, trailing indent gets
         *             the precedence.
         *         if token is last and is 'other' or 'newline', apply leading indent afterwards
         *         else if token is newline or exact with newline ending, and is followed by
         *              'other' token, then apply leading indent afterwards.
         *
         */

        for (tkIndex = 0; tkIndex < tokens.size(); tkIndex++) {
            Tk token = tokens.get(tkIndex);
            Tk prevToken = null, nextToken = null;
            if (tkIndex - 1 >= 0) {
                prevToken = tokens.get(tkIndex - 1);
            }
            if (tkIndex + 1 < tokens.size()) {
                nextToken = tokens.get(tkIndex + 1);
            }
            // determine trailing indent before application
            if (prevToken == null) {
                if (token.type == Tk.TYPE_NEW_LINE || token.type == Tk.TYPE_OTHER) {
                    token.applyTrailingIndentBefore = true;
                }
            }
            else if (token.type == Tk.TYPE_NEW_LINE) {
                if (prevToken.endsWithNewline || prevToken.type == Tk.TYPE_OTHER) {
                    token.applyTrailingIndentBefore = true;
                }
            }
            // determine leading indent afterwards application
            if (nextToken == null) {
                if (token.type == Tk.TYPE_NEW_LINE || token.type == Tk.TYPE_OTHER) {
                    token.applyLeadingIndentAfter = true;
                }
            }
            else if (token.endsWithNewline) {
                if (nextToken.type == Tk.TYPE_OTHER) {
                    token.applyLeadingIndentAfter = true;
                }
            }
        }

        if (loggingEnabled) {
            System.out.format("After phase 3, tokens = %s\n\n", tokens);
        }

        /*
         * completion phase:
         *  1. for each token,
         *      if applyTrailingIndentBefore, append zero or more non newline whitespace regex
         *      if ws, append one or more non newline whitespace regex
         *      else append raw token value
         *      if applyLeadingIndentAfter, append zero or more non newline whitespace regex
         * 2. if regex builder is empty at this stage,
         *        if exactMatchSeen, then leave it empty
         *        else add zero or more non newline whitespace regex to it.
         */
        for (Tk token : tokens) {
            if (token.applyTrailingIndentBefore) {
                similarityRegex.add(MATCH_TYPE_ANY_SPACES);
                regexPositions.add(0);
            }
            if (token.type == Tk.TYPE_SPACE) {
                similarityRegex.add(MATCH_TYPE_REQUIRE_SPACE);
                regexPositions.add(token.startPos);
            }
            else {
                similarityRegex.add(token.value);
                regexPositions.add(token.startPos);
            }
            if (token.applyLeadingIndentAfter) {
                similarityRegex.add(MATCH_TYPE_ANY_SPACES);
                regexPositions.add(0);
            }
        }
        if (similarityRegex.isEmpty()) {
            boolean exactMatchSeen = contentParts.stream().anyMatch(x -> x.isExactMatch());
            if (!exactMatchSeen) {
                similarityRegex.add(MATCH_TYPE_ANY_SPACES);
                regexPositions.add(0);
            }
        }
    }

    private String fetchContiguousTkVal(int[] nextIndices, Predicate<Character> condition) {
        StringBuilder tkVal = new StringBuilder();
        boolean contentIndexUsed = false;
        outer: for (; nextIndices[0] < contentParts.size(); nextIndices[0]++) {
            ContentPart part = contentParts.get(nextIndices[0]);
            String content = part.getContent();
            int j = 0;
            if (!contentIndexUsed) {
                contentIndexUsed = true;
                j = nextIndices[1];
                nextIndices[1] = 0;
                // assert
                char validChar = content.charAt(j);
                assert condition.test(validChar): "Received position invalid with supplied " +
                        " test conditon at part " + nextIndices[0] + ", content char " + j;
                assert !part.isExactMatch();
            }
            // ignore empty strings even if it is exact match.
            if (content.isEmpty()) {
                continue;
            }
            if (part.isExactMatch()) {
                break;
            }
            for (; j < content.length(); j++, nextIndices[2]++) {
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

    private static class Tk {
        public Tk(int type, String value, int startPos) {
            this.type = type;
            this.value = value;
            this.startPos = startPos;
            if (type == Tk.TYPE_EXACT_MATCH) {
                char lastChar = value.charAt(value.length() - 1);
                endsWithNewline = TaskUtils.isNewLine(lastChar);
            }
            else {
                endsWithNewline = type == TYPE_NEW_LINE;
            }
        }

        static final int TYPE_EXACT_MATCH = 1;
        static final int TYPE_SPACE = 2;
        static final int TYPE_NEW_LINE = 3;
        static final int TYPE_OTHER = 4;

        final int type;
        final String value;
        final int startPos;
        final boolean endsWithNewline;
        boolean applyTrailingIndentBefore;
        boolean applyLeadingIndentAfter;

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
                ", startPos=" + startPos +
                ", applyTrailingIndentBefore=" + applyTrailingIndentBefore + 
                ", applyLeadingIndentAfter=" + applyLeadingIndentAfter + 
                ", endsWithNewline=" + endsWithNewline + "}";
        }
    }
}
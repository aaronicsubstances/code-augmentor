package com.aaronicsubstances.code.augmentor.parsing.peg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
//import java.util.PrimitiveIterator.OfInt;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext.ExpectationFrame;
import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext.StateSnapshot;

/**
 * Base class for parser classes.
 * 
 * <p>
 * To define a grammar, create a derived class. Each method in the derived class
 * represents a grammar rule. The rule methods can return arbitrary results.
 * </p>
 */
public class Parser<TCtx extends ParsingContext<?>> {

    private final TCtx ctx;

    public static class RuleCacheKey {
        public int methodNr;
        public Object[] args;
        public ParsingState<?> state;

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(args), state, methodNr);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RuleCacheKey other = (RuleCacheKey) obj;
            return Objects.equals(state, other.state) && methodNr == other.methodNr && Arrays.equals(args, other.args);
        }

        @Override
        public String toString() {
            return "(methodNr: " + methodNr + " state: " + state + " args: " + Arrays.toString(args) + ")";
        }
    }

    public static class RuleCacheValue {
        public Object result;
        public Throwable exception;
        public StateSnapshot snapshot;

        @Override
        public String toString() {
            return "(result: " + result + " exception: " + exception + ")";
        }
    }

    protected Map<RuleCacheKey, RuleCacheValue> ruleCache = new HashMap<>();

    public Parser(TCtx ctx) {
        this.ctx = ctx;
    }

    /**
     * Matches the end of the input
     */
    public final void EOI() {
        if (ctx.hasNext()) {
            throw ctx.noMatch("End Of Input");
        }
    }

    /**
     * Match the given runnable. In any case, the input position is restored
     * after matching. If the runnable matches, a {@link NoMatchException} is
     * raised.
     */
    public void TestNot(Runnable runnable, Supplier<String> expectation) {
        StateSnapshot snapshot = ctx.snapshot();
        boolean success = false;
        try {
            runnable.run();
            success = true;
        } catch (NoMatchException e) {
            // swallow
        } finally {
            snapshot.restore();
        }
        if (success) {
            throw ctx.noMatch(expectation.get());
        }
    }

    /**
     * Match the given runnable. The input position is not advanced.
     */
    public void Test(Runnable runnable) {
        StateSnapshot snapshot = ctx.snapshot();
        try {
            runnable.run();
        } finally {
            snapshot.restore();
        }
    }

    /**
     * Match the given runnable. The input position is not advanced.
     */
    public <T> T TestRet(Supplier<T> term) {
        StateSnapshot snapshot = ctx.snapshot();
        try {
            return term.get();
        } finally {
            snapshot.restore();
        }
    }

    /**
     * Tries each choice in turn until a choice can successfully be matched. If
     * a choice is null, it is ignored.
     */
    @SafeVarargs
    public final void FirstOf(Runnable... choices) {
        for (Runnable choice : choices) {
            if (choice == null)
                continue;
            StateSnapshot snapshot = ctx.snapshot();
            try {
                choice.run();
                return;
            } catch (NoMatchException e) {
                // swallow, restore
                snapshot.restore();
            }
        }
        throw ctx.noMatch();
    }

    /**
     * Return the first provided value
     */
    public final <T> T FirstValue(T first, Object... others) {
        return first;
    }

    /**
     * Run the given runnables and return the first provided value
     */
    public final <T> T FirstValue(T first, Runnable... runnables) {
        for (Runnable runnable : runnables) {
            runnable.run();
        }
        return first;
    }

    /**
     * Match the first supplier followed by the runnables and return the result
     */
    public final <T> T FirstValue(Supplier<? extends T> first, Runnable... runnables) {
        T result = first.get();
        for (Runnable runnable : runnables) {
            runnable.run();
        }
        return result;
    }

    /**
     * Match the runnable and return the result
     */
    public final <T> T LastValue(Runnable runnable, T result) {
        runnable.run();
        return result;
    }

    /**
     * Match the runnable and return the result of the supplier.
     */
    public final <T> T LastValue(Runnable runnable, Supplier<? extends T> last) {
        runnable.run();
        return last.get();
    }

    /**
     * Tries each choice in turn until a choice can successfully be matched and
     * returns it's value. If a choice is null, it is ignored.
     */
    @SafeVarargs
    public final <T> T FirstOfRet(Supplier<? extends T>... choices) {
        for (Supplier<? extends T> choice : choices) {
            if (choice == null)
                continue;
            StateSnapshot snapshot = ctx.snapshot();
            try {
                return choice.get();
            } catch (NoMatchException e) {
                // swallow, restore
                snapshot.restore();
            }
        }
        throw ctx.noMatch();
    }

    /**
     * Tries each choice in turn until a choice can successfully be matched and
     * returns it's value. If a choice is null, it is ignored.
     */
    public final <T> T FirstOfRet(Iterable<Supplier<? extends T>> choices) {
        for (Supplier<? extends T> choice : choices) {
            if (choice == null)
                continue;
            StateSnapshot snapshot = ctx.snapshot();
            try {
                return choice.get();
            } catch (NoMatchException e) {
                // swallow, restore
                snapshot.restore();
            }
        }
        throw ctx.noMatch();
    }

    /**
     * Repeat matching the term until it fails. Succeeds even if the term never
     * matches.
     */
    public final void ZeroOrMore(Runnable term) {
        while (true) {
            StateSnapshot snapshot = ctx.snapshot();
            try {
                term.run();
            } catch (NoMatchException e) {
                // swallow, restore, break loop
                snapshot.restore();
                break;
            }
        }
    }

    /**
     * Repeat matching the term until it fails. Succeeds even if the term never
     * matches. The return values of the terms are collected and returned.
     */
    public final <T> Collection<T> ZeroOrMoreRet(Supplier<T> term) {
        ArrayList<T> parts = new ArrayList<>();
        while (true) {
            StateSnapshot snapshot = ctx.snapshot();
            try {
                parts.add(term.get());
            } catch (NoMatchException e) {
                // swallow, restore, break loop
                snapshot.restore();
                break;
            }
        }
        return parts;
    }

    /**
     * Check if the given precedence level is equal or greater than current
     * minimum precedence level. If true, evaluate the term with the given
     * precedence level as new minimum precedence level and return the result.
     * Otherwise the match fails.
     */
    public final <T> T PrecedenceGTE(int level, Supplier<T> term) {
        ParsingState<?> state = ctx.state();
        if (state.minPrecedenceLevel <= level) {
            int old = state.minPrecedenceLevel;
            ctx.state().minPrecedenceLevel = level;
            try {
                return term.get();
            } finally {
                ctx.state().minPrecedenceLevel = old;
            }
        } else
            throw ctx.noMatch();
    }

    /**
     * Check if the given precedence level greater than the current minimum
     * precedence level. If true, evaluate the term with the given precedence
     * level as new minimum precedence level and return the result. Otherwise
     * the match fails.
     */
    public final <T> T PrecedenceGT(int level, Supplier<T> term) {
        ParsingState<?> state = ctx.state();
        if (state.minPrecedenceLevel < level) {
            int old = state.minPrecedenceLevel;
            ctx.state().minPrecedenceLevel = level;
            try {
                return term.get();
            } finally {
                ctx.state().minPrecedenceLevel = old;
            }
        } else
            throw ctx.noMatch();
    }

    /**
     * Evaluate the term with the minimum precedence level cleared (set to 0)
     * and return the result.
     */
    public final <T> T PrecedenceClear(Supplier<T> term) {
        return PrecedenceSet(0, term);

    }

    /**
     * Evaluate the term with the given precedence level as new minimum
     * precedence level and return the result.
     */
    public final <T> T PrecedenceSet(int level, Supplier<T> term) {
        ParsingState<?> state = ctx.state();
        int old = state.minPrecedenceLevel;
        ctx.state().minPrecedenceLevel = level;
        try {
            return term.get();
        } finally {
            ctx.state().minPrecedenceLevel = old;
        }
    }

    /**
     * Try to match the term. If it fails, succeed anyways
     */
    public final void Opt(Runnable term) {
        OptRet(() -> {
            term.run();
            return null;
        });
    }

    /**
     * Try to match the term. If it fails, succeed anyways. If the term matches,
     * return the result, otherwise {@link java.util.Optional#empty()}
     */
    public final <T> Optional<T> OptRet(Supplier<T> term) {
        StateSnapshot snapshot = ctx.snapshot();
        try {
            return Optional.ofNullable(term.get());
        } catch (NoMatchException e) {
            // swallow, restore, break loop
            snapshot.restore();
            return Optional.empty();
        }
    }

    /**
     * Match one or more chars matching the criteria. If no matching character
     * is found, report the unmet expectation.
     */
    //public final String OneOrMoreChars(Predicate<Integer> criteria, String expectation) {
    public final String OneOrMoreChars(Predicate<Character> criteria, String expectation) {
        String result = ZeroOrMoreChars(criteria, expectation);
        if (result.isEmpty()) {
            throw ctx.noMatch(expectation);
        }
        return result;
    }

    /**
     * Match zero or more chars matching the criteria.
     */
    //public final String ZeroOrMoreChars(Predicate<Integer> criteria, String expectation) {
    public final String ZeroOrMoreChars(Predicate<Character> criteria, String expectation) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (ctx.hasNext() && criteria.test(ctx.peek())) {
                //sb.appendCodePoint(ctx.next());
                sb.append(ctx.next());
            } else {
                ctx.registerExpectation(expectation);
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Match the term one ore more times. Return the results of the matched
     * terms.
     */
    public final <T> Collection<T> OneOrMoreRet(Supplier<T> term) {
        ArrayList<T> parts = new ArrayList<>();
        while (true) {
            StateSnapshot snapshot = ctx.snapshot();
            try {
                parts.add(term.get());
            } catch (NoMatchException e) {
                // swallow, restore, break loop
                snapshot.restore();
                break;
            }
        }
        if (parts.isEmpty()) {
            throw ctx.noMatch();
        }
        return parts;
    }

    /**
     * Match the term one or more times
     */
    public final void OneOrMore(Runnable term) {
        boolean found = false;
        while (true) {
            StateSnapshot snapshot = ctx.snapshot();
            try {
                term.run();
                found = true;
            } catch (NoMatchException e) {
                // swallow, restore, break loop
                snapshot.restore();
                break;
            }
        }
        if (!found) {
            throw ctx.noMatch();
        }
    }

    public final <T> Collection<T> OneOrMoreRet(Supplier<T> term, Runnable separator) {
        ArrayList<T> result = new ArrayList<>();
        result.add(term.get());
        result.addAll(ZeroOrMoreRet(() -> {
            separator.run();
            return term.get();
        }));
        return result;
    }

    public final <T> Collection<T> ZeroOrMoreRet(Supplier<T> term, Runnable separator) {
        return OptRet(() -> OneOrMoreRet(term, separator)).orElse(Collections.emptyList());
    }

    /**
     * Match the supplied term. All expectations generated while matching the
     * term are dropped. If matching the term fails, the single specified
     * expectation is registered as expected at the input position at the
     * beginning of the matching attempt.
     */
    public final void Atomic(String expectation, Runnable term) {
        AtomicRet(expectation, () -> {
            term.run();
            return null;
        });
    }

    /**
     * Match the supplied term. All expectations generated while matching the
     * term are dropped. If matching the term fails, the single specified
     * expectation is registered as expected at the input position at the
     * beginning of the matching attempt.
     */
    public final <T> T AtomicRet(String expectation, Supplier<T> term) {
        int startIdx = ctx.getIndex();
        ExpectationFrame oldFrame = ctx.getExpectationFrame();
        ctx.setNewExpectationFrame();
        try {
            return term.get();
        } catch (NoMatchException e) {
            oldFrame.registerExpectation(startIdx, expectation);
            throw e;
        } finally {
            ctx.setExpectationFrame(oldFrame);
        }
    }

    /**
     * Match the supplied term. All expectations generated while matching the
     * term are dropped. If matching the term fails, the single specified
     * expectation is registered as expected at the input position farthest to
     * the right which has been reached.
     */
    public final <T> T Expect(String expectation, Supplier<T> term) {
        ExpectationFrame oldFrame = ctx.getExpectationFrame();
        ExpectationFrame newFrame = ctx.setNewExpectationFrame();
        try {
            return term.get();
        } catch (NoMatchException e) {
            ctx.setExpectationFrame(oldFrame);
            oldFrame = null;
            ctx.registerExpectation(expectation, newFrame.index);
            throw e;
        } finally {
            if (oldFrame != null)
                ctx.setExpectationFrame(oldFrame);
        }
    }

    /**
     * Match any character. The returned string contains the matched unicode
     * character, as one or two chars (for surrogate pairs)
     */
    //public final String AnyChar() {
    public final char AnyChar() {
        if (!ctx.hasNext())
            throw ctx.noMatch("any character");
        //return new String(Character.toChars(ctx.next()));
        return ctx.next();
    }

    /**
     * Helper method matching a string. Returns false if the string could not be
     * found.
     */
    private boolean matchString(String expected) {
        /*OfInt it = expected.codePoints().iterator();
        while (it.hasNext() && ctx.hasNext()) {
            if (it.nextInt() != ctx.peek()) {
                return false;
            }
            ctx.next();
        }

        return !it.hasNext();*/
        int idx = 0;
        for (; idx < expected.length() && ctx.hasNext(); idx++) {
            if (expected.charAt(idx) != ctx.peek()) {
                return false;
            }
            ctx.next();
        }
        return idx >= expected.length();
    }

    /**
     * Match a String. The matched string is returned.
     */
    public final String Str(String expected) {
        int startIndex = ctx.getIndex();
        if (!matchString(expected))
            throw ctx.noMatch(escapeString(expected), startIndex);
        else
            return expected;
    }

    /**
     * Match a String. The provided result is returned.
     */
    public final <T> T Str(String expected, T result) {
        int startIndex = ctx.getIndex();
        if (!matchString(expected))
            throw ctx.noMatch(escapeString(expected), startIndex);
        else
            return result;
    }

    /**
     * Match the input at the current position against the expected string. If
     * the input matches, use the result supplier to return the result
     */
    public final <T> T Str(String expected, Supplier<T> result) {
        int startIndex = ctx.getIndex();
        if (!matchString(expected))
            throw ctx.noMatch(escapeString(expected), startIndex);
        else
            return result.get();
    }

    /**
     * Match a character using the given predicate which is evaluated against
     * the next code point in the input. If the match fails, the specified
     * expectation is reported.
     */
    //public final String Char(Predicate<Integer> predicate, String expectation) {
    public final char Char(Predicate<Character> predicate, String expectation) {
        int startIndex = ctx.getIndex();
        if (ctx.hasNext()) {
            /*int*/char cp = ctx.next();
            if (predicate.test(cp)) {
                //return new String(Character.toChars(cp));
                return cp;
            }
        }
        throw ctx.noMatch(expectation, startIndex);

    }

    /**
     * Match specified char.
     */
    public final char MatchChar(char ch) {
        int startIndex = ctx.getIndex();
        if (ctx.hasNext()) {
            char cp = ctx.next();
            if (ch == cp) {
                return cp;
            }
        }
        throw ctx.noMatch(escapeChar(ch), startIndex);
    }

    public static final String escapeChar(char ch) {
        StringBuilder escape = new StringBuilder("'");
        switch (ch) {
            case '\\':
            case '\'':
                escape.append('\\');
                break;
            default:
                break;
        }
        if (ch != ' ' && Character.isWhitespace(ch)) {
            escape.append(String.format("\\u%04x", (int)ch));
        }
        else {
            escape.append(ch);
        }
        escape.append("'");
        return escape.toString();
    }

    public static final String escapeString(String s) {
        StringBuilder escape = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\\':
                case '"':
                    escape.append('\\');
                    break;
                default:
                    break;
            }
            if (ch != ' ' && Character.isWhitespace(ch)) {
                escape.append(String.format("\\u%04x", (int)ch));
            }
            else {
                escape.append(ch);
            }
        }
        escape.append("\"");
        return escape.toString();
    }

    /**
     * Match any of the specified chars.
     */
    public final char AnyOf(String chars) {
        int startIndex = ctx.getIndex();
        if (ctx.hasNext()) {
            char cp = ctx.next();
            if (chars.indexOf(cp) != -1) {
                return cp;
            }
        }
        String expectation = createCharsExpectation(chars);
        throw ctx.noMatch(expectation, startIndex);
    }

    public static String createCharsExpectation(String chars) {        
        StringBuilder expectation = new StringBuilder();
        for (int i = 0; i < chars.length(); i++) {
            if (i > 0) {
                if (i + 1 < chars.length()) {
                    expectation.append(", ");
                }
                else {
                    expectation.append(" or ");
                }
            }
            expectation.append(escapeChar(chars.charAt(i)));
        }
        return expectation.toString();
    }

    /**
     * Match all chars except the ones specified
     */
    //public final String NoneOf(String chars) {
    public final char NoneOf(String chars) {
        int startIndex = ctx.getIndex();
        if (ctx.hasNext()) {
            /*int*/char cp = ctx.next();
            if (chars.indexOf(cp) == -1) {
            //if (chars.codePoints().allMatch(x -> x != cp)) {
                //return String.valueOf(Character.toChars(cp));
                return cp;
            }
        }
        String expectation = createCharsExpectation(chars);
        throw ctx.noMatch("any char except " + expectation, startIndex);
    }

    /**
     * Match all characters in a given range (inclusive). Return a string
     * containing only the matched character.
     */
    //public final String CharRange(int first, int last) {
    public final char CharRange(char first, char last) {
        int startIndex = ctx.getIndex();
        if (ctx.hasNext()) {
            /*int*/char cp = ctx.next();
            if (cp >= first && cp <= last) {
                //return new String(Character.toChars(cp));
                return cp;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("character between ");
        //sb.appendCodePoint(first);
        sb.append(escapeChar(first));
        sb.append(" and ");
        //sb.appendCodePoint(last);
        sb.append(escapeChar(last));
        throw ctx.noMatch(sb.toString(), startIndex);
    }

    public TCtx getParsingContext() {
        return ctx;
    }

    /**
     * get the current position
     */
    public PositionInfo pos() {
        return ctx.currentPositionInfo();
    }

    @Override
    public java.lang.String toString() {
        PositionInfo info = ctx.currentPositionInfo();
        return getClass().getSimpleName() + " line: " + info.getLineNr() + "\n" + info.getLine() + "\n"
                + info.getUnderline();
    }
}

package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import java.util.Stack;

import com.aaronicsubstances.code.augmentor.parsing.peg.Parser;

public class StackEnabledParser<T> extends Parser<StackEnabledParsingContext<T>> {
    private Stack<Integer> startPosStack = new Stack<>();
    private int lastStartPos;
    
    public StackEnabledParser(StackEnabledParsingContext<T> ctx) {
        super(ctx);
    }

    public void push(T item) {
        getParsingContext().getValueStack().push(item);
    }

    public T pop() {
        T item = getParsingContext().getValueStack().pop();
        return item;
    }

    public int markRuleStart() {
        int startPos = getParsingContext().state().index;
        startPosStack.push(startPos);
        return startPos;
    }

    public IndexRange markRuleEnd() {
        lastStartPos = startPosStack.pop();
        return matchRange();
    }

    public IndexRange matchRange() {
        int endPos = getParsingContext().state().index;
        return new IndexRange(lastStartPos, endPos);
    }
}
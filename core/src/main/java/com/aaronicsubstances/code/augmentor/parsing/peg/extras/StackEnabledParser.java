package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.parsing.peg.NoMatchException;
import com.aaronicsubstances.code.augmentor.parsing.peg.Parser;
import com.aaronicsubstances.code.augmentor.parsing.peg.PositionInfo;
import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext.ErrorDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackEnabledParser extends Parser<StackEnabledParsingContext> {
    private final Logger LOG = LoggerFactory.getLogger(getClass().getSimpleName());
    private StackEnabledParsingState lastState;
    private boolean loggingEnabled = false;
    
    public StackEnabledParser(StackEnabledParsingContext ctx) {
        super(ctx);
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    protected void beginLog(String ruleName, boolean significant) {
        if (!loggingEnabled) {
            return;
        }
        if (!significant) {
            return;
        }
        beginLog(ruleName);
    }

    protected void beginLog(String ruleName) {
        if (!loggingEnabled) {
            return;
        }
        int currPos = getParsingContext().state().index;
        LOG.info("Attempting to match rule '{}' at pos {}...", ruleName, currPos);
    }

    protected void endLog(String ruleName, boolean significant, NoMatchException ex) {
        if (!loggingEnabled) {
            return;
        }
        if (!significant) {
            return;
        }
        endLog(ruleName, ex);
    }

    protected void endLog(String ruleName, NoMatchException ex) {
        if (!loggingEnabled) {
            return;
        }
        int currPos = getParsingContext().state().index;
        if (ex != null) {
            String errorMsg = craftErrorMessage();
            LOG.info("Failed to match rule '{}' at pos {}: {}", ruleName, currPos, errorMsg);
        }
        else {
            LOG.info("Successfully matched rule '{}' at pos {}.", ruleName, currPos - 1);
        }
    }

    protected void endLogMajorSuccess(String ruleName) {
        if (!loggingEnabled) {
            return;
        }
        int currPos = getParsingContext().state().index;
        String successMsg = craftSuccessMessage();
        LOG.info("Successfully matched rule '{}' at pos {}: {}", ruleName, currPos - 1, 
            successMsg);
    }

    private String craftErrorMessage() {
        ErrorDescription errorDesc = getParsingContext().getErrorDescription();
        // Replace whitespace expected chars with hex values.
        String expectations = errorDesc.expectations.stream()
            .collect(Collectors.joining(", "));
        return "Error on line " + errorDesc.errorLineInfo.getLineNr() + ". Expected: "
                    + expectations + " instead of '" + errorDesc.errorLineInfo.getPositionChar()
                    + "'\n" + errorDesc.errorLineInfo.getLine() + "\n" + 
                    errorDesc.errorLineInfo.getUnderline(' ', '^');
    }

    private String craftSuccessMessage() {
        int currPos = getParsingContext().state().index;
        PositionInfo currPosInfo = new PositionInfo(getParsingContext().getContent(), 
            currPos - 1);
        return "Line " + currPosInfo.getLineNr()
                    + "\n" + currPosInfo.getLine() + "\n" + 
                    currPosInfo.getUnderline(' ', '^');

    }

    protected void push(Object item) {
        LinkedList<Object> valueStack = getParsingContext().getValueStack();
        valueStack.push(item);
    }

    protected <T> T peek(Class<T> itemClass) {        
        T item = searchStack(itemClass, false);
        return item;
    }

    protected <T> T pop(Class<T> itemClass) {
        T item = searchStack(itemClass, true);
        return item;
    }

    @SuppressWarnings("unchecked")
    private <T> T searchStack(Class<T> itemClass, boolean remove) {
        LinkedList<Object> valueStack = getParsingContext().getValueStack();
        T item = null;
        Iterator<Object> it = valueStack.iterator();
        while (it.hasNext()) {
            Object stackItem = it.next();
            if (itemClass.isAssignableFrom(stackItem.getClass())) {
                item = (T)stackItem;
                if (remove) {
                    it.remove();
                }
                break;
            }
        }
        if (item == null && remove) {
            throw new NoSuchElementException();
        }
        return item;
    }

    protected void markRuleStart() {
        push(getParsingContext().state().clone());
    }

    protected void markRuleEnd() {
        lastState = pop(StackEnabledParsingState.class);
    }

    protected IndexRange matchRange() {
        int startPos = lastState.index;
        int endPos = getParsingContext().state().index;
        return new IndexRange(startPos, endPos);
    }
}
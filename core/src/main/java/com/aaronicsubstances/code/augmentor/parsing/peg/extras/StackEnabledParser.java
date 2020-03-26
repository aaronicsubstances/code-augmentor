package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.parsing.peg.NoMatchException;
import com.aaronicsubstances.code.augmentor.parsing.peg.Parser;
import com.aaronicsubstances.code.augmentor.parsing.peg.PositionInfo;
import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext.ErrorDescription;

public class StackEnabledParser extends Parser<StackEnabledParsingContext> {
    private StackEnabledParsingState lastState;
    private Consumer<String> traceLog;
    private Consumer<String> infoLog;
    private int indentSize = 4;
    private char indentChar = ' ';
    
    public StackEnabledParser(StackEnabledParsingContext ctx) {
        super(ctx);
    }

    public Consumer<String> getTraceLog() {
        return traceLog;
    }

    public void setTraceLog(Consumer<String> traceLog) {
        this.traceLog = traceLog;
    }

    public Consumer<String> getInfoLog() {
        return infoLog;
    }

    public void setInfoLog(Consumer<String> infoLog) {
        this.infoLog = infoLog;
    }

    public int getIndentSize() {
        return indentSize;
    }

    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

    public char getIndentChar() {
        return indentChar;
    }

    public void setIndentChar(char indentChar) {
        this.indentChar = indentChar;
    }

    protected List<PegToken> getTokenList() {        
        List<PegToken> tokenList = new ArrayList<>();
        Stack<Object> tokenStack = getParsingContext().getValueStackMap().get(PegToken.class);
        if (tokenStack != null) {
            for (Object stackItem : tokenStack) {
                assert stackItem instanceof PegToken;
                // looks like stack iterator presents items in FIFO order.
                tokenList.add((PegToken)stackItem);
            }
        }
        return tokenList;
    }

    public void runRule(String ruleName, Runnable ruleCode) {
        if (traceLog == null && infoLog == null) {
            ruleCode.run();
            return;
        }
        runRule(ruleName, false, ruleCode);
    }

    public void runRule(String ruleName, boolean suppressChildNodes, Runnable ruleCode) {
        if (traceLog == null && infoLog == null) {
            ruleCode.run();
            return;
        }
        LogData prevLogData = peek(LogData.class);
        if (prevLogData == null) {
            prevLogData = new LogData(0, false);
            push(prevLogData);
        }
        if (!prevLogData.supressLogs) {
            beginLog(ruleName, prevLogData.depth);
        }
        push(new LogData(prevLogData.depth + 1, suppressChildNodes || prevLogData.supressLogs));
        try {
            ruleCode.run();
            if (!prevLogData.supressLogs) {
                // make major log if at boundary of child node suppression.
                boolean isMajor = infoLog != null && suppressChildNodes;
                endLog(ruleName, prevLogData.depth, null, isMajor);
            }
        }
        catch (NoMatchException ex) {
            if (!prevLogData.supressLogs) {
                endLog(ruleName, prevLogData.depth, ex, false);
            }
            throw ex;
        }
        finally {
            pop(LogData.class);
        }
    }

    private void beginLog(String ruleName, int depth) {
        if (traceLog == null && infoLog == null) {
            return;
        }
        int currPos = getParsingContext().state().index;
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        if (traceLog != null) {
            String log = String.format("%s[TRACE] Attempting to match rule '%s' at pos %s...", 
                indent, ruleName, currPos);
            traceLog.accept(log);
        }
        else {
            String log = String.format("%s[INFO] Attempting to match rule '%s' at pos %s...", 
                indent, ruleName, currPos);
            infoLog.accept(log);
        }
    }

    private void endLog(String ruleName, int depth, NoMatchException ex, boolean isMajor) {
        if (ex != null && traceLog == null) {
            return;
        }
        if (ex == null && traceLog == null && infoLog == null) {
            return;
        }
        int currPos = getParsingContext().state().index;
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        if (ex != null) {
            StringBuilder msgIndent = new StringBuilder(indent);
            for (int i = 0; i < "[TRACE] ".length(); i++) {
                msgIndent.append(indentChar);
            }
            String errorMsg = craftErrorMessage(msgIndent.toString());
            String log = String.format("%s[TRACE] Failed to match rule '%s' at pos %s: %s",
                indent, ruleName, currPos, errorMsg);
            traceLog.accept(log);
        }
        else {
            if (isMajor) { 
                logMajorSuccess(ruleName, indent.toString());
            }
            else {
                if (traceLog != null) {
                    String log = String.format("%s[TRACE] Successfully matched rule '%s' just before pos %s.", 
                        indent, ruleName, currPos);
                    traceLog.accept(log);
                }
                else {
                    String log = String.format("%s[INFO] Successfully matched rule '%s' just before pos %s.", 
                        indent, ruleName, currPos);
                    infoLog.accept(log);
                }
            }
        }
    }

    private void logMajorSuccess(String ruleName, String indent) {
        StringBuilder msgIndent = new StringBuilder(indent);
        for (int i = 0; i < "[INFO] ".length(); i++) {
            msgIndent.append(indentChar);
        }
        String successMsg = craftSuccessMessage(msgIndent.toString());        
        int currPos = getParsingContext().state().index;
        String log = String.format("%s[INFO] Successfully matched rule '%s' just before pos %s: %s", 
            indent, ruleName, currPos, successMsg);
        infoLog.accept(log);
    }

    private String craftErrorMessage(String indent) {
        ErrorDescription errorDesc = getParsingContext().getErrorDescription();
        String expectations = errorDesc.expectations.stream()
            .collect(Collectors.joining(", "));
        return "Error on line " + errorDesc.errorLineInfo.getLineNr() + ". Expected: "
                    + expectations + " instead of " + escapeString(errorDesc.errorLineInfo.getPositionChar())
                    + "\n" + indent + errorDesc.errorLineInfo.getLine() + "\n" + indent +
                    errorDesc.errorLineInfo.getUnderline(' ', '^');
    }

    private String craftSuccessMessage(String indent) {
        int currPos = getParsingContext().state().index;
        PositionInfo currPosInfo = new PositionInfo(getParsingContext().getContent(), 
            currPos - 1);
        return "Line " + currPosInfo.getLineNr()
                    + "\n" + indent + currPosInfo.getLine() + "\n" + indent +
                    currPosInfo.getUnderline(' ', '^');

    }

    protected void push(Object item) {
        Class<?> itemClass = item.getClass();
        Map<Class<?>, Stack<Object>> valueStackMap = getParsingContext().getValueStackMap();
        Stack<Object> valueStack;
        if (valueStackMap.containsKey(itemClass)) {
            valueStack = valueStackMap.get(itemClass);
        }
        else {
            valueStack = new Stack<>();
            valueStackMap.put(itemClass, valueStack);
        }
        valueStack.push(item);
    }

    @SuppressWarnings("unchecked")
    protected <T> T peek(Class<T> itemClass) {
        Map<Class<?>, Stack<Object>> valueStackMap = getParsingContext().getValueStackMap();
        if (valueStackMap.containsKey(itemClass)) {
            Stack<Object> valueStack = valueStackMap.get(itemClass);
            T stackItem = (T)valueStack.peek();
            return stackItem;
        }
        else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T pop(Class<T> itemClass) {
        Map<Class<?>, Stack<Object>> valueStackMap = getParsingContext().getValueStackMap();
        if (valueStackMap.containsKey(itemClass)) {
            Stack<Object> valueStack = valueStackMap.get(itemClass);
            T stackItem = (T)valueStack.pop();
            return stackItem;
        }
        else {
            throw new NoSuchElementException();
        }
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